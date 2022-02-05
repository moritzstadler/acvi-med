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
    int vid;
    ArrayList<Integer> maxColSizes;

    List<Variant> batch;

    int positionOfConsequenceInCSQ;
    int positionOfBiotypeInCSQ;
    HashSet<Integer> positionOfSpecialCSQFields;
    HashSet<Integer> positionOfVerboseCSQFields;
    int positionOfGenotype;

    public Importer() {
        headerById = new HashMap<>();
        formatTypes = new ArrayList<>();
        formatTypesSet = new HashSet<>();
        batch = new LinkedList<>();
        positionOfSpecialCSQFields = new HashSet<>();
        positionOfVerboseCSQFields = new HashSet<>();
    }

    public int importFile(String name, String tableName, boolean determineFormat) throws IOException, SQLException {
        int lines = 0;
        pid = 1;
        vid = 1;
        try (BufferedReader br = new BufferedReader(new FileReader(name))) {
            String line;
            while ((line = br.readLine()) != null) {
                processLine(line, determineFormat);
                int batchSize = batch.stream().map(b -> b.getCSQs().length).reduce(0, Integer::sum);
                if (batchSize >= 50) {
                    SqlConnector.getInstance().insertVariantBatch(pid - batchSize, vid - batch.size(), batch, tableName, formatNames);
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
            int batchSize = batch.stream().map(b -> b.getCSQs().length).reduce(0, Integer::sum);
            SqlConnector.getInstance().insertVariantBatch(pid - batchSize, vid - batch.size(), batch, tableName, formatNames);
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
        for (int i = 0; i < formatNames.length; i++) {
            formatNames[i] = formatNames[i].replaceAll("-", "_").replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
            if (formatNames[i].endsWith("_gt")) {
                positionOfGenotype = i;
            }
        }
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

            if (csqArrayIds[i].toLowerCase().equals("consequence")) {
                positionOfConsequenceInCSQ = i;
            } else if (csqArrayIds[i].toLowerCase().equals("biotype")) {
                positionOfBiotypeInCSQ = i;
            } else if (Config.specialCsqFields.contains(csqArrayIds[i].toLowerCase())) {
                positionOfSpecialCSQFields.add(i);
            }
            if (Config.verbsoseCsqFields.contains(csqArrayIds[i].toLowerCase())) {
                positionOfVerboseCSQFields.add(i);
            }
        }
        System.out.println("pos biotype" + positionOfBiotypeInCSQ);
        System.out.println("pos cosequence " + positionOfConsequenceInCSQ);
        System.out.println("found specialfields " + positionOfSpecialCSQFields.size());
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

            for (String csq : variant.getCSQs()) {
                if (!csq.equals("")) {
                    String[] csqInputs = csq.split("\\|", -1);
                    for (int i = 0; i < csqInputs.length; i++) {
                        String inputToMatch = csqInputs[i];

                        if (Config.specialCsqFields.contains(csqs[i].getName().toLowerCase())) {
                            if (csqInputs[positionOfConsequenceInCSQ].equals("missense_variant") && csqInputs[positionOfBiotypeInCSQ].equals("protein_coding")) {
                                String[] ampersandSplit = inputToMatch.split("&");

                                for (String ampersandItem : ampersandSplit) {
                                    if (!ampersandItem.equals(".")) {
                                        csqs[i].matchType(ampersandItem);
                                    }
                                }
                            }
                        } else {
                            csqs[i].matchType(inputToMatch);
                        }
                    }
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
            alterCSQFields(variant);
            variant.getFormats().set(positionOfGenotype, variant.getFormats().get(positionOfGenotype).replaceAll("\\|", "/"));
            batch.add(variant);
            pid += variant.getCSQs().length;
            vid++;
        }
    }

    private void alterCSQFields(Variant variant) {
        List<String> alteredCsqs = new LinkedList<>();

        int rightVariantCount = 0;
        for (String csq : variant.getCSQs()) {
            if (!csq.equals("")) {
                String[] csqInputs = csq.split("\\|", -1);
                boolean rightVariant = false;
                for (int position : positionOfSpecialCSQFields) {
                    String inputToMatch = csqInputs[position];
                    if (csqInputs[positionOfConsequenceInCSQ].equals("missense_variant") && csqInputs[positionOfBiotypeInCSQ].equals("protein_coding")) {
                        String[] ampersandSplit = inputToMatch.split("&");

                        String singleAmpersandValue = "";
                        if (rightVariantCount < ampersandSplit.length) {
                            singleAmpersandValue = ampersandSplit[rightVariantCount];
                            if (singleAmpersandValue.equals(".")) {
                                singleAmpersandValue = "";
                            }

                            csqInputs[position] = singleAmpersandValue;
                        } else {
                            System.out.println("Could not fully parse " + variant.getChrom() + " " + variant.getPos());
                            csqInputs[position] = "";
                        }

                        rightVariant = true;
                    } else {
                        csqInputs[position] = "";
                    }
                }
                if (rightVariant) {
                    rightVariantCount++;
                }

                for (int verbosePosition : positionOfVerboseCSQFields) {
                    csqInputs[verbosePosition] = csqInputs[verbosePosition].replaceAll("[^A-Za-z]", "").toLowerCase();
                }

                alteredCsqs.add(Arrays.stream(csqInputs).collect(Collectors.joining("|")));
            }
        }

        variant.getInfoMap().put("CSQ", String.join(",", alteredCsqs));
    }

    private void determineMaxColSize(Variant variant) {
        for (String csq : variant.getCSQs()) {
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

            if (!csq.equals("")) {
                String[] csqCols = csq.split("\\|", -1);
                cols.addAll(Arrays.asList(csqCols));
            }

            if (maxColSizes == null) {
                maxColSizes = new ArrayList<>();
                for (int i = 0; i < cols.size(); i++) {
                    maxColSizes.add(0);
                }
            }

            for (int i = 0; i < cols.size(); i++) {
                maxColSizes.set(i, Math.max(maxColSizes.get(i), cols.get(i).length() + 1)); //TODO: THIS IS WRONG FOR CSQ SPECIAL FIELDS
            }
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
