###this was the final run for clqe experiment
##this experiement checks semantic meanings of the expanded query
#######...............Final run clqe
# CLQE Semantic Translation Coverage — final V2 Jupyter cell
# Install once in Jupyter: %pip install openpyxl openai
import os, re, json, time, random, hashlib, tempfile
from pathlib import Path
from getpass import getpass
from collections import Counter
from datetime import datetime, timezone
from openpyxl import load_workbook
from openpyxl.styles import Font, PatternFill
from openpyxl.cell.cell import ILLEGAL_CHARACTERS_RE
from openai import OpenAI

# ---------------- SETTINGS ----------------
DIRECTORY = Path(r'D:\BORUE')
INPUT_FILE = DIRECTORY / 'NewTestBenchSort_Expanded.xlsx'
OUTPUT_FILE = DIRECTORY / 'CLQE_Semantic_Evaluation_Final.xlsx'
MODEL = 'gpt-4o-mini'
PROMPT_VERSION = 'CLQE_SEMANTIC_V2'
MAX_QUERIES = 0                 # Full dataset; completed rows are skipped
MAX_JUDGE_ATTEMPTS = 4
MAX_API_RETRIES = 5
CONDITIONS = ['var1', 'var2', 'var3', 'var4', 'var5', 'common']

PROMPT = '''You are an independent bilingual lexical-semantic evaluator.
Evaluate whether ontology-derived English expansion terms preserve the meaning
of the expected English translation for a Roman Urdu lexical item.
This is SEMANTIC TRANSLATION COVERAGE, NOT document retrieval.
Judge each supplied condition independently against reference_english.
roman_urdu is only disambiguating context; do not replace the reference meaning.

FIXED GRADES:
3 = Equivalent or near-equivalent in the intended sense: candidate can replace
    the reference translation in an ordinary context with little or no change
    in meaning. Mere thematic relatedness is NEVER enough. Broader or narrower
    expressions that materially change the meaning do not receive 3.
2 = Substantial semantic overlap, but not sufficiently interchangeable.
    Includes meaningful broader/narrower related senses when the overlap is clear.
1 = Weak conceptual or topical association; limited or indirect relationship.
0 = Unrelated or misleading in the reference sense.

RULES:
- Prefer the intended sense of reference_english; avoid exploiting unrelated
  polysemous senses merely to award a higher grade.
- Do not confuse phonetic resemblance, shared topic, or positivity with synonymy.
- A continuity-related term is not automatically equivalent to forever.
- Select the single strongest term FROM the supplied candidates for each condition.
- For grades 1, 2, 3, best_term MUST be copied exactly from candidates.
- For grade 0, best_term may be empty. If no candidate is related, use empty.
- Never invent or rewrite a candidate term.
- Return one judgment per condition, no extra or missing conditions.
- Give a concise reason grounded in the reference/candidate meaning.

Return JSON object only:
{"judgments":[{"condition":"var1","score":2,"best_term":"candidate from list","reason":"Brief reason"}]}
'''

def clean(x):
    return '' if x is None else str(x).strip()

def norm(x):
    return re.sub(r'\s+', ' ', clean(x)).casefold()

def safe(x):
    return ILLEGAL_CHARACTERS_RE.sub('', x)[:32767] if isinstance(x, str) else x

def terms(x):
    out, seen = [], set()
    for item in clean(x).split(','):
        item = item.strip()
        if norm(item) and norm(item) not in seen:
            seen.add(norm(item)); out.append(item)
    return out

def pct(n, d):
    return 100 * n / d if d else 0.0

def save_atomic(wb, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, temp = tempfile.mkstemp(prefix='clqe_v2_', suffix='.xlsx', dir=str(path.parent))
    os.close(fd)
    try:
        wb.save(temp)
        check = load_workbook(temp, read_only=True)
        check.close()
        os.replace(temp, path)
    finally:
        if os.path.exists(temp):
            os.remove(temp)

def call_with_retry(fn):
    for i in range(MAX_API_RETRIES):
        try:
            return fn()
        except Exception as e:
            status = getattr(e, 'status_code', None)
            transient = (status in (408, 409, 429) or
                         (isinstance(status, int) and 500 <= status < 600) or
                         type(e).__name__ in ('APIConnectionError', 'APITimeoutError',
                                              'RateLimitError', 'InternalServerError'))
            if not transient or i == MAX_API_RETRIES - 1:
                raise
            delay = min(60, 2**i + random.random())
            print(f'  API retry {i+1}/{MAX_API_RETRIES} in {delay:.1f}s: {str(e)[:100]}')
            time.sleep(delay)

def validate_response(data, pending):
    items = data.get('judgments')
    if not isinstance(items, list):
        raise ValueError('Missing judgments array')
    output = {}
    for item in items:
        if not isinstance(item, dict):
            raise ValueError('Non-object judgment')
        condition = clean(item.get('condition'))
        if condition not in pending or condition in output:
            raise ValueError(f'Unexpected/duplicate condition: {condition}')
        score = item.get('score')
        if type(score) is not int or score not in (0, 1, 2, 3):
            raise ValueError(f'Invalid score for {condition}: {score}')
        best, reason = clean(item.get('best_term')), clean(item.get('reason'))
        if not reason:
            raise ValueError(f'Missing reason for {condition}')
        mapping = {norm(t): t for t in pending[condition]}
        if score == 0 and not best:
            best = ''  # Correct: score-zero cases need not identify a term.
        elif norm(best) not in mapping:
            raise ValueError(f'Invented/missing best_term for {condition}: [{best}] (score {score})')
        else:
            best = mapping[norm(best)]
        output[condition] = dict(score=score, best=best, reason=reason, source='GPT-4o-mini')
    if set(output) != set(pending):
        raise ValueError(f'Missing conditions: {sorted(set(pending)-set(output))}')
    return output

def judge_one_indexed(reference, roman, condition, candidates, client):
    """Rejudge one condition; accept both V2 envelope and flat JSON responses.

    V2 rubric is unchanged. Only the response representation is more robust.
    A positive grade always needs a real index from the supplied candidates.
    """
    numbered = [{'index': i, 'term': term}
                for i, term in enumerate(candidates, 1)]
    payload = {
        'reference_english': reference,
        'roman_urdu': roman,
        'conditions': [{'condition': condition, 'candidates': candidates}],
        'numbered_candidates': numbered,
    }
    instruction = (
        'This is a SINGLE-CONDITION repair request. Apply the unchanged V2 '
        'semantic rubric. Return exactly ONE judgment in the SAME JSON '
        'envelope as the rubric: '
        '{"judgments":[{"condition":"' + condition + '",'
        '"score":1,"best_index":1,"reason":"short explanation"}]}. '
        'The example score and index are only structural examples; choose '
        'the correct grade and candidate independently. '
        'For scores 1, 2 or 3, best_index MUST be an integer from 1 to '
        'the number of supplied numbered_candidates. For score 0, '
        'best_index may be null. Do not return best_term. '
        'Never change a semantic grade simply to satisfy formatting.'
    )
    last_error = None
    for attempt in range(1, MAX_JUDGE_ATTEMPTS + 1):
        messages = [
            {'role': 'system', 'content': PROMPT + '\n' + instruction},
            {'role': 'user', 'content': json.dumps(payload, ensure_ascii=False)}
        ]
        def request():
            return client.chat.completions.create(
                model=MODEL, temperature=0,
                response_format={'type': 'json_object'}, messages=messages)
        response = call_with_retry(request)
        try:
            raw = response.choices[0].message.content
            data = json.loads(raw)
            if not isinstance(data, dict):
                raise ValueError('Response is not a JSON object')
            if isinstance(data.get('judgments'), list):
                items = data['judgments']
                if len(items) != 1 or not isinstance(items[0], dict):
                    raise ValueError('Expected exactly one judgment in envelope')
                item = items[0]
            elif isinstance(data.get('judgment'), dict):
                item = data['judgment']
            else:
                item = data
            returned_condition = clean(item.get('condition'))
            if returned_condition and returned_condition != condition:
                raise ValueError(f'Wrong condition: {returned_condition!r}')
            score = item.get('score')
            index = item.get('best_index')
            reason = clean(item.get('reason'))
            if type(score) is not int or score not in (0, 1, 2, 3):
                raise ValueError(f'Invalid score: {score!r}')
            if not reason:
                raise ValueError('Missing reason')
            if score == 0 and index is None:
                best = ''
            elif type(index) is int and 1 <= index <= len(candidates):
                best = candidates[index - 1]
            else:
                raise ValueError(f'Invalid candidate index: {index!r} for score {score}')
            return dict(score=score, best=best, reason=reason, source='GPT-4o-mini')
        except (ValueError, TypeError, KeyError, IndexError) as exc:
            last_error = exc
            print(f'  Indexed judgment retry {attempt}/{MAX_JUDGE_ATTEMPTS} '
                  f'for {condition}: {exc}')
            if attempt < MAX_JUDGE_ATTEMPTS:
                time.sleep(min(2**attempt, 10))
    raise ValueError(f'Indexed judgment invalid for {condition}: {last_error}')


def judge(reference, roman, pending, client):
    """Frozen V2 grades; preserve valid batch judgments, repair invalid ones.

    Primary request uses the original V2 prompt. If an individual response is
    malformed, evaluate only that condition using a numbered candidate list.
    """
    payload = {'reference_english': reference, 'roman_urdu': roman,
               'conditions': [{'condition': c, 'candidates': v}
                              for c, v in pending.items()]}
    messages = [
        {'role': 'system', 'content': PROMPT},
        {'role': 'user', 'content': json.dumps(payload, ensure_ascii=False)}
    ]
    def request():
        return client.chat.completions.create(
            model=MODEL, temperature=0,
            response_format={'type': 'json_object'}, messages=messages)
    response = call_with_retry(request)
    accepted = {}
    try:
        data = json.loads(response.choices[0].message.content)
        items = data.get('judgments', [])
        if not isinstance(items, list):
            items = []
        for item in items:
            if not isinstance(item, dict):
                continue
            condition = clean(item.get('condition'))
            if condition not in pending or condition in accepted:
                continue
            try:
                single = validate_response({'judgments': [item]},
                                           {condition: pending[condition]})
                accepted[condition] = single[condition]
            except (ValueError, TypeError, KeyError) as exc:
                print(f'  Repairing {condition}: {exc}')
    except (ValueError, TypeError, KeyError) as exc:
        print(f'  Batch JSON invalid; switching to indexed judgments: {exc}')

    for condition, candidates in pending.items():
        if condition not in accepted:
            accepted[condition] = judge_one_indexed(
                reference, roman, condition, candidates, client)

    if set(accepted) != set(pending):
        raise ValueError('Incomplete semantic judgments after indexed repair')
    return accepted

# ---------------- INPUT & CHECKPOINT ----------------
if not INPUT_FILE.is_file():
    raise FileNotFoundError(INPUT_FILE)
source_sha = hashlib.sha256(INPUT_FILE.read_bytes()).hexdigest()
prompt_sha = hashlib.sha256(PROMPT.encode('utf-8')).hexdigest()
if OUTPUT_FILE.exists():
    wb = load_workbook(OUTPUT_FILE)
    if 'Evaluation_Metadata' not in wb.sheetnames:
        raise ValueError('Output checkpoint has no Evaluation_Metadata sheet')
    metadata = {clean(a): clean(b) for a, b in wb['Evaluation_Metadata'].iter_rows(min_row=2, max_col=2, values_only=True)}
    expected = {'Input_SHA256': source_sha, 'Model': MODEL,
                'Prompt_Version': PROMPT_VERSION, 'Prompt_SHA256': prompt_sha}
    if any(metadata.get(k) != v for k, v in expected.items()):
        raise ValueError('Checkpoint input/model/prompt mismatch. Use a new output filename.')
    print('Resuming verified V2 checkpoint.')
else:
    wb = load_workbook(INPUT_FILE)
    meta = wb.create_sheet('Evaluation_Metadata')
    meta.append(['Property', 'Value'])
    for key, val in [('Input_SHA256', source_sha), ('Model', MODEL),
                     ('Prompt_Version', PROMPT_VERSION), ('Prompt_SHA256', prompt_sha),
                     ('Temperature', 0), ('Semantic_Threshold', 2),
                     ('Strict_Threshold', 3),
                     ('Started_UTC', datetime.now(timezone.utc).isoformat())]:
        meta.append([key, val])
    print('New V2 evaluation initialized.')

ws = wb.worksheets[0]
headers = {norm(c.value): c.column for c in ws[1] if c.value is not None}

def require(name):
    if norm(name) not in headers:
        raise ValueError(f'Missing input column: {name}')
    return headers[norm(name)]

def ensure(name):
    if norm(name) not in headers:
        col = ws.max_column + 1
        ws.cell(1, col, name)
        headers[norm(name)] = col
    return headers[norm(name)]

def get(row_num, name):
    return ws.cell(row_num, headers[norm(name)]).value

def put(row_num, name, val):
    ws.cell(row_num, headers[norm(name)], safe(val))

require('Common'); require('English_Translated')
for c in CONDITIONS:
    require('exp_' + c); require('Hit_' + c)
    for prefix in ('SemanticScore_', 'SemanticHit_', 'StrictSemanticHit_',
                   'BestSemanticTerm_', 'SemanticReason_', 'JudgmentSource_'):
        ensure(prefix + c)
for col in ('SemanticSuccessfulCount', 'SemanticAnySuccess',
            'SemanticVariantRecovery', 'LexicalSuccessfulCount'):
    ensure(col)

def completed(row_num):
    for c in CONDITIONS:
        score = get(row_num, 'SemanticScore_' + c)
        if type(score) is not int or score not in (0, 1, 2, 3):
            return False
        if clean(get(row_num, 'SemanticHit_' + c)) != ('Yes' if score >= 2 else 'No'):
            return False
        if clean(get(row_num, 'StrictSemanticHit_' + c)) != ('Yes' if score == 3 else 'No'):
            return False
        if not clean(get(row_num, 'JudgmentSource_' + c)):
            return False
    return True

def process_row(row_num, client):
    reference = clean(get(row_num, 'English_Translated'))
    roman = clean(get(row_num, 'Common'))
    if not reference:
        raise ValueError(f'Missing English_Translated at row {row_num}')
    results, pending = {}, {}
    lexical_count = 0
    for c in CONDITIONS:
        expansion = terms(get(row_num, 'exp_' + c))
        hit = norm(get(row_num, 'Hit_' + c))
        if hit not in ('yes', 'no'):
            raise ValueError(f'Invalid Hit_{c} at row {row_num}: {hit}')
        exact = [t for t in expansion if norm(t) == norm(reference)]
        if (hit == 'yes') != bool(exact):
           print(
                f"WARNING: Row {row_num}, {c}: "
                f"Java lexical hit={hit}, "
                f"Python normalized exact match={bool(exact)}"
           )
        if exact:
            lexical_count += 1
            results[c] = dict(score=3, best=exact[0],
                              reason='Exact lexical translation match.', source='Deterministic')
        elif not expansion:
            results[c] = dict(score=0, best='',
                              reason='No English expansion terms.', source='Deterministic')
        else:
            pending[c] = expansion
    if pending:
        results.update(judge(reference, roman, pending, client))
    if set(results) != set(CONDITIONS):
        raise ValueError(f'Incomplete query results at row {row_num}')
    semantic_count = sum(results[c]['score'] >= 2 for c in CONDITIONS)
    canonical_success = results['common']['score'] >= 2
    variant_success = any(results[c]['score'] >= 2 for c in CONDITIONS[:5])
    for c in CONDITIONS:
        r = results[c]; score = r['score']
        put(row_num, 'SemanticScore_' + c, score)
        put(row_num, 'SemanticHit_' + c, 'Yes' if score >= 2 else 'No')
        put(row_num, 'StrictSemanticHit_' + c, 'Yes' if score == 3 else 'No')
        put(row_num, 'BestSemanticTerm_' + c, r['best'])
        put(row_num, 'SemanticReason_' + c, r['reason'])
        put(row_num, 'JudgmentSource_' + c, r['source'])
    put(row_num, 'SemanticSuccessfulCount', semantic_count)
    put(row_num, 'SemanticAnySuccess', 'Yes' if semantic_count else 'No')
    put(row_num, 'SemanticVariantRecovery',
        'Yes' if not canonical_success and variant_success else 'No')
    put(row_num, 'LexicalSuccessfulCount', lexical_count)
    return len(pending)

# ---------------- RUN ----------------
selected = []
for r in range(2, ws.max_row + 1):
    if not clean(get(r, 'Common')) and not clean(get(r, 'English_Translated')):
        continue
    selected.append(r)
    if MAX_QUERIES and len(selected) >= MAX_QUERIES:
        break
remaining = [r for r in selected if not completed(r)]
print(f'Selected: {len(selected)} | Already complete: {len(selected)-len(remaining)} | Remaining: {len(remaining)}')
client = None
if remaining:
    if not os.environ.get('OPENAI_API_KEY'):
        os.environ['OPENAI_API_KEY'] = getpass('OpenAI API key: ')
    client = OpenAI(api_key=os.environ['OPENAI_API_KEY'])
processed = api_queries = 0
for i, r in enumerate(selected, 1):
    if completed(r):
        print(f'[{i}/{len(selected)}] SKIP row {r}')
        continue
    print(f'[{i}/{len(selected)}] Row {r}: {clean(get(r, "Common"))}')
    try:
        n = process_row(r, client)
        processed += 1
        api_queries += bool(n)
        save_atomic(wb, OUTPUT_FILE)
        print(f'  SAVED | GPT-assessed conditions: {n}')
    except Exception as exc:
        print(f'ERROR row {r}: {type(exc).__name__}: {exc}')
        print('Last successfully saved checkpoint remains intact. Correct and rerun.')
        raise

# ---------------- REBUILD ANALYSIS (NO API CALLS) ----------------
for name in ('Variant_Comparison', 'Semantic_Recovery', 'Score_Distribution',
             'Query_Distribution', 'Overall_Summary'):
    if name in wb.sheetnames:
        del wb[name]
variant = wb.create_sheet('Variant_Comparison')
recovery = wb.create_sheet('Semantic_Recovery')
scores_sheet = wb.create_sheet('Score_Distribution')
query_sheet = wb.create_sheet('Query_Distribution')
summary = wb.create_sheet('Overall_Summary')
variant.append(['Variant','Evaluated','Lexical_Yes','Lexical_HR_Percent','Semantic_Yes_GE2',
                'Semantic_HR_Percent','Strict_Yes_EQ3','Strict_HR_Percent','Mean_Expansion_Terms'])
recovery.append(['Variant','Lexical_Misses','Recovered_GE2','Recovered_EQ3',
                 'Weak_Score1_Among_Misses','Unrelated_Score0_Among_Misses',
                 'Recovery_Rate_Percent','Strict_Recovery_Rate_Percent'])
scores_sheet.append(['Variant','Score_0','Score_1','Score_2','Score_3','Total'])
query_sheet.append(['Successful_Variants','Lexical_Queries','Semantic_Queries',
                    'Lexical_Percent','Semantic_Percent'])
summary.append(['Metric','Value'])
counts = {c: {'n':0,'lexical':0,'semantic':0,'strict':0,'misses':0,
              'recovered':0,'strict_recovered':0,'terms':0,'scores':Counter(),
              'miss_scores':Counter()} for c in CONDITIONS}
lex_dist, sem_dist = Counter(), Counter()
evaluated = incomplete = canonical = any_variant = variant_recovered = 0
lex_any = sem_any = 0
for r in range(2, ws.max_row + 1):
    if not clean(get(r, 'Common')) and not clean(get(r, 'English_Translated')):
        continue
    if not completed(r):
        incomplete += 1
        continue
    evaluated += 1
    lex_n = sem_n = 0
    sem_flags = {}
    for c in CONDITIONS:
        d = counts[c]
        lexical = norm(get(r, 'Hit_' + c)) == 'yes'
        score = int(get(r, 'SemanticScore_' + c))
        sem = score >= 2
        d['n'] += 1; d['scores'][score] += 1
        d['terms'] += len(terms(get(r, 'exp_' + c)))
        if lexical:
            d['lexical'] += 1; lex_n += 1
        else:
            d['misses'] += 1; d['miss_scores'][score] += 1
            if sem: d['recovered'] += 1
            if score == 3: d['strict_recovered'] += 1
        if sem: d['semantic'] += 1; sem_n += 1
        if score == 3: d['strict'] += 1
        sem_flags[c] = sem
    lex_dist[lex_n] += 1; sem_dist[sem_n] += 1
    lex_any += lex_n > 0; sem_any += sem_n > 0
    canonical += sem_flags['common']
    v_success = any(sem_flags[c] for c in CONDITIONS[:5])
    any_variant += v_success
    variant_recovered += v_success and not sem_flags['common']
for c in CONDITIONS:
    d = counts[c]; n = d['n']; misses = d['misses']
    variant.append([c,n,d['lexical'],pct(d['lexical'],n),d['semantic'],
                    pct(d['semantic'],n),d['strict'],pct(d['strict'],n),
                    d['terms']/n if n else 0])
    recovery.append([c,misses,d['recovered'],d['strict_recovered'],
                     d['miss_scores'][1],d['miss_scores'][0],
                     pct(d['recovered'],misses),pct(d['strict_recovered'],misses)])
    scores_sheet.append([c,*[d['scores'][s] for s in range(4)],n])
for k in range(7):
    query_sheet.append([f'{k} of 6',lex_dist[k],sem_dist[k],
                        pct(lex_dist[k],evaluated),pct(sem_dist[k],evaluated)])
total_pairs = 6 * evaluated
total_lex = sum(counts[c]['lexical'] for c in CONDITIONS)
total_sem = sum(counts[c]['semantic'] for c in CONDITIONS)
total_strict = sum(counts[c]['strict'] for c in CONDITIONS)
total_misses = sum(counts[c]['misses'] for c in CONDITIONS)
total_recovered = sum(counts[c]['recovered'] for c in CONDITIONS)
mean_lex = sum(k*v for k,v in lex_dist.items())/evaluated if evaluated else 0
mean_sem = sum(k*v for k,v in sem_dist.items())/evaluated if evaluated else 0
summary_values = [
    ('Evaluated Queries',evaluated),('Incomplete Queries',incomplete),
    ('Total Query-Variant Pairs',total_pairs),('Exact Lexical Hits',total_lex),
    ('Semantic Hits GE2',total_sem),('Strict Semantic Hits EQ3',total_strict),
    ('Exact Lexical HR (%)',pct(total_lex,total_pairs)),
    ('Semantic HR GE2 (%)',pct(total_sem,total_pairs)),
    ('Strict Semantic HR EQ3 (%)',pct(total_strict,total_pairs)),
    ('Lexical Misses',total_misses),('Recovered Lexical Misses GE2',total_recovered),
    ('Semantic Recovery Rate (%)',pct(total_recovered,total_misses)),
    ('Queries With Any Lexical Success',lex_any),
    ('Queries With Any Semantic Success',sem_any),
    ('Canonical Semantic Success',canonical),('Any-Variant Semantic Success',any_variant),
    ('Semantic Variant Recovery',variant_recovered),
    ('Semantic Variant Recovery (%)',pct(variant_recovered,evaluated)),
    ('Mean Lexical Successful Variants',mean_lex),
    ('Mean Semantic Successful Variants',mean_sem),
    ('GPT Model',MODEL),('Prompt Version',PROMPT_VERSION),
    ('Semantic Threshold','Score >= 2'),('Strict Threshold','Score = 3'),
    ('Exact Lexical Hits Scored Deterministically','Yes')]
for entry in summary_values:
    summary.append(entry)
for sheet in (variant,recovery,scores_sheet,query_sheet,summary):
    sheet.freeze_panes = 'A2'
    sheet.auto_filter.ref = sheet.dimensions
    for cell in sheet[1]:
        cell.font = Font(bold=True,color='FFFFFF')
        cell.fill = PatternFill('solid',fgColor='17365D')
    for column in sheet.columns:
        letter = column[0].column_letter
        sheet.column_dimensions[letter].width = min(48,max(16,max(len(str(c.value or '')) for c in column)+2))
save_atomic(wb,OUTPUT_FILE)
print('\n' + '='*64)
print('CLQE SEMANTIC TRANSLATION COVERAGE — V2')
print(f'Evaluated: {evaluated} | Incomplete: {incomplete} | Newly processed: {processed}')
print(f'Queries requiring GPT this run: {api_queries}')
for c in CONDITIONS:
    d = counts[c]
    print(f'{c:7s} Lexical {pct(d["lexical"],d["n"]):6.2f}% | '
          f'Semantic>=2 {pct(d["semantic"],d["n"]):6.2f}% | '
          f'Strict=3 {pct(d["strict"],d["n"]):6.2f}% | '
          f'Recovered {d["recovered"]}')
print(f'Overall lexical HR: {pct(total_lex,total_pairs):.2f}%')
print(f'Overall semantic HR >=2: {pct(total_sem,total_pairs):.2f}%')
print(f'Overall strict semantic HR =3: {pct(total_strict,total_pairs):.2f}%')
print(f'Lexical misses recovered: {total_recovered}/{total_misses} '
      f'({pct(total_recovered,total_misses):.2f}%)')
print('Output:',OUTPUT_FILE)
print('EVALUATION AND ANALYSIS COMPLETED')
