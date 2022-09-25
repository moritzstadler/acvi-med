package at.ac.meduniwien.vcfvisualize.model;

import lombok.Getter;
import lombok.Setter;

public class Column {

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String datatype;

    public Column(String name, String datatype) {
        this.name = name;
        this.datatype = datatype;
    }
}
