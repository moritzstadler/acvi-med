package at.ac.meduniwien.vcfvisualize.rest;

import at.ac.meduniwien.vcfvisualize.data.VariantProvider;
import at.ac.meduniwien.vcfvisualize.knowledgebase.pharmgkb.PharmGKB;
import at.ac.meduniwien.vcfvisualize.knowledgebase.pharmgkb.PharmGKBAllele;
import at.ac.meduniwien.vcfvisualize.knowledgebase.pharmgkb.PharmGKBAnnotation;
import at.ac.meduniwien.vcfvisualize.knowledgebase.pharmgkb.PharmGKBEvidence;
import at.ac.meduniwien.vcfvisualize.model.Filter;
import at.ac.meduniwien.vcfvisualize.model.User;
import at.ac.meduniwien.vcfvisualize.model.Variant;
import at.ac.meduniwien.vcfvisualize.model.expression.*;
import at.ac.meduniwien.vcfvisualize.rest.dto.*;
import at.ac.meduniwien.vcfvisualize.security.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
public class PharmacoGenomicsLoader {

    public PharmacoGenomicsLoader() {
        processIdToVariants = new HashMap<>();
        processIdToProcessingStart = new HashMap<>();
        processIdToProcessedCount = new HashMap<>();
        processIdToCompleted = new HashMap<>();
        processIdToSample = new HashMap<>();
        sampleToProcessId = new HashMap<>();
    }

    @Autowired
    VariantProvider variantProvider;

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    PharmGKB pharmGKB;

    HashMap<String, List<Variant>> processIdToVariants;

    HashMap<String, Long> processIdToProcessingStart;

    HashMap<String, Integer> processIdToProcessedCount;

    HashMap<String, Boolean> processIdToCompleted;

    HashMap<String, String> processIdToSample;

    HashMap<String, String> sampleToProcessId;

    @CrossOrigin
    @PostMapping("/pharmacogenomics/startcomputation")
    public String startComputation(@RequestBody PharmacoGenomicsLoadRequestDTO pharmacoGenomicsLoadRequestDTO) {
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

        //avoid restarting in case of losing the processId
        if (sampleToProcessId.containsKey(sample)) {
            return sampleToProcessId.get(sample);
        }

        String processId = UUID.randomUUID().toString();
        prepareClearCache(processId, sample);

        new Thread(() -> {
            try {
                //version for single size batches
                /*for (String rsId : pharmGKB.getKnownRsIds()) {
                    List<Variant> partialResult = variantProvider.getVariants(user, sample, new Filter(buildExpressionByRsId(Collections.singletonList(rsId)), new LinkedList<>(), 0), false);
                    processIdToVariants.get(processId).addAll(partialResult);
                    processIdToProcessedCount.put(processId, processIdToProcessedCount.get(processId) + 1);
                }*/

                List<String> rsIds = new LinkedList<>();
                rsIds.addAll(pharmGKB.getKnownRsIds());

                int batchStart = 0;
                int batchSize = 5;
                List<List<String>> rsIdsBatches = new LinkedList<>();

                while (batchStart + batchSize < rsIds.size()) {
                    rsIdsBatches.add(rsIds.subList(batchStart, batchSize));
                    batchStart += batchSize;
                }
                rsIdsBatches.add(rsIds.subList(batchStart, rsIds.size()));

                for (List<String> rsIdsBatch : rsIdsBatches) {
                    List<Variant> partialResult = variantProvider.getVariants(user, sample, new Filter(buildExpressionByRsId(rsIdsBatch), new LinkedList<>(), 0), false);
                    processIdToVariants.get(processId).addAll(partialResult);
                    processIdToProcessedCount.put(processId, processIdToProcessedCount.get(processId) + rsIdsBatch.size());
                }

                processIdToCompleted.put(processId, true);
            } catch (Exception ex) {
                System.out.println("Error performing PGx: " + ex.getMessage());
                //delete cached data
                emptyCache(processId, sample);
            }
        }).start();

        return processId;
    }

    private void prepareClearCache(String processId, String sample) {
        processIdToVariants.put(processId, new LinkedList<>());
        processIdToProcessingStart.put(processId, System.currentTimeMillis());
        processIdToProcessedCount.put(processId, 0);
        processIdToCompleted.put(processId, false);
        processIdToSample.put(processId, sample);
        sampleToProcessId.put(sample, processId);
    }

    private void emptyCache(String processId, String sample) {
        processIdToVariants.remove(processId);
        processIdToProcessingStart.remove(processId);
        processIdToProcessedCount.remove(processId);
        processIdToCompleted.remove(processId);
        processIdToSample.remove(processId);
        sampleToProcessId.remove(sample);
    }

    @CrossOrigin
    @PostMapping("/pharmacogenomics/progress/{processId}")
    public PharmacoGenomicsProgressDTO progress(@RequestBody PharmacoGenomicsLoadRequestDTO pharmacoGenomicsLoadRequestDTO, @PathVariable String processId) {
        //this does not need to be behind security since the processId is a secret and there is no important data here
        return new PharmacoGenomicsProgressDTO(processIdToProcessedCount.get(processId), pharmGKB.getKnownRsIds().size(), System.currentTimeMillis() - processIdToProcessingStart.get(processId));
    }

    @CrossOrigin
    @PostMapping("/pharmacogenomics/result/{processId}")
    public PharmacoGenomicsQueryResultDTO result(@RequestBody PharmacoGenomicsLoadRequestDTO pharmacoGenomicsLoadRequestDTO, @PathVariable String processId) {
        User user = authenticationService.getUserForToken(pharmacoGenomicsLoadRequestDTO.token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        String sample = pharmacoGenomicsLoadRequestDTO.sample;
        if (!variantProvider.isValidSampleId(sample) || !processIdToSample.get(processId).equals(sample)) {
            System.out.println("process id does not match sample");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        if (!authenticationService.userCanAccessSample(user, sample)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        if (!processIdToCompleted.get(processId)) {
            System.out.println("process not completed");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "process not completed yet");
        }

        PharmacoGenomicsQueryResultDTO queryResultDTO = new PharmacoGenomicsQueryResultDTO();
        List<Variant> variants = processIdToVariants.get(processId);

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
            Variant primeVariant = rsIdToVariant.get(rsId).get(0);
            for (Variant variant : rsIdToVariant.get(rsId)) {
                if (variant.getInfo().containsKey("info_csq_canonical") && variant.getInfo().get("info_csq_canonical") != null && variant.getInfo().get("info_csq_canonical").toUpperCase().equals("YES")) {
                    primeVariant = variant;
                }
            }

            List<PharmGKBAnnotation> pharmGKBAnnotations = pharmGKB.getAnnotationsByRsId(rsId);
            List<PharmGKBAnnotationDTO> pharmGKBAnnotationDTOs = new LinkedList<>();
            for (PharmGKBAnnotation pharmGKBAnnotation : pharmGKBAnnotations) {
                List<PharmGKBEvidence> pharmGKBEvidences = pharmGKB.getEvidenceByAnnotationId(pharmGKBAnnotation.getClinicalAnnotationId());
                List<PharmGKBAllele> pharmGKBAlleles = pharmGKB.getAllelesByAnnotationId(pharmGKBAnnotation.getClinicalAnnotationId());

                PharmGKBAnnotationDTO pharmGKBAnnotationDTO = new PharmGKBAnnotationDTO(pharmGKBAnnotation, pharmGKBEvidences, pharmGKBAlleles);
                pharmGKBAnnotationDTOs.add(pharmGKBAnnotationDTO);
            }

            Set<String> infoFieldsToKeep = new HashSet<>(Arrays.asList("info_csq_symbol", "info_csq_hgvsc"));
            //genotype is automatically kept
            VariantDTO primeVariantDTO = primeVariant.convertToReducedDTO(infoFieldsToKeep);
            PharmacoGenomicsVariantDTO pharmacoGenomicsVariantDTO = new PharmacoGenomicsVariantDTO(primeVariantDTO, pharmGKBAnnotationDTOs);
            pharmacoGenomicsVariantDTOs.add(pharmacoGenomicsVariantDTO);
        }

        queryResultDTO.setVariants(pharmacoGenomicsVariantDTOs);
        queryResultDTO.elapsedMilliseconds = System.currentTimeMillis() - processIdToProcessingStart.get(processId);

        //clear cache
        //TODO add back if re-analysis is important
        //emptyCache(processId, sample);

        return queryResultDTO;
    }

    @Deprecated
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

            PharmacoGenomicsVariantDTO pharmacoGenomicsVariantDTO = new PharmacoGenomicsVariantDTO(primeVariant.convertToDTO(), pharmGKBAnnotationDTOs);
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
