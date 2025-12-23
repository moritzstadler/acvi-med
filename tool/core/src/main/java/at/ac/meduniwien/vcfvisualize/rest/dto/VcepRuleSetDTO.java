package at.ac.meduniwien.vcfvisualize.rest.dto;

import at.ac.meduniwien.vcfvisualize.knowledgebase.cspec.VcepRuleSet;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VcepRuleSetDTO {

    /**
     * The gene symbol (e.g., "MYH7", "RYR1")
     */
    private String geneSymbol;

    /**
     * The VCEP name (e.g., "Cardiomyopathy Variant Curation Expert Panel")
     */
    private String vcepName;

    /**
     * URL to the ClinGen affiliation page
     */
    private String vcepUrl;

    /**
     * The MONDO disease IDs associated with this gene-ruleset combination
     */
    private List<String> mondoIds;

    /**
     * The RuleSet ID
     */
    private String ruleSetId;

    /**
     * URL to the SVI documentation page
     */
    private String sviUrl;

    /**
     * Version of the SVI (e.g., "2.0.0")
     */
    private String version;

    /**
     * The full RuleSet JSON containing all classification criteria
     */
    private JsonNode ruleSetDetails;

    /**
     * Convert from domain object to DTO
     * @param vcepRuleSet the domain object
     * @return the DTO for frontend consumption
     */
    public static VcepRuleSetDTO fromVcepRuleSet(VcepRuleSet vcepRuleSet) {
        VcepRuleSetDTO dto = new VcepRuleSetDTO();
        dto.setGeneSymbol(vcepRuleSet.getGeneSymbol());
        dto.setVcepName(vcepRuleSet.getVcepName());
        dto.setVcepUrl(vcepRuleSet.getVcepUrl());
        dto.setMondoIds(vcepRuleSet.getMondoIds());
        dto.setRuleSetId(vcepRuleSet.getRuleSetId());
        dto.setSviUrl(vcepRuleSet.getSviUrl());
        dto.setVersion(vcepRuleSet.getVersion());
        dto.setRuleSetDetails(vcepRuleSet.getRuleSetDetails());
        return dto;
    }
}

