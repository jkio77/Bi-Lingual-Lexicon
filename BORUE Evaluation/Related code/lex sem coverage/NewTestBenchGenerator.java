package BORUE;



import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;



public class NewTestBenchGenerator {

    private static final String DIRECTORY =
            "D:\\BORUE\\";

    private static final String REFERENCE_FILE =
            DIRECTORY + "TestBenchFinalCount.xlsx";

    private static final String SOURCE_FILE =
            DIRECTORY
            + "Roman Urdu words and English Translation with spelling variation.xlsx";

    private static final String OUTPUT_FILE =
            DIRECTORY + "NewTestBench.xlsx";

    public static void main(String[] args) {

        try {
            generateTestBench();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void generateTestBench() throws Exception {

        Set<String> referenceWords = new HashSet<String>();

        DataFormatter formatter = new DataFormatter();

        int sourceRows = 0;
        int matchedRows = 0;
        int excludedRows = 0;
        int emptyCommonRows = 0;

        
        try (FileInputStream fis =
                     new FileInputStream(REFERENCE_FILE);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            Row header = sheet.getRow(0);

            int commonColumn =
                    findCommonColumn(header, formatter);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                String value = getCellValue(
                        row, commonColumn, formatter);

                if (!value.isEmpty()) {
                    referenceWords.add(value);
                }
            }
        }

        System.out.println("Unique reference Common values: "
                + referenceWords.size());

        try (FileInputStream fis =
                     new FileInputStream(SOURCE_FILE);
             Workbook sourceWorkbook = new XSSFWorkbook(fis);
             Workbook outputWorkbook = new XSSFWorkbook()) {

            Sheet sourceSheet = sourceWorkbook.getSheetAt(0);

            Sheet outputSheet =
                    outputWorkbook.createSheet("NewTestBench");

            Row sourceHeader = sourceSheet.getRow(0);

            int commonColumn =
                    findCommonColumn(sourceHeader, formatter);

            int totalColumns =
                    sourceHeader.getLastCellNum();

            Map<Short, CellStyle> styleMap =
                    new HashMap<Short, CellStyle>();

            
            Row outputHeader = outputSheet.createRow(0);

            copyRow(sourceHeader, outputHeader,
                    outputWorkbook, styleMap);

            int outputRowIndex = 1;

           
            for (int i = 1;
                 i <= sourceSheet.getLastRowNum(); i++) {

                Row sourceRow = sourceSheet.getRow(i);

                if (sourceRow == null) {
                    continue;
                }

                sourceRows++;

                String commonValue = getCellValue(
                        sourceRow, commonColumn, formatter);

                if (commonValue.isEmpty()) {
                    emptyCommonRows++;
                    continue;
                }

                if (referenceWords.contains(commonValue)) {

                    Row outputRow =
                            outputSheet.createRow(outputRowIndex++);

                    copyRow(sourceRow, outputRow,
                            outputWorkbook, styleMap);

                    matchedRows++;

                } else {
                    excludedRows++;
                }
            }

            
            for (int c = 0; c < totalColumns; c++) {

                outputSheet.setColumnWidth(
                        c, sourceSheet.getColumnWidth(c));
            }

            
            try (FileOutputStream fos =
                         new FileOutputStream(OUTPUT_FILE)) {

                outputWorkbook.write(fos);
            }
        }

        
        System.out.println();
        System.out.println("========== SUMMARY ==========");
        System.out.println("Reference Common values: "
                + referenceWords.size());
        System.out.println("Source rows: " + sourceRows);
        System.out.println("Matched rows: " + matchedRows);
        System.out.println("Excluded rows: " + excludedRows);
        System.out.println("Empty Common rows: " + emptyCommonRows);
        System.out.println("Output file: " + OUTPUT_FILE);
        System.out.println("=============================");
        System.out.println("PROCESS COMPLETED SUCCESSFULLY");
    }

    
    private static int findCommonColumn(
            Row header, DataFormatter formatter) {

        if (header == null) {
            throw new IllegalArgumentException(
                    "Excel header row is missing.");
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

    
    private static String getCellValue(
            Row row, int column, DataFormatter formatter) {

        Cell cell = row.getCell(column);

        if (cell == null) {
            return "";
        }

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

        for (int c = 0;
             c < sourceRow.getLastCellNum(); c++) {

            Cell sourceCell = sourceRow.getCell(c);

            if (sourceCell == null) {
                continue;
            }

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

                CellStyle targetStyle =
                        styleMap.get(styleIndex);

                if (targetStyle == null) {

                    targetStyle =
                            targetWorkbook.createCellStyle();

                    targetStyle.cloneStyleFrom(sourceStyle);

                    styleMap.put(styleIndex, targetStyle);
                }

                targetCell.setCellStyle(targetStyle);
            }
        }
    }
}

