package at.ac.meduniwien.vcfvisualize.model.expression;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is for finding things like rsIds
 *
 * Assume that the field 'name' contains data like "xxxxxxx&rs1234&xxxxxx" or "rs1234&xxxxx" or "xxxxx&rs1234" or "rs1234".
 * Now we want to find all where rs1234 is included between '&' (=delimiter).
 * We don't want to find something like "xxxx&rs1234567&xxxx" with our r1234 query which is why we canont use LIKE.
 *
 * Note that most likely an index is necessary:
 *
 * create extension pg_trgm;
 * create index on TABLENAME using gin (COLUMNNAME gin_trgm_ops);
 */
public class SeparatedInclusionExpression implements Expression {

    public SeparatedInclusionExpression(String name, List<String> values, String delimiter) {
        this.name = name;
        this.values = values;
        this.delimiter = delimiter;
    }

    //TODO avoid sql injection
    String name;
    List<String> values;
    String delimiter;

    @Override
    public String toSQLString() {
        if (values.size() == 0) {
            return "false";
        }

        //info_csq_existing_variation ~ '(^|&)(rs1234|rs1172912582|rs1553179954)(&|$)'

        return name + " ~ '(^|&)(" + String.join("|", values) + ")(&|$)'";
    }

    @Override
    public Set<String> getFields() {
        return new HashSet<>(Collections.singletonList(name));
    }
}
