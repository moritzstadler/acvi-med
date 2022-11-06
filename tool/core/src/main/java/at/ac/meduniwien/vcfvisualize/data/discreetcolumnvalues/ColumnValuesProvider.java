package at.ac.meduniwien.vcfvisualize.data.discreetcolumnvalues;

import at.ac.meduniwien.vcfvisualize.data.MySqlLoader;
import at.ac.meduniwien.vcfvisualize.data.PostgresLoader;
import at.ac.meduniwien.vcfvisualize.model.Column;
import at.ac.meduniwien.vcfvisualize.security.ConfigurationService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ColumnValuesProvider {

    public static final int DISCREET_COLUMN_LIMIT = 100; //if a column has more than this many distinct values, it is regarded as continuous.
    public static final String DISCREET_DATA_TYPE = "character varying"; //only columns with this datatype can even be discreet
    public static final String DISCREET_VALUES_METANAME = "VALUE";
    public static final String COUNT_METANAME = "COUNT";
    public static final String NOT_NULL_COUNT_METANAME = "COUNTNOTNULL";

    @Autowired
    PostgresLoader postgresLoader;

    @Autowired
    MySqlLoader mySqlLoader;

    private Map<String, Map<String, List<String>>> columnValuesBySample;
    private Map<String, Map<String, Long>> columnNonNullCountsBySample;

    public ColumnValuesProvider() {
        columnValuesBySample = new HashMap<>();
        columnNonNullCountsBySample = new HashMap<>();
    }

    /**
     * returns all possible values for a column or null above a certain number of possibilities
     * e. g. a column with values A, B, C will be returned, a column with values ranging
     * from 0 to 1 will be returned as null
     * @param sample the sample name
     * @param columnName the column name
     * @return the possible values of the column or null
     */
    public List<String> getColumnValues(String sample, String columnName) {
        Map<String, List<String>> sampleColumns = columnValuesBySample.get(sample);
        if (sampleColumns == null) {
            return null;
        }
        return sampleColumns.get(columnName);
    }

    /**
     * returns the number of rows where the given column name is not null
     * @param sample the id of the sample
     * @param columnName the name of the column
     * @return the number of non null entries
     */
    public long getNonNullColumnCount(String sample, String columnName) {
        Map<String, Long> sampleNonNullCounts = columnNonNullCountsBySample.get(sample);
        if (sampleNonNullCounts == null) {
            return 0;
        }
        Long result = sampleNonNullCounts.get(columnName);
        if (result == null) {
            return 0;
        }
        return result;
    }

    public void loadColumnValues() {
        List<String> sampleIds = postgresLoader.getSampleIds();

        for (String sampleId : sampleIds) {
            List<Column> allColumns = postgresLoader.getColumns(sampleId);

            //the values computed in this method are only needed for filterable columns
            String columnsMetaDataString = ConfigurationService.read("columns.json");
            JSONArray filterableIds = new JSONObject(columnsMetaDataString).getJSONArray("filterable");
            HashSet<String> filterableColumns = new HashSet<>();
            for (int i = 0; i < filterableIds.length(); i++) {
                filterableColumns.add(filterableIds.getString(i));
            }

            //after this columns only contains filterables
            List<Column> columns = new LinkedList<>();
            for (Column column : allColumns) {
                if (filterableColumns.contains(column.getName())) {
                    columns.add(column);
                }
            }

            //get row count
            long actualRowCount = postgresLoader.estimateAllVariantsCount(sampleId);
            long persistedRowCount = loadCountFromSampleMeta(sampleId);
            boolean numberOfRowsChanged = actualRowCount != persistedRowCount;//!rowsBySample.containsKey(sampleId) || rowsBySample.get(sampleId) != rowCountForSample;

            if (numberOfRowsChanged) {
                //find discreet values
                HashMap<String, List<String>> innerMap = new HashMap<>();
                for (Column column : columns) {
                    if (column.getDatatype().equals(DISCREET_DATA_TYPE)) {
                        String columnName = column.getName();
                        System.out.println("determining discreet values for " + sampleId + "." + columnName);
                        List<String> values = postgresLoader.getGroupedColumnValues(sampleId, columnName, DISCREET_COLUMN_LIMIT + 1);
                        if (values.size() <= DISCREET_COLUMN_LIMIT) {
                            innerMap.put(columnName, values);
                        }
                    }
                }
                System.out.println(innerMap.size() + " discreet columns for " + sampleId);
                columnValuesBySample.put(sampleId, innerMap);
                persistDiscreetValuesAsSampleMeta(sampleId, innerMap);

                //count non nulls
                HashMap<String, Long> columnsNonNullCountForCurrentSample = new HashMap<>();
                for (Column column : columns) {
                    System.out.println("counting non null values for " + sampleId + "." + column.getName());
                    long currentNonNullCount = postgresLoader.countNonNullColumnValues(sampleId, column.getName());
                    columnsNonNullCountForCurrentSample.put(column.getName(), currentNonNullCount);
                    persistNonNullCountAsSampleMeta(sampleId, column.getName(), currentNonNullCount);
                }
                columnNonNullCountsBySample.put(sampleId, columnsNonNullCountForCurrentSample);

                //store new number of rows after finishing persistence
                persistCountAsSampleMeta(sampleId, actualRowCount);
            } else {
                System.out.println("retrieving persisted samplemeta for " + sampleId);

                //load all for which discreet values are available
                List<String> discreetColumns = columns.stream().filter(c -> c.getDatatype().equals(DISCREET_DATA_TYPE)).map(Column::getName).collect(Collectors.toList());
                HashMap<String, List<String>> alreadyPersistedDiscreedValuesByColumn = loadDiscreetValuesFromSampleMeta(sampleId, discreetColumns);
                Set<String> alreadyPersistedDiscreetColumns = alreadyPersistedDiscreedValuesByColumn.keySet();

                //compare with list of necessary columns
                for (String discreetColumn : discreetColumns) {
                    if (!alreadyPersistedDiscreetColumns.contains(discreetColumn)) {
                        //persist missing column
                        System.out.println("determining discreet values for " + sampleId + "." + discreetColumn);
                        List<String> values = postgresLoader.getGroupedColumnValues(sampleId, discreetColumn, DISCREET_COLUMN_LIMIT + 1);
                        if (values.size() <= DISCREET_COLUMN_LIMIT) {
                            persistDiscreetValueAsSampleMeta(sampleId, discreetColumn, values);
                        }
                    }
                }

                //load all for which non-null counts are available
                HashMap<String, Long> alreadyPersistedNonNullCountsByColumn = loadNonNullCountFromSampleMeta(sampleId, columns.stream().map(Column::getName).collect(Collectors.toList()));

                //compare with list of necessary columns
                for (Column column : columns) {
                    if (!alreadyPersistedNonNullCountsByColumn.containsKey(column.getName())) {
                        System.out.println("counting non null values for " + sampleId + "." + column.getName());
                        long currentNonNullCount = postgresLoader.countNonNullColumnValues(sampleId, column.getName());
                        persistNonNullCountAsSampleMeta(sampleId, column.getName(), currentNonNullCount);
                    }
                }

                //load local state from persisted values
                columnValuesBySample.put(sampleId, loadDiscreetValuesFromSampleMeta(sampleId, columns.stream().filter(c -> c.getDatatype().equals(DISCREET_DATA_TYPE)).map(Column::getName).collect(Collectors.toList())));
                columnNonNullCountsBySample.put(sampleId, loadNonNullCountFromSampleMeta(sampleId, columns.stream().map(Column::getName).collect(Collectors.toList())));
            }
        }
    }

    private void persistDiscreetValuesAsSampleMeta(String sampleId, HashMap<String, List<String>> discreetValues) {
        for (String column : discreetValues.keySet()) {
            List<String> values = discreetValues.get(column);
            mySqlLoader.deleteSampleMeta(sampleId, column, DISCREET_VALUES_METANAME);
            for (String value : values) {
                mySqlLoader.insertSampleMeta(sampleId, column, DISCREET_VALUES_METANAME, value);
            }
        }
    }

    private void persistDiscreetValueAsSampleMeta(String sampleId, String column, List<String> discreetValues) {
        mySqlLoader.deleteSampleMeta(sampleId, column, DISCREET_VALUES_METANAME);
        for (String value : discreetValues) {
            mySqlLoader.insertSampleMeta(sampleId, column, DISCREET_VALUES_METANAME, value);
        }
    }

    private HashMap<String, List<String>> loadDiscreetValuesFromSampleMeta(String sampleId, List<String> columns) {
        HashMap<String, List<String>> result = new HashMap<>();
        for (String column : columns) {
            List<String> values = mySqlLoader.loadSampleMeta(sampleId, column, DISCREET_VALUES_METANAME);
            if (values != null && values.size() > 0) {
                result.put(column, values);
            }
        }
        return result;
    }

    private void persistCountAsSampleMeta(String sampleId, long rowCount) {
        mySqlLoader.deleteSampleMeta(sampleId, "", COUNT_METANAME);
        mySqlLoader.insertSampleMeta(sampleId, "", COUNT_METANAME, rowCount + "");
    }

    private long loadCountFromSampleMeta(String sampleId) {
        List<String> result = mySqlLoader.loadSampleMeta(sampleId, "", COUNT_METANAME);
        if (result.size() > 0) {
            String value = result.get(0);
            return Long.parseLong(value);
        }
        return 0;
    }

    private void persistNonNullCountAsSampleMeta(String sampleId, String column, long rowCount) {
        mySqlLoader.deleteSampleMeta(sampleId, column, NOT_NULL_COUNT_METANAME);
        mySqlLoader.insertSampleMeta(sampleId, column, NOT_NULL_COUNT_METANAME, rowCount + "");
    }

    private HashMap<String, Long> loadNonNullCountFromSampleMeta(String sampleId, List<String> columns) {
        HashMap<String, Long> result = new HashMap<>();
        for (String column : columns) {
            List<String> values = mySqlLoader.loadSampleMeta(sampleId, column, NOT_NULL_COUNT_METANAME);
            if (values.size() > 0) {
                String value = values.get(0);
                result.put(column, Long.parseLong(value));
            }
        }
        return result;
    }

}
