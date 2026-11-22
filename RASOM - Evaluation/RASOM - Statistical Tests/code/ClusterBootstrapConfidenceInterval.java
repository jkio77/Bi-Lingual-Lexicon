package BORUE;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;



public class ClusterBootstrapConfidenceInterval {




    private static final String INPUT_FILE =
            "D:/BORUE/PhoneticEvaluation.xlsx";


    private static final String OUTPUT_FILE =
            "D:/BORUE/BootstrapConfidenceIntervals.xlsx";


    private static final int BOOTSTRAP_REPLICATES = 10000;

    private static final int EXPECTED_CONCEPTS = 2441;

    private static final int EXPECTED_VALID_VARIANTS = 10161;


    private static final long RANDOM_SEED = 20260922L;



    private static final int ALGORITHM_COLUMN = 0;   

    private static final int CONCEPT_COLUMN = 1;     

    private static final int QUERY_COLUMN = 3;       

    private static final int PRECISION_COLUMN = 8;   

    private static final int RECALL_COLUMN = 9;      

    private static final int F1_COLUMN = 10;         

    private static final int HIT_COLUMN = 11;        


    private static final DataFormatter DATA_FORMATTER =
            new DataFormatter();




    public static void main(String[] args) {

        try {

            System.out.println();

            System.out.println(
                    "=============================================="
            );

            System.out.println(
                    "CLUSTER BOOTSTRAP CONFIDENCE INTERVAL"
            );

            System.out.println(
                    "=============================================="
            );

            System.out.println();

            System.out.println(
                    "Bootstrap replicates: "
                    + BOOTSTRAP_REPLICATES
            );

            System.out.println(
                    "Random seed: "
                    + RANDOM_SEED
            );

            System.out.println();



            Map<String, AlgorithmData> algorithms =
                    readEvaluationFile();


            System.out.println();

            System.out.println(
                    "Algorithms found: "
                    + algorithms.size()
            );


            if (algorithms.size() != 10) {

                System.out.println(
                        "WARNING: Expected 10 algorithms."
                );
            }



            XSSFWorkbook outputWorkbook =
                    new XSSFWorkbook();


            Sheet outputSheet =
                    outputWorkbook.createSheet(
                            "Bootstrap 95 CI"
                    );


            createHeader(
                    outputSheet
            );


            int outputRow = 1;

            for (Map.Entry<String, AlgorithmData> entry
                    : algorithms.entrySet()) {


                String algorithm =
                        entry.getKey();


                AlgorithmData data =
                        entry.getValue();


                System.out.println();

                System.out.println(
                        "----------------------------------------------"
                );

                System.out.println(
                        "Processing: " + algorithm
                );


                System.out.println(
                        "Concept clusters: "
                        + data.concepts.size()
                );


                int totalVariants =
                        countVariants(
                                data
                        );


                System.out.println(
                        "Valid variant observations: "
                        + totalVariants
                );


                if (data.concepts.size()
                        != EXPECTED_CONCEPTS) {

                    System.out.println(
                            "WARNING: Expected "
                            + EXPECTED_CONCEPTS
                            + " concept clusters."
                    );
                }


                if (totalVariants
                        != EXPECTED_VALID_VARIANTS) {

                    System.out.println(
                            "WARNING: Expected "
                            + EXPECTED_VALID_VARIANTS
                            + " valid variant observations."
                    );
                }


                

                MetricValues observed =
                        calculateObservedMetrics(
                                data
                        );


                

                BootstrapResult bootstrap =
                        performBootstrap(
                                algorithm,
                                data
                        );


                

                outputRow =
                        writeMetricResult(
                                outputSheet,
                                outputRow,
                                algorithm,
                                "Hit Rate",
                                observed.hitRate,
                                bootstrap.hitRates
                        );


                outputRow =
                        writeMetricResult(
                                outputSheet,
                                outputRow,
                                algorithm,
                                "Macro Precision",
                                observed.precision,
                                bootstrap.precisions
                        );


                outputRow =
                        writeMetricResult(
                                outputSheet,
                                outputRow,
                                algorithm,
                                "Macro Recall",
                                observed.recall,
                                bootstrap.recalls
                        );


                outputRow =
                        writeMetricResult(
                                outputSheet,
                                outputRow,
                                algorithm,
                                "Macro F1",
                                observed.f1,
                                bootstrap.f1Scores
                        );
            }


            

            outputSheet.createFreezePane(
                    0,
                    1
            );


            
            for (int column = 0;
                 column < 7;
                 column++) {

                outputSheet.autoSizeColumn(
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
                    "BOOTSTRAP COMPLETED SUCCESSFULLY"
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


        Map<String, AlgorithmData> algorithms =
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


            int conceptRow =
                    (int) Math.round(
                            getDouble(
                                    row,
                                    CONCEPT_COLUMN
                            )
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


            

            ConceptCluster concept =
                    algorithmData.concepts.get(
                            conceptRow
                    );


            if (concept == null) {


                concept =
                        new ConceptCluster(
                                conceptRow
                        );


                algorithmData.concepts.put(
                        conceptRow,
                        concept
                );
            }


            

            VariantMetric variant =
                    new VariantMetric();


            variant.precision =
                    precision;


            variant.recall =
                    recall;


            variant.f1 =
                    f1;


            variant.hit =
                    hit;


            concept.variants.add(
                    variant
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
                + (validRowsRead
                + blankQueryRowsSkipped)
        );


        

        validateClusters(
                algorithms
        );


        return algorithms;
    }


    

    private static void validateClusters(
            Map<String, AlgorithmData> algorithms) {


        for (Map.Entry<String, AlgorithmData> entry
                : algorithms.entrySet()) {


            String algorithm =
                    entry.getKey();


            AlgorithmData data =
                    entry.getValue();


            int invalidClusters = 0;


            int[] distribution =
                    new int[7];


            for (ConceptCluster concept
                    : data.concepts.values()) {


                int variantCount =
                        concept.variants.size();


                if (variantCount >= 0
                        && variantCount < distribution.length) {

                    distribution[variantCount]++;
                }


                if (variantCount < 2
                        || variantCount > 6) {


                    invalidClusters++;


                    System.out.println(
                            "WARNING: "
                            + algorithm
                            + " | ConceptRow "
                            + concept.conceptRow
                            + " has "
                            + variantCount
                            + " valid variants."
                    );
                }
            }


            System.out.println();

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


            if (invalidClusters == 0) {

                System.out.println(
                        algorithm
                        + ": all concept clusters contain "
                        + "between 2 and 6 valid variants."
                );
            }
            else {

                System.out.println(
                        "WARNING: "
                        + algorithm
                        + " contains "
                        + invalidClusters
                        + " invalid concept clusters."
                );
            }
        }
    }


    

    private static int countVariants(
            AlgorithmData data) {


        int count = 0;


        for (ConceptCluster concept
                : data.concepts.values()) {


            count +=
                    concept.variants.size();
        }


        return count;
    }


    

    private static MetricValues calculateObservedMetrics(
            AlgorithmData data) {


        double precisionSum = 0.0;

        double recallSum = 0.0;

        double f1Sum = 0.0;

        double hitSum = 0.0;


        int observations = 0;


        for (ConceptCluster concept
                : data.concepts.values()) {


            for (VariantMetric variant
                    : concept.variants) {


                precisionSum +=
                        variant.precision;


                recallSum +=
                        variant.recall;


                f1Sum +=
                        variant.f1;


                hitSum +=
                        variant.hit;


                observations++;
            }
        }


        MetricValues result =
                new MetricValues();


        if (observations == 0) {

            return result;
        }


        result.precision =
                precisionSum
                /
                observations;


        result.recall =
                recallSum
                /
                observations;


        result.f1 =
                f1Sum
                /
                observations;


        result.hitRate =
                hitSum
                /
                observations;


        return result;
    }


    

    private static BootstrapResult performBootstrap(
            String algorithm,
            AlgorithmData data) {


        List<ConceptCluster> concepts =
                new ArrayList<ConceptCluster>(
                        data.concepts.values()
                );


        int numberOfConcepts =
                concepts.size();


        BootstrapResult result =
                new BootstrapResult(
                        BOOTSTRAP_REPLICATES
                );


        /*
         * Deterministic algorithm-specific seed.
         */
        Random random =
                new Random(
                        RANDOM_SEED
                        + algorithm.hashCode()
                );


        

        for (int replicate = 0;
             replicate < BOOTSTRAP_REPLICATES;
             replicate++) {


            double precisionSum = 0.0;

            double recallSum = 0.0;

            double f1Sum = 0.0;

            double hitSum = 0.0;


            int observations = 0;


            /*
             * Sample exactly NC concept clusters
             * WITH replacement.
             */
            for (int sample = 0;
                 sample < numberOfConcepts;
                 sample++) {


                int selectedIndex =
                        random.nextInt(
                                numberOfConcepts
                        );


                ConceptCluster selectedConcept =
                        concepts.get(
                                selectedIndex
                        );


                /*
                 * CRITICAL:
                 *
                 * All AVAILABLE non-empty variants belonging
                 * to the selected concept enter together.
                 *
                 * Cluster sizes may therefore vary from
                 * 2 through 6.
                 */
                for (VariantMetric variant
                        : selectedConcept.variants) {


                    precisionSum +=
                            variant.precision;


                    recallSum +=
                            variant.recall;


                    f1Sum +=
                            variant.f1;


                    hitSum +=
                            variant.hit;


                    observations++;
                }
            }


            if (observations == 0) {

                throw new IllegalStateException(
                        "Bootstrap replicate contained "
                        + "zero valid observations."
                );
            }


            result.precisions[replicate] =
                    precisionSum
                    /
                    observations;


            result.recalls[replicate] =
                    recallSum
                    /
                    observations;


            result.f1Scores[replicate] =
                    f1Sum
                    /
                    observations;


            result.hitRates[replicate] =
                    hitSum
                    /
                    observations;


            if ((replicate + 1) % 1000 == 0) {

                System.out.println(
                        algorithm
                        + " bootstrap: "
                        + (replicate + 1)
                        + " / "
                        + BOOTSTRAP_REPLICATES
                );
            }
        }


        return result;
    }


    

    private static int writeMetricResult(
            Sheet sheet,
            int rowNumber,
            String algorithm,
            String metric,
            double estimate,
            double[] bootstrapValues) {


        double lowerCI =
                percentile(
                        bootstrapValues,
                        2.5
                );


        double upperCI =
                percentile(
                        bootstrapValues,
                        97.5
                );


        double bootstrapMean =
                mean(
                        bootstrapValues
                );


        double bootstrapSE =
                sampleStandardDeviation(
                        bootstrapValues
                );


        Row row =
                sheet.createRow(
                        rowNumber
                );


        row.createCell(0)
           .setCellValue(
                   algorithm
           );


        row.createCell(1)
           .setCellValue(
                   metric
           );


        row.createCell(2)
           .setCellValue(
                   estimate
           );


        row.createCell(3)
           .setCellValue(
                   lowerCI
           );


        row.createCell(4)
           .setCellValue(
                   upperCI
           );


        row.createCell(5)
           .setCellValue(
                   bootstrapMean
           );


        row.createCell(6)
           .setCellValue(
                   bootstrapSE
           );


        System.out.println();

        System.out.println(
                metric
        );


        System.out.println(
                "Observed estimate: "
                + estimate
        );


        System.out.println(
                "95% CI: ["
                + lowerCI
                + ", "
                + upperCI
                + "]"
        );


        System.out.println(
                "Bootstrap mean: "
                + bootstrapMean
        );


        System.out.println(
                "Bootstrap SE: "
                + bootstrapSE
        );


        return rowNumber + 1;
    }


    

    private static double percentile(
            double[] values,
            double percentile) {


        List<Double> sorted =
                new ArrayList<Double>(
                        values.length
                );


        for (double value : values) {

            sorted.add(
                    value
            );
        }


        Collections.sort(
                sorted
        );


        if (sorted.isEmpty()) {

            return 0.0;
        }


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


    

    private static double mean(
            double[] values) {


        if (values.length == 0) {

            return 0.0;
        }


        double sum = 0.0;


        for (double value : values) {

            sum += value;
        }


        return sum
                /
                values.length;
    }


    

    private static double sampleStandardDeviation(
            double[] values) {


        if (values.length <= 1) {

            return 0.0;
        }


        double mean =
                mean(
                        values
                );


        double squaredDifferenceSum =
                0.0;


        for (double value : values) {


            double difference =
                    value - mean;


            squaredDifferenceSum +=
                    difference
                    *
                    difference;
        }


        return Math.sqrt(
                squaredDifferenceSum
                /
                (values.length - 1)
        );
    }


      

    private static void createHeader(
            Sheet sheet) {


        Row row =
                sheet.createRow(0);


        String[] headings = {

                "Algorithm",
                "Metric",
                "ObservedEstimate",
                "CI95Lower",
                "CI95Upper",
                "BootstrapMean",
                "BootstrapSE"
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


    
    private static class AlgorithmData {


    
        Map<Integer, ConceptCluster> concepts =
                new LinkedHashMap<Integer, ConceptCluster>();
    }


    private static class ConceptCluster {


        int conceptRow;


        List<VariantMetric> variants =
                new ArrayList<VariantMetric>();


        ConceptCluster(
                int conceptRow) {


            this.conceptRow =
                    conceptRow;
        }
    }


    private static class VariantMetric {


        double precision;

        double recall;

        double f1;

        double hit;
    }


    private static class MetricValues {


        double hitRate;

        double precision;

        double recall;

        double f1;
    }


    private static class BootstrapResult {


        double[] hitRates;

        double[] precisions;

        double[] recalls;

        double[] f1Scores;


        BootstrapResult(
                int replicates) {


            hitRates =
                    new double[replicates];


            precisions =
                    new double[replicates];


            recalls =
                    new double[replicates];


            f1Scores =
                    new double[replicates];
        }
    }
}