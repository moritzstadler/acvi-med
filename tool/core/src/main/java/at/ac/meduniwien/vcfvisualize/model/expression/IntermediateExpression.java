package at.ac.meduniwien.vcfvisualize.model.expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class IntermediateExpression implements Expression {

    public IntermediateExpression(ArrayList<String> operators, ArrayList<Expression> children) {
        this.operators = operators;
        this.children = children;
    }

    //TODO check for operator length, security
    ArrayList<String> operators;
    ArrayList<Expression> children;

    //TODO this poses a security issue, operators can contain any sql
    @Override
    public String toSQLString() {
        StringBuilder result = new StringBuilder();

        result.append("(");
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) {
                result.append(" ").append(operators.get(i - 1)).append(" ");
            }
            result.append(children.get(i).toSQLString());
        }
        result.append(")");

        return result.toString();
    }

    @Override
    public Set<String> getFields() {
        HashSet<String> fields = new HashSet<>();
        for (Expression expression : children) {
            fields.addAll(expression.getFields());
        }
        return fields;
    }
}
