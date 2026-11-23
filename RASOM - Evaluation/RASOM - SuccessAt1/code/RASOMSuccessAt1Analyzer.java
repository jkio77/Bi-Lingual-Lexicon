package BORUE;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class RASOMSuccessAt1Analyzer {
    private static final String INPUT = "D:/BORUE/RASOM_Result.xlsx";
    private static final String OUTPUT = "D:/BORUE/RASOMSuccessAt1.xlsx";
    private static final DataFormatter FORMAT = new DataFormatter();
    private static final String[] TYPES = {"var1", "var2", "var3", "var4", "var5", "Common"};

    public static void main(String[] args) throws Exception {
        long[] trials = new long[6], retrieved = new long[6], successes = new long[6];
        try (FileInputStream in = new FileInputStream(INPUT);
             XSSFWorkbook source = new XSSFWorkbook(in);
             XSSFWorkbook result = new XSSFWorkbook()) {
            Sheet data = source.getSheetAt(0);
            Sheet audit = result.createSheet("Query Level Success@1");
            writeHeader(audit, "ConceptRow", "Variant", "Query", "BORUE", "TopCandidate",
                    "CandidateReturned", "RelevantAt1", "SuccessAt1");
            int outRow = 1;
            for (int r = 1; r <= data.getLastRowNum(); r++) {
                Row row = data.getRow(r);
                if (row == null) continue;
                String gold = cell(row, 6);
                for (int v = 0; v < 6; v++) {
                    String query = cell(row, v);
                    if (query.isEmpty()) continue; 
                    trials[v]++;
                    String candidates = cell(row, v + 8);
                    String first = firstCandidate(candidates);
                    boolean hasCandidate = !first.isEmpty();
                    boolean hit = hasCandidate && relevant(first, gold);
                    if (hasCandidate) retrieved[v]++;
                    if (hit) successes[v]++;
                    Row out = audit.createRow(outRow++);
                    put(out, 0, r + 1); 
                    put(out, 1, TYPES[v]);
                    put(out, 2, query);
                    put(out, 3, gold);
                    put(out, 4, first);
                    put(out, 5, hasCandidate);
                    put(out, 6, hit);
                    put(out, 7, hit ? 1 : 0);
                }
            }
            Sheet summary = result.createSheet("Success@1 Summary");
            writeHeader(summary, "Variant", "ValidQueries", "WithCandidates", "NoCandidates",
                    "RelevantAtRank1", "SuccessAt1", "Top1AccuracyWhenRetrieved");
            long allTrials = 0, allRetrieved = 0, allSuccesses = 0;
            for (int v = 0; v < 6; v++) {
                addSummary(summary, v + 1, TYPES[v], trials[v], retrieved[v], successes[v]);
                allTrials += trials[v];
                allRetrieved += retrieved[v];
                allSuccesses += successes[v];
            }
            addSummary(summary, 7, "OVERALL", allTrials, allRetrieved, allSuccesses);
            summary.createFreezePane(0, 1);
            audit.createFreezePane(0, 1);
            for (int c = 0; c < 7; c++) summary.autoSizeColumn(c);
            for (int c = 0; c < 8; c++) audit.setColumnWidth(c, c == 3 ? 9500 : 5500);
            try (FileOutputStream out = new FileOutputStream(OUTPUT)) { result.write(out); }
            System.out.printf(Locale.ROOT,
                    "Trials=%d; Returned=%d; Relevant@1=%d; Success@1=%.6f; Conditional top-1 accuracy=%.6f%n",
                    allTrials, allRetrieved, allSuccesses, ratio(allSuccesses, allTrials),
                    ratio(allSuccesses, allRetrieved));
            if (allTrials != 10161) System.out.println("WARNING: expected 10161 valid variants.");
        }
    }

    private static String cell(Row row, int col) {
        Cell c = row.getCell(col);
        return c == null ? "" : FORMAT.formatCellValue(c).trim();
    }

    private static String firstCandidate(String candidates) {
        if (candidates == null || candidates.trim().isEmpty()) return "";
        return candidates.split(",", 2)[0].trim();
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    
    private static boolean relevant(String candidate, String borue) {
        String text = normalize(candidate);
        if (text.isEmpty() || borue == null) return false;
        for (String alternative : borue.split(",")) {
            String gold = normalize(alternative);
            if (gold.isEmpty()) continue;
            Pattern p = Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(gold)
                    + "(?![\\p{L}\\p{N}])");
            if (p.matcher(text).find()) return true;
        }
        return false;
    }

    private static void addSummary(Sheet s, int rowNum, String label,
                                   long n, long returned, long hits) {
        Row r = s.createRow(rowNum);
        put(r, 0, label);
        put(r, 1, n);
        put(r, 2, returned);
        put(r, 3, n - returned);
        put(r, 4, hits);
        put(r, 5, ratio(hits, n));
        put(r, 6, ratio(hits, returned));
        r.getCell(5).setCellStyle(percentStyle(s.getWorkbook()));
        r.getCell(6).setCellStyle(percentStyle(s.getWorkbook()));
    }

    private static CellStyle percentStyle(Workbook w) {
        CellStyle style = w.createCellStyle();
        style.setDataFormat(w.createDataFormat().getFormat("0.00%"));
        return style;
    }

    private static double ratio(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    private static void writeHeader(Sheet s, String... labels) {
        Row r = s.createRow(0);
        for (int i = 0; i < labels.length; i++) put(r, i, labels[i]);
    }

    private static void put(Row row, int col, String value) {
        row.createCell(col).setCellValue(value);
    }
    private static void put(Row row, int col, long value) {
        row.createCell(col).setCellValue((double) value);
    }
    private static void put(Row row, int col, double value) {
        row.createCell(col).setCellValue(value);
    }
    private static void put(Row row, int col, boolean value) {
        row.createCell(col).setCellValue(value);
    }
}
