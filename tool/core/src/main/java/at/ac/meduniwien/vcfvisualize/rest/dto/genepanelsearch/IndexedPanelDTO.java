package at.ac.meduniwien.vcfvisualize.rest.dto.genepanelsearch;

import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.Panel;

public class IndexedPanelDTO {

    public IndexedPanelDTO(Panel panel) {
        this.name = panel.getPanel().name;
        this.diseaseGroup = panel.getPanel().disease_group;
        this.diseaseSubGroup = panel.getPanel().disease_sub_group;
        this.relevantDisorders = panel.getPanel().relevant_disorders;
        this.geneSymbols = panel.getGenes().stream().map(g -> g.getGene().gene_data.gene_symbol).toArray(String[]::new);
    }

    public String name;
    public String diseaseGroup;
    public String diseaseSubGroup;
    public String[] relevantDisorders;
    public String[] geneSymbols;

    //TODO strs here?

}
