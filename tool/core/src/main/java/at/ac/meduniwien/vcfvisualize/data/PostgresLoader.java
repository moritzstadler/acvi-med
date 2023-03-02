package at.ac.meduniwien.vcfvisualize.data;

import at.ac.meduniwien.vcfvisualize.model.Column;
import at.ac.meduniwien.vcfvisualize.model.Filter;
import at.ac.meduniwien.vcfvisualize.model.Variant;
import at.ac.meduniwien.vcfvisualize.model.VariantIdentifier;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.sql.*;
import java.util.*;

@Service
public class PostgresLoader {

    public static final long GLOBAL_LIMIT = 200; //TODO only select columns to show in overview
    public static final int CONNECTION_POOL_SIZE = 30;

    @Autowired
    private Environment env;

    private String databaseUrl;
    private String databaseUser;
    private String databasePassword;

    private LinkedList<Connection> connectionPool;
    private HashSet<String> sampleIds;

    @SneakyThrows
    public PostgresLoader(@Value("${database.postgres.url}") String databaseUrl, @Value("${database.postgres.user}") String databaseUser, @Value("${database.postgres.password}") String databasePassword) {
        this.databaseUrl = databaseUrl;
        this.databaseUser = databaseUser;
        this.databasePassword = databasePassword;

        initConnectionPool();
    }

    private void initConnectionPool() {
        connectionPool = new LinkedList<>();
        for (int i = 0; i < CONNECTION_POOL_SIZE; i++) {
            connectionPool.add(openConnection());
        }
    }

    @SneakyThrows
    private Connection openConnection() {
        System.out.println("Opening connection to " + databaseUrl);
        return DriverManager.getConnection("jdbc:postgresql://" + databaseUrl, databaseUser, databasePassword);
    }

    @SneakyThrows
    private Connection getConnection() {
        Connection connection;
        if (connectionPool.isEmpty()) {
            connection = openConnection(); //pool's closed
        } else {
            synchronized (this) {
                connection = connectionPool.pollFirst();
            }
        }

        //check if connection still works
        if (connection == null || connection.isClosed()) {
            connection = openConnection();
        }

        return connection;
    }

    @SneakyThrows
    private void returnConnection(Connection connection) {
        if (connectionPool.size() < CONNECTION_POOL_SIZE && connection != null && !connection.isClosed()) {
            connectionPool.add(connection);
        }
    }

    @SneakyThrows
    protected List<Variant> getVariants(String sample, Filter filter, boolean useLimit) {
        Connection connection = getConnection();
        String sql = String.format("SELECT * FROM %s WHERE %s LIMIT %s OFFSET %s",
                sample,
                filter.toSqlString(),
                GLOBAL_LIMIT,
                filter.getOffset()
            );

        if (!useLimit) {
            sql = String.format("SELECT * FROM %s WHERE %s",
                    sample,
                    filter.toSqlString()
            );
        }

        System.out.println(sql);

        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();

        List<Variant> result = new LinkedList<>();
        while (resultSet.next()) {
            result.add(convertResultToVariant(resultSet));
        }

        resultSet.close();
        statement.close();
        returnConnection(connection);

        return result;
    }

    @SneakyThrows
    public long estimateAllVariantsCount(String sample) {
        Connection connection = getConnection();
        String sql = String.format("SELECT (reltuples / relpages * (pg_relation_size(oid) / 8192))::bigint as n FROM pg_class WHERE  oid = '%s'::regclass;",
                sample
        );

        PreparedStatement statement = connection.prepareStatement(sql);

        try {
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();

            long result = resultSet.getLong("n");
            resultSet.close();
            statement.close();
            returnConnection(connection);

            return result;
        } catch (Exception ex) {
            System.err.println(ex.toString());
            return 1;
        }
    }

    @SneakyThrows
    public long countVariantsToLimit(String sample, Filter filter, long limit) {
        Connection connection = getConnection();
        String sql = String.format("SELECT COUNT(*) as n FROM (SELECT 1 FROM %s WHERE %s LIMIT %s) as sub",
                sample,
                filter.toSqlString(),
                limit
        );

        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();

        long result = resultSet.getLong("n");

        resultSet.close();
        statement.close();
        returnConnection(connection);

        return result;
    }


    @SneakyThrows
    public Variant getVariant(String sample, VariantIdentifier variantIdentifier) {
        Connection connection = getConnection();
        String sql = String.format("SELECT * FROM %s WHERE pid = %s LIMIT 1",
                sample,
                variantIdentifier.getPid()
            );

        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();

        Variant variant = convertResultToVariant(resultSet);

        resultSet.close();
        statement.close();
        returnConnection(connection);

        return variant;
    }

    @SneakyThrows
    private Variant convertResultToVariant(ResultSet resultSet) {
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        int columnCount = resultSetMetaData.getColumnCount();

        Variant variant = new Variant();
        variant.setPid(resultSet.getLong("pid"));
        variant.setVid(resultSet.getLong("vid"));
        variant.setChrom(resultSet.getString("chrom"));
        variant.setPos(resultSet.getString("pos"));
        variant.setRef(resultSet.getString("ref"));
        variant.setAlt(resultSet.getString("alt"));
        variant.setQual(resultSet.getString("qual"));
        variant.setFilter(resultSet.getString("filter"));

        HashMap<String, String> info = new HashMap<>();
        for (int i = 8; i < columnCount; i++) {
            info.put(resultSetMetaData.getColumnName(i), resultSet.getString(i));
        }
        variant.setInfo(info);

        return variant;
    }


    @SneakyThrows
    public List<Variant> getIsoforms(String sample, long vid) {
        Connection connection = getConnection();
        String sql = String.format("SELECT pid, info_csq_feature FROM %s WHERE vid = %s",
                sample,
                vid
        );

        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();

        List<Variant> result = new LinkedList<>();
        while (resultSet.next()) {
            Variant variant = new Variant();
            variant.setPid(resultSet.getLong("pid"));
            HashMap<String, String> map = new HashMap<>();
            map.put("info_csq_feature", resultSet.getString("info_csq_feature"));
            variant.setInfo(map);
            result.add(variant);
        }

        resultSet.close();
        statement.close();
        returnConnection(connection);

        return result;
    }

    @SneakyThrows
    public List<String> getSampleIds() {
        Connection connection = getConnection();
        PreparedStatement showTables = connection.prepareStatement("SELECT * FROM pg_catalog.pg_tables WHERE schemaname != 'pg_catalog' AND schemaname != 'information_schema'");
        ResultSet resultSet = showTables.executeQuery();

        List<String> result = new LinkedList<>();
        while (resultSet.next()) {
            result.add(resultSet.getString("tablename"));
        }

        sampleIds = new HashSet<>();
        sampleIds.addAll(result);

        resultSet.close();
        showTables.close();
        returnConnection(connection);

        return result;
    }

    public boolean isValidSampleId(String sampleId) {
        return sampleIds.contains(sampleId);
    }

    @SneakyThrows
    public List<Column> getColumns(String sample) {
        Connection connection = getConnection();
        String sql = String.format("SELECT column_name, data_type FROM information_schema.columns WHERE table_name = '%s';", sample);
        PreparedStatement showColumns = connection.prepareStatement(sql);
        ResultSet resultSet = showColumns.executeQuery();

        List<Column> result = new LinkedList<>();
        while (resultSet.next()) {
            result.add(new Column(resultSet.getString("column_name"), resultSet.getString("data_type")));
        }

        resultSet.close();
        showColumns.close();
        returnConnection(connection);

        return result;
    }

    @SneakyThrows
    public List<String> getGroupedColumnValues(String sample, String column, int limit) {
        Connection connection = getConnection();
        String sql = String.format("select %s from %s group by %s limit %s;", column, sample, column, limit);
        PreparedStatement showColumns = connection.prepareStatement(sql);
        ResultSet resultSet = showColumns.executeQuery();

        List<String> result = new LinkedList<>();
        while (resultSet.next()) {
            result.add(resultSet.getString(column));
        }

        resultSet.close();
        showColumns.close();
        returnConnection(connection);

        return result;
    }

    @SneakyThrows
    public long countNonNullColumnValues(String sample, String column) {
        Connection connection = getConnection();
        String sql = String.format("select count(*) as n from %s where %s is not null;", sample, column);
        PreparedStatement showColumns = connection.prepareStatement(sql);
        ResultSet resultSet = showColumns.executeQuery();

        resultSet.next();
        long count = resultSet.getLong("n");

        resultSet.close();
        showColumns.close();
        returnConnection(connection);

        return count;
    }

    @SneakyThrows
    public void deleteSample(String name) {
        Connection connection = getConnection();
        String sql = String.format("DROP TABLE IF EXISTS %s", name);
        PreparedStatement dropTable = connection.prepareStatement(sql);
        dropTable.executeUpdate();
    }
}
