package at.ac.meduniwien.vcfvisualize.knowledgebase.acmg;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class SecondaryFindingDefinition {

    @Getter
    @Setter
    String domain;

    @Getter
    @Setter
    String phenotype;

    @Getter
    @Setter
    String acmgListVersion;

    @Getter
    @Setter
    List<String> omimId;

    @Getter
    @Setter
    String gene;

    @Getter
    @Setter
    String inheritance; //AR, AD, XL

    @Getter
    @Setter
    boolean reportKnownPathogenic;

    @Getter
    @Setter
    boolean reportExpectedPathogenic;

    @Getter
    @Setter
    boolean typicalOnsetChild;

    @Getter
    @Setter
    boolean typicalOnsetAdult;

}
