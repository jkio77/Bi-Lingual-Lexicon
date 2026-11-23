package BORUE;

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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
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
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.search.EntitySearcher;

public class CLQEOntologyExpansion {

    

    private static final String ONTOLOGY_PATH ="D:/BORUE/BiOnt.owl";

    private static final String DIRECTORY = "D:/BORUE/";

    private static final String INPUT_FILE =   DIRECTORY + "NewTestBenchSort_Updated.xlsx";

    private static final String OUTPUT_FILE =   DIRECTORY + "NewTestBenchSort_Expanded.xlsx";

    

    private static final String BASE_IRI = "http://localhost:3030/BiOnt.owl#";

    private static final String LANGUAGE_IN_CLASS = "Language_In";

    private static final String STR_VAL_PROPERTY =   "strVal";

    

    private static final String[] RESULT_COLUMNS = {
            "res_var1",
            "res_var2",
            "res_var3",
            "res_var4",
            "res_var5",
            "res_common"
    };

    private static final String[] EXPANSION_COLUMNS = {
            "exp_var1",
            "exp_var2",
            "exp_var3",
            "exp_var4",
            "exp_var5",
            "exp_common"
    };

    private static final String[] HIT_COLUMNS = {
            "Hit_var1",
            "Hit_var2",
            "Hit_var3",
            "Hit_var4",
            "Hit_var5",
            "Hit_common"
    };

    

    private final OWLOntology ontology;
    private final OWLDataProperty strVal;

    private final Map<String, OWLNamedIndividual> romanIndex =
            new HashMap<String, OWLNamedIndividual>();

    private final List<OWLObjectProperty> englishProperties =
            new ArrayList<OWLObjectProperty>();

    private final List<OWLObjectProperty> synonymProperties =
            new ArrayList<OWLObjectProperty>();

    private final Map<String, List<String>> expansionCache =
            new HashMap<String, List<String>>();

    

    private CLQEOntologyExpansion(
            OWLOntology ontology,
            OWLDataFactory factory) {

        this.ontology = ontology;

        this.strVal = factory.getOWLDataProperty(
                IRI.create(BASE_IRI + STR_VAL_PROPERTY));

        buildRomanIndex(factory);
        identifyRelations();
    }

    

    private void buildRomanIndex(OWLDataFactory factory) {

        OWLClass languageIn = factory.getOWLClass(
                IRI.create(BASE_IRI + LANGUAGE_IN_CLASS));

        Set<OWLClass> eligible =
                new HashSet<OWLClass>();

        ArrayDeque<OWLClass> pending =
                new ArrayDeque<OWLClass>();

        pending.add(languageIn);

        while (!pending.isEmpty()) {

            OWLClass parent = pending.removeFirst();

            if (!eligible.add(parent)) {
                continue;
            }

            for (OWLSubClassOfAxiom axiom :
                    ontology.getSubClassAxiomsForSuperClass(parent)) {

                OWLClassExpression sub = axiom.getSubClass();

                if (!sub.isAnonymous()) {
                    pending.add(sub.asOWLClass());
                }
            }
        }

        List<OWLNamedIndividual> individuals =
                new ArrayList<OWLNamedIndividual>(
                        ontology.getIndividualsInSignature());

        Collections.sort(individuals,
                Comparator.comparing(
                        a -> a.getIRI().toString()));

        for (OWLNamedIndividual individual : individuals) {

            boolean isRoman = false;

            for (OWLClassExpression type :
                    EntitySearcher.getTypes(individual, ontology)) {

                if (!type.isAnonymous()
                        && eligible.contains(type.asOWLClass())) {

                    isRoman = true;
                    break;
                }
            }

            if (!isRoman) {
                continue;
            }

            for (String value : values(individual)) {

                if (!romanIndex.containsKey(value)) {
                    romanIndex.put(value, individual);
                }
            }
        }

        System.out.println(
                "Indexed Roman Urdu values: "
                + romanIndex.size());
    }

    

    private void identifyRelations() {

        List<OWLObjectProperty> properties =
                new ArrayList<OWLObjectProperty>(
                        ontology.getObjectPropertiesInSignature());

        Collections.sort(properties,
                Comparator.comparing(
                        p -> p.getIRI().toString()));

        for (OWLObjectProperty property : properties) {

            String name = property.getIRI().getShortForm();

            if (name.equals("isSynFor")) {

                synonymProperties.add(property);

            } else if (name.startsWith("isRoman")
                    && name.endsWith("For")
                    && name.length() > "isRomanFor".length()) {

                englishProperties.add(property);
            }
        }

        System.out.println(
                "English relation types: "
                + englishProperties.size());

        System.out.println(
                "Synonym relation types: "
                + synonymProperties.size());
    }

    

    private List<String> values(
            OWLNamedIndividual individual) {

        List<String> result = new ArrayList<String>();

        EntitySearcher.getDataPropertyValues(
                individual, strVal, ontology).forEach(literal -> {

            String value = literal.getLiteral().trim();

            if (!value.isEmpty()) {
                result.add(value);
            }
        });

        Collections.sort(result);

        return result;
    }

    

    private List<OWLNamedIndividual> targets(
            OWLNamedIndividual source,
            OWLObjectProperty property) {

        List<OWLNamedIndividual> result =
                new ArrayList<OWLNamedIndividual>();

        EntitySearcher.getObjectPropertyValues(
                source, property, ontology).forEach(target -> {

            if (target.isNamed()) {
                result.add(target.asOWLNamedIndividual());
            }
        });

        Collections.sort(result,
                Comparator.comparing(
                        a -> a.getIRI().toString()));

        return result;
    }

    

    private void collectEnglish(
            OWLNamedIndividual roman,
            LinkedHashSet<String> englishTerms) {

        for (OWLObjectProperty property : englishProperties) {

            List<OWLNamedIndividual> englishIndividuals =
                    targets(roman, property);

            for (OWLNamedIndividual english :
                    englishIndividuals) {

                for (String value : values(english)) {

                    englishTerms.add(value);
                }
            }
        }
    }

    

    private List<String> expand(String romanWord) {

        if (expansionCache.containsKey(romanWord)) {
            return expansionCache.get(romanWord);
        }

        LinkedHashSet<String> englishTerms =
                new LinkedHashSet<String>();

        OWLNamedIndividual roman =
                romanIndex.get(romanWord);

        if (roman == null) {

            List<String> empty =
                    new ArrayList<String>();

            expansionCache.put(romanWord, empty);

            return empty;
        }

        
        collectEnglish(roman, englishTerms);

        
        for (OWLObjectProperty property : synonymProperties) {

            List<OWLNamedIndividual> synonyms =
                    targets(roman, property);

            for (OWLNamedIndividual synonym : synonyms) {

        
                collectEnglish(synonym, englishTerms);
            }
        }

        
        List<String> result =
                new ArrayList<String>(englishTerms);

        expansionCache.put(romanWord, result);

        return result;
    }

    

    private static boolean isHit(
            String englishTranslated,
            List<String> expansions) {

        String expected =
                englishTranslated.trim().toLowerCase(Locale.ROOT);

        if (expected.isEmpty()) {
            return false;
        }

        for (String expansion : expansions) {

            
            if (expansion.trim()
                    .toLowerCase(Locale.ROOT)
                    .equals(expected)) {

                return true;
            }
        }

        return false;
    }



    private static String cellText(
            Row row,
            int column,
            DataFormatter formatter) {

        if (row == null) {
            return "";
        }

        Cell cell = row.getCell(column);

        if (cell == null) {
            return "";
        }

        return formatter.formatCellValue(cell).trim();
    }

    private static void setText(
            Row row,
            int column,
            String value) {

        Cell cell = row.getCell(column);

        if (cell == null) {
            cell = row.createCell(column);
        }

        cell.setCellValue(value);
    }

    private static Map<String, Integer> readHeaders(
            Row header,
            DataFormatter formatter) {

        Map<String, Integer> columns =
                new HashMap<String, Integer>();

        if (header == null) {
            throw new IllegalArgumentException(
                    "Excel header row is missing.");
        }

        for (Cell cell : header) {

            String name = formatter
                    .formatCellValue(cell)
                    .trim()
                    .toLowerCase(Locale.ROOT);

            if (!name.isEmpty()) {
                columns.put(name, cell.getColumnIndex());
            }
        }

        return columns;
    }

    private static int requireColumn(
            Map<String, Integer> columns,
            String name) {

        Integer index =
                columns.get(name.toLowerCase(Locale.ROOT));

        if (index == null) {
            throw new IllegalArgumentException(
                    "Missing required Excel column: " + name);
        }

        return index;
    }

    private static int ensureColumn(
            Row header,
            Map<String, Integer> columns,
            String name) {

        String key = name.toLowerCase(Locale.ROOT);

        if (columns.containsKey(key)) {
            return columns.get(key);
        }

        int newIndex = header.getLastCellNum();

        if (newIndex < 0) {
            newIndex = 0;
        }

        header.createCell(newIndex).setCellValue(name);

        columns.put(key, newIndex);

        return newIndex;
    }

    
private void processExcel() throws Exception {

    DataFormatter formatter = new DataFormatter();

    int processedRows = 0;
    int lexicalMatches = 0;
    int lexicalMisses = 0;
    int totalHits = 0;
    int totalChecks = 0;

    try (FileInputStream input =
                 new FileInputStream(INPUT_FILE);
         XSSFWorkbook workbook =
                 new XSSFWorkbook(input)) {

        Sheet sheet = workbook.getSheetAt(0);
        Row header = sheet.getRow(0);

        Map<String, Integer> columns =
                readHeaders(header, formatter);

        
        int englishColumn =
                requireColumn(columns, "EnglishTranslated");

        int[] resultIndexes = new int[6];
        int[] expansionIndexes = new int[6];
        int[] hitIndexes = new int[6];

        for (int j = 0; j < 6; j++) {

            resultIndexes[j] =
                    requireColumn(columns, RESULT_COLUMNS[j]);

            expansionIndexes[j] =
                    ensureColumn(header, columns,
                            EXPANSION_COLUMNS[j]);

            hitIndexes[j] =
                    ensureColumn(header, columns,
                            HIT_COLUMNS[j]);
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {

            Row row = sheet.getRow(i);

            if (row == null) {
                continue;
            }

            
            String englishTranslated =
                    cellText(row, englishColumn, formatter);

            
            setText(row, englishColumn, englishTranslated);

            for (int j = 0; j < 6; j++) {

                String romanWord =
                        cellText(row, resultIndexes[j], formatter);

                List<String> expansions;

                if (romanWord.isEmpty()) {

                    expansions = new ArrayList<String>();

                } else {

                    if (romanIndex.containsKey(romanWord)) {
                        lexicalMatches++;
                    } else {
                        lexicalMisses++;
                    }

                    expansions = expand(romanWord);
                }

                String expandedText =
                        String.join(", ", expansions);

                setText(row, expansionIndexes[j],
                        expandedText);

                boolean hit =
                        isHit(englishTranslated, expansions);

                setText(row, hitIndexes[j],
                        hit ? "Yes" : "No");

                if (hit) {
                    totalHits++;
                }

                totalChecks++;
            }

            processedRows++;

            if (processedRows % 250 == 0) {
                System.out.println(
                        "Processed rows: " + processedRows);
            }
        }

        try (FileOutputStream output =
                     new FileOutputStream(OUTPUT_FILE)) {

            workbook.write(output);
        }
    }

    System.out.println();
    System.out.println(
            "========== CLQE EXPANSION SUMMARY ==========");

    System.out.println("Processed rows: " + processedRows);
    System.out.println("Lexical matches: " + lexicalMatches);
    System.out.println("Lexical misses: " + lexicalMisses);
    System.out.println("Total hit checks: " + totalChecks);
    System.out.println("Total Yes hits: " + totalHits);
    System.out.println(
            "Total No hits: " + (totalChecks - totalHits));

    if (totalChecks > 0) {

        double hitPercentage =
                100.0 * totalHits / totalChecks;

        System.out.printf(
                Locale.ROOT,
                "Overall hit percentage: %.2f%%%n",
                hitPercentage);
    }

    System.out.println(
            "Unique expansion lookups cached: "
            + expansionCache.size());

    System.out.println("Output: " + OUTPUT_FILE);
    System.out.println(
            "============================================");
    System.out.println("PROCESS COMPLETED SUCCESSFULLY");
}

    

    public static void main(String[] args) throws Exception {

        System.out.println("Loading ontology...");

        OWLOntologyManager manager =
                OWLManager.createOWLOntologyManager();

        OWLOntology ontology =
                manager.loadOntologyFromOntologyDocument(
                        new File(ONTOLOGY_PATH));

        System.out.println("Ontology loaded successfully.");

        CLQEOntologyExpansion generator =
                new CLQEOntologyExpansion(
                        ontology,
                        manager.getOWLDataFactory());

        generator.processExcel();
    }
}
