package at.ac.meduniwien.vcfvisualize.knowledgebase.pharmgkb;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public class PharmGKBEvidence {

    @Getter
    @Setter
    String clinicalAnnotationId;

    @Getter
    @Setter
    String evidenceId;

    @Getter
    @Setter
    String evidenceType;

    @Getter
    @Setter
    String evidenceUrl;

    @Getter
    @Setter
    String pmid;

    @Getter
    @Setter
    String summary;

    @Getter
    @Setter
    String score;

    public PharmGKBEvidence(String clinicalAnnotationId, String evidenceId, String evidenceType, String evidenceUrl, String pmid, String summary, String score) {
        this.clinicalAnnotationId = clinicalAnnotationId;
        this.evidenceId = evidenceId;
        this.evidenceType = evidenceType;
        this.evidenceUrl = evidenceUrl;
        this.pmid = pmid;
        this.summary = summary;
        this.score = score;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PharmGKBEvidence that = (PharmGKBEvidence) o;
        return Objects.equals(clinicalAnnotationId, that.clinicalAnnotationId) &&
                Objects.equals(evidenceId, that.evidenceId) &&
                Objects.equals(evidenceType, that.evidenceType) &&
                Objects.equals(evidenceUrl, that.evidenceUrl) &&
                Objects.equals(pmid, that.pmid) &&
                Objects.equals(summary, that.summary) &&
                Objects.equals(score, that.score);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clinicalAnnotationId, evidenceId, evidenceType, evidenceUrl, pmid, summary, score);
    }
}
