import org.postgresql.copy.CopyIn;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.postgresql.jdbc.PgConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class SqlConnector {

    private static SqlConnector instance;

    public static SqlConnector getInstance() {
        if (instance == null) {
            instance = new SqlConnector();
        }
        return instance;
    }

    private String[] columnsToIndex;
    private Connection connection;
    private Connection[] batchConnections;
    private int batchConnectionPosition = 0;
    private ArrayList<String> csqNames;
    private ArrayList<String> infoNames;
    private ArrayList<String> formatTypes;
    private ArrayList<String> fullColList;
    private Random random;

    public SqlConnector() {
        random = new Random();

        Properties prop = new Properties();
        try {
            //load columns which are to be indexed from config file
            prop.load(SqlConnector.class.getResourceAsStream("/application.properties"));
            columnsToIndex = prop.getProperty("columnsToIndex", "").split(",");
        } catch (Exception ex) {
            columnsToIndex = new String[0];
            ex.printStackTrace();
        }
    }

    public void connect(String url, String username, String password) throws SQLException {
        System.out.println("Connecting database...");

        connection = DriverManager.getConnection(url, username, password);
        batchConnections = new Connection[20];
        for (int i = 0; i < batchConnections.length; i++) {
            batchConnections[i] = DriverManager.getConnection(url, username, password);
        }
    }

    private Connection getBatchConnection() {
        batchConnectionPosition++;
        batchConnectionPosition %= batchConnections.length;
        return batchConnections[batchConnectionPosition];
    }

    public void dropTable(String name) throws SQLException {
        PreparedStatement drop = connection.prepareCall("DROP TABLE IF EXISTS " + name);
        drop.execute();
    }

    public void createTable(String name, List<Info> header, Csq[] csqFields, String[] formatNames, ArrayList<String> formatTypes, ArrayList<Integer> maxColsSizes) throws SQLException {
        fullColList = new ArrayList<>();

        ArrayList<String> cols = new ArrayList<>();
        cols.add("pid BIGINT");
        cols.add("vid BIGINT");
        cols.add(String.format("chrom %s", getSqlTypeString(maxColsSizes.get(0))));
        cols.add("pos BIGINT");
        cols.add(String.format("ref %s", getSqlTypeString(maxColsSizes.get(2))));
        cols.add(String.format("alt %s", getSqlTypeString(maxColsSizes.get(3))));
        cols.add(String.format("qual %s", getSqlTypeString(maxColsSizes.get(4))));
        cols.add(String.format("filter %s", getSqlTypeString(maxColsSizes.get(5))));

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
            String infoType = getSqlType(info.getSqlTypeLevel(), maxColsSizes.get(colCount));
            colCount++;
            cols.add(String.format("%s %s", infoName, infoType));
            infoNames.add(info.getId());
            fullColList.add(infoName);
        }

        csqNames = new ArrayList<>();
        for (Csq csqField : csqFields) {
            String csqName = "info_csq_" + csqField.getName().replaceAll("[^a-zA-Z0-9_]", "");
            cols.add(String.format("%s %s", csqName, getSqlType(csqField.getSqlTypeLevel(), maxColsSizes.get(colCount))));
            colCount++;
            csqNames.add(csqName);
            fullColList.add(csqName);
        }

        this.formatTypes = formatTypes;
        for (String formatName : formatNames) {
            for (String formatType : formatTypes) {
                String colName = "format_" + formatName + "_" + formatType;
                if (formatType.toLowerCase().endsWith("_gt")) {
                    cols.add(String.format("%s %s", colName, "VARCHAR(255)"));
                } else {
                    cols.add(String.format("%s %s", colName, "TEXT"));
                }
                fullColList.add(colName);
            }
        }

        cols.add("PRIMARY KEY(pid)");
        String inner = String.join(", ", cols);
        String sql = String.format("CREATE TABLE %s (%s)", name, inner);

        System.out.println(sql);

        PreparedStatement create = connection.prepareCall(sql);
        create.execute();
        create.close();
    }

    public void insertVariantBatch(final int pidStart, final int vidStart, List<Variant> variants, String tableName, String[] formatNames) throws SQLException {
        if (variants.size() == 0) {
            return;
        }

        /*Connection connection = getBatchConnection();

        int currentPid = pidStart;
        int currentVid = vidStart;

        String sql = String.format("INSERT INTO %s VALUES ", tableName);
        connection.setAutoCommit(false);
        ArrayList<String> individualSqls = new ArrayList<>();

        for (Variant variant : variants) {
            ArrayList<String> singleVariantSqls = getSingleVariantSqls(variant, currentPid, currentVid);
            individualSqls.addAll(singleVariantSqls);
            currentPid += singleVariantSqls.size();
            currentVid++;
        }

        sql += String.join(", ", individualSqls);

        PreparedStatement create = connection.prepareStatement(sql);
        create.execute();
        create.close();

        connection.commit();
        */

        List<String> rowsToAdd = new LinkedList<>();

        int currentPid = pidStart;
        int currentVid = vidStart;

        for (Variant variant : variants) {
            ArrayList<String> singleVariantSqls = getSingleVariantCopySqls(variant, currentPid, currentVid);
            rowsToAdd.addAll(singleVariantSqls);
            currentPid += singleVariantSqls.size();
            currentVid++;
        }

        PgConnection copyOperationConnection = (PgConnection) connection;
        CopyManager copyManager = new CopyManager(copyOperationConnection);
        CopyIn copyIn = copyManager.copyIn(String.format("COPY %s FROM STDIN WITH DELIMITER '§'", tableName));

        try {
            for (String row : rowsToAdd) {
                byte[] bytes = row.getBytes();
                copyIn.writeToCopy(bytes, 0, bytes.length);
            }
            copyIn.endCopy();
        } finally {
            if (copyIn.isActive()) {
                copyIn.cancelCopy();
            }
        }
    }

    private ArrayList<String> getSingleVariantCopySqls(Variant variant, int pid, int vid) {
        ArrayList<String> result = new ArrayList<>();
        for (String csq : variant.getCSQs()) {
            result.add(getSingleVariantCopySql(variant, pid, vid, csq));
            pid++;
        }
        return result;
    }

    private String getSingleVariantCopySql(Variant variant, int pid, int vid, String csqString) {
        ArrayList<String> values = new ArrayList<>();

        values.add(pid + ""); //primary id
        values.add(vid + ""); //variant id
        values.add(convertToCopySqlString(variant.getChrom()));
        values.add(convertToCopySqlString(variant.getPos()));
        values.add(convertToCopySqlString(variant.getRef()));
        values.add(convertToCopySqlString(variant.getAlt()));
        values.add(convertToCopySqlString(variant.getQual()));
        values.add(convertToCopySqlString(variant.getFilter()));

        for (String infoKey : infoNames) {
            if (!infoKey.equals("CSQ")) {
                String value = variant.getInfoMap().get(infoKey);
                if (value == null || value.equals("")) {
                    values.add("NULL");
                } else {
                    values.add(convertToCopySqlString(value));
                }
            }
        }

        if (!csqString.equals("")) {
            String[] csq = csqString.split("\\|", -1);
            for (String s : csq) {
                String value = s;
                if (value.equals("")) {
                    value = null;
                }
                if (value == null) {
                    values.add("NULL");
                } else {
                    values.add(convertToCopySqlString(value));
                }
            }
        } else {
            for (int i = 0; i < csqNames.size(); i++) {
                values.add("NULL");
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
                    if (formatType.toLowerCase().equals("gt")) {
                        String cleanGt = formatKeyValueIndividual.get(formatType).replaceAll("\\|", "/");
                        cleanGt = cleanGt.replaceAll("[1-9]+[0-9]*/[1-9]+[0-9]*", "1/1");
                        cleanGt = cleanGt.replaceAll("[1-9]+[0-9]*/0", "1/0");
                        cleanGt = cleanGt.replaceAll("0/[1-9]+[0-9]*", "0/1");
                        values.add(convertToCopySqlString(cleanGt));
                    } else {
                        values.add(convertToCopySqlString(formatKeyValueIndividual.get(formatType)));
                    }
                } else {
                    values.add("NULL");
                }
            }
        }

        return "" + String.join("§", values) + "\n";
    }

    private String convertToCopySqlString(String value) {
        return value.replaceAll("§", "");
    }

    private ArrayList<String> getSingleVariantSqls(Variant variant, int pid, int vid) {
        ArrayList<String> result = new ArrayList<>();
        for (String csq : variant.getCSQs()) {
            result.add(getSingleVariantSql(variant, pid, vid, csq));
            pid++;
        }
        return result;
    }

    private String getSingleVariantSql(Variant variant, int pid, int vid, String csqString) {
        ArrayList<String> values = new ArrayList<>();

        values.add(pid + ""); //primary id
        values.add(vid + ""); //variant id
        values.add(convertToMySqlString(variant.getChrom()));
        values.add(convertToMySqlString(variant.getPos()));
        values.add(convertToMySqlString(variant.getRef()));
        values.add(convertToMySqlString(variant.getAlt()));
        values.add(convertToMySqlString(variant.getQual()));
        values.add(convertToMySqlString(variant.getFilter()));

        for (String infoKey : infoNames) {
            if (!infoKey.equals("CSQ")) {
                String value = variant.getInfoMap().get(infoKey);
                if (value == null || value.equals("")) {
                    values.add("NULL");
                } else {
                    values.add(convertToMySqlString(value));
                }
            }
        }

        if (!csqString.equals("")) {
            String[] csq = csqString.split("\\|", -1);
            for (String s : csq) {
                String value = s;
                if (value.equals("")) {
                    value = null;
                }
                if (value == null) {
                    values.add("NULL");
                } else {
                    values.add(convertToMySqlString(value));
                }
            }
        } else {
            for (int i = 0; i < csqNames.size(); i++) {
                values.add("NULL");
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
                    if (formatType.toLowerCase().equals("gt")) {
                        String cleanGt = formatKeyValueIndividual.get(formatType).replaceAll("\\|", "/");
                        cleanGt = cleanGt.replaceAll("[1-9]+[0-9]*/[1-9]+[0-9]*", "1/1");
                        cleanGt = cleanGt.replaceAll("[1-9]+[0-9]*/0", "1/0");
                        cleanGt = cleanGt.replaceAll("0/[1-9]+[0-9]*", "0/1");
                        values.add(convertToMySqlString(cleanGt));
                    } else {
                        values.add(convertToMySqlString(formatKeyValueIndividual.get(formatType)));
                    }
                } else {
                    values.add("NULL");
                }
            }
        }

        return "(" + String.join(", ", values) + ")";
    }

    public void makeIndices(String tableName, String[] formatNames) throws SQLException {
        connection.setAutoCommit(true);

        Set<String> colsToIndex = new HashSet<>();
        colsToIndex.addAll(Arrays.asList(columnsToIndex));

        final String tableUniqueIndexName = tableName.substring(0, Math.min(15, tableName.length()));

        String vidIndexName = tableUniqueIndexName + Math.abs((tableName).hashCode()) + randomString(15);
        String vidSql = String.format("CREATE INDEX %s on %s (vid)", vidIndexName, tableName);
        System.out.println(vidSql);
        executeStatement(vidSql);

        //create index for genotype
        for (String formatName : formatNames) {
            String col = "format_" + formatName + "_gt";
            String gtIndexName = tableUniqueIndexName + Math.abs((tableName).hashCode()) + randomString(15);
            String gtSql = String.format("CREATE INDEX %s on %s (%s)", gtIndexName, tableName, col);
            System.out.println(gtSql);
            executeStatement(gtSql);
        }

        //create index for impact
        String impactIndexName = tableUniqueIndexName + "imp" + Math.abs((tableName).hashCode()) + randomString(15);
        String impactSql = String.format("create index %s on %s (array_position(array[Cast('MODIFIER' AS VARCHAR),Cast('LOW' AS VARCHAR),Cast('MODERATE' AS VARCHAR),Cast('HIGH' AS VARCHAR)],info_csq_impact) nulls first);", impactIndexName, tableName);
        System.out.println(impactSql);
        executeStatement(impactSql);

        int count = 0;
        for (String col : colsToIndex) {
            String indexName = tableUniqueIndexName + count + Math.abs((tableName + col + count).hashCode()) + randomString(15);
            String sql = String.format("CREATE INDEX %s on %s (%s DESC NULLS LAST)", indexName, tableName, col);
            System.out.println(sql);
            executeStatement(sql);
            count++;
        }
    }

    private void executeStatement(String sql) {
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.execute();
            statement.close();
        } catch (Exception ex) {
            System.out.println("could not perform '" + sql + "'! Skipped with error: ");
            System.out.println(ex);
        }
    }

    private String convertToMySqlString(String value) {
        return "'" + value.replaceAll("\"", "").replaceAll("'", "").replaceAll("\\\\", "") + "'";
    }

    private String randomString(int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int r = random.nextInt(26+10);
            char next;
            if (r < 10) {
                next = (char) ('0' + r);
            } else {
                next = (char) ('a' + (r - 10));
            }
            result.append(next);
        }
        return result.toString();
    }

    public static String getSqlType(int level, int size) {
        if (level == 0) {
            return "BIGINT";
        } else if (level == 1) {
            return "double precision";
        } else {
            if (size < 256) {
                return String.format("VARCHAR(%s)", size);
            } else {
                return "TEXT";
            }
        }
    }

    private String getSqlTypeString(int length) {
        return getSqlType(2, length);
    }

    public static int computeSqlTypeLevel(String input) {
        if (input == null || input.equals("")) {
            return 0;
        }

        if (input.matches("[-+]?[0-9]+")) {
            return 0;
        } else if (input.matches("[-+]?([0-9]+[.])?[0-9]+([eE][-+]?[0-9]+)?")) {
            return 1;
        }
        return 2;
    }

}
