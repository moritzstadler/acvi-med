package at.ac.meduniwien.vcfvisualize.knowledgebase.pharmgkb;

import at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar.GenomicPosition;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class PharmGKB {

    private boolean dataLoaded;

    private HashMap<String, List<PharmGKBAnnotation>> rsIdToAnnotations;
    private HashMap<String, List<PharmGKBEvidence>> clinicalAnnotationIdToEvidences;
    private HashMap<String, List<PharmGKBAllele>> clinicalAnnotationIdToAlleles;

    public PharmGKB() {
        rsIdToAnnotations = new HashMap<>();
        clinicalAnnotationIdToEvidences = new HashMap<>();
        clinicalAnnotationIdToAlleles = new HashMap<>();
    }

    public void initiate() {
        if (!dataLoaded) {
            loadAllelesFromFile();
            loadEvidenceFromFile();
            loadAnnotationsFromFile();
            dataLoaded = true;
        }
    }

    /**
     * Clinical Annotation ID	Genotype/Allele	Annotation Text	Allele Function
     * 981755803	AA	Patients with the rs75527207 AA genotype (two copies of the CFTR G551D variant) and cystic fibrosis may respond to ivacaftor treatment. FDA-approved drug labeling information and CPIC guidelines indicate use of ivacaftor in cystic fibrosis patients with at least one copy of a list of 33 CFTR genetic variants, including G551D. Other genetic and clinical factors may also influence response to ivacaftor.
     */
    @SneakyThrows
    public void loadAllelesFromFile() {
        Path varPath = new File("src/main/resources/pharmgkb/clinical_ann_alleles.tsv").toPath();
        if (Files.exists(varPath)) {
            String input = Files.readString(varPath);
            String[] lines = input.split("\n");
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                if (line.length() < 1) {
                    continue;
                }

                String[] tabSeparated = line.split("\t");

                String clinicalAnnotationId = tabSeparated[0];
                String genotype = tabSeparated[1];
                String annotationText = tabSeparated[2];
                String alleleFunction = null;
                if (tabSeparated.length > 3) {
                    alleleFunction = tabSeparated[3];
                }

                PharmGKBAllele pharmGKBAllele = new PharmGKBAllele(clinicalAnnotationId, genotype, annotationText, alleleFunction);

                if (!clinicalAnnotationIdToAlleles.containsKey(clinicalAnnotationId)) {
                    clinicalAnnotationIdToAlleles.put(clinicalAnnotationId, new LinkedList<>());
                }
                clinicalAnnotationIdToAlleles.get(clinicalAnnotationId).add(pharmGKBAllele);
            }
        } else {
            System.err.println("Could not locate src/main/resources/pharmgkb/clinical_ann_alleles.tsv");
        }
    }

    /**
     Clinical Annotation ID	Evidence ID	Evidence Type	Evidence URL	PMID	Summary	Score
     981755803	PA166114461	Guideline Annotation	https://www.pharmgkb.org/guidelineAnnotation/PA166114461		Annotation of CPIC Guideline for ivacaftor and CFTR	100
     */
    @SneakyThrows
    public void loadEvidenceFromFile() {
        Path varPath = new File("src/main/resources/pharmgkb/clinical_ann_evidence.tsv").toPath();
        if (Files.exists(varPath)) {
            String input = Files.readString(varPath);
            String[] lines = input.split("\n");
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                if (line.length() < 1) {
                    continue;
                }

                String[] tabSeparated = line.split("\t");

                String clinicalAnnotationId = tabSeparated[0];
                String evidenceId = tabSeparated[1];
                String evidenceType = tabSeparated[2];
                String evidenceUrl = tabSeparated[3];
                String pmid = tabSeparated[4];
                String summary = tabSeparated[5];
                String score = tabSeparated[6];

                PharmGKBEvidence pharmGKBEvidence = new PharmGKBEvidence(clinicalAnnotationId, evidenceId, evidenceType, evidenceUrl, pmid, summary, score);

                if (!clinicalAnnotationIdToEvidences.containsKey(clinicalAnnotationId)) {
                    clinicalAnnotationIdToEvidences.put(clinicalAnnotationId, new LinkedList<>());
                }
                clinicalAnnotationIdToEvidences.get(clinicalAnnotationId).add(pharmGKBEvidence);
            }
        } else {
            System.err.println("Could not locate src/main/resources/pharmgkb/clinical_ann_evidence.tsv");
        }
    }

    /**
     Clinical Annotation ID	Variant/Haplotypes	Gene	Level of Evidence	Level Override	Level Modifiers	Score	Phenotype Category	PMID Count	Evidence Count	Drug(s)	Phenotype(s)	Latest History Date (YYYY-MM-DD)	URL	Specialty Population
     981755803	rs75527207	CFTR	1A		Rare Variant; Tier 1 VIP	234.875	Efficacy	28	30	ivacaftor	Cystic Fibrosis	2021-03-24	https://www.pharmgkb.org/clinicalAnnotation/981755803	Pediatric
     */
    @SneakyThrows
    public void loadAnnotationsFromFile() {
        Path varPath = new File("src/main/resources/pharmgkb/clinical_annotations.tsv").toPath();
        if (Files.exists(varPath)) {
            String input = Files.readString(varPath);
            String[] lines = input.split("\n");
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                if (line.length() < 1) {
                    continue;
                }

                String[] tabSeparated = line.split("\t");

                String clinicalAnnotationId = tabSeparated[0];
                String variantOrHaplotypes = tabSeparated[1];
                String gene = tabSeparated[2];
                String levelOfEvidence = tabSeparated[3];
                String levelOverride = tabSeparated[4];
                String levelModifiers = tabSeparated[5];
                String score = tabSeparated[6];
                String phenotypeCategory = tabSeparated[7];
                String pmidCount = tabSeparated[8];
                String evidenceCount = tabSeparated[9];
                String drugs = tabSeparated[10];
                String phenotypes = tabSeparated[11];
                String latestHistory = tabSeparated[12];
                String url = tabSeparated[13];
                String specialtyPopulation = null;
                if (tabSeparated.length > 14) {
                    specialtyPopulation = tabSeparated[14];
                }

                PharmGKBAnnotation pharmGKBAnnotation = new PharmGKBAnnotation(clinicalAnnotationId, variantOrHaplotypes, gene, levelOfEvidence, levelOverride, levelModifiers, score, phenotypeCategory, pmidCount, evidenceCount, drugs, phenotypes, latestHistory, url, specialtyPopulation);

                //check if variantor haplotypes contains rsId
                if (!variantOrHaplotypes.startsWith("rs")) {
                    continue;
                }

                String rsId = variantOrHaplotypes;
                System.out.println(rsId);

                if (!rsIdToAnnotations.containsKey(rsId)) {
                    rsIdToAnnotations.put(rsId, new LinkedList<>());
                }
                rsIdToAnnotations.get(rsId).add(pharmGKBAnnotation);
            }
        } else {
            System.err.println("Could not locate src/main/resources/pharmgkb/clinical_annotations.tsv");
        }
    }

    /**
     * returns all rsIds for which information is included in PharmGKB
     *
     * @return a set of strings
     */
    public Set<String> getKnownRsIds() {
        return rsIdToAnnotations.keySet();
    }

    public List<PharmGKBAnnotation> getAnnotationsByRsId(String rsId) {
        initiate();
        return rsIdToAnnotations.get(rsId);
    }

    public List<PharmGKBEvidence> getEvidenceByAnnotationId(String clinicalAnnotationId) {
        initiate();
        return clinicalAnnotationIdToEvidences.get(clinicalAnnotationId);
    }

    public List<PharmGKBAllele> getAllelesByAnnotationId(String clinicalAnnotationId) {
        initiate();
        return clinicalAnnotationIdToAlleles.get(clinicalAnnotationId);
    }

}
