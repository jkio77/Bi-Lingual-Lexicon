package org.borue;
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

/*
 * this class counts number of instances of borue word which will be used in the recall 
 * calculation it is first in the chain to shoot
 */

public class BORUECountGenerator {

    // ============================================================
    // FILE PATHS
    // ============================================================

    private static final String ROMAN_URDU_FILE =
            "D:/BORUE/RomanUrduWords.xlsx";

    private static final String TEST_BENCH_FILE =
            "D:/BORUE/TestBenchFinal.xlsx";

    /*
     * A new file is produced so the original TestBenchFinal.xlsx
     * remains untouched.
     */
    private static final String OUTPUT_FILE =
            "D:/BORUE/TestBenchFinalCount.xlsx";


    // ============================================================
    // COLUMN HEADINGS
    // ============================================================

    private static final String ROMAN_URDU_HEADING =
            "RomanUrduWord";

    private static final String BORUE_HEADING =
            "BORUE";

    private static final String COUNT_HEADING =
            "Count";


    // ============================================================
    // MAIN
    // ============================================================

    public static void main(String[] args) {

        try {

            System.out.println(
                    "==============================================");

            System.out.println(
                    "BORUE Gold Relevant Count Generator");

            System.out.println(
                    "==============================================");


            // ----------------------------------------------------
            // STEP 1:
            // Load all Roman Urdu dictionary entries
            // ----------------------------------------------------

            List<String> romanUrduWords =
                    loadRomanUrduWords(ROMAN_URDU_FILE);


            System.out.println();

            System.out.println(
                    "Roman Urdu dictionary entries loaded: "
                    + romanUrduWords.size());


            // ----------------------------------------------------
            // STEP 2:
            // Calculate BORUE counts
            // ----------------------------------------------------

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


    // ============================================================
    // LOAD ALL ROMAN URDU WORDS
    // ============================================================

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


            // ----------------------------------------------------
            // Read all dictionary entries
            // ----------------------------------------------------

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


    // ============================================================
    // POPULATE BORUE COUNT COLUMN
    // ============================================================

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


            // ----------------------------------------------------
            // Create Count column if it does not exist
            // ----------------------------------------------------

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


            // ----------------------------------------------------
            // Process every BORUE row
            // ----------------------------------------------------

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


                // ------------------------------------------------
                // Count all dictionary entries containing BORUE
                // as a continuous character block
                // ------------------------------------------------

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


            // ----------------------------------------------------
            // Save output workbook
            // ----------------------------------------------------

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


    // ============================================================
    // CALCULATE GOLD RELEVANT COUNT
    // ============================================================
    private static int calculateBORUECount(
            String borue,
            List<String> romanUrduWords) {

        if (borue == null || borue.trim().isEmpty()) {
            return 0;
        }

        /*
         * RULES:
         *
         * 1. Space means the BORUE may be a compound/phrase.
         *
         *    Example:
         *        ada kaar
         *
         *    Search "ada kaar" as one continuous block.
         *
         *    MATCH:
         *        ada kaar
         *        ada kaari
         *        ada kaari karna
         *        mashhoor ada kaar
         *        ada kaar banna
         *
         *    NO MATCH:
         *        ada mat kaar
         *
         *
         * 2. Comma means alternative BORUE spellings.
         *
         *    Example:
         *        abad,aabaad
         *
         *    Search "abad" separately.
         *    Search "aabaad" separately.
         *    Add the two counts.
         *
         *    Example:
         *        abad   = 3
         *        aabaad = 4
         *
         *        Total Count = 7
         */

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
                    /*        temp addition
                    if (target.equalsIgnoreCase("aap")) {
                        System.out.println(
                            "AAP DEBUG | target=[" + target
                            + "] | dictionaryEntry=[" + dictionaryEntry + "]"
                        );
                    }
                    
                    */
                     
                }
            }

            totalCount += alternativeCount;

            /*
             * Print breakdown only when BORUE contains
             * multiple comma-separated alternatives.
             */
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
    // ============================================================
    // BORUE RELEVANCE RULE
    // ============================================================

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


        /*
         * ========================================================
         * RELEVANCE RULE
         * ========================================================
         *
         * BORUE must occur as one CONTINUOUS CHARACTER BLOCK
         * somewhere inside the dictionary entry.
         *
         * Matching is case-insensitive because both strings
         * have already been converted to lower case.
         *
         *
         * Example:
         *
         * BORUE:
         *
         *     ada kaar
         *
         *
         * MATCH:
         *
         *     ada kaar
         *
         *     ada kaari
         *
         *     ada kaari karna
         *
         *     mashhoor ada kaar
         *
         *     ada kaar banna
         *
         *
         * NO MATCH:
         *
         *     ada mat kaar
         *
         * because "ada kaar" does not occur as a continuous
         * block in that string.
         *
         *
         * Example:
         *
         * BORUE:
         *
         *     aadat
         *
         *
         * MATCH:
         *
         *     aadat
         *
         *     achi aadat
         *
         *     buri aadat
         *
         *     aadati
         *
         *     aadat dalna
         *
         * ========================================================
         */

        return normalizedEntry.contains(
                normalizedBORUE);
    }


    // ============================================================
    // NORMALIZE TEXT
    // ============================================================

    private static String normalize(
            String text) {


        if (text == null) {
            return "";
        }


        /*
         * Convert to lower case.
         *
         * Also collapse multiple spaces so that:
         *
         * "ada   kaar"
         *
         * becomes:
         *
         * "ada kaar"
         *
         * This prevents accidental Excel spacing differences
         * from affecting the count.
         */

        return text
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }


    // ============================================================
    // FIND COLUMN BY COLUMN HEADING
    // ============================================================

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