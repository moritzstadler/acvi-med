package at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp;

import lombok.Getter;
import lombok.Setter;

public class PanelIdGene {

    public PanelIdGene(int id, Gene gene) {
        this.id = id;
        this.gene = gene;
    }

    @Getter
    @Setter
    private int id;

    @Getter
    @Setter
    private Gene gene;


}
