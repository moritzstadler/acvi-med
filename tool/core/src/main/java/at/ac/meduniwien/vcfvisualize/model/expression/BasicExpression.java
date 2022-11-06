package at.ac.meduniwien.vcfvisualize.model.expression;

import java.util.*;

public class BasicExpression<T> implements Expression {

    public BasicExpression(String name, String comparator, T value) {
        this.name = name;
        this.comparator = comparator;
        this.value = value;
    }

    //TODO security avoid SQL injection
    String name;
    String comparator;
    T value;

    @Override
    public String toSQLString() {
        //TODO move to config
        List<String> treatNullAsZeroFields = Arrays.asList(
                "info_controls_af_popmax",
                "info_af_afr",
                "info_af_amr",
                "info_af_asj",
                "info_af_eas",
                "info_af_nfe",
                "info_af_oth",
                "info_af_raw");
        if (comparator.equals("<") && treatNullAsZeroFields.contains(name)) {
            return "(" + name + " " + comparator + " " + value + " OR " + name + " IS NULL)";
        }

        return name + " " + comparator + " " + value;
    }

    @Override
    public Set<String> getFields() {
        return new HashSet<>(Collections.singletonList(name));
    }
}
