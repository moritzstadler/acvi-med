package at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp;

import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto.GeneDTO;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto.PanelDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

public class Panel {

    public Panel(PanelDTO panel) {
        this.panel = panel;
        this.genes = new ArrayList<>();
    }

    @Getter
    @Setter
    PanelDTO panel;

    @Getter
    @Setter
    ArrayList<Gene> genes;

}
