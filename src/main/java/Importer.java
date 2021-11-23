import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class Importer {

    HashMap<String, Info> headerById;
    Csq[] csqs;
    ArrayList<String> formatTypes;
    Set<String> formatTypesSet;
    String tableName;
    String[] formatNames;
    int pid;
    ArrayList<Integer> maxColSizes;

    List<Variant> batch;

    public Importer() {
        headerById = new HashMap<>();
        formatTypes = new ArrayList<>();
        formatTypesSet = new HashSet<>();
        batch = new LinkedList<>();
    }

    public int importFile(String name, String tableName, boolean determineFormat) throws IOException, SQLException {
        int lines = 0;
        pid = 1;
        try (BufferedReader br = new BufferedReader(new FileReader(name))) {
            String line;
            while ((line = br.readLine()) != null) {
                processLine(line, determineFormat);
                if (batch.size() >= 200) {
                    SqlConnector.getInstance().insertVariantBatch(pid - batch.size(), batch, tableName, formatNames);
                    batch = new LinkedList<>();
                }

                lines++;

                if (lines % 1000 == 0) {
                    System.out.println("Processing line " + lines);
                }
            }
        }

        //insert remaining batch
        if (!determineFormat) {
            SqlConnector.getInstance().insertVariantBatch(pid - batch.size(), batch, tableName, formatNames);
            System.out.println("Creating Indexes");
            SqlConnector.getInstance().makeIndices(tableName);
        }

        this.tableName = tableName;

        if (determineFormat) {
            List<Info> headers = headerById.keySet().stream().map(k -> headerById.get(k)).collect(Collectors.toList());
            SqlConnector.getInstance().createTable(tableName, headers, csqs, formatNames, formatTypes, maxColSizes);
        }

        /*for (Csq x : csqs) {
            System.out.println(x.getName());
        }

        for (String i : headerById.keySet()) {
            System.out.println(headerById.get(i).getId() + " " + headerById.get(i).getType());
        }*/

        return lines;
    }

    private void processLine(String line, boolean determineFormat) throws SQLException {
        if (line.startsWith("##")) {
            processHeader(getAfter(line, "##"), determineFormat);
        } else if (line.startsWith("#")) {
            processDescriptor(getAfter(line, "#"));
        } else {
            //normal lines go here
            processVariant(line, determineFormat);
        }
    }

    private void processHeader(String headerLine, boolean determineFormat) {
        if (headerLine.startsWith("INFO")) {
            processInfoHeader(getAfter(headerLine, "INFO"), determineFormat);
        }
    }

    private void processDescriptor(String descriptorLine) {
        String nameLine = getAfter(descriptorLine, "FORMAT\t");
        formatNames = nameLine.split("\t");
    }

    private void processInfoHeader(String infoHeader, boolean determineFormat) {
        String content = getBetweenMax(infoHeader, "<", ">");
        String[] pairs = content.split(",");

        Info info = new Info();

        for (String pair : pairs) {
            if (!pair.contains("=")) {
                continue;
            }

            String[] split = pair.split("=");
            String key = split[0];
            String value = split[1];

            if (key.equals("ID")) {
                info.setId(value);
            } else if (key.equals("Number")) {
                info.setNumber(value);
            } else if (key.equals("Type")) {
                info.setType(value);
            } else if (key.equals("Description")) {
                info.setDescription(value);
            }
        }

        if (info.getId().equals("CSQ")) {
            if (determineFormat) {
                processCSQHeader(info);
            }
        } else {
            headerById.put(info.getId(), info);
        }
    }

    private void processCSQHeader(Info csq) {
        String descriptionString = getBetweenMax(csq.getDescription(), "\"", "\"");
        String arrayDescription = getAfter(descriptionString, "Format: ");
        String[] csqArrayIds = arrayDescription.split("\\|");
        csqs = new Csq[csqArrayIds.length];
        for (int i = 0; i < csqArrayIds.length; i++) {
            csqs[i] = new Csq(csqArrayIds[i]);
        }
    }

    private void processVariant(String variantLine, boolean determineFormat) throws SQLException {
        String[] fields = variantLine.split("\t");

        Variant variant = new Variant();
        for (int i = 0; i < fields.length; i++) {
            String value = fields[i];
            if (i == 0) {
                variant.setChrom(value);
            } else if (i == 1) {
                variant.setPos(value);
            } else if (i == 2) {
                variant.setId(value);
            } else if (i == 3) {
                variant.setRef(value);
            } else if (i == 4) {
                variant.setAlt(value);
            } else if (i == 5) {
                variant.setQual(value);
            } else if (i == 6) {
                variant.setFilter(value);
            } else if (i == 7) {
                variant.setInfo(value);
            } else if (i == 8) {
                variant.setFormat(value);
            } else {
                variant.getFormats().add(value);
            }
        }

        if (determineFormat) {
            for (String headerId : variant.getInfoMap().keySet()) {
                if (!headerId.equals("CSQ")) {
                    headerById.get(headerId).matchType(variant.getInfoMap().get(headerId));
                }
            }

            if (variant.getInfoMap().containsKey("CSQ")) {
                String[] csqInputs = variant.getInfoMap().get("CSQ").split("\\|");
                for (int i = 0; i < csqInputs.length; i++) {
                    csqs[i].matchType(csqInputs[i]);
                }
            }

            String[] formatSplit = variant.getFormat().split(":", -1);
            for (String s : formatSplit) {
                if (!formatTypesSet.contains(s)) {
                    formatTypesSet.add(s);
                    formatTypes.add(s);
                }
            }

            determineMaxColSize(variant);
        } else {
            //SqlConnector.getInstance().insertVariant(variant, tableName);
            batch.add(variant);
            pid++;
        }
    }

    private void determineMaxColSize(Variant variant) {
        ArrayList<String> cols = new ArrayList<>();

        cols.add(variant.getChrom());
        cols.add(variant.getPos());
        cols.add(variant.getRef());
        cols.add(variant.getAlt());
        cols.add(variant.getQual());
        cols.add(variant.getFilter());

        for (String key : headerById.keySet()) {
            if (!key.equals("CSQ")) {
                cols.add(variant.getInfoMap().getOrDefault(key, ""));
            }
        }

        String[] csqCols = variant.getInfoMap().get("CSQ").split("\\|", -1);
        cols.addAll(Arrays.asList(csqCols));

        if (maxColSizes == null) {
            maxColSizes = new ArrayList<>();
            for (int i = 0; i < cols.size(); i++) {
                maxColSizes.add(0);
            }
        }

        for (int i = 0; i < cols.size(); i++) {
            maxColSizes.set(i, Math.max(maxColSizes.get(i), cols.get(i).length() + 1));
        }
    }

    private String getBetweenMin(String line, String start, String end) {
        return getBefore(getAfter(line, start), end);
    }

    private String getBetweenMax(String line, String start, String end) {
        return getBeforeLast(getAfter(line, start), end);
    }

    private String getAfter(String line, String delimiter) {
        return line.substring(line.indexOf(delimiter) + delimiter.length());
    }

    private String getBefore(String line, String delimiter) {
        return line.substring(0, line.indexOf(delimiter));
    }

    private String getBeforeLast(String line, String delimiter) {
        return line.substring(0, line.lastIndexOf(delimiter));
    }
}
