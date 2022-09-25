package at.ac.meduniwien.vcfvisualize.rest.dto.genepanelsearch;

public class IndexedGeneAliasDTO {

    public IndexedGeneAliasDTO(String alias, String gene) {
        this.alias = alias;
        this.gene = gene;
    }

    public String alias;
    public String gene;

}
