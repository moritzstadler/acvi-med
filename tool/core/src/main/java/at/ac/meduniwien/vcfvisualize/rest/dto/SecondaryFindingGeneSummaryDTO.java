package at.ac.meduniwien.vcfvisualize.rest.dto;

import at.ac.meduniwien.vcfvisualize.knowledgebase.acmg.SecondaryFindingDefinition;
import at.ac.meduniwien.vcfvisualize.model.Variant;
import at.ac.meduniwien.vcfvisualize.processor.acmg.AcmgClassificationResult;
import at.ac.meduniwien.vcfvisualize.processor.acmg.AcmgTieringResult;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class SecondaryFindingGeneSummaryDTO {
    @Getter
    @Setter
    public String gene;

    @Getter
    @Setter
    public int numberOfVariants;

    @Getter
    @Setter
    public int numberOfFindings;

    public SecondaryFindingGeneSummaryDTO(String gene) {
        this.gene = gene;
    }
}
