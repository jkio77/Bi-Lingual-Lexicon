package BORUE;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class NewTestBenchSorter {

    private static final String DIRECTORY =  "D:\\BORUE\\";

    private static final String REFERENCE_FILE =  DIRECTORY + "TestBenchFinalCount.xlsx";

    private static final String INPUT_FILE = DIRECTORY + "NewTestBench.xlsx";

    private static final String OUTPUT_FILE =  DIRECTORY + "NewTestBenchSort.xlsx";

    public static void main(String[] args) {

        try {
            sortTestBench();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sortTestBench() throws Exception {

        DataFormatter formatter = new DataFormatter();

        
        List<String> referenceOrder = new ArrayList<String>();

        try (FileInputStream fis =
                     new FileInputStream(REFERENCE_FILE);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            int commonColumn =
                    findCommonColumn(sheet.getRow(0), formatter);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;

                String value =
                        getCommonValue(row, commonColumn, formatter);

                if (!value.isEmpty()
                        && !referenceOrder.contains(value)) {
                    referenceOrder.add(value);
                }
            }
        }

        System.out.println("Reference Common values: "
                + referenceOrder.size());

        
        try (FileInputStream fis =
                     new FileInputStream(INPUT_FILE);
             Workbook inputWorkbook = new XSSFWorkbook(fis);
             Workbook outputWorkbook = new XSSFWorkbook()) {

            Sheet inputSheet = inputWorkbook.getSheetAt(0);
            Sheet outputSheet =
                    outputWorkbook.createSheet("SortedTestBench");

            Row inputHeader = inputSheet.getRow(0);

            int commonColumn =
                    findCommonColumn(inputHeader, formatter);

            int totalColumns = inputHeader.getLastCellNum();

            Map<String, List<Row>> groupedRows =
                    new LinkedHashMap<String, List<Row>>();

            int inputRows = 0;

            for (int i = 1; i <= inputSheet.getLastRowNum(); i++) {

                Row row = inputSheet.getRow(i);
                if (row == null) continue;

                String common =
                        getCommonValue(row, commonColumn, formatter);

                if (common.isEmpty()) continue;

                if (!groupedRows.containsKey(common)) {
                    groupedRows.put(common, new ArrayList<Row>());
                }

                groupedRows.get(common).add(row);
                inputRows++;
            }

            Map<Short, CellStyle> styleMap =
                    new HashMap<Short, CellStyle>();

            
            copyRow(inputHeader, outputSheet.createRow(0),
                    outputWorkbook, styleMap);

            
            int outputRowIndex = 1;
            int matchedRows = 0;
            int missingReferenceWords = 0;

            for (String common : referenceOrder) {

                List<Row> rows = groupedRows.remove(common);

                if (rows == null) {
                    missingReferenceWords++;
                    continue;
                }

                for (Row sourceRow : rows) {

                    Row targetRow =
                            outputSheet.createRow(outputRowIndex++);

                    copyRow(sourceRow, targetRow,
                            outputWorkbook, styleMap);

                    matchedRows++;
                }
            }

            
            int unmatchedRows = 0;

            for (List<Row> rows : groupedRows.values()) {

                for (Row sourceRow : rows) {

                    Row targetRow =
                            outputSheet.createRow(outputRowIndex++);

                    copyRow(sourceRow, targetRow,
                            outputWorkbook, styleMap);

                    unmatchedRows++;
                }
            }

            
            for (int c = 0; c < totalColumns; c++) {

                outputSheet.setColumnWidth(
                        c, inputSheet.getColumnWidth(c));
            }

            
            try (FileOutputStream fos =
                         new FileOutputStream(OUTPUT_FILE)) {

                outputWorkbook.write(fos);
            }

            
            System.out.println();
            System.out.println("========== SORT SUMMARY ==========");
            System.out.println("Input rows: " + inputRows);
            System.out.println("Rows ordered by reference: "
                    + matchedRows);
            System.out.println("Unmatched rows appended: "
                    + unmatchedRows);
            System.out.println("Reference values without input rows: "
                    + missingReferenceWords);
            System.out.println("Total output rows: "
                    + (matchedRows + unmatchedRows));
            System.out.println("Output file: " + OUTPUT_FILE);
            System.out.println("==================================");
            System.out.println("SORTING COMPLETED SUCCESSFULLY");
        }
    }

    private static int findCommonColumn(
            Row header, DataFormatter formatter) {

        if (header == null) {
            throw new IllegalArgumentException(
                    "Header row is missing.");
        }

        for (Cell cell : header) {

            String value =
                    formatter.formatCellValue(cell).trim();

            if (value.equalsIgnoreCase("Common")) {
                return cell.getColumnIndex();
            }
        }

        throw new IllegalArgumentException(
                "Column 'Common' not found.");
    }

    private static String getCommonValue(
            Row row, int column, DataFormatter formatter) {

        Cell cell = row.getCell(column);

        if (cell == null) return "";

        return formatter.formatCellValue(cell)
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    
    private static void copyRow(
            Row sourceRow,
            Row targetRow,
            Workbook targetWorkbook,
            Map<Short, CellStyle> styleMap) {

        targetRow.setHeight(sourceRow.getHeight());

        for (int c = 0; c < sourceRow.getLastCellNum(); c++) {

            Cell sourceCell = sourceRow.getCell(c);
            if (sourceCell == null) continue;

            Cell targetCell = targetRow.createCell(c);

            switch (sourceCell.getCellType()) {

                case Cell.CELL_TYPE_STRING:
                    targetCell.setCellValue(
                            sourceCell.getStringCellValue());
                    break;

                case Cell.CELL_TYPE_NUMERIC:
                    targetCell.setCellValue(
                            sourceCell.getNumericCellValue());
                    break;

                case Cell.CELL_TYPE_BOOLEAN:
                    targetCell.setCellValue(
                            sourceCell.getBooleanCellValue());
                    break;

                case Cell.CELL_TYPE_FORMULA:
                    targetCell.setCellFormula(
                            sourceCell.getCellFormula());
                    break;

                case Cell.CELL_TYPE_ERROR:
                    targetCell.setCellErrorValue(
                            sourceCell.getErrorCellValue());
                    break;

                case Cell.CELL_TYPE_BLANK:
                default:
                    break;
            }

            CellStyle sourceStyle = sourceCell.getCellStyle();

            if (sourceStyle != null) {

                short styleIndex = sourceStyle.getIndex();

                CellStyle targetStyle = styleMap.get(styleIndex);

                if (targetStyle == null) {

                    targetStyle = targetWorkbook.createCellStyle();
                    targetStyle.cloneStyleFrom(sourceStyle);
                    styleMap.put(styleIndex, targetStyle);
                }

                targetCell.setCellStyle(targetStyle);
            }
        }
    }
}
