package at.ac.meduniwien.vcfvisualize.knowledgebase.pharmgkb;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public class PharmGKBAllele {

    @Getter
    @Setter
    String clinicalAnnotationId;

    @Getter
    @Setter
    String genotype;

    @Getter
    @Setter
    String annotationText;

    @Getter
    @Setter
    String alleleFunction;

    public PharmGKBAllele(String clinicalAnnotationId, String genotype, String annotationText, String alleleFunction) {
        this.clinicalAnnotationId = clinicalAnnotationId;
        this.genotype = genotype;
        this.annotationText = annotationText;
        this.alleleFunction = alleleFunction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PharmGKBAllele that = (PharmGKBAllele) o;
        return Objects.equals(clinicalAnnotationId, that.clinicalAnnotationId) &&
                Objects.equals(genotype, that.genotype) &&
                Objects.equals(annotationText, that.annotationText) &&
                Objects.equals(alleleFunction, that.alleleFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clinicalAnnotationId, genotype, annotationText, alleleFunction);
    }
}
