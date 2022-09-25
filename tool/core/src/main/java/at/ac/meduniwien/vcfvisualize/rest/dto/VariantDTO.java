package at.ac.meduniwien.vcfvisualize.rest.dto;

import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.dto.GeneDTO;
import at.ac.meduniwien.vcfvisualize.rest.dto.genepanelsearch.IndexedGeneDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

public class VariantDTO {
    @Getter
    @Setter
    long pid;

    @Getter
    @Setter
    long vid;

    @Getter
    @Setter
    String chrom;

    @Getter
    @Setter
    String pos;

    @Getter
    @Setter
    String id;

    @Getter
    @Setter
    String ref;

    @Getter
    @Setter
    String alt;

    @Getter
    @Setter
    String qual;

    @Getter
    @Setter
    String filter;

    @Getter
    @Setter
    HashMap<String, String> info;

    @Getter
    @Setter
    String format;

    @Getter
    @Setter
    List<IndexedGeneDTO> indexedGeneDTOs;

    @Getter
    @Setter
    List<VariantDTO> isoforms;

    @Getter
    @Setter
    String igvUrl;

    @Getter
    @Setter
    String igvIndexUrl;

    @Getter
    @Setter
    String igvFormat;

}
