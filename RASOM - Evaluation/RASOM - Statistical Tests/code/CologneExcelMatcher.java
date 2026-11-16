package org.borue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.language.ColognePhonetic;

import org.apache.poi.ss.usermodel.Cell;
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
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import org.semanticweb.owlapi.search.EntitySearcher;


public class CologneExcelMatcher {


    public static void main(String[] args) {


        try {


            String ontologyPath = "D:/BORUE/BiOnt.owl";

            
            String excelPath = "D:/BORUE/TestBenchFinalCount.xlsx";
                    


            String outputPath = "D:/BROUE/Cologne_Result.xlsx";
                    



            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();



            OWLOntology ontology =  manager.loadOntologyFromOntologyDocument(new File(ontologyPath));



            System.out.println("Ontology loaded successfully");



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



            OWLDataProperty colPhCode =
                    factory.getOWLDataProperty(
                            IRI.create(
                                    baseIRI + "colPhCode"
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



            Map<String,List<String>> cologneIndex =
                    buildCologneIndex(
                            ontology,
                            languageClasses,
                            strVal,
                            colPhCode
                    );



            System.out.println(
                    "Cologne index size: "
                    +
                    cologneIndex.size()
            );



            processExcel(
                    excelPath,
                    outputPath,
                    cologneIndex
            );



            System.out.println(
                    "Processing completed successfully"
            );


            System.out.println(
                    "Output file:"
            );


            System.out.println(
                    outputPath
            );


        }
        catch(Exception e) {


            e.printStackTrace();

        }


    }





    private static Map<String,List<String>> buildCologneIndex(
            OWLOntology ontology,
            Set<OWLClass> languageClasses,
            OWLDataProperty strVal,
            OWLDataProperty colPhCode) {



        Map<String,List<String>> index =
                new HashMap<String,List<String>>();



        int count = 0;



        for(OWLNamedIndividual individual :
                ontology.getIndividualsInSignature()) {



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


                    isLanguageIndividual = true;

                    break;

                }

            }



            if(!isLanguageIndividual) {

                continue;

            }



            List<String> strValues =
                    new ArrayList<String>();



            EntitySearcher
            .getDataPropertyValues(
                    individual,
                    strVal,
                    ontology
            )
            .forEach(
                    (OWLLiteral literal) -> {


                strValues.add(
                        literal.getLiteral()
                               .trim()
                );


            });



            List<String> cologneValues =
                    new ArrayList<String>();



            EntitySearcher
            .getDataPropertyValues(
                    individual,
                    colPhCode,
                    ontology
            )
            .forEach(
                    (OWLLiteral literal) -> {


                cologneValues.add(
                        literal.getLiteral()
                               .trim()
                );


            });



            if(strValues.isEmpty()
                    ||
               cologneValues.isEmpty()) {

                continue;

            }



            String originalValue =
                    strValues.get(0);



            String[] codes =
                    cologneValues
                    .get(0)
                    .split("\\s+");



            for(String code : codes) {


                if(code.trim().isEmpty()) {

                    continue;

                }



                if(!index.containsKey(code)) {

                    index.put(
                            code,
                            new ArrayList<String>()
                    );

                }



                if(!index.get(code)
                        .contains(originalValue)) {


                    index.get(code)
                         .add(originalValue);

                }


            }



            count++;



            if(count % 500 == 0) {

                System.out.println(
                        "Individuals indexed: "
                        +
                        count
                );

            }


        }



        return index;

    }
    private static void processExcel(
            String excelPath,
            String outputPath,
            Map<String,List<String>> cologneIndex)
            throws Exception {



        FileInputStream input =
                new FileInputStream(
                        excelPath
                );



        XSSFWorkbook workbook =
                new XSSFWorkbook(
                        input
                );



        Sheet sheet =
                workbook.getSheetAt(0);



        ColognePhonetic encoder =
                new ColognePhonetic();



        int processedRows = 0;



        for(Row row : sheet) {



            if(row.getRowNum() == 0) {

                continue;

            }



            for(int i = 0; i < 6; i++) {



                Cell inputCell =
                        row.getCell(i);



                Cell outputCell =
                        row.getCell(i + 8);  // Originally it was 6 here but due to inclo BORUE column i made it 7 and then made 8 after inclusion of count column



                if(outputCell == null) {

                    outputCell =
                            row.createCell(i + 8); // Originally it was 6 here but due to inclo BORUE column i made it 7 and then made 8 after inclusion of count column

                }



                if(inputCell == null) {continue;}



                String value =
                        inputCell
                        .toString()
                        .trim();



                if(value.isEmpty()) {

                    continue;

                }



                String code =
                        encoder.encode(
                                value
                        );



                List<String> matches =
                        cologneIndex.get(code);



                if(matches != null
                        &&
                   !matches.isEmpty()) {



                    /*
                     * Excel cell maximum size:
                     * 32,767 characters
                     *
                     * Build output safely.
                     * Excess matches are discarded.
                     */

                    StringBuilder result =
                            new StringBuilder();



                    for(String match : matches) {



                        if(result.length() > 0) {


                            if(result.length() + 2 > 32767) {

                                break;

                            }


                            result.append(
                                    ", "
                            );


                        }



                        int remaining =
                                32767 -
                                result.length();



                        if(remaining <= 0) {

                            break;

                        }



                        if(match.length() <= remaining) {


                            result.append(
                                    match
                            );


                        }
                        else {


                            result.append(
                                    match.substring(
                                            0,
                                            remaining
                                    )
                            );


                            break;

                        }


                    }



                    outputCell.setCellValue(
                            result.toString()
                    );


                }


            }



            processedRows++;



            if(processedRows % 500 == 0) {

                System.out.println(
                        "Excel rows processed: "
                        +
                        processedRows
                );

            }


        }



        input.close();



        FileOutputStream output =
                new FileOutputStream(
                        outputPath
                );



        workbook.write(
                output
        );



        output.close();



        workbook.close();


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


}
    