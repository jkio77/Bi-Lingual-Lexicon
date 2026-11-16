package org.borue;
// this file generates query in Roman → English → Urdu format 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.search.EntitySearcher;

/**
 * BRUSE expansion using existing RuMetA results, but NO RuMetA matching.
 * Compatible with the OWLAPI EntitySearcher style used in
 * RomanUrduSimilarityExcelMatcher.java and Apache POI XSSF.
 */
public class BRUSEQueryGenerator {

    // Adjust these three paths for your computer.
    private static final String ONTOLOGY_PATH = "D:/BORUE/BiOnt.owl";
    private static final String EXCEL_PATH = "D:/BORUE/BRUSE_TEST.xlsx";
    private static final String OUTPUT_PATH = "D:/BORUE/BRUSE_TEST_Expanded.xlsx";

    private static final String BASE_IRI = "http://localhost:3030/BiOnt.owl#";
    private static final String LANGUAGE_IN_CLASS = "Language_In";
    private static final String STR_VAL_PROPERTY = "strVal";
    private static final String STR_VAL_UNI_PROPERTY = "strValUni";

    private final OWLOntology ontology;
    private final OWLDataProperty strVal;
    private final OWLDataProperty strValUni;
    private final Map<String, OWLNamedIndividual> romanByStrVal =
            new HashMap<String, OWLNamedIndividual>();

    private BRUSEQueryGenerator(OWLOntology ontology, OWLDataFactory factory) {
        this.ontology = ontology;
        this.strVal = factory.getOWLDataProperty(IRI.create(BASE_IRI + STR_VAL_PROPERTY));
        this.strValUni = factory.getOWLDataProperty(IRI.create(BASE_IRI + STR_VAL_UNI_PROPERTY));
        buildRomanIndex(factory);
    }

    private void buildRomanIndex(OWLDataFactory factory) {
        OWLClass languageIn = factory.getOWLClass(IRI.create(BASE_IRI + LANGUAGE_IN_CLASS));
        Set<OWLClass> eligible = new HashSet<OWLClass>();
        ArrayDeque<OWLClass> pending = new ArrayDeque<OWLClass>();
        pending.add(languageIn);
        while (!pending.isEmpty()) {
            OWLClass parent = pending.removeFirst();
            if (!eligible.add(parent)) continue;
            for (OWLSubClassOfAxiom axiom : ontology.getSubClassAxiomsForSuperClass(parent)) {
                OWLClassExpression sub = axiom.getSubClass();
                if (!sub.isAnonymous()) pending.add(sub.asOWLClass());
            }
        }

        List<OWLNamedIndividual> individuals = new ArrayList<OWLNamedIndividual>(
                ontology.getIndividualsInSignature());
        Collections.sort(individuals, Comparator.comparing(a -> a.getIRI().toString()));
        for (OWLNamedIndividual individual : individuals) {
            boolean isRoman = false;
            for (OWLClassExpression type : EntitySearcher.getTypes(individual, ontology)) {
                if (!type.isAnonymous() && eligible.contains(type.asOWLClass())) {
                    isRoman = true;
                    break;
                }
            }
            if (!isRoman) continue;
            for (String value : values(individual, strVal)) {
                // Keep the first IRI deterministically if duplicate strVal exists.
                if (!romanByStrVal.containsKey(value)) romanByStrVal.put(value, individual);
            }
        }
        System.out.println("Indexed Language_In strVal entries: " + romanByStrVal.size());
    }

    private List<String> values(OWLNamedIndividual individual, OWLDataProperty property) {
        List<String> result = new ArrayList<String>();
        EntitySearcher.getDataPropertyValues(individual, property, ontology).forEach(literal -> {
            String v = literal.getLiteral().trim();
            if (!v.isEmpty()) result.add(v);
        });
        Collections.sort(result);
        return result;
    }

    private String firstValue(OWLNamedIndividual individual, OWLDataProperty property) {
        List<String> found = values(individual, property);
        return found.isEmpty() ? "" : found.get(0);
    }

    private static final class EnglishEntry {
        final String english;
        final List<String> urdu;
        final String iri;
        EnglishEntry(String english, List<String> urdu, String iri) {
            this.english = english;
            this.urdu = urdu;
            this.iri = iri;
        }
    }

    private static final class Relations {
        final List<String> synonyms = new ArrayList<String>();
        final List<EnglishEntry> english = new ArrayList<EnglishEntry>();
    }

    private Relations extract(OWLNamedIndividual source) {
        Relations relations = new Relations();
        Set<String> seenSynonyms = new HashSet<String>();
        Set<String> seenEnglish = new HashSet<String>();

        // Iterate properties deterministically; never traverse a target's own relations.
        List<OWLObjectProperty> properties = new ArrayList<OWLObjectProperty>(
                ontology.getObjectPropertiesInSignature());
        Collections.sort(properties, Comparator.comparing(p -> p.getIRI().toString()));
        for (OWLObjectProperty property : properties) {
            String name = property.getIRI().getShortForm();
            boolean synonym = "isSynFor".equals(name);
            boolean english = name.startsWith("isRoman") && name.endsWith("For")
                    && !"isSynFor".equals(name) && !"isSemSimTo".equals(name);
            if (!synonym && !english) continue;

            List<OWLNamedIndividual> targets = new ArrayList<OWLNamedIndividual>();
            EntitySearcher.getObjectPropertyValues(source, property, ontology).forEach(target -> {
                if (target.isNamed()) targets.add(target.asOWLNamedIndividual());
            });
            Collections.sort(targets, Comparator.comparing(t -> t.getIRI().toString()));
            for (OWLNamedIndividual target : targets) {
                String label = firstValue(target, strVal);
                if (label.isEmpty()) continue;
                if (synonym) {
                    // Only a direct isSynFor target. No semantic-similarity expansion.
                    if (seenSynonyms.add(label)) relations.synonyms.add(label);
                } else {
                    if (seenEnglish.add(label)) {
                        relations.english.add(new EnglishEntry(
                                label, values(target, strValUni), target.getIRI().toString()));
                    }
                }
            }
        }
        Collections.sort(relations.synonyms);
        Collections.sort(relations.english, Comparator
                .comparing((EnglishEntry e) -> e.english).thenComparing(e -> e.iri));
        return relations;
    }

    private static void addTerm(LinkedHashSet<String> terms, String value) {
        if (value != null && !value.trim().isEmpty()) terms.add(value.trim());
    }

    private static String query(String common, Relations r, int limit) {
        LinkedHashSet<String> terms = new LinkedHashSet<String>();
        addTerm(terms, common);
        for (int i = 0; i < r.synonyms.size() && i < limit; i++)
            addTerm(terms, r.synonyms.get(i));
        // Pick up to 'limit' English individuals; Urdu is paired to the picked ones.
        List<String> urdu = new ArrayList<String>();
        for (int i = 0; i < r.english.size() && i < limit; i++) {
            EnglishEntry e = r.english.get(i);
            addTerm(terms, e.english);
            urdu.addAll(e.urdu);
        }
        for (String value : urdu) addTerm(terms, value);
        return String.join(" ", terms);
    }

    private static String cellText(Row row, int col, DataFormatter formatter) {
        if (row == null) return "";
        Cell cell = row.getCell(col);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private static void setText(Row row, int col, String value) {
        Cell cell = row.getCell(col);
        if (cell == null) cell = row.createCell(col, CellType.STRING);
        cell.setCellValue(value);
    }

    private static int column(Row header, String name, DataFormatter formatter) {
        for (int i = 0; i < header.getLastCellNum(); i++) {
            if (name.equalsIgnoreCase(cellText(header, i, formatter))) return i;
        }
        throw new IllegalArgumentException("Missing Excel column: " + name);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Loading ontology...");
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(ONTOLOGY_PATH));
        BRUSEQueryGenerator generator = new BRUSEQueryGenerator(ontology, manager.getOWLDataFactory());

        int matched = 0;
        int unmatched = 0;
        int processed = 0;
        try (FileInputStream input = new FileInputStream(EXCEL_PATH);
             XSSFWorkbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) throw new IllegalArgumentException("Missing header row");
            DataFormatter formatter = new DataFormatter();
            int commonCol = column(header, "common", formatter);
            int resultsCol = column(header, "res_common", formatter);
            int c2 = column(header, "bruse_query2", formatter);
            int c4 = column(header, "bruse_query4", formatter);
            int c6 = column(header, "bruse_query6", formatter);
            int cFull = column(header, "bruse_query_full", formatter);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String common = cellText(row, commonCol, formatter);
                if (common.isEmpty()) continue;
                String ranked = cellText(row, resultsCol, formatter);
                // First comma-separated RuMetA result is lookup key ONLY.
                String lookup = ranked.isEmpty() ? "" : ranked.split(",", 2)[0].trim();
                OWLNamedIndividual individual = generator.romanByStrVal.get(lookup);
                Relations r;
                if (individual == null) {
                    unmatched++;
                    r = new Relations(); // preserve original word, no invented expansion
                    System.out.println("NO LEXICAL MATCH row " + (i + 1) + ": " + lookup);
                } else {
                    matched++;
                    r = generator.extract(individual);
                }
                setText(row, c2, query(common, r, 2));
                setText(row, c4, query(common, r, 4));
                setText(row, c6, query(common, r, 6));
                setText(row, cFull, query(common, r, Integer.MAX_VALUE));
                processed++;
                if (processed % 250 == 0) System.out.println("Processed " + processed + " rows");
            }
            try (FileOutputStream output = new FileOutputStream(OUTPUT_PATH)) {
                workbook.write(output);
            }
        }
        System.out.println("Completed. Rows=" + processed + ", matched=" + matched
                + ", unmatched=" + unmatched);
        System.out.println("Saved: " + OUTPUT_PATH);
    }
}
