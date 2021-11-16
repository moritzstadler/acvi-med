import com.mysql.cj.jdbc.MysqlDataSource;

import java.sql.*;
import java.util.ArrayList;
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

    public void createTable(String name, List<Info> header, List<Csq> csqArrayIds) throws SQLException {
        ArrayList<String> cols = new ArrayList<>();
        cols.add("pid BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY");
        cols.add("chrom VARCHAR(31)");
        cols.add("pos BIGINT");
        cols.add("ref TEXT");
        cols.add("alt TEXT");
        cols.add("qual TEXT");
        cols.add("filter TEXT");

        for (Info info : header) {
            String infoName = "info_" + info.getId().replaceAll("[^a-zA-Z0-9_]", "");
            String infoType = convertInfoTypeToMySqlType(info.getType());
            cols.add(String.format("%s %s", infoName, infoType));
        }

        csqNames = new ArrayList<>();
        for (Csq csqField : csqArrayIds) {
            String csqName = "info_csq_" + csqField.getName().replaceAll("[^a-zA-Z0-9_]", "");
            cols.add(String.format("%s %s", csqName, csqField.getMySqlType()));
            csqNames.add(csqName);
        }

        //TODO format

        String inner = cols.stream().collect(Collectors.joining(", "));
        String sql = String.format("CREATE TABLE %s (%s) ENGINE=InnoDB ROW_FORMAT=DYNAMIC", name, inner);

        PreparedStatement create = connection.prepareCall(sql);
        create.execute();
    }

    private String convertInfoTypeToMySqlType(String type) {
        type = type.toLowerCase();
        if (type.equals("string")) {
            return "TEXT";
        } else if (type.equals("integer")) {
            return "BIGINT";
        } else if (type.equals("float")) {
            return "DOUBLE";
        }

        return "TEXT";
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
        System.out.println(sql);
        PreparedStatement create = connection.prepareCall(sql);
        create.execute();
    }

    private String convertToMySqlString(String value) {
        return "'" + value.replaceAll("\"", "").replaceAll("'", "").replaceAll("\\\\", "") + "'";
    }

}
