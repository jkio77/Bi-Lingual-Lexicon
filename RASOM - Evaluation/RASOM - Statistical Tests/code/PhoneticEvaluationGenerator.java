package org.borue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//...............This is 1st step of statistical pipeline

/**
 * ============================================================
 * PhoneticEvaluationGenerator
 * ============================================================
 *
 * Creates one standardized evaluation dataset from the result
 * files of all 10 phonetic retrieval methods.
 *
 * INPUT STRUCTURE OF EACH RESULT FILE
 * -----------------------------------
 *
 * A-F = Six Roman Urdu query/spelling variants
 * G   = BORUE
 * H   = Gold relevant Count
 * I-N = Retrieved results corresponding to A-F
 *
 * Row 1     = headings
 * Rows 2... = concepts
 *
 *
 * OUTPUT
 * ------
 *
 * One row represents:
 *
 *     ONE algorithm
 *     x ONE concept
 *     x ONE spelling variant
 *
 * Output columns:
 *
 * A = Algorithm
 * B = ConceptRow
 * C = VariantNo
 * D = Query
 * E = BORUE
 * F = GoldCount
 * G = RetrievedCount
 * H = RelevantRetrieved
 * I = Precision
 * J = Recall
 * K = F1
 * L = Hit
 *
 *
 * RELEVANCE RULE
 * --------------
 *
 * A retrieved result is relevant if BORUE occurs as a
 * continuous character block in the retrieved string.
 *
 * Matching is:
 *
 *     - case insensitive
 *     - leading/trailing whitespace ignored
 *     - repeated whitespace normalized
 *
 * Example:
 *
 * BORUE = "aadat"
 *
 *     aadat             -> relevant
 *     buri aadat        -> relevant
 *     aadati            -> relevant
 *     aadat dalna       -> relevant
 *
 *
 * BORUE = "ada kaar"
 *
 *     ada kaar          -> relevant
 *     mashhoor ada kaar -> relevant
 *     ada kaari         -> relevant
 *     ada mat kaar      -> NOT relevant
 *
 *
 * BORUE ALTERNATIVES
 * ------------------
 *
 * Comma separates alternatives:
 *
 *     abad,aabaad
 *
 * A retrieved result is relevant if ANY alternative occurs
 * as a continuous character block.
 *
 * A retrieved result can contribute at most ONE relevant hit,
 * even if more than one alternative matches it.
 *
 *
 * METRICS
 * -------
 *
 * TP = number of retrieved results relevant to BORUE
 *
 * N  = number of retrieved results
 *
 * G  = gold Count
 *
 * Precision = TP / N
 *
 * Recall = TP / G
 *
 * F1 = 2PR / (P + R)
 *
 * Hit = 1 if TP > 0
 *       0 otherwise
 *
 *
 * IMPORTANT
 * ---------
 *
 * This class performs NO statistical significance testing.
 *
 * It generates the standardized per-query dataset that will
 * later be used by:
 *
 *     - descriptive statistics
 *     - bootstrap confidence intervals
 *     - Wilcoxon signed-rank tests
 *     - McNemar tests
 *     - Holm correction
 *     - effect-size calculations
 *
 * ============================================================
 */
public class PhoneticEvaluationGenerator {


    // ========================================================
    // CONFIGURATION
    // ========================================================

    private static final String BASE_PATH =
            "D:/BORUE/";


    private static final String OUTPUT_FILE = BASE_PATH + "PhoneticEvaluation.xlsx";


    /**
     * One configuration object for each algorithm.
     */
    private static final AlgorithmFile[] ALGORITHMS = {

        new AlgorithmFile(
                "Soundex",
                BASE_PATH + "Soundex_Result.xlsx"
        ),

        new AlgorithmFile(
                "Refined Soundex",
                BASE_PATH + "RefSound_Result.xlsx"
        ),

        new AlgorithmFile(
                "Cologne",
                BASE_PATH + "Cologne_Result.xlsx"
        ),

        new AlgorithmFile(
                "NYSIIS",
                BASE_PATH + "NYSIIS_Result.xlsx"
        ),

        new AlgorithmFile(
                "Metaphone",
                BASE_PATH + "Metaphone_Result.xlsx"
        ),

        new AlgorithmFile(
                "Double Metaphone",
                BASE_PATH + "DblMet_Result.xlsx"
        ),

        new AlgorithmFile(
                "Caverphone1",
                BASE_PATH + "Caverphone1_Result.xlsx"
        ),

        new AlgorithmFile(
                "Caverphone2",
                BASE_PATH + "Caverphone2_Result.xlsx"
        ),

        new AlgorithmFile(
                "UrduPhone",
                BASE_PATH + "UrduPhone_Result.xlsx"
        ),

        new AlgorithmFile(
                "RomanMetaphone",
                BASE_PATH + "Rumeta_Result.xlsx"
        )
    };


    // ========================================================
    // INPUT COLUMN NUMBERS
    //
    // Apache POI uses ZERO-BASED columns.
    // ========================================================

    private static final int FIRST_QUERY_COLUMN = 0; // A

    private static final int QUERY_VARIANT_COUNT = 6;

    private static final int BORUE_COLUMN = 6;       // G

    private static final int GOLD_COUNT_COLUMN = 7;  // H

    private static final int FIRST_RESULT_COLUMN = 8; // I


    // ========================================================
    // DATA FORMATTER
    // ========================================================

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
                    "PHONETIC EVALUATION GENERATOR"
            );

            System.out.println(
                    "=============================================="
            );

            System.out.println();


            // ------------------------------------------------
            // Validate all input files before starting.
            // ------------------------------------------------

            validateInputFiles();


            // ------------------------------------------------
            // Create output workbook.
            // ------------------------------------------------

            XSSFWorkbook outputWorkbook =
                    new XSSFWorkbook();


            Sheet outputSheet =
                    outputWorkbook.createSheet(
                            "Evaluation"
                    );


            // ------------------------------------------------
            // Create headings.
            // ------------------------------------------------

            createHeader(outputSheet);


            // First output row after header.
            int outputRowNumber = 1;


            // ------------------------------------------------
            // Process all 10 algorithms.
            // ------------------------------------------------

            for (AlgorithmFile algorithm : ALGORITHMS) {


                System.out.println();
                System.out.println(
                        "----------------------------------------------"
                );

                System.out.println(
                        "Processing: "
                        + algorithm.algorithmName
                );

                System.out.println(
                        "File: "
                        + algorithm.filePath
                );

                System.out.println(
                        "----------------------------------------------"
                );


                outputRowNumber =
                        processAlgorithm(
                                algorithm,
                                outputSheet,
                                outputRowNumber
                        );


                System.out.println(
                        algorithm.algorithmName
                        + " completed."
                );

            }


            // ------------------------------------------------
            // Freeze header row.
            // ------------------------------------------------

            outputSheet.createFreezePane(
                    0,
                    1
            );


            // ------------------------------------------------
            // Save output workbook.
            // ------------------------------------------------

            System.out.println();
            System.out.println(
                    "Saving evaluation dataset..."
            );


            FileOutputStream output =
                    new FileOutputStream(
                            OUTPUT_FILE
                    );


            outputWorkbook.write(output);


            output.close();

            outputWorkbook.close();


            // ------------------------------------------------
            // Final report.
            // ------------------------------------------------

            int totalEvaluationRows =
                    outputRowNumber - 1;


            System.out.println();
            System.out.println(
                    "=============================================="
            );

            System.out.println(
                    "PROCESSING COMPLETED SUCCESSFULLY"
            );

            System.out.println(
                    "=============================================="
            );

            System.out.println();

            System.out.println(
                    "Algorithms processed: "
                    + ALGORITHMS.length
            );

            System.out.println(
                    "Evaluation rows generated: "
                    + totalEvaluationRows
            );

            System.out.println();

            System.out.println(
                    "Expected for 2441 concepts:"
            );

            System.out.println(
                    "10 algorithms x 2441 concepts x 6 variants"
            );

            System.out.println(
                    "= 146460 evaluation rows"
            );

            System.out.println();

            System.out.println(
                    "Output file:"
            );

            System.out.println(
                    OUTPUT_FILE
            );

        }
        catch (Exception e) {

            e.printStackTrace();

        }

    }


    // ========================================================
    // PROCESS ONE ALGORITHM
    // ========================================================

    private static int processAlgorithm(
            AlgorithmFile algorithm,
            Sheet outputSheet,
            int outputRowNumber)
            throws Exception {


        FileInputStream input =
                new FileInputStream(
                        algorithm.filePath
                );


        XSSFWorkbook workbook =
                new XSSFWorkbook(
                        input
                );


        Sheet sheet =
                workbook.getSheetAt(0);


        int conceptsProcessed = 0;

        int variantsProcessed = 0;


        // ----------------------------------------------------
        // Start from row 1 because row 0 contains headings.
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


            // ------------------------------------------------
            // Read BORUE.
            // ------------------------------------------------

            String borue =
                    getCellString(
                            row,
                            BORUE_COLUMN
                    );


            // ------------------------------------------------
            // Read gold Count.
            // ------------------------------------------------

            int goldCount =
                    getIntegerCellValue(
                            row,
                            GOLD_COUNT_COLUMN
                    );


            // ------------------------------------------------
            // We expect BORUE for every concept.
            // ------------------------------------------------

            if (borue.isEmpty()) {


                System.out.println(
                        "WARNING: Empty BORUE at Excel row "
                        + (rowNumber + 1)
                        + " in "
                        + algorithm.algorithmName
                );


                continue;

            }


            // ------------------------------------------------
            // Gold Count should normally be > 0.
            // ------------------------------------------------

            if (goldCount <= 0) {


                System.out.println(
                        "WARNING: Gold Count <= 0 at Excel row "
                        + (rowNumber + 1)
                        + " | BORUE = "
                        + borue
                        + " | Algorithm = "
                        + algorithm.algorithmName
                );

            }


            // ------------------------------------------------
            // Process six query variants.
            // ------------------------------------------------

            for (int variantIndex = 0;
                 variantIndex < QUERY_VARIANT_COUNT;
                 variantIndex++) {


                int queryColumn =
                        FIRST_QUERY_COLUMN
                        + variantIndex;


                int resultColumn =
                        FIRST_RESULT_COLUMN
                        + variantIndex;


                String query =
                        getCellString(
                                row,
                                queryColumn
                        );


                String resultCell =
                        getCellString(
                                row,
                                resultColumn
                        );


                // --------------------------------------------
                // Parse retrieved candidates.
                // --------------------------------------------

                List<String> retrieved =
                        parseRetrievedResults(
                                resultCell
                        );


                int retrievedCount =
                        retrieved.size();


                // --------------------------------------------
                // Count relevant retrieved candidates.
                // --------------------------------------------

                int relevantRetrieved =
                        countRelevantRetrieved(
                                retrieved,
                                borue
                        );


                // --------------------------------------------
                // Calculate metrics.
                // --------------------------------------------

                double precision = 0.0;

                double recall = 0.0;

                double f1 = 0.0;


                if (retrievedCount > 0) {

                    precision =
                            (double) relevantRetrieved
                            /
                            retrievedCount;

                }


                if (goldCount > 0) {

                    recall =
                            (double) relevantRetrieved
                            /
                            goldCount;

                }


                if ((precision + recall) > 0.0) {

                    f1 =
                            (2.0
                             * precision
                             * recall)
                            /
                            (precision + recall);

                }


                int hit =
                        relevantRetrieved > 0
                        ? 1
                        : 0;


                // --------------------------------------------
                // Sanity check.
                //
                // TP must never exceed retrieved count.
                // --------------------------------------------

                if (relevantRetrieved
                        > retrievedCount) {


                    throw new IllegalStateException(
                            "RelevantRetrieved > RetrievedCount"
                            + "\nAlgorithm: "
                            + algorithm.algorithmName
                            + "\nExcel row: "
                            + (rowNumber + 1)
                            + "\nQuery: "
                            + query
                    );

                }


                // --------------------------------------------
                // Recall theoretically should not exceed 1.
                //
                // If this happens, there is disagreement
                // between gold Count generation and result
                // relevance calculation.
                // --------------------------------------------

                if (recall > 1.0000001) {


                    System.out.println();

                    System.out.println(
                            "WARNING: RECALL > 1"
                    );

                    System.out.println(
                            "Algorithm: "
                            + algorithm.algorithmName
                    );

                    System.out.println(
                            "Excel row: "
                            + (rowNumber + 1)
                    );

                    System.out.println(
                            "Variant: "
                            + (variantIndex + 1)
                    );

                    System.out.println(
                            "Query: "
                            + query
                    );

                    System.out.println(
                            "BORUE: "
                            + borue
                    );

                    System.out.println(
                            "Gold Count: "
                            + goldCount
                    );

                    System.out.println(
                            "Relevant Retrieved: "
                            + relevantRetrieved
                    );

                    System.out.println();

                }


                // --------------------------------------------
                // Write standardized output row.
                // --------------------------------------------

                Row outputRow =
                        outputSheet.createRow(
                                outputRowNumber
                        );


                writeEvaluationRow(
                        outputRow,
                        algorithm.algorithmName,
                        rowNumber + 1,
                        variantIndex + 1,
                        query,
                        borue,
                        goldCount,
                        retrievedCount,
                        relevantRetrieved,
                        precision,
                        recall,
                        f1,
                        hit
                );


                outputRowNumber++;

                variantsProcessed++;

            }


            conceptsProcessed++;


            if (conceptsProcessed % 500 == 0) {

                System.out.println(
                        "Concepts processed: "
                        + conceptsProcessed
                );

            }

        }


        workbook.close();

        input.close();


        System.out.println(
                "Concepts processed: "
                + conceptsProcessed
        );

        System.out.println(
                "Variant evaluations: "
                + variantsProcessed
        );


        return outputRowNumber;

    }


    // ========================================================
    // COUNT RELEVANT RETRIEVED RESULTS
    // ========================================================

    private static int countRelevantRetrieved(
            List<String> retrieved,
            String borue) {


        List<String> alternatives =
                getBorueAlternatives(
                        borue
                );


        int relevantCount = 0;


        for (String candidate : retrieved) {


            String normalizedCandidate =
                    normalize(
                            candidate
                    );


            if (normalizedCandidate.isEmpty()) {

                continue;

            }


            boolean relevant = false;


            // ------------------------------------------------
            // OR relationship between BORUE alternatives.
            //
            // Candidate contributes only once even if multiple
            // alternatives match it.
            // ------------------------------------------------

            for (String alternative : alternatives) {


                if (normalizedCandidate.contains(
                        alternative)) {


                    relevant = true;

                    break;

                }

            }


            if (relevant) {

                relevantCount++;

            }

        }


        return relevantCount;

    }


    // ========================================================
    // BORUE ALTERNATIVES
    // ========================================================

    private static List<String> getBorueAlternatives(
            String borue) {


        List<String> alternatives =
                new ArrayList<String>();


        if (borue == null) {

            return alternatives;

        }


        // ----------------------------------------------------
        // ONLY comma separates alternatives.
        //
        // Spaces DO NOT.
        //
        // "ada kaar" remains one phrase.
        //
        // "abad,aabaad" becomes:
        //
        //     abad
        //     aabaad
        // ----------------------------------------------------

        String[] pieces =
                borue.split(",");


        for (String piece : pieces) {


            String normalized =
                    normalize(
                            piece
                    );


            if (!normalized.isEmpty()
                    &&
                !alternatives.contains(
                        normalized)) {


                alternatives.add(
                        normalized
                );

            }

        }


        return alternatives;

    }


    // ========================================================
    // PARSE RETRIEVED RESULTS
    // ========================================================

    private static List<String> parseRetrievedResults(
            String resultCell) {


        List<String> results =
                new ArrayList<String>();


        if (resultCell == null
                ||
            resultCell.trim().isEmpty()) {


            return results;

        }


        // ----------------------------------------------------
        // Result candidates are comma-separated.
        //
        // Handles:
        //
        // word1, word2, word3
        //
        // and:
        //
        // word1,word2,word3
        // ----------------------------------------------------

        String[] pieces =
                resultCell.split(
                        "\\s*,\\s*"
                );


        for (String piece : pieces) {


            String value =
                    piece.trim();


            if (!value.isEmpty()) {

                results.add(
                        value
                );

            }

        }


        return results;

    }


    // ========================================================
    // NORMALIZATION
    // ========================================================

    private static String normalize(
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


    // ========================================================
    // WRITE ONE EVALUATION ROW
    // ========================================================

    private static void writeEvaluationRow(
            Row row,
            String algorithm,
            int conceptRow,
            int variantNo,
            String query,
            String borue,
            int goldCount,
            int retrievedCount,
            int relevantRetrieved,
            double precision,
            double recall,
            double f1,
            int hit) {


        row.createCell(0)
           .setCellValue(
                   algorithm
           );


        row.createCell(1)
           .setCellValue(
                   conceptRow
           );


        row.createCell(2)
           .setCellValue(
                   variantNo
           );


        row.createCell(3)
           .setCellValue(
                   query
           );


        row.createCell(4)
           .setCellValue(
                   borue
           );


        row.createCell(5)
           .setCellValue(
                   goldCount
           );


        row.createCell(6)
           .setCellValue(
                   retrievedCount
           );


        row.createCell(7)
           .setCellValue(
                   relevantRetrieved
           );


        row.createCell(8)
           .setCellValue(
                   precision
           );


        row.createCell(9)
           .setCellValue(
                   recall
           );


        row.createCell(10)
           .setCellValue(
                   f1
           );


        row.createCell(11)
           .setCellValue(
                   hit
           );

    }


    // ========================================================
    // CREATE OUTPUT HEADER
    // ========================================================

    private static void createHeader(
            Sheet sheet) {


        Row header =
                sheet.createRow(0);


        String[] headings = {

                "Algorithm",
                "ConceptRow",
                "VariantNo",
                "Query",
                "BORUE",
                "GoldCount",
                "RetrievedCount",
                "RelevantRetrieved",
                "Precision",
                "Recall",
                "F1",
                "Hit"

        };


        for (int i = 0;
             i < headings.length;
             i++) {


            header.createCell(i)
                  .setCellValue(
                          headings[i]
                  );

        }

    }


    // ========================================================
    // READ CELL AS STRING
    // ========================================================

    private static String getCellString(
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
    // READ INTEGER CELL
    // ========================================================

    private static int getIntegerCellValue(
            Row row,
            int column) {


        Cell cell =
                row.getCell(
                        column
                );


        if (cell == null) {

            return 0;

        }


        // ----------------------------------------------------
        // Numeric Excel cell.
        // ----------------------------------------------------

        if (cell.getCellType()
                == Cell.CELL_TYPE_NUMERIC) {

            return (int)
                    Math.round(
                            cell.getNumericCellValue()
                    );
        }


        // ----------------------------------------------------
        // String/formatted cell.
        // ----------------------------------------------------

        String value =
                DATA_FORMATTER
                .formatCellValue(
                        cell
                )
                .trim();


        if (value.isEmpty()) {

            return 0;

        }


        try {


            return (int)
                    Math.round(
                            Double.parseDouble(
                                    value
                            )
                    );

        }
        catch (NumberFormatException e) {


            throw new IllegalArgumentException(
                    "Invalid Gold Count: "
                    + value
                    + " at Excel row "
                    + (row.getRowNum() + 1)
            );

        }

    }


    // ========================================================
    // VALIDATE INPUT FILES
    // ========================================================

    private static void validateInputFiles() {


        for (AlgorithmFile algorithm : ALGORITHMS) {


            File file =
                    new File(
                            algorithm.filePath
                    );


            if (!file.exists()) {


                throw new IllegalArgumentException(
                        "Input file not found:"
                        + "\n"
                        + algorithm.filePath
                );

            }

        }

    }


    // ========================================================
    // ALGORITHM FILE CONFIGURATION
    // ========================================================

    private static class AlgorithmFile {


        String algorithmName;

        String filePath;


        AlgorithmFile(
                String algorithmName,
                String filePath) {


            this.algorithmName =
                    algorithmName;


            this.filePath =
                    filePath;

        }

    }

}
