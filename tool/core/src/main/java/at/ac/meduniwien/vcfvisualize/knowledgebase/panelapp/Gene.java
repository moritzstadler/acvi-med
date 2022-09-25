package at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp;

import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto.GeneDTO;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

public class Gene {

    public Gene(GeneDTO gene) {
        this.gene = gene;
    }

    @Getter
    private GeneDTO gene;

}
