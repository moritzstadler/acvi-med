# Vcfimport

Before importing a VCF file make sure that the Postgres database is running on port `5432`. 

## Build the container

After cloning the repository and navigating to the folder containing the Dockerfile, you can use the following command to build the container. Depending on your local setup you might need to prepend `sudo`.

<pre><code>docker build -t vcfimport .</code></pre>

## Import the file

Once the container has been built successfully, you can start the import by executing the following command:
- Replace <b>/absolute/path/to/your/file</b> with the absolute path to the folder your file is located in.
- Replace <b>file.vcf</b> with the name of your VCF file you want to import. 

<pre><code>docker run --rm -it --net="host" -v <b>/absolute/path/to/your/vcffolder</b>:/files vcfimport:latest /files/<b>file.vcf</b> jdbc:postgresql://localhost:5432/sample postgres password</code></pre>

<i>Note that if you decided to use a different Postgres host, username or password that change needs to be reflected in the command:
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
