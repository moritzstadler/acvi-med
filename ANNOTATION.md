# Annotating a VCF file
This file contains the instructions for annotating a VCF file which is necessary before importing it.

**Required installations**
- docker
- wget

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

Start by creating a directory for the data needed by the plugins in your ``vep_data`` directory.
<pre><code>mkdir $HOME/vep_data/libs/</code></pre>

Navigate to the newly created ``libs`` directory.
<pre><code>cd $HOME/vep_data/libs/</code></pre>

### CADD

Create a directory for CADD
<pre><code>mkdir $HOME/vep_data/libs/cadd</code></pre>

**TODO - figshare**

### ClinVar

Create a directory for ClinVar and navigate to it.
<pre><code>mkdir $HOME/vep_data/libs/clinvar
cd $HOME/vep_data/libs/clinvar</code></pre>

Copy the files via wget
<pre><code>wget https://ftp.ncbi.nlm.nih.gov/pub/clinvar/vcf_GRCh38/clinvar.vcf.gz
wget https://ftp.ncbi.nlm.nih.gov/pub/clinvar/vcf_GRCh38/clinvar.vcf.gz.tbi</code></pre>

### dbNSFP

Create a directory for dbNSFP and navigate to it.
<pre><code>mkdir $HOME/vep_data/libs/dbnsfp
cd $HOME/vep_data/libs/dbnsfp</code></pre>

Run the following commands one by one to fetch and format the dbNSFP data. Note that some commands may take a long time to run. In that case you might want to use the ``nohup``command.
<pre><code>
wget https://dbnsfp.s3.amazonaws.com/dbNSFP4.3a.zip
unzip dbNSFP4.3a.zip
zcat dbNSFP4.3a_variant.chr1.gz | head -n1 > h
mkdir tmp
zgrep -h -v ^#chr dbNSFP4.3a_variant.chr* | sort -T ./tmp -k1,1 -k2,2n - | cat h - | bgzip -c > dbNSFP4.3a_grch38.gz
tabix -s 1 -b 2 -e 2 dbNSFP4.3a_grch38.gz
</code></pre>

### gnomAD

Create a directory for gnomAD and navigate to it.
<pre><code>mkdir $HOME/vep_data/libs/gnomad
cd $HOME/vep_data/libs/gnomad</code></pre>

<pre><code>
wget https://storage.googleapis.com/gcp-public-data--gnomad/release/3.0.1/coverage/genomes/gnomad.genomes.r3.0.1.coverage.summary.tsv.bgz  --no-check-certificate
gunzip -c gnomad.genomes.r3.0.1.coverage.summary.tsv.bgz | sed '1s/.*/#&/' > gnomad.genomesv3.tabbed.tsv
sed "1s/locus/chr\tpos/; s/:/\t/g" gnomad.genomesv3.tabbed.tsv > gnomad.ch.genomesv3.tabbed.tsv
bgzip gnomad.ch.genomesv3.tabbed.tsv 
tabix -s 1 -b 2 -e 2 gnomad.ch.genomesv3.tabbed.tsv.gz
</code></pre>

### Mastermind

Create a directory for Mastermind and navigate to it.
<pre><code>mkdir $HOME/vep_data/libs/mastermind</code>
cd $HOME/vep_data/libs/mastermind</pre>

**TODO - figshare**

### Phenotypes

Create a directory for Phenotypes and navigate to it.
<pre><code>mkdir $HOME/vep_data/libs/phenotypes
cd $HOME/vep_data/libs/phenotypes</code></pre>

**TODO - figshare**

## Annotate your file

Finally everything is set up to start annotating your VCF file.

**Congratulations! You can now continue by [importing the file](README.md#import-the-file)**
