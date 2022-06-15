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
    private Connection[] batchConnections;
    private int batchConnectionPosition = 0;
    private ArrayList<String> csqNames;
    private ArrayList<String> infoNames;
    private ArrayList<String> formatTypes;
    private ArrayList<Integer> maxColSizes;
    private ArrayList<String> fullColList;
    private Random random;

    public SqlConnector() {
        random = new Random();
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

    public void createTable(String name, List<Info> header, Csq[] csqFields, String[] formatNames, ArrayList<String> formatTypes, ArrayList<Integer> maxColsSizes) throws SQLException {
        fullColList = new ArrayList<>();
        this.maxColSizes = maxColsSizes;

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
        String inner = cols.stream().collect(Collectors.joining(", "));
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

        Connection connection = getBatchConnection();

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

        sql += individualSqls.stream().collect(Collectors.joining(", "));

        PreparedStatement create = connection.prepareStatement(sql);
        create.execute();
        create.close();

        connection.commit();
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

        if (!csqString.equals("")) {
            String[] csq = csqString.split("\\|", -1);
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
                    values.add(convertToMySqlString(formatKeyValueIndividual.get(formatType)));
                } else {
                    values.add("NULL");
                }
            }
        }

        return "(" + values.stream().collect(Collectors.joining(", ")) + ")";
    }

    public void makeIndices(String tableName, String[] formatNames) throws SQLException {
        connection.setAutoCommit(true);

        Set<String> colsToIndex = new HashSet<>();
        /*for (int i = 0; i < maxColSizes.size(); i++) {
            if (maxColSizes.get(i) < 256) {
                colsToIndex.add(fullColList.get(i));
            }
        }*/
        colsToIndex.addAll(Arrays.asList(
                //info_AF_afr,... CADD, impact, max_af
                "info_gnomadg_af",
                "info_af_raw",
                "info_af_popmax",
                "info_csq_gnomadg_af",
                "info_csq_max_af",
                "info_caddind_raw",
                "info_caddind_phred",
                "info_cadd_raw",
                "info_cadd_phred",
                "info_af_raw",
                "info_controls_af_popmax",
                "info_af_afr",
                "info_af_amr",
                "info_af_asj",
                "info_af_eas",
                "info_af_nfe",
                "info_af_oth",

                "info_csq_polyphen",
                "info_csq_dann_score",
                "info_csq_sift",
                "info_csq_gerp_rs",
                "info_csq_fathmm_pred",
                "info_csq_primateai_pred",
                "info_csq_lrt_pred",
                "info_csq_metasvm_pred",

                "info_csq_af_afr",
                "info_csq_af_amr",
                "info_csq_af_asj",
                "info_csq_af_eas",
                "info_csq_af_nfe",
                "info_csq_af_nfe",
                "info_csq_af_oth",
                "info_csq_af",
                "info_csq_CADD_PHRED",
                "info_csq_CADD_RAW",

                "info_csq_feature_type",
                "info_csq_canonical",
                "info_csq_gnomad_af",
                "info_csq_gnomad_afr_af",
                "info_csq_gnomad_amr_af",
                "info_csq_gnomad_asj_af",
                "info_csq_gnomad_eas_af",
                "info_csq_gnomad_fin_af",
                "info_csq_gnomad_nfe_af",
                "info_csq_gnomad_oth_af",
                "info_csq_gnomad_sas_af",
                "info_csq_gnomadg_af",
                "info_csq_gnomadg_af_nfe",
                "info_csq_gnomadg_af_afr",
                "info_csq_gnomadg_af_amr",
                "info_csq_gnomadg_af_asj",
                "info_csq_gnomadg_af_eas",
                "info_csq_gnomadg_af_fin",
                "info_csq_gnomadg_af_oth",
                "info_csq_mutationtaster_pred",
                "info_csq_mvp_score",
                "info_csq_spliceai_pred_ds_ag",
                "info_csq_spliceai_pred_ds_al",
                "info_csq_spliceai_pred_ds_dg",
                "info_csq_spliceai_pred_ds_dl",
                "filter",
                "info_csq_symbol"));

        String vidIndexName = tableName.substring(0, Math.min(15, tableName.length())) + Math.abs((tableName).hashCode()) + randomString(15);
        String vidSql = String.format("CREATE INDEX %s on %s (vid)", vidIndexName, tableName);
        System.out.println(vidSql);
        executeStatement(vidSql);

        //create index for genotype
        for (String formatName : formatNames) {
            String col = "format_" + formatName + "_gt";
            String gtIndexName = tableName.substring(0, Math.min(15, tableName.length())) + Math.abs((tableName).hashCode()) + randomString(15);
            String gtSql = String.format("CREATE INDEX %s on %s (%s)", gtIndexName, tableName, col);
            System.out.println(gtSql);
            executeStatement(gtSql);
        }

        //create index for impact
        String impactIndexName = tableName.substring(0, Math.min(15, tableName.length())) + "imp" + Math.abs((tableName).hashCode()) + randomString(15);
        String impactSql = String.format("create index %s on %s (array_position(array[Cast('MODIFIER' AS VARCHAR),Cast('LOW' AS VARCHAR),Cast('MODERATE' AS VARCHAR),Cast('HIGH' AS VARCHAR)],info_csq_impact) nulls first);", impactIndexName, tableName);
        System.out.println(impactSql);
        executeStatement(impactSql);

        int count = 0;
        for (String col : colsToIndex) {
            String indexName = tableName.substring(0, Math.min(15, tableName.length())) + count + Math.abs((tableName + col + count).hashCode()) + randomString(15);
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

    /*public void insertVariant(Variant variant, String tableName) throws SQLException {
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
    }*/

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
