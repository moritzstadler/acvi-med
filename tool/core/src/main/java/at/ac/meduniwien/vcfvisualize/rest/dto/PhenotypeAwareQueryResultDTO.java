package at.ac.meduniwien.vcfvisualize.rest.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class PhenotypeAwareQueryResultDTO {

    @Getter
    @Setter
    public List<PhenotypeAwareVariantDTO> variants;

    @Getter
    @Setter
    public long elapsedMilliseconds;

}
