package at.ac.meduniwien.vcfvisualize.model;

import at.ac.meduniwien.vcfvisualize.rest.dto.SampleDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public class Sample {

    @Getter
    @Setter
    long id;

    @Getter
    @Setter
    String name;

    @Getter
    @Setter
    String type;

    @Getter
    @Setter
    String igvPath;

    public Sample(long id) {
        this.id = id;
    }

    public Sample(String name, String type, String igvPath) {
        this.name = name;
        this.type = type;
        this.igvPath = igvPath;
    }

    public Sample(long id, String name, String type, String igvPath) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.igvPath = igvPath;
    }

    public SampleDTO convertToDTO() {
        SampleDTO sampleDTO = new SampleDTO();
        sampleDTO.id = this.id;
        sampleDTO.name = this.name;
        sampleDTO.type = this.type;
        sampleDTO.igvPath = this.igvPath;
        return sampleDTO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sample sample = (Sample) o;
        return id == sample.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
