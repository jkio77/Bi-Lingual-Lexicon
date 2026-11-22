package BORUE;




import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;


public class BiOntComprehensiveAudit {
    private static final String NS = "http://localhost:3030/BiOnt.owl#";
    private static final String INPUT = "D:/BORUE/BiOnt.owl";
    private static final String OUTPUT_DIR = "D:/BORUE";
    private static final String[] CODES = {"romMeta","soundexMeta","refSoundMeta","colPhCode",
            "metaphoneCode","dblMetCode","nysiisCode","caver1Code","caver2Code","urduPhoneCode"};
    private static final Set<String> CODE_SET = new HashSet<String>(Arrays.asList(CODES));
    private static final String[] LEXICAL = {"strVal","strValUni"};

    static String name(IRI iri) {
        String s=iri.toString(); int p=Math.max(s.lastIndexOf('#'),s.lastIndexOf('/'));
        return s.substring(p+1);
    }
    static String csv(String s) { return "\""+s.replace("\"","\"\"").replace("\r"," ").replace("\n"," ")+"\""; }
    static void row(PrintWriter w, String... a) {
        for(int i=0;i<a.length;i++){if(i>0)w.print(',');w.print(csv(a[i]));}w.println();
    }
    static PrintWriter writer(File dir,String file) throws IOException {
        return new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(dir,file)),StandardCharsets.UTF_8));
    }
    static void add(Map<String,Long> m,String k,long v) {Long old=m.get(k);m.put(k,(old==null?0:old)+v);}
    static String num(long n){return Long.toString(n);}
    static String values(Set<String> v){return String.join(" | ",v);}
    static String getGroup(Set<String> classes){
        boolean in=false,out=false;
        for(String c:classes){if(c.endsWith("_In")||c.equals("Language_In"))in=true;
            if(c.endsWith("_Out")||c.equals("Language_Out"))out=true;}
        if(in&&out)return "BOTH_IN_OUT";
        if(in)return "ROMAN_URDU_IN";
        if(out)return "ENGLISH_OUT";
        return "OTHER_OR_UNTYPED";
    }
    static class Item {
        String iri;
        Set<String> classes=new TreeSet<String>();
        Map<String,Set<String>> data=new TreeMap<String,Set<String>>();
        Map<String,Integer> dataAxioms=new TreeMap<String,Integer>();
        Map<String,Integer> objects=new TreeMap<String,Integer>();
        Set<String> val(String k){Set<String> s=data.get(k);return s==null?Collections.<String>emptySet():s;}
        int n(String k){Integer v=dataAxioms.get(k);return v==null?0:v;}
    }
    public static void main(String[] args) throws Exception {
        String input=args.length>0?args[0]:INPUT;
        File out=new File(args.length>1?args[1]:OUTPUT_DIR);
        if(!out.exists()&&!out.mkdirs())throw new IOException("Cannot create "+out);
        System.out.println("Loading "+input);
        OWLOntology o=OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File(input));
        Map<IRI,Item> items=new HashMap<IRI,Item>();
        for(OWLNamedIndividual x:o.getIndividualsInSignature()){
            Item it=new Item();it.iri=x.getIRI().toString();items.put(x.getIRI(),it);
        }
        Map<String,Long> axiomTypes=new TreeMap<String,Long>();
        for(AxiomType<?> type:AxiomType.AXIOM_TYPES){long n=o.getAxiomCount(type);if(n>0)axiomTypes.put(type.toString(),n);}
        Map<String,Long> classAxioms=new TreeMap<String,Long>();
        Map<String,Long> objectAxioms=new TreeMap<String,Long>();
        Map<String,Long> dataAxioms=new TreeMap<String,Long>();
        Map<String,Long> objectSelf=new TreeMap<String,Long>();
        Map<String,Long> objectMissingTarget=new TreeMap<String,Long>();
        Map<String,Long> dataBlank=new TreeMap<String,Long>();
        Map<String,Long> dataDistinctValues=new TreeMap<String,Long>();
        Map<String,Set<String>> distinct=new TreeMap<String,Set<String>>();
        Map<String,Set<String>> valueOwners=new TreeMap<String,Set<String>>();
        for(OWLClassAssertionAxiom a:o.getAxioms(AxiomType.CLASS_ASSERTION)){
            if(!a.getIndividual().isNamed())continue;
            Item it=items.get(a.getIndividual().asOWLNamedIndividual().getIRI());if(it==null)continue;
            if(a.getClassExpression().isAnonymous()) {add(classAxioms,"[anonymous class expression]",1);continue;}
            String c=name(a.getClassExpression().asOWLClass().getIRI());it.classes.add(c);add(classAxioms,c,1);
        }
        for(OWLDataPropertyAssertionAxiom a:o.getAxioms(AxiomType.DATA_PROPERTY_ASSERTION)){
            if(!a.getSubject().isNamed())continue;
            Item it=items.get(a.getSubject().asOWLNamedIndividual().getIRI());if(it==null)continue;
            String p=a.getProperty().isAnonymous()?"[anonymous]":name(a.getProperty().asOWLDataProperty().getIRI());
            String v=a.getObject().getLiteral();
            add(dataAxioms,p,1);if(v.trim().isEmpty())add(dataBlank,p,1);
            Set<String> s=it.data.get(p);if(s==null){s=new LinkedHashSet<String>();it.data.put(p,s);}s.add(v);
            Integer n=it.dataAxioms.get(p);it.dataAxioms.put(p,n==null?1:n+1);
            Set<String> d=distinct.get(p);if(d==null){d=new HashSet<String>();distinct.put(p,d);}d.add(v);
            if(p.equals("strVal")){
                String key=v.trim().toLowerCase(Locale.ROOT);
                Set<String> owners=valueOwners.get(key);if(owners==null){owners=new HashSet<String>();valueOwners.put(key,owners);}owners.add(it.iri);
            }
        }
        for(OWLObjectPropertyAssertionAxiom a:o.getAxioms(AxiomType.OBJECT_PROPERTY_ASSERTION)){
            if(!a.getSubject().isNamed())continue;
            Item it=items.get(a.getSubject().asOWLNamedIndividual().getIRI());if(it==null)continue;
            String p=a.getProperty().isAnonymous()?"[inverse object property]":name(a.getProperty().asOWLObjectProperty().getIRI());
            add(objectAxioms,p,1);Integer n=it.objects.get(p);it.objects.put(p,n==null?1:n+1);
            if(a.getObject().isNamed()){
                IRI target=a.getObject().asOWLNamedIndividual().getIRI();
                if(target.equals(a.getSubject().asOWLNamedIndividual().getIRI()))add(objectSelf,p,1);
                if(!items.containsKey(target))add(objectMissingTarget,p,1);
            }
        }
        Map<String,Long> groups=new TreeMap<String,Long>();
        Map<String,Long> groupClass=new TreeMap<String,Long>();
        Map<String,Long> propertyCoverage=new TreeMap<String,Long>();
        Map<String,Long> propertyMissing=new TreeMap<String,Long>();
        Map<String,Long> propertyMultiple=new TreeMap<String,Long>();
        Map<String,Long> groupPropertyAssertions=new TreeMap<String,Long>();
        Map<String,Long> missingPatterns=new TreeMap<String,Long>();
        long noTypes=0,roman=0,english=0,both=0;
        PrintWriter individuals=writer(out,"individuals.csv");
        PrintWriter issues=writer(out,"missing_or_multiple_properties.csv");
        row(individuals,"Individual_IRI","Group","Asserted_Classes","strVal_values","strValUni_values","romMeta_values","Data_property_assertion_total","Object_property_assertion_total");
        row(issues,"Individual_IRI","Group","Asserted_Classes","Property","Assertion_count","Values","Issue");
        for(Item it:items.values()){
            String g=getGroup(it.classes);add(groups,g,1);
            if(it.classes.isEmpty())noTypes++;
            if(g.equals("ROMAN_URDU_IN"))roman++;
            if(g.equals("ENGLISH_OUT"))english++;
            if(g.equals("BOTH_IN_OUT"))both++;
            for(String c:it.classes)add(groupClass,g+" / "+c,1);
            long dn=0,on=0;for(Integer n:it.dataAxioms.values())dn+=n;for(Integer n:it.objects.values())on+=n;
            row(individuals,it.iri,g,values(it.classes),values(it.val("strVal")),values(it.val("strValUni")),values(it.val("romMeta")),num(dn),num(on));
            Set<String> check=new LinkedHashSet<String>();check.addAll(Arrays.asList(LEXICAL));check.addAll(Arrays.asList(CODES));
            for(String p:check){
                int n=it.n(p);add(propertyCoverage,g+" / "+p,n>0?1:0);
                add(groupPropertyAssertions,g+" / "+p,n);
                if(n==0){add(propertyMissing,g+" / "+p,1);}
                if(n>1){add(propertyMultiple,g+" / "+p,1);}
                boolean expected=(g.equals("ROMAN_URDU_IN")&&(p.equals("strVal")||CODE_SET.contains(p)))
                    ||(g.equals("ENGLISH_OUT")&&(p.equals("strVal")||p.equals("strValUni")));
                if(expected&&(n==0||n>1)){
                    row(issues,it.iri,g,values(it.classes),p,num(n),values(it.val(p)),n==0?"MISSING":"MULTIPLE");
                }
            }
            if(g.equals("ROMAN_URDU_IN")){
                String pattern=(it.n("strVal")>0?"strVal:Y":"strVal:N")+";"+(it.n("romMeta")>0?"romMeta:Y":"romMeta:N");
                add(missingPatterns,pattern,1);
            }
        }
        individuals.close();issues.close();
        PrintWriter summary=writer(out,"summary.csv");row(summary,"Metric","Count");
        row(summary,"Total declared named individuals",num(items.size()));
        row(summary,"Roman Urdu *_In (directly asserted)",num(roman));
        row(summary,"English *_Out (directly asserted)",num(english));
        row(summary,"Both _In and _Out",num(both));
        row(summary,"Untyped or other",num(groups.containsKey("OTHER_OR_UNTYPED")?groups.get("OTHER_OR_UNTYPED"):0));
        row(summary,"Named classes in signature",num(o.getClassesInSignature().size()));
        row(summary,"Object properties in signature",num(o.getObjectPropertiesInSignature().size()));
        row(summary,"Data properties in signature",num(o.getDataPropertiesInSignature().size()));
        row(summary,"Ontology logical + annotation axioms",num(o.getAxiomCount()));
        summary.close();
        writeCounts(out,"asserted_class_membership.csv","Class",classAxioms);
        writeCounts(out,"class_membership_by_group.csv","Group / Class",groupClass);
        writeCounts(out,"individual_groups.csv","Group",groups);
        writeCounts(out,"data_property_assertions.csv","Data property",dataAxioms);
        writeCounts(out,"object_property_assertions.csv","Object property",objectAxioms);
        writeCounts(out,"axiom_types.csv","Axiom type",axiomTypes);
        writeCounts(out,"property_coverage_by_group.csv","Group / Property (individuals with >=1)",propertyCoverage);
        writeCounts(out,"property_missing_by_group.csv","Group / Property (individuals with 0)",propertyMissing);
        writeCounts(out,"property_multiple_by_group.csv","Group / Property (individuals with >1)",propertyMultiple);
        writeCounts(out,"property_assertions_by_group.csv","Group / Property (assertions)",groupPropertyAssertions);
        writeCounts(out,"roman_strval_rommeta_patterns.csv","Pattern",missingPatterns);
        writeCounts(out,"blank_data_assertions.csv","Data property",dataBlank);
        writeCounts(out,"self_object_relations.csv","Object property",objectSelf);
        writeCounts(out,"object_targets_not_in_signature.csv","Object property",objectMissingTarget);
        PrintWriter distinctOut=writer(out,"data_property_distinct_values.csv");row(distinctOut,"Property","Distinct literal values","Assertion count");
        for(String p:distinct.keySet())row(distinctOut,p,num(distinct.get(p).size()),num(dataAxioms.get(p)));
        distinctOut.close();
        PrintWriter dup=writer(out,"duplicate_roman_lexical_values.csv");row(dup,"Normalized strVal","Number of individuals","Individual IRIs");
        for(Map.Entry<String,Set<String>> e:valueOwners.entrySet())if(e.getValue().size()>1)
            row(dup,e.getKey(),num(e.getValue().size()),String.join(" | ",e.getValue()));
        dup.close();
        PrintWriter hierarchy=writer(out,"subclass_axioms.csv");row(hierarchy,"Subclass","Superclass");
        for(OWLSubClassOfAxiom a:o.getAxioms(AxiomType.SUBCLASS_OF))
            row(hierarchy,a.getSubClass().isAnonymous()?a.getSubClass().toString():name(a.getSubClass().asOWLClass().getIRI()),
                a.getSuperClass().isAnonymous()?a.getSuperClass().toString():name(a.getSuperClass().asOWLClass().getIRI()));
        hierarchy.close();
        System.out.println("Individuals: "+items.size()+", Roman Urdu: "+roman+", English: "+english);
        System.out.println("romMeta assertions: "+dataAxioms.get("romMeta")+", strVal assertions: "+dataAxioms.get("strVal"));
        System.out.println("Output directory: "+out.getAbsolutePath());
        System.out.println("IMPORTANT: Counts are asserted, not inferred; direct *_In/_Out grouping; no reasoner.");
    }
    static void writeCounts(File dir,String file,String label,Map<String,Long> m)throws IOException{
        PrintWriter w=writer(dir,file);row(w,label,"Count");for(Map.Entry<String,Long> e:m.entrySet())row(w,e.getKey(),num(e.getValue()));w.close();
    }
}
