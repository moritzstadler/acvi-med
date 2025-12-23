package at.ac.meduniwien.vcfvisualize.knowledgebase.cspec.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CspecRuleSetRefDTO {

    @JsonProperty("@id")
    public String id;

    @JsonProperty("@type")
    public String type;

    public CspecGeneDTO[] genes;
}

