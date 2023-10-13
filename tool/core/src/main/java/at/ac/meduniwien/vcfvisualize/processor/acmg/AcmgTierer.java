package at.ac.meduniwien.vcfvisualize.processor.acmg;

import at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar.Clinvar;
import at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar.GenomicPosition;
import at.ac.meduniwien.vcfvisualize.model.Variant;
import at.ac.meduniwien.vcfvisualize.rest.dto.HumanDTO;
import at.ac.meduniwien.vcfvisualize.rest.dto.HumansDTO;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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
    public List<AcmgTieringResult> performAcmgTiering(Variant variant, boolean foundThroughClinvar, boolean foundThroughGreenPanelApp, HumansDTO humansDTO) {
        this.foundThroughClinvar = foundThroughClinvar;
        this.foundThroughGreenPanelApp = foundThroughGreenPanelApp;
        this.humansDTO = humansDTO;

        List<AcmgTieringResult> tiers = new LinkedList<>();

        //TODO check if the whole system can find stuff like Huntington's disease mutations?

        for (AcmgTier acmgTier : AcmgTier.values()) {
            //calls isPVS1(variant), isPS1(variant), isPS2(variant) ...
            AcmgTieringResult result = (AcmgTieringResult) this.getClass().getMethod("is" + acmgTier, Variant.class).invoke(this, variant);
            result.setTier(acmgTier);
            if (result.isTierApplies()) {
                tiers.add(result);
            }
        }

        return tiers;
    }

    /**
     * Classifies the variant based on table 5 https://www.ncbi.nlm.nih.gov/pmc/articles/PMC4544753/
     *
     * @param tiers the tiers leading to the classification
     * @return the ACMG classification of the variant
     */
    public AcmgClassificationResult performAcmgClassification(List<AcmgTier> tiers) {
        //make a set to avoid mistakenly doubly added tiers leading to wrong interpretations
        HashSet<AcmgTier> acmgTiers = new HashSet<>(tiers);

        int pathogenicVeryStrongCount = 0;
        int pathogenicStrongCount = 0;
        int pathogenicModerateCount = 0;
        int pathogenicSupportingCount = 0;
        int benignStandAloneCount = 0;
        int benignStrongCount = 0;
        int benignSupportingCount = 0;

        for (AcmgTier tier : acmgTiers) {
           if (tier.equals(AcmgTier.PVS1)) {
               pathogenicVeryStrongCount++;
           } else if (tier.equals(AcmgTier.PS1) || tier.equals(AcmgTier.PS2) || tier.equals(AcmgTier.PS3) || tier.equals(AcmgTier.PS4)) {
               pathogenicStrongCount++;
           } else if (tier.equals(AcmgTier.PM1) || tier.equals(AcmgTier.PM2) || tier.equals(AcmgTier.PM3) || tier.equals(AcmgTier.PM4) || tier.equals(AcmgTier.PM5) || tier.equals(AcmgTier.PM6)) {
               pathogenicModerateCount++;
           } else if (tier.equals(AcmgTier.PP1) || tier.equals(AcmgTier.PP2) || tier.equals(AcmgTier.PP3) || tier.equals(AcmgTier.PP4) || tier.equals(AcmgTier.PP5)) {
               pathogenicSupportingCount++;
           } else if (tier.equals(AcmgTier.BA1)) {
               benignStandAloneCount++;
           } else if (tier.equals(AcmgTier.BS1) || tier.equals(AcmgTier.BS2) || tier.equals(AcmgTier.BS3) || tier.equals(AcmgTier.BS4)) {
               benignStrongCount++;
           } else if (tier.equals(AcmgTier.BP1) || tier.equals(AcmgTier.BP2) || tier.equals(AcmgTier.BP3) || tier.equals(AcmgTier.BP4) || tier.equals(AcmgTier.BP5) || tier.equals(AcmgTier.BP6) || tier.equals(AcmgTier.BP7)) {
               benignSupportingCount++;
           }
        }

        AcmgClassificationResult acmgClassificationResult = new AcmgClassificationResult();

        boolean pathogenic = false;
        if (pathogenicVeryStrongCount >= 1 && pathogenicStrongCount >= 1) {
            pathogenic = true;
            acmgClassificationResult.addExplanation("Pathogenic", "1 Very Strong (PVS1) and ≥1 Strong (PS1–PS4)");
        } else if (pathogenicVeryStrongCount >= 1 && pathogenicModerateCount >= 2) {
            pathogenic = true;
            acmgClassificationResult.addExplanation("Pathogenic", "1 Very Strong (PVS1) and ≥2 Moderate (PM1–PM6)");
        } else if (pathogenicVeryStrongCount >= 1 && pathogenicModerateCount >= 1 && pathogenicSupportingCount >= 1) {
            pathogenic = true;
            acmgClassificationResult.addExplanation("Pathogenic", "1 Very Strong (PVS1) and 1 Moderate (PM1–PM6) and 1 Supporting (PP1–PP5)");
        } else if (pathogenicVeryStrongCount >= 1 && pathogenicSupportingCount >= 2) {
            pathogenic = true;
            acmgClassificationResult.addExplanation("Pathogenic", "1 Very Strong (PVS1) and ≥2 Supporting (PP1–PP5)");
        } else if (pathogenicStrongCount >= 2) {
            pathogenic = true;
            acmgClassificationResult.addExplanation("Pathogenic", "≥2 Strong (PS1–PS4)");
        } else if (pathogenicStrongCount >= 1 && pathogenicModerateCount >= 3) {
            pathogenic = true;
            acmgClassificationResult.addExplanation("Pathogenic", "1 Strong (PS1–PS4) and ≥3 Moderate (PM1–PM6)");
        } else if (pathogenicStrongCount >= 1 && pathogenicModerateCount >= 2 && pathogenicSupportingCount >= 2) {
            pathogenic = true;
            acmgClassificationResult.addExplanation("Pathogenic", "1 Strong (PS1–PS4) and 2 Moderate (PM1–PM6) AND ≥2 Supporting (PP1–PP5)");
        } else if (pathogenicStrongCount >= 1 && pathogenicModerateCount >= 1 && pathogenicSupportingCount >= 4) {
            pathogenic = true;
            acmgClassificationResult.addExplanation("Pathogenic", "1 Strong (PS1–PS4) and 1 Moderate (PM1–PM6) AND ≥4 Supporting (PP1–PP5)");
        }

        boolean likelyPathogenic = false;
        if (pathogenicVeryStrongCount >= 1 && pathogenicModerateCount >= 1) {
            likelyPathogenic = true;
            acmgClassificationResult.addExplanation("Likely Pathogenic", "1 Very Strong (PVS1) AND 1 Moderate (PM1–PM6)");
        } else if (pathogenicStrongCount >= 1 && pathogenicModerateCount >= 1) {
            likelyPathogenic = true;
            acmgClassificationResult.addExplanation("Likely Pathogenic", "1 Strong (PS1–PS4) AND 1–2 Moderate (PM1–PM6)");
        } else if (pathogenicStrongCount >= 1 && pathogenicSupportingCount >= 2) {
            likelyPathogenic = true;
            acmgClassificationResult.addExplanation("Likely Pathogenic", "1 Strong (PS1–PS4) AND ≥2 Supporting (PP1–PP5)");
        } else if (pathogenicModerateCount >= 3) {
            likelyPathogenic = true;
            acmgClassificationResult.addExplanation("Likely Pathogenic", "≥3 Moderate (PM1–PM6)");
        } else if (pathogenicModerateCount >= 2 && pathogenicSupportingCount >= 2) {
            likelyPathogenic = true;
            acmgClassificationResult.addExplanation("Likely Pathogenic", "2 Moderate (PM1–PM6) AND ≥2 Supporting (PP1–PP5)");
        } else if (pathogenicModerateCount >= 1 && pathogenicSupportingCount >= 4) {
            likelyPathogenic = true;
            acmgClassificationResult.addExplanation("Likely Pathogenic", "1 Moderate (PM1–PM6) AND ≥4 Supporting (PP1–PP5)");
        }

        boolean benign = false;
        if (benignStandAloneCount >= 1) {
            benign = true;
            acmgClassificationResult.addExplanation("Benign", "1 Stand-Alone (BA1)");
        } else if (benignStrongCount >= 2) {
            benign = true;
            acmgClassificationResult.addExplanation("Benign", "≥2 Strong (BS1–BS4)");
        }

        boolean likelyBenign = false;
        if (benignStrongCount >= 1 && benignSupportingCount >= 1) {
            likelyBenign = true;
            acmgClassificationResult.addExplanation("Likely Benign", "1 Strong (BS1–BS4) and 1 Supporting (BP1–BP7)");
        } else if (benignSupportingCount >= 2) {
            likelyBenign = true;
            acmgClassificationResult.addExplanation("Likely Benign", "≥2 Supporting (BP1–BP7)");
        }

        //combine results for final verdict
        boolean anyPathogenic = pathogenic || likelyPathogenic;
        boolean anyBenign = benign || likelyBenign;

        if (anyPathogenic && anyBenign) {
            acmgClassificationResult.setAcmgClassification(AcmgClassification.UNCERTAIN_SIGNIFICANCE);
            acmgClassificationResult.addExplanation("Uncertain significance", "Both pathogenic and benign interpretations");
        } else if (anyPathogenic)  {
            if (pathogenic) {
                acmgClassificationResult.setAcmgClassification(AcmgClassification.PATHOGENIC);
            } else {
                acmgClassificationResult.setAcmgClassification(AcmgClassification.LIKELY_PATHOGENIC);
            }
        } else if (anyBenign) {
            if (benign) {
                acmgClassificationResult.setAcmgClassification(AcmgClassification.BENIGN);
            } else {
                acmgClassificationResult.setAcmgClassification(AcmgClassification.LIKELY_BENIGN);
            }
        } else {
            acmgClassificationResult.addExplanation("Uncertain significance", "Neither pathogenic nor benign interpretations");
            acmgClassificationResult.setAcmgClassification(AcmgClassification.UNCERTAIN_SIGNIFICANCE);
        }

        return acmgClassificationResult;
    }

    /**
     * Null variant (nonsense, frameshift, canonical +/−1 or 2 splice sites, initiation
     * codon, single or multi-exon deletion) in a gene where loss of function (LOF)
     * is a known mechanism of disease
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public AcmgTieringResult isPVS1(Variant variant) {
        AcmgTieringResult acmgTieringResult = new AcmgTieringResult();

        //check if the chrom pos alt entry is known and Pathogenic or Likely_pathogenic as defined by ClinVar
        List<GenomicPosition> clinvarResult = clinvar.findPathogenics(variant.getChrom().replace("chr", ""), variant.getPos(), variant.getAlt());
        boolean inClinvarAsPathogenicOrLikelyPathogenic = clinvarResult != null && !clinvarResult.isEmpty();

        //this covers null variant, checking for frameshift, missense, etc. is not necessary (https://www.ensembl.org/info/genome/variation/prediction/predicted_data.html#consequences)
        boolean impactful = variant.getInfo().containsKey("info_csq_impact") && Arrays.asList("high", "moderate").contains(variant.getInfo().get("info_csq_impact").toLowerCase());
        acmgTieringResult.addExplanation("Impact", variant.getInfo().get("info_csq_impact"));

        boolean lossOfFunctionIsKnownMechanism = false;
        if (variant.getInfo().containsKey("info_csq_symbol")) {
            String gene = variant.getInfo().get("info_csq_symbol");

            int numberOfPathogenicOrLikelyPathogenicNullVariants = clinvar.countPathogenicOrLikelyPathogenicNullVariants(gene);
            acmgTieringResult.addExplanation("Gene", gene);
            acmgTieringResult.addExplanation("Number of pathogenic or likely pathogenic null variants on gene", numberOfPathogenicOrLikelyPathogenicNullVariants);

            boolean hasAtLeastOnePathogenicOrLikelyPathogenicNullVariant = numberOfPathogenicOrLikelyPathogenicNullVariants > 0;
            lossOfFunctionIsKnownMechanism = hasAtLeastOnePathogenicOrLikelyPathogenicNullVariant;
        }


        //TODO check if impactful should contain missense or not (currently it does while the .txt does not)
        //TODO check if this should be && inClinvarAsPathogenicOrLikelyPathogenic
        //TODO test
        boolean pvs1Applies = impactful && lossOfFunctionIsKnownMechanism;
        return acmgTieringResult.setTierApplies(pvs1Applies);
    }

    /**
     * Same amino acid change as a previously established pathogenic variant
     * regardless of nucleotide change
     * Example:	Val->Leu caused by either G>C or G>T in the same codon
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public AcmgTieringResult isPS1(Variant variant) {
        //in the clinvar file '=' which can be a part of HGVSp is stored as %3D
        //chrom can be 1, 2 ...  X and Y and MT
        AcmgTieringResult acmgTieringResult = new AcmgTieringResult();

        //check if the protein change (HGVSp) is known to ClinVar as Pathogenic or Likely_pathogenic. In that case the nucleotides might be different but the protein is still similar to a pathogenic or likely pathogenic protein
        String hgvspKey = "info_csq_hgvsp";
        if (!variant.getInfo().containsKey(hgvspKey)) {
            return new AcmgTieringResult(false);
        }
        String hgvsp = variant.getInfo().get(hgvspKey);
        if (hgvsp == null) {
            return new AcmgTieringResult(false);
        }

        List<GenomicPosition> result = clinvar.findPathogenics(variant.getChrom().replace("chr", ""), variant.getPos(), variant.getAlt());
        boolean identicalFoundInClinvar = result != null && !result.isEmpty();
        if (identicalFoundInClinvar) {
            acmgTieringResult.addExplanation("Exact matches found for pathogenic variants", result.stream().map(GenomicPosition::toString).collect(Collectors.joining(", ")));
        }

        List<GenomicPosition> hgvspResult = clinvar.findPathogenicsByHgvsP(hgvsp);
        boolean matchingHgvspFound = hgvspResult != null && !hgvspResult.isEmpty();
        if (matchingHgvspFound) {
            acmgTieringResult.addExplanation("Pathogenic variants found with the same amino acid change", hgvspResult.stream().map(GenomicPosition::toString).collect(Collectors.joining(", ")));
        }

        boolean ps1Applies = matchingHgvspFound || identicalFoundInClinvar;
        return acmgTieringResult.setTierApplies(ps1Applies);
    }

    /**
     * De novo (both maternity and paternity confirmed) in a patient with the
     * disease and no family history
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public AcmgTieringResult isPS2(Variant variant) {
        AcmgTieringResult acmgTieringResult = new AcmgTieringResult();

        AcmgTieringResult pm6 = isPM6(variant);
        boolean ps2Applies = pm6.isTierApplies() && humansDTO.isParentHoodConfirmed();

        acmgTieringResult.setExplanation(pm6.getExplanation());
        acmgTieringResult.addExplanation("Parenthood confirmed", "By researcher");

        return acmgTieringResult.setTierApplies(ps2Applies);
    }

    /**
     * Well-established in vitro or in vivo functional studies supportive of a
     * damaging effect on the gene or gene product
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public AcmgTieringResult isPS3(Variant variant) {
        //not possible
        return new AcmgTieringResult(false);
    }

    public AcmgTieringResult isPS4(Variant variant) {
        //not possible
        return new AcmgTieringResult(false);
    }

    /**
     * Located in a mutational hot spot and/or critical and well-established
     * functional domain (e.g. active site of an enzyme) without benign variation
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public AcmgTieringResult isPM1(Variant variant) {
        //TODO
        // true wenn PFAM in variante in smartclinvar.vcf enthalten ist

        //TODO this is possible

        //TODO franklin:
        // Non-truncating non-synonymous variant &&
        // Exonic hotspot
        // 5 pathogenic or likely pathogenic reported variants were found in a 15bp region surrounding this variant in exon 5 within the region 135800973-135800988 without any missense benign variant


        //TODO final: (Non-truncating non-synonymous variant) && check all mutations for this (area=gene? or +-15bp) without any of them being non p/lp

        AcmgTieringResult acmgTieringResult = new AcmgTieringResult();

        boolean onlyPathogenicLikelyPathogenicInArea = true;
        int area = 15;
        int pos = -2 * area;

        try {
            pos = Integer.parseInt(variant.getPos());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        for (int i = pos - area; i <= pos + area; i++) {
            List<GenomicPosition> nonPathogenics = clinvar.findNonPathogenics(variant.getChrom(), i + "");
            if (nonPathogenics != null && nonPathogenics.size() > 0) {
                onlyPathogenicLikelyPathogenicInArea = false;
                break;
            }
        }
        acmgTieringResult.addExplanation("No benign variations in this area", variant.getChrom() + ":" + pos + " +/-" + area + "bp");

        boolean impactful = variant.getInfo().containsKey("info_csq_impact") && Arrays.asList("high", "moderate").contains(variant.getInfo().get("info_csq_impact").toLowerCase());
        acmgTieringResult.addExplanation("Impact", variant.getInfo().get("info_csq_impact"));

        boolean onGene = variant.getInfo().containsKey("info_csq_symbol");
        if (onGene) {
            acmgTieringResult.addExplanation("On gene", variant.getInfo().get("info_csq_symbol"));
        }

        boolean pm1Applies = onGene && impactful && onlyPathogenicLikelyPathogenicInArea;
        return acmgTieringResult.setTierApplies(pm1Applies);
    }

    /**
     * Absent from controls (or at extremely low frequency if recessive)
     * in Exome Sequencing Project, 1000 Genomes or ExAC
     *
     * @param variant the variant to be checked
     * @return true if the variant confirms with the tier
     */
    public AcmgTieringResult isPM2(Variant variant) {
        AcmgTieringResult acmgTieringResult = new AcmgTieringResult();
        List<String> alleleFrequencies = Arrays.asList("info_af_afr", "info_af_amr", "info_af_asj", "info_af_eas", "info_af_nfe", "info_af_oth", "info_af_raw", "info_controls_af_popmax", "info_csq_af", "info_csq_gnomad_af", "info_csq_gnomad_afr_af", "info_csq_gnomad_amr_af", "info_csq_gnomad_asj_af", "info_csq_gnomad_eas_af", "info_csq_gnomad_fin_af", "info_csq_gnomad_nfe_af", "info_csq_gnomad_oth_af", "info_csq_gnomad_sas_af", "info_csq_gnomadg_ac", "info_csq_gnomadg_af", "info_csq_gnomadg_af_afr", "info_csq_gnomadg_af_amr", "info_csq_gnomadg_af_asj", "info_csq_gnomadg_af_eas", "info_csq_gnomadg_af_fin", "info_csq_gnomadg_af_nfe", "info_csq_gnomadg_af_oth", "info_csq_gnomadg_controls_ac", "info_csq_gnomadg_controls_af", "info_csq_gnomadg_controls_nhomalt", "info_csq_gnomadg_nhomalt_nfe");

        String maxPop = "Not found in any populations";
        double maxPopValue = 0.0;

        for (String key : alleleFrequencies) {
            if (variant.getInfo().containsKey(key) && variant.getInfo().get(key) != null) {
                String value = variant.getInfo().get(key);
                //check if value is larger than a threshold, if yes return false
                try {
                    double doubleValue = Double.parseDouble(value);
                    if (doubleValue > 0.001) { //TODO maybe adapt this threshold
                        return new AcmgTieringResult(false);
                    }

                    if (doubleValue > maxPopValue) {
                        maxPopValue = doubleValue;
                        maxPop = key;
                    }

                } catch (NumberFormatException e) {
                    //System.out.println("Cannot parse AF " + value);
                }
            }
        }

        acmgTieringResult.addExplanation("Population with highest frequency", maxPop);
        acmgTieringResult.addExplanation("Highest allele frequency throughout populations", maxPopValue);
        return acmgTieringResult.setTierApplies(true);
    }

    public AcmgTieringResult isPM3(Variant variant) {
        //not possible
        return new AcmgTieringResult(false);
    }

    /**
     * Protein length changes due to in-frame deletions/insertions in a non-repeat
     * region or stop-loss variants
     *
     * @param variant the variant to be checked
     * @return true if the variant confirms with the tier
     */
    public AcmgTieringResult isPM4(Variant variant) {
        AcmgTieringResult acmgTieringResult = new AcmgTieringResult();

        //info_csq_consequence
        String key = "info_csq_consequence";
        if (variant.getInfo().containsKey(key)) {
            String value = variant.getInfo().get(key);
            if (value.contains("inframe_insertion") || value.contains("inframe_deletion")) {
                acmgTieringResult.addExplanation("Variant type", value);
                return acmgTieringResult.setTierApplies(true); //TODO has to be in a non-repeat regions
                //TODO UCSC repeat tracks
            } else if (value.contains("stop_lost")) {
                acmgTieringResult.addExplanation("Variant type", value);
                return acmgTieringResult.setTierApplies(true);
            }
        }

        return acmgTieringResult.setTierApplies(false);
    }

    /**
     * Novel missense change at an amino acid residue where a different
     * missense change determined to be pathogenic has been seen before
     * Example: Arg156His is pathogenic; now you observe Arg156Cys
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public AcmgTieringResult isPM5(Variant variant) {
        AcmgTieringResult acmgTieringResult = new AcmgTieringResult();

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
            acmgTieringResult.addExplanation("Pathogenic variant on same chromosome and position found", resultNucleotide.stream().map(GenomicPosition::toString).collect(Collectors.joining(", ")));
        }

        if (resultProtein != null) {
            foundResults += resultProtein.size();
            acmgTieringResult.addExplanation("Similar protein as produced by pathogenic variant found", resultProtein.stream().map(GenomicPosition::toString).collect(Collectors.joining(", ")));
        }

        boolean pm5Applies = foundResults > 0;
        return acmgTieringResult.setTierApplies(pm5Applies);
    }

    /**
     * Assumed de novo, but without confirmation of paternity and maternity
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public AcmgTieringResult isPM6(Variant variant) {
        AcmgTieringResult acmgTieringResult = new AcmgTieringResult();

        //get rid of non trios
        if (humansDTO == null || humansDTO.getHumans().size() != 3) {
            return new AcmgTieringResult(false);
        }

        //get rid of trios where the parents are affected
        for (HumanDTO humanDTO : humansDTO.getHumans()) {
            boolean isHealthyNonIndexPerson = !humanDTO.getIsAffected() && !humanDTO.getIsIndex();
            boolean isAffectedIndexPerson = humanDTO.getIsAffected() && humanDTO.getIsIndex();
            if (!(isHealthyNonIndexPerson || isAffectedIndexPerson)) {
                return new AcmgTieringResult(false);
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
                    return new AcmgTieringResult(false);
                }
                acmgTieringResult.addExplanation("Parent is not affected", humanDTO.getPseudonym());
            }
        }

        return acmgTieringResult.setTierApplies(true);
    }

    /**
     * Co-segregation with disease in multiple affected family members in a gene
     * definitively known to cause the disease
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public AcmgTieringResult isPP1(Variant variant) {
        AcmgTieringResult acmgTieringResult = new AcmgTieringResult();
        //needs trio or at least a multi person sample. E. g. mother is sick and child -> if mother has mutation and child has mutation it is this tier

        //get rid of non multi person sample
        if (humansDTO == null || humansDTO.getHumans().size() <= 1) {
            return new AcmgTieringResult(false);
        }

        HumanDTO indexPerson = null;
        for (HumanDTO humanDTO : humansDTO.getHumans()) {
            if (humanDTO.getIsIndex()) {
                indexPerson = humanDTO;
            }
        }

        if (indexPerson == null) {
            return new AcmgTieringResult(false);
        }
        String genotypeIndexPerson = variant.getInfo().get("format_" + indexPerson.getPseudonym() + "_gt");

        //TODO assumes index is affected
        if (genotypeIndexPerson.contains("0")) {
            //index person is 0/1 -> tier is only true if everyone who is affected has 1/1 or 0/1 and everyone who is not affected has 0/0
            for (HumanDTO humanDTO : humansDTO.getHumans()) {
                if (!humanDTO.getIsIndex()) {
                    String genotypeParent = variant.getInfo().get("format_" + humanDTO.getPseudonym() + "_gt");
                    boolean hasMutation = !(genotypeParent.equals("0/0") || genotypeParent.equals("0|0"));
                    if ((hasMutation && !humanDTO.getIsAffected()) || (!hasMutation && humanDTO.getIsAffected())) {
                        return new AcmgTieringResult(false);
                    }
                }
            }
            acmgTieringResult.addExplanation("Co-segregation with disease", "Index patient is 0/1, everyone who is affected has 1/1 or 0/1");
            return acmgTieringResult.setTierApplies(true);
        } else {
            //index person is 1/1 -> tier is only true if everyone who is affected has 1/1 and everyone who is not affected has 0/0 or 0/1
            for (HumanDTO humanDTO : humansDTO.getHumans()) {
                if (!humanDTO.getIsIndex()) {
                    String genotypeParent = variant.getInfo().get("format_" + humanDTO.getPseudonym() + "_gt");
                    boolean parent11 = !(genotypeParent.contains("0"));
                    if ((parent11 && !humanDTO.getIsAffected()) || (!parent11 && humanDTO.getIsAffected())) {
                        return new AcmgTieringResult(false);
                    }
                }
            }
            acmgTieringResult.addExplanation("Co-segregation with disease", "Index patient is 1/1, everyone not affected has 0/0 or 0/1");
            return acmgTieringResult.setTierApplies(true);
        }
    }

    /**
     * Missense variant in a gene with low rate of benign missense mutations and for which missense mutation is a common mechanism of a disease
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public AcmgTieringResult isPP2(Variant variant) {
        AcmgTieringResult acmgTieringResult = new AcmgTieringResult();
        //TODO test
        //needs to be on a gene which has three times more path. or likely path. missense mutations than benign missense mutations and needs to be a missense mutation
        if (variant.getInfo().containsKey("info_csq_symbol")) {
            String gene = variant.getInfo().get("info_csq_symbol");
            double factor = 3.0;
            boolean mostlyPathogenicOrLikelyPathogenicMissenseMutations = clinvar.countPathogenicOrLikelyPathogenicMissenseVariants(gene) >= factor * clinvar.countBenignMissenseVariants(gene);

            acmgTieringResult.addExplanation("Gene", gene);
            acmgTieringResult.addExplanation("Pathogenic or likely pathogenic missense variants", clinvar.countPathogenicOrLikelyPathogenicMissenseVariants(gene));
            acmgTieringResult.addExplanation("Benign missense variants", clinvar.countBenignMissenseVariants(gene));

            boolean isMissense = false;
            String key = "info_csq_consequence";
            if (variant.getInfo().containsKey(key)) {
                String value = variant.getInfo().get(key);
                isMissense = value.equals("missense_variant");
                acmgTieringResult.addExplanation("Consequence", value);
            }

            boolean pp2Applies = isMissense && mostlyPathogenicOrLikelyPathogenicMissenseMutations;
            return acmgTieringResult.setTierApplies(pp2Applies);
        }

        return new AcmgTieringResult(false);
    }

    /**
     * Multiple lines of computational evidence support a deleterious effect on
     * the gene or gene product (conservation, evolutionary, splicing impact, etc)
     *
     * @param variant the variant to be checked
     * @return true if the variants confirms with the tier
     */
    public AcmgTieringResult isPP3(Variant variant) {
        //TODO check thresholds
        //TODO make sure these are not co-dependent
        //TODO what about info_cadd_phred, info_caddind_phred", "info_caddind_raw
        AcmgTieringResult acmgTieringResult = new AcmgTieringResult();

        int numberOfScores = 0;
        int numberOfPathogenicScores = 0;

        //info_csq_gerp_rs > 4
        String gerpKey = "info_csq_gerp_rs";
        if (variant.getInfo().containsKey(gerpKey) && variant.getInfo().get(gerpKey) != null) {
            numberOfScores++;
            try {
                if (Double.parseDouble(variant.getInfo().get(gerpKey)) >= 4.0) {
                    numberOfPathogenicScores++;
                    acmgTieringResult.addExplanation("GERP", Double.parseDouble(variant.getInfo().get(gerpKey)));
                }
            } catch (Exception ex) {
                //System.out.println("Cannot parse " + variant.getInfo().get(gerpKey));
            }
        }

        //info_cadd_raw > 10
        String caddKey = "info_cadd_phred";
        if (variant.getInfo().containsKey(caddKey) && variant.getInfo().get(caddKey) != null) {
            numberOfScores++;
            try {
                if (Double.parseDouble(variant.getInfo().get(caddKey)) >= 25.3) {
                    numberOfPathogenicScores++;
                    acmgTieringResult.addExplanation("CADD", Double.parseDouble(variant.getInfo().get(caddKey)));
                }
            } catch (Exception ex) {
                //System.out.println("Cannot parse " + variant.getInfo().get(caddKey));
            }
        }

        //info_csq_polyphen tolower contains 'damaging'
        String polyphenKey = "info_csq_polyphen";
        if (variant.getInfo().containsKey(polyphenKey) && variant.getInfo().get(polyphenKey) != null) {
            numberOfScores++;
            if (variant.getInfo().get(polyphenKey).toLowerCase().contains("damaging")) {
                numberOfPathogenicScores++;
                acmgTieringResult.addExplanation("Polyphen", variant.getInfo().get(polyphenKey));
            }
        }

        //OK info_csq_sift = deleterious
        String siftKey = "info_csq_sift";
        if (variant.getInfo().containsKey(siftKey) && variant.getInfo().get(siftKey) != null) {
            numberOfScores++;
            if (variant.getInfo().get(siftKey).toLowerCase().contains("deleterious")) {
                numberOfPathogenicScores++;
                acmgTieringResult.addExplanation("SIFT", variant.getInfo().get(siftKey));
            }
        }

        //if there is at least a score and the majority of scores are pathogenic
        boolean pp3Applies = numberOfScores > 0 && 2 * numberOfPathogenicScores >= numberOfScores;
        return acmgTieringResult.setTierApplies(pp3Applies);
    }

    public AcmgTieringResult isPP4(Variant variant) {
        return new AcmgTieringResult(false); //TODO either for all or for none, impossible
    }

    public AcmgTieringResult isPP5(Variant variant) {
        //TODO should this just check clinvar?
        /*List<GenomicPosition> result = clinvar.findPathogenics(variant.getChrom().replace("chr", ""), variant.getPos(), variant.getAlt());
        return result != null && !result.isEmpty();*/
        return new AcmgTieringResult(false);
    }

    /**
     * Allele frequency is above 5% in Exome Sequencing Project, 1000 Genomes,
     * or ExAC
     *
     * @param variant the variant to be checked
     * @return ture if the variant is the tier
     */
    public AcmgTieringResult isBA1(Variant variant) {
        AcmgTieringResult acmgTieringResult = new AcmgTieringResult();

        List<String> alleleFrequencies = Arrays.asList("info_af_raw", "info_controls_af_popmax", "info_af_afr", "info_af_amr", "info_af_asj", "info_af_eas", "info_af_nfe", "info_af_oth", "info_csq_af", "info_csq_gnomadg_ac", "info_csq_gnomadg_af", "info_csq_gnomadg_controls_ac", "info_csq_gnomadg_controls_af", "info_csq_gnomadg_controls_nhomalt", "info_csq_gnomadg_nhomalt_nfe", "info_csq_gnomadg_af_afr", "info_csq_gnomadg_af_amr", "info_csq_gnomadg_af_asj", "info_csq_gnomadg_af_eas", "info_csq_gnomadg_af_fin", "info_csq_gnomadg_af_nfe", "info_csq_gnomadg_af_oth", "info_csq_gnomad_af", "info_csq_gnomad_afr_af", "info_csq_gnomad_amr_af", "info_csq_gnomad_asj_af", "info_csq_gnomad_eas_af", "info_csq_gnomad_fin_af", "info_csq_gnomad_nfe_af", "info_csq_gnomad_oth_af", "info_csq_gnomad_sas_af");

        for (String key : alleleFrequencies) {
            if (variant.getInfo().containsKey(key)) {
                String value = variant.getInfo().get(key);
                try {
                    double doubleValue = Double.parseDouble(value);
                    if (doubleValue > 0.05) {
                        acmgTieringResult.addExplanation("Allele frequency exceeds 5% in population", key);
                        acmgTieringResult.addExplanation("Allele frequency in population", doubleValue);
                        return acmgTieringResult.setTierApplies(true);
                    }
                } catch (Exception ex) {
                    //System.out.println("Cannot parse AF " + value);
                }
            }
        }

        return new AcmgTieringResult(false);
    }

    public AcmgTieringResult isBS1(Variant variant) {
        AcmgTieringResult acmgTieringResult = new AcmgTieringResult();

        List<String> alleleFrequencies = Arrays.asList("info_af_raw", "info_controls_af_popmax", "info_af_afr", "info_af_amr", "info_af_asj", "info_af_eas", "info_af_nfe", "info_af_oth", "info_csq_af", "info_csq_gnomadg_ac", "info_csq_gnomadg_af", "info_csq_gnomadg_controls_ac", "info_csq_gnomadg_controls_af", "info_csq_gnomadg_controls_nhomalt", "info_csq_gnomadg_nhomalt_nfe", "info_csq_gnomadg_af_afr", "info_csq_gnomadg_af_amr", "info_csq_gnomadg_af_asj", "info_csq_gnomadg_af_eas", "info_csq_gnomadg_af_fin", "info_csq_gnomadg_af_nfe", "info_csq_gnomadg_af_oth", "info_csq_gnomad_af", "info_csq_gnomad_afr_af", "info_csq_gnomad_amr_af", "info_csq_gnomad_asj_af", "info_csq_gnomad_eas_af", "info_csq_gnomad_fin_af", "info_csq_gnomad_nfe_af", "info_csq_gnomad_oth_af", "info_csq_gnomad_sas_af");

        for (String key : alleleFrequencies) {
            if (variant.getInfo().containsKey(key)) {
                String value = variant.getInfo().get(key);
                try {
                    double doubleValue = Double.parseDouble(value);
                    if (doubleValue > 0.015) {
                        acmgTieringResult.addExplanation("Allele frequency exceeds 1.5% in population", key);
                        acmgTieringResult.addExplanation("Allele frequency in population", doubleValue);
                        return acmgTieringResult.setTierApplies(true);
                    }
                } catch (Exception ex) {
                    //System.out.println("Cannot parse AF " + value);
                }
            }
        }

        return new AcmgTieringResult(false);
    }

    public AcmgTieringResult isBS2(Variant variant) {
        return new AcmgTieringResult(false); //TODO gnomad tells us how many homozygote are
        // true if >= people are homozygote in gnomad
        //TODO probably impossible
    }

    public AcmgTieringResult isBS3(Variant variant) {
        return new AcmgTieringResult(false); //TODO will never appear
    }

    /**
     * Lack of segregation in affected members of a family
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public AcmgTieringResult isBS4(Variant variant) {
        AcmgTieringResult acmgTieringResult = new AcmgTieringResult();

        //needs trio or at least a multi person sample. True if a healthy parent has the same mutation
        //get rid of non multi person sample
        if (humansDTO == null || humansDTO.getHumans().size() <= 1) {
            return new AcmgTieringResult(false);
        }

        HumanDTO indexPerson = null;
        for (HumanDTO humanDTO : humansDTO.getHumans()) {
            if (humanDTO.getIsIndex()) {
                indexPerson = humanDTO;
            }
        }

        if (indexPerson == null) {
            return new AcmgTieringResult(false);
        }
        String genotypeIndexPerson = variant.getInfo().get("format_" + indexPerson.getPseudonym() + "_gt");

        //TODO assumes index is affected
        if (genotypeIndexPerson.contains("0")) {
            //index person is 0/1, if parents are 1/1 or 0/1 but not affected this tier is true
            for (HumanDTO humanDTO : humansDTO.getHumans()) {
                if (!humanDTO.getIsIndex()) {
                    String genotypeParent = variant.getInfo().get("format_" + humanDTO.getPseudonym() + "_gt");
                    boolean hasMutation = !(genotypeParent.equals("0/0") || genotypeParent.equals("0|0"));
                    if ((hasMutation && !humanDTO.getIsAffected()) || (!hasMutation && humanDTO.getIsAffected())) {
                        acmgTieringResult.addExplanation("Lack of segregation for", humanDTO.getPseudonym());
                        return acmgTieringResult.setTierApplies(true);
                    }
                }
            }
            return new AcmgTieringResult(false);
        } else {
            //index person is 1/1, if parents are 1/1 but not affected this tier is true
            for (HumanDTO humanDTO : humansDTO.getHumans()) {
                if (!humanDTO.getIsIndex()) {
                    String genotypeParent = variant.getInfo().get("format_" + humanDTO.getPseudonym() + "_gt");
                    boolean parent11 = !genotypeParent.contains("0");
                    if ((parent11 && !humanDTO.getIsAffected()) || (!parent11 && humanDTO.getIsAffected())) {
                        acmgTieringResult.addExplanation("Lack of segregation for", humanDTO.getPseudonym());
                        return acmgTieringResult.setTierApplies(true);
                    }
                }
            }
            return new AcmgTieringResult(false);
        }
    }

    /**
     * Missense variant in a gene for which loss of function is the known mechanism of disease
     *
     * @param variant the variant to be checked
     * @return true if the tier applies
     */
    public AcmgTieringResult isBP1(Variant variant) {
        AcmgTieringResult acmgTieringResult = new AcmgTieringResult();

        boolean isMissense = false;
        String key = "info_csq_consequence";
        if (variant.getInfo().containsKey(key)) {
            String value = variant.getInfo().get(key);
            isMissense = value.equals("missense_variant");
            acmgTieringResult.addExplanation("Variant type", value);
        }

        boolean nullVariantsIsOnlyKnownMechanismOfDisease = false;
        if (variant.getInfo().containsKey("info_csq_symbol")) {
            String gene = variant.getInfo().get("info_csq_symbol");
            //only if there are no non null pathogenic variants in clinvar, then it's the only mechanism
            nullVariantsIsOnlyKnownMechanismOfDisease = clinvar.countPathogenicOrLikelyPathogenicNonNullVariants(gene) == 0;
            acmgTieringResult.addExplanation("Gene", gene);
            acmgTieringResult.addExplanation("Pathogenic or likely pathogenic non null variants found for gene", "0");
        }

        boolean bp1Applies = isMissense && nullVariantsIsOnlyKnownMechanismOfDisease;
        return acmgTieringResult.setTierApplies(bp1Applies);
    }

    public AcmgTieringResult isBP2(Variant variant) {
        return new AcmgTieringResult(false); //TODO impossible(?) check haplotype? could be possible for trios
    }

    public AcmgTieringResult isBP3(Variant variant) {
        return new AcmgTieringResult(false); //TODO difficult maybe
    }

    public AcmgTieringResult isBP4(Variant variant) {
        AcmgTieringResult acmgTieringResult = new AcmgTieringResult();

        int numberOfScores = 0;
        int numberOfBenignScores = 0;

        //info_csq_gerp_rs > 4
        String gerpKey = "info_csq_gerp_rs";
        if (variant.getInfo().containsKey(gerpKey) && variant.getInfo().get(gerpKey) != null) {
            numberOfScores++;
            try {
                if (Double.parseDouble(variant.getInfo().get(gerpKey)) <= -4.54) {
                    numberOfBenignScores++;
                    acmgTieringResult.addExplanation("GERP", Double.parseDouble(variant.getInfo().get(gerpKey)));
                }
            } catch (Exception ex) {
                //System.out.println("Cannot parse " + variant.getInfo().get(gerpKey));
            }
        }

        //info_cadd_raw > 10
        String caddKey = "info_cadd_phred";
        if (variant.getInfo().containsKey(caddKey) && variant.getInfo().get(caddKey) != null) {
            numberOfScores++;
            try {
                if (Double.parseDouble(variant.getInfo().get(caddKey)) <= 17.3) {
                    numberOfBenignScores++;
                    acmgTieringResult.addExplanation("CADD", Double.parseDouble(variant.getInfo().get(caddKey)));
                }
            } catch (Exception ex) {
                //System.out.println("Cannot parse " + variant.getInfo().get(caddKey));
            }
        }

        //info_csq_polyphen tolower contains 'damaging'
        String polyphenKey = "info_csq_polyphen";
        if (variant.getInfo().containsKey(polyphenKey) && variant.getInfo().get(polyphenKey) != null) {
            numberOfScores++;
            if (variant.getInfo().get(polyphenKey).toLowerCase().contains("benign")) {
                numberOfBenignScores++;
                acmgTieringResult.addExplanation("Polyphen", variant.getInfo().get(polyphenKey));
            }
        }

        //OK info_csq_sift = deleterious
        String siftKey = "info_csq_sift";
        if (variant.getInfo().containsKey(siftKey) && variant.getInfo().get(siftKey) != null) {
            numberOfScores++;
            if (variant.getInfo().get(siftKey).toLowerCase().contains("tolerated")) {
                numberOfBenignScores++;
                acmgTieringResult.addExplanation("SIFT", variant.getInfo().get(siftKey));
            }
        }

        //if there is at least a score and all are benign
        boolean bp4Applies = numberOfScores > 0 && numberOfBenignScores == numberOfScores;
        return acmgTieringResult.setTierApplies(bp4Applies);
    }

    public AcmgTieringResult isBP5(Variant variant) {
        return new AcmgTieringResult(false); //TODO necessary data not available yet
    }

    public AcmgTieringResult isBP6(Variant variant) {
        return new AcmgTieringResult(false); //TODO impossible
    }

    public AcmgTieringResult isBP7(Variant variant) {
        return new AcmgTieringResult(false); //TODO similar amino acid and spliceAI non pathogenic(?) EnsemblImpact also GERP
    }

}
