package at.ac.meduniwien.vcfvisualize.rest.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

public class SecondaryFindingsQueryResultDTO {

    @Getter
    @Setter
    public List<SecondaryFindingVariantDTO> variants;

    @Getter
    @Setter
    public long elapsedMilliseconds;

    @Getter
    @Setter
    public HashMap<String, SecondaryFindingGeneSummaryDTO> secondaryFindingGeneSummaryDTOs;

    public SecondaryFindingsQueryResultDTO() {
        secondaryFindingGeneSummaryDTOs = new HashMap<>();
    }

    /**
     * this will increase a counter stating that a variant on the gene has been analyzed
     *
     * @param gene the gene of the variant
     * @param finding true if there was a (secondary) finding on this gene, false if it was just analyzed without pathogenic result
     */
    public void increaseVariantCounter(String gene, boolean finding) {
        if (!secondaryFindingGeneSummaryDTOs.containsKey(gene)) {
            secondaryFindingGeneSummaryDTOs.put(gene, new SecondaryFindingGeneSummaryDTO(gene));
        }

        if (finding) {
            secondaryFindingGeneSummaryDTOs.get(gene).numberOfFindings++;
        }
        secondaryFindingGeneSummaryDTOs.get(gene).numberOfVariants++;
    }
}
