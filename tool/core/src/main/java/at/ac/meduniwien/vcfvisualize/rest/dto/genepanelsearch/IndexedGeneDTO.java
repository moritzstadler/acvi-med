package at.ac.meduniwien.vcfvisualize.rest.dto.genepanelsearch;

import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.Gene;

public class IndexedGeneDTO {

    public IndexedGeneDTO(Gene gene, String[] panelNames) {
        this.name = gene.getGene().gene_data.gene_name;
        this.hgncId = gene.getGene().gene_data.hgnc_id;
        this.omim = gene.getGene().gene_data.omim_gene;
        this.aliasName = gene.getGene().gene_data.alias_name;
        this.symbol = gene.getGene().gene_data.hgnc_symbol;
        this.confidenceLevel = gene.getGene().confidence_level;
        this.penetrance = gene.getGene().penetrance;
        this.modeOfPathogenicity = gene.getGene().mode_of_pathogenicity;
        this.publications = gene.getGene().publications;
        this.evidence = gene.getGene().evidence;
        this.phenotypes = gene.getGene().phenotypes;
        this.modeOfInheritance = gene.getGene().mode_of_inheritance;
        this.panelNames = panelNames;
    }

    public IndexedGeneDTO(String symbol) {
        this.symbol = symbol;
    }

    public String name;
    public String hgncId; //link this
    public String[] omim; //link this
    public String[] aliasName;
    public String symbol; //hgnc symbol
    public String confidenceLevel;
    public String penetrance;
    public String modeOfPathogenicity;
    public String[] publications;
    public String[] evidence;
    public String[] phenotypes;
    public String modeOfInheritance;
    public String[] panelNames;

}
