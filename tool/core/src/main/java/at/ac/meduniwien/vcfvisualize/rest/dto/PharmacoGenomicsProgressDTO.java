package at.ac.meduniwien.vcfvisualize.rest.dto;

import lombok.Getter;
import lombok.Setter;

public class PharmacoGenomicsProgressDTO {
    @Getter
    @Setter
    public int processed;

    @Getter
    @Setter
    public int total;

    @Getter
    @Setter
    public long elapsed;

    public PharmacoGenomicsProgressDTO(int processed, int total, long elapsed) {
        this.processed = processed;
        this.total = total;
        this.elapsed = elapsed;
    }
}
