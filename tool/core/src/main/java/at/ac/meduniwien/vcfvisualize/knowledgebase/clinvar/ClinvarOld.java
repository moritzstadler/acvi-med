package at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Service
public class ClinvarOld {

    private HashMap<String, List<GenomicPosition>> genomicPositionsByHpoTerm;
    private HashMap<String, List<GenomicPosition>> genomicPositionsByChromAndPos; //chrom:pos (e. g. "1:1234")
    private HashMap<String, List<GenomicPosition>> genomicPositionsByChromAndPosAndAlt; //chrom:pos:alt
    private HashMap<String, List<GenomicPosition>> genomicPositionsByHgvsp; //e. g. ENSP00000343864.2:p.Val663Ala
    private HashMap<String, List<GenomicPosition>> genomicPositionsByHalfHgvsp; //e. g. ENSP00000343864.2:p.Val663
    private Set<String> likelyPathogenicAndPathogenicTerms; //contains all strings which indicate pathogenicity (e. g. fields like Pathogenic&_association&_protective)
    private boolean dataLoaded;

    public ClinvarOld() {
        genomicPositionsByHpoTerm = new HashMap<>();
        likelyPathogenicAndPathogenicTerms = new HashSet<>();
        genomicPositionsByChromAndPos = new HashMap<>();
        genomicPositionsByChromAndPosAndAlt = new HashMap<>();
        genomicPositionsByHgvsp = new HashMap<>();
        genomicPositionsByHalfHgvsp = new HashMap<>();
        dataLoaded = false;
    }


    private void initiate() {
        if (!dataLoaded) {
            loadDataFromFile();
        }
    }

    /**
     * Annotate the clinvar.vcf.gz file with the following commands
     *
     * sudo docker run --rm -it -v /data/vep:/opt/vep/.vep ensemblorg/ensembl-vep ./vep -i /opt/vep/.vep/clinvar.vcf --dir_cache /opt/vep/.vep --cache --offline --format vcf --warning_file /opt/vep/.vep/warnings --o /opt/vep/.vep/clinvar_annotated.vcf --force_overwrite --vcf -hgvsg --shift_hgvs 1 --domains --hgvs --fork 4
     * bgzip -c clinvar_annotated.vcf > clinvar_annotated.vcf.gz
     * tabix -p vcf clinvar_annotated.vcf.gz
     * bcftools +split-vep.so -f '%CHROM\t%POS\t%REF\t%ALT\t%INFO/CLNSIG\t%INFO/CLNDISDB,\t%HGVSp\n' clinvar_annotated.vcf.gz > clinvar_annotated_reduced.vcf
     * grep -E 'Likely_pathogenic|Pathogenic' clinvar_annotated_reduced.vcf > smartclinvar.vcf
     */
    @SneakyThrows
    public void loadDataFromFile() {
        Path varPath = new File("src/main/resources/smartclinvar.vcf").toPath();
        if (Files.exists(varPath)) {
            parseFileData(Files.readString(varPath));
        } else {
            System.err.println("Could not locate src/main/resources/smartclinvar.vcf!");
        }
    }

    /**
     * Parses clinvar data - the file must adhere to the following:
     *  - Only contain lines where CLNSIG is Likely_pathogenic and Pathogenic
     *  - Each line is of the format %CHROM\t%POS\t%REF\t%ALT\t%INFO/CLNSIG\t%INFO/CLNDISDB,\t%HGVSp\n
     *  - No header is included
     *
     *  A sample line of this file is: "1	976215	A	G	Pathogenic	Human_Phenotype_Ontology:HP:0001932,Human_Phenotype_Ontology:HP:0008264,MedGen:C4021547|Human_Phenotype_Ontology:HP:0032647,MedGen:C5397664,	ENSP00000343864.2:p.Val663Ala,.,.,.,ENSP00000414022.3:p.Val777Ala,.,.,.,ENSP00000511592.1:p.Val777Ala"
     *
     * @param vcf a string containing the file contents
     */
    @SneakyThrows
    private void parseFileData(String vcf) {
        String[] lines = vcf.split("\n");
        for (String line : lines) {
            if (!line.startsWith("#")) {
                //System.out.println(line);
                String[] tabseperated = line.split("\t");

                //make genomic position
                String chrom = tabseperated[0];
                long pos = Long.parseLong(tabseperated[1]);
                String ref = tabseperated[2];
                String alt = tabseperated[3];
                String clnsig = tabseperated[4];
                String clndisdb = tabseperated[5];
                String hgvsp = tabseperated[6];

                GenomicPosition genomicPosition = new GenomicPosition(chrom, pos, ref, alt);
                genomicPosition.setChrom(chrom);
                genomicPosition.setPos(pos);
                genomicPosition.setRef(ref);
                genomicPosition.setAlt(alt);

                likelyPathogenicAndPathogenicTerms.add(clnsig); //since the file was annotated like the imported vcf, the value is equal

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

                //TODO test this!
                //map hpo terms to this
                List<String> hpoTerms = getAllBetween(clndisdb + ",", "Human_Phenotype_Ontology:", ","); //since the file is formated like this: %CHROM\t%POS\t%REF\t%ALT\t%INFO/CLNSIG\t%INFO/CLNDISDB,\t%HGVSp\n there is always a trailing ',' after CLNDISDB
                for (String hpoTerm : hpoTerms) {
                    if (!genomicPositionsByHpoTerm.containsKey(hpoTerm)) {
                        genomicPositionsByHpoTerm.put(hpoTerm, new LinkedList<>());
                    }
                    genomicPositionsByHpoTerm.get(hpoTerm).add(genomicPosition);
                }

                //map HGVSp ids to this
                List<String> hgvsps = Arrays.stream(hgvsp.split(",")).filter(x -> !x.equals(".")).collect(Collectors.toList());
                for (String rawHgvspId : hgvsps) {
                    String hgvspId = rawHgvspId.replaceAll("%3D", "=");
                    if (!genomicPositionsByHgvsp.containsKey(hgvspId)) {
                        genomicPositionsByHgvsp.put(hgvspId, new LinkedList<>());
                    }
                    genomicPositionsByHgvsp.get(hgvspId).add(genomicPosition);

                    String halfHgvspId = getHalfHgvsP(hgvspId);
                    if (!genomicPositionsByHalfHgvsp.containsKey(halfHgvspId)) {
                        genomicPositionsByHalfHgvsp.put(halfHgvspId, new LinkedList<>());
                    }
                    genomicPositionsByHalfHgvsp.get(halfHgvspId).add(genomicPosition);
                }

            }
        }

        int sum = 0;
        for (String key : genomicPositionsByHpoTerm.keySet()) {
            int size = genomicPositionsByHpoTerm.get(key).size();
            sum += size;
        }

        System.out.println(genomicPositionsByHgvsp.size() + " Pathogenic or Likely_pathogenic protein changes imported");
        System.out.println(sum + " genomic positions are associated with HPO terms");
        System.out.println(lines.length + " Pathogenic or Likely_pathogenic variants imported");
        System.out.println(genomicPositionsByHpoTerm.size() + " HPO terms are associated with genomic positions as Pathogenic or Likely_pathogenic");

        dataLoaded = true;
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

    @SneakyThrows
    @Deprecated
    public void loadDataFromAPI() {
        System.out.println("loading Clinvar");
        String url = "https://ftp.ncbi.nlm.nih.gov/pub/clinvar/vcf_GRCh38/clinvar.vcf.gz";
        GZIPInputStream stream = new GZIPInputStream(new URL(url).openStream());
        //TODO the stream does not load everything
        parseAPIData(stream);
    }

    @SneakyThrows
    @Deprecated
    private void parseAPIData(GZIPInputStream gzipStream) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gzipStream));

        int lines = 0;
        int cnt = 0;
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            lines++;
            if (!line.startsWith("#")) {
                //System.out.println(line);
                String clnsig = getBetween(line, "CLNSIG=", ";");
                if (clnsig != null) {
                    HashSet<String> clnsigs = new HashSet<>();
                    String[] split = clnsig.split("[/|]");
                    for (String value : split) {
                        clnsigs.add(value.toLowerCase());
                    }

                    if (clnsigs.contains("Pathogenic".toLowerCase()) || clnsigs.contains("Likely_pathogenic".toLowerCase())) {
                        likelyPathogenicAndPathogenicTerms.add(clnsig.replaceAll("[|]", "&")); //in the imported file the values are connected via & instead of |
                        cnt++;

                        //make genomic position
                        String[] tabseperated = line.split("\t");
                        GenomicPosition genomicPosition = new GenomicPosition();
                        genomicPosition.setChrom(tabseperated[0]);
                        genomicPosition.setPos(Long.parseLong(tabseperated[1]));
                        genomicPosition.setRef(tabseperated[3]);
                        genomicPosition.setAlt(tabseperated[4]);

                        System.out.println(tabseperated[0] + " " + line);
                        if (tabseperated[0].equals("9")) {
                            if (tabseperated[1].startsWith("3")) {
                                System.out.println("here");
                            }
                        }

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
                        List<String> hpoTerms = getAllBetween(clndisdb + ",", "Human_Phenotype_Ontology:", ","); //TODO what about the last one?

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

        dataLoaded = true;
    }

    public List<GenomicPosition> findPathogenics(String chrom, String pos) {
        initiate();
        return genomicPositionsByChromAndPos.get(chrom + ":" + pos);
    }

    public List<GenomicPosition> findPathogenics(String chrom, String pos, String alt) {
        initiate();
        return genomicPositionsByChromAndPosAndAlt.get(chrom + ":" + pos + ":" + alt);
    }

    public List<GenomicPosition> findPathogenicsByHgvsP(String hgvsp) {
        initiate();
        return genomicPositionsByHgvsp.get(hgvsp);
    }

    public int countPathogenicOrLikelyPathogenicNullVariants(String gene) {
        //TODO consider that gene = DOCK8 and in file it might be DOCK8:1234 or something similar
        //TODO consider stuff like DOCK8:81704|DOCK8-AS1:157983
        return -1;
    }

    public boolean nonPathogenicEntryExistsForPosition(String chrom, int pos) {
        //TODO implement this
        return false;
    }

    public int countPathogenicOrLikelyPathogenicMissenseMutationsForGene(String gene) {
        //TODO consider stuff like DOCK8:81704|DOCK8-AS1:157983
        //TODO implement this
        return -1;
    }

    public int countBenignMissenseMutationsForGene(String gene) {
        //TODO consider stuff like DOCK8:81704|DOCK8-AS1:157983
        //TODO implement this
        return -1;
    }

    public int countPathogenicOrLikelyPathogenicNonNullVariants(String gene) {
        //TODO consider stuff like DOCK8:81704|DOCK8-AS1:157983
        //TODO implement this
        return -1;
    }

    /**
     * Upon inputting ENSP00000343864.2:p.Val663Ala values for ENSP00000343864.2:p.Val663Arg, ENSP00000343864.2:p.Val663Met etc. are returned.
     * Only the reference amino acid is relevant.
     *
     * @param hgvsp full hgvsp id
     * @return list of genomic positions
     */
    public List<GenomicPosition> findPathogenicsByHgvsPNonMatchAlt(String hgvsp) {
        initiate();
        return genomicPositionsByHalfHgvsp.get(getHalfHgvsP(hgvsp));
    }

    private String getHalfHgvsP(String hgvspId) {
        return hgvspId.replaceAll("[A-Za-z]+$", "");
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
