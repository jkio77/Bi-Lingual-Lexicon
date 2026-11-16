package org.borue;
//...................3 third in clqe line


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class RumetaResultTransfer {

    private static final String DIRECTORY =
            "D:\\BORUE\\";

    private static final String SOURCE_FILE =
            DIRECTORY + "Rumeta_Result.xlsx";

    private static final String TARGET_FILE =
            DIRECTORY + "NewTestBenchSort.xlsx";

    private static final String OUTPUT_FILE =
            DIRECTORY + "NewTestBenchSort_Updated.xlsx";

    private static final String[] MATCH_COLUMNS = {
            "var1", "var2", "var3",
            "var4", "var5", "Common"
    };

    private static final String[] RESULT_COLUMNS = {
            "res_var1", "res_var2", "res_var3",
            "res_var4", "res_var5", "res_common"
    };

    public static void main(String[] args) {

        try {
            transferResults();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void transferResults() throws Exception {

        DataFormatter formatter = new DataFormatter();

        int sourceRows = 0;
        int duplicateKeys = 0;
        int matchedRows = 0;
        int unmatchedRows = 0;

        try (FileInputStream sourceStream =
                     new FileInputStream(SOURCE_FILE);
             FileInputStream targetStream =
                     new FileInputStream(TARGET_FILE);
             Workbook sourceWorkbook =
                     new XSSFWorkbook(sourceStream);
             Workbook targetWorkbook =
                     new XSSFWorkbook(targetStream)) {

            Sheet sourceSheet = sourceWorkbook.getSheetAt(0);
            Sheet targetSheet = targetWorkbook.getSheetAt(0);

            Map<String, Integer> sourceHeaders =
                    readHeaders(sourceSheet.getRow(0), formatter);

            Map<String, Integer> targetHeaders =
                    readHeaders(targetSheet.getRow(0), formatter);

            // Validate all required columns
            for (String column : MATCH_COLUMNS) {
                requireColumn(sourceHeaders, column);
                requireColumn(targetHeaders, column);
            }

            for (String column : RESULT_COLUMNS) {
                requireColumn(sourceHeaders, column);
            }

            // Create missing result columns in target
            Row targetHeader = targetSheet.getRow(0);

            int nextColumn = targetHeader.getLastCellNum();

            for (String column : RESULT_COLUMNS) {

                if (!targetHeaders.containsKey(column)) {

                    Cell cell = targetHeader.createCell(nextColumn);
                    cell.setCellValue(column);

                    targetHeaders.put(column, nextColumn);
                    nextColumn++;
                }
            }

            // STEP 1: Index source rows using six-column key
            Map<String, String[]> sourceIndex =
                    new HashMap<String, String[]>();

            for (int i = 1; i <= sourceSheet.getLastRowNum(); i++) {

                Row row = sourceSheet.getRow(i);
                if (row == null) continue;

                sourceRows++;

                String key = buildKey(
                        row, sourceHeaders, formatter);

                // Retain first occurrence only
                if (sourceIndex.containsKey(key)) {
                    duplicateKeys++;
                    continue;
                }

                String[] results =
                        new String[RESULT_COLUMNS.length];

                for (int j = 0; j < RESULT_COLUMNS.length; j++) {

                    int columnIndex =
                            sourceHeaders.get(RESULT_COLUMNS[j]);

                    Cell cell = row.getCell(columnIndex);

                    String fullValue = cell == null
                            ? ""
                            : formatter.formatCellValue(cell);

                    results[j] = extractFirstValue(fullValue);
                }

                sourceIndex.put(key, results);
            }

            // STEP 2: Match target rows and transfer results
            for (int i = 1; i <= targetSheet.getLastRowNum(); i++) {

                Row row = targetSheet.getRow(i);
                if (row == null) continue;

                String key = buildKey(
                        row, targetHeaders, formatter);

                String[] results = sourceIndex.get(key);

                if (results == null) {
                    unmatchedRows++;
                    continue;
                }

                // Write six extracted values
                for (int j = 0; j < RESULT_COLUMNS.length; j++) {

                    int columnIndex =
                            targetHeaders.get(RESULT_COLUMNS[j]);

                    Cell cell = row.getCell(columnIndex);

                    if (cell == null) {
                        cell = row.createCell(columnIndex);
                    }

                    cell.setCellValue(results[j]);
                }

                matchedRows++;
            }

            // STEP 3: Save updated workbook
            try (FileOutputStream fos =
                         new FileOutputStream(OUTPUT_FILE)) {

                targetWorkbook.write(fos);
            }
        }

        // STEP 4: Console summary
        System.out.println();
        System.out.println("========== RUMETA TRANSFER SUMMARY ==========");
        System.out.println("Source rows: " + sourceRows);
        System.out.println("Unique source keys: "
                + (sourceRows - duplicateKeys));
        System.out.println("Duplicate source keys: " + duplicateKeys);
        System.out.println("Matched target rows: " + matchedRows);
        System.out.println("Unmatched target rows: " + unmatchedRows);
        System.out.println("Output file: " + OUTPUT_FILE);
        System.out.println("=============================================");
        System.out.println("PROCESS COMPLETED SUCCESSFULLY");
    }

    // Read column names from header row
    private static Map<String, Integer> readHeaders(
            Row header, DataFormatter formatter) {

        if (header == null) {
            throw new IllegalArgumentException(
                    "Excel header row is missing.");
        }

        Map<String, Integer> headers =
                new HashMap<String, Integer>();

        for (Cell cell : header) {

            String name =
                    formatter.formatCellValue(cell).trim();

            if (!name.isEmpty()) {
                headers.put(name, cell.getColumnIndex());
            }
        }

        return headers;
    }

    // Ensure required column exists
    private static void requireColumn(
            Map<String, Integer> headers, String name) {

        if (!headers.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Required column not found: " + name);
        }
    }

    // Build exact six-column comparison key
    private static String buildKey(
            Row row,
            Map<String, Integer> headers,
            DataFormatter formatter) {

        StringBuilder key = new StringBuilder();

        for (String column : MATCH_COLUMNS) {

            int columnIndex = headers.get(column);

            Cell cell = row.getCell(columnIndex);

            String value = cell == null
                    ? ""
                    : formatter.formatCellValue(cell);

            // Length prefix prevents ambiguous concatenation
            key.append(value.length())
               .append(':')
               .append(value);
        }

        return key.toString();
    }

    // Extract first comma-separated value
    private static String extractFirstValue(String value) {

        if (value == null || value.isEmpty()) {
            return "";
        }

        int commaIndex = value.indexOf(',');

        if (commaIndex >= 0) {
            return value.substring(0, commaIndex).trim();
        }

        return value.trim();
    }
}

