package at.ac.meduniwien.vcfvisualize.rest.dto;

import at.ac.meduniwien.vcfvisualize.knowledgebase.acmg.SecondaryFindingDefinition;
import at.ac.meduniwien.vcfvisualize.model.Variant;
import at.ac.meduniwien.vcfvisualize.processor.acmg.AcmgClassificationResult;
import at.ac.meduniwien.vcfvisualize.processor.acmg.AcmgTieringResult;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

public class SecondaryFindingVariantDTO {
    @Getter
    @Setter
    public VariantDTO variant;

    @Getter
    @Setter
    public List<AcmgTieringResult> acmgTieringResults;

    @Getter
    @Setter
    public AcmgClassificationResult acmgClassificationResult;

    @Getter
    @Setter
    public boolean clinvarPositive;

    @Getter
    @Setter
    public SecondaryFindingDefinition secondaryFindingDefinition;

    public SecondaryFindingVariantDTO(Variant variant) {
        this.variant = variant.convertToDTO();
    }

    public SecondaryFindingVariantDTO() {

    }
}
