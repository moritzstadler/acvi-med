package at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto;

public class PanelDTO {
    public int id;
    public String hash_id;
    public String name;
    public String disease_group;
    public String disease_sub_group;
    public String version;
    public String version_created;
    public String[] relevant_disorders;
    public StatsDTO stats;
    public TypeDTO[] types;
}

class StatsDTO {
    public int number_of_genes;
    public int number_of_strs;
    public int number_of_regions;
}

class TypeDTO {
    public String name;
    public String slug;
    public String description;
}
