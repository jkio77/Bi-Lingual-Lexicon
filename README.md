# Ontology-Driven Single-Word Roman Urdu Cross-Lingual Query Expansion For Multilingual Information Retrieval
***Usage***<br> 
### Repository Contents and Reproducibility

This repository provides the source code, ontology, experimental artifacts, and supporting documentation for the proposed Roman Urdu information retrieval framework organisted in following folders:-
<br>
**1. BORUE** <br>
This folder has the implementation of BORUE (named as BiOnt.owl) and input files for various java / python classes. This folder has to be copied at location D:\BORUE\ 
for seamless code exection.  <br>
**2. Common Code** <br>
This folder has following main implemenation classes that can run independently. There are short videos showing the experimental run on author system.
**a. RuMetA.java** (Roman Urdu phonetic encoding). <br>
**b. RASOM.java** (Rule-based Approximate String-to-Ontology Matching). <br>
**c. BRUSE.java** (ontology-driven multilingual semantic query expansion). <br> 
<br>
**3. Introduction Section - Weblinks Annotation** <br>
This section has document file and excel sheet for our manual weblinks annotation in the paper introduction section.
**4. Ontology** <br>
BORUE file is placed in this folder (BiOnt.owl)
<br>

The repository also contains three evaluation directories: **RASOM Evaluation**, **BORUE Evaluation**, and **BRUSE Evaluation**. Each directory includes a detailed reproducibility document describing the experimental methodology, execution instructions, input/output files, and associated artifacts; a **Code** folder containing the relevant implementation scripts; and a **Results** folder containing experimental outputs in the form of Excel workbooks, tables, or figures, wherever applicable. These resources are provided to facilitate independent verification, replication, and further investigation of the proposed framework.<br>
**Step 1 - Environment setting** <br>
1. Read Tech stack settings document inside common code folder. <br>
2. Create a folder D:\BORUE and place all *.java , *.xlsx files in it along with BiOnt.owl file. <br>
3. Watch zipped videos in Common code for main classes RuMeta.java, RASOM.java and BRUSE.java. <br>
**Step 2 - RASOM Evaluation** <br>
1. Read 'Detailed eval docu RASOM.docx' inside RASOM - Evaluation. <br>
2. Run files as per detailed documents for reproducibility/ verification. <br>
**Step 3 - BORUE Evaluation** <br>
1. Read 'Detailed eval docu BORUE.docx' inside BORUE - Evaluation. <br>
2. Run files as per detailed documents for reproducibility/ verification. <br>
**Step 4 - BRUSE Evaluation** <br>
1. Read 'Detailed eval docu BRUSE.docx' inside BRUSE - Evaluation. <br>
2. Run files as per detailed documents for reproducibility/ verification. <br>


**Importan Note** <br>
PLEASE READ COMMON CODE > TECHNOLOGY STACK SETTINGS.DOCX BEFORE CONDUCTING EXPERIMENTS