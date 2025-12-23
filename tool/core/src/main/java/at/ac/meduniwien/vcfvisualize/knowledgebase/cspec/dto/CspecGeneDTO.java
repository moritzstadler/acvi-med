package at.ac.meduniwien.vcfvisualize.knowledgebase.cspec.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CspecGeneDTO {

    @JsonProperty("@id")
    public String id;

    @JsonProperty("@type")
    public String type;

    public String label;

    public CspecDiseaseDTO[] diseases;
}

