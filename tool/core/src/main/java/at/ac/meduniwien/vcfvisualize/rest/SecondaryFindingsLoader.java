package at.ac.meduniwien.vcfvisualize.rest;

import at.ac.meduniwien.vcfvisualize.data.VariantProvider;
import at.ac.meduniwien.vcfvisualize.knowledgebase.acmg.SecondaryFindingDefinition;
import at.ac.meduniwien.vcfvisualize.knowledgebase.acmg.SecondaryFindings;
import at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar.Clinvar;
import at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar.GenomicPosition;
import at.ac.meduniwien.vcfvisualize.knowledgebase.hpo.Hpo;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.PanelApp;
import at.ac.meduniwien.vcfvisualize.model.Filter;
import at.ac.meduniwien.vcfvisualize.model.User;
import at.ac.meduniwien.vcfvisualize.model.Variant;
import at.ac.meduniwien.vcfvisualize.model.expression.BasicExpression;
import at.ac.meduniwien.vcfvisualize.model.expression.EnumExpression;
import at.ac.meduniwien.vcfvisualize.model.expression.Expression;
import at.ac.meduniwien.vcfvisualize.model.expression.IntermediateExpression;
import at.ac.meduniwien.vcfvisualize.processor.acmg.*;
import at.ac.meduniwien.vcfvisualize.rest.dto.*;
import at.ac.meduniwien.vcfvisualize.security.AuthenticationService;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class SecondaryFindingsLoader {

    String symbolKey = "info_csq_symbol";
    static final List<String> DEFAULT_INFO_FIELDS = Arrays.asList("info_csq_canonical", "info_csq_hgvsc", "info_csq_hgvsg");

    @Autowired
    VariantProvider variantProvider; //TODO replace with VariantProvider

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    Clinvar clinvar;

    @Autowired
    AcmgTierer acmgTierer;

    @Autowired
    SecondaryFindings secondaryFindings;

    @CrossOrigin
    @PostMapping("/secondaryfindings/load")
    public SecondaryFindingsQueryResultDTO load(@RequestBody SecondaryFindingsLoadRequestDTO secondaryFindingsLoadRequestDTO) {
        User user = authenticationService.getUserForToken(secondaryFindingsLoadRequestDTO.token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        String sample = secondaryFindingsLoadRequestDTO.sample;
        if (!variantProvider.isValidSampleId(sample)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        if (!authenticationService.userCanAccessSample(user, sample)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        SecondaryFindingsQueryResultDTO queryResultDTO = new SecondaryFindingsQueryResultDTO();
        long startTime = System.currentTimeMillis();

        List<SecondaryFindingDefinition> secondaryFindingsDefinitions = secondaryFindings.getDefinitions();
        HashMap<String, SecondaryFindingDefinition> secondaryFindingDefinitionByGene = new HashMap<>();
        for (SecondaryFindingDefinition sfd : secondaryFindingsDefinitions) {
            if (secondaryFindingDefinitionByGene.containsKey(sfd.getGene())) {
                System.out.println("Secondary finding gene " + sfd.getGene() + " already defined!");
            }
            secondaryFindingDefinitionByGene.put(sfd.getGene(), sfd);
            queryResultDTO.getSecondaryFindingGeneSummaryDTOs().put(sfd.getGene(), new SecondaryFindingGeneSummaryDTO(sfd.getGene()));
        }

        List<Variant> variants = variantProvider.getVariants(user, sample, new Filter(buildExpressionByGenes(secondaryFindingsDefinitions.stream().map(SecondaryFindingDefinition::getGene).collect(Collectors.toList())), new LinkedList<>(), 0), false);

        List<SecondaryFindingVariantDTO> variantsToReport = new LinkedList<>();
        for (Variant variant : variants) {
            if (variant.getInfo().containsKey("info_csq_symbol")) {
                String gene = variant.getInfo().get("info_csq_symbol");
                SecondaryFindingDefinition secondaryFindingDefinition = secondaryFindingDefinitionByGene.get(gene);

                //check clinvar
                List<GenomicPosition> clinvarResult = clinvar.findPathogenics(variant.getChrom(), variant.getPos(), variant.getAlt());
                boolean clinvarPathogenicFound = clinvarResult != null && clinvarResult.size() > 0;
                boolean clinvarPathogenicAnnotated = false;
                if (variant.getInfo().containsKey("info_csq_clinvar_clnsig") && variant.getInfo().get("info_csq_clinvar_clnsig") != null) {
                    System.out.println(variant.getInfo().get("info_csq_clinvar_clnsig"));
                    clinvarPathogenicAnnotated = variant.getInfo().get("info_csq_clinvar_clnsig").toLowerCase().contains("pathogenic"); //targets both pathogenic and likely pathogenic
                }
                boolean clinvarPositive = clinvarPathogenicFound || clinvarPathogenicAnnotated;

                //tier the variant
                List<AcmgTieringResult> acmgTieringResults = acmgTierer.performAcmgTiering(variant, false, false, null);
                AcmgClassificationResult acmgClassificationResult = acmgTierer.performAcmgClassification(acmgTieringResults.stream().map(AcmgTieringResult::getTier).collect(Collectors.toList()));

                //decide if the variant should be reported based on clinvar, tiering and secondary findings definition
                boolean homozygousAffected = StringUtils.countMatches(variant.getInfo().get("format_" + sample + "_gt"), "1") >= 2;

                boolean genotypeMatches = secondaryFindingDefinition.getInheritance().toUpperCase().equals("AD")
                                || secondaryFindingDefinition.getInheritance().toUpperCase().equals("AR") && homozygousAffected
                                || secondaryFindingDefinition.getInheritance().toUpperCase().equals("XL") && variant.getChrom().toUpperCase().contains("X") && secondaryFindingsLoadRequestDTO.hasSingleXChromosome;

                //TODO this might cause a problem - ACMG is not clear on this, should we only consider ClinVar known as know or also ACMG pathogenic
                boolean pathogenicityMatches = secondaryFindingDefinition.isReportKnownPathogenic() && clinvarPositive
                        || secondaryFindingDefinition.isReportExpectedPathogenic() && (clinvarPositive || acmgClassificationResult.getAcmgClassification().equals(AcmgClassification.PATHOGENIC) || acmgClassificationResult.getAcmgClassification().equals(AcmgClassification.LIKELY_PATHOGENIC));

                boolean pathogenicFinding = genotypeMatches && pathogenicityMatches;

                if (pathogenicFinding) {
                    SecondaryFindingVariantDTO variantDTO = new SecondaryFindingVariantDTO(variant);
                    variantDTO.setAcmgClassificationResult(acmgClassificationResult);
                    variantDTO.setAcmgTieringResults(acmgTieringResults);
                    variantDTO.setSecondaryFindingDefinition(secondaryFindingDefinition);
                    variantDTO.setClinvarPositive(clinvarPositive);
                    variantsToReport.add(variantDTO);
                }

                //increase counter
                queryResultDTO.increaseVariantCounter(gene, pathogenicFinding);
            }
        }

        queryResultDTO.setVariants(variantsToReport);
        queryResultDTO.elapsedMilliseconds = System.currentTimeMillis() - startTime;

        return queryResultDTO;
    }

    private Expression buildExpressionByGenes(List<String> genes) {
        Expression geneExpression = new EnumExpression(symbolKey, genes);
        /*Expression afExpression = new BasicExpression<Double>("info_af_raw", "<", 0.05);
        Expression impactExpression = new EnumExpression("info_csq_impact", Arrays.asList("HIGH", "LOW", "MODERATE", "null"));
        return new IntermediateExpression(new ArrayList<>(Arrays.asList("AND", "AND")), new ArrayList<>(Arrays.asList(geneExpression, afExpression, impactExpression)));*/
        return geneExpression;
    }

}
