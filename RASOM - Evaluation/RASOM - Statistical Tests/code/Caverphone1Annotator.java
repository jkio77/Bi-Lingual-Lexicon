package BORUE;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.language.Caverphone1;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import org.semanticweb.owlapi.search.EntitySearcher;


public class Caverphone1Annotator {


    public static void main(String[] args) {


        try {


            String ontologyPath =
                    "D:/BORUE/BiOnt.owl";



            OWLOntologyManager manager =
                    OWLManager.createOWLOntologyManager();



            OWLOntology ontology =
                    manager.loadOntologyFromOntologyDocument(
                            new File(ontologyPath)
                    );



            System.out.println(
                    "Ontology loaded successfully"
            );



            OWLDataFactory factory =
                    manager.getOWLDataFactory();



            String baseIRI =
                    "http://localhost:3030/BiOnt.owl#";



            OWLDataProperty strVal =
                    factory.getOWLDataProperty(
                            IRI.create(
                                    baseIRI + "strVal"
                            )
                    );



            OWLDataProperty caver1Code =
                    factory.getOWLDataProperty(
                            IRI.create(
                                    baseIRI + "caver1Code"
                            )
                    );



            OWLClass languageIn =
                    factory.getOWLClass(
                            IRI.create(
                                    baseIRI + "Language_In"
                            )
                    );



            Set<OWLClass> languageClasses =
                    new HashSet<OWLClass>();



            collectSubClasses(
                    languageIn,
                    ontology,
                    languageClasses
            );



            System.out.println(
                    "Language classes found: "
                    +
                    languageClasses.size()
            );



            for(OWLClass cls : languageClasses) {

                System.out.println(
                        cls.getIRI()
                );

            }



            Caverphone1 encoder =
                    new Caverphone1();



            int processed = 0;
            int matched = 0;



            Set<OWLAxiom> newAxioms =
                    new HashSet<OWLAxiom>();



            Set<OWLAxiom> removedAxioms =
                    new HashSet<OWLAxiom>();



            System.out.println(
                    "Processing language individuals..."
            );



            int totalIndividuals =
                    ontology
                    .getIndividualsInSignature()
                    .size();



            System.out.println(
                    "Total ontology individuals: "
                    +
                    totalIndividuals
            );



            for(OWLNamedIndividual individual :
                    ontology.getIndividualsInSignature()) {



                processed++;



                if(processed % 100 == 0) {

                    System.out.println(
                            "Checked individuals: "
                            +
                            processed
                    );

                }



                boolean isLanguageIndividual =
                        false;



                for(OWLClassExpression type :
                        EntitySearcher
                        .getTypes(
                                individual,
                                ontology
                        )) {



                    if(type instanceof OWLClass
                            &&
                       languageClasses.contains(
                               type.asOWLClass()
                       )) {


                        isLanguageIndividual =
                                true;

                        break;

                    }

                }



                if(!isLanguageIndividual) {

                    continue;

                }



                matched++;



                /*
                 * Remove previous caver1Code values
                 */

                EntitySearcher
                .getDataPropertyValues(
                        individual,
                        caver1Code,
                        ontology
                )
                .forEach(
                        (OWLLiteral literal) -> {


                    OWLAxiom oldAxiom =
                            factory
                            .getOWLDataPropertyAssertionAxiom(
                                    caver1Code,
                                    individual,
                                    literal
                            );


                    removedAxioms.add(
                            oldAxiom
                    );


                });



                EntitySearcher
                .getDataPropertyValues(
                        individual,
                        strVal,
                        ontology
                )
                .forEach(
                        (OWLLiteral literal) -> {



                    String value =
                            literal.getLiteral()
                                   .trim();



                    if(value.isEmpty()) {

                        return;

                    }



                    String encodedValue =
                            generateCaverphone1Code(
                                    value,
                                    encoder
                            );



                    if(encodedValue.isEmpty()) {

                        return;

                    }



                    OWLAxiom axiom =
                            factory
                            .getOWLDataPropertyAssertionAxiom(
                                    caver1Code,
                                    individual,
                                    encodedValue
                            );



                    newAxioms.add(
                            axiom
                    );



                    System.out.println(
                            value
                            +
                            " --> "
                            +
                            encodedValue
                    );


                });



            }



            System.out.println(
                    "Language individuals found: "
                    +
                    matched
            );



            System.out.println(
                    "Old caver1Code assertions removed: "
                    +
                    removedAxioms.size()
            );



            System.out.println(
                    "New caver1Code assertions prepared: "
                    +
                    newAxioms.size()
            );



            manager.removeAxioms(
                    ontology,
                    removedAxioms
            );



            manager.addAxioms(
                    ontology,
                    newAxioms
            );



            System.out.println(
                    "Saving ontology..."
            );



            manager.saveOntology(
                    ontology,
                    IRI.create(
                            new File(
                                    ontologyPath
                            )
                    )
            );



            System.out.println(
                    "Ontology updated successfully:"
            );



            System.out.println(
                    ontologyPath
            );


        }
        catch(Exception e) {


            e.printStackTrace();

        }


    }





    private static void collectSubClasses(
            OWLClass parent,
            OWLOntology ontology,
            Set<OWLClass> result) {



        if(result.contains(parent)) {

            return;

        }



        result.add(parent);



        for(OWLClassExpression child :
                ontology
                .getSubClassAxiomsForSuperClass(parent)
                .stream()
                .map(ax -> ax.getSubClass())
                .collect(Collectors.toList())) {



            if(child instanceof OWLClass) {


                collectSubClasses(
                        child.asOWLClass(),
                        ontology,
                        result
                );


            }

        }


    }





    private static String generateCaverphone1Code(
            String value,
            Caverphone1 encoder) {



        StringBuilder result =
                new StringBuilder();



        String[] words =
                value.split("\\s+");



        for(String word : words) {



            if(word.trim().isEmpty()) {

                continue;

            }



            String encoded =
                    encoder.encode(
                            word
                    );



            if(result.length() > 0) {

                result.append(" ");

            }



            result.append(
                    encoded
            );


        }



        return result.toString();

    }


}
