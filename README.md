<p align="center">
<img width="300px" src="/tool/web/src/logo.png"/
</p>
<p align="center">
<b>ACVI-MED: Accelerated Clinical Variant Interpretation</b>
</p>
<hr/>


**TODO: Add DOI, citation info and paper**

**TODO: add commits explaining installation of docker**

# Introduction

This tool is designed to allow clinicians and researchers to annotate and analyze VCF files.
**TODO add video here once everything is finished**

**TODO add installation video**

## Architecture

![system architecture](/misc/diagrams/architecture.png)

## System Requirements

It is highly recommended to install the system on Linux. If you wish to annotate the files before importing them (this is necessary if your
VCF files are not annotated with vep already) roughly 709GB of free disk space are needed for genomic databases. In order to simply test the application this is not necessary however.
The following programs are required for starting the system:

- <a href="https://git-scm.com/book/en/v2/Getting-Started-Installing-Git" target="_blank">git</a>
- <a href="https://docs.docker.com/engine/install/" target="_blank">Docker</a>
- <a href="https://docs.docker.com/compose/install/" target="_blank">Docker-Compose</a>
- Free ports on `80` and `10938`

>For checking out what process runs on the respective ports you may use ``sudo lsof -i :80`` 

# The Tool - ACVI-MED

## Necessary Configurations

You can run the installation out of the box without needing to make any changes to the [docker-compose.yaml file](tool/docker-compose.yaml). (If you plan on importing a large number of files, it might make sense to to adapt the [docker-compose.yaml file](tool/docker-compose.yaml) to your needs.)
Given that the PostgreSQL database can grow to a considerable size, consuming large ammounts of disk space it is worth
choosing an appropriate location for storing its files. You might want to adapt the accordingly commented lines in [docker-compose.yaml file](tool/docker-compose.yaml).

## Deploying the System

First navigate to the `/vcfimport/tool` folder. Starting the web application and the server backend alongside the database can be done with the following command. Depending on your local setup it might not be necessary to prepend `sudo`.

<pre><code>sudo docker compose up -d --build</code></pre>

You should be able to view the web application in your browser by typing `http://localhost` or the domain of the server you set the project up on in your browser's address bar. The initial credentials are `changeme` `changeme`. After logging in you should first create a new admin user for yourself by entering your email, checking 'Admin' and clicking 'Create'. After that open the activation link and select a secure password for yourself. Make sure to only delete the `changeme` account after you have successfully created another admin user.

> If you were able to run the docker compose command successfully but cannot reach the service, check if your browser automatically changed http to https. You can only connect via https after adding a certificate! 

# Annotation and Import

Before importing a VCF file make sure that the application is up and running. Only annotated files can be imported.

## Build the container

After cloning the repository and navigating to the folder containing the Dockerfile ``/importer/Dockerfile``, you can use the following command to build the container. Depending on your local setup you might need to prepend `sudo`.

<pre><code>sudo docker build -t acviimporter .</code></pre>

## Annotating a VCF file

Annotating a VCF file requires downloading a number of libraries and databases containing genomic information. Annotation needs to be done before importing a VCF file.

<b>[Instructions on how to annotate your VCF file.](ANNOTATION.md)</b>

## Import the file

Once the container has been built successfully, you can start the import by executing the following command:
- Replace <b>/absolute/path/to/your/file</b> with the absolute path to the folder your file is located in.
- Replace <b>file.vcf</b> with the name of your VCF file you want to import. 

<pre><code>sudo docker run --rm -it --net="host" -v <b>/absolute/path/to/your/vcffolder</b>:/files acviimporter:latest ./import /files/<b>file.vcf</b> jdbc:postgresql://localhost:10938/sample postgres password</code></pre>

<i>Note that if you decided to use a different PostgreSQL host, username or password that change needs to be reflected in the command:
```sudo docker run --rm -it --net="host" -v <folderpath>:/files vcfimport:latest ./import /files/<filename> <postgreshost>/<database> <postgresusername> <postgrespassword>```</i>

> We recommend starting your first import with one of the **annotated** sample files provided in this repository like ```demo_data/HG001_GIAB_annotated_downsampled.vcf```
  
After successfully importing the file you can change to your browser, click 'Synchronize Datasets' in the Admin page to immediately view the newly imported sample. Synchronization of datasets is also performed automatically each night. You can now create a study for your sample and allow different users to access it.

The importer should now import your file and will terminate upon completion. 
This may take some time depending on the size of your VCF file. The file is traversed a total of two times.
If you import a file twice or a file with an equal name, the first table is overwritten.
1. A preprocessing step is performed determining the data type of <code>INFO</code> and <code>CSQ</code> fields.
2. The <code>CREATE TABLE</code> command is executed.
3. The variants are inserted into the table.
4. The <code>CREATE INDEX</code> commands are executed increasing the performance of further requests.
  
#### Configurating index creation

Database indices are created after every variant has been inserted into the table. In order to optimize performance and storage consumption, the columns which are to be indexed can be individually defined. The <code>application.properties</code> file in the root directory contains a list of these column names. In case your VCF files contain an additional info or consequence field that needs to be indexed (i. e. searched for or filtered), note that in the `application.properties`file  VCF `INFO` fields are to be denominated with a leading `info_`and VCF Consequence fields are to be denominated with a leading `info_csq_`.   

### Add a .cram or .bam file

Lastly you can add a `.cram` or `.bam` file to allow researchers and clinicians to view the individual reads via [the Integrative Genomics Viewer IGV](https://igv.org/).
Add these files to the `~/data/files/` directory (or your directory in case you changed the [docker-compose.yaml file](tool/docker-compose.yaml)). You can now link these files to your patient samples by entering the location `/bam/somebam.bam` alongside the sample in the GUI of your admin panel. Please note that the .bam and .bai file should have the same file name and be stored in the same location (e. g. `sample.bam` and `sample.bai`).

  
**[Learn how customize the application like changing filterable items, names and descriptions.](CUSTOMIZE.md)**
