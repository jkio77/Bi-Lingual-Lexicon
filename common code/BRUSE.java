package BORUE;

import java.io.File;
import java.util.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;

public class BRUSE {
    private static final String ONTOLOGY_PATH = "D:\\BORUE\\BiOnt.owl";
    private static final String BASE_IRI = "http://localhost:3030/BiOnt.owl#";

    private final OWLOntology ontology;
    private final OWLDataProperty strVal;
    private final OWLDataProperty strValUni;
    private final Map<String, List<OWLNamedIndividual>> exactIndex = new TreeMap<>();

    public static final class Translation {
        public final String english;
        public final String urdu;
        public final String individualIRI;
        public Translation(String english, String urdu, String iri) {
            this.english = english; this.urdu = urdu; this.individualIRI = iri;
        }
        @Override public String toString() {
            return english + " | " + (urdu.isEmpty() ? "[strValUni unavailable]" : urdu);
        }
    }

    public static final class ExpansionResult {
        public final String query;
        public final int k;
        public final String sourceIRI;
        public final List<String> synonyms;
        public final List<String> semanticSimilar;
        public final List<Translation> translations;
        ExpansionResult(String query, int k, String sourceIRI, List<String> synonyms,
                        List<String> semanticSimilar, List<Translation> translations) {
            this.query = query; this.k = k; this.sourceIRI = sourceIRI;
            this.synonyms = Collections.unmodifiableList(synonyms);
            this.semanticSimilar = Collections.unmodifiableList(semanticSimilar);
            this.translations = Collections.unmodifiableList(translations);
        }
    }

    public BRUSE() throws Exception {
        File file = new File(ONTOLOGY_PATH);
        if (!file.isFile()) throw new java.io.FileNotFoundException("Ontology not found: " + file);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        ontology = manager.loadOntologyFromOntologyDocument(file);
        OWLDataFactory factory = manager.getOWLDataFactory();
        strVal = factory.getOWLDataProperty(IRI.create(BASE_IRI + "strVal"));
        strValUni = factory.getOWLDataProperty(IRI.create(BASE_IRI + "strValUni"));
        OWLClass languageIn = factory.getOWLClass(IRI.create(BASE_IRI + "Language_In"));
        Set<OWLClass> eligibleClasses = new HashSet<>();
        collectSubClasses(languageIn, eligibleClasses);
        for (OWLNamedIndividual individual : ontology.getIndividualsInSignature()) {
            boolean eligible = false;
            for (OWLClassExpression type : EntitySearcher.getTypes(individual, ontology)) {
                if (!type.isAnonymous() && eligibleClasses.contains(type.asOWLClass())) {
                    eligible = true; break;
                }
            }
            if (!eligible) continue;
            for (String value : values(individual, strVal)) {
                exactIndex.computeIfAbsent(normalize(value), x -> new ArrayList<>()).add(individual);
            }
        }
        for (List<OWLNamedIndividual> entries : exactIndex.values()) {
            entries.sort(Comparator.comparing(x -> x.getIRI().toString()));
        }
        System.out.println("Ontology loaded: " + file.getAbsolutePath());
        System.out.println("Indexed exact Roman Urdu words: " + exactIndex.size());
    }

    private void collectSubClasses(OWLClass cls, Set<OWLClass> found) {
        if (!found.add(cls)) return;
        for (OWLSubClassOfAxiom ax : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
            if (!ax.getSuperClass().isAnonymous() && ax.getSuperClass().asOWLClass().equals(cls)
                    && !ax.getSubClass().isAnonymous()) {
                collectSubClasses(ax.getSubClass().asOWLClass(), found);
            }
        }
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private List<String> values(OWLNamedIndividual individual, OWLDataProperty property) {
        TreeSet<String> result = new TreeSet<>();
        EntitySearcher.getDataPropertyValues(individual, property, ontology).forEach(literal -> {
            String s = literal.getLiteral().trim();
            if (!s.isEmpty()) result.add(s);
        });
        return new ArrayList<>(result);
    }

    private String firstValue(OWLNamedIndividual individual, OWLDataProperty property) {
        List<String> list = values(individual, property);
        return list.isEmpty() ? "" : list.get(0);
    }

    private static boolean isEnglishRelation(String propertyName) {
        
        return propertyName.startsWith("isRoman") && propertyName.endsWith("For");
    }

    private static final class Edge {
        final String relation;
        final OWLNamedIndividual target;
        Edge(String relation, OWLNamedIndividual target) {
            this.relation = relation; this.target = target;
        }
    }

    private List<Edge> directEdges(OWLNamedIndividual source) {
        List<Edge> edges = new ArrayList<>();
        for (OWLObjectPropertyAssertionAxiom ax : ontology.getObjectPropertyAssertionAxioms(source)) {
            if (ax.getProperty().isAnonymous() || !ax.getObject().isNamed()) continue;
            String property = ax.getProperty().asOWLObjectProperty().getIRI().getShortForm();
            edges.add(new Edge(property, ax.getObject().asOWLNamedIndividual()));
        }
        edges.sort(Comparator.comparing((Edge e) -> e.relation)
                .thenComparing(e -> e.target.getIRI().toString()));
        return edges;
    }


    public ExpansionResult BRUSE(String query, int k, char syn, char sem) {
        if (query == null || normalize(query).isEmpty())
            throw new IllegalArgumentException("Query must be a nonempty exact ontology word.");
        if (k < 0) throw new IllegalArgumentException("k cannot be negative.");
        syn = Character.toLowerCase(syn);
        sem = Character.toLowerCase(sem);
        if ((syn != 'y' && syn != 'n') || (sem != 'y' && sem != 'n'))
            throw new IllegalArgumentException("syn and sem must each be y or n.");

        List<OWLNamedIndividual> matches = exactIndex.get(normalize(query));
        if (matches == null || matches.isEmpty())
            throw new IllegalArgumentException("Exact Roman Urdu strVal not found: " + query);
        if (matches.size() > 1)
            System.out.println("Note: " + matches.size() + " individuals share this strVal; using first IRI in sorted order.");
        OWLNamedIndividual source = matches.get(0);
        boolean includeSyn = k == 0 || syn == 'y';
        boolean includeSem = k == 0 || sem == 'y';
        TreeSet<String> synonyms = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        TreeSet<String> similar = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        TreeMap<String, Translation> english = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Edge edge : directEdges(source)) {
            if (edge.relation.equals("isSynFor") && includeSyn) {
                for (String v : values(edge.target, strVal)) synonyms.add(v);
            } else if (edge.relation.equals("isSemSimTo") && includeSem) {
                for (String v : values(edge.target, strVal)) similar.add(v);
            } else if (isEnglishRelation(edge.relation)) {
                String urdu = firstValue(edge.target, strValUni);
                
                for (String v : values(edge.target, strVal)) {
                    english.putIfAbsent(v, new Translation(v, urdu, edge.target.getIRI().toString()));
                }
            }
        }
        return new ExpansionResult(query, k, source.getIRI().toString(),
                take(synonyms, k), take(similar, k), takeTranslations(english.values(), k));
    }

    private static List<String> take(Collection<String> input, int k) {
        List<String> result = new ArrayList<>();
        for (String value : input) {
            if (k > 0 && result.size() >= k) break;
            result.add(value);
        }
        return result;
    }

    private static List<Translation> takeTranslations(Collection<Translation> input, int k) {
        List<Translation> result = new ArrayList<>();
        for (Translation value : input) {
            if (k > 0 && result.size() >= k) break;
            result.add(value);
        }
        return result;
    }

    
    private static void printResult(ExpansionResult result) {
        
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        terms.addAll(result.synonyms);
        terms.addAll(result.semanticSimilar);
        for (Translation translation : result.translations) {
            if (!translation.english.isEmpty()) terms.add(translation.english);
            if (!translation.urdu.isEmpty()) terms.add(translation.urdu);
        }
        System.out.println("{" + String.join(",", terms) + "}");
    }

    public static void main(String[] args) {
        try {
            BRUSE engine = new BRUSE();
            Scanner scanner = new Scanner(System.in);
            System.out.println("valid query example: = aadat,2,y,y");
            System.out.println("=====================================================================================");
            while (true) {
            	      
            	     System.out.print("Enter query for expansion in format query,k(1,2etc),syn-y/n,sem-y/n (Enter to end): ");
                if (!scanner.hasNextLine()) break;
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) break;
                String[] parts = line.split(",", -1);
                if (parts.length != 4) {
                    System.out.println("Invalid format. Example: hakumat,2,y,y\n");
                    continue;
                }
                try {
                    String query = parts[0].trim();
                    int k = Integer.parseInt(parts[1].trim());
                    String synText = parts[2].trim();
                    String semText = parts[3].trim();
                    if (synText.length() != 1 || semText.length() != 1)
                        throw new IllegalArgumentException("syn and sem must be y or n.");
                    printResult(engine.BRUSE(query, k, synText.charAt(0), semText.charAt(0)));
                } catch (IllegalArgumentException ex) {
                    System.out.println("Input error: " + ex.getMessage() + "\n");
                }
            }
            System.out.println("BRUSE finished.");
        } catch (Exception ex) {
            System.err.println("BRUSE initialization failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
