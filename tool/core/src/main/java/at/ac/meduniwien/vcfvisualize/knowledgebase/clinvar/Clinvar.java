package at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Service
public class Clinvar {

    HashMap<String, List<GenomicPosition>> genomicPositionsByHpoTerm;

    public Clinvar() {
        genomicPositionsByHpoTerm = new HashMap<>();
    }

    @SneakyThrows
    public void loadDataFromAPI() {
        System.out.println("loading Clinvar");
        String url = "https://ftp.ncbi.nlm.nih.gov/pub/clinvar/vcf_GRCh38/clinvar.vcf.gz";
        GZIPInputStream stream = new GZIPInputStream(new URL(url).openStream());
        String clinvar = new String(stream.readAllBytes());
        parseData(clinvar);
    }

    private void parseData(String vcf) {
        String[] lines = vcf.split("\n");
        int cnt = 0;

        for (String line : lines) {
            if (!line.startsWith("#")) {
                String clnsig = getBetween(line, "CLNSIG=", ";");
                if (clnsig != null) {
                    HashSet<String> clnsigs = new HashSet<>();
                    String[] split = clnsig.split("[/|]");
                    for (String value : split) {
                        clnsigs.add(value.toLowerCase());
                    }

                    if (clnsigs.contains("Pathogenic".toLowerCase()) || clnsigs.contains("Likely_pathogenic".toLowerCase())) {
                        cnt++;
                        String clndisdb = getBetween(line, "CLNDISDB=", ";");
                        List<String> hpoTerms = getAllBetween(clndisdb + ",", "Human_Phenotype_Ontology:", ",");

                        for (String hpoTerm : hpoTerms) {
                            String[] tabseperated = line.split("\t");
                            GenomicPosition genomicPosition = new GenomicPosition();
                            genomicPosition.setChrom(tabseperated[0]);
                            genomicPosition.setPos(Long.parseLong(tabseperated[1]));
                            genomicPosition.setRef(tabseperated[3]);
                            genomicPosition.setAlt(tabseperated[4]);
                            if (!genomicPositionsByHpoTerm.containsKey(hpoTerm)) {
                                genomicPositionsByHpoTerm.put(hpoTerm, new LinkedList<GenomicPosition>());
                            }
                            genomicPositionsByHpoTerm.get(hpoTerm).add(genomicPosition);
                        }
                    }
                }
            }
        }

        int sum = 0;
        for (String key : genomicPositionsByHpoTerm.keySet()) {
            int size = genomicPositionsByHpoTerm.get(key).size();
            System.out.println(key + ": " + size);
            sum += size;
        }

        System.out.println(sum);
        System.out.println(cnt + " Pathogenic or Likely_pathogenic variants found");
        System.out.println(genomicPositionsByHpoTerm.size() + " HPO terms are associated with genomic positions as Pathogenic or Likely_pathogenic");
    }

    private List<String> getAllBetween(String text, String from, String to) {
        List<String> result = new LinkedList<>();

        int indexFrom = text.indexOf(from);
        while (indexFrom != -1) {
            result.add(getBetween(text, from, to));
            text = text.substring(indexFrom + from.length());
            indexFrom = text.indexOf(from);
        }

        return result;
    }

    private String getBetween(String text, String from, String to) {
        int indexFrom = text.indexOf(from);
        if (indexFrom == -1) {
            return null;
        }
        String a = text.substring(indexFrom + from.length());
        int indexTo = a.indexOf(to);
        if (indexTo == -1) {
            return null;
        }
        return a.substring(0, indexTo);
    }

}
