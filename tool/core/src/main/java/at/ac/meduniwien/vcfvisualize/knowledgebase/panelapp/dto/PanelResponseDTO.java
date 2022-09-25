package at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto;

import at.ac.meduniwien.vcfvisualize.knowledgebase.ApiResponseBody;

public class PanelResponseDTO extends PanelDTO implements ApiResponseBody {

    public GeneDTO[] genes;
    public ShortTandemRepeatsDTO[] strs;
    public RegionDTO[] regions;

}
