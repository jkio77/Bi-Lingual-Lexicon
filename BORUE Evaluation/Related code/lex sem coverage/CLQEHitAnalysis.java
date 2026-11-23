package BORUE;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class CLQEHitAnalysis {

    private static final String DIRECTORY =  "D:\\BORUE\\";

    private static final String INPUT_FILE =  DIRECTORY + "NewTestBenchSort_Expanded.xlsx";

    private static final String OUTPUT_FILE =  DIRECTORY + "CLQE_Hit_Analysis.xlsx";

    private static final String[] HIT_COLUMNS = {
            "Hit_var1",
            "Hit_var2",
            "Hit_var3",
            "Hit_var4",
            "Hit_var5",
            "Hit_common"
    };

    private static final String[] LABELS = {
            "var1", "var2", "var3",
            "var4", "var5", "Common"
    };

    public static void main(String[] args) {

        try {
            analyze();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void analyze() throws Exception {

        DataFormatter formatter = new DataFormatter();

        int[] yesCounts = new int[6];
        int[] noCounts = new int[6];
        int[] distribution = new int[7];

        int totalQueries = 0;

        int completeSuccess = 0;
        int partialSuccess = 0;
        int completeFailure = 0;

        int canonicalSuccess = 0;
        int anyVariantSuccess = 0;
        int variantRecovery = 0;

        int totalSuccessfulVariants = 0;

        try (FileInputStream fis =  new FileInputStream(INPUT_FILE);
             XSSFWorkbook inputWorkbook =
                     new XSSFWorkbook(fis);
             XSSFWorkbook outputWorkbook =
                     new XSSFWorkbook()) {

            Sheet inputSheet = inputWorkbook.getSheetAt(0);
            Row header = inputSheet.getRow(0);

            if (header == null) {
                throw new IllegalArgumentException(
                        "Input workbook has no header row.");
            }

            int[] hitIndexes = new int[6];

            for (int j = 0; j < 6; j++) 
            {
                hitIndexes[j] =  findColumn(header, HIT_COLUMNS[j], formatter);
            }

            
            int commonIndex =
                    findOptionalColumn(header, "Common", formatter);

            int englishIndex =
                    findOptionalColumn(
                            header, "English_Translated", formatter);

            

            Sheet detailSheet =
                    outputWorkbook.createSheet("Query_Details");

            Row detailHeader = detailSheet.createRow(0);

            String[] detailColumns = {
                    "Source_Excel_Row",
                    "Common",
                    "English_Translated",
                    "Hit_var1",
                    "Hit_var2",
                    "Hit_var3",
                    "Hit_var4",
                    "Hit_var5",
                    "Hit_common",
                    "Successful_Count",
                    "Unsuccessful_Count",
                    "Category",
                    "Variant_Recovery"
            };

            for (int c = 0; c < detailColumns.length; c++) {
                detailHeader.createCell(c)
                        .setCellValue(detailColumns[c]);
            }

            int detailRowIndex = 1;

            

            for (int i = 1; i <= inputSheet.getLastRowNum(); i++) {

                Row row = inputSheet.getRow(i);

                if (row == null) {
                    continue;
                }

                String[] hits = new String[6];

                boolean[] success = new boolean[6];

                int successfulCount = 0;

                for (int j = 0; j < 6; j++) {

                    String value =
                            cellText(row, hitIndexes[j], formatter);

                    if (value.equalsIgnoreCase("Yes")) {

                        success[j] = true;
                        hits[j] = "Yes";
                        yesCounts[j]++;
                        successfulCount++;

                    } else if (value.equalsIgnoreCase("No")) {

                        success[j] = false;
                        hits[j] = "No";
                        noCounts[j]++;

                    } else {

                        throw new IllegalArgumentException(
                                "Invalid hit value at Excel row "
                                + (i + 1)
                                + ", column "
                                + HIT_COLUMNS[j]
                                + ": [" + value + "]");
                    }
                }

                totalQueries++;
                totalSuccessfulVariants += successfulCount;

                distribution[successfulCount]++;

                boolean canonical = success[5];

                boolean anyVariant = false;

                for (int j = 0; j < 5; j++) {

                    if (success[j]) {
                        anyVariant = true;
                        break;
                    }
                }

                boolean recovery =
                        !canonical && anyVariant;

                if (successfulCount == 6) {
                    completeSuccess++;
                } else if (successfulCount == 0) {
                    completeFailure++;
                } else {
                    partialSuccess++;
                }

                if (canonical) {
                    canonicalSuccess++;
                }

                if (anyVariant) {
                    anyVariantSuccess++;
                }

                if (recovery) {
                    variantRecovery++;
                }

                String category;

                if (successfulCount == 6) {
                    category = "Complete Success";
                } else if (successfulCount == 0) {
                    category = "Complete Failure";
                } else {
                    category = "Partial Success";
                }

                

                Row detailRow =
                        detailSheet.createRow(detailRowIndex++);

                detailRow.createCell(0).setCellValue(i + 1);

                detailRow.createCell(1).setCellValue(
                        commonIndex >= 0
                        ? cellText(row, commonIndex, formatter)
                        : "");

                detailRow.createCell(2).setCellValue(
                        englishIndex >= 0
                        ? cellText(row, englishIndex, formatter)
                        : "");

                for (int j = 0; j < 6; j++) {
                    detailRow.createCell(j + 3)
                            .setCellValue(hits[j]);
                }

                detailRow.createCell(9)
                        .setCellValue(successfulCount);

                detailRow.createCell(10)
                        .setCellValue(6 - successfulCount);

                detailRow.createCell(11)
                        .setCellValue(category);

                detailRow.createCell(12)
                        .setCellValue(recovery ? "Yes" : "No");
            }

            

            Sheet variantSheet =
                    outputWorkbook.createSheet("Variant_Hit_Rates");

            writeHeader(variantSheet,
                    "Variant",
                    "Yes",
                    "No",
                    "Total",
                    "Hit Rate (%)");

            for (int j = 0; j < 6; j++) {

                Row r = variantSheet.createRow(j + 1);

                int total = yesCounts[j] + noCounts[j];

                double rate = total == 0
                        ? 0
                        : 100.0 * yesCounts[j] / total;

                r.createCell(0).setCellValue(LABELS[j]);
                r.createCell(1).setCellValue(yesCounts[j]);
                r.createCell(2).setCellValue(noCounts[j]);
                r.createCell(3).setCellValue(total);
                r.createCell(4).setCellValue(rate);
            }

            

            Sheet distributionSheet =
                    outputWorkbook.createSheet(
                            "Success_Distribution");

            writeHeader(distributionSheet,
                    "Successful Variants",
                    "Number of Queries",
                    "Percentage (%)");

            for (int k = 0; k <= 6; k++) {

                Row r = distributionSheet.createRow(k + 1);

                double percentage = totalQueries == 0
                        ? 0
                        : 100.0 * distribution[k] / totalQueries;

                r.createCell(0).setCellValue(k + " of 6");
                r.createCell(1).setCellValue(distribution[k]);
                r.createCell(2).setCellValue(percentage);
            }

            Row totalRow = distributionSheet.createRow(8);

            totalRow.createCell(0).setCellValue("Total");
            totalRow.createCell(1).setCellValue(totalQueries);
            totalRow.createCell(2).setCellValue(
                    totalQueries == 0 ? 0 : 100.0);

            

            Sheet comparisonSheet =
                    outputWorkbook.createSheet(
                            "Canonical_vs_Variants");

            writeHeader(comparisonSheet,
                    "Category",
                    "Number of Queries",
                    "Percentage (%)");

            String[] categories = {
                    "Complete Success (6 Yes)",
                    "Partial Success (1-5 Yes)",
                    "Complete Failure (0 Yes)",
                    "Canonical Success",
                    "Any-Variant Success",
                    "Variant Recovery",
                    "At Least One Success",
                    "Mean Successful Variants"
            };

            double meanSuccessful = totalQueries == 0
                    ? 0
                    : (double) totalSuccessfulVariants / totalQueries;

            double[] values = {
                    completeSuccess,
                    partialSuccess,
                    completeFailure,
                    canonicalSuccess,
                    anyVariantSuccess,
                    variantRecovery,
                    totalQueries - completeFailure,
                    meanSuccessful
            };

            for (int k = 0; k < categories.length; k++) {

                Row r = comparisonSheet.createRow(k + 1);

                r.createCell(0).setCellValue(categories[k]);
                r.createCell(1).setCellValue(values[k]);

                if (k < 7) {

                    double percentage = totalQueries == 0
                            ? 0
                            : 100.0 * values[k] / totalQueries;

                    r.createCell(2).setCellValue(percentage);
                }
            }

            

            for (int c = 0; c < 5; c++) {
                variantSheet.setColumnWidth(c, 5500);
            }

            for (int c = 0; c < 3; c++) {
                distributionSheet.setColumnWidth(c, 6500);
            }

            comparisonSheet.setColumnWidth(0, 8500);
            comparisonSheet.setColumnWidth(1, 5500);
            comparisonSheet.setColumnWidth(2, 5500);

            detailSheet.setColumnWidth(0, 5000);
            detailSheet.setColumnWidth(1, 6500);
            detailSheet.setColumnWidth(2, 7500);

            for (int c = 3; c < detailColumns.length; c++) {
                detailSheet.setColumnWidth(c, 5500);
            }

            

            try (FileOutputStream fos =
                         new FileOutputStream(OUTPUT_FILE)) {

                outputWorkbook.write(fos);
            }
        }

        

        System.out.println();
        System.out.println(
                "========== CLQE HIT ANALYSIS ==========");

        System.out.println("Total queries: " + totalQueries);

        System.out.println();
        System.out.println("VARIANT HIT RATES");

        for (int j = 0; j < 6; j++) {

            double rate = totalQueries == 0
                    ? 0
                    : 100.0 * yesCounts[j] / totalQueries;

            System.out.printf(
                    Locale.ROOT,
                    "%s: Yes=%d, No=%d, Hit Rate=%.2f%%%n",
                    LABELS[j],
                    yesCounts[j],
                    noCounts[j],
                    rate);
        }

        System.out.println();
        System.out.println("SUCCESS DISTRIBUTION");

        for (int k = 0; k <= 6; k++) {

            System.out.println(
                    k + " of 6: " + distribution[k]);
        }

        System.out.println();
        System.out.println("CANONICAL VS VARIANTS");

        System.out.println(
                "Complete Success: " + completeSuccess);

        System.out.println(
                "Partial Success: " + partialSuccess);

        System.out.println(
                "Complete Failure: " + completeFailure);

        System.out.println(
                "Canonical Success: " + canonicalSuccess);

        System.out.println(
                "Any-Variant Success: " + anyVariantSuccess);

        System.out.println(
                "Variant Recovery: " + variantRecovery);

        System.out.println(
                "At Least One Success: "
                + (totalQueries - completeFailure));

        double mean = totalQueries == 0
                ? 0
                : (double) totalSuccessfulVariants / totalQueries;

        System.out.printf(
                Locale.ROOT,
                "Mean Successful Variants: %.4f%n",
                mean);

        System.out.println();
        System.out.println("Output: " + OUTPUT_FILE);
        System.out.println(
                "=======================================");
        System.out.println("ANALYSIS COMPLETED SUCCESSFULLY");
    }

    

    private static int findColumn(
            Row header,
            String name,
            DataFormatter formatter) {

        int index = findOptionalColumn(header, name, formatter);

        if (index < 0) {
            throw new IllegalArgumentException(
                    "Missing required column: " + name);
        }

        return index;
    }

    private static int findOptionalColumn(
            Row header,
            String name,
            DataFormatter formatter) {

        for (Cell cell : header) {

            String value =
                    formatter.formatCellValue(cell).trim();

            if (value.equalsIgnoreCase(name)) {
                return cell.getColumnIndex();
            }
        }

        return -1;
    }

    private static String cellText(
            Row row,
            int column,
            DataFormatter formatter) {

        if (row == null || column < 0) {
            return "";
        }

        Cell cell = row.getCell(column);

        if (cell == null) {
            return "";
        }

        return formatter.formatCellValue(cell).trim();
    }

    private static void writeHeader(
            Sheet sheet,
            String... headers) {

        Row row = sheet.createRow(0);

        for (int i = 0; i < headers.length; i++) {

            row.createCell(i).setCellValue(headers[i]);
        }
    }
}

