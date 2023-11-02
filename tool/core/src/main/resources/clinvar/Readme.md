
# CLINVAR FILES INSTRUCTIONS
>Note: The directory needs to be chmod +777  
  
```
wget https://ftp.ncbi.nlm.nih.gov/pub/clinvar/vcf_GRCh38/clinvar.vcf.gz
``` 
  
```
gzip -d clinvar.vcf.gz
```  
  
```
sudo docker run --rm -it -v /data/vep:/opt/vep/.vep ensemblorg/ensembl-vep ./vep -i /opt/vep/.vep/clinvar.vcf --dir_cache /opt/vep/.vep --cache --offline --format vcf --warning_file /opt/vep/.vep/warnings --o /opt/vep/.vep/cv_full.vcf --force_overwrite --vcf -hgvsg --shift_hgvs 1 --hgvs --symbol --fork 4
```  
  
```
nohup bgzip -c cv_full.vcf > cv_full.vcf.gz &
```  

```
tabix -p vcf cv_full.vcf.gz
```  
  
## This is so we don't miss anything with the panels
```
/data/bcf/bcftools/bcftools plugin /data/bcf/bcftools/plugins/split-vep.so -f '%CHROM\t%POS\t%REF\t%ALT\t%INFO/CLNSIG\n' cv_full.vcf.gz > cv_clnsig.txt
```
  
## This is for getting chrom:pos:(ref:alt) by HPO-term  
```
/data/bcf/bcftools/bcftools plugin /data/bcf/bcftools/plugins/split-vep.so -f '%CHROM\t%POS\t%REF\t%ALT\t%INFO/CLNDISDB,\n' cv_full.vcf.gz > cv_clndisdb_f.vcf
```
  
## This is for finding mutations with similar amino acids but different bases to a known one  
```
/data/bcf/bcftools/bcftools plugin /data/bcf/bcftools/plugins/split-vep.so -f '%CHROM\t%POS\t%REF\t%ALT\t%INFO/CLNSIG\t%HGVSp\n' cv_full.vcf.gz > cv_hgvsp_f.vcf 
``` 
  
## This is for computing if LOF is a known mechanism of disease for a gene (L1)  
```
/data/bcf/bcftools/bcftools plugin /data/bcf/bcftools/plugins/split-vep.so -f '%INFO/CLNSIG\t%Consequence\t%INFO/GENEINFO\n' cv_full.vcf.gz > cv_clnsig_csq_gene.txt  
```

## Now format the files

```
grep -E 'Human_Phenotype_Ontology' cv_clndisdb_f.vcf > cv_clndisdb.txt
```  
```
grep -E 'Likely_pathogenic|Pathogenic' cv_hgvsp_f.vcf > cv_hgvsp_path.txt
```

## For the last formatting step you need a pyhton script

```
import sys

#DOCK8: PLP_Missense: 30; BENIGN_Missense: 5, PATHOGENIC_NULL_VARIANTS: 2, PATHOGENIC_NON_NULL_VARIANTS: 0

def isPLp(sig):
    return sig in ["Likely_pathogenic", "Pathogenic"]

def isMissenseVariant(csq):
    return csq == "missense_variant"

def isNullVariant(csq):
    return csq in ["transcript_ablation", "splice_acceptor_variant", "splice_donor_variant", "stop_gained", "frameshift_variant", "stop_lost", "start_lost", "transcript_amplification", "inframe_insertion", "inframe_deletion"]


PLpMissenseByGene = {} #pathogenic likely pathogenic
BMissenseByGene = {} #benign
PLpNullVariants = {}
PLpNonNullVariants = {}
total = {}
genes = set()

print("Gene\tTotalClinvarVariants\tPLpMissense\tBenignMissense\tPLpNullVariants\tPLpNonNullVariants")
for line in sys.stdin:
    s = line.rstrip("\n").split("\t")
    sig = s[0]
    csqMesh = s[1].split(",")
    hasNullVariant = False
    hasMissenseVariant = False
    for csq in csqMesh:
        hasNullVariant = hasNullVariant or isNullVariant(csq)
        hasMissenseVariant = hasMissenseVariant or isMissenseVariant(csq)
    gene = s[2]
    genes.add(gene)

    if hasMissenseVariant and isPLp(sig):
        if gene in PLpMissenseByGene:
            PLpMissenseByGene[gene] = PLpMissenseByGene[gene] + 1
        else:
            PLpMissenseByGene[gene] = 1

    if hasMissenseVariant and not isPLp(sig):
        if gene in BMissenseByGene:
            BMissenseByGene[gene] = BMissenseByGene[gene] + 1
        else:
            BMissenseByGene[gene] = 1

    if hasNullVariant and isPLp(sig):
        if gene in PLpNullVariants:
            PLpNullVariants[gene] = PLpNullVariants[gene] + 1
        else:
            PLpNullVariants[gene] = 1

    if not hasNullVariant and isPLp(sig):
        if gene in PLpNonNullVariants:
            PLpNonNullVariants[gene] = PLpNonNullVariants[gene] + 1
        else:
            PLpNonNullVariants[gene] = 1

    if gene in total:
        total[gene] = total[gene] + 1
    else:
        total[gene] = 1

for gene in genes:
    o0 = total[gene]

    o1 = 0
    if gene in PLpMissenseByGene:
        o1 = PLpMissenseByGene[gene]

    o2 = 0
    if gene in BMissenseByGene:
        o2 = BMissenseByGene[gene]

    o3 = 0
    if gene in PLpNullVariants:
        o3 = PLpNullVariants[gene]

    o4 = 0
    if gene in PLpNonNullVariants:
        o4 = PLpNonNullVariants[gene]

    print(gene + "\t" + str(o0) + "\t"  + str(o1) + "\t" + str(o2) + "\t" + str(o3) + "\t" + str(o4))
```

Then format the file by performing

```
python genegrouper.py < cv_clnsig_csq_gene.txt > cv_gene_info.txt
```
