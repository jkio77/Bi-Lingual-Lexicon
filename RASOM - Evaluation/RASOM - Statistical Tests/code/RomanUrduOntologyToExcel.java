package org.borue;


/*
 * This class writes all roman urdu words into excel
 * so that you create PUs for them
 */
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


/*
 * RomanUrduOntologyToExcel
 *
 * PURPOSE:
 *
 * 1. Load BiOnt.owl
 * 2. Find Language_In
 * 3. Find Language_In and all its subclasses recursively
 * 4. Find individuals belonging to those classes
 * 5. Read strVal from those individuals
 * 6. Remove duplicate and empty values
 * 7. Sort the Roman Urdu words alphabetically
 * 8. Write them to a new Excel file
 *
 * IMPORTANT:
 *
 * This program DOES NOT modify the ontology.
 * It only reads the ontology and creates an Excel file.
 *
 */
public class RomanUrduOntologyToExcel {


    public static void main(String[] args) {


        try {


            /*
             * ---------------------------------------------------------
             * PATHS
             * ---------------------------------------------------------
             */

            String ontologyPath = "D:/BORUE/BiOnt.owl";


            String outputPath ="D:/BORUE/RomanUrduWords.xlsx";



            /*
             * ---------------------------------------------------------
             * LOAD ONTOLOGY
             * ---------------------------------------------------------
             */

            OWLOntologyManager manager =
                    OWLManager.createOWLOntologyManager();


            OWLOntology ontology =
                    manager.loadOntologyFromOntologyDocument(
                            new File(ontologyPath)
                    );


            System.out.println(
                    "Ontology loaded successfully"
            );



            /*
             * ---------------------------------------------------------
             * DATA FACTORY
             * ---------------------------------------------------------
             */

            OWLDataFactory factory =
                    manager.getOWLDataFactory();



            /*
             * ---------------------------------------------------------
             * BASE IRI
             * ---------------------------------------------------------
             */

            String baseIRI =
                    "http://localhost:3030/BiOnt.owl#";



            /*
             * ---------------------------------------------------------
             * strVal PROPERTY
             * ---------------------------------------------------------
             */

            OWLDataProperty strVal =
                    factory.getOWLDataProperty(
                            IRI.create(
                                    baseIRI + "strVal"
                            )
                    );



            /*
             * ---------------------------------------------------------
             * Language_In CLASS
             * ---------------------------------------------------------
             */

            OWLClass languageIn =
                    factory.getOWLClass(
                            IRI.create(
                                    baseIRI + "Language_In"
                            )
                    );



            /*
             * ---------------------------------------------------------
             * COLLECT Language_In + ALL SUBCLASSES
             * ---------------------------------------------------------
             */

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



            /*
             * Optional:
             * Print classes so that we can verify what was found.
             */

            for(OWLClass cls : languageClasses) {

                System.out.println(
                        "Language class: "
                        +
                        cls.getIRI()
                );

            }



            /*
             * ---------------------------------------------------------
             * EXTRACT strVal VALUES
             * ---------------------------------------------------------
             *
             * HashSet is intentional.
             *
             * If the same strVal occurs more than once in the ontology,
             * it will appear only once in Excel.
             *
             */

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


            System.out.println(
                    "Reading Language_In individuals..."
            );



            /*
             * ---------------------------------------------------------
             * CHECK EVERY NAMED INDIVIDUAL
             * ---------------------------------------------------------
             */

            for(OWLNamedIndividual individual :
                    ontology.getIndividualsInSignature()) {


                checkedIndividuals++;



                /*
                 * Print progress every 500 individuals.
                 */

                if(checkedIndividuals % 500 == 0) {

                    System.out.println(
                            "Checked individuals: "
                            +
                            checkedIndividuals
                    );

                }



                /*
                 * -----------------------------------------------------
                 * CHECK WHETHER INDIVIDUAL BELONGS TO Language_In
                 * OR ANY OF ITS SUBCLASSES
                 * -----------------------------------------------------
                 */

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



                /*
                 * Ignore individuals outside Language_In hierarchy.
                 */

                if(!isLanguageIndividual) {

                    continue;

                }



                languageIndividuals++;



                /*
                 * -----------------------------------------------------
                 * READ ALL strVal VALUES
                 * -----------------------------------------------------
                 *
                 * We deliberately read ALL strVal assertions.
                 *
                 * If an individual has more than one strVal,
                 * none of them will be lost.
                 *
                 */

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


                    /*
                     * Ignore empty strings.
                     */

                    if(value.isEmpty()) {

                        return;

                    }


                    /*
                     * Add to Set.
                     *
                     * Duplicate values are automatically ignored.
                     */

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



    /*
     * =============================================================
     * RECURSIVELY COLLECT Language_In AND ALL SUBCLASSES
     * =============================================================
     *
     * This is the same approach already working in your
     * existing ontology classes.
     *
     */
    private static void collectSubClasses(
            OWLClass parent,
            OWLOntology ontology,
            Set<OWLClass> result) {


        /*
         * Avoid processing the same class twice.
         */

        if(result.contains(parent)) {

            return;

        }



        /*
         * Include the parent itself.
         *
         * Therefore Language_In itself is included as well.
         */

        result.add(
                parent
        );



        /*
         * Find direct subclasses of this class.
         */

        for(OWLClassExpression child :
                ontology
                .getSubClassAxiomsForSuperClass(parent)
                .stream()
                .map(ax -> ax.getSubClass())
                .collect(Collectors.toList())) {


            /*
             * Only process named OWL classes.
             */

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
