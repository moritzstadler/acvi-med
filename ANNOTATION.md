# Annotating a VCF file
This file contains the instructions for annotating a VCF file which is necessary before importing it.

**Required installations**
- docker
- wget

## Pulling the Ensembl VEP Docker container
After cloning the repository and navigating to the importer folder containing the Dockerfile (``cd importer``), you can use the following command to build the container if you haven't done so already. Depending on your local setup you might need to prepend `sudo`.

<pre><code>sudo docker pull ensemblorg/ensembl-vep</code></pre>

## Installing plugins

Depending on your local setup you might need to prepend the ``sudo`` command to following actions.

Start by creating a directory for the data needed to annotate VCF files. You can create the new folder in your home directory or anywhere else.
It is important that enough disc spaces is available for these operations.
<pre><code>mkdir $HOME/vep_data</code></pre>

Depending on your local setup you might need to change the access rights to the directory you created.
<pre><code>chmod -R a+rwx $HOME/vep_data</code></pre>

Then install all vep plugins in your container by running the following command.
<pre><code>docker run -t -i -v $HOME/vep_data:/data ensemblorg/ensembl-vep INSTALL.pl -a cfp -s homo_sapiens -y GRCh38 -g all</code></pre>

*Check out the official documentation of vep for further information https://www.ensembl.org/info/docs/tools/vep/script/vep_download.html#docker*

## Adding libraries

Some plugins need additional data to run. You need to perform all steps presented below or otherwise the annotion of your VCF file cannot be completed.

Start by creating a directory for the data needed by the plugins in your ``vep_data`` directory.
<pre><code>mkdir $HOME/vep_data/libs/</code></pre>

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
<pre><code>wget https://dbnsfp.s3.amazonaws.com/dbNSFP4.3a.zip
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

<pre><code>wget https://storage.googleapis.com/gcp-public-data--gnomad/release/3.0.1/coverage/genomes/gnomad.genomes.r3.0.1.coverage.summary.tsv.bgz  --no-check-certificate
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
Move the file you want to annotate to your ``$HOME/vep_data`` folder and run the following command. Replace ``yourvcffile.vcf`` with the actual name your the VCF file you want to annotate,

<pre><code>sudo docker run --rm -it -v $HOME/vep_data:/data ensemblorg/ensembl-vep ./vep -i /data/yourvcffile.vcf --dir_cache /data --everything --cache --offline --format vcf --warning_file /data/warnings --verbose \
--plugin CADD,"/data/libs/cadd/whole_genome_SNVs.tsv.gz","/data/libs/cadd/gnomad.genomes.r3.0.indel.tsv.gz" \
--plugin Phenotypes,file="/data/libs/phenotypes/Phenotypes.pm_homo_sapiens_102_GRCh38.gvf.gz",include_types=Gene \
--plugin Mastermind,"/data/libs/mastermind/mastermind_cited_variants_reference-2021.08.03-grch38.vcf.gz" \
--plugin dbNSFP,/data/libs/dbnsfp/dbNSFP4.3a_grch38.gz,aaref,aaalt,codonpos,SIFT4G_score,Polyphen2_HDIV_score,Polyphen2_HDIV_pred,LRT_score,LRT_pred,MutationTaster_score,MutationTaster_pred,MutationTaster_AAE,FATHMM_score,FATHMM_pred,MetaSVM_score,MetaSVM_pred,MetaLR_score,MetaLR_pred,Reliability_index,M-CAP_score,M-CAP_pred,PrimateAI_score,PrimateAI_pred,Aloft_Fraction_transcripts_affected,DANN_score,VEST4_score,REVEL_score,MVP_score,Aloft_prob_Recessive,Aloft_prob_Dominant,Aloft_pred,GERP++_RS,clinvar_OMIM_id,Interpro_domain \
--plugin gnomADc,/data/libs/gnomad/gnomad.ch.genomesv3.tabbed.tsv.gz \
--custom "/data/libs/clinvar/clinvar.vcf.gz",ClinVar,vcf,exact,0,ALLELEID,CLNSIG,CLNREVSTAT,CLNDN,CLNDISDB,CLNDNINCL,CLNDISDBINCL,CLNHGVS,CLNSIGCONF,CLNSIGINCL,CLNVC,CLNVCSO,CLNVI,DBVARID,GENEINFO,MC,ORIGIN,RS,SSR \
 --o /data/output.vcf \
 --force_overwrite \
--vcf \
-hgvsg --shift_hgvs 1 --max_af --terms SO --regulatory --check_existing \
--offline \
--fork 4
</code></pre>

**Congratulations! You can now continue by [importing the file](README.md#import-the-file)**
