package org.borue;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//...............This is 7th and last step of statistical pipeline

/**
 * ============================================================
 * FinalStatisticalSummaryGenerator
 * ============================================================
 *
 * PURPOSE
 * ------------------------------------------------------------
 *
 * Combines the major statistical outputs of the intrinsic
 * phonetic evaluation into one publication-oriented workbook.
 *
 *
 * INPUT FILES
 * ------------------------------------------------------------
 *
 * 1. DescriptiveStatistics.xlsx
 *
 * 2. BootstrapConfidenceIntervals.xlsx
 *
 * 3. WilcoxonHolmCorrected.xlsx
 *
 * 4. HitFailureMcNemarResults.xlsx
 *
 *
 * OUTPUT
 * ------------------------------------------------------------
 *
 * FinalStatisticalSummary.xlsx
 *
 *
 * OUTPUT SHEETS
 * ------------------------------------------------------------
 *
 * Sheet 1:
 * Overall Summary
 *
 * Sheet 2:
 * Wilcoxon Summary
 *
 * Sheet 3:
 * Hit McNemar Summary
 *
 *
 * IMPORTANT
 * ------------------------------------------------------------
 *
 * This class DOES NOT perform new statistical tests.
 *
 * It only consolidates results already produced by the
 * previous validated analysis classes.
 *
 * ============================================================
 */
public class FinalStatisticalSummaryGenerator {


    // ========================================================
    // FILE PATHS
    // ========================================================

    private static final String BASE_PATH =
            "D:/BORUE/";


    private static final String DESCRIPTIVE_FILE =
            BASE_PATH
            + "DescriptiveStatistics.xlsx";


    private static final String BOOTSTRAP_FILE =
            BASE_PATH
            + "BootstrapConfidenceIntervals.xlsx";


    private static final String WILCOXON_FILE =
            BASE_PATH
            + "WilcoxonHolmCorrected.xlsx";


    private static final String MCNEMAR_FILE =
            BASE_PATH
            + "HitFailureMcNemarResults.xlsx";


    private static final String OUTPUT_FILE =
            BASE_PATH
            + "FinalStatisticalSummary.xlsx";


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
                    "FINAL STATISTICAL SUMMARY GENERATOR"
            );

            System.out.println(
                    "=============================================="
            );

            System.out.println();


            // ------------------------------------------------
            // Read descriptive results.
            // ------------------------------------------------

            Map<String, DescriptiveResult> descriptive =
                    readDescriptiveResults();


            System.out.println(
                    "Descriptive algorithms read: "
                    + descriptive.size()
            );


            // ------------------------------------------------
            // Read bootstrap confidence intervals.
            // ------------------------------------------------

            readBootstrapResults(
                    descriptive
            );


            // ------------------------------------------------
            // Create final workbook.
            // ------------------------------------------------

            XSSFWorkbook outputWorkbook =
                    new XSSFWorkbook();


            // =================================================
            // SHEET 1
            // =================================================

            createOverallSummary(
                    outputWorkbook,
                    descriptive
            );


            // =================================================
            // SHEET 2
            // =================================================

            createWilcoxonSummary(
                    outputWorkbook
            );


            // =================================================
            // SHEET 3
            // =================================================

            createMcNemarSummary(
                    outputWorkbook
            );


            // ------------------------------------------------
            // Save.
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
                    "FINAL SUMMARY GENERATED SUCCESSFULLY"
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
                    "Sheets created:"
            );


            System.out.println(
                    "1. Overall Summary"
            );


            System.out.println(
                    "2. Wilcoxon Summary"
            );


            System.out.println(
                    "3. Hit McNemar Summary"
            );


        }
        catch (Exception e) {


            e.printStackTrace();

        }

    }


    // ========================================================
    // READ DESCRIPTIVE RESULTS
    // ========================================================

    private static Map<String, DescriptiveResult>
            readDescriptiveResults()
            throws Exception {


        FileInputStream input =
                new FileInputStream(
                        DESCRIPTIVE_FILE
                );


        XSSFWorkbook workbook =
                new XSSFWorkbook(
                        input
                );


        Sheet sheet =
                workbook.getSheetAt(0);


        Map<String, DescriptiveResult> results =
                new LinkedHashMap<String, DescriptiveResult>();


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
                            0
                    );


            if (algorithm.isEmpty()) {

                continue;

            }


            DescriptiveResult result =
                    new DescriptiveResult();


            result.algorithm =
                    algorithm;


            result.trials =
                    getDouble(row, 1);


            result.hitRate =
                    getDouble(row, 2);


            result.precision =
                    getDouble(row, 3);


            result.recall =
                    getDouble(row, 4);


            result.f1 =
                    getDouble(row, 5);


            result.meanCandidateSet =
                    getDouble(row, 6);


            result.medianCandidateSet =
                    getDouble(row, 7);


            result.sdCandidateSet =
                    getDouble(row, 8);


            result.q1CandidateSet =
                    getDouble(row, 9);


            result.q3CandidateSet =
                    getDouble(row, 10);


            result.iqrCandidateSet =
                    getDouble(row, 11);


            result.minCandidateSet =
                    getDouble(row, 12);


            result.maxCandidateSet =
                    getDouble(row, 13);


            results.put(
                    algorithm,
                    result
            );

        }


        workbook.close();

        input.close();


        return results;

    }


    // ========================================================
    // READ BOOTSTRAP RESULTS
    // ========================================================

    private static void readBootstrapResults(
            Map<String, DescriptiveResult> descriptive)
            throws Exception {


        FileInputStream input =
                new FileInputStream(
                        BOOTSTRAP_FILE
                );


        XSSFWorkbook workbook =
                new XSSFWorkbook(
                        input
                );


        Sheet sheet =
                workbook.getSheetAt(0);


        int bootstrapRows =
                0;


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
                            0
                    );


            String metric =
                    getString(
                            row,
                            1
                    );


            if (algorithm.isEmpty()
                    ||
                    metric.isEmpty()) {


                continue;

            }


            DescriptiveResult result =
                    descriptive.get(
                            algorithm
                    );


            if (result == null) {


                throw new IllegalStateException(
                        "Bootstrap algorithm not found "
                        + "in descriptive results: "
                        + algorithm
                );

            }


            /*
             * Bootstrap file:
             *
             * A Algorithm
             * B Metric
             * C ObservedEstimate
             * D CI95Lower
             * E CI95Upper
             * F BootstrapMean
             * G BootstrapSE
             */

            double lower =
                    getDouble(
                            row,
                            3
                    );


            double upper =
                    getDouble(
                            row,
                            4
                    );


            double se =
                    getDouble(
                            row,
                            6
                    );


            if (metric.equalsIgnoreCase(
                    "Hit Rate")) {


                result.hitLower =
                        lower;


                result.hitUpper =
                        upper;


                result.hitSE =
                        se;

            }
            else if (metric.equalsIgnoreCase(
                    "Macro Precision")) {


                result.precisionLower =
                        lower;


                result.precisionUpper =
                        upper;


                result.precisionSE =
                        se;

            }
            else if (metric.equalsIgnoreCase(
                    "Macro Recall")) {


                result.recallLower =
                        lower;


                result.recallUpper =
                        upper;


                result.recallSE =
                        se;

            }
            else if (metric.equalsIgnoreCase(
                    "Macro F1")) {


                result.f1Lower =
                        lower;


                result.f1Upper =
                        upper;


                result.f1SE =
                        se;

            }
            else {


                System.out.println(
                        "WARNING: Unknown bootstrap metric: "
                        + metric
                );

            }


            bootstrapRows++;

        }


        workbook.close();

        input.close();


        System.out.println(
                "Bootstrap rows read: "
                + bootstrapRows
        );


        if (bootstrapRows != 40) {


            System.out.println(
                    "WARNING: Expected 40 bootstrap rows."
            );

        }

    }


    // ========================================================
    // CREATE OVERALL SUMMARY
    // ========================================================

    private static void createOverallSummary(
            XSSFWorkbook workbook,
            Map<String, DescriptiveResult> results) {


        Sheet sheet =
                workbook.createSheet(
                        "Overall Summary"
                );


        Row header =
                sheet.createRow(0);


        String[] headings = {

                "Algorithm",

                "Trials",

                "HitRate",
                "HitCI95Lower",
                "HitCI95Upper",

                "MacroPrecision",
                "PrecisionCI95Lower",
                "PrecisionCI95Upper",

                "MacroRecall",
                "RecallCI95Lower",
                "RecallCI95Upper",

                "MacroF1",
                "F1CI95Lower",
                "F1CI95Upper",

                "MeanCandidateSet",

                "MedianCandidateSet",

                "SDCandidateSet",

                "Q1CandidateSet",

                "Q3CandidateSet",

                "IQRCandidateSet",

                "MinCandidateSet",

                "MaxCandidateSet"

        };


        for (int i = 0;
             i < headings.length;
             i++) {


            header.createCell(i)
                  .setCellValue(
                          headings[i]
                  );

        }


        int rowNumber =
                1;


        for (DescriptiveResult result
                : results.values()) {


            Row row =
                    sheet.createRow(
                            rowNumber
                    );


            row.createCell(0)
               .setCellValue(
                       result.algorithm
               );


            row.createCell(1)
               .setCellValue(
                       result.trials
               );


            row.createCell(2)
               .setCellValue(
                       result.hitRate
               );


            row.createCell(3)
               .setCellValue(
                       result.hitLower
               );


            row.createCell(4)
               .setCellValue(
                       result.hitUpper
               );


            row.createCell(5)
               .setCellValue(
                       result.precision
               );


            row.createCell(6)
               .setCellValue(
                       result.precisionLower
               );


            row.createCell(7)
               .setCellValue(
                       result.precisionUpper
               );


            row.createCell(8)
               .setCellValue(
                       result.recall
               );


            row.createCell(9)
               .setCellValue(
                       result.recallLower
               );


            row.createCell(10)
               .setCellValue(
                       result.recallUpper
               );


            row.createCell(11)
               .setCellValue(
                       result.f1
               );


            row.createCell(12)
               .setCellValue(
                       result.f1Lower
               );


            row.createCell(13)
               .setCellValue(
                       result.f1Upper
               );


            row.createCell(14)
               .setCellValue(
                       result.meanCandidateSet
               );


            row.createCell(15)
               .setCellValue(
                       result.medianCandidateSet
               );


            row.createCell(16)
               .setCellValue(
                       result.sdCandidateSet
               );


            row.createCell(17)
               .setCellValue(
                       result.q1CandidateSet
               );


            row.createCell(18)
               .setCellValue(
                       result.q3CandidateSet
               );


            row.createCell(19)
               .setCellValue(
                       result.iqrCandidateSet
               );


            row.createCell(20)
               .setCellValue(
                       result.minCandidateSet
               );


            row.createCell(21)
               .setCellValue(
                       result.maxCandidateSet
               );


            rowNumber++;

        }


        sheet.createFreezePane(
                0,
                1
        );


        for (int column = 0;
             column < headings.length;
             column++) {


            sheet.autoSizeColumn(
                    column
            );

        }


        System.out.println(
                "Overall Summary rows: "
                + (rowNumber - 1)
        );

    }


    // ========================================================
    // CREATE WILCOXON SUMMARY
    // ========================================================

    private static void createWilcoxonSummary(
            XSSFWorkbook outputWorkbook)
            throws Exception {


        FileInputStream input =
                new FileInputStream(
                        WILCOXON_FILE
                );


        XSSFWorkbook inputWorkbook =
                new XSSFWorkbook(
                        input
                );


        Sheet inputSheet =
                inputWorkbook.getSheetAt(0);


        Sheet outputSheet =
                outputWorkbook.createSheet(
                        "Wilcoxon Summary"
                );


        Row header =
                outputSheet.createRow(0);


        String[] headings = {

                "Baseline",

                "Metric",

                "PairedConcepts",

                "RomanMean",

                "BaselineMean",

                "MeanDifference",

                "WStatistic",

                "Z",

                "RawPValue",

                "HolmAdjustedPValue",

                "SignificantAt0.05",

                "RankBiserialCorrelation"

        };


        for (int i = 0;
             i < headings.length;
             i++) {


            header.createCell(i)
                  .setCellValue(
                          headings[i]
                  );

        }


        int outputRow =
                1;


        for (int rowNumber = 1;
             rowNumber <= inputSheet.getLastRowNum();
             rowNumber++) {


            Row source =
                    inputSheet.getRow(
                            rowNumber
                    );


            if (source == null) {

                continue;

            }


            Row target =
                    outputSheet.createRow(
                            outputRow
                    );


            /*
             * Source columns:
             *
             * B  BaselineAlgorithm
             * C  Metric
             * D  PairedConcepts
             * G  RomanMean
             * H  BaselineMean
             * I  MeanDifference
             * L  WStatistic
             * M  Z
             * N  RawPValue
             * O  RankBiserialCorrelation
             * R  HolmAdjustedPValue
             * S  SignificantAt0.05
             */


            target.createCell(0)
                  .setCellValue(
                          getString(source, 1)
                  );


            target.createCell(1)
                  .setCellValue(
                          getString(source, 2)
                  );


            target.createCell(2)
                  .setCellValue(
                          getDouble(source, 3)
                  );


            target.createCell(3)
                  .setCellValue(
                          getDouble(source, 6)
                  );


            target.createCell(4)
                  .setCellValue(
                          getDouble(source, 7)
                  );


            target.createCell(5)
                  .setCellValue(
                          getDouble(source, 8)
                  );


            target.createCell(6)
                  .setCellValue(
                          getDouble(source, 11)
                  );


            target.createCell(7)
                  .setCellValue(
                          getDouble(source, 12)
                  );


            target.createCell(8)
                  .setCellValue(
                          getDouble(source, 13)
                  );


            target.createCell(9)
                  .setCellValue(
                          getDouble(source, 17)
                  );


            target.createCell(10)
                  .setCellValue(
                          getBoolean(
                                  source,
                                  18
                          )
                  );


            target.createCell(11)
                  .setCellValue(
                          getDouble(source, 14)
                  );


            outputRow++;

        }


        inputWorkbook.close();

        input.close();


        outputSheet.createFreezePane(
                0,
                1
        );


        for (int column = 0;
             column < headings.length;
             column++) {


            outputSheet.autoSizeColumn(
                    column
            );

        }


        System.out.println(
                "Wilcoxon Summary rows: "
                + (outputRow - 1)
        );


        if ((outputRow - 1) != 36) {


            System.out.println(
                    "WARNING: Expected 36 Wilcoxon rows."
            );

        }

    }


    // ========================================================
    // CREATE McNEMAR SUMMARY
    // ========================================================

    private static void createMcNemarSummary(
            XSSFWorkbook outputWorkbook)
            throws Exception {


        FileInputStream input =
                new FileInputStream(
                        MCNEMAR_FILE
                );


        XSSFWorkbook inputWorkbook =
                new XSSFWorkbook(
                        input
                );


        Sheet inputSheet =
                inputWorkbook.getSheetAt(0);


        Sheet outputSheet =
                outputWorkbook.createSheet(
                        "Hit McNemar Summary"
                );


        Row header =
                outputSheet.createRow(0);


        String[] headings = {

                "Baseline",

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

                "HolmAdjustedPValue",

                "SignificantAt0.05",

                "MatchedPairsOddsRatio"

        };


        for (int i = 0;
             i < headings.length;
             i++) {


            header.createCell(i)
                  .setCellValue(
                          headings[i]
                  );

        }


        int outputRow =
                1;


        for (int rowNumber = 1;
             rowNumber <= inputSheet.getLastRowNum();
             rowNumber++) {


            Row source =
                    inputSheet.getRow(
                            rowNumber
                    );


            if (source == null) {

                continue;

            }


            Row target =
                    outputSheet.createRow(
                            outputRow
                    );


            /*
             * HitFailureMcNemarResults.xlsx:
             *
             * A ReferenceAlgorithm
             * B BaselineAlgorithm
             * C PairedConcepts
             * D BothHit
             * E RomanHitBaselineFail
             * F RomanFailBaselineHit
             * G BothFail
             * H DiscordantPairs
             * I RomanConceptHitRate
             * J BaselineConceptHitRate
             * K HitRateDifference
             * L McNemarChiSquare
             * M RawPValue
             * N MatchedPairsOddsRatio
             * O HolmRank
             * P HolmMultiplier
             * Q HolmAdjustedPValue
             * R SignificantAt0.05
             */


            target.createCell(0)
                  .setCellValue(
                          getString(source, 1)
                  );


            target.createCell(1)
                  .setCellValue(
                          getDouble(source, 2)
                  );


            target.createCell(2)
                  .setCellValue(
                          getDouble(source, 3)
                  );


            target.createCell(3)
                  .setCellValue(
                          getDouble(source, 4)
                  );


            target.createCell(4)
                  .setCellValue(
                          getDouble(source, 5)
                  );


            target.createCell(5)
                  .setCellValue(
                          getDouble(source, 6)
                  );


            target.createCell(6)
                  .setCellValue(
                          getDouble(source, 7)
                  );


            target.createCell(7)
                  .setCellValue(
                          getDouble(source, 8)
                  );


            target.createCell(8)
                  .setCellValue(
                          getDouble(source, 9)
                  );


            target.createCell(9)
                  .setCellValue(
                          getDouble(source, 10)
                  );


            target.createCell(10)
                  .setCellValue(
                          getDouble(source, 11)
                  );


            target.createCell(11)
                  .setCellValue(
                          getDouble(source, 12)
                  );


            target.createCell(12)
                  .setCellValue(
                          getDouble(source, 16)
                  );


            target.createCell(13)
                  .setCellValue(
                          getBoolean(
                                  source,
                                  17
                          )
                  );


            target.createCell(14)
                  .setCellValue(
                          getDouble(source, 13)
                  );


            outputRow++;

        }


        inputWorkbook.close();

        input.close();


        outputSheet.createFreezePane(
                0,
                1
        );


        for (int column = 0;
             column < headings.length;
             column++) {


            outputSheet.autoSizeColumn(
                    column
            );

        }


        System.out.println(
                "Hit McNemar Summary rows: "
                + (outputRow - 1)
        );


        if ((outputRow - 1) != 9) {


            System.out.println(
                    "WARNING: Expected 9 McNemar rows."
            );

        }

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
    // Compatible with older Apache POI.
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
    // GET BOOLEAN
    // ========================================================

    private static boolean getBoolean(
            Row row,
            int column) {


        Cell cell =
                row.getCell(
                        column
                );


        if (cell == null) {

            return false;

        }


        if (cell.getCellType()
                == Cell.CELL_TYPE_BOOLEAN) {


            return cell.getBooleanCellValue();

        }


        String value =
                DATA_FORMATTER
                .formatCellValue(
                        cell
                )
                .trim();


        return value.equalsIgnoreCase(
                "true"
        )
        ||
        value.equalsIgnoreCase(
                "yes"
        )
        ||
        value.equals(
                "1"
        );

    }


    // ========================================================
    // DESCRIPTIVE RESULT CONTAINER
    // ========================================================

    private static class DescriptiveResult {


        String algorithm;


        double trials;


        double hitRate;

        double hitLower;

        double hitUpper;

        double hitSE;


        double precision;

        double precisionLower;

        double precisionUpper;

        double precisionSE;


        double recall;

        double recallLower;

        double recallUpper;

        double recallSE;


        double f1;

        double f1Lower;

        double f1Upper;

        double f1SE;


        double meanCandidateSet;

        double medianCandidateSet;

        double sdCandidateSet;

        double q1CandidateSet;

        double q3CandidateSet;

        double iqrCandidateSet;

        double minCandidateSet;

        double maxCandidateSet;

    }

}
