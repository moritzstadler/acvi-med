import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Importer {

    HashMap<String, Info> headerById;
    HashMap<Integer, Csq> csqByPosition;
    String tableName;

    List<Variant> batch;

    public Importer() {
        headerById = new HashMap<>();
        csqByPosition = new HashMap<>();
        batch = new LinkedList<>();
    }

    public int importFile(String name, String tableName, boolean determineFormat) throws IOException, SQLException {
        int lines = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(name))) {
            String line;
            while ((line = br.readLine()) != null) {
                processLine(line, determineFormat);
                if (batch.size() >= 100) {
                    SqlConnector.getInstance().insertVariantBatch(batch, tableName);
                    batch = new LinkedList<>();
                }

                lines++;

                if (lines % 1000 == 0) {
                    System.out.println("Processing line " + lines);
                }

                if (determineFormat && lines > 1000000) {
                    break;
                }
            }
        }

        //insert remaining batch
        if (!determineFormat) {
            SqlConnector.getInstance().insertVariantBatch(batch, tableName);
        }

        this.tableName = tableName;

        if (determineFormat) {
            List<Info> headers = headerById.keySet().stream().map(k -> headerById.get(k)).collect(Collectors.toList());
            List<Csq> csqs = csqByPosition.keySet().stream().map(k -> csqByPosition.get(k)).collect(Collectors.toList());
            SqlConnector.getInstance().createTable(tableName, headers, csqs);
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
            processHeader(getAfter(line, "##"));
        } else if (line.startsWith("#")) {

        } else {
            //normal lines go here
            processVariant(line, determineFormat);
        }
    }

    private void processHeader(String headerLine) {
        if (headerLine.startsWith("INFO")) {
            processInfoHeader(getAfter(headerLine, "INFO"));
        }
    }

    private void processInfoHeader(String infoHeader) {
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
            processCSQHeader(info);
        } else {
            headerById.put(info.getId(), info);
        }
    }

    private void processCSQHeader(Info csq) {
        String descriptionString = getBetweenMax(csq.getDescription(), "\"", "\"");
        String arrayDescription = getAfter(descriptionString, "Format: ");
        String[] csqArrayIds = arrayDescription.split("\\|");
        csqByPosition = new HashMap<>();
        for (int i = 0; i < csqArrayIds.length; i++) {
            csqByPosition.put(i, new Csq(csqArrayIds[i]));
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
                variant.getSamples().add(value);
            }
        }

        if (determineFormat) {
            String[] csqInputs = variant.getInfoMap().get("CSQ").split("\\|");
            for (int i = 0; i < csqInputs.length; i++) {
                if (csqByPosition.containsKey(i)) {
                    csqByPosition.get(i).matchType(csqInputs[i]);
                }
            }
        } else {
            //SqlConnector.getInstance().insertVariant(variant, tableName);
            batch.add(variant);
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
