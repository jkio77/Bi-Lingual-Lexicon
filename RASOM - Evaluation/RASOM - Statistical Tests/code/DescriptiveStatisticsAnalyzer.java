package BORUE;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;



public class DescriptiveStatisticsAnalyzer {




    private static final String INPUT_FILE =
            "D:/BORUE/PhoneticEvaluation.xlsx";


    private static final String OUTPUT_FILE =
            "D:/BORUE/DescriptiveStatistics.xlsx";




    private static final int ALGORITHM_COLUMN = 0;        

    private static final int QUERY_COLUMN = 3;            

    private static final int RETRIEVED_COUNT_COLUMN = 6; 

    private static final int PRECISION_COLUMN = 8;       

    private static final int RECALL_COLUMN = 9;          

    private static final int F1_COLUMN = 10;             

    private static final int HIT_COLUMN = 11;            


    private static final DataFormatter DATA_FORMATTER =
            new DataFormatter();


    

    public static void main(String[] args) {

        try {

            System.out.println();

            System.out.println("==============================================");

            System.out.println("DESCRIPTIVE STATISTICS ANALYZER");

            System.out.println(
                    "=============================================="
            );

            System.out.println();




            Map<String, AlgorithmData> algorithmData =
                    readEvaluationFile();



            System.out.println();

            System.out.println(
                    "Algorithms found: "
                    + algorithmData.size()
            );


            if (algorithmData.size() != 10) {

                System.out.println();

                System.out.println(
                        "WARNING:"
                );

                System.out.println(
                        "Expected 10 algorithms but found "
                        + algorithmData.size()
                );
            }



            System.out.println();

            System.out.println(
                    "VALID TRIAL COUNTS PER ALGORITHM"
            );

            System.out.println(
                    "----------------------------------------------"
            );

            for (Map.Entry<String, AlgorithmData> entry
                    : algorithmData.entrySet()) {

                System.out.println(
                        entry.getKey()
                        + ": "
                        + entry.getValue().precisions.size()
                );
            }




            XSSFWorkbook outputWorkbook =
                    new XSSFWorkbook();


            Sheet summarySheet =
                    outputWorkbook.createSheet(
                            "Descriptive Statistics"
                    );


            createHeader(
                    summarySheet
            );


            int outputRowNumber = 1;



            for (Map.Entry<String, AlgorithmData> entry
                    : algorithmData.entrySet()) {


                String algorithm =
                        entry.getKey();


                AlgorithmData data =
                        entry.getValue();


                StatisticsResult result =
                        calculateStatistics(
                                algorithm,
                                data
                        );


                writeResult(
                        summarySheet,
                        outputRowNumber,
                        result
                );


                printResult(
                        result
                );


                outputRowNumber++;
            }



            summarySheet.createFreezePane(
                    0,
                    1
            );



            for (int column = 0;
                 column < 14;
                 column++) {

                summarySheet.autoSizeColumn(
                        column
                );
            }




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
                    "PROCESSING COMPLETED SUCCESSFULLY"
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

        }
        catch (Exception e) {

            e.printStackTrace();
        }
    }



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


        

        Map<String, AlgorithmData> algorithmData =
                new LinkedHashMap<String, AlgorithmData>();


        int validRowsRead = 0;

        int blankQueryRowsSkipped = 0;


        

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


            

            String query =
                    getString(
                            row,
                            QUERY_COLUMN
                    );


            if (query.isEmpty()) {

                blankQueryRowsSkipped++;

                continue;
            }


            

            if (!algorithmData.containsKey(
                    algorithm)) {


                algorithmData.put(
                        algorithm,
                        new AlgorithmData()
                );
            }


            AlgorithmData data =
                    algorithmData.get(
                            algorithm
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


            double hit =
                    getDouble(
                            row,
                            HIT_COLUMN
                    );


            
            data.retrievedCounts.add(
                    retrievedCount
            );


            data.precisions.add(
                    precision
            );


            data.recalls.add(
                    recall
            );


            data.f1Scores.add(
                    f1
            );


            data.hits.add(
                    hit
            );


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
                + (validRowsRead + blankQueryRowsSkipped)
        );


        return algorithmData;
    }


    

    private static StatisticsResult calculateStatistics(
            String algorithm,
            AlgorithmData data) {


        StatisticsResult result =
                new StatisticsResult();


        result.algorithm =
                algorithm;


        result.trials =
                data.precisions.size();


        

        result.hitRate =
                mean(
                        data.hits
                );


        result.macroPrecision =
                mean(
                        data.precisions
                );


        result.macroRecall =
                mean(
                        data.recalls
                );


        result.macroF1 =
                mean(
                        data.f1Scores
                );


        

        result.meanCandidateSet =
                mean(
                        data.retrievedCounts
                );


        result.medianCandidateSet =
                percentile(
                        data.retrievedCounts,
                        50.0
                );


        result.sdCandidateSet =
                sampleStandardDeviation(
                        data.retrievedCounts
                );


        result.q1CandidateSet =
                percentile(
                        data.retrievedCounts,
                        25.0
                );


        result.q3CandidateSet =
                percentile(
                        data.retrievedCounts,
                        75.0
                );


        result.iqrCandidateSet =
                result.q3CandidateSet
                -
                result.q1CandidateSet;


        result.minCandidateSet =
                minimum(
                        data.retrievedCounts
                );


        result.maxCandidateSet =
                maximum(
                        data.retrievedCounts
                );


        return result;
    }


    

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


    

    private static double sampleStandardDeviation(
            List<Double> values) {


        int n =
                values.size();


        if (n <= 1) {

            return 0.0;
        }


        double mean =
                mean(
                        values
                );


        double sumSquaredDifference =
                0.0;


        for (double value : values) {


            double difference =
                    value - mean;


            sumSquaredDifference +=
                    difference
                    *
                    difference;
        }


        return Math.sqrt(
                sumSquaredDifference
                /
                (n - 1)
        );
    }


    

    private static double percentile(
            List<Double> values,
            double percentile) {


        if (values.isEmpty()) {

            return 0.0;
        }


        List<Double> sorted =
                new ArrayList<Double>(
                        values
                );


        Collections.sort(
                sorted
        );


        if (sorted.size() == 1) {

            return sorted.get(0);
        }


        double position =
                (percentile / 100.0)
                *
                (sorted.size() - 1);


        int lowerIndex =
                (int) Math.floor(
                        position
                );


        int upperIndex =
                (int) Math.ceil(
                        position
                );


        if (lowerIndex == upperIndex) {

            return sorted.get(
                    lowerIndex
            );
        }


        double lowerValue =
                sorted.get(
                        lowerIndex
                );


        double upperValue =
                sorted.get(
                        upperIndex
                );


        double fraction =
                position
                -
                lowerIndex;


        return lowerValue
                +
                fraction
                *
                (upperValue - lowerValue);
    }


    

    private static double minimum(
            List<Double> values) {


        if (values.isEmpty()) {

            return 0.0;
        }


        double min =
                Double.POSITIVE_INFINITY;


        for (double value : values) {


            if (value < min) {

                min = value;
            }
        }


        return min;
    }


    

    private static double maximum(
            List<Double> values) {


        if (values.isEmpty()) {

            return 0.0;
        }


        double max =
                Double.NEGATIVE_INFINITY;


        for (double value : values) {


            if (value > max) {

                max = value;
            }
        }


        return max;
    }


    private static void writeResult(
            Sheet sheet,
            int rowNumber,
            StatisticsResult result) {


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
                   result.macroPrecision
           );


        row.createCell(4)
           .setCellValue(
                   result.macroRecall
           );


        row.createCell(5)
           .setCellValue(
                   result.macroF1
           );


        row.createCell(6)
           .setCellValue(
                   result.meanCandidateSet
           );


        row.createCell(7)
           .setCellValue(
                   result.medianCandidateSet
           );


        row.createCell(8)
           .setCellValue(
                   result.sdCandidateSet
           );


        row.createCell(9)
           .setCellValue(
                   result.q1CandidateSet
           );


        row.createCell(10)
           .setCellValue(
                   result.q3CandidateSet
           );


        row.createCell(11)
           .setCellValue(
                   result.iqrCandidateSet
           );


        row.createCell(12)
           .setCellValue(
                   result.minCandidateSet
           );


        row.createCell(13)
           .setCellValue(
                   result.maxCandidateSet
           );
    }


    


    private static void createHeader(
            Sheet sheet) {


        Row row =
                sheet.createRow(0);


        String[] headings = {

                "Algorithm",
                "Trials",
                "HitRate",
                "MacroPrecision",
                "MacroRecall",
                "MacroF1",
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


            row.createCell(i)
               .setCellValue(
                       headings[i]
               );
        }
    }


    

    private static void printResult(
            StatisticsResult result) {


        System.out.println();

        System.out.println(
                "----------------------------------------------"
        );


        System.out.println(
                result.algorithm
        );


        System.out.println(
                "----------------------------------------------"
        );


        System.out.println(
                "Valid Trials: "
                + result.trials
        );


        System.out.println(
                "Hit Rate: "
                + result.hitRate
        );


        System.out.println(
                "Macro Precision: "
                + result.macroPrecision
        );


        System.out.println(
                "Macro Recall: "
                + result.macroRecall
        );


        System.out.println(
                "Macro F1: "
                + result.macroF1
        );


        System.out.println(
                "Mean Candidate Set: "
                + result.meanCandidateSet
        );


        System.out.println(
                "Median Candidate Set: "
                + result.medianCandidateSet
        );


        System.out.println(
                "Candidate Set SD: "
                + result.sdCandidateSet
        );


        System.out.println(
                "Candidate Set Q1: "
                + result.q1CandidateSet
        );


        System.out.println(
                "Candidate Set Q3: "
                + result.q3CandidateSet
        );


        System.out.println(
                "Candidate Set IQR: "
                + result.iqrCandidateSet
        );


        System.out.println(
                "Candidate Set Min: "
                + result.minCandidateSet
        );


        System.out.println(
                "Candidate Set Max: "
                + result.maxCandidateSet
        );
    }


    

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


    
    private static class AlgorithmData {


        List<Double> retrievedCounts =
                new ArrayList<Double>();


        List<Double> precisions =
                new ArrayList<Double>();


        List<Double> recalls =
                new ArrayList<Double>();


        List<Double> f1Scores =
                new ArrayList<Double>();


        List<Double> hits =
                new ArrayList<Double>();
    }


    

    private static class StatisticsResult {


        String algorithm;

        int trials;

        double hitRate;

        double macroPrecision;

        double macroRecall;

        double macroF1;

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