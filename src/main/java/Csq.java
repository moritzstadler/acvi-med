public class Csq {

    private String name;
    private int sqlTypeLevel;

    public Csq(String name) {
        this.name = name;
        sqlTypeLevel = 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSqlTypeLevel() {
        return sqlTypeLevel;
    }

    /**
     * sets the type according to the worst input
     * INT = 0
     * DOUBLE = 1
     * TEXT = 2
     * the final type used in the creation of the database will be the maximum of all types
     * e. g. if at least one field is a string, the type will be TEXT; if all are either int or double, the type will be double
     * @param input the value in the variant
     */
    public void matchType(String input) {
        int prevLevel = sqlTypeLevel;
        sqlTypeLevel = Math.max(sqlTypeLevel, SqlConnector.computeSqlTypeLevel(input));

        if (sqlTypeLevel != prevLevel) {
            System.out.println("CSQ Field " + name + " was changed from " + SqlConnector.getSqlType(prevLevel, Integer.MAX_VALUE) + " to " + SqlConnector.getSqlType(sqlTypeLevel, Integer.MAX_VALUE) + " by '" + input + "'");
        }
    }

}
