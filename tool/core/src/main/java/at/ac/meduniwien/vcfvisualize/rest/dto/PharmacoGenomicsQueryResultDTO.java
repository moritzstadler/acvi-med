package at.ac.meduniwien.vcfvisualize.rest.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

public class PharmacoGenomicsQueryResultDTO {

    @Getter
    @Setter
    public List<PharmacoGenomicsVariantDTO> variants;

    @Getter
    @Setter
    public long elapsedMilliseconds;

    public PharmacoGenomicsQueryResultDTO() {
    }

}
