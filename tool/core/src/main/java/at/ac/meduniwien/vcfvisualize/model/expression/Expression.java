package at.ac.meduniwien.vcfvisualize.model.expression;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface Expression {

    String toSQLString();

    Set<String> getFields();

}
