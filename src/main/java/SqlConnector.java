import com.mysql.cj.jdbc.MysqlDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
    private ArrayList<String> genotypeRows;

    public SqlConnector() {
        this.genotypeRows = new ArrayList<>();
    }

    public void connect(String url, String username, String password) {
        System.out.println("Connecting database...");

        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUser(username);
        dataSource.setPassword(password);
        dataSource.setServerName(url);
        //dataSource.setDatabaseName("mysql");

        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void useDatabase(String name) throws SQLException {
        PreparedStatement setFormat = connection.prepareStatement("SET innodb_strict_mode = 0");
        setFormat.execute();

        PreparedStatement create = connection.prepareCall("CREATE DATABASE IF NOT EXISTS " + name);
        create.execute();

        PreparedStatement use = connection.prepareCall("USE " + name);
        use.execute();
    }

    public void dropTable(String name) throws SQLException {
        PreparedStatement drop = connection.prepareCall("DROP TABLE IF EXISTS " + name);
        drop.execute();
    }

    public void createTable(String name, List<Info> header, List<Csq> csqArrayIds, String[] formatNames, ArrayList<String> formatTypes, ArrayList<Integer> maxColsSizes) throws SQLException {
        ArrayList<String> cols = new ArrayList<>();
        cols.add("pid BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY");
        cols.add(String.format("chrom VARCHAR(%s)", maxColsSizes.get(0)));
        cols.add("pos BIGINT");
        cols.add(String.format("ref VARCHAR(%s)", maxColsSizes.get(2)));
        cols.add(String.format("alt VARCHAR(%s)", maxColsSizes.get(3)));
        cols.add(String.format("qual VARCHAR(%s)", maxColsSizes.get(4)));
        cols.add(String.format("filter VARCHAR(%s)", maxColsSizes.get(5)));

        int colCount = 6;

        infoNames = new ArrayList<>();
        for (Info info : header) {
            String infoName = "info_" + info.getId().replaceAll("[^a-zA-Z0-9_]", "");
            String infoType = convertInfoTypeToMySqlType(info.getType(), maxColsSizes.get(colCount));
            colCount++;
            cols.add(String.format("%s %s", infoName, infoType));
            infoNames.add(infoName);
        }

        csqNames = new ArrayList<>();
        for (Csq csqField : csqArrayIds) {
            String csqName = "info_csq_" + csqField.getName().replaceAll("[^a-zA-Z0-9_]", "");
            cols.add(String.format("%s %s", csqName, csqField.getMySqlType(maxColsSizes.get(colCount))));
            colCount++;
            csqNames.add(csqName);
        }

        this.formatNames = formatNames;
        this.formatTypes = formatTypes;
        for (String formatName : formatNames) {
            for (String formatType : formatTypes) {
                String colName = "format_" + formatName + "_" + formatType;
                cols.add(String.format("%s %s", colName, "VARCHAR(63)"));
                if (formatType.toLowerCase().equals("gt")) {
                    genotypeRows.add(colName);
                }
            }
        }

        String inner = cols.stream().collect(Collectors.joining(", "));
        String sql = String.format("CREATE TABLE %s (%s) ENGINE=InnoDB ROW_FORMAT=DYNAMIC", name, inner);

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
            return "DOUBLE";
        }

        if (size < 32) {
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
        String inner = genotypeRows.stream().collect(Collectors.joining(", "));
        String sql = String.format("CREATE INDEX format_gt_index on %s (%s);", tableName, inner);
        PreparedStatement genotypeIndex = connection.prepareStatement(sql);
        genotypeIndex.execute();
        genotypeIndex.close();
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

}
