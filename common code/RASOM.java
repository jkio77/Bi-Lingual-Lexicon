package BORUE;

import java.io.File;
import java.util.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
//............main RASOM implementation for reproducibility only

/** Self-contained RASOM matcher. Requires OWLAPI, but no Excel/POI library. */
public class RASOM {
    private static final String ONTOLOGY_PATH = "F:\\Eclipse\\EclipseHome\\BiOnt.owl";
    // This is the ontology vocabulary namespace, NOT a server connection.
    private static final String BASE_IRI = "http://localhost:3030/BiOnt.owl#";
    private static final double A = 1.0, B = 2.0, SURFACE_PENALTY_WEIGHT = 0.5;
    private static final String[] ALLOWED_VARIATIONS = {"I", "YT"};
    private static final Set<String> STOPWORDS = new HashSet<String>();
    static {
        String[] groups = {
        "a, aayi, ab, an, ao, asal, aur, bas, bus, chalo, dr, h",
        "ha, hae, hai, ham, he, hi, hn, ho, hue, hui, hum, is",
        "jab, jo, k, ka, kam, kar, kay, ke, ki, kiya, ko, kon",
        "kum, kya, mein, ne, oh, par, raha, s, sab, say, se, sey",
        "si, sub, sy, tak, tha, the, thi, to, tom, tu, tum, wo",
        "ya, ye"
        };
        for (String group : groups)
            for (String word : group.split(",\\s*")) STOPWORDS.add(word);
    }
    private final List<DictionaryEntry> dictionary;
    private final RomanMetaphone encoder = new RomanMetaphone();

    public RASOM() throws Exception {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        File source = new File(ONTOLOGY_PATH);
        if (!source.isFile()) throw new java.io.FileNotFoundException(source.toString());
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(source);
        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLDataProperty strVal = factory.getOWLDataProperty(IRI.create(BASE_IRI + "strVal"));
        OWLDataProperty romMeta = factory.getOWLDataProperty(IRI.create(BASE_IRI + "romMeta"));
        OWLClass languageIn = factory.getOWLClass(IRI.create(BASE_IRI + "Language_In"));
        Set<OWLClass> classes = new HashSet<OWLClass>();
        collectSubClasses(languageIn, ontology, classes);
        dictionary = new ArrayList<DictionaryEntry>();
        for (OWLNamedIndividual individual : ontology.getIndividualsInSignature()) {
            boolean eligible = false;
            for (OWLClassExpression type : EntitySearcher.getTypes(individual, ontology)) {
                if (type instanceof OWLClass && classes.contains(type.asOWLClass())) { eligible = true; break; }
            }
            if (!eligible) continue;
            List<String> surfaces = new ArrayList<String>();
            List<String> codes = new ArrayList<String>();
            EntitySearcher.getDataPropertyValues(individual, strVal, ontology)
                .forEach(v -> { if (!v.getLiteral().trim().isEmpty()) surfaces.add(v.getLiteral().trim()); });
            EntitySearcher.getDataPropertyValues(individual, romMeta, ontology)
                .forEach(v -> { if (!v.getLiteral().trim().isEmpty()) codes.add(v.getLiteral().trim()); });
            if (surfaces.isEmpty() || codes.isEmpty()) continue;
            DictionaryEntry entry = new DictionaryEntry();
            entry.individualIRI = individual.getIRI().toString();
            entry.strVal = surfaces.get(0);
            entry.romMeta = codes.get(0).toUpperCase(Locale.ROOT);
            dictionary.add(entry);
        }
        // Deterministic ordering in case of otherwise tied ranking fields.
        Collections.sort(dictionary, Comparator.comparing(d -> d.individualIRI));
    }

    /** Returns at most r ranked ontology individuals for a single- or multiword query. */
    public List<MatchResult> RASOM(String query, int r) {
        if (r <= 0) throw new IllegalArgumentException("maxcandidates must be positive");
        String cleaned = removeStopwords(query);
        if (cleaned.isEmpty()) return Collections.emptyList();
        String code = encodeSentence(cleaned);
        List<MatchResult> results = findTopMatches(cleaned, code, dictionary);
        return new ArrayList<MatchResult>(results.subList(0, Math.min(r, results.size())));
    }

    private static String removeStopwords(String query) {
        if (query == null) return "";
        String normalized = query.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ").trim();
        StringBuilder result = new StringBuilder();
        for (String token : normalized.split("\\s+")) {
            if (token.isEmpty() || STOPWORDS.contains(token)) continue;
            if (result.length() > 0) result.append(' ');
            result.append(token);
        }
        return result.toString();
    }

    private String encodeSentence(String value) {
        StringBuilder output = new StringBuilder();
        for (String word : value.split("\\s+")) {
            String code = encoder.encodeRoman(word);
            if (code == null || code.isEmpty()) continue;
            if (output.length() > 0) output.append(' ');
            output.append(code);
        }
        return output.toString();
    }

    public static void main(String[] args) {
        try {
            System.out.println("Loading BORUE ontology...");
            RASOM matcher = new RASOM();
            System.out.println("Loaded " + matcher.dictionary.size() + " ontology entries.");
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Please enter your query in format query, maxcandidates (Enter to end): ");
                if (!scanner.hasNextLine()) break;
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) break;
                int comma = line.lastIndexOf(',');
                if (comma < 1) { System.out.println("Format: hukumat ki zimadari, 5"); continue; }
                try {
                    String query = line.substring(0, comma).trim();
                    int count = Integer.parseInt(line.substring(comma + 1).trim());
                    if (query.isEmpty() || count < 1) { System.out.println("Query and positive maxcandidates required."); continue; }
                    String filtered = removeStopwords(query);
                    System.out.println("After stopwords: " + filtered);
                    System.out.println("Tokens: " + getWordList(filtered));
                    System.out.println("Encoded: " + matcher.encodeSentence(filtered));
                    List<MatchResult> matches = matcher.RASOM(query, count);
                    if (matches.isEmpty()) System.out.println("No eligible ontology matches.");
                    for (int i = 0; i < matches.size(); i++) {
                        MatchResult x = matches.get(i);
                        System.out.printf(Locale.ROOT, "%d. %s | romMeta=%s | tier=%s | score=%.6f | IRI=%s%n",
                            i + 1, x.strVal, x.romMeta, x.matchTier.description, x.similarityScore, x.individualIRI);
                    }
                } catch (NumberFormatException ex) {
                    System.out.println("maxcandidates must be an integer, e.g. 10.");
                }
                System.out.println();
            }
            System.out.println("RASOM terminated.");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

private static List<MatchResult> findTopMatches(

            String originalQuery,

            String encodedQuery,

            List<DictionaryEntry> dictionary) {



        List<String> queryPhoneticWords =

                getWordList(

                        encodedQuery

                );



        List<String> querySurfaceWords =

                getWordList(

                        originalQuery

                );



        List<MatchResult> results =

                new ArrayList<MatchResult>();



        /*

         * Compare against every dictionary entry.

         */

        for (DictionaryEntry entry :

                dictionary) {



            List<String> candidatePhoneticWords =

                    getWordList(

                            entry.romMeta

                    );



            /*

             * --------------------------------------------------------

             * STAGE 1

             *

             * Strict phonetic relevance filtering.

             * --------------------------------------------------------

             */

            MatchAnalysis analysis =

                    analyzeCandidate(

                            queryPhoneticWords,

                            candidatePhoneticWords

                    );



            /*

             * Reject candidate if there is no exact or explicitly

             * allowed phonetic match.

             */

            if (analysis.matchTier

                    ==

                MatchTier.REJECTED) {

                continue;

            }



            /*

             * --------------------------------------------------------

             * STAGE 2

             *

             * Calculate ranking metrics.

             * --------------------------------------------------------

             */

            double phraseMetric =

                    calculatePhraseMetric(

                            encodedQuery,

                            entry.romMeta

                    );



            double wordMetric =

                    calculateWordMetric(

                            queryPhoneticWords,

                            candidatePhoneticWords,

                            analysis

                    );



            /*

             * Surface spelling comparison.

             */

            double surfacePenalty =

                    calculateSurfacePenalty(

                            querySurfaceWords,

                            entry.strVal

                    );



            /*

             * Exact original spelling.

             */

            boolean exactSurfaceMatch =

                    normalizeSurfaceText(

                            originalQuery

                    )

                    .equals(

                            normalizeSurfaceText(

                                    entry.strVal

                            )

                    );



            /*

             * Final score.

             *

             * Lower score is better.

             */

            double similarityScore =

                    phraseMetric

                    +

                    wordMetric

                    +

                    (

                        SURFACE_PENALTY_WEIGHT

                        *

                        surfacePenalty

                    );



            MatchResult result =

                    new MatchResult();



            result.individualIRI =

                    entry.individualIRI;



            result.strVal =

                    entry.strVal;



            result.romMeta =

                    entry.romMeta;



            result.matchTier =

                    analysis.matchTier;



            result.exactWordCount =

                    analysis.exactWordCount;



            result.variationWordCount =

                    analysis.variationWordCount;



            result.alignmentWordCount =

                    analysis.alignmentWordCount;



            result.phraseMetric =

                    phraseMetric;



            result.wordMetric =

                    wordMetric;



            result.surfacePenalty =

                    surfacePenalty;



            result.exactSurfaceMatch =

                    exactSurfaceMatch;



            result.similarityScore =

                    similarityScore;



            results.add(

                    result

            );

        }



        /*

         * ============================================================

         * SORT

         * ============================================================

         */

        Collections.sort(

                results,

                new Comparator<MatchResult>() {



                    @Override

                    public int compare(

                            MatchResult first,

                            MatchResult second) {



                        /*

                         * 1. Exact phonetic match before variation.

                         */

                        int tierComparison =

                                Integer.compare(

                                        first.matchTier.rank,

                                        second.matchTier.rank

                                );



                        if (tierComparison != 0) {

                            return tierComparison;

                        }



                        /*

                         * 2. Exact surface spelling.

                         */

                        if (first.exactSurfaceMatch

                                !=

                            second.exactSurfaceMatch) {

                            return first.exactSurfaceMatch

                                    ?

                                    -1

                                    :

                                    1;

                        }



                        /*

                         * 3. Lower surface penalty.

                         */

                        int surfaceComparison =

                                Double.compare(

                                        first.surfacePenalty,

                                        second.surfacePenalty

                                );



                        if (surfaceComparison != 0) {

                            return surfaceComparison;

                        }



                        /*

                         * 4. More exact phonetic words.

                         */

                        int exactComparison =

                                Integer.compare(

                                        second.exactWordCount,

                                        first.exactWordCount

                                );



                        if (exactComparison != 0) {

                            return exactComparison;

                        }



                        /*

                         * 5. Fewer variation words.

                         */

                        int variationComparison =

                                Integer.compare(

                                        first.variationWordCount,

                                        second.variationWordCount

                                );



                        if (variationComparison != 0) {

                            return variationComparison;

                        }



                        /*

                         * 6. Lower PhraseMetric.

                         */

                        int phraseComparison =

                                Double.compare(

                                        first.phraseMetric,

                                        second.phraseMetric

                                );



                        if (phraseComparison != 0) {

                            return phraseComparison;

                        }



                        /*

                         * 7. Lower WordMetric.

                         */

                        int wordComparison =

                                Double.compare(

                                        first.wordMetric,

                                        second.wordMetric

                                );



                        if (wordComparison != 0) {

                            return wordComparison;

                        }



                        /*

                         * 8. Lower final score.

                         */

                        return Double.compare(

                                first.similarityScore,

                                second.similarityScore

                        );

                    }

                }

        );



        /*

         * Return TOP_K.

         */

        int resultCount =

                Math.min(

                        results.size(),

                        results.size()

                );



        return new ArrayList<MatchResult>(

                results.subList(

                        0,

                        resultCount

                )

        );

    }

private static MatchAnalysis analyzeCandidate(
            List<String> queryWords,
            List<String> candidateWords) {

        MatchAnalysis analysis = new MatchAnalysis();
        analysis.matchTier = MatchTier.REJECTED;
        analysis.exactWordCount = 0;
        analysis.alignmentWordCount = 0;
        analysis.variationWordCount = 0;
        analysis.matchedCandidateWordCount = 0;

        Set<Integer> usedCandidateIndexes = new HashSet<Integer>();
        Set<Integer> matchedQueryIndexes = new HashSet<Integer>();

        /* STEP 1A: exact complete single-word phonetic matches. */
        for (int q = 0; q < queryWords.size(); q++) {
            String queryWord = queryWords.get(q);
            for (int i = 0; i < candidateWords.size(); i++) {
                if (usedCandidateIndexes.contains(i)) continue;
                if (queryWord.equals(candidateWords.get(i))) {
                    usedCandidateIndexes.add(i);
                    matchedQueryIndexes.add(q);
                    analysis.exactWordCount++;
                    analysis.matchedCandidateWordCount++;
                    break;
                }
            }
        }

        /* STEP 1B: exact joined-adjacent match, e.g.
         * ZMADRI <-> ZMA DRI. */
        for (int q = 0; q < queryWords.size(); q++) {
            if (matchedQueryIndexes.contains(q)) continue;
            String queryWord = queryWords.get(q);
            boolean found = false;

            for (int startIndex = 0; startIndex < candidateWords.size() && !found; startIndex++) {
                if (usedCandidateIndexes.contains(startIndex)) continue;
                StringBuilder joined = new StringBuilder();

                for (int endIndex = startIndex; endIndex < candidateWords.size(); endIndex++) {
                    if (usedCandidateIndexes.contains(endIndex)) break;
                    joined.append(candidateWords.get(endIndex));

                    if (endIndex > startIndex && queryWord.equals(joined.toString())) {
                        for (int k = startIndex; k <= endIndex; k++) usedCandidateIndexes.add(k);
                        matchedQueryIndexes.add(q);
                        analysis.exactWordCount++;
                        analysis.matchedCandidateWordCount += endIndex - startIndex + 1;
                        found = true;
                        break;
                    }
                    if (joined.length() >= queryWord.length()) break;
                }
            }
        }

        /* STEP 1C: controlled single-word phonetic alignment.
         * Consonants remain hard anchors. */
        for (int q = 0; q < queryWords.size(); q++) {
            if (matchedQueryIndexes.contains(q)) continue;
            String queryWord = queryWords.get(q);

            for (int i = 0; i < candidateWords.size(); i++) {
                if (usedCandidateIndexes.contains(i)) continue;
                if (isControlledPhoneticAlignment(queryWord, candidateWords.get(i))) {
                    usedCandidateIndexes.add(i);
                    matchedQueryIndexes.add(q);
                    analysis.alignmentWordCount++;
                    analysis.matchedCandidateWordCount++;
                    break;
                }
            }
        }

        /* STEP 1D: joined-adjacent alignment with duplicate collapse at
         * the candidate word boundary. Example:
         * YDSHT <-> YD DSHT -> YDDSHT -> YDSHT. */
        for (int q = 0; q < queryWords.size(); q++) {
            if (matchedQueryIndexes.contains(q)) continue;
            String queryWord = queryWords.get(q);
            boolean found = false;

            for (int startIndex = 0; startIndex < candidateWords.size() && !found; startIndex++) {
                if (usedCandidateIndexes.contains(startIndex)) continue;
                String joined = candidateWords.get(startIndex);

                for (int endIndex = startIndex + 1; endIndex < candidateWords.size(); endIndex++) {
                    if (usedCandidateIndexes.contains(endIndex)) break;
                    joined = appendWithBoundaryDuplicateCollapsed(joined, candidateWords.get(endIndex));

                    if (queryWord.equals(joined) || isControlledPhoneticAlignment(queryWord, joined)) {
                        for (int k = startIndex; k <= endIndex; k++) usedCandidateIndexes.add(k);
                        matchedQueryIndexes.add(q);
                        analysis.alignmentWordCount++;
                        analysis.matchedCandidateWordCount += endIndex - startIndex + 1;
                        found = true;
                        break;
                    }
                }
            }
        }

        /* STEP 2: explicitly allowed suffix variations. */
        for (int q = 0; q < queryWords.size(); q++) {
            if (matchedQueryIndexes.contains(q)) continue;
            String queryWord = queryWords.get(q);

            for (int i = 0; i < candidateWords.size(); i++) {
                if (usedCandidateIndexes.contains(i)) continue;
                if (isAllowedVariation(queryWord, candidateWords.get(i))) {
                    usedCandidateIndexes.add(i);
                    matchedQueryIndexes.add(q);
                    analysis.variationWordCount++;
                    analysis.matchedCandidateWordCount++;
                    break;
                }
            }
        }

        if (analysis.exactWordCount > 0) {
            analysis.matchTier = MatchTier.EXACT;
        }
        else if (analysis.alignmentWordCount > 0) {
            analysis.matchTier = MatchTier.CONTROLLED_ALIGNMENT;
        }
        else if (analysis.variationWordCount > 0) {
            analysis.matchTier = MatchTier.ALLOWED_VARIATION;
        }
        else {
            analysis.matchTier = MatchTier.REJECTED;
        }

        return analysis;
    }

private static boolean isControlledPhoneticAlignment(String first, String second) {
        if (first == null || second == null) return false;
        first = first.trim().toUpperCase();
        second = second.trim().toUpperCase();
        if (first.isEmpty() || second.isEmpty() || first.equals(second)) return false;

        /* Special terminal A <-> N: complete preceding sequence must match. */
        if (first.length() == second.length() && first.length() > 1) {
            String p1 = first.substring(0, first.length() - 1);
            String p2 = second.substring(0, second.length() - 1);
            char c1 = first.charAt(first.length() - 1);
            char c2 = second.charAt(second.length() - 1);
            if (p1.equals(p2) && ((c1 == 'A' && c2 == 'N') || (c1 == 'N' && c2 == 'A'))) {
                return true;
            }
        }

        /* Hard consonant anchor rule. */
        if (!consonantSkeleton(first).equals(consonantSkeleton(second))) return false;

        /* Same length: differences must be vowel-for-vowel substitutions. */
        if (first.length() == second.length()) {
            for (int i = 0; i < first.length(); i++) {
                char a = first.charAt(i), b = second.charAt(i);
                if (a != b && !(isPhoneticVowel(a) && isPhoneticVowel(b))) return false;
            }
            return true;
        }

        /* Y is a soft vowel-like marker and may be inserted/deleted internally. */
        if (removeCharacter(first, 'Y').equals(removeCharacter(second, 'Y'))) return true;

        /* One terminal vowel may be inserted/deleted. */
        if (Math.abs(first.length() - second.length()) == 1) {
            String longer = first.length() > second.length() ? first : second;
            String shorter = first.length() > second.length() ? second : first;
            if (isPhoneticVowel(longer.charAt(longer.length() - 1))
                    && longer.substring(0, longer.length() - 1).equals(shorter)) return true;
        }

        /* Internal non-Y vowel insertion/deletion is allowed only when both
         * complete codes retain a vowel ending. This accepts DUNU/DNU but
         * rejects the counterexample ZUHR/ZHR. */
        if (Math.abs(first.length() - second.length()) == 1
                && isPhoneticVowel(first.charAt(first.length() - 1))
                && isPhoneticVowel(second.charAt(second.length() - 1))) {
            return differsBySingleVowelInsertionDeletion(first, second);
        }

        return false;
    }

private static boolean differsBySingleVowelInsertionDeletion(String first, String second) {
        String longer = first.length() > second.length() ? first : second;
        String shorter = first.length() > second.length() ? second : first;
        if (longer.length() != shorter.length() + 1) return false;

        for (int i = 0; i < longer.length(); i++) {
            if (!isPhoneticVowel(longer.charAt(i))) continue;
            String removed = longer.substring(0, i) + longer.substring(i + 1);
            if (removed.equals(shorter)) return true;
        }
        return false;
    }

private static String consonantSkeleton(String value) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!isPhoneticVowel(c)) result.append(c);
        }
        return result.toString();
    }

private static boolean isPhoneticVowel(char c) {
        return c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U' || c == 'Y';
    }

private static String removeCharacter(String value, char character) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != character) result.append(value.charAt(i));
        }
        return result.toString();
    }

private static String appendWithBoundaryDuplicateCollapsed(String left, String right) {
        if (left == null || left.isEmpty()) return right == null ? "" : right;
        if (right == null || right.isEmpty()) return left;
        if (left.charAt(left.length() - 1) == right.charAt(0)) {
            return left + right.substring(1);
        }
        return left + right;
    }

private static boolean isAllowedVariation(

            String queryWord,

            String candidateWord) {



        if (queryWord == null

                ||

            candidateWord == null) {

            return false;

        }



        queryWord =

                queryWord

                        .trim()

                        .toUpperCase();



        candidateWord =

                candidateWord

                        .trim()

                        .toUpperCase();



        /*

         * Candidate must be longer.

         */

        if (candidateWord.length()

                <=

            queryWord.length()) {

            return false;

        }



        /*

         * Candidate must start with complete query code.

         */

        if (!candidateWord.startsWith(

                queryWord

        )) {

            return false;

        }



        /*

         * Get remaining suffix.

         */

        String suffix =

                candidateWord.substring(

                        queryWord.length()

                );



        /*

         * Only explicitly allowed suffixes are accepted.

         */

        for (String allowedSuffix :

                ALLOWED_VARIATIONS) {



            if (suffix.equals(

                    allowedSuffix

                            .trim()

                            .toUpperCase()

            )) {



                return true;

            }

        }



        return false;

    }

private static double calculateSurfacePenalty(

            List<String> queryWords,

            String candidateText) {



        List<String> candidateWords =

                getWordList(

                        candidateText

                );



        if (queryWords.isEmpty()

                &&

            candidateWords.isEmpty()) {

            return 0.0;

        }



        if (queryWords.isEmpty()

                ||

            candidateWords.isEmpty()) {

            return 1.0;

        }



        /*

         * Single-word case.

         */

        if (queryWords.size() == 1

                &&

            candidateWords.size() == 1) {



            return normalizedLevenshteinDistance(

                    queryWords.get(0),

                    candidateWords.get(0)

            );

        }



        /*

         * Multi-word case.

         */

        double totalPenalty =

                0.0;



        int matchedQueryWords =

                0;



        Set<Integer> usedCandidateIndexes =

                new HashSet<Integer>();



        for (String queryWord :

                queryWords) {



            double bestPenalty =

                    Double.MAX_VALUE;



            int bestIndex =

                    -1;



            for (int i = 0;

                 i < candidateWords.size();

                 i++) {



                if (usedCandidateIndexes.contains(

                        i

                )) {

                    continue;

                }



                double penalty =

                        normalizedLevenshteinDistance(

                                queryWord,

                                candidateWords.get(i)

                        );



                if (penalty < bestPenalty) {



                    bestPenalty =

                            penalty;



                    bestIndex =

                            i;

                }

            }



            if (bestIndex >= 0) {



                usedCandidateIndexes.add(

                        bestIndex

                );



                totalPenalty +=

                        bestPenalty;



                matchedQueryWords++;

            }

        }



        if (matchedQueryWords == 0) {

            return 1.0;

        }



        double averagePenalty =

                totalPenalty

                /

                queryWords.size();



        int missingWords =

                queryWords.size()

                -

                matchedQueryWords;



        double missingPenalty =

                (

                    (double) missingWords

                    /

                    queryWords.size()

                );



        int extraWords =

                Math.max(

                        0,

                        candidateWords.size()

                        -

                        matchedQueryWords

                );



        double extraPenalty =

                (

                    (double) extraWords

                    /

                    candidateWords.size()

                )

                *

                0.25;



        return Math.min(

                1.0,

                averagePenalty

                +

                missingPenalty

                +

                extraPenalty

        );

    }

private static double normalizedLevenshteinDistance(

            String first,

            String second) {



        first =

                normalizeSurfaceText(

                        first

                );



        second =

                normalizeSurfaceText(

                        second

                );



        if (first.equals(

                second

        )) {

            return 0.0;

        }



        int maxLength =

                Math.max(

                        first.length(),

                        second.length()

                );



        if (maxLength == 0) {

            return 0.0;

        }



        int distance =

                levenshteinDistance(

                        first,

                        second

                );



        return

                (double) distance

                /

                maxLength;

    }

private static int levenshteinDistance(

            String first,

            String second) {



        int firstLength =

                first.length();



        int secondLength =

                second.length();



        int[][] matrix =

                new int[

                        firstLength + 1

                ][

                        secondLength + 1

                ];



        for (int i = 0;

             i <= firstLength;

             i++) {

            matrix[i][0] =

                    i;

        }



        for (int j = 0;

             j <= secondLength;

             j++) {

            matrix[0][j] =

                    j;

        }



        for (int i = 1;

             i <= firstLength;

             i++) {



            for (int j = 1;

                 j <= secondLength;

                 j++) {



                int substitutionCost =

                        first.charAt(i - 1)

                                ==

                        second.charAt(j - 1)

                                ?

                        0

                        :

                        1;



                matrix[i][j] =

                        Math.min(

                                Math.min(

                                        matrix[i - 1][j]

                                        +

                                        1,

                                        matrix[i][j - 1]

                                        +

                                        1

                                ),

                                matrix[i - 1][j - 1]

                                +

                                substitutionCost

                        );

            }

        }



        return matrix[

                firstLength

        ][

                secondLength

        ];

    }

private static double calculatePhraseMetric(

            String query,

            String candidate) {



        if (query == null) {

            query = "";

        }



        if (candidate == null) {

            candidate = "";

        }



        query =

                query

                        .trim()

                        .toUpperCase();



        candidate =

                candidate

                        .trim()

                        .toUpperCase();



        if (query.equals(

                candidate

        )) {

            return 0.0;

        }



        if (query.isEmpty()

                ||

            candidate.isEmpty()) {

            return 1.0;

        }



        String[] queryWords =

                query.split(

                        "\\s+"

                );



        String[] candidateWords =

                candidate.split(

                        "\\s+"

                );



        int commonWordCount =

                Math.min(

                        queryWords.length,

                        candidateWords.length

                );



        double totalMetric =

                0.0;



        /*

         * Compare corresponding word positions.

         */

        for (int i = 0;

             i < commonWordCount;

             i++) {



            totalMetric +=

                    calculatePhoneticWordMetric(

                            queryWords[i],

                            candidateWords[i]

                    );

        }



        /*

         * Penalize unmatched words.

         */

        int unmatchedWords =

                Math.abs(

                        queryWords.length

                        -

                        candidateWords.length

                );



        totalMetric +=

                unmatchedWords;



        /*

         * Normalize.

         */

        int totalWordCount =

                Math.max(

                        queryWords.length,

                        candidateWords.length

                );



        if (totalWordCount == 0) {

            return 0.0;

        }



        return

                totalMetric

                /

                totalWordCount;

    }

private static double calculatePhoneticWordMetric(

            String queryWord,

            String candidateWord) {



        if (queryWord == null) {

            queryWord = "";

        }



        if (candidateWord == null) {

            candidateWord = "";

        }



        queryWord =

                queryWord

                        .trim()

                        .toUpperCase();



        candidateWord =

                candidateWord

                        .trim()

                        .toUpperCase();



        if (queryWord.equals(

                candidateWord

        )) {

            return 0.0;

        }



        if (queryWord.isEmpty()

                ||

            candidateWord.isEmpty()) {

            return 1.0;

        }



        int editDistance =

                levenshteinDistance(

                        queryWord,

                        candidateWord

                );



        int maximumLength =

                Math.max(

                        queryWord.length(),

                        candidateWord.length()

                );



        double normalizedLevenshtein =

                (double) editDistance

                /

                maximumLength;



        int commonPrefixLength =

                longestCommonPrefixLength(

                        queryWord,

                        candidateWord

                );



        double prefixSimilarity =

                (double) commonPrefixLength

                /

                maximumLength;



        double prefixPenalty =

                1.0

                -

                prefixSimilarity;



        return

                (

                    0.4

                    *

                    normalizedLevenshtein

                )

                +

                (

                    0.6

                    *

                    prefixPenalty

                );

    }

private static int longestCommonPrefixLength(

            String first,

            String second) {



        int maximum =

                Math.min(

                        first.length(),

                        second.length()

                );



        int count =

                0;



        for (int i = 0;

             i < maximum;

             i++) {



            if (first.charAt(i)

                    ==

                second.charAt(i)) {



                count++;

            }

            else {

                break;

            }

        }



        return count;

    }

private static double calculateWordMetric(

            List<String> queryWords,

            List<String> candidateWords,

            MatchAnalysis analysis) {



        if (queryWords.isEmpty()) {

            return 0.0;

        }



        if (candidateWords.isEmpty()) {

            return B;

        }



        int matchedWords =

                analysis.exactWordCount

                +

                analysis.alignmentWordCount

                +

                analysis.variationWordCount;



        int matchedCandidateWords =

                analysis.matchedCandidateWordCount;



        /*

         * Extra candidate words.

         */

        int extraElements =

                Math.max(

                        0,

                        candidateWords.size()

                        -

                        matchedCandidateWords

                );



        /*

         * Missing query words.

         */

        int missingElements =

                Math.max(

                        0,

                        queryWords.size()

                        -

                        matchedWords

                );



        double extraPenalty =

                A

                *

                (

                    (double) extraElements

                    /

                    candidateWords.size()

                );



        double missingPenalty =

                B

                *

                (

                    (double) missingElements

                    /

                    queryWords.size()

                );



        return

                extraPenalty

                +

                missingPenalty;

    }

private static List<String> getWordList(

            String value) {



        List<String> words =

                new ArrayList<String>();



        if (value == null

                ||

            value.trim().isEmpty()) {

            return words;

        }



        String[] parts =

                value

                        .trim()

                        .toUpperCase()

                        .split(

                                "\\s+"

                        );



        for (String part :

                parts) {



            if (part != null

                    &&

                !part.trim().isEmpty()) {



                words.add(

                        part.trim()

                );

            }

        }



        return words;

    }

private static String normalizeSurfaceText(

            String value) {



        if (value == null) {

            return "";

        }



        return value

                .trim()

                .toLowerCase()

                .replaceAll(

                        "\\s+",

                        " "

                );

    }

private static void collectSubClasses(

            OWLClass parent,

            OWLOntology ontology,

            Set<OWLClass> result) {



        if (result.contains(

                parent

        )) {

            return;

        }



        result.add(

                parent

        );



        for (org.semanticweb.owlapi.model.OWLSubClassOfAxiom axiom :

                ontology.getSubClassAxiomsForSuperClass(

                        parent

                )) {



            OWLClassExpression child =

                    axiom.getSubClass();



            if (child instanceof OWLClass) {



                collectSubClasses(

                        child.asOWLClass(),

                        ontology,

                        result

                );

            }

        }

    }
    private enum MatchTier {



        EXACT(

                0,

                "EXACT COMPLETE CODE MATCH"

        ),



        CONTROLLED_ALIGNMENT(
                1,

                "CONTROLLED PHONETIC ALIGNMENT"

        ),



        ALLOWED_VARIATION(

                2,

                "ALLOWED SUFFIX VARIATION"

        ),



        REJECTED(

                3,

                "REJECTED"

        );



        final int rank;



        final String description;



        MatchTier(

                int rank,

                String description) {



            this.rank =

                    rank;



            this.description =

                    description;

        }

    }



    /*

     * ================================================================

     * MATCH ANALYSIS

     * ================================================================

     */

    private static class MatchAnalysis {



        MatchTier matchTier;



        int exactWordCount;



        int variationWordCount;



        int alignmentWordCount;



        int matchedCandidateWordCount;

    }



    /*

     * ================================================================

     * DICTIONARY ENTRY

     * ================================================================

     */

    private static class DictionaryEntry {



        String individualIRI;



        String strVal;



        String romMeta;

    }



    /*

     * ================================================================

     * MATCH RESULT

     * ================================================================

     */

    public static class MatchResult {



        String individualIRI;



        String strVal;



        String romMeta;



        MatchTier matchTier;



        int exactWordCount;



        int variationWordCount;



        int alignmentWordCount;



        boolean exactSurfaceMatch;



        double surfacePenalty;



        double phraseMetric;



        double wordMetric;



        double similarityScore;
    }
    private static class RomanMetaphone {

//....backbone of fuzzy string matching RUMETA
	//.....Ruman Urdu MEtaphone Algorithm
	

		  private static final String VOWELS = "AEIOU";
		  String encodeString = "";	
		  int index = 0;
		  
	    	  
		  public RomanMetaphone(){}  // default constructor
		  
		  public boolean isRomanMetaphoneEqual(String v1, String v2)
		  {
			  
			  String code1;
			  String code2;
			
			  //preprocessing.......................................
		//    Source: http://www.javaforschool.com/788673-program-to-remove-duplicate-characters-from-a-word/#ixzz3MYzaIs5x
			  //.....................................................
			  
			  code1 = encodeRoman(v1);
              code2 = encodeRoman(v2); 
			  
			  if (code1.equalsIgnoreCase(code2))
				  return true;
			  else
				  return false;
		  }
	    
		  
		  public String encodeRoman(String value)
		   {
			  value = cleanInput(value);
			  value = removeConsecDups(value);
			  value  = replaceEmbeddedDigits(value);
			  if (value == null) 
			  return null;
	          	   
			  index = 0; // resetting index value for successive calls
		      this.encodeString = ""; // resetting encode string value for successive calls
			  
		   
			  while ( index <= value.length() - 1) 
			    {
			      switch (value.charAt(index))
		          {
		          case 'A': 
		          case 'E': 
		          case 'I': 
		          case 'O': 
		          case 'U': 
			   
		        	  index = handleAEIOUY(value,index);
		                 break;
		      //BDFGJLMNPRY
		          case 'B': 
		              this.encodeString = this.encodeString + 'B';
		        	      ++index;
		        	      break;
		        	      //this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'B') ? this.encodeString : this.encodeString + 'B';
		        	      //	index = charAt(value, index + 1) == 'B' ? index + 2 : index + 1;
		          case 'D': 
			    	   this.encodeString += 'D' ;
			    	   ++index;
			    	   break;
			    	   //this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'D') ? this.encodeString : this.encodeString + 'D';
			    	   //index = charAt(value, index + 1) == 'D' ? index + 2 : index + 1;
			       case 'F': 
			    	   this.encodeString  += 'F';
			    	   ++index;
			    	   break;
			    	   //this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'F') ? this.encodeString : this.encodeString + 'F';
			    	   //index = charAt(value, index + 1) == 'F' ? index + 2 : index + 1;
			    	   case 'G': 
			    	   this.encodeString = this.encodeString + 'G';
			    	   ++index; 
			    	   break;
			    	   //this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'G') ? this.encodeString : this.encodeString + 'G';
			    	   //index = charAt(value, index + 1) == 'G' ? index + 2 : index + 1;
			    	   case 'J': 
				    	   this.encodeString  += 'J';
				    	   ++index;
				    	   break;
				    	   //this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'J') ? this.encodeString : this.encodeString + 'J';
				    	   //index = charAt(value, index + 1) == 'J' ? index + 2 : index + 1;
			    		case 'L': 
				       		this.encodeString += 'L';
					        ++index;
					        break;
				       		//this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'L') ? this.encodeString : this.encodeString + 'L';
				       		//index = charAt(value, index + 1) == 'L' ? index + 2 : index + 1;
					        
				       case 'M': 
				    	   this.encodeString += 'M';
				    	   ++index;
				    	   break;
				    	   //this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'M') ? this.encodeString : this.encodeString + 'M';
				    	   //index = charAt(value, index + 1) == 'M' ? index + 2 : index + 1;
				       case 'N': 
				    	   this.encodeString += 'N';
				    	   ++index;
				    	   break;
				    	   // this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'N') ? this.encodeString : this.encodeString + 'N';
				    	   //index = charAt(value, index + 1) == 'N' ? index + 2 : index + 1;
					   
				       case 'P': 
				    	   this.encodeString += 'P';
				    	   ++index;
				    	   break;
				    	   //this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'P') ? this.encodeString : this.encodeString + 'P'; 
				    	   //index = charAt(value, index + 1) == 'P' ? index + 2 : index + 1;

				       case 'Y': 
					        this.encodeString  += 'Y';
					        //index = charAt(value, index + 1) == 'Y' ? index + 2 : index + 1;
					        ++index;
					        break;
				       case 'R': 
					    	  this.encodeString += 'R';
					    	  //this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'R') ? this.encodeString : this.encodeString + 'R';
					    	  //index = charAt(value, index + 1) == 'R' ? index + 2 : index + 1;
				            ++index;
					    	  break;
					      case 'S': 
					    	  //this.encodeString =this.encodeString + 'S';
					    	 // this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'S') ? this.encodeString : this.encodeString + 'S';     
					    	  this.encodeString += 'S';
					    	 // index = charAt(value, index + 1) == 'S' ? index + 2 : index + 1;
					      ++index; 
					    	  break;
					       case 'T': 
					    	   this.encodeString += 'T';
					    	   //this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'T') ? this.encodeString : this.encodeString + 'T'; 
					    	   //index = charAt(value, index + 1) == 'T' ? index + 2 : index + 1;
					       ++index;
					    	   break;
				       
					       
				   case 'C': 
			      	   handleC(value, index);
		               break;
			  	   case 'H': 
			    	       handleH(value,index);
			    	       break;
			       case 'K': 
			    	   		this.encodeString += 'K';
			    	   		index = charAt(value, index + 1) == 'Q' ? index + 2 : index + 1;
			    	   		break;
			    	         //this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'K') ? this.encodeString : this.encodeString + 'K';
			    	         //index = charAt(value, index + 1) == 'K' ? index + 2 : index + 1;
			        case 'Q': 
			    	  	     this.encodeString += 'K';
         			    	 index = charAt(value, index + 1) == 'K' ? index + 2 : index + 1;
         			    	 break;
         			    	 //	this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'K') ? this.encodeString : this.encodeString + 'K'; 
			    	    case 'V': 
			    	    			this.encodeString = this.encodeString + 'W';
			    	    			//this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'W') ? this.encodeString : this.encodeString + 'W';
			    	    			index = (charAt(value, index + 1) == 'W' ) ? index + 2 : index + 1;
			    	    			break;
			    	    case 'W': 
			    	    			this.encodeString +=  'W';
			    	    			//this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'W') ? this.encodeString : this.encodeString + 'W';
			    	    			index = (charAt(value, index + 1) == 'V' ) ? index + 2 : index + 1;
			    	    			break;
			    	    case 'X': 
			    	    			this.encodeString  += 'Z';
			    	    			index = (charAt(value, index + 1) == 'Z' ) ? index + 2 : index + 1;
			    	    			break;
			         case 'Z': 
			        	 		this.encodeString += 'Z';
			        	 		//this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'Z') ? this.encodeString : this.encodeString + 'Z';
			        	 		index = (charAt(value, index + 1) == 'X' ) ? index + 2 : index + 1;
			        	 		break;
			       default: ++index;
			        
		        } //switch end
		      } //while end
		    
		    
		    
		    return this.encodeString;
		    
		   } // encode end
		   
		  private int handleAEIOUY(String value, int i)
		   {
		      
			  //.................CASE 1. WE ARE AT THE FIRST CHARACTER..............................................................................
			   
			  if (i == 0) 
		        {
		             if (charAt(value,i) == 'A' || charAt(value,i) == 'E' || charAt(value,i) == 'I')
		             {
			        	  this.encodeString = this.encodeString + 'A';
		              i = (charAt(value, i + 1) == 'A' || charAt(value, i + 1) == 'E' || charAt(value, i + 1) == 'I') ? i + 2 : i + 1;
		              return i;
		             }
		             
		             if (charAt(value, i) == 'U' || charAt(value, i) == 'O')
				          { 
		            	          this.encodeString = this.encodeString + 'U';
					          i = (charAt(value, i + 1) == 'U' || charAt(value, i + 1) == 'O') ? i + 2 : i + 1;
					          return i;
				          }
		                   
		        } 
			   
			  //.............CASE 2. WE ARE AT THE LAST CHARACTER..................................................................................
			   if (i == value.length() - 1 ) // we are at the last character 
			   {
				      if (charAt(value,i) == 'A')
				      {
				    	  this.encodeString = this.encodeString + 'A';
				          i = i + 1;
				          return i;
				      }
				      
				      if (charAt(value,i) == 'E' || charAt(value, i) == 'I')
				      {
				    	  this.encodeString = this.encodeString + 'I';
				          i = i + 1;
				          return i;
				      }
				      if (charAt(value,i) == 'O' || charAt(value, i) == 'U')
				      {
				    	  this.encodeString = this.encodeString + 'U';
				          i = i + 1;
				          return i;
				      }
			   }
				
			   //...........IF WE HAVE REACHED HERE THAT MEANS WE ARE ANY WHERE FROM 2ND TO 2ND LAST CHARACTER
			   
			    // we are anywhere from 2nd character to 2nd last. here previous and next character matters
			    	      
			 	  
				   //..........applies to any place.....CASE 3 2ND TO 2ND LAST...............................
			   //System.out.println("stuck here" + value);
			   	 //  System.out.println(i+1);
				   //System.out.println(value.length());
				   
				   if (i + 1 < value.length()) 
				      {
					    switch (value.substring(i, i + 2)) 
					    {
					        case "AI":
					        case "AY":
					        case "AE":
					        case "EY":
					        case "IY":
					        case "IA":
					        	this.encodeString += 'Y';  
								   i=i+2;
								   return i;
					      
					        		   
					     /*
					        case "AH":
					        	this.encodeString += 'A';
					        	i=i+2;
					        	return i;
					        	*/
					        	}
					
					    if (charAt(value,i) == 'O' || charAt(value, i) == 'U')
					      {
					    	  this.encodeString = this.encodeString + 'U';
					          i = i + 1;
					          return i;
					      }
				      
				      }
				  //...................2nd last character...............................................
				   if (i == value.length() - 2)    
				   {
					   if ((charAt(value, i) == 'A' || charAt(value, i) == 'U') && "AEIOU".indexOf(value.charAt(i+1)) == -1)
					      {
						   i+=1; // igonore A and U
						   return i;
					      }
					   
					   switch (charAt(value, i)) 
					    {
					        case 'U':
					         	this.encodeString += 'U';
				    	     		i = charAt(value, index + 1) == 'O' ? index + 2 : index + 1;
				    	     		return i;
				    	     		
					        case 'O':
					         	this.encodeString += 'U';
				    	   	     	i = charAt(value, index + 1) == 'U' ? index + 2 : index + 1;
				    	   	        return i;
				    	   		 
					    }
				    
		    	       }
				  
			   return ++i;  // means its A,E,I inside the word and we ignore
		   }
		  
		  
		  //............................................................................... 
		  private void handleC(String value,int i)
		   {

		     if (contains(value, i, 2, "CH") && !(contains(value, i-2, 2, "CH")))
	             {
						 this.encodeString = this.encodeString + "CH";
						 this.index = this.index + 2;
						 return ;
		         }
		     if (contains(value, i, 2, "CH") && (contains(value, i-2, 2, "CH")))
             {
					 this.index = this.index + 2;
					 return ;
	         }
		     else
			    {
				//this.encodeString = this.encodeString + 'K';
				this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'K') ? this.encodeString : this.encodeString + 'K';
				this.index = this.index + 1;
				}
			    }
		   
		  private void handleH(String value, int i)
		   {
			         
		     if (i == 0) // we are at the first character 
		     {
	       	 	  this.encodeString = this.encodeString + 'H';
	       	 	  this.index = this.index +1;
	       	    if (charAt(value, i + 1) == 'U' || charAt(value, i + 1) == 'A' || charAt(value, i + 1) == 'O')
	       	 	   this.index = this.index + 1;  
	       	    	return ; 	  
		     }
		      
			  if (charAt(value, i - 1) != 'K' &&  charAt(value, i - 1) != 'G' && i != value.length() - 1)
			  {
				  	this.encodeString = this.encodeString + 'H';
			  }
			        
			          this.index = this.index +1;
		      }
		  
		  //........................................................................................................
		  
		  private String cleanInput(String input)
		  {
		    if (input == null) 
		     {
		      return null;
		     }
		    
		    input = input.trim();
		     if (input.length() == 0) 
		     {
		       return null;
		     }
		     return input.toUpperCase();
		 
		  }
		   //.......................................................
		   public static String removeConsecDups(String str) {
			   
            
            
               // 
               /// 
            
			   if (str == null || str.isEmpty()) 
			   { return ""; } 
			// Normalize common Unicode whitespace characters
			   str = str .replace('\u00A0', ' ') // Non-breaking space  
			    .replace('\u2007', ' ') // Figure space  
			    .replace('\u202F', ' ');  // Narrow no-break space  
			    
			   // Normalize multiple spaces into one 
			   str = str.replaceAll("\\s+", " ").trim(); 
			   // Convert to uppercase 
			   str = str.toUpperCase(); 
			   // previous logic
			   
			   if (str == null || str.length() == 0) {
			        return str;
			    }

			   
			    //................................
			    
			    StringBuilder result = new StringBuilder();
			    result.append(str.charAt(0));

			    for (int i = 1; i < str.length(); i++) {
			        if (str.charAt(i) != str.charAt(i - 1)) {
			            result.append(str.charAt(i));
			        }
			    }

			    return result.toString();
			
			
				/*  
               if (str == null || str.isEmpty()) {
				        return "";
				    }

				    // Normalize Unicode whitespace
				    str = str.replace('\u00A0', ' ')
				             .replace('\u2007', ' ')
				             .replace('\u202F', ' ');

				    // Normalize spaces and convert to uppercase
				    str = str.replaceAll("\\s+", " ")
				             .trim()
				             .toUpperCase(java.util.Locale.ROOT);

				    // Collapse repeated CH and GH sequences
				    str = str.replaceAll("(?:CH){2,}", "CH");
				    str = str.replaceAll("(?:GH){2,}", "GH");

				    // Remove consecutive duplicate letters only
				    // Repeated digits remain unchanged
				    StringBuilder result = new StringBuilder();

				    for (int i = 0; i < str.length(); i++) {

				        char current = str.charAt(i);

				        if (result.length() > 0) {

				            char previous = result.charAt(result.length() - 1);

				            if (Character.isLetter(current)
				                    && current == previous) {
				                continue;
				            }
				        }

				        result.append(current);
				    }

				    return result.toString();
				*/
				}
			   
			
		   //.......................................................................................
		   protected char charAt(String value, int index)
		  {
		    if ((index < 0) || (index >= value.length())) {
		       return '\000';
		     }
		     return value.charAt(index);
		   }
		   
		   private static boolean contains(String value, int start, int length, String criteria)
		   {
		     return contains(value, start, length, new String[] { criteria });
		   }
		   
		   private static boolean contains(String value, int start, int length, String criteria1, String criteria2)
		  {
		     return contains(value, start, length, new String[] { criteria1, criteria2 });
		   }
		   
		  private static boolean contains(String value, int start, int length, String criteria1, String criteria2, String criteria3)
		   {
	     return contains(value, start, length, new String[] { criteria1, criteria2, criteria3 });
		   }
		   
		   private static boolean contains(String value, int start, int length, String criteria1, String criteria2, String criteria3, String criteria4)
	  {
		    return contains(value, start, length, new String[] { criteria1, criteria2, criteria3, criteria4 });
	   }
	  
		   private static boolean contains(String value, int start, int length, String criteria1, String criteria2, String criteria3, String criteria4, String criteria5)
		  {
		    return contains(value, start, length, new String[] { criteria1, criteria2, criteria3, criteria4, criteria5 });
		   }
		  
		   private static boolean contains(String value, int start, int length, String criteria1, String criteria2, String criteria3, String criteria4, String criteria5, String criteria6)
	   {
	    return contains(value, start, length, new String[] { criteria1, criteria2, criteria3, criteria4, criteria5, criteria6 });
	   }
		  
		   protected static boolean contains(String value, int start, int length, String[] criteria)
		   {
		    boolean result = false;
		     if ((start >= 0) && (start + length <= value.length()))
		    {
		    String target = value.substring(start, start + length);
	      for (int i = 0; i < criteria.length; i++) {
		        if (target.equals(criteria[i]))
		        {
		          result = true;
	          break;
		         }
		       }
		     }
		     return result;
		   }
//..................new function added................
		   private static String replaceEmbeddedDigits(String input) {

			    if (input == null || input.isEmpty()) {
			        return input;
			    }

			    /*
			     * =========================================================
			     * NUMERIC ROMAN-URDU RULES
			     * =========================================================
			     *
			     * Add new rules here only.
			     *
			     * IMPORTANT:
			     * Longer numbers are automatically checked before
			     * shorter numbers.
			     */
			    java.util.Map<String, String> rules =
			            new java.util.HashMap<String, String>();

			    rules.put("1",  "aik");
			    rules.put("2",  "do");
			    rules.put("3",  "ten");
			    rules.put("4",  "char");
			    rules.put("5",  "panch");
			    rules.put("6",  "ch");
			    rules.put("7",  "sat");
			    rules.put("8",  "at");
			    rules.put("9",  "no");

			    rules.put("10", "dus");
			    rules.put("20", "bes");
			    rules.put("30", "tes");


			    /*
			     * Sort numeric rules by length.
			     *
			     * Example:
			     * 20 must be tested before 2.
			     * 100 would be tested before 10 and 1.
			     */
			    java.util.List<String> numbers =
			            new java.util.ArrayList<String>(rules.keySet());

			    java.util.Collections.sort(
			            numbers,
			            new java.util.Comparator<String>() {

			                @Override
			                public int compare(String a, String b) {

			                    return Integer.compare(
			                            b.length(),
			                            a.length()
			                    );
			                }
			            }
			    );


			    String result = input;


			    /*
			     * =========================================================
			     * PROCESS EACH RULE
			     * =========================================================
			     *
			     * A numeric rule is converted when:
			     *
			     * 1. It touches a letter on at least one side.
			     *
			     * 2. It is NOT touching another digit.
			     *
			     * Therefore:
			     *
			     * la4       -> lachar
			     * 4bar      -> charbar
			     * a4b       -> acharb
			     * 2no       -> dono
			     * kha20     -> khabes
			     * 20yar     -> besyar
			     * zabar10t  -> zabardust
			     *
			     * But:
			     *
			     * 4         -> 4
			     * tum 4 bar -> tum 4 bar
			     * 44bar     -> 44bar
			     * la44      -> la44
			     * 22no      -> 22no
			     * abc44xyz  -> abc44xyz
			     */
			    for (String number : numbers) {

			        String replacement = rules.get(number);

			        String pattern =
			                "(?<![0-9])" +
			                "(?:" +
			                    "(?<=[A-Za-z])" +
			                    java.util.regex.Pattern.quote(number) +
			                    "|" +
			                    java.util.regex.Pattern.quote(number) +
			                    "(?=[A-Za-z])" +
			                ")" +
			                "(?![0-9])";

			        result = result.replaceAll(
			                pattern,
			                java.util.regex.Matcher.quoteReplacement(replacement)
			        );
			    }


			    /*
			     * Remove punctuation / special characters.
			     * Keep letters, digits and whitespace.
			     */
			    result = result.replaceAll(
			            "[^A-Za-z0-9\\s]",
			            ""
			    );


			    return result.toUpperCase();
			}
		   //..........................function ended
    }
}
