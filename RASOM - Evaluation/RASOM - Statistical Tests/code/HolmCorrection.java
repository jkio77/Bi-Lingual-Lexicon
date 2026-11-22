package BORUE;

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





public class HolmCorrection {




    private static final String INPUT_FILE =
            "D:/BORUE/WilcoxonPairwiseResults.xlsx";


    private static final String OUTPUT_FILE =
            "D:/BORUE/WilcoxonHolmCorrected.xlsx";




    private static final double ALPHA = 0.05;



    private static final int METRIC_COLUMN = 2;       

    private static final int RAW_P_COLUMN = 13;       


    private static final DataFormatter DATA_FORMATTER =
            new DataFormatter();


    

    public static void main(String[] args) {


        try {


            System.out.println();

            System.out.println(
                    "=============================================="
            );

            System.out.println(
                    "HOLM MULTIPLE-COMPARISON CORRECTION"
            );

            System.out.println(
                    "=============================================="
            );

            System.out.println();


            // ------------------------------------------------
            // Read Wilcoxon results.
            // ------------------------------------------------

            List<TestResult> tests =
                    readWilcoxonResults();


            System.out.println(
                    "Wilcoxon tests read: "
                    + tests.size()
            );


            if (tests.size() != 36) {


                System.out.println(
                        "WARNING: Expected 36 Wilcoxon tests."
                );

            }


    

            Map<String, List<TestResult>> families =
                    groupByMetric(
                            tests
                    );


            System.out.println();

            System.out.println(
                    "Metric families found: "
                    + families.size()
            );


    

            for (Map.Entry<String, List<TestResult>> entry
                    : families.entrySet()) {


                String metric =
                        entry.getKey();


                List<TestResult> family =
                        entry.getValue();


                System.out.println();

                System.out.println(
                        "----------------------------------------------"
                );

                System.out.println(
                        "Metric family: "
                        + metric
                );


                System.out.println(
                        "Comparisons: "
                        + family.size()
                );


                if (family.size() != 9) {


                    System.out.println(
                            "WARNING: Expected 9 comparisons "
                            + "for this metric family."
                    );

                }


                applyHolmCorrection(
                        family
                );


                printFamily(
                        family
                );

            }


    
            writeOutput(
                    tests
            );


            System.out.println();

            System.out.println("==============================================");

            System.out.println("HOLM CORRECTION COMPLETED SUCCESSFULLY");

            System.out.println("==============================================");

            System.out.println();

            System.out.println("Output file:");

            System.out.println(OUTPUT_FILE);


            System.out.println();

            System.out.println(
                    "Expected output:"
            );

            System.out.println(
                    "36 data rows + 1 heading = 37 rows"
            );


        }
        catch (Exception e) {


            e.printStackTrace();

        }

    }


    
    private static List<TestResult>
            readWilcoxonResults()
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


        List<TestResult> tests =
                new ArrayList<TestResult>();


    
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


            String metric =
                    getString(
                            row,
                            METRIC_COLUMN
                    );


            if (metric.isEmpty()) {

                continue;

            }


            TestResult result =
                    new TestResult();


    
            result.referenceAlgorithm =
                    getString(row, 0);


            result.baselineAlgorithm =
                    getString(row, 1);


            result.metric =
                    metric;


            result.pairedConcepts =
                    getDouble(row, 3);


            result.nonZeroPairs =
                    getDouble(row, 4);


            result.zeroDifferences =
                    getDouble(row, 5);


            result.romanMean =
                    getDouble(row, 6);


            result.baselineMean =
                    getDouble(row, 7);


            result.meanDifference =
                    getDouble(row, 8);


            result.wPlus =
                    getDouble(row, 9);


            result.wMinus =
                    getDouble(row, 10);


            result.wStatistic =
                    getDouble(row, 11);


            result.z =
                    getDouble(row, 12);


            result.rawPValue =
                    getDouble(
                            row,
                            RAW_P_COLUMN
                    );


            result.rankBiserialCorrelation =
                    getDouble(row, 14);


    
            result.originalOrder =
                    tests.size();


            tests.add(
                    result
            );

        }


        workbook.close();

        input.close();


        return tests;

    }


    
    private static Map<String, List<TestResult>>
            groupByMetric(
                    List<TestResult> tests) {


        Map<String, List<TestResult>> families =
                new LinkedHashMap<String, List<TestResult>>();


        for (TestResult test : tests) {


            List<TestResult> family =
                    families.get(
                            test.metric
                    );


            if (family == null) {


                family =
                        new ArrayList<TestResult>();


                families.put(
                        test.metric,
                        family
                );

            }


            family.add(
                    test
            );

        }


        return families;

    }


    
    private static void applyHolmCorrection(
            List<TestResult> family) {


    
        List<TestResult> sorted =
                new ArrayList<TestResult>(
                        family
                );


    
        Collections.sort(
                sorted,
                new Comparator<TestResult>() {

                    @Override
                    public int compare(
                            TestResult a,
                            TestResult b) {


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


            TestResult test =
                    sorted.get(i);


    
            int multiplier =
                    m - i;


            double preliminaryAdjusted =
                    test.rawPValue
                    *
                    multiplier;


    
            if (preliminaryAdjusted > 1.0) {


                preliminaryAdjusted =
                        1.0;

            }


   
            double adjusted =
                    Math.max(
                            previousAdjusted,
                            preliminaryAdjusted
                    );


            if (adjusted > 1.0) {


                adjusted =
                        1.0;

            }


            test.holmAdjustedPValue =
                    adjusted;


            test.significant =
                    adjusted <= ALPHA;


   
            test.holmRank =
                    i + 1;


            test.holmMultiplier =
                    multiplier;


            previousAdjusted =
                    adjusted;

        }

    }


   
    private static void printFamily(
            List<TestResult> family) {


   
        List<TestResult> sorted =
                new ArrayList<TestResult>(
                        family
                );


        Collections.sort(
                sorted,
                new Comparator<TestResult>() {

                    @Override
                    public int compare(
                            TestResult a,
                            TestResult b) {


                        return Integer.compare(
                                a.holmRank,
                                b.holmRank
                        );

                    }

                }
        );


        for (TestResult test : sorted) {


            System.out.println();


            System.out.println(
                    "Baseline: "
                    + test.baselineAlgorithm
            );


            System.out.println(
                    "Raw p-value: "
                    + test.rawPValue
            );


            System.out.println(
                    "Holm rank: "
                    + test.holmRank
            );


            System.out.println(
                    "Holm multiplier: "
                    + test.holmMultiplier
            );


            System.out.println(
                    "Holm-adjusted p-value: "
                    + test.holmAdjustedPValue
            );


            System.out.println(
                    "Significant at alpha = 0.05: "
                    + test.significant
            );

        }

    }


   
    private static void writeOutput(
            List<TestResult> tests)
            throws Exception {


   
        Collections.sort(
                tests,
                new Comparator<TestResult>() {

                    @Override
                    public int compare(
                            TestResult a,
                            TestResult b) {


                        return Integer.compare(
                                a.originalOrder,
                                b.originalOrder
                        );

                    }

                }
        );


        XSSFWorkbook workbook =
                new XSSFWorkbook();


        Sheet sheet =
                workbook.createSheet(
                        "Wilcoxon Holm Corrected"
                );


        createHeader(
                sheet
        );


        int rowNumber = 1;


        for (TestResult result : tests) {


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
                       result.metric
               );


            row.createCell(3)
               .setCellValue(
                       result.pairedConcepts
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
                       result.romanMean
               );


            row.createCell(7)
               .setCellValue(
                       result.baselineMean
               );


            row.createCell(8)
               .setCellValue(
                       result.meanDifference
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
                       result.rawPValue
               );


            row.createCell(14)
               .setCellValue(
                       result.rankBiserialCorrelation
               );


   
            row.createCell(15)
               .setCellValue(
                       result.holmRank
               );


            row.createCell(16)
               .setCellValue(
                       result.holmMultiplier
               );


            row.createCell(17)
               .setCellValue(
                       result.holmAdjustedPValue
               );


            row.createCell(18)
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
             column < 19;
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
                "RankBiserialCorrelation",

   

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


   
    private static class TestResult {


        String referenceAlgorithm;

        String baselineAlgorithm;

        String metric;


        double pairedConcepts;

        double nonZeroPairs;

        double zeroDifferences;


        double romanMean;

        double baselineMean;

        double meanDifference;


        double wPlus;

        double wMinus;

        double wStatistic;


        double z;

        double rawPValue;

        double rankBiserialCorrelation;



        int holmRank;

        int holmMultiplier;

        double holmAdjustedPValue;

        boolean significant;


        int originalOrder;

    }

}
