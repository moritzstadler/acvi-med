package at.ac.meduniwien.vcfvisualize.rest.dto.genepanelsearch;

import java.util.ArrayList;

public class GenePanelIndexDTO {

    //this should be as small as possible

    //TODO consider using a trie (or any other advanced data) structure if local search is to slow

    //TODO include short tandem repeats here

    public ArrayList<IndexedGeneAliasDTO> aliasIndex; //todo compress maybe
    public ArrayList<IndexedGeneDTO> geneIndex;
    public ArrayList<IndexedPanelDTO> panelIndex;

}
