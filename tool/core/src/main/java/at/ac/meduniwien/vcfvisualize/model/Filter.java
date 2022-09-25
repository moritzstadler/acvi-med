package at.ac.meduniwien.vcfvisualize.model;

import at.ac.meduniwien.vcfvisualize.model.expression.*;
import at.ac.meduniwien.vcfvisualize.rest.dto.FilterDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Filter {

    //TODO check security
    public Filter(FilterDTO filterDTO) {
        this.offset = filterDTO.offset;
        this.order = filterDTO.order.stream().map((order) -> new Order(order.name, order.ascending)).collect(Collectors.toList());
        this.expression = convertToExpression(filterDTO.expression);
    }

    //this needs to be replaced somehow
    @Getter
    @Setter
    Expression expression;

    @Getter
    @Setter
    List<Order> order;

    @Getter
    @Setter
    long offset;

    public String toSqlString() {
        String result = expression.toSQLString();
        if (order.size() > 0) {
            String orderExpression = order.stream().map(o -> alterForCustomSort(o.name) + " " + (o.ascending ? "ASC NULLS FIRST" : "DESC NULLS LAST")).collect(Collectors.joining(", "));
            result += " ORDER BY " + orderExpression;
        } else {
            result += " ORDER BY pid";
        }
        return result;
    }

    private static String alterForCustomSort(String name) {
        if (name.equals("info_csq_impact")) {
            return "array_position(array[Cast('MODIFIER' AS VARCHAR),Cast('LOW' AS VARCHAR),Cast('MODERATE' AS VARCHAR),Cast('HIGH' AS VARCHAR)],info_csq_impact)";
        }
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Filter filter = (Filter) o;
        return offset == filter.offset &&
                expression.toSQLString().equals(filter.expression.toSQLString()) &&
                order.equals(filter.order);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, order, offset);
    }

    private Expression convertToExpression(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.get("basic") == null) {
            return new EmptyExpression();
        }

        if (jsonNode.get("basic").asBoolean()) {
            if (jsonNode.get("comparator").asText().equals("IN")) {
                ArrayList<String> values = new ArrayList<>();
                ArrayNode arrayNode = (ArrayNode) jsonNode.get("values");
                for (int i = 0; i < arrayNode.size(); i++) {
                    values.add(arrayNode.get(i).textValue());
                }
                return new EnumExpression(jsonNode.get("name").asText(), values);
            } else {
                return new BasicExpression(jsonNode.get("name").asText(), jsonNode.get("comparator").asText(), jsonNode.get("value").asDouble());
            }
        } else {
            ArrayList<Expression> children = new ArrayList<>();
            ArrayNode childrenArrayNode = (ArrayNode) jsonNode.get("children");
            for (int i = 0; i < childrenArrayNode.size(); i++) {
                children.add(convertToExpression(childrenArrayNode.get(i)));
            }

            if (children.size() == 0) {
                return new EmptyExpression();
            }

            //TODO build this to iterator
            ArrayList<String> operators = new ArrayList<>();
            ArrayNode operatorsArrayNode = (ArrayNode) jsonNode.get("operators");
            for (int i = 0; i < operatorsArrayNode.size(); i++) {
                operators.add(operatorsArrayNode.get(i).asText());
            }
            return new IntermediateExpression(operators, children);

        }
    }

    /**
     * returns all fields in the expression
     * e. g. x > 1 AND y > 2 AND x < 5 returns {x, y}
     * @return all fields
     */
    public Set<String> getFields() {
        return expression.getFields();
    }
}

class Order {
    String name;
    boolean ascending;

    public Order(String name, boolean ascending) {
        this.name = name;
        this.ascending = ascending;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order that = (Order) o;
        return ascending == that.ascending &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, ascending);
    }
}
