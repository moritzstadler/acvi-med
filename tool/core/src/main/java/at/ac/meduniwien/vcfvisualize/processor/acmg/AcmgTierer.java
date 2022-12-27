package at.ac.meduniwien.vcfvisualize.processor.acmg;

import at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar.Clinvar;
import at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar.GenomicPosition;
import at.ac.meduniwien.vcfvisualize.model.Variant;
import at.ac.meduniwien.vcfvisualize.rest.dto.HumanDTO;
import at.ac.meduniwien.vcfvisualize.rest.dto.HumansDTO;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AcmgTierer {

    @Autowired
    Clinvar clinvar;

    private boolean foundThroughClinvar; //TODO is this used
    private boolean foundThroughGreenPanelApp; //TODO is this used
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
        //TODO is wrong, does not work
        //TODO all would be psv1 right now
        //TODO needs to be pathogenic or panel or hpo
        List<GenomicPosition> result = clinvar.findPathogenics(variant.getChrom(), variant.getPos(), variant.getAlt());
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
        //TODO test this thoroughly
        //TODO check if the whole system can find stuff like Huntington's disease mutations?

        return false;
        /*
        String changeInCodons = variant.getInfo().get("info_csq_codons"); // aAg/aGg or aAA/aGG
        if (changeInCodons == null) {
            return false;
        }

        String referenceTriplet = changeInCodons.split("/")[0]; //aAg
        String actualTriplet = changeInCodons.split("/")[1]; //aGg

        List<String> tripletsWithSimilarEffect = GeneticCode.triplesWithSimilarAminoAcid(actualTriplet.toUpperCase()); //CGU, CGC, CGA, CGG, AGA, AGG

        int offset = -1 * positionOfFirstUppercaseLetter(referenceTriplet);
        List<GenomicPosition>[] pathogenicMutations = (ArrayList<GenomicPosition>[]) new ArrayList[3];
        for (int i = 0; i < 3; i++) {
            pathogenicMutations[i] = clinvar.findPathogenics(variant.getChrom(), variant.getPos() + offset + i);
        }

        char wildcard = '_';
        for (String similarTriplet : tripletsWithSimilarEffect) {
            String difference = tripletDifference(referenceTriplet, similarTriplet, wildcard);
            for (int i = 0; i < 3; i++) {
                for (GenomicPosition pathogenic : pathogenicMutations[i]) {
                    if (equalAltAndDifference(pathogenic.getAlt(), difference, i, wildcard)) {
                        return true;
                    }
                }
            }
        }

        return false;

        /*
        TODO this comment is no longer true
        take possible triplets and find difference to reference genome

        difference to reference AAG:
        CGU, CGC, CGA, CGG, AGA, AGG, AAA (example)
        CGU, CGC, CGA, CG_, _GA, _G_, __A (example)

        determine positions of bases, in this case: -1, 0, +1
        check the pathogenic Clinvar entries at these positions
        This returns e. g.
        (1)
            -1: C
            0: G
            +1: C, A

            now check for each difference if it matches
            CGU: yes, yes, no -> no match
            CGC: yes, yes, yes -> match
            CGA: yes, yes, yes -> match
            CG_: yes, yes, _ -> match
            _GA: _, yes, yes -> match
            _G_: _, yes, _ -> match

        (2)
            -1:
            0:
            +1: A

            only ___A will match here
         */


        //TODO take these from clinvar vcf - ref alt does not need to be equal only crom and pos!!!

        //TODO e. g. patient GAG = Leu then search for all which could also be Leu

        //TODO check which amino acid it is and which other mutations would keep the same amino acid.
        //TODO check "change in codons". check all other trios which would make the same amino acid
        //TODO we now have a list of triplets. if any of these triplets is found in clinvar as pathogenic
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
                //TODO check if genotype can be accessed like this
                String genotypeParent = variant.getInfo().get("format_" + humanDTO.getPseudonym() + "_gt");
                boolean hasNoMutation = genotypeParent.equals("0/0") || genotypeParent.equals("0|0");
                if (!hasNoMutation) {
                    return false;
                }
            }
        }

        return true; //TODO implement
    }

    /**
     * Well-established in vitro or in vivo functional studies supportive of a
     * damaging effect on the gene or gene product
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public boolean isPS3(Variant variant) {
        //TODO schicken wie ich jetzt tiere
        //TODO das passt noch nicht
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
        List<String> alleleFrequencies = Arrays.asList("info_af_raw", "info_controls_af_popmax", "info_af_afr", "info_af_amr", "info_af_asj", "info_af_eas", "info_af_nfe", "info_af_oth", "info_csq_af", "info_csq_gnomadg_ac", "info_csq_gnomadg_af", "info_csq_gnomadg_controls_ac", "info_csq_gnomadg_controls_af", "info_csq_gnomadg_controls_nhomalt", "info_csq_gnomadg_nhomalt_nfe", "info_csq_gnomadg_af_afr", "info_csq_gnomadg_af_amr", "info_csq_gnomadg_af_asj", "info_csq_gnomadg_af_eas", "info_csq_gnomadg_af_fin", "info_csq_gnomadg_af_nfe", "info_csq_gnomadg_af_oth");

        for (String key : alleleFrequencies) {
            if (variant.getInfo().containsKey(key)) {
                String value = variant.getInfo().get(key);
                //check if value is larger than a threshold, if yes return false
                if (StringUtils.isNumeric(value)) {
                    double doubleValue = Double.parseDouble(value);
                    if (doubleValue > 0.001) { //TODO is this extremely low
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
        List<GenomicPosition> result = clinvar.findPathogenics(variant.getChrom(), variant.getPos());
        return result != null && !result.isEmpty();
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
            //TODO check if genotype can be accessed like this
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
        //TODO CADD Indel raw or CADD raw > 10 -> CADD+
        //wenn nur cadd 1/1 oder 1/2 oder 2/3 oder 2/4
        //TODO check GERP, Polyphen=deleterious,possiblydamaging, SIFT=deleterious,possiblydamaging, CADD (what thresholds), what if some values are not present
        return false;
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
        List<String> alleleFrequencies = Arrays.asList("info_af_raw", "info_controls_af_popmax", "info_af_afr", "info_af_amr", "info_af_asj", "info_af_eas", "info_af_nfe", "info_af_oth", "info_csq_af", "info_csq_gnomadg_ac", "info_csq_gnomadg_af", "info_csq_gnomadg_controls_ac", "info_csq_gnomadg_controls_af", "info_csq_gnomadg_controls_nhomalt", "info_csq_gnomadg_nhomalt_nfe", "info_csq_gnomadg_af_afr", "info_csq_gnomadg_af_amr", "info_csq_gnomadg_af_asj", "info_csq_gnomadg_af_eas", "info_csq_gnomadg_af_fin", "info_csq_gnomadg_af_nfe", "info_csq_gnomadg_af_oth");

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
            //TODO check if genotype can be accessed like this
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

    //helper methods

    public int positionOfFirstUppercaseLetter(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isUpperCase(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    public String tripletDifference(String reference, String actual, char wildcard) {
        String cleanReference = reference.toUpperCase();
        String cleanActual = actual.toUpperCase();

        assert cleanReference.length() == 3 && cleanActual.length() == 3;

        String difference = "";
        for (int i = 0; i < 3; i++) {
            if (cleanReference.charAt(i) == cleanActual.charAt(i)) {
                difference += wildcard;
            } else {
                difference += cleanActual.charAt(i);
            }
        }

        return difference;
    }

    public boolean equalAltAndDifference(String alt, String difference, int start, char wildcard) {
        alt = alt.toUpperCase();
        difference = difference.toUpperCase();

        assert difference.length() == 3;

        for (int i = 0; i < start; i++) {
            alt = wildcard + alt;
        }

        while (alt.length() < 3) {
            alt += wildcard;
        }

        /*
        alt: ACGGGGGGGGGGGGG
        altnew: _ACGGGGGGGGGGGGG
        difference: _AC
        start: 1
        True

        alt: ACGGGGGGGGGGGGG
        altnew: _ACGGGGGGGGGGGGG
        difference: _A_
        False

        alt: A
        altnew: _A_
        difference: _A_
        True

        alt: AAA
        altnew: AAA
        difference: A_A
        False
         */

        return alt.substring(0, 3).equals(difference);
    }


}
