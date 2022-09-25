package at.ac.meduniwien.vcfvisualize.model.expression;

import java.util.*;
import java.util.stream.Collectors;

public class EnumExpression implements Expression {

    public EnumExpression(String name, List<String> values) {
        //TODO check if any of these contain anything other than a-zA-Z0-9_.
        this.name = name;
        this.values = values;
    }

    //TODO avoid sql injection
    String name;
    List<String> values;

    @Override
    public String toSQLString() {
        if (values.size() == 0) {
            return "false";
        }

        return name + " IN (" + values.stream().map((value) -> "'" + value + "'").collect(Collectors.joining(",")) + ")";
    }

    @Override
    public Set<String> getFields() {
        return new HashSet<>(Collections.singletonList(name));
    }
}
