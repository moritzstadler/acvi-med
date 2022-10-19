package at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar;

import lombok.Getter;
import lombok.Setter;

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

}
