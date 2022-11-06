package at.ac.meduniwien.vcfvisualize.rest.dto;

import lombok.Getter;
import lombok.Setter;

public class HumanDTO {

    @Getter
    @Setter
    String pseudonym;

    @Getter
    @Setter
    Boolean isAffected;

    @Getter
    @Setter
    Boolean isIndex;

}
