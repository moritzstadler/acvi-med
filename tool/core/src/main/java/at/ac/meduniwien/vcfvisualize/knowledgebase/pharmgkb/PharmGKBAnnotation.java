package at.ac.meduniwien.vcfvisualize.knowledgebase.pharmgkb;

import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.Gene;
import at.ac.meduniwien.vcfvisualize.model.Variant;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public class PharmGKBAnnotation {

    @Getter
    @Setter
    String clinicalAnnotationId;

    @Getter
    @Setter
    String variantOrHaplotypes;

    @Getter
    @Setter
    String gene;

    @Getter
    @Setter
    String levelOfEvidence;

    @Getter
    @Setter
    String levelOverride;

    @Getter
    @Setter
    String levelModifierss;

    @Getter
    @Setter
    String score;

    @Getter
    @Setter
    String phenotypeCategory;

    @Getter
    @Setter
    String pmidCount;

    @Getter
    @Setter
    String evidenceCount;

    @Getter
    @Setter
    String drugs;

    @Getter
    @Setter
    String phenotypes;

    @Getter
    @Setter
    String latestHistoryDate;

    @Getter
    @Setter
    String url;

    @Getter
    @Setter
    String specialtyPopulation;

    public PharmGKBAnnotation(String clinicalAnnotationId, String variantOrHaplotypes, String gene, String levelOfEvidence, String levelOverride, String levelModifierss, String score, String phenotypeCategory, String pmidCount, String evidenceCount, String drugs, String phenotypes, String latestHistoryDate, String url, String specialtyPopulation) {
        this.clinicalAnnotationId = clinicalAnnotationId;
        this.variantOrHaplotypes = variantOrHaplotypes;
        this.gene = gene;
        this.levelOfEvidence = levelOfEvidence;
        this.levelOverride = levelOverride;
        this.levelModifierss = levelModifierss;
        this.score = score;
        this.phenotypeCategory = phenotypeCategory;
        this.pmidCount = pmidCount;
        this.evidenceCount = evidenceCount;
        this.drugs = drugs;
        this.phenotypes = phenotypes;
        this.latestHistoryDate = latestHistoryDate;
        this.url = url;
        this.specialtyPopulation = specialtyPopulation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PharmGKBAnnotation that = (PharmGKBAnnotation) o;
        return Objects.equals(clinicalAnnotationId, that.clinicalAnnotationId) &&
                Objects.equals(variantOrHaplotypes, that.variantOrHaplotypes) &&
                Objects.equals(gene, that.gene) &&
                Objects.equals(levelOfEvidence, that.levelOfEvidence) &&
                Objects.equals(levelOverride, that.levelOverride) &&
                Objects.equals(levelModifierss, that.levelModifierss) &&
                Objects.equals(score, that.score) &&
                Objects.equals(phenotypeCategory, that.phenotypeCategory) &&
                Objects.equals(pmidCount, that.pmidCount) &&
                Objects.equals(evidenceCount, that.evidenceCount) &&
                Objects.equals(drugs, that.drugs) &&
                Objects.equals(phenotypes, that.phenotypes) &&
                Objects.equals(latestHistoryDate, that.latestHistoryDate) &&
                Objects.equals(url, that.url) &&
                Objects.equals(specialtyPopulation, that.specialtyPopulation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clinicalAnnotationId, variantOrHaplotypes, gene, levelOfEvidence, levelOverride, levelModifierss, score, phenotypeCategory, pmidCount, evidenceCount, drugs, phenotypes, latestHistoryDate, url, specialtyPopulation);
    }
}
