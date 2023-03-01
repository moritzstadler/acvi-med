package at.ac.meduniwien.vcfvisualize.rest.dto;

import java.time.LocalDateTime;

public class NoteDTO {

    public long id;
    public long sampleId;
    public String sampleName;
    public long researcherId;
    public String researcherName;
    public long variantId;
    public String variantPosition;
    public String note;
    public LocalDateTime time;

}
