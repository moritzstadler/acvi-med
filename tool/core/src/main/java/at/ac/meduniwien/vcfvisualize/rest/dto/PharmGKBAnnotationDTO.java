package at.ac.meduniwien.vcfvisualize.rest.dto;

import at.ac.meduniwien.vcfvisualize.knowledgebase.pharmgkb.PharmGKBAllele;
import at.ac.meduniwien.vcfvisualize.knowledgebase.pharmgkb.PharmGKBAnnotation;
import at.ac.meduniwien.vcfvisualize.knowledgebase.pharmgkb.PharmGKBEvidence;
import at.ac.meduniwien.vcfvisualize.model.Variant;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class PharmGKBAnnotationDTO {

    @Getter
    @Setter
    public PharmGKBAnnotation pharmGKBAnnotation;

    @Getter
    @Setter
    public List<PharmGKBEvidence> pharmGKBEvidence;

    @Getter
    @Setter
    public List<PharmGKBAllele> pharmGKBAlleles;

    public PharmGKBAnnotationDTO(PharmGKBAnnotation pharmGKBAnnotation, List<PharmGKBEvidence> pharmGKBEvidence, List<PharmGKBAllele> pharmGKBAlleles) {
        this.pharmGKBAnnotation = pharmGKBAnnotation;
        this.pharmGKBEvidence = pharmGKBEvidence;
        this.pharmGKBAlleles = pharmGKBAlleles;
    }
}
