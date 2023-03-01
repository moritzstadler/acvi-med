package at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class Clinvar {

    private HashMap<String, List<GenomicPosition>> genomicPositionsByHpoTerm;

    private HashMap<String, List<GenomicPosition>> pathogenicOrLikelyPathogenicGenomicPositionsByChromAndPos; //chrom:pos (e. g. "1:1234")
    private HashMap<String, List<GenomicPosition>> pathogenicOrLikelyPathogenicGenomicPositionsByChromAndPosAndAlt; //chrom:pos:alt

    private HashMap<String, List<GenomicPosition>> benignGenomicPositionsByChromAndPos; //chrom:pos
    private HashMap<String, List<GenomicPosition>> benignGenomicPositionsByChromAndPosAndAlt; //chrom:pos:alt

    private HashMap<String, List<GenomicPosition>> pathogenicOrLikelyPathogenicGenomicPositionsByHgvsp; //e. g. ENSP00000343864.2:p.Val663Ala
    private HashMap<String, List<GenomicPosition>> pathogenicOrLikelyPathogenicGenomicPositionsByHalfHgvsp; //e. g. ENSP00000343864.2:p.Val663

    private HashMap<String, Integer> pathogenicLikelyPathogenicMissenseVariantsByGene;
    private HashMap<String, Integer> benignMissenseVariantsByGene;
    private HashMap<String, Integer> pathogenicLikelyPathogenicNullVariantsByGene;
    private HashMap<String, Integer> pathogenicLikelyPathogenicNonNullVariantsByGene;

    private boolean dataLoaded;

    public Clinvar() {
        genomicPositionsByHpoTerm = new HashMap<>();

        pathogenicOrLikelyPathogenicGenomicPositionsByChromAndPos = new HashMap<>();
        pathogenicOrLikelyPathogenicGenomicPositionsByChromAndPosAndAlt = new HashMap<>();

        benignGenomicPositionsByChromAndPos = new HashMap<>();
        benignGenomicPositionsByChromAndPosAndAlt = new HashMap<>();

        pathogenicOrLikelyPathogenicGenomicPositionsByHgvsp = new HashMap<>();
        pathogenicOrLikelyPathogenicGenomicPositionsByHalfHgvsp = new HashMap<>();

        pathogenicLikelyPathogenicMissenseVariantsByGene = new HashMap<>();
        benignMissenseVariantsByGene = new HashMap<>();
        pathogenicLikelyPathogenicNullVariantsByGene = new HashMap<>();
        pathogenicLikelyPathogenicNonNullVariantsByGene = new HashMap<>();

        dataLoaded = false;
    }


    public void initiate() {
        if (!dataLoaded) {
            loadClndisdbFromFile();
            loadClnsigFromFile();
            loadHgvspPathogenicOrLikelyPathogenicFromFile();
            loadGeneInfoFromFile();
            dataLoaded = true;
        }
    }

    /**
     * sample: 1	5867833	G	A	Human_Phenotype_Ontology:HP:0000090,Human_Phenotype_Ontology:HP:0004748,MONDO:MONDO:0019005,MedGen:C0687120,OMIM:PS256100,Orphanet:ORPHA655,SNOMED_CT:204958008,
     */
    @SneakyThrows
    public void loadClndisdbFromFile() {
        Path varPath = new File("src/main/resources/clinvar/cv_clndisdb.txt").toPath();
        if (Files.exists(varPath)) {
            String input = Files.readString(varPath);
            String[] lines = input.split("\n");
            for (String line : lines) {
                if (line.length() < 1) {
                    continue;
                }

                String[] tabSeperated = line.split("\t");

                String chrom = tabSeperated[0];
                long pos = Long.parseLong(tabSeperated[1]);
                String ref = tabSeperated[2];
                String alt = tabSeperated[3];
                String clndisdb = tabSeperated[4];

                GenomicPosition genomicPosition = new GenomicPosition(chrom, pos, ref, alt);

                List<String> hpoTerms = getAllBetween(clndisdb + ",", "Human_Phenotype_Ontology:", ","); //since the file is formatted like this: %CHROM\t%POS\t%REF\t%ALT\t%INFO/CLNSIG\t%INFO/CLNDISDB,\n there is always a trailing ',' after CLNDISDB
                for (String hpoTerm : hpoTerms) {
                    if (!genomicPositionsByHpoTerm.containsKey(hpoTerm)) {
                        genomicPositionsByHpoTerm.put(hpoTerm, new LinkedList<>());
                    }
                    genomicPositionsByHpoTerm.get(hpoTerm).add(genomicPosition);
                }
            }
        } else {
            System.err.println("Could not locate src/main/resources/clinvar/cv_clndisdb.txt");
        }
    }

    /**
     * samples:
     * 1	145927447	C	T	Pathogenic/Likely_pathogenic/Pathogenic,_low_penetrance|other
     * 1	145926837	C	T	Likely_benign
     */
    @SneakyThrows
    public void loadClnsigFromFile() {
        Path varPath = new File("src/main/resources/clinvar/cv_clnsig.txt").toPath();
        if (Files.exists(varPath)) {
            String input = Files.readString(varPath);
            String[] lines = input.split("\n");
            for (String line : lines) {
                if (line.length() < 1) {
                    continue;
                }

                String[] tabSeperated = line.split("\t");

                String chrom = tabSeperated[0];
                long pos = Long.parseLong(tabSeperated[1]);
                String ref = tabSeperated[2];
                String alt = tabSeperated[3];
                String clnsigLowerCase = tabSeperated[4].toLowerCase();

                GenomicPosition genomicPosition = new GenomicPosition(chrom, pos, ref, alt);

                if (clnsigLowerCase.contains("pathogenic") || clnsigLowerCase.contains("likely_pathogenic")) {
                    //map chrom:pos to this
                    String chromPosKey = genomicPosition.getChrom() + ":" + genomicPosition.getPos();
                    if (!pathogenicOrLikelyPathogenicGenomicPositionsByChromAndPos.containsKey(chromPosKey)) {
                        pathogenicOrLikelyPathogenicGenomicPositionsByChromAndPos.put(chromPosKey, new LinkedList<>());
                    }
                    pathogenicOrLikelyPathogenicGenomicPositionsByChromAndPos.get(chromPosKey).add(genomicPosition);

                    //map chrom:pos:alt to this
                    String chromPosAltKey = genomicPosition.getChrom() + ":" + genomicPosition.getPos() + ":" + genomicPosition.getAlt();
                    if (!pathogenicOrLikelyPathogenicGenomicPositionsByChromAndPosAndAlt.containsKey(chromPosAltKey)) {
                        pathogenicOrLikelyPathogenicGenomicPositionsByChromAndPosAndAlt.put(chromPosAltKey, new LinkedList<>());
                    }
                    pathogenicOrLikelyPathogenicGenomicPositionsByChromAndPosAndAlt.get(chromPosAltKey).add(genomicPosition);
                } else {
                    String chromPosKey = genomicPosition.getChrom() + ":" + genomicPosition.getPos();
                    if (!benignGenomicPositionsByChromAndPos.containsKey(chromPosKey)) {
                        benignGenomicPositionsByChromAndPos.put(chromPosKey, new LinkedList<>());
                    }
                    benignGenomicPositionsByChromAndPos.get(chromPosKey).add(genomicPosition);

                    //map chrom:pos:alt to this
                    String chromPosAltKey = genomicPosition.getChrom() + ":" + genomicPosition.getPos() + ":" + genomicPosition.getAlt();
                    if (!benignGenomicPositionsByChromAndPosAndAlt.containsKey(chromPosAltKey)) {
                        benignGenomicPositionsByChromAndPosAndAlt.put(chromPosAltKey, new LinkedList<>());
                    }
                    benignGenomicPositionsByChromAndPosAndAlt.get(chromPosAltKey).add(genomicPosition);
                }
            }
        } else {
            System.err.println("Could not locate src/main/resources/clinvar/cv_clnsig.txt");
        }
    }

    /**
     * samples:
     * 1	976215	A	G	Pathogenic	ENSP00000343864.2:p.Val663Ala,.,.,.,ENSP00000414022.3:p.Val777Ala,.,.,.,ENSP00000511592.1:p.Val777Ala
     */
    @SneakyThrows
    public void loadHgvspPathogenicOrLikelyPathogenicFromFile() {
        Path varPath = new File("src/main/resources/clinvar/cv_hgvsp_path.txt").toPath();
        if (Files.exists(varPath)) {
            String input = Files.readString(varPath);
            String[] lines = input.split("\n");
            for (String line : lines) {
                if (line.length() < 1) {
                    continue;
                }

                String[] tabSeperated = line.split("\t");

                String chrom = tabSeperated[0];
                long pos = Long.parseLong(tabSeperated[1]);
                String ref = tabSeperated[2];
                String alt = tabSeperated[3];
                String hgvspCommaSeparated = tabSeperated[5].toLowerCase();

                GenomicPosition genomicPosition = new GenomicPosition(chrom, pos, ref, alt);

                List<String> hgvsps = Arrays.stream(hgvspCommaSeparated.split(",")).filter(x -> !x.equals(".")).collect(Collectors.toList());
                for (String rawHgvspId : hgvsps) {
                    String hgvspId = rawHgvspId.replaceAll("%3D", "=");
                    if (!pathogenicOrLikelyPathogenicGenomicPositionsByHgvsp.containsKey(hgvspId)) {
                        pathogenicOrLikelyPathogenicGenomicPositionsByHgvsp.put(hgvspId, new LinkedList<>());
                    }
                    pathogenicOrLikelyPathogenicGenomicPositionsByHgvsp.get(hgvspId).add(genomicPosition);

                    String halfHgvspId = getHalfHgvsP(hgvspId);
                    if (!pathogenicOrLikelyPathogenicGenomicPositionsByHalfHgvsp.containsKey(halfHgvspId)) {
                        pathogenicOrLikelyPathogenicGenomicPositionsByHalfHgvsp.put(halfHgvspId, new LinkedList<>());
                    }
                    pathogenicOrLikelyPathogenicGenomicPositionsByHalfHgvsp.get(halfHgvspId).add(genomicPosition);
                }
            }
        } else {
            System.err.println("Could not locate src/main/resources/clinvar/cv_hgvsp_path.txt");
        }
    }


    /**
     * samples:
     * #Gene	TotalClinvarVariants	PLpMissense	BenignMissense	PLpNullVariants	PLpNonNullVariants
     * TTC22:55001	14	0	14	0	0
     * DOCK8:81704|DOCK8-AS1:157983	57	0	14	2	0
     * DOCK8:81704	2029	1	775	58	7
     */
    @SneakyThrows
    public void loadGeneInfoFromFile() {
        Path varPath = new File("src/main/resources/clinvar/cv_gene_info.txt").toPath();
        if (Files.exists(varPath)) {
            String input = Files.readString(varPath);
            String[] lines = input.split("\n");
            for (String line : lines) {
                if (line.startsWith("#") || line.length() < 1) {
                    continue; //ignore header
                }

                String[] tabSeperated = line.split("\t");

                String[] genes = tabSeperated[0].split("[|]"); //for cases like UGT1A:7361|UGT1A10:54575|UGT1A8:54576|UGT1A7:54577|UGT1A6:54578|UGT1A5:54579|UGT1A9:54600|UGT1A4:54657|UGT1A3:54659|DNAJB3:414061
                int totalClinvarVariants = Integer.parseInt(tabSeperated[1]);
                int pathogenicLikelyPathogenicMissenseVariants = Integer.parseInt(tabSeperated[2]);
                int benignMissenseVariants = Integer.parseInt(tabSeperated[3]);
                int pathogenicLikelyPathogenicNullVariants = Integer.parseInt(tabSeperated[4]);
                int pathogenicLikelyPathogenicNonNullVariants = Integer.parseInt(tabSeperated[5]);

                for (String rawGeneName : genes) {
                    String gene = cleanGeneName(rawGeneName);

                    if (!pathogenicLikelyPathogenicMissenseVariantsByGene.containsKey(gene)) {
                        pathogenicLikelyPathogenicMissenseVariantsByGene.put(gene, 0);
                    }
                    pathogenicLikelyPathogenicMissenseVariantsByGene.put(gene, pathogenicLikelyPathogenicMissenseVariantsByGene.get(gene) + pathogenicLikelyPathogenicMissenseVariants);

                    if (!benignMissenseVariantsByGene.containsKey(gene)) {
                        benignMissenseVariantsByGene.put(gene, 0);
                    }
                    benignMissenseVariantsByGene.put(gene, benignMissenseVariantsByGene.get(gene) + benignMissenseVariants);

                    if (!pathogenicLikelyPathogenicNullVariantsByGene.containsKey(gene)) {
                        pathogenicLikelyPathogenicNullVariantsByGene.put(gene, 0);
                    }
                    pathogenicLikelyPathogenicNullVariantsByGene.put(gene, pathogenicLikelyPathogenicNullVariantsByGene.get(gene) + pathogenicLikelyPathogenicNullVariants);

                    if (!pathogenicLikelyPathogenicNonNullVariantsByGene.containsKey(gene)) {
                        pathogenicLikelyPathogenicNonNullVariantsByGene.put(gene, 0);
                    }
                    pathogenicLikelyPathogenicNonNullVariantsByGene.put(gene, pathogenicLikelyPathogenicNonNullVariantsByGene.get(gene) + pathogenicLikelyPathogenicNonNullVariants);
                }
            }
        } else {
            System.err.println("Could not locate src/main/resources/clinvar/cv_gene_info.txt");
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

    public List<GenomicPosition> findPathogenics(String chrom, String pos) {
        initiate();
        return pathogenicOrLikelyPathogenicGenomicPositionsByChromAndPos.get(chrom + ":" + pos);
    }

    public List<GenomicPosition> findPathogenics(String chrom, String pos, String alt) {
        initiate();
        return pathogenicOrLikelyPathogenicGenomicPositionsByChromAndPosAndAlt.get(chrom + ":" + pos + ":" + alt);
    }

    public List<GenomicPosition> findPathogenicsByHgvsP(String hgvsp) {
        initiate();
        return pathogenicOrLikelyPathogenicGenomicPositionsByHgvsp.get(hgvsp);
    }

    public List<GenomicPosition> findNonPathogenics(String chrom, String pos) {
        initiate();
        return benignGenomicPositionsByChromAndPos.get(chrom + ":" + pos);
    }

    public int countPathogenicOrLikelyPathogenicMissenseVariants(String gene) {
        initiate();
        String cleanGeneName = cleanGeneName(gene);
        if (pathogenicLikelyPathogenicMissenseVariantsByGene.containsKey(cleanGeneName)) {
            return pathogenicLikelyPathogenicMissenseVariantsByGene.get(cleanGeneName);
        }
        return 0;
    }

    public int countBenignMissenseVariants(String gene) {
        initiate();
        String cleanGeneName = cleanGeneName(gene);
        if (benignMissenseVariantsByGene.containsKey(cleanGeneName)) {
            return benignMissenseVariantsByGene.get(cleanGeneName(gene));
        }
        return 0;
    }

    public int countPathogenicOrLikelyPathogenicNullVariants(String gene) {
        initiate();
        String cleanGeneName = cleanGeneName(gene);
        if (pathogenicLikelyPathogenicNullVariantsByGene.containsKey(cleanGeneName)) {
            return pathogenicLikelyPathogenicNullVariantsByGene.get(cleanGeneName(gene));
        }
        return 0;
    }

    public int countPathogenicOrLikelyPathogenicNonNullVariants(String gene) {
        initiate();
        String cleanGeneName = cleanGeneName(gene);
        if (pathogenicLikelyPathogenicNonNullVariantsByGene.containsKey(cleanGeneName)) {
            return pathogenicLikelyPathogenicNonNullVariantsByGene.get(cleanGeneName(gene));
        }
        return 0;
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
        return pathogenicOrLikelyPathogenicGenomicPositionsByHalfHgvsp.get(getHalfHgvsP(hgvsp));
    }

    private String getHalfHgvsP(String hgvspId) {
        return hgvspId.replaceAll("[A-Za-z]+$", "");
    }

    /**
     * TTC22:55001 -> TTC22
     * DOCK8:81704 -> DOCK8
     *
     * @param input the raw gene name
     * @return the cleaned HGNC gene name
     */
    private String cleanGeneName(String input) {
        if (input.contains(":")) {
            return input.split(":")[0];
        }
        return input;
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
