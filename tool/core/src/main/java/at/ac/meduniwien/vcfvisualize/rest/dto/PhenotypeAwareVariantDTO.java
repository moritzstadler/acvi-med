package at.ac.meduniwien.vcfvisualize.rest.dto;

import at.ac.meduniwien.vcfvisualize.model.Variant;
import at.ac.meduniwien.vcfvisualize.processor.acmg.AcmgTieringResult;
import at.ac.meduniwien.vcfvisualize.rest.dto.genepanelsearch.IndexedGeneDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

public class PhenotypeAwareVariantDTO {
    @Getter
    @Setter
    VariantDTO variant;

    @Getter
    @Setter
    List<AcmgTieringResult> acmgTieringResults;

    @Getter
    @Setter
    List<String> hpoTermsLeadToDiscovery;

    @Getter
    @Setter
    List<String> panelsLeadToDiscovery;

    @Getter
    @Setter
    List<String> genesLeadToDiscovery;

    public PhenotypeAwareVariantDTO(Variant variant) {
        this.variant = variant.convertToDTO();
    }

    public PhenotypeAwareVariantDTO() {

    }
}
