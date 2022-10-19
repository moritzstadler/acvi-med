package at.ac.meduniwien.vcfvisualize.knowledgebase.hpo;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

public class HpoTerm {

    @Getter
    @Setter
    String id;

    @Getter
    List<String> altIds;

    @Getter
    @Setter
    String name;

    @Getter
    @Setter
    String def;

    @Getter
    List<String> synonym;

    public HpoTerm() {
        this.altIds = new LinkedList<>();
        this.synonym = new LinkedList<>();
    }
}
