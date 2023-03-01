package at.ac.meduniwien.vcfvisualize.model;

import at.ac.meduniwien.vcfvisualize.rest.dto.NoteDTO;
import at.ac.meduniwien.vcfvisualize.rest.dto.SampleDTO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

public class Note {

    @Getter
    @Setter
    long id;

    @Getter
    @Setter
    long sampleId;

    @Getter
    @Setter
    String sampleName;

    @Getter
    @Setter
    long researcherId;

    @Getter
    @Setter
    String researcherName;

    @Getter
    @Setter
    long variantId;

    @Getter
    @Setter
    String variantPosition;

    @Getter
    @Setter
    String note;

    @Getter
    @Setter
    LocalDateTime time;

    public Note(long id) {
        this.id = id;
    }

    public Note(long id, long sampleId, String sampleName, long researcherId, String researcherName, long variantId, String variantPosition, String note, LocalDateTime time) {
        this.id = id;
        this.sampleId = sampleId;
        this.sampleName = sampleName;
        this.researcherId = researcherId;
        this.researcherName = researcherName;
        this.variantId = variantId;
        this.variantPosition = variantPosition;
        this.note = note;
        this.time = time;
    }

    public NoteDTO convertToDTO() {
        NoteDTO noteDTO = new NoteDTO();
        noteDTO.id = this.id;
        noteDTO.sampleId = this.sampleId;
        noteDTO.sampleName = this.sampleName;
        noteDTO.researcherId = this.researcherId;
        noteDTO.researcherName = this.researcherName;
        noteDTO.variantId = this.variantId;
        noteDTO.variantPosition = this.variantPosition;
        noteDTO.note = this.note;
        noteDTO.time = this.time;
        return noteDTO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note sample = (Note) o;
        return id == sample.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
