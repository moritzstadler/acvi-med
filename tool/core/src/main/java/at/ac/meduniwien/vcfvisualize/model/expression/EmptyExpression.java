package at.ac.meduniwien.vcfvisualize.model.expression;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EmptyExpression implements Expression {


    @Override
    public String toSQLString() {
        return "true";
    }

    @Override
    public Set<String> getFields() {
        return new HashSet<>();
    }
}
