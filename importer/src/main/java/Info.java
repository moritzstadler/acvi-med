public class Info {

    private String id;
    private String number;
    private String type;
    private String description;
    private int sqlTypeLevel;

    public Info() {
    }

    public Info(String id, String number, String type, String description) {
        this.id = id;
        this.number = number;
        this.type = type;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
            System.out.println("INFO Field " + id + " was changed from " + SqlConnector.getSqlType(prevLevel, Integer.MAX_VALUE) + " to " + SqlConnector.getSqlType(sqlTypeLevel, Integer.MAX_VALUE) + " by '" + input + "'");
        }
    }
}
