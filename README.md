__TOC__

# Introduction

This tool ...
TODO add video here

## Architecture

TODO add diagram

## System Requirements

It is highly recommended to install the system on linux. If you wish to annotate the files before importing them (this is necessary if your
VCF files are not annotated with vep already) you need roughly 500GB (TODO how much is actually needed?) of free disk space.
The following programs are required for starting the system: (TODO spellcheck)

- <a href="https://git-scm.com/book/en/v2/Getting-Started-Installing-Git" target="_blank">git</a>
- <a href="https://docs.docker.com/engine/install/" target="_blank">Docker</a>
- <a href="https://docs.docker.com/compose/install/" target="_blank">Docker-Compose</a>
- Free ports on `8080`, `3000`, `5432`, `3306`

# Tool (TODO add name)

## Necessary Configurations

Before deploying the system make sure to adapt the [docker-compose.yaml file](tool/docker-compose.yaml) to your needs.
Given that the PostgreSQL database can grow to a considerable size, consuming large ammounts of disk space it is worth
choosing an appropriate location for storing its files. Adapt line 15 in [docker-compose.yaml file](tool/docker-compose.yaml) <code>- <b>/data/vv/postgres</b>:/var/lib/postgresql/data</code> to change
the location. 
Secondly if you are not running the system on localhost and rather want to provide access to other user, replace the URLs
in the [config.js file](tool/web/src/config.js). `appBaseUrl` refers to the URL the application can be accessed through in the web browser. `apiBaseUrl` defines the URL core is accessed through. Please consider that if you are not running the project on your local machine but a server you need to adapt these values and potentially create some form of URL forwarding.

## Deploying the System

Starting the web application and the server backend alongside the database can be done with the following command
<pre><code>docker-compose up --build</code></pre>

# Annotation and Import

Before importing a VCF file make sure that the PostgreSQL database is running on port `5432`. 

## Build the container

After cloning the repository and navigating to the folder containing the Dockerfile, you can use the following command to build the container. Depending on your local setup you might need to prepend `sudo`.

<pre><code>docker build -t vcfimport .</code></pre>

## Import the file

Once the container has been built successfully, you can start the import by executing the following command:
- Replace <b>/absolute/path/to/your/file</b> with the absolute path to the folder your file is located in.
- Replace <b>file.vcf</b> with the name of your VCF file you want to import. 

<pre><code>docker run --rm -it --net="host" -v <b>/absolute/path/to/your/vcffolder</b>:/files vcfimport:latest /files/<b>file.vcf</b> jdbc:postgresql://localhost:5432/sample postgres password</code></pre>

<i>Note that if you decided to use a different PostgreSQL host, username or password that change needs to be reflected in the command:
```docker run --rm -it --net="host" -v <folderpath>:/files vcfimport:latest /files/<filename> <postgreshost>/<database> <postgresusername> <postgrespassword>```</i>

The importer should now import your file and will terminate upon completion. 
This may take some time depending on the size of your VCF file. The file is traversed a total of two times.
If you import a file twice or a file with an equal name, the first table is overwritten.
1. A preprocessing step is performed determining the data type of <code>INFO</code> and <code>CSQ</code> fields.
2. The <code>CREATE TABLE</code> command is executed.
3. The variants are inserted into the table.
4. The <code>CREATE INDEX</code> commands are executed increasing the performance of further requests.

### Configurating index creation

Database indices are created after every variant has been inserted into the table. In order to optimize performance and storage consumption, the columns which are to be indexed can be individually defined. The <code>application.properties</code> file in the root directory contains a list of these column names. In case your VCF files contain an additional info or consequence field that needs to be indexed (i. e. searched for or filtered), note that in the `application.properties`file  VCF `INFO` fields are to be denominated with a leading `info_`and VCF Consequence fields are to be denominated with a leading `info_csq_`. 

  
  
  
  TODO add fav and notes to variants
