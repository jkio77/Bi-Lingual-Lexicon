package org.borue;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Counts explicitly asserted property axioms in BiOnt.owl (no reasoner).
 * Writes an Excel workbook with the four Table X statistics and details.  // change later
 * Requires OWLAPI and Apache POI (poi + poi-ooxml) on the Eclipse build path.
 */
public class BiOntFourPropertyAudit {

    // EDIT THESE TWO PATHS ONLY IF YOUR FILES ARE STORED ELSEWHERE.
    private static final String ONTOLOGY_PATH = "D:\\BORUE\\BiOnt.owl";
    private static final String OUTPUT_XLSX =   "D:\\BORUE\\BiOnt_Four_Property_Audit.xlsx";

    private static final String ROMMETA = "romMeta";
    private static final String SYNONYM = "isSynFor";
    private static final String SEMANTIC_SIMILARITY = "isSemSimTo";

    private static final String[] CROSS_LANGUAGE = {
        "isRomanNounFor", "isRomanAdjectiveFor", "isRomanTVerbFor",
        "isRomanIVerbFor", "isRomanAdverbFor", "isRomanPrepFor"
    };

    public static void main(String[] args) {
        try {
            File ontologyFile = new File(ONTOLOGY_PATH);
            if (!ontologyFile.isFile()) {
                throw new IllegalArgumentException(
                    "Ontology file not found: " + ontologyFile.getAbsolutePath()
                    + "\nUpdate ONTOLOGY_PATH at the top of this Java class."
                );
            }

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);

            Map<String, Long> counts = new LinkedHashMap<String, Long>();
            counts.put(ROMMETA, 0L);
            counts.put(SYNONYM, 0L);
            counts.put(SEMANTIC_SIMILARITY, 0L);
            for (String property : CROSS_LANGUAGE) {
                counts.put(property, 0L);
            }

            // Each matching explicitly asserted data-property axiom counts once.
            for (OWLDataPropertyAssertionAxiom axiom : ontology.getAxioms(AxiomType.DATA_PROPERTY_ASSERTION)) {
                if (!axiom.getProperty().isAnonymous()) {
                    String name = axiom.getProperty().asOWLDataProperty().getIRI().getShortForm();
                    if (counts.containsKey(name)) {
                        counts.put(name, counts.get(name) + 1L);
                    }
                }
            }

            // Each matching explicitly asserted object-property axiom counts once.
            for (OWLObjectPropertyAssertionAxiom axiom : ontology.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
                if (!axiom.getProperty().isAnonymous()) {
                    String name = axiom.getProperty().asOWLObjectProperty().getIRI().getShortForm();
                    if (counts.containsKey(name)) {
                        counts.put(name, counts.get(name) + 1L);
                    }
                }
            }

            long crossTotal = 0L;
            for (String property : CROSS_LANGUAGE) {
                crossTotal += counts.get(property);
            }

            System.out.println("Ontology: " + ontologyFile.getAbsolutePath());
            System.out.println("Total named individuals: " + ontology.getIndividualsInSignature().size());
            System.out.println("\nTABLE X — FOUR PROPERTY STATISTICS (asserted axioms)");
            System.out.println("RuMetA phonetic-code assertions (romMeta): " + counts.get(ROMMETA));
            System.out.println("Asserted synonym relationships (isSynFor): " + counts.get(SYNONYM));
            System.out.println("Asserted semantic-similarity relationships (isSemSimTo): " + counts.get(SEMANTIC_SIMILARITY));
            System.out.println("Roman Urdu–English lexical relation assertions: " + crossTotal);
            System.out.println("\nCross-language property breakdown:");
            for (String property : CROSS_LANGUAGE) {
                System.out.println("  " + property + ": " + counts.get(property));
            }

            Path output = Paths.get(OUTPUT_XLSX);
            if (output.getParent() != null) {
                Files.createDirectories(output.getParent());
            }
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet summary = workbook.createSheet("Table X Summary");
                int r = 0;
                addRow(summary, r++, "Ontology path", ontologyFile.getAbsolutePath());
                addRow(summary, r++, "Counting method", "Explicitly asserted axioms only; no inferred relations");
                addRow(summary, r++, "Total named individuals", ontology.getIndividualsInSignature().size());
                r++;
                addRow(summary, r++, "Ontology characteristic", "Count");
                addRow(summary, r++, "RuMetA phonetic-code assertions (romMeta)", counts.get(ROMMETA));
                addRow(summary, r++, "Asserted synonym relationships (isSynFor)", counts.get(SYNONYM));
                addRow(summary, r++, "Asserted semantic-similarity relationships (isSemSimTo)", counts.get(SEMANTIC_SIMILARITY));
                addRow(summary, r++, "Roman Urdu–English lexical relation assertions", crossTotal);
                summary.setColumnWidth(0, 17500);
                summary.setColumnWidth(1, 21000);

                Sheet detail = workbook.createSheet("Property Breakdown");
                addRow(detail, 0, "OWL property short name", "Asserted axiom count");
                int rowIndex = 1;
                for (Map.Entry<String, Long> entry : counts.entrySet()) {
                    addRow(detail, rowIndex++, entry.getKey(), entry.getValue());
                }
                addRow(detail, rowIndex, "TOTAL cross-language relations", crossTotal);
                detail.setColumnWidth(0, 13000);
                detail.setColumnWidth(1, 8000);

                try (FileOutputStream out = new FileOutputStream(output.toFile())) {
                    workbook.write(out);
                }
            }
            System.out.println("\nExcel saved to: " + output.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Audit failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addRow(Sheet sheet, int index, String label, String value) {
        Row row = sheet.createRow(index);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    private static void addRow(Sheet sheet, int index, String label, long value) {
        Row row = sheet.createRow(index);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue((double) value);
    }
}
