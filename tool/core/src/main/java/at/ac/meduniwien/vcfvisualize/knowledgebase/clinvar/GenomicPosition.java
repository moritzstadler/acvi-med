package at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public class GenomicPosition {

    @Getter
    @Setter
    String chrom;

    @Getter
    @Setter
    long pos;

    @Getter
    @Setter
    String ref;

    @Getter
    @Setter
    String alt;

    public GenomicPosition(String chrom, long pos, String ref, String alt) {
        this.chrom = chrom;
        this.pos = pos;
        this.ref = ref;
        this.alt = alt;
    }

    public GenomicPosition() {

    }

    @Override
    public String toString() {
        return "chr" + chrom + "-" + pos + "-" + ref + "-" + alt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenomicPosition that = (GenomicPosition) o;
        return pos == that.pos &&
                chrom.equals(that.chrom) &&
                ref.equals(that.ref) &&
                alt.equals(that.alt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chrom, pos, ref, alt);
    }
}
