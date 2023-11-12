package at.ac.meduniwien.vcfvisualize.rest;

import at.ac.meduniwien.vcfvisualize.data.VariantProvider;
import at.ac.meduniwien.vcfvisualize.knowledgebase.acmg.SecondaryFindingDefinition;
import at.ac.meduniwien.vcfvisualize.knowledgebase.acmg.SecondaryFindings;
import at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar.Clinvar;
import at.ac.meduniwien.vcfvisualize.knowledgebase.clinvar.GenomicPosition;
import at.ac.meduniwien.vcfvisualize.knowledgebase.pharmgkb.PharmGKB;
import at.ac.meduniwien.vcfvisualize.knowledgebase.pharmgkb.PharmGKBAllele;
import at.ac.meduniwien.vcfvisualize.knowledgebase.pharmgkb.PharmGKBAnnotation;
import at.ac.meduniwien.vcfvisualize.knowledgebase.pharmgkb.PharmGKBEvidence;
import at.ac.meduniwien.vcfvisualize.model.Filter;
import at.ac.meduniwien.vcfvisualize.model.User;
import at.ac.meduniwien.vcfvisualize.model.Variant;
import at.ac.meduniwien.vcfvisualize.model.expression.*;
import at.ac.meduniwien.vcfvisualize.processor.acmg.AcmgClassification;
import at.ac.meduniwien.vcfvisualize.processor.acmg.AcmgClassificationResult;
import at.ac.meduniwien.vcfvisualize.processor.acmg.AcmgTierer;
import at.ac.meduniwien.vcfvisualize.processor.acmg.AcmgTieringResult;
import at.ac.meduniwien.vcfvisualize.rest.dto.*;
import at.ac.meduniwien.vcfvisualize.security.AuthenticationService;
import org.apache.commons.lang3.StringUtils;
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
public class PharmacoGenomicsLoader {

    @Autowired
    VariantProvider variantProvider;

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    PharmGKB pharmGKB;

    @CrossOrigin
    @PostMapping("/pharmacogenomics/load")
    public PharmacoGenomicsQueryResultDTO load(@RequestBody PharmacoGenomicsLoadRequestDTO pharmacoGenomicsLoadRequestDTO) {
        User user = authenticationService.getUserForToken(pharmacoGenomicsLoadRequestDTO.token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        String sample = pharmacoGenomicsLoadRequestDTO.sample;
        if (!variantProvider.isValidSampleId(sample)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        if (!authenticationService.userCanAccessSample(user, sample)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        PharmacoGenomicsQueryResultDTO queryResultDTO = new PharmacoGenomicsQueryResultDTO();
        long startTime = System.currentTimeMillis();

        //find all variants with rsIds known in PharmGKB. This might have to be split into multiple requests
        List<Variant> variants = variantProvider.getVariants(user, sample, new Filter(buildExpressionByRsId(new ArrayList<>(pharmGKB.getKnownRsIds())), new LinkedList<>(), 0), false);

        List<PharmacoGenomicsVariantDTO> pharmacoGenomicsVariantDTOs = new LinkedList<>();

        Map<String, List<Variant>> rsIdToVariant = new HashMap<>();

        for (Variant variant : variants) {
            String rsId = parseRsId(variant);

            if (!rsIdToVariant.containsKey(rsId)) {
                rsIdToVariant.put(rsId, new LinkedList<>());
            }
            rsIdToVariant.get(rsId).add(variant);
        }

        for (String rsId : rsIdToVariant.keySet()) {
            //find variant which is canonical (or any if none is)
            Variant primeVariant = rsIdToVariant.get(rsId).stream().filter(v -> v.getInfo().get("info_csq_canonical").toUpperCase().equals("YES")).findFirst().orElse(rsIdToVariant.get(rsId).get(0));

            List<PharmGKBAnnotation> pharmGKBAnnotations = pharmGKB.getAnnotationsByRsId(rsId);
            List<PharmGKBAnnotationDTO> pharmGKBAnnotationDTOs = new LinkedList<>();
            for (PharmGKBAnnotation pharmGKBAnnotation : pharmGKBAnnotations) {
                List<PharmGKBEvidence> pharmGKBEvidences = pharmGKB.getEvidenceByAnnotationId(pharmGKBAnnotation.getClinicalAnnotationId());
                List<PharmGKBAllele> pharmGKBAlleles = pharmGKB.getAllelesByAnnotationId(pharmGKBAnnotation.getClinicalAnnotationId());

                PharmGKBAnnotationDTO pharmGKBAnnotationDTO = new PharmGKBAnnotationDTO(pharmGKBAnnotation, pharmGKBEvidences, pharmGKBAlleles);
                pharmGKBAnnotationDTOs.add(pharmGKBAnnotationDTO);
            }

            PharmacoGenomicsVariantDTO pharmacoGenomicsVariantDTO = new PharmacoGenomicsVariantDTO(primeVariant, pharmGKBAnnotationDTOs);
            pharmacoGenomicsVariantDTOs.add(pharmacoGenomicsVariantDTO);
        }

        queryResultDTO.setVariants(pharmacoGenomicsVariantDTOs);
        queryResultDTO.elapsedMilliseconds = System.currentTimeMillis() - startTime;

        return queryResultDTO;
    }

    private Expression buildExpressionByRsId(List<String> rsIds) {
        return new SeparatedInclusionExpression("info_csq_existing_variation", rsIds, "&");
    }

    private String parseRsId(Variant variant) {
        String value = variant.getInfo().get("info_csq_existing_variation");

        if (!value.contains("rs")) {
            return "";
        }

        if (value.contains("&")) {
            return value.split("&")[0];
        } else {
            return value;
        }
    }

}
