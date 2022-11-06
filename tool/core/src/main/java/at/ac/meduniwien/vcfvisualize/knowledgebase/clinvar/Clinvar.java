package at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;

@Service
public class Clinvar {

    private HashMap<String, List<GenomicPosition>> genomicPositionsByHpoTerm;
    private HashMap<String, List<GenomicPosition>> genomicPositionsByChromAndPos; //chrom:pos
    private HashMap<String, List<GenomicPosition>> genomicPositionsByChromAndPosAndAlt; //chrom:pos:alt
    private Set<String> likelyPathogenicAndPathogenicTerms; //contains all strings which indicate pathogenicity (e. g. fields like Pathogenic&_association&_protective)
    private boolean apiLoaded;

    public Clinvar() {
        genomicPositionsByHpoTerm = new HashMap<>();
        likelyPathogenicAndPathogenicTerms = new HashSet<>();
        genomicPositionsByChromAndPos = new HashMap<>();
        genomicPositionsByChromAndPosAndAlt = new HashMap<>();
        apiLoaded = false;
    }

    @SneakyThrows
    public void loadDataFromAPI() {
        System.out.println("loading Clinvar");
        String url = "https://ftp.ncbi.nlm.nih.gov/pub/clinvar/vcf_GRCh38/clinvar.vcf.gz";
        GZIPInputStream stream = new GZIPInputStream(new URL(url).openStream());
        String clinvar = new String(stream.readAllBytes());
        parseData(clinvar);
    }

    private void initiate() {
        if (!apiLoaded) {
            loadDataFromAPI();
        }
    }

    /**
     * Takes a number of HPO Terms and returns a list of chrom pos ref alt associated with this term
     * @param hpoTermIds the ids of the HPO-Term (e. g. HP:0000822)
     * @return a list of associated chrom pos ref alt
     */
    public List<GenomicPosition> getGenomicPositionsByHpoTerms(List<String> hpoTermIds) {
        List<GenomicPosition> genomicPositions = new LinkedList<>();
        for (String hpoTermId : hpoTermIds) {
            genomicPositions.addAll(getGenomicPositionsByHpoTerm(hpoTermId));
        }
        return genomicPositions;
    }

    public Set<String> getLikelyPathogenicAndPathogenicTerms() {
        initiate();
        return likelyPathogenicAndPathogenicTerms;
    }

    /**
     * Takes an HPO Term and returns a list of chrom pos ref alt associated with this term
     * @param hpoTermId the id of the HPO-Term (e. g. HP:0000822)
     * @return a list of associated chrom pos ref alt
     */
    public List<GenomicPosition> getGenomicPositionsByHpoTerm(String hpoTermId) {
        if (!genomicPositionsByHpoTerm.containsKey(hpoTermId)) {
            return new LinkedList<>();
        }
        return genomicPositionsByHpoTerm.get(hpoTermId);
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
                        likelyPathogenicAndPathogenicTerms.add(clnsig);
                        cnt++;

                        //make genomic position
                        String[] tabseperated = line.split("\t");
                        GenomicPosition genomicPosition = new GenomicPosition();
                        genomicPosition.setChrom(tabseperated[0]);
                        genomicPosition.setPos(Long.parseLong(tabseperated[1]));
                        genomicPosition.setRef(tabseperated[3]);
                        genomicPosition.setAlt(tabseperated[4]);

                        //map chrom:position to this
                        String chromPosKey = genomicPosition.getChrom() + ":" + genomicPosition.getPos();
                        if (!genomicPositionsByChromAndPos.containsKey(chromPosKey)) {
                            genomicPositionsByChromAndPos.put(chromPosKey, new LinkedList<>());
                        }
                        genomicPositionsByChromAndPos.get(chromPosKey).add(genomicPosition);

                        //map chrom:position:alt to this
                        String chromPosAltKey = genomicPosition.getChrom() + ":" + genomicPosition.getPos() + ":" + genomicPosition.getAlt();
                        if (!genomicPositionsByChromAndPosAndAlt.containsKey(chromPosAltKey)) {
                            genomicPositionsByChromAndPosAndAlt.put(chromPosAltKey, new LinkedList<>());
                        }
                        genomicPositionsByChromAndPosAndAlt.get(chromPosAltKey).add(genomicPosition);

                        //map hpo terms to this
                        String clndisdb = getBetween(line, "CLNDISDB=", ";");
                        List<String> hpoTerms = getAllBetween(clndisdb + ",", "Human_Phenotype_Ontology:", ",");

                        for (String hpoTerm : hpoTerms) {
                            if (!genomicPositionsByHpoTerm.containsKey(hpoTerm)) {
                                genomicPositionsByHpoTerm.put(hpoTerm, new LinkedList<>());
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
            sum += size;
        }

        System.out.println(sum);
        System.out.println(cnt + " Pathogenic or Likely_pathogenic variants found");
        System.out.println(genomicPositionsByHpoTerm.size() + " HPO terms are associated with genomic positions as Pathogenic or Likely_pathogenic");

        apiLoaded = true;
    }

    public List<GenomicPosition> findPathogenics(String chrom, String pos) {
        initiate();
        return genomicPositionsByChromAndPos.get(chrom + ":" + pos);
    }

    public List<GenomicPosition> findPathogenics(String chrom, String pos, String alt) {
        initiate();
        return genomicPositionsByChromAndPosAndAlt.get(chrom + ":" + pos + ":" + alt);
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
