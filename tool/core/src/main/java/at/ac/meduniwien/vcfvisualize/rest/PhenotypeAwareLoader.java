package at.ac.meduniwien.vcfvisualize.rest;

import at.ac.meduniwien.vcfvisualize.data.VariantProvider;
import at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar.Clinvar;
import at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar.GenomicPosition;
import at.ac.meduniwien.vcfvisualize.knowledgebase.hpo.Hpo;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.PanelApp;
import at.ac.meduniwien.vcfvisualize.mocking.MockVariantProvider;
import at.ac.meduniwien.vcfvisualize.model.Filter;
import at.ac.meduniwien.vcfvisualize.model.User;
import at.ac.meduniwien.vcfvisualize.model.Variant;
import at.ac.meduniwien.vcfvisualize.model.expression.BasicExpression;
import at.ac.meduniwien.vcfvisualize.model.expression.EnumExpression;
import at.ac.meduniwien.vcfvisualize.model.expression.Expression;
import at.ac.meduniwien.vcfvisualize.model.expression.IntermediateExpression;
import at.ac.meduniwien.vcfvisualize.processor.acmg.AcmgTier;
import at.ac.meduniwien.vcfvisualize.processor.acmg.AcmgTierer;
import at.ac.meduniwien.vcfvisualize.processor.acmg.AcmgTieringResult;
import at.ac.meduniwien.vcfvisualize.rest.dto.PhenotypeAwareLoadRequestDTO;
import at.ac.meduniwien.vcfvisualize.rest.dto.PhenotypeAwareQueryResultDTO;
import at.ac.meduniwien.vcfvisualize.rest.dto.PhenotypeAwareVariantDTO;
import at.ac.meduniwien.vcfvisualize.security.AuthenticationService;
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
public class PhenotypeAwareLoader {

    String symbolKey = "info_csq_symbol";
    static final List<String> DEFAULT_INFO_FIELDS = Arrays.asList("info_csq_canonical", "info_csq_hgvsc", "info_csq_hgvsg");

    @Autowired
    VariantProvider variantProvider; //TODO replace with VariantProvider

    @Autowired
    PanelApp panelApp;

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    Hpo hpo;

    @Autowired
    Clinvar clinvar;

    @Autowired
    AcmgTierer acmgTierer;

    @CrossOrigin
    @PostMapping("/phenotypeaware/load")
    public PhenotypeAwareQueryResultDTO load(@RequestBody PhenotypeAwareLoadRequestDTO phenotypeAwareLoadRequestDTO) {
        User user = authenticationService.getUserForToken(phenotypeAwareLoadRequestDTO.token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        String sample = phenotypeAwareLoadRequestDTO.sample;
        if (!variantProvider.isValidSampleId(sample)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        if (!authenticationService.userCanAccessSample(user, sample)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        //TODO empty filters are possible but produce an error e. g. select * from x where ()

        long startTime = System.currentTimeMillis();
        List<GenomicPosition> genomicPositionsByHpo = clinvar.getGenomicPositionsByHpoTerms(phenotypeAwareLoadRequestDTO.hpoTerms);
        System.out.println(genomicPositionsByHpo.size() + " ClinVar entries are associated with the phenotype");
        List<Variant> variantsByHpoTerms = new LinkedList<>();
        if (genomicPositionsByHpo.size() > 0) {
            Filter filterHpoTerms = new Filter(buildExpressionSelectingByGenomicPositions(genomicPositionsByHpo), new LinkedList<>(), 0);
            variantsByHpoTerms = variantProvider.getVariants(user, sample, filterHpoTerms, false);
        }

        List<Variant> variantsByGenes = new LinkedList<>();
        if (phenotypeAwareLoadRequestDTO.genes.size() > 0) {
            Filter filterGenes = new Filter(buildExpressionByGenes(phenotypeAwareLoadRequestDTO.genes), new LinkedList<>(), 0);
            variantsByGenes = variantProvider.getVariants(user, sample, filterGenes, false);
        }

        //TODO should we just tier all variants?
        Set<String> infoFieldsToKeep = new HashSet<>(DEFAULT_INFO_FIELDS);

        PhenotypeAwareQueryResultDTO queryResultDTO = new PhenotypeAwareQueryResultDTO();
        queryResultDTO.setVariants(new LinkedList<>());
        for (Variant variant : variantsByHpoTerms) {
            PhenotypeAwareVariantDTO phenotypeAwareVariantDTO = new PhenotypeAwareVariantDTO();
            phenotypeAwareVariantDTO.setVariant(variant.convertToReducedDTO(infoFieldsToKeep));
            phenotypeAwareVariantDTO.setAcmgTieringResults(acmgTierer.performAcmgTiering(variant, true, false, phenotypeAwareLoadRequestDTO.humansDTO));
            phenotypeAwareVariantDTO.setHpoTermsLeadToDiscovery(List.of());
            queryResultDTO.getVariants().add(phenotypeAwareVariantDTO);
        }

        for (Variant variant : variantsByGenes) {
            PhenotypeAwareVariantDTO phenotypeAwareVariantDTO = new PhenotypeAwareVariantDTO();
            phenotypeAwareVariantDTO.setVariant(variant.convertToReducedDTO(infoFieldsToKeep));
            phenotypeAwareVariantDTO.setAcmgTieringResults(acmgTierer.performAcmgTiering(variant, false, true, phenotypeAwareLoadRequestDTO.humansDTO));
            queryResultDTO.getVariants().add(phenotypeAwareVariantDTO);
        }

        //add classifications for tiers
        queryResultDTO.getVariants().forEach(v -> v.setAcmgClassificationResult(acmgTierer.performAcmgClassification(v.getAcmgTieringResults().stream().map(AcmgTieringResult::getTier).collect(Collectors.toList()))));

        queryResultDTO.getVariants().sort((a, b) -> {
            int classificationComparison = Integer.compare(a.getAcmgClassificationResult().getAcmgClassification().ordinal(), b.getAcmgClassificationResult().getAcmgClassification().ordinal());
            if (classificationComparison == 0) {
                int scoreA = a.getAcmgTieringResults().stream().map(AcmgTieringResult::getTier).map(Enum::toString).mapToInt(t -> AcmgTier.valueOf(t).ordinal() * AcmgTier.values().length * AcmgTier.values().length).sum();
                int scoreB = b.getAcmgTieringResults().stream().map(AcmgTieringResult::getTier).map(Enum::toString).mapToInt(t -> AcmgTier.valueOf(t).ordinal() * AcmgTier.values().length * AcmgTier.values().length).sum();
                return Integer.compare(scoreB, scoreA);
            }
            return classificationComparison;
        });

        queryResultDTO.elapsedMilliseconds = System.currentTimeMillis() - startTime;

        return queryResultDTO;
    }

    private Expression buildExpressionByGenes(List<String> genes) {
        Expression geneExpression = new EnumExpression(symbolKey, genes);
        /*Expression clinvarEnum = new EnumExpression("info_csq_clinvar_clnsig", new LinkedList<>(clinvar.getLikelyPathogenicAndPathogenicTerms()));
        Expression clinEnum = new EnumExpression("info_csq_clin_sig", new LinkedList<>(clinvar.getLikelyPathogenicAndPathogenicTerms()));
        Expression pathogenicityExpression = new IntermediateExpression(new ArrayList<>(Collections.singletonList("OR")), new ArrayList<>(Arrays.asList(clinEnum, clinvarEnum)));*/
        Expression afExpression = new BasicExpression<Double>("info_csq_gnomadg_af", "<", 0.05);
        Expression impactExpression = new EnumExpression("info_csq_impact", Arrays.asList("HIGH", "LOW", "MODERATE", "null"));
        return new IntermediateExpression(new ArrayList<>(Arrays.asList("AND", "AND")), new ArrayList<>(Arrays.asList(geneExpression, afExpression, impactExpression)));
    }

    private Expression buildExpressionSelectingByExactGenomicPositions(List<GenomicPosition> genomicPositionsByHpo) {
        ArrayList<String> operators = new ArrayList<>();
        ArrayList<Expression> children = new ArrayList<>();
        for (GenomicPosition genomicPosition : genomicPositionsByHpo) {
            ArrayList<String> innerOperators = new ArrayList<>(Arrays.asList("AND", "AND", "AND"));
            ArrayList<Expression> basics = new ArrayList<>();
            basics.add(new EnumExpression("chrom", Collections.singletonList("chrom" + genomicPosition.getChrom())));
            basics.add(new BasicExpression("pos", "=", (double) genomicPosition.getPos()));
            basics.add(new EnumExpression("ref", Collections.singletonList(genomicPosition.getRef())));
            basics.add(new EnumExpression("alt", Collections.singletonList(genomicPosition.getAlt())));
            children.add(new IntermediateExpression(innerOperators, basics));
        }

        for (int i = 1; i < children.size(); i++) {
            operators.add("OR");
        }

        return new IntermediateExpression(operators, children);
    }

    private Expression buildExpressionSelectingByGenomicPositions(List<GenomicPosition> genomicPositionsByHpo) {
        ArrayList<String> operators = new ArrayList<>();
        ArrayList<Expression> children = new ArrayList<>();
        for (GenomicPosition genomicPosition : genomicPositionsByHpo) {
            ArrayList<String> innerOperators = new ArrayList<>(Collections.singletonList("AND"));
            ArrayList<Expression> basics = new ArrayList<>();
            basics.add(new EnumExpression("chrom", Collections.singletonList("chr" + genomicPosition.getChrom())));
            basics.add(new BasicExpression<>("pos", "=", genomicPosition.getPos()));
            children.add(new IntermediateExpression(innerOperators, basics));
        }

        for (int i = 1; i < children.size(); i++) {
            operators.add("OR");
        }

        return new IntermediateExpression(operators, children);
    }

}
