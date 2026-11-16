package org.borue;


import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

//...............This is 6th step of statistical pipeline
/**
 * ============================================================
 * HitFailureMcNemarAnalyzer
 * ============================================================
 *
 * PURPOSE
 * ------------------------------------------------------------
 *
 * Compares RASOM with each of the other 9 algorithms
 * using concept-level Hit / Failure.
 *
 *
 * DATASET
 * ------------------------------------------------------------
 *
 * NC = 2441 concepts
 *
 * NT = 10161 valid query variants per algorithm
 *
 * Each concept contains:
 *
 *      mc = 2 to 6 available spelling variants
 *
 * Empty Query cells are structural placeholders and are
 * excluded completely from the analysis.
 *
 * A genuine non-empty query with Hit = 0 remains a valid
 * observation.
 *
 *
 * CONCEPT-LEVEL HIT DEFINITION
 * ------------------------------------------------------------
 *
 * For algorithm A and concept c:
 *
 *      Concept Hit = 1
 *
 * if AT LEAST ONE of the mc available spelling variants
 * has Hit = 1.
 *
 * Otherwise:
 *
 *      Concept Hit = 0
 *
 *
 * McNEMAR TABLE
 * ------------------------------------------------------------
 *
 *                           Baseline
 *                       Hit         Fail
 *
 * Roman Hit             n11          n10
 *
 * Roman Fail            n01          n00
 *
 *
 * n10:
 * RASOM Hit, Baseline Fail
 *
 * n01:
 * RASOM Fail, Baseline Hit
 *
 *
 * TEST
 * ------------------------------------------------------------
 *
 * Continuity-corrected McNemar statistic:
 *
 *      X^2 =
 *
 *      (|n10 - n01| - 1)^2
 *      ---------------------
 *          n10 + n01
 *
 * df = 1
 *
 *
 * EFFECT SIZE
 * ------------------------------------------------------------
 *
 * Matched-pairs odds ratio:
 *
 *      OR = n10 / n01
 *
 * If either discordant cell is zero:
 *
 *      OR = (n10 + 0.5) / (n01 + 0.5)
 *
 *
 * MULTIPLE COMPARISONS
 * ------------------------------------------------------------
 *
 * RASOM is compared against 9 baselines.
 *
 * Holm correction is applied across the 9 McNemar
 * raw p-values.
 *
 *
 * INPUT
 * ------------------------------------------------------------
 *
 * F:/Eclipse/EclipseHome/DS_TestBench/
 * PhoneticEvaluation.xlsx
 *
 *
 * OUTPUT
 * ------------------------------------------------------------
 *
 * F:/Eclipse/EclipseHome/DS_TestBench/
 * HitFailureMcNemarResults.xlsx
 *
 * ============================================================
 */
public class HitFailureMcNemarAnalyzer {


    // ========================================================
    // FILES
    // ========================================================

    private static final String INPUT_FILE =
            "D:/BORUE/PhoneticEvaluation.xlsx";


    private static final String OUTPUT_FILE =
            "D:/BORUE/HitFailureMcNemarResults.xlsx";


    // ========================================================
    // CONFIGURATION
    // ========================================================

    private static final String REFERENCE_ALGORITHM =
            "RASOM";


    private static final double ALPHA =
            0.05;


    private static final int EXPECTED_ALGORITHMS =
            10;


    private static final int EXPECTED_CONCEPTS =
            2441;


    private static final int EXPECTED_VALID_VARIANTS =
            10161;


    // ========================================================
    // INPUT COLUMNS
    //
    // Zero-based Apache POI indexing
    //
    // A = Algorithm
    // B = ConceptRow
    // D = Query
    // L = Hit
    // ========================================================

    private static final int ALGORITHM_COLUMN = 0;

    private static final int CONCEPT_COLUMN = 1;

    private static final int QUERY_COLUMN = 3;

    private static final int HIT_COLUMN = 11;


    private static final DataFormatter DATA_FORMATTER =
            new DataFormatter();


    // ========================================================
    // MAIN
    // ========================================================

    public static void main(String[] args) {


        try {


            System.out.println();

            System.out.println(
                    "=============================================="
            );

            System.out.println(
                    "HIT / FAILURE McNEMAR ANALYZER"
            );

            System.out.println(
                    "=============================================="
            );

            System.out.println();


            // ------------------------------------------------
            // Read VALID variant-level hits and aggregate
            // them by concept.
            // ------------------------------------------------

            Map<String, AlgorithmData> algorithms =
                    readEvaluationFile();


            System.out.println();

            System.out.println(
                    "Algorithms found: "
                    + algorithms.size()
            );


            if (algorithms.size()
                    != EXPECTED_ALGORITHMS) {


                System.out.println(
                        "WARNING: Expected "
                        + EXPECTED_ALGORITHMS
                        + " algorithms."
                );

            }


            AlgorithmData romanData =
                    algorithms.get(
                            REFERENCE_ALGORITHM
                    );


            if (romanData == null) {


                throw new IllegalStateException(
                        "RASOM not found."
                );

            }


            System.out.println(
                    "RASOM concepts: "
                    + romanData.concepts.size()
            );


            if (romanData.concepts.size()
                    != EXPECTED_CONCEPTS) {


                System.out.println(
                        "WARNING: RASOM expected "
                        + EXPECTED_CONCEPTS
                        + " concepts."
                );

            }


            // ------------------------------------------------
            // Perform 9 paired McNemar comparisons.
            // ------------------------------------------------

            List<McNemarResult> results =
                    new ArrayList<McNemarResult>();


            for (Map.Entry<String, AlgorithmData> entry
                    : algorithms.entrySet()) {


                String baselineName =
                        entry.getKey();


                if (baselineName.equals(
                        REFERENCE_ALGORITHM)) {


                    continue;

                }


                McNemarResult result =
                        compareAlgorithms(
                                baselineName,
                                romanData,
                                entry.getValue()
                        );


                results.add(
                        result
                );

            }


            System.out.println();

            System.out.println(
                    "Pairwise comparisons: "
                    + results.size()
            );


            if (results.size() != 9) {


                System.out.println(
                        "WARNING: Expected 9 comparisons."
                );

            }


            // ------------------------------------------------
            // Holm correction across the 9 Hit comparisons.
            // ------------------------------------------------

            applyHolmCorrection(
                    results
            );


            // ------------------------------------------------
            // Print final results.
            // ------------------------------------------------

            for (McNemarResult result : results) {


                printResult(
                        result
                );

            }


            // ------------------------------------------------
            // Write Excel.
            // ------------------------------------------------

            writeOutput(
                    results
            );


            System.out.println();

            System.out.println(
                    "=============================================="
            );

            System.out.println(
                    "McNEMAR ANALYSIS COMPLETED SUCCESSFULLY"
            );

            System.out.println(
                    "=============================================="
            );

            System.out.println();


            System.out.println(
                    "Output file:"
            );


            System.out.println(
                    OUTPUT_FILE
            );


            System.out.println();


            System.out.println(
                    "Expected:"
            );


            System.out.println(
                    "9 comparisons + 1 heading = 10 Excel rows"
            );


        }
        catch (Exception e) {


            e.printStackTrace();

        }

    }


    // ========================================================
    // READ EVALUATION FILE
    //
    // CRITICAL:
    //
    // Empty Query rows are structural placeholders.
    //
    // They are excluded BEFORE concept-level Hit aggregation.
    //
    // Genuine non-empty queries with Hit = 0 remain included.
    // ========================================================

    private static Map<String, AlgorithmData>
            readEvaluationFile()
            throws Exception {


        FileInputStream input =
                new FileInputStream(
                        INPUT_FILE
                );


        XSSFWorkbook workbook =
                new XSSFWorkbook(
                        input
                );


        Sheet sheet =
                workbook.getSheetAt(0);


        Map<String, AlgorithmData> algorithms =
                new LinkedHashMap<String, AlgorithmData>();


        int validRowsRead =
                0;


        int blankQueryRowsSkipped =
                0;


        // ----------------------------------------------------
        // Skip heading row.
        // ----------------------------------------------------

        for (int rowNumber = 1;
             rowNumber <= sheet.getLastRowNum();
             rowNumber++) {


            Row row =
                    sheet.getRow(
                            rowNumber
                    );


            if (row == null) {


                continue;

            }


            String algorithm =
                    getString(
                            row,
                            ALGORITHM_COLUMN
                    );


            if (algorithm.isEmpty()) {


                continue;

            }


            // ------------------------------------------------
            // CRITICAL CORRECTION
            //
            // Blank Query means this spelling variant does
            // not exist for the concept.
            //
            // It is NOT a failed retrieval trial.
            // ------------------------------------------------

            String query =
                    getString(
                            row,
                            QUERY_COLUMN
                    );


            if (query.isEmpty()) {


                blankQueryRowsSkipped++;


                continue;

            }


            int conceptRow =
                    (int) Math.round(
                            getDouble(
                                    row,
                                    CONCEPT_COLUMN
                            )
                    );


            double hit =
                    getDouble(
                            row,
                            HIT_COLUMN
                    );


            // ------------------------------------------------
            // Algorithm container.
            // ------------------------------------------------

            AlgorithmData algorithmData =
                    algorithms.get(
                            algorithm
                    );


            if (algorithmData == null) {


                algorithmData =
                        new AlgorithmData();


                algorithms.put(
                        algorithm,
                        algorithmData
                );

            }


            // ------------------------------------------------
            // Concept container.
            // ------------------------------------------------

            ConceptHit concept =
                    algorithmData.concepts.get(
                            conceptRow
                    );


            if (concept == null) {


                concept =
                        new ConceptHit(
                                conceptRow
                        );


                algorithmData.concepts.put(
                        conceptRow,
                        concept
                );

            }


            // ------------------------------------------------
            // Add VALID variant.
            // ------------------------------------------------

            concept.variantCount++;


            /*
             * Concept becomes a Hit if ANY available
             * non-empty spelling variant has Hit = 1.
             */

            if (hit > 0.5) {


                concept.hit =
                        true;

            }


            validRowsRead++;


            if (validRowsRead % 20000 == 0) {


                System.out.println(
                        "Valid evaluation rows read: "
                        + validRowsRead
                );

            }

        }


        workbook.close();

        input.close();


        System.out.println();

        System.out.println(
                "Total VALID evaluation rows read: "
                + validRowsRead
        );


        System.out.println(
                "Blank-query placeholder rows skipped: "
                + blankQueryRowsSkipped
        );


        System.out.println(
                "Total accounted rows: "
                + (
                    validRowsRead
                    +
                    blankQueryRowsSkipped
                )
        );


        // ----------------------------------------------------
        // Validate algorithms and concept clusters.
        // ----------------------------------------------------

        for (Map.Entry<String, AlgorithmData> entry
                : algorithms.entrySet()) {


            String algorithm =
                    entry.getKey();


            AlgorithmData data =
                    entry.getValue();


            int malformed =
                    0;


            int validVariants =
                    0;


            int[] distribution =
                    new int[7];


            for (ConceptHit concept
                    : data.concepts.values()) {


                validVariants +=
                        concept.variantCount;


                if (concept.variantCount >= 0
                        &&
                        concept.variantCount
                        < distribution.length) {


                    distribution[
                            concept.variantCount
                    ]++;

                }


                /*
                 * Correct dataset structure:
                 *
                 * mc is between 2 and 6.
                 */

                if (concept.variantCount < 2
                        ||
                        concept.variantCount > 6) {


                    malformed++;


                    System.out.println(
                            "WARNING: "
                            + algorithm
                            + " | ConceptRow "
                            + concept.conceptRow
                            + " has "
                            + concept.variantCount
                            + " valid variants."
                    );

                }

            }


            System.out.println();

            System.out.println(
                    algorithm
                    + " concepts: "
                    + data.concepts.size()
            );


            System.out.println(
                    algorithm
                    + " valid variants: "
                    + validVariants
            );


            System.out.println(
                    algorithm
                    + " cluster-size distribution:"
            );


            for (int variants = 2;
                 variants <= 6;
                 variants++) {


                System.out.println(
                        "  "
                        + variants
                        + " variants: "
                        + distribution[variants]
                        + " concepts"
                );

            }


            System.out.println(
                    algorithm
                    + " malformed clusters: "
                    + malformed
            );


            if (data.concepts.size()
                    != EXPECTED_CONCEPTS) {


                System.out.println(
                        "WARNING: "
                        + algorithm
                        + " expected "
                        + EXPECTED_CONCEPTS
                        + " concepts."
                );

            }


            if (validVariants
                    != EXPECTED_VALID_VARIANTS) {


                System.out.println(
                        "WARNING: "
                        + algorithm
                        + " expected "
                        + EXPECTED_VALID_VARIANTS
                        + " valid variants."
                );

            }

        }


        return algorithms;

    }


    // ========================================================
    // COMPARE RASOM WITH ONE BASELINE
    // ========================================================

    private static McNemarResult compareAlgorithms(
            String baselineName,
            AlgorithmData romanData,
            AlgorithmData baselineData) {


        McNemarResult result =
                new McNemarResult();


        result.referenceAlgorithm =
                REFERENCE_ALGORITHM;


        result.baselineAlgorithm =
                baselineName;


        int n11 = 0;

        int n10 = 0;

        int n01 = 0;

        int n00 = 0;


        // ----------------------------------------------------
        // Pair concepts using ConceptRow.
        // ----------------------------------------------------

        for (Map.Entry<Integer, ConceptHit> entry
                : romanData.concepts.entrySet()) {


            int conceptRow =
                    entry.getKey();


            ConceptHit romanConcept =
                    entry.getValue();


            ConceptHit baselineConcept =
                    baselineData.concepts.get(
                            conceptRow
                    );


            if (baselineConcept == null) {


                throw new IllegalStateException(
                        "Missing concept "
                        + conceptRow
                        + " for "
                        + baselineName
                );

            }


            /*
             * Every algorithm should be evaluated on exactly
             * the same available variants for each concept.
             */

            if (romanConcept.variantCount
                    != baselineConcept.variantCount) {


                throw new IllegalStateException(
                        "Variant-count mismatch for ConceptRow "
                        + conceptRow
                        + " | RASOM="
                        + romanConcept.variantCount
                        + " | "
                        + baselineName
                        + "="
                        + baselineConcept.variantCount
                );

            }


            boolean romanHit =
                    romanConcept.hit;


            boolean baselineHit =
                    baselineConcept.hit;


            if (romanHit && baselineHit) {


                n11++;

            }
            else if (romanHit && !baselineHit) {


                n10++;

            }
            else if (!romanHit && baselineHit) {


                n01++;

            }
            else {


                n00++;

            }

        }


        result.pairedConcepts =
                n11 + n10 + n01 + n00;


        result.bothHit =
                n11;


        result.romanHitBaselineFail =
                n10;


        result.romanFailBaselineHit =
                n01;


        result.bothFail =
                n00;


        if (result.pairedConcepts
                != EXPECTED_CONCEPTS) {


            System.out.println(
                    "WARNING: "
                    + baselineName
                    + " has "
                    + result.pairedConcepts
                    + " paired concepts instead of "
                    + EXPECTED_CONCEPTS
                    + "."
            );

        }


        // ----------------------------------------------------
        // Concept-level Hit Rates.
        // ----------------------------------------------------

        if (result.pairedConcepts == 0) {


            result.romanHitRate =
                    0.0;


            result.baselineHitRate =
                    0.0;

        }
        else {


            result.romanHitRate =
                    ((double) (n11 + n10))
                    /
                    result.pairedConcepts;


            result.baselineHitRate =
                    ((double) (n11 + n01))
                    /
                    result.pairedConcepts;

        }


        result.hitRateDifference =
                result.romanHitRate
                -
                result.baselineHitRate;


        // ----------------------------------------------------
        // Discordant pairs.
        // ----------------------------------------------------

        int discordant =
                n10 + n01;


        result.discordantPairs =
                discordant;


        // ----------------------------------------------------
        // McNemar test.
        // ----------------------------------------------------

        if (discordant == 0) {


            result.chiSquare =
                    0.0;


            result.rawPValue =
                    1.0;

        }
        else {


            /*
             * Continuity-corrected McNemar statistic:
             *
             *      (|n10 - n01| - 1)^2
             *      ---------------------
             *          n10 + n01
             */

            double numerator =
                    Math.abs(
                            n10 - n01
                    )
                    -
                    1.0;


            /*
             * If |n10-n01| = 0, continuity correction should
             * not create an artificial positive statistic.
             */

            if (numerator < 0.0) {


                numerator =
                        0.0;

            }


            result.chiSquare =
                    (
                        numerator
                        *
                        numerator
                    )
                    /
                    discordant;


            /*
             * For chi-square with df = 1:
             *
             *      p = erfc(
             *              sqrt(chiSquare) / sqrt(2)
             *          )
             *
             * This is equivalent to:
             *
             *      2 * [1 - Phi(sqrt(chiSquare))]
             *
             * but is numerically safer for very small
             * tail probabilities.
             */

            double z =
                    Math.sqrt(
                            result.chiSquare
                    );


            result.rawPValue =
                    erfc(
                            z
                            /
                            Math.sqrt(2.0)
                    );


            if (result.rawPValue < 0.0) {


                result.rawPValue =
                        0.0;

            }


            if (result.rawPValue > 1.0) {


                result.rawPValue =
                        1.0;

            }

        }


        // ----------------------------------------------------
        // Matched-pairs odds ratio.
        //
        // OR = n10 / n01
        //
        // Apply 0.5 correction ONLY when either discordant
        // cell is zero.
        // ----------------------------------------------------

        if (n10 == 0 || n01 == 0) {


            result.matchedPairsOddsRatio =
                    (n10 + 0.5)
                    /
                    (n01 + 0.5);

        }
        else {


            result.matchedPairsOddsRatio =
                    ((double) n10)
                    /
                    n01;

        }


        return result;

    }


    // ========================================================
    // HOLM CORRECTION
    //
    // One family:
    //
    // RASOM vs 9 baselines for concept-level Hit.
    // ========================================================

    private static void applyHolmCorrection(
            List<McNemarResult> results) {


        List<McNemarResult> sorted =
                new ArrayList<McNemarResult>(
                        results
                );


        Collections.sort(
                sorted,
                new Comparator<McNemarResult>() {

                    @Override
                    public int compare(
                            McNemarResult a,
                            McNemarResult b) {


                        return Double.compare(
                                a.rawPValue,
                                b.rawPValue
                        );

                    }

                }
        );


        int m =
                sorted.size();


        double previousAdjusted =
                0.0;


        for (int i = 0;
             i < m;
             i++) {


            McNemarResult result =
                    sorted.get(i);


            int multiplier =
                    m - i;


            double preliminaryAdjusted =
                    result.rawPValue
                    *
                    multiplier;


            if (preliminaryAdjusted > 1.0) {


                preliminaryAdjusted =
                        1.0;

            }


            /*
             * Enforce Holm monotonicity.
             */

            double adjusted =
                    Math.max(
                            previousAdjusted,
                            preliminaryAdjusted
                    );


            if (adjusted > 1.0) {


                adjusted =
                        1.0;

            }


            result.holmRank =
                    i + 1;


            result.holmMultiplier =
                    multiplier;


            result.holmAdjustedPValue =
                    adjusted;


            result.significant =
                    adjusted <= ALPHA;


            previousAdjusted =
                    adjusted;

        }

    }


    // ========================================================
    // WRITE OUTPUT
    // ========================================================

    private static void writeOutput(
            List<McNemarResult> results)
            throws Exception {


        XSSFWorkbook workbook =
                new XSSFWorkbook();


        Sheet sheet =
                workbook.createSheet(
                        "Hit Failure McNemar"
                );


        createHeader(
                sheet
        );


        int rowNumber =
                1;


        for (McNemarResult result : results) {


            Row row =
                    sheet.createRow(
                            rowNumber
                    );


            row.createCell(0)
               .setCellValue(
                       result.referenceAlgorithm
               );


            row.createCell(1)
               .setCellValue(
                       result.baselineAlgorithm
               );


            row.createCell(2)
               .setCellValue(
                       result.pairedConcepts
               );


            row.createCell(3)
               .setCellValue(
                       result.bothHit
               );


            row.createCell(4)
               .setCellValue(
                       result.romanHitBaselineFail
               );


            row.createCell(5)
               .setCellValue(
                       result.romanFailBaselineHit
               );


            row.createCell(6)
               .setCellValue(
                       result.bothFail
               );


            row.createCell(7)
               .setCellValue(
                       result.discordantPairs
               );


            row.createCell(8)
               .setCellValue(
                       result.romanHitRate
               );


            row.createCell(9)
               .setCellValue(
                       result.baselineHitRate
               );


            row.createCell(10)
               .setCellValue(
                       result.hitRateDifference
               );


            row.createCell(11)
               .setCellValue(
                       result.chiSquare
               );


            row.createCell(12)
               .setCellValue(
                       result.rawPValue
               );


            row.createCell(13)
               .setCellValue(
                       result.matchedPairsOddsRatio
               );


            row.createCell(14)
               .setCellValue(
                       result.holmRank
               );


            row.createCell(15)
               .setCellValue(
                       result.holmMultiplier
               );


            row.createCell(16)
               .setCellValue(
                       result.holmAdjustedPValue
               );


            row.createCell(17)
               .setCellValue(
                       result.significant
               );


            rowNumber++;

        }


        sheet.createFreezePane(
                0,
                1
        );


        for (int column = 0;
             column < 18;
             column++) {


            sheet.autoSizeColumn(
                    column
            );

        }


        FileOutputStream output =
                new FileOutputStream(
                        OUTPUT_FILE
                );


        workbook.write(
                output
        );


        output.close();

        workbook.close();

    }


    // ========================================================
    // CREATE HEADER
    // ========================================================

    private static void createHeader(
            Sheet sheet) {


        Row row =
                sheet.createRow(0);


        String[] headings = {

                "ReferenceAlgorithm",
                "BaselineAlgorithm",
                "PairedConcepts",

                "BothHit",

                "RomanHitBaselineFail",

                "RomanFailBaselineHit",

                "BothFail",

                "DiscordantPairs",

                "RomanConceptHitRate",

                "BaselineConceptHitRate",

                "HitRateDifference",

                "McNemarChiSquare",

                "RawPValue",

                "MatchedPairsOddsRatio",

                "HolmRank",

                "HolmMultiplier",

                "HolmAdjustedPValue",

                "SignificantAt0.05"

        };


        for (int i = 0;
             i < headings.length;
             i++) {


            row.createCell(i)
               .setCellValue(
                       headings[i]
               );

        }

    }


    // ========================================================
    // PRINT RESULT
    // ========================================================

    private static void printResult(
            McNemarResult result) {


        System.out.println();

        System.out.println(
                "=============================================="
        );


        System.out.println(
                "RASOM vs "
                + result.baselineAlgorithm
        );


        System.out.println(
                "=============================================="
        );


        System.out.println(
                "Paired concepts: "
                + result.pairedConcepts
        );


        System.out.println(
                "Both Hit: "
                + result.bothHit
        );


        System.out.println(
                "Roman Hit / Baseline Fail: "
                + result.romanHitBaselineFail
        );


        System.out.println(
                "Roman Fail / Baseline Hit: "
                + result.romanFailBaselineHit
        );


        System.out.println(
                "Both Fail: "
                + result.bothFail
        );


        System.out.println(
                "Discordant pairs: "
                + result.discordantPairs
        );


        System.out.println(
                "Roman concept Hit Rate: "
                + result.romanHitRate
        );


        System.out.println(
                "Baseline concept Hit Rate: "
                + result.baselineHitRate
        );


        System.out.println(
                "Hit Rate difference: "
                + result.hitRateDifference
        );


        System.out.println(
                "McNemar Chi-square: "
                + result.chiSquare
        );


        System.out.println(
                "Raw p-value: "
                + result.rawPValue
        );


        System.out.println(
                "Matched-pairs odds ratio: "
                + result.matchedPairsOddsRatio
        );


        System.out.println(
                "Holm rank: "
                + result.holmRank
        );


        System.out.println(
                "Holm multiplier: "
                + result.holmMultiplier
        );


        System.out.println(
                "Holm-adjusted p-value: "
                + result.holmAdjustedPValue
        );


        System.out.println(
                "Significant at 0.05: "
                + result.significant
        );

    }


    // ========================================================
    // COMPLEMENTARY ERROR FUNCTION
    //
    // Numerical Recipes approximation.
    //
    // Used instead of:
    //
    //      2 * (1 - normalCDF(z))
    //
    // to avoid cancellation for very small tail probabilities.
    // ========================================================

    private static double erfc(
            double x) {


        if (x < 0.0) {


            return 2.0
                    -
                    erfc(
                            -x
                    );

        }


        double t =
                1.0
                /
                (
                    1.0
                    +
                    0.5
                    *
                    x
                );


        double tau =
                t
                *
                Math.exp(
                        -x * x
                        - 1.26551223
                        + t
                        * (
                            1.00002368
                            + t
                            * (
                                0.37409196
                                + t
                                * (
                                    0.09678418
                                    + t
                                    * (
                                        -0.18628806
                                        + t
                                        * (
                                            0.27886807
                                            + t
                                            * (
                                                -1.13520398
                                                + t
                                                * (
                                                    1.48851587
                                                    + t
                                                    * (
                                                        -0.82215223
                                                        + t
                                                        * 0.17087277
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                );


        return tau;

    }


    // ========================================================
    // GET STRING
    // ========================================================

    private static String getString(
            Row row,
            int column) {


        Cell cell =
                row.getCell(
                        column
                );


        if (cell == null) {


            return "";

        }


        return DATA_FORMATTER
                .formatCellValue(
                        cell
                )
                .trim();

    }


    // ========================================================
    // GET DOUBLE
    //
    // Compatible with older Apache POI version.
    // ========================================================

    private static double getDouble(
            Row row,
            int column) {


        Cell cell =
                row.getCell(
                        column
                );


        if (cell == null) {


            return 0.0;

        }


        if (cell.getCellType()
                == Cell.CELL_TYPE_NUMERIC) {


            return cell.getNumericCellValue();

        }


        String value =
                DATA_FORMATTER
                .formatCellValue(
                        cell
                )
                .trim();


        if (value.isEmpty()) {


            return 0.0;

        }


        try {


            return Double.parseDouble(
                    value
            );

        }
        catch (NumberFormatException e) {


            throw new IllegalArgumentException(
                    "Invalid numeric value: "
                    + value
                    + " | Excel row: "
                    + (row.getRowNum() + 1)
                    + " | Column: "
                    + (column + 1)
            );

        }

    }


    // ========================================================
    // DATA STRUCTURES
    // ========================================================

    private static class AlgorithmData {


        Map<Integer, ConceptHit> concepts =
                new LinkedHashMap<Integer, ConceptHit>();

    }


    private static class ConceptHit {


        int conceptRow;

        int variantCount;

        boolean hit;


        ConceptHit(
                int conceptRow) {


            this.conceptRow =
                    conceptRow;

        }

    }


    private static class McNemarResult {


        String referenceAlgorithm;

        String baselineAlgorithm;


        int pairedConcepts;


        int bothHit;

        int romanHitBaselineFail;

        int romanFailBaselineHit;

        int bothFail;


        int discordantPairs;


        double romanHitRate;

        double baselineHitRate;

        double hitRateDifference;


        double chiSquare;

        double rawPValue;


        double matchedPairsOddsRatio;


        int holmRank;

        int holmMultiplier;

        double holmAdjustedPValue;

        boolean significant;

    }

}