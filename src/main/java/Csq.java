public class Csq {

    private String name;
    private int mySqlTypeLevel;

    public Csq(String name) {
        this.name = name;
        mySqlTypeLevel = 0;
    }

    public Csq() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMySqlType(int size) {
        if (mySqlTypeLevel == 0) {
            return "BIGINT";
        } else if (mySqlTypeLevel == 1) {
            return "DOUBLE";
        } else {
            return String.format("VARCHAR(%s)", size);
        }
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
        int prevLevel = mySqlTypeLevel;
        mySqlTypeLevel = Math.max(mySqlTypeLevel, getMySqlTypeLevel(input));

        if (mySqlTypeLevel != prevLevel) {
            System.out.println("Field " + name + " was changed from " + prevLevel + " to " + mySqlTypeLevel + " by '" + input + "'");
        }
    }

    private int getMySqlTypeLevel(String input) {
        if (input == null || input.equals("")) {
            return 0;
        }

        if (input.matches("[-]?[0-9]+")) {
            return 0;
        } else if (input.matches("[-]?([0-9]+[.])?[0-9]+")) {
            return 1;
        }
        return 2;
    }

}
