package BORUE;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class BORUECountGenerator {



    private static final String ROMAN_URDU_FILE ="D:/BORUE/RomanUrduWords.xlsx";

    private static final String TEST_BENCH_FILE = "D:/BORUE/TestBenchFinal.xlsx";

    
    private static final String OUTPUT_FILE =  "D:/BORUE/TestBenchFinalCount.xlsx";


    

    private static final String ROMAN_URDU_HEADING = "RomanUrduWord";

    private static final String BORUE_HEADING =  "BORUE";

    private static final String COUNT_HEADING =  "Count";


    

    public static void main(String[] args) {

        try {

            System.out.println("==============================================");

            System.out.println("BORUE Gold Relevant Count Generator");

            System.out.println("==============================================");


            

            List<String> romanUrduWords =
                    loadRomanUrduWords(ROMAN_URDU_FILE);


            System.out.println();

            System.out.println(
                    "Roman Urdu dictionary entries loaded: "
                    + romanUrduWords.size());


            

            populateBORUECounts(
                    TEST_BENCH_FILE,
                    OUTPUT_FILE,
                    romanUrduWords);


            System.out.println();

            System.out.println(
                    "==============================================");

            System.out.println(
                    "Completed successfully.");

            System.out.println(
                    "Output file:");

            System.out.println(
                    OUTPUT_FILE);

            System.out.println(
                    "==============================================");


        } catch (Exception e) {

            System.err.println(
                    "ERROR:");

            e.printStackTrace();
        }
    }


    

    private static List<String> loadRomanUrduWords(
            String filePath)
            throws IOException {

        List<String> words =
                new ArrayList<>();

        DataFormatter formatter =
                new DataFormatter();


        try (FileInputStream fis =
                     new FileInputStream(filePath);

             Workbook workbook =
                     new XSSFWorkbook(fis)) {


            Sheet sheet =
                    workbook.getSheetAt(0);


            Row headerRow =
                    sheet.getRow(0);


            if (headerRow == null) {

                throw new IllegalStateException(
                        "RomanUrduWords.xlsx has no header row.");
            }


            int wordColumn =
                    findColumnIndex(
                            headerRow,
                            ROMAN_URDU_HEADING,
                            formatter);


            if (wordColumn == -1) {

                throw new IllegalStateException(
                        "Column '"
                        + ROMAN_URDU_HEADING
                        + "' was not found in RomanUrduWords.xlsx.");
            }


            System.out.println(
                    "RomanUrduWord column found at index: "
                    + wordColumn);


    

            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {


                Row row =
                        sheet.getRow(rowIndex);


                if (row == null) {
                    continue;
                }


                Cell cell =
                        row.getCell(wordColumn);


                if (cell == null) {
                    continue;
                }


                String value =
                        formatter.formatCellValue(cell);


                if (value == null) {
                    continue;
                }


                value =
                        normalize(value);


                if (!value.isEmpty()) {

                    words.add(value);
                }
            }
        }


        return words;
    }


    

    private static void populateBORUECounts(
            String inputFile,
            String outputFile,
            List<String> romanUrduWords)
            throws IOException {


        DataFormatter formatter =
                new DataFormatter();


        try (FileInputStream fis =
                     new FileInputStream(inputFile);

             Workbook workbook =
                     new XSSFWorkbook(fis)) {


            Sheet sheet =
                    workbook.getSheetAt(0);


            Row headerRow =
                    sheet.getRow(0);


            if (headerRow == null) {

                throw new IllegalStateException(
                        "TestBenchFinal.xlsx has no header row.");
            }


            int borueColumn =
                    findColumnIndex(
                            headerRow,
                            BORUE_HEADING,
                            formatter);


            int countColumn =
                    findColumnIndex(
                            headerRow,
                            COUNT_HEADING,
                            formatter);


            if (borueColumn == -1) {

                throw new IllegalStateException(
                        "Column '"
                        + BORUE_HEADING
                        + "' was not found in TestBenchFinal.xlsx.");
            }


    
            if (countColumn == -1) {

                countColumn =
                        borueColumn + 1;


                Cell countHeader =
                        headerRow.getCell(countColumn);


                if (countHeader == null) {

                    countHeader =
                            headerRow.createCell(countColumn);
                }


                countHeader.setCellValue(
                        COUNT_HEADING);
            }


            System.out.println(
                    "BORUE column found at index: "
                    + borueColumn);


            System.out.println(
                    "Count column found at index: "
                    + countColumn);


            int processed = 0;
            int skipped = 0;


    

            for (int rowIndex = 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {


                Row row =
                        sheet.getRow(rowIndex);


                if (row == null) {

                    System.out.println(
                            "WARNING: Missing Excel row "
                            + (rowIndex + 1));

                    skipped++;

                    continue;
                }


                Cell borueCell =
                        row.getCell(borueColumn);


                if (borueCell == null) {

                    System.out.println(
                            "WARNING: Empty BORUE cell at Excel row "
                            + (rowIndex + 1));

                    skipped++;

                    continue;
                }


                String borue =
                        formatter.formatCellValue(
                                borueCell);


                if (borue == null
                        || borue.trim().isEmpty()) {

                    System.out.println(
                            "WARNING: Blank BORUE value at Excel row "
                            + (rowIndex + 1));

                    skipped++;

                    continue;
                }


                borue =
                        normalize(borue);


    
                int count =
                        calculateBORUECount(
                                borue,
                                romanUrduWords);


                Cell countCell =
                        row.getCell(countColumn);


                if (countCell == null) {

                    countCell =
                            row.createCell(
                                    countColumn,
                                    CellType.NUMERIC);
                }


                countCell.setCellValue(
                        count);


                processed++;


                System.out.println(
                        "Row "
                        + (rowIndex + 1)
                        + " | BORUE = "
                        + borue
                        + " | Count = "
                        + count);
            }


    

            try (FileOutputStream fos =
                         new FileOutputStream(outputFile)) {

                workbook.write(fos);
            }


            System.out.println();

            System.out.println(
                    "BORUE rows processed: "
                    + processed);


            System.out.println(
                    "BORUE rows skipped: "
                    + skipped);
        }
    }


    
    
    private static int calculateBORUECount(
            String borue,
            List<String> romanUrduWords) {

        if (borue == null || borue.trim().isEmpty()) {
            return 0;
        }

    

        String[] alternatives = borue.split(",");

        int totalCount = 0;

        for (String alternative : alternatives) {

            String target = normalize(alternative);

            if (target.isEmpty()) {
                continue;
            }

            int alternativeCount = 0;

            for (String dictionaryEntry : romanUrduWords) {

                if (containsBORUEBlock(
                        dictionaryEntry,
                        target)) {

                    alternativeCount++;
    
                     
                }
            }

            totalCount += alternativeCount;

    
            if (alternatives.length > 1) {

                System.out.println(
                        "      Alternative = "
                        + target
                        + " | Count = "
                        + alternativeCount);
            }
        }

        return totalCount;
    }
    
    

    private static boolean containsBORUEBlock(
            String dictionaryEntry,
            String borue) {


        if (dictionaryEntry == null
                || borue == null) {

            return false;
        }


        String normalizedEntry =
                normalize(dictionaryEntry);


        String normalizedBORUE =
                normalize(borue);


        if (normalizedEntry.isEmpty()
                || normalizedBORUE.isEmpty()) {

            return false;
        }


    
        return normalizedEntry.contains(
                normalizedBORUE);
    }


    

    private static String normalize(
            String text) {


        if (text == null) {
            return "";
        }


    

        return text
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }


    

    private static int findColumnIndex(
            Row headerRow,
            String heading,
            DataFormatter formatter) {


        for (Cell cell : headerRow) {


            String value =
                    formatter.formatCellValue(cell);


            if (value != null
                    && value.trim()
                            .equalsIgnoreCase(
                                    heading)) {


                return cell.getColumnIndex();
            }
        }


        return -1;
    }
}