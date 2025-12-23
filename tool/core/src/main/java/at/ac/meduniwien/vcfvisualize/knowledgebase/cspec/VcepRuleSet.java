package at.ac.meduniwien.vcfvisualize.knowledgebase.cspec;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VcepRuleSet {

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
     * The primary MONDO disease ID (e.g., "MONDO:0005021")
     * A gene-ruleset combination may cover multiple related diseases
     */
    private List<String> mondoIds;

    /**
     * The RuleSet ID extracted from the API (e.g., "135641053")
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
     * The raw JSON response from /api/RuleSet/id/{id}
     * Contains the detailed classification rules
     */
    private JsonNode ruleSetDetails;

    public VcepRuleSet(String geneSymbol, String vcepName, String vcepUrl, 
                       List<String> mondoIds, String ruleSetId, String sviUrl, 
                       String version) {
        this.geneSymbol = geneSymbol;
        this.vcepName = vcepName;
        this.vcepUrl = vcepUrl;
        this.mondoIds = mondoIds;
        this.ruleSetId = ruleSetId;
        this.sviUrl = sviUrl;
        this.version = version;
    }

    @Override
    public String toString() {
        return "VcepRuleSet{" +
                "geneSymbol='" + geneSymbol + '\'' +
                ", vcepName='" + vcepName + '\'' +
                ", mondoIds=" + mondoIds +
                ", ruleSetId='" + ruleSetId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}

