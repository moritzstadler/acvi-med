package at.ac.meduniwien.vcfvisualize.rest;

import at.ac.meduniwien.vcfvisualize.data.VariantProvider;
import at.ac.meduniwien.vcfvisualize.mocking.MockVariantProvider;
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
import at.ac.meduniwien.vcfvisualize.processor.acmg.AcmgTier;
import at.ac.meduniwien.vcfvisualize.processor.acmg.AcmgTierer;
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

    @Autowired
    VariantProvider variantProvider;

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

        long startTime = System.currentTimeMillis();
        List<GenomicPosition> genomicPositionsByHpo = clinvar.getGenomicPositionsByHpoTerms(phenotypeAwareLoadRequestDTO.hpoTerms);
        Filter filterHpoTerms = new Filter(buildExpressionSelectingByGenomicPositions(genomicPositionsByHpo), new LinkedList<>(), 0);
        List<Variant> variantsByHpoTerms = variantProvider.getVariants(user, sample, filterHpoTerms);

        Filter filterGenes = new Filter(buildExpressionByGenes(phenotypeAwareLoadRequestDTO.genes), new LinkedList<>(), 0);
        List<Variant> variantsByGenes = variantProvider.getVariants(user, sample, filterGenes);

        //TODO only take green panels!!!!

        PhenotypeAwareQueryResultDTO queryResultDTO = new PhenotypeAwareQueryResultDTO();
        queryResultDTO.setVariants(new LinkedList<>());
        for (Variant variant : variantsByHpoTerms) {
            PhenotypeAwareVariantDTO phenotypeAwareVariantDTO = new PhenotypeAwareVariantDTO(variant);
            phenotypeAwareVariantDTO.setAcmgTiers(acmgTierer.performAcmgTiering(variant, true, false, phenotypeAwareLoadRequestDTO.humansDTO).stream().map(Enum::toString).collect(Collectors.toList()));
            queryResultDTO.getVariants().add(phenotypeAwareVariantDTO);
        }

        for (Variant variant : variantsByGenes) {
            PhenotypeAwareVariantDTO phenotypeAwareVariantDTO = new PhenotypeAwareVariantDTO(variant);
            phenotypeAwareVariantDTO.setAcmgTiers(acmgTierer.performAcmgTiering(variant, false, true, phenotypeAwareLoadRequestDTO.humansDTO).stream().map(Enum::toString).collect(Collectors.toList()));
            queryResultDTO.getVariants().add(phenotypeAwareVariantDTO);
        }

        queryResultDTO.getVariants().sort((a, b) -> {
            int scoreA = a.getAcmgTiers().stream().mapToInt(t -> AcmgTier.valueOf(t).ordinal() * AcmgTier.values().length * AcmgTier.values().length).sum();
            int scoreB = b.getAcmgTiers().stream().mapToInt(t -> AcmgTier.valueOf(t).ordinal() * AcmgTier.values().length * AcmgTier.values().length).sum();
            return Integer.compare(scoreB, scoreA);
        });

        queryResultDTO.elapsedMilliseconds = System.currentTimeMillis() - startTime;

        return queryResultDTO;
    }

    private Expression buildExpressionByGenes(List<String> genes) {
        Expression geneExpression = new EnumExpression(symbolKey, genes);
        Expression pathogenicityExpression = new EnumExpression("info_csq_clinvar_clnsig", new LinkedList<>(clinvar.getLikelyPathogenicAndPathogenicTerms()));
        return new IntermediateExpression(new ArrayList<>(Collections.singletonList("AND")), new ArrayList<>(Arrays.asList(geneExpression, pathogenicityExpression)));
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
