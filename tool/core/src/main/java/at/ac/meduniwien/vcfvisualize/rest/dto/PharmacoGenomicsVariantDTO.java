package at.ac.meduniwien.vcfvisualize.rest.dto;

import at.ac.meduniwien.vcfvisualize.model.Variant;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class PharmacoGenomicsVariantDTO {
    @Getter
    @Setter
    public VariantDTO variant;

    @Getter
    @Setter
    public List<PharmGKBAnnotationDTO> pharmGKBAnnotations;

    public PharmacoGenomicsVariantDTO(Variant variant, List<PharmGKBAnnotationDTO> pharmGKBAnnotations) {
        this.variant = variant.convertToDTO();
        this.pharmGKBAnnotations = pharmGKBAnnotations;
    }
}
