package BORUE;

import java.io.File;

import java.io.FileInputStream;

import java.io.FileOutputStream;

import java.util.ArrayList;

import java.util.Collections;

import java.util.Comparator;

import java.util.HashSet;

import java.util.List;

import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;

import org.apache.poi.ss.usermodel.Row;

import org.apache.poi.ss.usermodel.Sheet;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.semanticweb.owlapi.apibinding.OWLManager;

import org.semanticweb.owlapi.model.IRI;

import org.semanticweb.owlapi.model.OWLClass;

import org.semanticweb.owlapi.model.OWLClassExpression;

import org.semanticweb.owlapi.model.OWLDataFactory;

import org.semanticweb.owlapi.model.OWLDataProperty;

import org.semanticweb.owlapi.model.OWLNamedIndividual;

import org.semanticweb.owlapi.model.OWLLiteral;

import org.semanticweb.owlapi.model.OWLOntology;

import org.semanticweb.owlapi.model.OWLOntologyManager;

import org.semanticweb.owlapi.search.EntitySearcher;



/**

 * ================================================================

 * RomanUrduSimilarityExcelMatcher

 * ================================================================

 *

 * Excel-based Roman Urdu ontology matcher.

 *

 * MATCHING PROCESS

 * ----------------

 *

 * Stage 1:

 *

 * Strict phonetic relevance filtering.

 *

 * A candidate is accepted only if at least one query phonetic word

 * has either:

 *

 * 1. An exact complete match

 *

 * OR

 *

 * 2. An explicitly allowed suffix variation.

 *

 *

 * Example:

 *

 * Query:

 *      MUSM

 *

 * Accepted:

 *      MUSM

 *      GRM K MUSM

 *      MUSMI

 *      MUSMYT

 *

 * Rejected:

 *      MUSMN

 *      MUSMM

 *      MUSR

 *      MUSLM

 *

 *

 * Stage 2:

 *

 * Ranking of already valid phonetic candidates.

 *

 * Ranking priority:

 *

 * 1. Match tier

 * 2. Exact surface spelling

 * 3. Surface spelling penalty

 * 4. Exact phonetic word count

 * 5. Allowed variation count

 * 6. Phrase metric

 * 7. Word metric

 * 8. Final similarity score

 *

 * Lower metric / score is better.

 *

 * ================================================================

 */

public class RomanUrduSimilarityExcelMatcher {

    /*

     * ================================================================

     * CONFIGURATION

     * ================================================================

     */

    private static final String ONTOLOGY_PATH = "D:/BORUE/BiOnt.owl";



    private static final String EXCEL_PATH = "D:/BORUE/TestBenchFinalCount.xlsx";

    private static final String OUTPUT_PATH = "D:/BORUE/RASOM_Result.xlsx";

    private static final String BASE_IRI =  "http://localhost:3030/BiOnt.owl#";



    private static final String STR_VAL_PROPERTY = "strVal";



    private static final String ROM_META_PROPERTY = "romMeta";



    private static final String LANGUAGE_IN_CLASS = "Language_In";



    private static final int INPUT_COLUMN_COUNT =  6;



    private static final int OUTPUT_COLUMN_START = 8;  



    private static final int TOP_K = 1919; 



    private static final double A = 1.0;



    private static final double B = 2.0;



    
    private static final double SURFACE_PENALTY_WEIGHT =  0.5;



    

    private static final String[] ALLOWED_VARIATIONS =

            {

                    "I",

                    "YT"

            };



    
    public static void main(String[] args) {

        try {

            System.out.println();

            System.out.println(

                    "=================================================="

            );

            System.out.println(

                    "ROMAN URDU SIMILARITY EXCEL MATCHER"

            );

            System.out.println(

                    "=================================================="

            );



                RomanMetaphone encoder =

                    new RomanMetaphone();



    

            OWLOntologyManager manager =

                    OWLManager.createOWLOntologyManager();



    
            System.out.println("Loading ontology...");



            OWLOntology ontology =

                    manager.loadOntologyFromOntologyDocument(

                            new File(

                                    ONTOLOGY_PATH

                            )

                    );



            System.out.println("Ontology loaded successfully.");


            OWLDataFactory factory =  manager.getOWLDataFactory();



            

            OWLDataProperty strVal =

                    factory.getOWLDataProperty(

                            IRI.create(

                                    BASE_IRI

                                    +

                                    STR_VAL_PROPERTY

                            )

                    );



            
            OWLDataProperty romMeta =

                    factory.getOWLDataProperty(

                            IRI.create(

                                    BASE_IRI

                                    +

                                    ROM_META_PROPERTY

                            )

                    );



            
            OWLClass languageIn =

                    factory.getOWLClass(

                            IRI.create(

                                    BASE_IRI

                                    +

                                    LANGUAGE_IN_CLASS

                            )

                    );



            

            Set<OWLClass> languageClasses =

                    new HashSet<OWLClass>();



            collectSubClasses(

                    languageIn,

                    ontology,

                    languageClasses

            );



            System.out.println(

                    "Language classes found: "

                    +

                    languageClasses.size()

            );



            
            List<DictionaryEntry> dictionary =

                    buildDictionary(

                            ontology,

                            languageClasses,

                            strVal,

                            romMeta

                    );



            System.out.println(

                    "Dictionary entries loaded: "

                    +

                    dictionary.size()

            );



            System.out.println();

            System.out.println(

                    "Allowed phonetic variations:"

            );



            for (String variation :

                    ALLOWED_VARIATIONS) {

                System.out.println(

                        "   +"

                        +

                        variation

                );

            }



           
            processExcel(

                    EXCEL_PATH,

                    OUTPUT_PATH,

                    dictionary,

                    encoder

            );



            System.out.println();

            System.out.println(

                    "=================================================="

            );

            System.out.println(

                    "Processing completed successfully."

            );

            System.out.println(

                    "Output file:"

            );

            System.out.println(

                    OUTPUT_PATH

            );

            System.out.println(

                    "=================================================="

            );



        }

        catch (Exception e) {

            System.out.println();

            System.out.println(

                    "ERROR DURING PROCESSING"

            );

            e.printStackTrace();

        }

    }



    

    private static List<DictionaryEntry> buildDictionary(

            OWLOntology ontology,

            Set<OWLClass> languageClasses,

            OWLDataProperty strVal,

            OWLDataProperty romMeta) {



        List<DictionaryEntry> dictionary =

                new ArrayList<DictionaryEntry>();



        int count =

                0;



        for (OWLNamedIndividual individual :

                ontology.getIndividualsInSignature()) {



            boolean isLanguageIndividual =

                    false;



            /*

             * Check class membership.

             */

            for (OWLClassExpression type :

                    EntitySearcher.getTypes(

                            individual,

                            ontology

                    )) {



                if (type instanceof OWLClass

                        &&

                    languageClasses.contains(

                            type.asOWLClass()

                    )) {



                    isLanguageIndividual =

                            true;



                    break;

                }

            }



            if (!isLanguageIndividual) {

                continue;

            }



            /*

             * Read strVal.

             */

            List<String> strValues =

                    new ArrayList<String>();



            EntitySearcher

                    .getDataPropertyValues(

                            individual,

                            strVal,

                            ontology

                    )

                    .forEach(

                            (OWLLiteral literal) -> {

                                if (literal != null) {

                                    String value =

                                            literal

                                                    .getLiteral()

                                                    .trim();



                                    if (!value.isEmpty()) {

                                        strValues.add(

                                                value

                                        );

                                    }

                                }

                            }

                    );



            /*

             * Read romMeta.

             */

            List<String> romMetaValues =

                    new ArrayList<String>();



            EntitySearcher

                    .getDataPropertyValues(

                            individual,

                            romMeta,

                            ontology

                    )

                    .forEach(

                            (OWLLiteral literal) -> {

                                if (literal != null) {

                                    String value =

                                            literal

                                                    .getLiteral()

                                                    .trim();



                                    if (!value.isEmpty()) {

                                        romMetaValues.add(

                                                value

                                        );

                                    }

                                }

                            }

                    );



            /*

             * Ignore incomplete entries.

             */

            if (strValues.isEmpty()

                    ||

                romMetaValues.isEmpty()) {

                continue;

            }



            DictionaryEntry entry =

                    new DictionaryEntry();



            entry.individualIRI =

                    individual

                            .getIRI()

                            .toString();



            entry.strVal =

                    strValues.get(0);



            entry.romMeta =

                    romMetaValues

                            .get(0)

                            .trim()

                            .toUpperCase();



            dictionary.add(

                    entry

            );



            count++;



            if (count % 500 == 0) {

                System.out.println(

                        "Individuals loaded: "

                        +

                        count

                );

            }

        }



        return dictionary;

    }



    /*

     * ================================================================

     * PROCESS EXCEL

     * ================================================================

     */

    private static void processExcel(

            String excelPath,

            String outputPath,

            List<DictionaryEntry> dictionary,

            RomanMetaphone encoder)

            throws Exception {



        FileInputStream input =

                new FileInputStream(

                        excelPath

                );



        XSSFWorkbook workbook =

                new XSSFWorkbook(

                        input

                );



        Sheet sheet =

                workbook.getSheetAt(0);



        int processedRows =

                0;



        for (Row row :

                sheet) {



            /*

             * Skip header.

             */

            if (row.getRowNum() == 0) {

                continue;

            }



            /*

             * Process six input columns.

             */

            for (int i = 0;

                 i < INPUT_COLUMN_COUNT;

                 i++) {



                Cell inputCell =

                        row.getCell(

                                i

                        );



                Cell outputCell =

                        row.getCell(

                                i

                                +

                                OUTPUT_COLUMN_START

                        );



                if (outputCell == null) {

                    outputCell =

                            row.createCell(

                                    i

                                    +

                                    OUTPUT_COLUMN_START

                            );

                }



                if (inputCell == null) {

                    continue;

                }



                String query =

                        inputCell

                                .toString()

                                .trim();



                if (query.isEmpty()) {

                    continue;

                }



                System.out.println();

                System.out.println(

                        "Processing row "

                        +

                        row.getRowNum()

                        +

                        ", column "

                        +

                        i

                        +

                        ": "

                        +

                        query

                );



                /*

                 * Encode query.

                 */

                String encodedQuery =

                        encodeSentence(

                                query,

                                encoder

                        );



                System.out.println(

                        "Encoded query: "

                        +

                        encodedQuery

                );



                /*

                 * Find top matches.

                 */

                List<MatchResult> results =

                        findTopMatches(

                                query,

                                encodedQuery,

                                dictionary

                        );



                /*

                 * Format results.

                 */

                String resultText =

                        formatResults(

                                results

                        );



                /*

                 * Write results.

                 */

                outputCell.setCellValue(resultText);

            }



            processedRows++;



            if (processedRows % 10 == 0) {

                System.out.println("Excel rows processed: "

                        +

                        processedRows

                );

            }

        }



        input.close();



        FileOutputStream output =

                new FileOutputStream(

                        outputPath

                );



        workbook.write(

                output

        );



        output.close();



        workbook.close();

    }



    /*

     * ================================================================

     * FIND TOP MATCHES

     * ================================================================

     *

     * This is the main matching logic copied conceptually from

     * RomanUrduAppMatcher.

     *

     * Stage 1:

     *

     *      Strict phonetic filtering.

     *

     * Stage 2:

     *

     *      Ranking of valid candidates.

     *

     * ================================================================

     */

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

                        TOP_K,

                        results.size()

                );



        return new ArrayList<MatchResult>(

                results.subList(

                        0,

                        resultCount

                )

        );

    }



    /*

     * ================================================================

     * ANALYZE CANDIDATE

     * ================================================================

     *

     * Exact and explicitly allowed variation matching.

     *

     * Example:

     *

     * Query:

     *      MUSM

     *

     * Candidate:

     *      GRM K MUSM

     *

     * Accepted.

     *

     * Candidate:

     *      MUSMI

     *

     * Accepted if I is allowed.

     *

     * Candidate:

     *      MUSMN

     *

     * Rejected.

     *

     * ================================================================

     */

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

    /*
     * CONTROLLED PHONETIC ALIGNMENT
     *
     * Supported observations:
     * DUNU/DNU, ZHRLI/ZHRLA, ZRKZ/ZRKYZ, ZLYL/ZLL, ZBNU/ZBN,
     * and the exact-prefix terminal A/N case YKSA/YKSN.
     * ZUHR/ZHR is deliberately rejected.
     */
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



    /*

     * ================================================================

     * SURFACE PENALTY

     * ================================================================

     */

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



    /*

     * ================================================================

     * NORMALIZED LEVENSHTEIN

     * ================================================================

     */

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



    /*

     * ================================================================

     * LEVENSHTEIN DISTANCE

     * ================================================================

     */

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



    /*

     * ================================================================

     * PHRASE METRIC

     * ================================================================

     *

     * This is retained as a ranking metric only.

     *

     * It is NOT used to decide phonetic relevance.

     *

     * Relevance is decided by analyzeCandidate().

     *

     * The comparison is performed word-by-word in corresponding

     * positions, using the same phonetic word metric as the original

     * similarity matcher.

     * ================================================================

     */

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



    /*

     * ================================================================

     * PHONETIC WORD METRIC

     * ================================================================

     *

     * 0.4 * Normalized Levenshtein

     *

     * +

     *

     * 0.6 * Prefix Penalty

     * ================================================================

     */

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



    /*

     * ================================================================

     * LONGEST COMMON PREFIX

     * ================================================================

     */

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



    /*

     * ================================================================

     * WORD METRIC

     * ================================================================

     *

     * W =

     *

     * A * (|T-Q| / |T|)

     *

     * +

     *

     * B * (|Q-T| / |Q|)

     *

     *

     * Exact matches and allowed variations are considered matched.

     * ================================================================

     */

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



    /*

     * ================================================================

     * GET WORD LIST

     * ================================================================

     */

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



    /*

     * ================================================================

     * ENCODE SENTENCE

     * ================================================================

     */

    private static String encodeSentence(

            String value,

            RomanMetaphone encoder) {



        if (value == null

                ||

            value.trim().isEmpty()) {

            return "";

        }



        StringBuilder result =

                new StringBuilder();



        String[] words =

                value

                        .trim()

                        .split(

                                "\\s+"

                        );



        for (String word :

                words) {



            if (word == null

                    ||

                word.trim().isEmpty()) {

                continue;

            }

//...........................................................

            System.out.println("Word before encode = [" + word + "]");

            System.out.println("Word length = " + word.length());

            for (int i = 0; i < word.length(); i++) {

                System.out.println(

                    "Character " + i +

                    " = [" + word.charAt(i) + "]" +

                    " Unicode = U+" +

                    String.format("%04X", (int) word.charAt(i))

                );

            }

//......................................................

            String encoded =

                    encoder.encodeRoman(

                            word.trim()

                    );



            if (encoded == null

                    ||

                encoded.trim().isEmpty()) {

                continue;

            }



            if (result.length() > 0) {

                result.append(

                        " "

                );

            }



            result.append(

                    encoded

                            .trim()

                            .toUpperCase()

            );

        }



        return result.toString();

    }



    /*

     * ================================================================

     * NORMALIZE SURFACE TEXT

     * ================================================================

     */

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



    /*

     * ================================================================

     * COLLECT SUBCLASSES

     * ================================================================

     */

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



//.. .............. below is new format results, when you need to print ranks etc unhide this one 
    //..............and hide following format results back again baby
    private static String formatResults(
            List<MatchResult> results) {

        StringBuilder output =
                new StringBuilder();

        for (MatchResult result : results) {

            if (output.length() > 0) {
                output.append(", ");
            }


            output.append(result.strVal);
        }

        return output.toString();
    }
//.............................new format results end here ...................    
    


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




    private static class MatchAnalysis {



        MatchTier matchTier;



        int exactWordCount;



        int variationWordCount;



        int alignmentWordCount;



        int matchedCandidateWordCount;

    }




    private static class DictionaryEntry {



        String individualIRI;



        String strVal;



        String romMeta;

    }




    private static class MatchResult {



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
}

