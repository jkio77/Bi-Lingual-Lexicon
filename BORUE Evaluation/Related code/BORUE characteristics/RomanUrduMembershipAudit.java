package BORUE;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;


public class RomanUrduMembershipAudit {
    private static final String INPUT = "D:/BORUE/BiOnt.owl";
    private static final String OUTPUT = "D:/BORUE/RomanUrduMembershipAudit.xlsx";
    private static final String BASE = "http://localhost:3030/BiOnt.owl#";



    private static final boolean COUNT_ALL_DESCENDANT_CLASSES = false;

    static class Record {
        String iri;
        List<String> values = new ArrayList<>();
        List<OWLClass> classes = new ArrayList<>();
        Record(String iri) { this.iri = iri; }
    }

    public static void main(String[] args) throws Exception {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(INPUT));
        OWLDataFactory df = manager.getOWLDataFactory();
        OWLClass root = df.getOWLClass(IRI.create(BASE + "Language_In"));
        OWLDataProperty strVal = df.getOWLDataProperty(IRI.create(BASE + "strVal"));
        Set<OWLClass> descendants = new HashSet<>();
        collect(root, ontology, descendants);

        List<Record> records = new ArrayList<>();
        Map<String, Integer> categoryCounts = new TreeMap<>();
        Map<String, Integer> wordCounts = new HashMap<>();
        int totalValues = 0, memberships = 0, extraValues = 0;

        for (OWLNamedIndividual individual : ontology.getIndividualsInSignature()) {
            Set<OWLClass> asserted = new HashSet<>();
            for (OWLClassExpression type : EntitySearcher.getTypes(individual, ontology)) {
                if (!type.isAnonymous()) asserted.add(type.asOWLClass());
            }
            boolean isRoman = false;
            for (OWLClass cls : asserted) {
                if (descendants.contains(cls)) { isRoman = true; break; }
            }
            if (!isRoman) continue;
            Record r = new Record(individual.getIRI().toString());
            for (OWLLiteral literal : EntitySearcher.getDataPropertyValues(individual, strVal, ontology)) {
                String v = literal.getLiteral().trim();
                if (!v.isEmpty()) r.values.add(v);
            }
            Collections.sort(r.values, String.CASE_INSENSITIVE_ORDER);
            for (String v : r.values) wordCounts.merge(v, 1, Integer::sum);
            totalValues += r.values.size();
            extraValues += Math.max(0, r.values.size() - 1);

            for (OWLClass cls : asserted) {
                if (cls.equals(root) || !descendants.contains(cls)) continue;
                if (!COUNT_ALL_DESCENDANT_CLASSES && hasAssertedDescendant(cls, asserted, ontology)) continue;
                r.classes.add(cls);
            }
            r.classes.sort(Comparator.comparing(c -> c.getIRI().toString()));
            for (OWLClass cls : r.classes) {
                categoryCounts.merge(cls.getIRI().getShortForm(), 1, Integer::sum);
            }
            memberships += r.classes.size();
            records.add(r);
        }
        records.sort(Comparator.comparing(r -> r.iri));
        int distinctWords = wordCounts.size();
        int repeatExcess = totalValues - distinctWords;
        int membershipExcess = memberships - records.size();
        int unclassified = 0;
        int multiClass = 0;
        int multiValue = 0;
        for (Record r : records) {
            if (r.classes.isEmpty()) unclassified++;
            if (r.classes.size() > 1) multiClass++;
            if (r.values.size() > 1) multiValue++;
        }

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet summary = wb.createSheet("Summary");
            String[][] metrics = {
                {"Metric", "Computed value"},
                {"Distinct Language_In individuals", "" + records.size()},
                {"Grammatical class memberships", "" + memberships},
                {"Memberships minus individuals", "" + membershipExcess},
                {"Individuals with multiple classes", "" + multiClass},
                {"Individuals without counted grammatical class", "" + unclassified},
                {"Nonempty strVal assertions", "" + totalValues},
                {"Distinct exact strVal strings", "" + distinctWords},
                {"Extra strVal assertions over individuals", "" + (totalValues - records.size())},
                {"Individuals with multiple strVal", "" + multiValue},
                {"Extra strVal beyond first per individual", "" + extraValues},
                {"Repeated strVal excess", "" + repeatExcess},
                {"Memberships minus strVal assertions", "" + (memberships - totalValues)},
                {"Expected manuscript memberships", "28242"},
                {"Expected manuscript individuals", "25640"},
                {"Expected extracted strVal count", "25888"},
                {"Memberships match manuscript", "" + (memberships == 28242)},
                {"Individuals match manuscript", "" + (records.size() == 25640)},
                {"strVal matches previous extraction", "" + (totalValues == 25888)},
                {"Counting mode", COUNT_ALL_DESCENDANT_CLASSES ? "All asserted descendant classes" : "Most-specific asserted descendant classes"}
            };
            for (int i = 0; i < metrics.length; i++) {
                Row row = summary.createRow(i);
                row.createCell(0).setCellValue(metrics[i][0]);
                row.createCell(1).setCellValue(metrics[i][1]);
            }
            Sheet cats = wb.createSheet("Class Counts");
            header(cats, "Asserted grammatical class", "Memberships");
            int rowIndex = 1;
            for (Map.Entry<String,Integer> e : categoryCounts.entrySet()) {
                Row row = cats.createRow(rowIndex++);
                row.createCell(0).setCellValue(e.getKey());
                row.createCell(1).setCellValue(e.getValue());
            }
            Sheet multi = wb.createSheet("Multi-Class Individuals");
            header(multi, "Individual IRI", "strVal values", "Class count", "Excess memberships", "Grammatical classes");
            Sheet membershipDetails = wb.createSheet("Membership Details");
            header(membershipDetails, "Individual IRI", "strVal values", "Grammatical class IRI");
            Sheet values = wb.createSheet("Multi-strVal Individuals");
            header(values, "Individual IRI", "strVal count", "Extra beyond first", "strVal values", "Grammatical classes");
            Sheet all = wb.createSheet("All Individuals");
            header(all, "Individual IRI", "strVal count", "strVal values", "Grammatical memberships", "Grammatical classes");
            int mr=1, dr=1, vr=1, ar=1;
            for (Record r : records) {
                String words = String.join(" | ", r.values);
                String classes = r.classes.stream().map(c -> c.getIRI().getShortForm()).collect(Collectors.joining(" | "));
                Row a = all.createRow(ar++);
                a.createCell(0).setCellValue(r.iri);
                a.createCell(1).setCellValue(r.values.size());
                a.createCell(2).setCellValue(words);
                a.createCell(3).setCellValue(r.classes.size());
                a.createCell(4).setCellValue(classes);
                if (r.classes.size() > 1) {
                    Row m = multi.createRow(mr++);
                    m.createCell(0).setCellValue(r.iri);
                    m.createCell(1).setCellValue(words);
                    m.createCell(2).setCellValue(r.classes.size());
                    m.createCell(3).setCellValue(r.classes.size()-1);
                    m.createCell(4).setCellValue(classes);
                }
                for (OWLClass cls : r.classes) {
                    Row d = membershipDetails.createRow(dr++);
                    d.createCell(0).setCellValue(r.iri);
                    d.createCell(1).setCellValue(words);
                    d.createCell(2).setCellValue(cls.getIRI().toString());
                }
                if (r.values.size() > 1) {
                    Row v = values.createRow(vr++);
                    v.createCell(0).setCellValue(r.iri);
                    v.createCell(1).setCellValue(r.values.size());
                    v.createCell(2).setCellValue(r.values.size()-1);
                    v.createCell(3).setCellValue(words);
                    v.createCell(4).setCellValue(classes);
                }
            }
            for (int s=0; s<wb.getNumberOfSheets(); s++) {
                Sheet sheet=wb.getSheetAt(s);
                sheet.createFreezePane(0,1);
                sheet.setColumnWidth(0, Math.min(80*256, 255*256));
                for (int c=1; c<5; c++) sheet.setColumnWidth(c, 32*256);
                if (sheet.getLastRowNum()>0 && s>0) {
                    int n=sheet.getRow(0).getLastCellNum();
                    sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,sheet.getLastRowNum(),0,n-1));
                }
            }
            try (FileOutputStream out = new FileOutputStream(OUTPUT)) { wb.write(out); }
        }
        System.out.println("Individuals=" + records.size() + ", memberships=" + memberships
                + ", strVal=" + totalValues + ", distinct strVal=" + distinctWords);
        System.out.println("Membership excess=" + membershipExcess + ", multi-class individuals=" + multiClass
                + ", unclassified=" + unclassified + ", multi-strVal individuals=" + multiValue);
        System.out.println("Saved: " + OUTPUT);
    }

    private static boolean hasAssertedDescendant(OWLClass parent, Set<OWLClass> asserted, OWLOntology ontology) {
        Set<OWLClass> descendants = new HashSet<>();
        collect(parent, ontology, descendants);
        descendants.remove(parent);
        for (OWLClass cls : asserted) if (descendants.contains(cls)) return true;
        return false;
    }

    private static void collect(OWLClass parent, OWLOntology ontology, Set<OWLClass> found) {
        if (!found.add(parent)) return;
        for (OWLSubClassOfAxiom ax : ontology.getSubClassAxiomsForSuperClass(parent)) {
            if (!ax.getSubClass().isAnonymous()) collect(ax.getSubClass().asOWLClass(), ontology, found);
        }
    }

    private static void header(Sheet sheet, String... names) {
        Row row = sheet.createRow(0);
        for (int i=0; i<names.length; i++) row.createCell(i).setCellValue(names[i]);
    }
}
