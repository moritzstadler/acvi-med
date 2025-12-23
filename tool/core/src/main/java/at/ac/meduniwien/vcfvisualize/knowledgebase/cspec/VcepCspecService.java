package at.ac.meduniwien.vcfvisualize.knowledgebase.cspec;

import at.ac.meduniwien.vcfvisualize.knowledgebase.ApiRequest;
import at.ac.meduniwien.vcfvisualize.knowledgebase.RestClient;
import at.ac.meduniwien.vcfvisualize.knowledgebase.cspec.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VcepCspecService {

    private static final String SVIS_URL = "https://cspec.genome.network/cspec/api/svis";
    private static final String RULESET_URL_TEMPLATE = "https://cspec.genome.network/cspec/api/RuleSet/id/%s";
    
    private static final long REQUEST_DELAY_MS = 500;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    @Autowired
    private RestClient restClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Map from gene symbol to list of VcepRuleSets
     * A gene can have multiple RuleSets if it's curated by different VCEPs for different diseases
     */
    private Map<String, List<VcepRuleSet>> ruleSetsByGene = new HashMap<>();

    /**
     * Get all RuleSets for a given gene symbol
     * @param geneSymbol the gene symbol (e.g., "MYH7", "RYR1")
     * @return list of VcepRuleSets, or empty list if gene not found
     */
    public List<VcepRuleSet> getRuleSetsForGene(String geneSymbol) {
        if (ruleSetsByGene.isEmpty()) {
            // Fallback - should be loaded by cron, but load if empty
            loadDataFromAPI();
        }
        return ruleSetsByGene.getOrDefault(geneSymbol, Collections.emptyList());
    }

    /**
     * Get all loaded gene symbols
     * @return set of gene symbols
     */
    public Set<String> getLoadedGenes() {
        return ruleSetsByGene.keySet();
    }

    /**
     * Load all VCEP RuleSets from the CSpec API
     * Called by Cron on startup and scheduled intervals
     */
    public void loadDataFromAPI() {
        System.out.println("VcepCspecService: Starting to load data from CSpec API...");

        try {
            // Step 1: Fetch all SVIs
            CspecSvisResponseDTO svisResponse = fetchSvisFromAPI();
            if (svisResponse == null || svisResponse.data == null) {
                System.out.println("VcepCspecService: Failed to fetch SVIs or empty response");
                return;
            }

            // Step 2: Filter for Released status only
            List<CspecSviDTO> releasedSvis = Arrays.stream(svisResponse.data)
                    .filter(svi -> "Released".equals(svi.status))
                    .collect(Collectors.toList());

            System.out.println("VcepCspecService: Found " + releasedSvis.size() + " released SVIs");

            // Step 3: Collect all RuleSets with their metadata
            List<RuleSetMetadata> ruleSetMetadataList = collectRuleSetMetadata(releasedSvis);
            System.out.println("VcepCspecService: Found " + ruleSetMetadataList.size() + " gene-ruleset combinations to fetch");

            // Step 4: Fetch each RuleSet's detailed JSON with delay and retry
            Map<String, List<VcepRuleSet>> newRuleSetsByGene = new HashMap<>();
            int count = 0;
            int total = ruleSetMetadataList.size();

            for (RuleSetMetadata metadata : ruleSetMetadataList) {
                count++;
                System.out.println("VcepCspecService: loading ruleset (" + count + "/" + total + ") " 
                        + metadata.ruleSetId + " for gene " + metadata.geneSymbol);

                try {
                    JsonNode ruleSetDetails = fetchRuleSetWithRetry(metadata.ruleSetId);
                    
                    VcepRuleSet vcepRuleSet = new VcepRuleSet(
                            metadata.geneSymbol,
                            metadata.vcepName,
                            metadata.vcepUrl,
                            metadata.mondoIds,
                            metadata.ruleSetId,
                            metadata.sviUrl,
                            metadata.version
                    );
                    vcepRuleSet.setRuleSetDetails(ruleSetDetails);

                    newRuleSetsByGene
                            .computeIfAbsent(metadata.geneSymbol, k -> new ArrayList<>())
                            .add(vcepRuleSet);

                } catch (Exception e) {
                    System.out.println("VcepCspecService: Failed to fetch RuleSet " + metadata.ruleSetId 
                            + " for gene " + metadata.geneSymbol + ": " + e.getMessage());
                }

                // Delay between requests
                if (count < total) {
                    Thread.sleep(REQUEST_DELAY_MS);
                }
            }

            // Step 5: Replace the map atomically
            this.ruleSetsByGene = newRuleSetsByGene;

            // Summary
            int totalGenes = ruleSetsByGene.size();
            int totalRuleSets = ruleSetsByGene.values().stream().mapToInt(List::size).sum();
            System.out.println("VcepCspecService: Loaded " + totalRuleSets + " RuleSets for " + totalGenes + " genes");

        } catch (Exception e) {
            System.out.println("VcepCspecService: Error loading data from API: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Fetch the SVIs list from the API
     */
    private CspecSvisResponseDTO fetchSvisFromAPI() {
        ApiRequest apiRequest = new ApiRequest(SVIS_URL, null);
        return (CspecSvisResponseDTO) restClient.performRequestSkipCache(apiRequest, new CspecSvisResponseDTO()).getApiResponseBody();
    }

    /**
     * Collect metadata for all gene-ruleset combinations from the SVIs
     */
    private List<RuleSetMetadata> collectRuleSetMetadata(List<CspecSviDTO> svis) {
        List<RuleSetMetadata> result = new ArrayList<>();

        for (CspecSviDTO svi : svis) {
            String vcepName = svi.affiliation != null ? svi.affiliation.label : "Unknown VCEP";
            String vcepUrl = svi.affiliation != null ? svi.affiliation.url : null;
            String sviUrl = svi.url;
            String version = svi.version;

            if (svi.ruleSets == null) continue;

            for (CspecRuleSetRefDTO ruleSetRef : svi.ruleSets) {
                String ruleSetId = extractRuleSetId(ruleSetRef.id);
                
                if (ruleSetRef.genes == null) continue;

                for (CspecGeneDTO gene : ruleSetRef.genes) {
                    String geneSymbol = gene.label;
                    List<String> mondoIds = new ArrayList<>();
                    
                    if (gene.diseases != null) {
                        for (CspecDiseaseDTO disease : gene.diseases) {
                            if (disease.label != null) {
                                mondoIds.add(disease.label);
                            }
                        }
                    }
                    
                    if (mondoIds.isEmpty()) {
                        mondoIds.add("UNSPECIFIED");
                    }

                    result.add(new RuleSetMetadata(
                            geneSymbol, vcepName, vcepUrl, mondoIds, 
                            ruleSetId, sviUrl, version
                    ));
                }
            }
        }

        return result;
    }

    /**
     * Extract the RuleSet ID from the full URL
     * e.g., "https://cspec.genome.network/cspec/api/RuleSet/id/135640453" -> "135640453"
     */
    private String extractRuleSetId(String ruleSetUrl) {
        if (ruleSetUrl == null) return null;
        int lastSlash = ruleSetUrl.lastIndexOf('/');
        return lastSlash >= 0 ? ruleSetUrl.substring(lastSlash + 1) : ruleSetUrl;
    }

    /**
     * Fetch a RuleSet with exponential backoff retry
     */
    private JsonNode fetchRuleSetWithRetry(String ruleSetId) throws Exception {
        String url = String.format(RULESET_URL_TEMPLATE, ruleSetId);
        
        Exception lastException = null;
        long backoffMs = INITIAL_BACKOFF_MS;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                ApiRequest apiRequest = new ApiRequest(url, null);
                String jsonResponse = restClient.callApiRaw(apiRequest);
                return objectMapper.readTree(jsonResponse);
            } catch (Exception e) {
                lastException = e;
                System.out.println("VcepCspecService: Attempt " + attempt + " failed for RuleSet " 
                        + ruleSetId + ": " + e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    System.out.println("VcepCspecService: Retrying in " + backoffMs + "ms...");
                    Thread.sleep(backoffMs);
                    backoffMs *= 2; // Exponential backoff
                }
            }
        }

        throw lastException != null ? lastException : new RuntimeException("Failed to fetch RuleSet " + ruleSetId);
    }

    /**
     * Internal class to hold metadata during processing
     */
    private static class RuleSetMetadata {
        final String geneSymbol;
        final String vcepName;
        final String vcepUrl;
        final List<String> mondoIds;
        final String ruleSetId;
        final String sviUrl;
        final String version;

        RuleSetMetadata(String geneSymbol, String vcepName, String vcepUrl,
                        List<String> mondoIds, String ruleSetId, String sviUrl, String version) {
            this.geneSymbol = geneSymbol;
            this.vcepName = vcepName;
            this.vcepUrl = vcepUrl;
            this.mondoIds = mondoIds;
            this.ruleSetId = ruleSetId;
            this.sviUrl = sviUrl;
            this.version = version;
        }
    }
}

