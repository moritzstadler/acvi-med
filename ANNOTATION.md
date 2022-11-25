# Annotating a VCF file
This file contains the instructions for annotating a VCF file which is necessary before importing it.

## Building the Docker container
After cloning the repository and navigating to the folder containing the Dockerfile (``cd /importer``), you can use the following command to build the container if you haven't done so already. Depending on your local setup you might need to prepend `sudo`.

<pre><code>docker build -t vcfimport .</code></pre>

## Installing plugins

Depending on your local setup you might need to prepend the ``sudo`` command to following actions.

Start by creating a directory for the data needed to annotate VCF files. You can create the new folder in your home directory or anywhere else.
It is important that enough disc spaces is available for these operations.
<pre><code>mkdir $HOME/vep_data</code></pre>

Depending on your local setup you might need to change the access rights to the directory you created.
<pre><code>chmod -R a+rwx $HOME/vep_data</code></pre>

Then install all vep plugins in your container by running the following command.
<pre><code>docker run -t -i -v $HOME/vep_data:/opt/vep/.vep vcfimport INSTALL.pl -a cfp -s homo_sapiens -y GRCh38 -g all</code></pre>

*Check out the official documentation of vep for further information https://www.ensembl.org/info/docs/tools/vep/script/vep_download.html#docker*

## Adding libraries

Some plugins need additional data to run. You need to perform all steps presented below or otherwise the annotion of your VCF file cannot be completed.

### some lib

## Annotate your file

Finally everything is set up to start annotating your VCF file.

**Congratulations! You can now continue by [importing the file](README.md#import-the-file)**
