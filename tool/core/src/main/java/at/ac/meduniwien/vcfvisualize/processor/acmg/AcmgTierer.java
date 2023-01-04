package at.ac.meduniwien.vcfvisualize.processor.acmg;

import at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar.Clinvar;
import at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar.GenomicPosition;
import at.ac.meduniwien.vcfvisualize.model.Variant;
import at.ac.meduniwien.vcfvisualize.rest.dto.HumanDTO;
import at.ac.meduniwien.vcfvisualize.rest.dto.HumansDTO;
import com.sun.xml.bind.v2.TODO;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AcmgTierer {

    @Autowired
    Clinvar clinvar;

    private boolean foundThroughClinvar;
    private boolean foundThroughGreenPanelApp;
    private HumansDTO humansDTO;

    /**
     * returns a list of ACMG tiers
     *
     * @param variant the variant to be tiered
     * @return a list of acmg tiers
     */
    @SneakyThrows
    public List<AcmgTier> performAcmgTiering(Variant variant, boolean foundThroughClinvar, boolean foundThroughGreenPanelApp, HumansDTO humansDTO) {
        this.foundThroughClinvar = foundThroughClinvar;
        this.foundThroughGreenPanelApp = foundThroughGreenPanelApp;
        this.humansDTO = humansDTO;

        List<AcmgTier> tiers = new LinkedList<>();

        //TODO only take green panels!
        //TODO check if the whole system can find stuff like Huntington's disease mutations?

        for (AcmgTier acmgTier : AcmgTier.values()) {
            //calls isPSV1(variant), isPS1(variant), isPS2(variant) ...
            boolean isTier = (boolean) this.getClass().getMethod("is" + acmgTier, Variant.class).invoke(this, variant);
            if (isTier) {
                tiers.add(acmgTier);
            }
        }

        return tiers;
    }

    /**
     * Null variant (nonsense, frameshift, canonical +/−1 or 2 splice sites, initiation
     * codon, single or multi-exon deletion) in a gene where loss of function (LOF)
     * is a known mechanism of disease
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public boolean isPSV1(Variant variant) {
        //check if the chrom pos alt entry is known and Pathogenic or Likely_pathogenic as defined by ClinVar
        List<GenomicPosition> result = clinvar.findPathogenics(variant.getChrom().replace("chr", ""), variant.getPos(), variant.getAlt());
        return result != null && !result.isEmpty();
    }

    /**
     * Same amino acid change as a previously established pathogenic variant
     * regardless of nucleotide change
     * Example:	Val->Leu caused by either G>C or G>T in the same codon
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public boolean isPS1(Variant variant) {
        //in the clinvar file '=' which can be a part of HGVSp is stored as %3D
        //chrom can be 1, 2 ...  X and Y and MT

        //check if the protein change (HGVSp) is known to ClinVar as Pathogenic or Likely_pathogenic. In that case the nucleotides might be different but the protein is still similar to a pathogenic or likely pathogenic protein
        String hgvspKey = "info_csq_hgvsp";
        if (!variant.getInfo().containsKey(hgvspKey)) {
            return false;
        }
        String hgvsp = variant.getInfo().get(hgvspKey);
        if (hgvsp == null) {
            return false;
        }

        return clinvar.findPathogenicsByHgvsP(hgvsp) != null && !clinvar.findPathogenicsByHgvsP(hgvsp).isEmpty();
    }

    /**
     * De novo (both maternity and paternity confirmed) in a patient with the
     * disease and no family history
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public boolean isPS2(Variant variant) {
        //TODO confirmation of paternity and maternity by counting mendelian errors

        //get rid of non trios
        if (humansDTO == null || humansDTO.getHumans().size() != 3) {
            return false;
        }

        //get rid of trios where the parents are affected
        for (HumanDTO humanDTO : humansDTO.getHumans()) {
            boolean isHealthyNonIndexPerson = !humanDTO.getIsAffected() && !humanDTO.getIsIndex();
            boolean isAffectedIndexPerson = humanDTO.getIsAffected() && humanDTO.getIsIndex();
            if (!(isHealthyNonIndexPerson || isAffectedIndexPerson)) {
                return false;
            }
        }

        //check if parents are 0/0
        List<HumanDTO> parents = new LinkedList<>();
        for (HumanDTO humanDTO : humansDTO.getHumans()) {
            if (!humanDTO.getIsIndex()) {
                //humanDTO is a parent, check if they are 0/0
                String genotypeParent = variant.getInfo().get("format_" + humanDTO.getPseudonym() + "_gt");
                boolean hasNoMutation = genotypeParent.equals("0/0") || genotypeParent.equals("0|0");
                if (!hasNoMutation) {
                    return false;
                }
            }
        }

        return true; //TODO implement paternity test
    }

    /**
     * Well-established in vitro or in vivo functional studies supportive of a
     * damaging effect on the gene or gene product
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public boolean isPS3(Variant variant) {
        //TODO this is not right, too loose
        //(iff gene is in a selected panel and green) OR (PS1 = true OR PVS1 = true)
        return foundThroughGreenPanelApp || (isPS1(variant) || isPSV1(variant)); //TODO evaluate if this is a performance problem
    }

    public boolean isPS4(Variant variant) {
        return false; //TODO will be difficult
    }

    public boolean isPM1(Variant variant) {
        return false; //TODO needs annotation
    }

    /**
     * Absent from controls (or at extremely low frequency if recessive)
     * in Exome Sequencing Project, 1000 Genomes or ExAC
     *
     * @param variant the variant to be checked
     * @return true if the variant confirms with the tier
     */
    public boolean isPM2(Variant variant) {
        List<String> alleleFrequencies = Arrays.asList("info_af_raw", "info_controls_af_popmax", "info_af_afr", "info_af_amr", "info_af_asj", "info_af_eas", "info_af_nfe", "info_af_oth", "info_csq_af", "info_csq_gnomadg_ac", "info_csq_gnomadg_af", "info_csq_gnomadg_controls_ac", "info_csq_gnomadg_controls_af", "info_csq_gnomadg_controls_nhomalt", "info_csq_gnomadg_nhomalt_nfe", "info_csq_gnomadg_af_afr", "info_csq_gnomadg_af_amr", "info_csq_gnomadg_af_asj", "info_csq_gnomadg_af_eas", "info_csq_gnomadg_af_fin", "info_csq_gnomadg_af_nfe", "info_csq_gnomadg_af_oth", "info_af_raw", "info_controls_af_popmax", "info_af_afr", "info_af_amr", "info_af_asj", "info_af_eas", "info_af_nfe", "info_af_oth", "info_csq_af","info_csq_gnomadg_ac","info_csq_gnomadg_af","info_csq_gnomadg_controls_ac","info_csq_gnomadg_controls_af","info_csq_gnomadg_controls_nhomalt", "info_csq_gnomadg_nhomalt_nfe","info_csq_gnomadg_af_afr","info_csq_gnomadg_af_amr","info_csq_gnomadg_af_asj","info_csq_gnomadg_af_eas","info_csq_gnomadg_af_fin", "info_csq_gnomadg_af_nfe","info_csq_gnomadg_af_oth", "info_csq_gnomad_af", "info_csq_gnomad_afr_af", "info_csq_gnomad_amr_af", "info_csq_gnomad_asj_af", "info_csq_gnomad_eas_af", "info_csq_gnomad_fin_af", "info_csq_gnomad_nfe_af", "info_csq_gnomad_oth_af", "info_csq_gnomad_sas_af");

        for (String key : alleleFrequencies) {
            if (variant.getInfo().containsKey(key) && variant.getInfo().get(key) != null) {
                String value = variant.getInfo().get(key);
                //check if value is larger than a threshold, if yes return false
                if (StringUtils.isNumeric(value)) {
                    double doubleValue = Double.parseDouble(value);
                    if (doubleValue > 0.001) { //TODO maybe adapt this threshold
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public boolean isPM3(Variant variant) {
        return false; //TODO impossible(?) check haplotype? could be possible for trios
    }

    /**
     * Protein length changes due to in-frame deletions/insertions in a non-repeat
     * region or stop-loss variants
     *
     * @param variant the variant to be checked
     * @return true if the variant confirms with the tier
     */
    public boolean isPM4(Variant variant) {
        //info_csq_consequence
        String key = "info_csq_consequence";
        if (variant.getInfo().containsKey(key)) {
            String value = variant.getInfo().get(key);
            if (value.contains("inframe_insertion") || value.contains("inframe_deletion")) {
                return true; //TODO has to be in a non-repeat region or stop-loss variants
                //TODO UCSC repeat tracks
            }
        }

        return false;
    }

    /**
     * Novel missense change at an amino acid residue where a different
     * missense change determined to be pathogenic has been seen before
     * Example: Arg156His is pathogenic; now you observe Arg156Cys
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public boolean isPM5(Variant variant) {
        List<GenomicPosition> resultNucleotide = clinvar.findPathogenics(variant.getChrom().replace("chr", ""), variant.getPos());

        List<GenomicPosition> resultProtein = null;
        String hgvspKey = "info_csq_hgvsp";
        if (variant.getInfo().containsKey(hgvspKey)) {
            String hgvsp = variant.getInfo().get(hgvspKey);
            if (hgvsp != null) {
                resultProtein = clinvar.findPathogenicsByHgvsPNonMatchAlt(hgvsp);
            }
        }

        int foundResults = 0;
        if (resultNucleotide != null) {
            foundResults += resultNucleotide.size();
        }

        if (resultProtein != null) {
            foundResults += resultProtein.size();
        }

        return foundResults > 0;
    }

    /**
     * Assumed de novo, but without confirmation of paternity and maternity
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public boolean isPM6(Variant variant) {
        return isPS2(variant);
    }

    /**
     * Co-segregation with disease in multiple affected family members in a gene
     * definitively known to cause the disease
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public boolean isPP1(Variant variant) {
        //needs trio or at least a multi person sample. E. g. mother is sick and child -> if mother has mutation and child has mutation it is this tier

        //get rid of non multi person sample
        if (humansDTO == null || humansDTO.getHumans().size() <= 1) {
            return false;
        }

        //check if everyone who is not affected is 0/0 and everyone who is affected is not 0/0
        for (HumanDTO humanDTO : humansDTO.getHumans()) {
            String genotypeParent = variant.getInfo().get("format_" + humanDTO.getPseudonym() + "_gt");
            boolean hasNoMutation = genotypeParent.equals("0/0") || genotypeParent.equals("0|0");
            boolean affectedButNoMutation = humanDTO.getIsAffected() && hasNoMutation;
            boolean notAffectedButMutation = !humanDTO.getIsAffected() && !hasNoMutation;
            if (affectedButNoMutation || notAffectedButMutation) {
                return false;
            }
        }

        return true;
    }

    public boolean isPP2(Variant variant) {
        return false; //TODO = missense AND (found via panelapp or clinvar) AND allele frequency < 0.001
    }

    /**
     * Multiple lines of computational evidence support a deleterious effect on
     * the gene or gene product (conservation, evolutionary, splicing impact, etc)
     *
     * @param variant the variant to be checked
     * @return true if the variants confirms with the tier
     */
    public boolean isPP3(Variant variant) {
        //TODO check thresholds
        //TODO make sure these are not co-dependent
        //TODO what about info_cadd_phred, info_caddind_phred", "info_caddind_raw

        int numberOfScores = 0;
        int numberOfPathogenicScores = 0;

        //info_csq_gerp_rs > 4
        String gerpKey = "info_csq_gerp_rs";
        if (variant.getInfo().containsKey(gerpKey) && variant.getInfo().get(gerpKey) != null) {
            numberOfScores++;
            if (StringUtils.isNumeric(variant.getInfo().get(gerpKey))) {
                if (Double.parseDouble(variant.getInfo().get(gerpKey)) >= 4.0) {
                    numberOfPathogenicScores++;
                }
            }
        }

        //info_cadd_raw > 10
        String caddKey = "info_cadd_raw";
        if (variant.getInfo().containsKey(caddKey) && variant.getInfo().get(caddKey) != null) {
            numberOfScores++;
            if (StringUtils.isNumeric(variant.getInfo().get(caddKey))) {
                if (Double.parseDouble(variant.getInfo().get(caddKey)) >= 10.0) {
                    numberOfPathogenicScores++;
                }
            }
        }

        //info_csq_polyphen tolower contains 'damaging'
        String polyphenKey = "info_csq_polyphen";
        if (variant.getInfo().containsKey(polyphenKey) && variant.getInfo().get(polyphenKey) != null) {
            numberOfScores++;
            if (variant.getInfo().get(polyphenKey).toLowerCase().contains("damaging")) {
                numberOfPathogenicScores++;
            }
        }

        //OK info_csq_sift = deleterious
        String siftKey = "info_csq_sift";
        if (variant.getInfo().containsKey(siftKey) && variant.getInfo().get(siftKey) != null) {
            numberOfScores++;
            if (variant.getInfo().get(siftKey).toLowerCase().contains("deleterious")) {
                numberOfPathogenicScores++;
            }
        }

        //if there is at least a score and the majority of scores are pathogenic
        return numberOfScores > 0 && 2 * numberOfPathogenicScores >= numberOfScores;
    }

    public boolean isPP4(Variant variant) {
        return false; //TODO either for all or for none, impossible
    }

    public boolean isPP5(Variant variant) {
        return false; //TODO impossible
    }

    /**
     * Allele frequency is above 5% in Exome Sequencing Project, 1000 Genomes,
     * or ExAC
     *
     * @param variant the variant to be checked
     * @return ture if the variant is the tier
     */
    public boolean isBA1(Variant variant) {
        List<String> alleleFrequencies = Arrays.asList("info_af_raw", "info_controls_af_popmax", "info_af_afr", "info_af_amr", "info_af_asj", "info_af_eas", "info_af_nfe", "info_af_oth", "info_csq_af", "info_csq_gnomadg_ac", "info_csq_gnomadg_af", "info_csq_gnomadg_controls_ac", "info_csq_gnomadg_controls_af", "info_csq_gnomadg_controls_nhomalt", "info_csq_gnomadg_nhomalt_nfe", "info_csq_gnomadg_af_afr", "info_csq_gnomadg_af_amr", "info_csq_gnomadg_af_asj", "info_csq_gnomadg_af_eas", "info_csq_gnomadg_af_fin", "info_csq_gnomadg_af_nfe", "info_csq_gnomadg_af_oth", "info_csq_gnomad_af", "info_csq_gnomad_afr_af", "info_csq_gnomad_amr_af", "info_csq_gnomad_asj_af", "info_csq_gnomad_eas_af", "info_csq_gnomad_fin_af", "info_csq_gnomad_nfe_af", "info_csq_gnomad_oth_af", "info_csq_gnomad_sas_af");

        for (String key : alleleFrequencies) {
            if (variant.getInfo().containsKey(key)) {
                String value = variant.getInfo().get(key);
                if (StringUtils.isNumeric(value)) {
                    double doubleValue = Double.parseDouble(value);
                    if (doubleValue > 0.05) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isBS1(Variant variant) {
        //TODO needs some sort of input from the user how high common the expected disorder is
        //TODO probably not
        return false; //TODO implement
    }

    public boolean isBS2(Variant variant) {
        return false; //TODO gnomad tells us how many homozygote are
        //TODO probably impossible
    }

    public boolean isBS3(Variant variant) {
        return false; //TODO will never appear
    }

    /**
     * Lack of segregation in affected members of a family
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public boolean isBS4(Variant variant) {
        //needs trio or at least a multi person sample. True if a healthy parent has the same mutation
        //get rid of non multi person sample
        if (humansDTO == null || humansDTO.getHumans().size() <= 1) {
            return false;
        }

        //check if there is at least one person who is affected but has 0/0 or who is not affected but has not 0/0
        for (HumanDTO humanDTO : humansDTO.getHumans()) {
            String genotypeParent = variant.getInfo().get("format_" + humanDTO.getPseudonym() + "_gt");
            boolean hasNoMutation = genotypeParent.equals("0/0") || genotypeParent.equals("0|0");
            boolean affectedButNoMutation = humanDTO.getIsAffected() && hasNoMutation;
            boolean notAffectedButMutation = !humanDTO.getIsAffected() && !hasNoMutation;
            if (affectedButNoMutation || notAffectedButMutation) {
                return true;
            }
        }

        return false;
    }

    public boolean isBP1(Variant variant) {
        return false; //TODO implement difficult
    }

    public boolean isBP2(Variant variant) {
        return false; //TODO impossible(?) check haplotype? could be possible for trios
    }

    public boolean isBP3(Variant variant) {
        return false; //TODO difficult maybe
    }

    public boolean isBP4(Variant variant) {
        return false; //TODO see PP3
    }

    public boolean isBP5(Variant variant) {
        return false; //TODO necessary data not available yet
    }

    public boolean isBP6(Variant variant) {
        return false; //TODO impossible
    }

    public boolean isBP7(Variant variant) {
        return false; //TODO similar amino acid and spliceAI non pathogenic(?) EnsemblImpact also GERP
    }

}
