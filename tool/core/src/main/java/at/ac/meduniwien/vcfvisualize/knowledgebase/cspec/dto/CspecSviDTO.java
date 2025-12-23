package at.ac.meduniwien.vcfvisualize.knowledgebase.cspec.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CspecSviDTO {

    @JsonProperty("@id")
    public String id;

    @JsonProperty("@type")
    public String type;

    public CspecAffiliationDTO affiliation;

    public CspecRuleSetRefDTO[] ruleSets;

    public String status;

    public String url;

    public String version;
}

