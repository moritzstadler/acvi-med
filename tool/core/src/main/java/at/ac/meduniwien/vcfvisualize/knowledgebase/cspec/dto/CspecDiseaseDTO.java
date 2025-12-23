package at.ac.meduniwien.vcfvisualize.knowledgebase.cspec.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CspecDiseaseDTO {

    @JsonProperty("@id")
    public String id;

    @JsonProperty("@type")
    public String type;

    public String label;

    public CspecModeOfInheritanceDTO[] modeOfInheritance;
}

