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
//...............This is 4th step of statistical pipeline

/**
 * ============================================================
 * WilcoxonPairwiseAnalyzer
 * ============================================================
 *
 * PURPOSE
 * ------------------------------------------------------------
 *
 * Performs paired Wilcoxon signed-rank tests comparing:
 *
 *      RASOM
 *
 * against each of the other 9 phonetic algorithms.
 *
 *
 * IMPORTANT: CONCEPT-LEVEL ANALYSIS
 * ------------------------------------------------------------
 *
 * Dataset:
 *
 *      NC = 2441 concepts
 *      NT = 10161 valid query variants per algorithm
 *      mc = 2 to 6 available variants per concept
 *
 * Empty Query cells are structural placeholders and are
 * excluded completely from the analysis.
 *
 * A genuine non-empty query with zero retrieved candidates
 * remains a valid observation.
 *
 *
 * CONCEPT-LEVEL AGGREGATION
 * ------------------------------------------------------------
 *
 * For each algorithm and concept, this class calculates:
 *
 *      Mean Precision across available variants
 *      Mean Recall across available variants
 *      Mean F1 across available variants
 *      Mean Candidate Set Size across available variants
 *
 * If concept c contains mc available variants:
 *
 *      ConceptMean =
 *
 *          Sum(metric over available variants) / mc
 *
 * The Wilcoxon signed-rank test is then performed on the
 * resulting 2441 paired concept-level observations.
 *
 *
 * METRICS TESTED
 * ------------------------------------------------------------
 *
 * 1. Precision
 * 2. Recall
 * 3. F1
 * 4. Candidate Set Size
 *
 *
 * WILCOXON TEST
 * ------------------------------------------------------------
 *
 * Two-sided paired Wilcoxon signed-rank test.
 *
 * Difference:
 *
 *      RASOM - Baseline
 *
 * Zero differences are excluded.
 *
 * Ties in absolute differences receive average ranks.
 *
 * For the large sample size, the p-value is calculated using
 * a normal approximation with:
 *
 *      - tie correction
 *      - continuity correction
 *
 *
 * EFFECT SIZE
 * ------------------------------------------------------------
 *
 * Rank-biserial correlation:
 *
 *      RBC = (W+ - W-) / (W+ + W-)
 *
 * Positive RBC:
 *
 *      RASOM tends to have larger values.
 *
 * Negative RBC:
 *
 *      RASOM tends to have smaller values.
 *
 * For Candidate Set Size, smaller values indicate greater
 * selectivity, so a negative effect favors RASOM
 * in terms of candidate-set compactness.
 *
 *
 * MULTIPLE COMPARISONS
 * ------------------------------------------------------------
 *
 * This class produces RAW p-values.
 *
 * Holm correction is performed separately in:
 *
 *      HolmCorrection.java
 *
 *
 * OUTPUT
 * ------------------------------------------------------------
 *
 * F:/Eclipse/EclipseHome/DS_TestBench/
 * WilcoxonPairwiseResults.xlsx
 *
 * 9 baselines x 4 metrics = 36 tests.
 *
 * ============================================================
 */
public class WilcoxonPairwiseAnalyzer {


    // ========================================================
    // FILE PATHS
    // ========================================================

    private static final String INPUT_FILE =
            "D:/BORUE/PhoneticEvaluation.xlsx";


    private static final String OUTPUT_FILE =
            "D:/BORUE/WilcoxonPairwiseResults.xlsx";


    // ========================================================
    // REFERENCE ALGORITHM
    // ========================================================

    private static final String REFERENCE_ALGORITHM =
            "RASOM";


    // ========================================================
    // EXPECTED DATA STRUCTURE
    // ========================================================

    private static final int EXPECTED_ALGORITHMS = 10;

    private static final int EXPECTED_CONCEPTS = 2441;

    private static final int EXPECTED_VALID_VARIANTS = 10161;


    // ========================================================
    // INPUT COLUMNS
    //
    // ZERO-BASED Apache POI indexing
    // ========================================================

    private static final int ALGORITHM_COLUMN = 0;        // A

    private static final int CONCEPT_COLUMN = 1;          // B

    private static final int QUERY_COLUMN = 3;            // D

    private static final int RETRIEVED_COUNT_COLUMN = 6; // G

    private static final int PRECISION_COLUMN = 8;        // I

    private static final int RECALL_COLUMN = 9;           // J

    private static final int F1_COLUMN = 10;              // K


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
                    "WILCOXON PAIRWISE ANALYZER"
            );

            System.out.println(
                    "=============================================="
            );

            System.out.println();


            // ------------------------------------------------
            // Read and aggregate data by concept.
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


            // ------------------------------------------------
            // Check RASOM exists.
            // ------------------------------------------------

            AlgorithmData romanData =
                    algorithms.get(
                            REFERENCE_ALGORITHM
                    );


            if (romanData == null) {

                throw new IllegalStateException(
                        "RASOM was not found "
                        + "in the evaluation file."
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
            // Create output workbook.
            // ------------------------------------------------

            XSSFWorkbook outputWorkbook =
                    new XSSFWorkbook();


            Sheet outputSheet =
                    outputWorkbook.createSheet(
                            "Wilcoxon Pairwise"
                    );


            createHeader(
                    outputSheet
            );


            int outputRow = 1;


            // ------------------------------------------------
            // Compare RASOM against each baseline.
            // ------------------------------------------------

            for (Map.Entry<String, AlgorithmData> entry
                    : algorithms.entrySet()) {


                String baselineName =
                        entry.getKey();


                if (baselineName.equals(
                        REFERENCE_ALGORITHM)) {

                    continue;
                }


                AlgorithmData baselineData =
                        entry.getValue();


                System.out.println();

                System.out.println(
                        "=============================================="
                );

                System.out.println(
                        "RASOM vs "
                        + baselineName
                );

                System.out.println(
                        "=============================================="
                );


                // --------------------------------------------
                // PRECISION
                // --------------------------------------------

                outputRow =
                        analyzeMetric(
                                outputSheet,
                                outputRow,
                                baselineName,
                                "Precision",
                                romanData,
                                baselineData,
                                MetricType.PRECISION
                        );


                // --------------------------------------------
                // RECALL
                // --------------------------------------------

                outputRow =
                        analyzeMetric(
                                outputSheet,
                                outputRow,
                                baselineName,
                                "Recall",
                                romanData,
                                baselineData,
                                MetricType.RECALL
                        );


                // --------------------------------------------
                // F1
                // --------------------------------------------

                outputRow =
                        analyzeMetric(
                                outputSheet,
                                outputRow,
                                baselineName,
                                "F1",
                                romanData,
                                baselineData,
                                MetricType.F1
                        );


                // --------------------------------------------
                // CANDIDATE SET SIZE
                // --------------------------------------------

                outputRow =
                        analyzeMetric(
                                outputSheet,
                                outputRow,
                                baselineName,
                                "Candidate Set Size",
                                romanData,
                                baselineData,
                                MetricType.CANDIDATE_SET
                        );
            }


            // ------------------------------------------------
            // Freeze heading.
            // ------------------------------------------------

            outputSheet.createFreezePane(
                    0,
                    1
            );


            // ------------------------------------------------
            // Auto-size.
            // ------------------------------------------------

            for (int column = 0;
                 column < 15;
                 column++) {

                outputSheet.autoSizeColumn(
                        column
                );
            }


            // ------------------------------------------------
            // Save workbook.
            // ------------------------------------------------

            FileOutputStream output =
                    new FileOutputStream(
                            OUTPUT_FILE
                    );


            outputWorkbook.write(
                    output
            );


            output.close();

            outputWorkbook.close();


            System.out.println();

            System.out.println(
                    "=============================================="
            );

            System.out.println(
                    "WILCOXON ANALYSIS COMPLETED SUCCESSFULLY"
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
                    "Expected comparisons:"
            );

            System.out.println(
                    "9 baselines x 4 metrics = 36 tests"
            );

        }
        catch (Exception e) {

            e.printStackTrace();
        }
    }


    // ========================================================
    // READ EVALUATION FILE
    //
    // IMPORTANT:
    //
    // Empty Query rows are structural placeholders.
    // They are excluded BEFORE concept-level aggregation.
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


        int validRowsRead = 0;

        int blankQueryRowsSkipped = 0;


        // ----------------------------------------------------
        // Row 0 = heading.
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
            // Blank Query = unavailable spelling variant.
            //
            // It is NOT a retrieval failure.
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


            double retrievedCount =
                    getDouble(
                            row,
                            RETRIEVED_COUNT_COLUMN
                    );


            double precision =
                    getDouble(
                            row,
                            PRECISION_COLUMN
                    );


            double recall =
                    getDouble(
                            row,
                            RECALL_COLUMN
                    );


            double f1 =
                    getDouble(
                            row,
                            F1_COLUMN
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

            ConceptData concept =
                    algorithmData.concepts.get(
                            conceptRow
                    );


            if (concept == null) {

                concept =
                        new ConceptData(
                                conceptRow
                        );


                algorithmData.concepts.put(
                        conceptRow,
                        concept
                );
            }


            // ------------------------------------------------
            // Add ONLY valid variant values.
            //
            // Note:
            // retrievedCount may be zero for a genuine query.
            // Such a query remains in the analysis.
            // ------------------------------------------------

            concept.precisionSum +=
                    precision;


            concept.recallSum +=
                    recall;


            concept.f1Sum +=
                    f1;


            concept.candidateSetSum +=
                    retrievedCount;


            concept.variantCount++;


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
                + (validRowsRead
                + blankQueryRowsSkipped)
        );


        // ----------------------------------------------------
        // Validate each algorithm.
        // ----------------------------------------------------

        for (Map.Entry<String, AlgorithmData> entry
                : algorithms.entrySet()) {


            String algorithm =
                    entry.getKey();


            AlgorithmData data =
                    entry.getValue();


            int validVariants = 0;

            int invalidClusters = 0;


            int[] distribution =
                    new int[7];


            for (ConceptData concept
                    : data.concepts.values()) {


                validVariants +=
                        concept.variantCount;


                if (concept.variantCount >= 0
                        && concept.variantCount
                        < distribution.length) {

                    distribution[
                            concept.variantCount
                    ]++;
                }


                if (concept.variantCount < 2
                        || concept.variantCount > 6) {

                    invalidClusters++;


                    System.out.println(
                            "WARNING: "
                            + algorithm
                            + " | ConceptRow "
                            + concept.conceptRow
                            + " contains "
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
                    + invalidClusters
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
    // ANALYZE ONE METRIC
    // ========================================================

    private static int analyzeMetric(
            Sheet sheet,
            int outputRow,
            String baselineName,
            String metricName,
            AlgorithmData romanData,
            AlgorithmData baselineData,
            MetricType metricType) {


        List<Double> romanValues =
                new ArrayList<Double>();


        List<Double> baselineValues =
                new ArrayList<Double>();


        // ----------------------------------------------------
        // Pair concepts by ConceptRow.
        // ----------------------------------------------------

        for (Map.Entry<Integer, ConceptData> entry
                : romanData.concepts.entrySet()) {


            int conceptRow =
                    entry.getKey();


            ConceptData romanConcept =
                    entry.getValue();


            ConceptData baselineConcept =
                    baselineData.concepts.get(
                            conceptRow
                    );


            if (baselineConcept == null) {

                throw new IllegalStateException(
                        "Missing concept "
                        + conceptRow
                        + " for baseline "
                        + baselineName
                );
            }


            // ------------------------------------------------
            // Because every algorithm is evaluated on the
            // identical master query set, each paired concept
            // should contain the same number of valid variants.
            // ------------------------------------------------

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


            romanValues.add(
                    getConceptMetric(
                            romanConcept,
                            metricType
                    )
            );


            baselineValues.add(
                    getConceptMetric(
                            baselineConcept,
                            metricType
                    )
            );
        }


        if (romanValues.size()
                != EXPECTED_CONCEPTS) {

            System.out.println(
                    "WARNING: "
                    + baselineName
                    + " "
                    + metricName
                    + " has "
                    + romanValues.size()
                    + " paired concepts instead of "
                    + EXPECTED_CONCEPTS
                    + "."
            );
        }


        // ----------------------------------------------------
        // Means shown for descriptive interpretation.
        //
        // NOTE:
        // These are means of the 2441 CONCEPT-LEVEL means.
        // They need not be numerically identical to the
        // variant-level macro means from descriptive analysis,
        // because concepts contain different numbers of
        // available variants.
        // ----------------------------------------------------

        double romanMean =
                mean(
                        romanValues
                );


        double baselineMean =
                mean(
                        baselineValues
                );


        double meanDifference =
                romanMean
                -
                baselineMean;


        // ----------------------------------------------------
        // Wilcoxon.
        // ----------------------------------------------------

        WilcoxonResult result =
                wilcoxonSignedRank(
                        romanValues,
                        baselineValues
                );


        // ----------------------------------------------------
        // Console.
        // ----------------------------------------------------

        System.out.println();

        System.out.println(
                metricName
        );


        System.out.println(
                "Paired concepts: "
                + romanValues.size()
        );


        System.out.println(
                "Non-zero differences: "
                + result.nonZeroPairs
        );


        System.out.println(
                "Zero differences excluded: "
                + result.zeroDifferences
        );


        System.out.println(
                "Roman concept mean: "
                + romanMean
        );


        System.out.println(
                "Baseline concept mean: "
                + baselineMean
        );


        System.out.println(
                "Mean difference (Roman - Baseline): "
                + meanDifference
        );


        System.out.println(
                "W+: "
                + result.wPlus
        );


        System.out.println(
                "W-: "
                + result.wMinus
        );


        System.out.println(
                "W statistic: "
                + result.wStatistic
        );


        System.out.println(
                "Z: "
                + result.z
        );


        System.out.println(
                "Raw two-sided p-value: "
                + result.pValue
        );


        System.out.println(
                "Rank-biserial correlation: "
                + result.rankBiserial
        );


        // ----------------------------------------------------
        // Write Excel row.
        // ----------------------------------------------------

        Row row =
                sheet.createRow(
                        outputRow
                );


        row.createCell(0)
           .setCellValue(
                   REFERENCE_ALGORITHM
           );


        row.createCell(1)
           .setCellValue(
                   baselineName
           );


        row.createCell(2)
           .setCellValue(
                   metricName
           );


        row.createCell(3)
           .setCellValue(
                   romanValues.size()
           );


        row.createCell(4)
           .setCellValue(
                   result.nonZeroPairs
           );


        row.createCell(5)
           .setCellValue(
                   result.zeroDifferences
           );


        row.createCell(6)
           .setCellValue(
                   romanMean
           );


        row.createCell(7)
           .setCellValue(
                   baselineMean
           );


        row.createCell(8)
           .setCellValue(
                   meanDifference
           );


        row.createCell(9)
           .setCellValue(
                   result.wPlus
           );


        row.createCell(10)
           .setCellValue(
                   result.wMinus
           );


        row.createCell(11)
           .setCellValue(
                   result.wStatistic
           );


        row.createCell(12)
           .setCellValue(
                   result.z
           );


        row.createCell(13)
           .setCellValue(
                   result.pValue
           );


        row.createCell(14)
           .setCellValue(
                   result.rankBiserial
           );


        return outputRow + 1;
    }


    // ========================================================
    // GET CONCEPT-LEVEL METRIC
    //
    // Mean is calculated across the ACTUAL available variants
    // for that concept:
    //
    //      mc = 2 ... 6
    // ========================================================

    private static double getConceptMetric(
            ConceptData concept,
            MetricType metricType) {


        if (concept.variantCount == 0) {

            throw new IllegalStateException(
                    "ConceptRow "
                    + concept.conceptRow
                    + " contains zero valid variants."
            );
        }


        switch (metricType) {


            case PRECISION:

                return concept.precisionSum
                        /
                        concept.variantCount;


            case RECALL:

                return concept.recallSum
                        /
                        concept.variantCount;


            case F1:

                return concept.f1Sum
                        /
                        concept.variantCount;


            case CANDIDATE_SET:

                return concept.candidateSetSum
                        /
                        concept.variantCount;


            default:

                throw new IllegalArgumentException(
                        "Unsupported metric type."
                );
        }
    }


    // ========================================================
    // WILCOXON SIGNED-RANK TEST
    // ========================================================

    private static WilcoxonResult wilcoxonSignedRank(
            List<Double> romanValues,
            List<Double> baselineValues) {


        if (romanValues.size()
                != baselineValues.size()) {

            throw new IllegalArgumentException(
                    "Paired samples have different sizes."
            );
        }


        List<Difference> differences =
                new ArrayList<Difference>();


        int zeroDifferences = 0;


        // ----------------------------------------------------
        // Build paired differences:
        //
        //      d_c = RASOM - Baseline
        // ----------------------------------------------------

        for (int i = 0;
             i < romanValues.size();
             i++) {


            double difference =
                    romanValues.get(i)
                    -
                    baselineValues.get(i);


            /*
             * Protect against meaningless floating-point
             * residue.
             */
            if (Math.abs(difference)
                    < 1.0e-12) {

                zeroDifferences++;

                continue;
            }


            Difference item =
                    new Difference();


            item.difference =
                    difference;


            item.absoluteDifference =
                    Math.abs(
                            difference
                    );


            differences.add(
                    item
            );
        }


        // ----------------------------------------------------
        // Sort by absolute difference.
        // ----------------------------------------------------

        Collections.sort(
                differences,
                new Comparator<Difference>() {

                    @Override
                    public int compare(
                            Difference a,
                            Difference b) {


                        return Double.compare(
                                a.absoluteDifference,
                                b.absoluteDifference
                        );
                    }
                }
        );


        int n =
                differences.size();


        WilcoxonResult result =
                new WilcoxonResult();


        result.nonZeroPairs =
                n;


        result.zeroDifferences =
                zeroDifferences;


        if (n == 0) {

            result.wPlus = 0.0;

            result.wMinus = 0.0;

            result.wStatistic = 0.0;

            result.z = 0.0;

            result.pValue = 1.0;

            result.rankBiserial = 0.0;

            return result;
        }


        // ----------------------------------------------------
        // Assign average ranks to tied absolute differences.
        //
        // Also accumulate:
        //
        //      sum(t^3 - t)
        //
        // for the tie-corrected variance.
        // ----------------------------------------------------

        double tieCorrectionSum =
                0.0;


        int start = 0;


        while (start < n) {


            int end =
                    start;


            double tieValue =
                    differences
                    .get(start)
                    .absoluteDifference;


            while (end + 1 < n
                    &&
                    Math.abs(
                            differences
                            .get(end + 1)
                            .absoluteDifference
                            -
                            tieValue
                    ) < 1.0e-12) {

                end++;
            }


            double firstRank =
                    start + 1.0;


            double lastRank =
                    end + 1.0;


            double averageRank =
                    (firstRank + lastRank)
                    /
                    2.0;


            for (int i = start;
                 i <= end;
                 i++) {


                differences
                .get(i)
                .rank =
                        averageRank;
            }


            int tieSize =
                    end - start + 1;


            if (tieSize > 1) {

                tieCorrectionSum +=
                        ((double) tieSize
                        * tieSize
                        * tieSize)
                        -
                        tieSize;
            }


            start =
                    end + 1;
        }


        // ----------------------------------------------------
        // Positive and negative rank sums.
        // ----------------------------------------------------

        double wPlus = 0.0;

        double wMinus = 0.0;


        for (Difference item
                : differences) {


            if (item.difference > 0.0) {

                wPlus +=
                        item.rank;
            }
            else {

                wMinus +=
                        item.rank;
            }
        }


        result.wPlus =
                wPlus;


        result.wMinus =
                wMinus;


        result.wStatistic =
                Math.min(
                        wPlus,
                        wMinus
                );


        // ----------------------------------------------------
        // Rank-biserial correlation.
        //
        //      RBC = (W+ - W-) / (W+ + W-)
        // ----------------------------------------------------

        double totalRank =
                wPlus
                +
                wMinus;


        if (totalRank == 0.0) {

            result.rankBiserial =
                    0.0;
        }
        else {

            result.rankBiserial =
                    (wPlus - wMinus)
                    /
                    totalRank;
        }


        // ----------------------------------------------------
        // Normal approximation.
        //
        // Expected W+:
        //
        //      n(n+1)/4
        //
        // Tie-corrected variance:
        //
        // [n(n+1)(2n+1) - 0.5*sum(t^3-t)] / 24
        // ----------------------------------------------------

        double expectedW =
                n
                *
                (n + 1.0)
                /
                4.0;


        double variance =
                (
                    n
                    *
                    (n + 1.0)
                    *
                    (2.0 * n + 1.0)
                    -
                    0.5
                    *
                    tieCorrectionSum
                )
                /
                24.0;


        if (variance <= 0.0) {

            result.z = 0.0;

            result.pValue = 1.0;

            return result;
        }


        double standardDeviation =
                Math.sqrt(
                        variance
                );


        // ----------------------------------------------------
        // Continuity correction.
        //
        // Move W+ by 0.5 toward its expected value.
        // ----------------------------------------------------

        double differenceFromMean =
                wPlus
                -
                expectedW;


        double correctedDifference;


        if (differenceFromMean > 0.0) {

            correctedDifference =
                    differenceFromMean
                    -
                    0.5;
        }
        else if (differenceFromMean < 0.0) {

            correctedDifference =
                    differenceFromMean
                    +
                    0.5;
        }
        else {

            correctedDifference =
                    0.0;
        }


        double z =
                correctedDifference
                /
                standardDeviation;


        result.z =
                z;


        // ----------------------------------------------------
        // Two-sided p-value.
        //
        // IMPORTANT:
        //
        // Instead of:
        //
        //      2 * (1 - Phi(|z|))
        //
        // use:
        //
        //      erfc(|z| / sqrt(2))
        //
        // This avoids catastrophic cancellation for large |z|
        // and prevents highly significant p-values from
        // becoming artificial literal zeros merely because
        // Phi(|z|) rounded to 1.0.
        // ----------------------------------------------------

        double pValue =
                erfc(
                        Math.abs(z)
                        /
                        Math.sqrt(2.0)
                );


        if (pValue < 0.0) {

            pValue = 0.0;
        }


        if (pValue > 1.0) {

            pValue = 1.0;
        }


        result.pValue =
                pValue;


        return result;
    }


    // ========================================================
    // COMPLEMENTARY ERROR FUNCTION
    //
    // Numerical Recipes approximation.
    //
    // For x >= 0:
    //
    //      erfc(x) = tau
    //
    // This is numerically preferable to computing:
    //
    //      1 - erf(x)
    //
    // for large x.
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
    // MEAN
    // ========================================================

    private static double mean(
            List<Double> values) {


        if (values.isEmpty()) {

            return 0.0;
        }


        double sum = 0.0;


        for (double value : values) {

            sum += value;
        }


        return sum
                /
                values.size();
    }


    // ========================================================
    // CREATE OUTPUT HEADER
    // ========================================================

    private static void createHeader(
            Sheet sheet) {


        Row row =
                sheet.createRow(0);


        String[] headings = {

                "ReferenceAlgorithm",
                "BaselineAlgorithm",
                "Metric",
                "PairedConcepts",
                "NonZeroPairs",
                "ZeroDifferences",
                "RomanMean",
                "BaselineMean",
                "MeanDifference",
                "WPlus",
                "WMinus",
                "WStatistic",
                "Z",
                "RawPValue",
                "RankBiserialCorrelation"
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
    // GET STRING CELL
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
    // GET NUMERIC CELL
    //
    // Old Apache POI compatible.
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
    // ENUM: METRIC TYPE
    // ========================================================

    private enum MetricType {

        PRECISION,

        RECALL,

        F1,

        CANDIDATE_SET
    }


    // ========================================================
    // ALGORITHM DATA
    // ========================================================

    private static class AlgorithmData {


        Map<Integer, ConceptData> concepts =
                new LinkedHashMap<Integer, ConceptData>();
    }


    // ========================================================
    // CONCEPT DATA
    // ========================================================

    private static class ConceptData {


        int conceptRow;

        int variantCount;

        double precisionSum;

        double recallSum;

        double f1Sum;

        double candidateSetSum;


        ConceptData(
                int conceptRow) {


            this.conceptRow =
                    conceptRow;
        }
    }


    // ========================================================
    // DIFFERENCE + RANK
    // ========================================================

    private static class Difference {


        double difference;

        double absoluteDifference;

        double rank;
    }


    // ========================================================
    // WILCOXON RESULT
    // ========================================================

    private static class WilcoxonResult {


        int nonZeroPairs;

        int zeroDifferences;

        double wPlus;

        double wMinus;

        double wStatistic;

        double z;

        double pValue;

        double rankBiserial;
    }
}