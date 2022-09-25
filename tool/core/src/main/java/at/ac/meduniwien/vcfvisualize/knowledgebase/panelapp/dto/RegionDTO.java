package at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto;

public class RegionDTO {

    GeneDataDTO gene_data;
    String entity_type;
    String entity_name;
    String verbose_name;
    String confidence_level;
    String penetrance;
    String mode_of_pathogenicity;
    String haploinsufficiency_score;
    String triplosensitivity_score;
    int required_overlap_percentage;
    String type_of_variants;
    String[] publications;
    String[] evidence;
    String[] phenotypes;
    String mode_of_inheritance;
    String chromosome;
    long[] grch37_coordinates;
    long[] grch38_coordinates;
    String[] tags;

}
