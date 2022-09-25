package at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto;

import at.ac.meduniwien.vcfvisualize.knowledgebase.ApiResponseBody;

public class PanelsResponseDTO implements ApiResponseBody {

    public int count;
    public String next;
    public String previous;
    public PanelDTO[] results;

}
