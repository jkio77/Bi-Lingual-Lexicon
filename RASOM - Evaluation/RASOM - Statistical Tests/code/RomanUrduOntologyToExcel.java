package BORUE;



import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import org.semanticweb.owlapi.search.EntitySearcher;



public class RomanUrduOntologyToExcel {


    public static void main(String[] args) {


        try {



            String ontologyPath = "D:/BORUE/BiOnt.owl";


            String outputPath ="D:/BORUE/RomanUrduWords.xlsx";




            OWLOntologyManager manager =
                    OWLManager.createOWLOntologyManager();


            OWLOntology ontology =
                    manager.loadOntologyFromOntologyDocument(
                            new File(ontologyPath)
                    );


            System.out.println("Ontology loaded successfully");



            

            OWLDataFactory factory =
                    manager.getOWLDataFactory();



            

            String baseIRI = "http://localhost:3030/BiOnt.owl#";



            

            OWLDataProperty strVal =
                    factory.getOWLDataProperty(
                            IRI.create(
                                    baseIRI + "strVal"
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
                        "Language class: "
                        +
                        cls.getIRI()
                );

            }



       
            Set<String> romanUrduWords =
                    new HashSet<String>();


            int checkedIndividuals = 0;

            int languageIndividuals = 0;

            int strValCount = 0;



            int totalIndividuals =
                    ontology
                    .getIndividualsInSignature()
                    .size();


            System.out.println(
                    "Total ontology individuals: "
                    +
                    totalIndividuals
            );


            System.out.println("Reading Language_In individuals...");



            

            for(OWLNamedIndividual individual :
                    ontology.getIndividualsInSignature()) {


                checkedIndividuals++;



            

                if(checkedIndividuals % 500 == 0) {

                    System.out.println(
                            "Checked individuals: "
                            +
                            checkedIndividuals
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



                languageIndividuals++;



            
                EntitySearcher
                .getDataPropertyValues(
                        individual,
                        strVal,
                        ontology
                )
                .forEach(
                        (OWLLiteral literal) -> {


                    String value =
                            literal
                            .getLiteral()
                            .trim();


            
                    if(value.isEmpty()) {

                        return;

                    }


                    

                    romanUrduWords.add(
                            value
                    );


                });


            }



            /*
             * ---------------------------------------------------------
             * SORT WORDS
             * ---------------------------------------------------------
             */

            List<String> sortedWords =
                    new ArrayList<String>(
                            romanUrduWords
                    );


            Collections.sort(
                    sortedWords,
                    String.CASE_INSENSITIVE_ORDER
            );


            strValCount =
                    sortedWords.size();



            /*
             * ---------------------------------------------------------
             * PRINT EXTRACTION STATISTICS
             * ---------------------------------------------------------
             */

            System.out.println();


            System.out.println(
                    "========================================"
            );


            System.out.println(
                    "EXTRACTION COMPLETE"
            );


            System.out.println(
                    "========================================"
            );


            System.out.println(
                    "Individuals checked: "
                    +
                    checkedIndividuals
            );


            System.out.println(
                    "Language individuals found: "
                    +
                    languageIndividuals
            );


            System.out.println(
                    "Unique non-empty strVal values: "
                    +
                    strValCount
            );


            System.out.println(
                    "========================================"
            );



            /*
             * ---------------------------------------------------------
             * WRITE EXCEL FILE
             * ---------------------------------------------------------
             */

            writeExcel(
                    outputPath,
                    sortedWords
            );



            System.out.println();


            System.out.println(
                    "Excel file created successfully:"
            );


            System.out.println(
                    outputPath
            );


            System.out.println();


            System.out.println(
                    "Total Roman Urdu words written: "
                    +
                    sortedWords.size()
            );


        }
        catch(Exception e) {


            e.printStackTrace();


        }


    }



    /*
     * =============================================================
     * WRITE WORDS TO EXCEL
     * =============================================================
     *
     * Column A:
     *
     * Roman Urdu Word
     * ----------------
     * acha
     * admi
     * ...
     *
     */
    private static void writeExcel(
            String outputPath,
            List<String> words)
            throws Exception {


        XSSFWorkbook workbook =
                new XSSFWorkbook();


        Sheet sheet =
                workbook.createSheet(
                        "Roman Urdu Words"
                );



        /*
         * ---------------------------------------------------------
         * HEADER
         * ---------------------------------------------------------
         */

        Row headerRow =
                sheet.createRow(0);


        Cell headerCell =
                headerRow.createCell(0);


        headerCell.setCellValue(
                "Roman Urdu Word"
        );



        /*
         * ---------------------------------------------------------
         * WRITE WORDS
         * ---------------------------------------------------------
         */

        int rowNumber = 1;


        for(String word : words) {


            Row row =
                    sheet.createRow(
                            rowNumber
                    );


            Cell cell =
                    row.createCell(0);


            cell.setCellValue(
                    word
            );


            rowNumber++;


        }



        /*
         * ---------------------------------------------------------
         * SET COLUMN WIDTH
         * ---------------------------------------------------------
         */

        sheet.setColumnWidth(
                0,
                35 * 256
        );



        /*
         * ---------------------------------------------------------
         * SAVE WORKBOOK
         * ---------------------------------------------------------
         */

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




        result.add(
                parent
        );





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
