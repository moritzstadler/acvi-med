package at.ac.meduniwien.vcfvisualize.rest.dto;

import java.util.List;

public class FieldMetaDataDTO {

    public String id;
    public String name;
    public String description;
    public String type;
    public String link;
    public String sample;
    public String samplecomparator;
    public String range;
    public String normalizationfunction;
    public boolean displayable;
    public double from;
    public double to;
    public String displaytype;
    public List<String> discreetvalues;
    public long nonnullrows;
    public long overallrows;

}
