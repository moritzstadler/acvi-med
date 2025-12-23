package at.ac.meduniwien.vcfvisualize.knowledgebase.cspec.dto;

import at.ac.meduniwien.vcfvisualize.knowledgebase.ApiResponseBody;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CspecSvisResponseDTO implements ApiResponseBody {

    @JsonProperty("@context")
    public String context;

    public CspecSviDTO[] data;
}

