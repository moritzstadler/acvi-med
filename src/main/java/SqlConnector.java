import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class SqlConnector {

    private static SqlConnector instance;

    public static SqlConnector getInstance() {
        if (instance == null) {
            instance = new SqlConnector();
        }
        return instance;
    }

    private Connection connection;
    private ArrayList<String> csqNames;
    private ArrayList<String> infoNames;
    private String[] formatNames;
    private ArrayList<String> formatTypes;
    private ArrayList<Integer> maxColSizes;
    private ArrayList<String> fullColList;
    private Random random = new Random();

    public SqlConnector() {
        random = new Random();
    }

    public void connect(String url, String username, String password) throws SQLException {
        System.out.println("Connecting database...");

        connection = DriverManager.getConnection(url, username, password);
    }

    public void useDatabase(String name) throws SQLException {
        /*PreparedStatement setFormat = connection.prepareStatement("SET innodb_strict_mode = 0");
        setFormat.execute();

        /*PreparedStatement create = connection.prepareCall("CREATE DATABASE IF NOT EXISTS " + name);
        create.execute();

        PreparedStatement use = connection.prepareStatement("\\c " + name);
        use.execute();*/
    }

    public void dropTable(String name) throws SQLException {
        PreparedStatement drop = connection.prepareCall("DROP TABLE IF EXISTS " + name);
        drop.execute();
    }

    public void createTable(String name, List<Info> header, List<Csq> csqArrayIds, String[] formatNames, ArrayList<String> formatTypes, ArrayList<Integer> maxColsSizes) throws SQLException {
        fullColList = new ArrayList<>();
        this.maxColSizes = maxColsSizes;

        ArrayList<String> cols = new ArrayList<>();
        cols.add("pid BIGINT");
        cols.add(String.format("chrom %s", convertInfoTypeToMySqlType("string", maxColsSizes.get(0))));
        cols.add("pos BIGINT");
        cols.add(String.format("ref %s", convertInfoTypeToMySqlType("string", maxColsSizes.get(2))));
        cols.add(String.format("alt %s", convertInfoTypeToMySqlType("string", maxColsSizes.get(3))));
        cols.add(String.format("qual %s", convertInfoTypeToMySqlType("string", maxColsSizes.get(4))));
        cols.add(String.format("filter %s", convertInfoTypeToMySqlType("string", maxColsSizes.get(5))));

        fullColList.add("chrom");
        fullColList.add("pos");
        fullColList.add("ref");
        fullColList.add("alt");
        fullColList.add("qual");
        fullColList.add("filter");

        int colCount = 6;

        infoNames = new ArrayList<>();
        for (Info info : header) {
            String infoName = "info_" + info.getId().replaceAll("[^a-zA-Z0-9_]", "");
            String infoType = convertInfoTypeToMySqlType(info.getType(), maxColsSizes.get(colCount));
            colCount++;
            cols.add(String.format("%s %s", infoName, infoType));
            infoNames.add(infoName);
            fullColList.add(infoName);
        }

        csqNames = new ArrayList<>();
        for (Csq csqField : csqArrayIds) {
            String csqName = "info_csq_" + csqField.getName().replaceAll("[^a-zA-Z0-9_]", "");
            cols.add(String.format("%s %s", csqName, csqField.getMySqlType(maxColsSizes.get(colCount))));
            colCount++;
            csqNames.add(csqName);
            fullColList.add(csqName);
        }

        this.formatNames = formatNames;
        this.formatTypes = formatTypes;
        for (String formatName : formatNames) {
            for (String formatType : formatTypes) {
                String colName = "format_" + formatName + "_" + formatType;
                cols.add(String.format("%s %s", colName, "VARCHAR(32)"));
                fullColList.add(colName);
            }
        }

        cols.add("PRIMARY KEY(pid)");
        String inner = cols.stream().collect(Collectors.joining(", "));
        String sql = String.format("CREATE TABLE %s (%s)", name, inner);

        System.out.println(sql);

        PreparedStatement create = connection.prepareCall(sql);
        create.execute();
        create.close();
    }

    private String convertInfoTypeToMySqlType(String type, int size) {
        type = type.toLowerCase();
        if (type.equals("integer")) {
            return "BIGINT";
        } else if (type.equals("float")) {
            return "DOUBLE PRECISION";
        }

        if (size < 256) {
            return String.format("VARCHAR(%s)", size);
        } else {
            return "TEXT";
        }
    }

    public void insertVariantBatch(final int pidStart, List<Variant> variants, String tableName, String[] formatNames) throws SQLException {
        if (variants.size() == 0) {
            return;
        }

        int currentPid = pidStart;

        String sql = String.format("INSERT INTO %s VALUES ", tableName);
        connection.setAutoCommit(false);
        ArrayList<String> individualSqls = new ArrayList<>();

        for (Variant variant : variants) {
            ArrayList<String> values = new ArrayList<>();

            values.add(currentPid + ""); //pid
            currentPid++;
            values.add(convertToMySqlString(variant.getChrom()));
            values.add(convertToMySqlString(variant.getPos()));
            values.add(convertToMySqlString(variant.getRef()));
            values.add(convertToMySqlString(variant.getAlt()));
            values.add(convertToMySqlString(variant.getQual()));
            values.add(convertToMySqlString(variant.getFilter()));

            /*create.setString(1, variant.getChrom());
            create.setString(2, variant.getPos());
            create.setString(3, variant.getRef());
            create.setString(4, variant.getAlt());
            create.setString(5, variant.getQual());
            create.setString(6, variant.getFilter());*/

            int colIndex = 7;

            for (String infoKey : infoNames) {
                if (!infoKey.equals("CSQ")) {
                    /*create.setString(colIndex, variant.getInfoMap().get(infoKey));
                    colIndex++;*/
                    String value = variant.getInfoMap().get(infoKey);
                    if (value == null || value.equals("")) {
                        values.add("NULL");
                    } else {
                        values.add(convertToMySqlString(value));
                    }
                }
            }

            String[] csq = variant.getInfoMap().get("CSQ").split("\\|", -1);
            for (int i = 0; i < csq.length; i++) {
                String value = csq[i];
                if (value.equals("")) {
                    value = null;
                }
                /*create.setString(colIndex, value);
                colIndex++;*/
                if (value == null) {
                    values.add("NULL");
                } else {
                    values.add(convertToMySqlString(value));
                }
            }

            String[] individualFormatTypes = variant.getFormat().split(":", -1);
            HashMap<String, String> formatKeyValueIndividual = new HashMap<>();
            for (String format : variant.getFormats()) {
                String[] formatSplit = format.split(":", -1);
                for (int i = 0; i < formatSplit.length; i++) {
                    formatKeyValueIndividual.put(individualFormatTypes[i], formatSplit[i]);
                }

                for (String formatType : formatTypes) {
                    if (formatKeyValueIndividual.containsKey(formatType)) {
                        values.add(convertToMySqlString(formatKeyValueIndividual.get(formatType)));
                    } else {
                        values.add("NULL");
                    }
                }
            }

            String individualSql = "(" + values.stream().collect(Collectors.joining(", ")) + ")";
            individualSqls.add(individualSql);
        }

        sql += individualSqls.stream().collect(Collectors.joining(", "));

        PreparedStatement create = connection.prepareStatement(sql);
        create.execute();
        create.close();

        connection.commit();
    }

    public void makeIndices(String tableName) throws SQLException {
        connection.setAutoCommit(true);

        Set<String> colsToIndex = new HashSet<>();
        for (int i = 0; i < maxColSizes.size(); i++) {
            if (maxColSizes.get(i) < 256) {
                colsToIndex.add(fullColList.get(i));
            }
        }

        int count = 0;
        for (String col : colsToIndex) {
            String indexName = tableName.substring(0, Math.min(15, tableName.length())) + count + Math.abs((tableName + col + count).hashCode()) + randomString(15);
            String sql = String.format("CREATE INDEX %s on %s (%s)", indexName, tableName, col);
            System.out.println(sql);
            PreparedStatement index = connection.prepareStatement(sql);
            index.execute();
            index.close();
            count++;
        }
    }

    public void insertVariant(Variant variant, String tableName) throws SQLException {
        List<String> columnsList = new LinkedList<>();
        List<String> valuesList = new LinkedList<>();

        columnsList.add("chrom");
        valuesList.add(convertToMySqlString(variant.getChrom()));
        columnsList.add("pos");
        valuesList.add(convertToMySqlString(variant.getPos()));
        columnsList.add("ref");
        valuesList.add(convertToMySqlString(variant.getRef()));
        columnsList.add("alt");
        valuesList.add(convertToMySqlString(variant.getAlt()));
        columnsList.add("qual");
        valuesList.add(convertToMySqlString(variant.getQual()));
        columnsList.add("filter");
        valuesList.add(convertToMySqlString(variant.getFilter()));

        for (String key : variant.getInfoMap().keySet()) {
            String value = variant.getInfoMap().get(key);
            if (key.equals("CSQ")) {
                String[] csqs = value.split("\\|");
                for (int i = 0; i < csqs.length; i++) {
                    columnsList.add(csqNames.get(i));
                    if (!csqs[i].equals("")) {
                        valuesList.add(convertToMySqlString(csqs[i]));
                    } else {
                        valuesList.add("NULL");
                    }
                }
            } else {
                columnsList.add("info_" + key);
                valuesList.add(convertToMySqlString(value));
            }
        }

        String columns = columnsList.stream().collect(Collectors.joining(", "));
        String values = valuesList.stream().collect(Collectors.joining(", "));

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, values);

        PreparedStatement create = connection.prepareCall(sql);
        create.execute();
        create.close();
    }

    private String convertToMySqlString(String value) {
        return "'" + value.replaceAll("\"", "").replaceAll("'", "").replaceAll("\\\\", "") + "'";
    }

    private String randomString(int length) {
        String result = "";
        for (int i = 0; i < length; i++) {
            int r = random.nextInt(26*2+10);
            char next;
            if (r < 10) {
                next = (char) ('0' + r);
            } else if (r < 36) {
                next = (char) ('a' + (r - 10));
            } else {
                next = (char) ('A' + ( - 36));
            }
            result += next;
        }
        return result;
    }

}
