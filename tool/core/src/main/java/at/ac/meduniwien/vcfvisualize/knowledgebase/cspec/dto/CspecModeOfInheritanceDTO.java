package at.ac.meduniwien.vcfvisualize.knowledgebase.cspec.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CspecModeOfInheritanceDTO {

    @JsonProperty("@id")
    public String id;

    @JsonProperty("@label")
    public String label;

    @JsonProperty("@type")
    public String type;
}

