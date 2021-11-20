import java.util.ArrayList;
import java.util.HashMap;

public class Variant {

    private String chrom;
    private String pos;
    private String id;
    private String ref;
    private String alt;
    private String qual;
    private String filter;
    private String info;
    private String format;
    private ArrayList<String> formats;
    private HashMap<String, String> infoMap;

    public Variant() {
        formats = new ArrayList<>();
    }

    public String getChrom() {
        return chrom;
    }

    public void setChrom(String chrom) {
        this.chrom = chrom;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public String getQual() {
        return qual;
    }

    public void setQual(String qual) {
        this.qual = qual;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public HashMap<String, String> getInfoMap() {
        if (infoMap == null) {
            makeInfoMap();
        }

        return infoMap;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public ArrayList<String> getFormats() {
        return formats;
    }

    private void makeInfoMap() {
        HashMap<String, String> result = new HashMap<>();

        String[] pairs = info.split(";");

        for (String pair : pairs) {
            if (!pair.contains("=")) {
                continue;
            }

            String[] split = pair.split("=");
            String key = split[0];
            String value = split[1];
            if (key.equals("difficultregion")) {
                System.out.println("f");
            }
            result.put(key, value);
        }
        infoMap = result;
    }
}
