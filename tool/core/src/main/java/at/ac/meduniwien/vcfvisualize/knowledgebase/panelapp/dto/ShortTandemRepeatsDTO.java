package at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto;

public class ShortTandemRepeatsDTO {

    public GeneDataDTO gene_data;
    public String entity_type;
    public String entity_name;
    public String confidence_level;
    public String penetrance;
    public String[] publications;
    public String[] evidence;
    public String[] phenotypes;
    public String mode_of_inheritance;
    public String repeated_sequence;
    public long[] grch37_coordinates;
    public long[] grch38_coordinates;
    public int normal_repeats;
    public int pathogenic_repeats;
    public String[] tags;

}
