package at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto;

public class GeneDataDTO {

    public String[] alias;
    public String biotype;
    public String hgnc_id;
    public String gene_name;
    public String[] omim_gene;
    public String[] alias_name;
    public String gene_symbol;
    public String hgnc_symbol;
    public String hgnc_release;
    //public EnsemblDTO ensembl_genes;
    public String hgnc_date_symbol_changed;

}

class EnsemblDTO {
    public GRch37 GRch37;
    public GRch38 GRch38;
}

class GRch37 {
    public X _82;
}

class GRch38 {
    public X _90;
}

class X {
    public String location;
    public String ensembl_id;
}
