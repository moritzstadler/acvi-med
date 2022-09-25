package at.ac.meduniwien.vcfvisualize.model;

import at.ac.meduniwien.vcfvisualize.rest.dto.StudyDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Study {

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
    List<Sample> samples;

    public Study(long id) {
        samples = new LinkedList<>();
        this.id = id;
    }

    public Study(String name, String type) {
        samples = new LinkedList<>();
        this.name = name;
        this.type = type;
    }

    public Study(long id, String name, String type) {
        samples = new LinkedList<>();
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public StudyDTO convertToDTO() {
        StudyDTO studyDTO = new StudyDTO();
        studyDTO.id = this.id;
        studyDTO.name = this.name;
        studyDTO.type = this.type;
        studyDTO.samples = this.samples.stream().map(Sample::convertToDTO).collect(Collectors.toList());
        return studyDTO;
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
