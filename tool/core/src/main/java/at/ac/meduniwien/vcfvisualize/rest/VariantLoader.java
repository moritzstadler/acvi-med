package at.ac.meduniwien.vcfvisualize.rest;

import at.ac.meduniwien.vcfvisualize.data.PostgresLoader;
import at.ac.meduniwien.vcfvisualize.data.VariantProvider;
import at.ac.meduniwien.vcfvisualize.data.discreetcolumnvalues.ColumnValuesProvider;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.PanelApp;
import at.ac.meduniwien.vcfvisualize.knowledgebase.panelapp.PanelIdGene;
import at.ac.meduniwien.vcfvisualize.model.*;
import at.ac.meduniwien.vcfvisualize.rest.dto.*;
import at.ac.meduniwien.vcfvisualize.rest.dto.genepanelsearch.IndexedGeneDTO;
import at.ac.meduniwien.vcfvisualize.security.AuthenticationService;
import at.ac.meduniwien.vcfvisualize.security.BucketAccessProvider;
import at.ac.meduniwien.vcfvisualize.security.ConfigurationService;
import at.ac.meduniwien.vcfvisualize.study.StudyService;
import lombok.SneakyThrows;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class VariantLoader {

    static final long PAGE_SIZE = 100; //needs to be a divisor of GLOBAL_LIMIT in order for the cache to take it in account
    static final long RESULT_COUNT_LIMIT = 1000; //move this to config file, determines when the result count stops
    //TODO move to config
    static final List<String> DEFAULT_INFO_FIELDS = Arrays.asList("info_csq_tsl", "info_csq_canonical", "info_csq_metasvm_score", "info_csq_revel_score", "info_csq_mvp_score", "info_csq_sift4g_score", "info_csq_dann_score", "info_csq_fathmm_score", "info_csq_primateai_score", "info_csq_polyphen", "info_csq_gerp_rs", "info_cadd_phred", "info_cadd_raw", "info_caddind_phred", "info_caddind_raw", "info_csq_impact", "info_controls_af_popmax", "info_csq_symbol", "info_csq_consequence");
    static final String googleCloudProjectId = "genomics-324511";
    static final String googleCloudBucketId = "genomics-324511-data";

    private Boolean useGoogleBucket;
    private String googleCloudCredentials;
    private String googleProjectId;
    private String googleBucketName;

    public VariantLoader(@Value("${storage.reads.usegoogle}") Boolean useGoogleBucket, @Value("${storage.reads.google.credentials}") String googleCloudCredentials, @Value("${storage.reads.google.projectid}") String googleProjectId, @Value("${storage.reads.google.bucketname}") String googleBucketName) {
        this.useGoogleBucket = useGoogleBucket;
        this.googleCloudCredentials = googleCloudCredentials;
        this.googleProjectId = googleProjectId;
        this.googleBucketName = googleBucketName;
    }

    @Autowired
    VariantProvider variantProvider;

    @Autowired
    PanelApp panelApp;

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    ColumnValuesProvider columnValuesProvider;

    @Autowired
    StudyService studyService;

    @CrossOrigin
    @PostMapping("/variant/load")
    public QueryResultDTO load(@RequestBody LoadRequestDTO loadRequestDTO) {
        User user = authenticationService.getUserForToken(loadRequestDTO.token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        String sample = loadRequestDTO.sample;

        if (!variantProvider.isValidSampleId(sample)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        if (!authenticationService.userCanAccessSample(user, sample)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        long startTime = System.currentTimeMillis();

        Filter filter = new Filter(loadRequestDTO.filter);

        //if we set offset automatically it should be a multiple of GLOBAL_LIMIT in order to use the cache
        long maxIndexRequested = (loadRequestDTO.page + 1) * PAGE_SIZE + filter.getOffset() - 1;
        long calculatedOffset = PostgresLoader.GLOBAL_LIMIT * ((int) (maxIndexRequested / PostgresLoader.GLOBAL_LIMIT));
        filter.setOffset(calculatedOffset);

        QueryResultDTO queryResultDTO = new QueryResultDTO();

        Set<String> infoFieldsToKeep = new HashSet<>(DEFAULT_INFO_FIELDS);
        infoFieldsToKeep.addAll(filter.getFields());

        System.out.println(LocalDateTime.now() + ": " + user.getEmail() + " accessing " + sample);
        queryResultDTO.variants = variantProvider.getVariants(user, sample, filter, true)
                .stream()
                .skip(loadRequestDTO.page * PAGE_SIZE - calculatedOffset)
                .limit(PAGE_SIZE)
                .map(v -> v.convertToReducedDTO(infoFieldsToKeep))
                .collect(Collectors.toList());

        System.out.println("returning " + queryResultDTO.variants.size() + " variants");

        queryResultDTO.resultCount = variantProvider.countVariantsToLimit(user, sample, filter, RESULT_COUNT_LIMIT);
        queryResultDTO.resultCountLimit = RESULT_COUNT_LIMIT;
        queryResultDTO.allVariantsCount = variantProvider.estimateAllVariantsCount(sample);
        queryResultDTO.elapsedMilliseconds = System.currentTimeMillis() - startTime;

        return queryResultDTO;
    }

    @CrossOrigin
    @PostMapping("variant/loadsingle")
    public VariantDTO loadSingle(@RequestBody LoadSingleRequestDTO loadSingleRequestDTO) {
        User user = authenticationService.getUserForToken(loadSingleRequestDTO.token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        String sample = loadSingleRequestDTO.sample;

        if (!authenticationService.userCanAccessSample(user, sample)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        //TODO return error here if assert fails
        //assert requestEnvelope.request instanceof LoadSingleRequest;
        //LoadSingleRequest loadSingleRequestDTO = (LoadSingleRequest) requestEnvelope.request;
        VariantIdentifier variantIdentifier = new VariantIdentifier(loadSingleRequestDTO.pid);

        System.out.println(LocalDateTime.now() + ": " + user.getEmail() + " accessing " + sample + "/" + variantIdentifier.getPid());
        Variant variant = variantProvider.getVariant(user, sample, variantIdentifier);
        if (variant == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "variant not found");
        } else {
            VariantDTO variantDTO = variant.convertToDTO();
            attachGeneDTO(variantDTO);
            attachIsoforms(sample, variantDTO);
            attachIgv(sample, variantDTO, loadSingleRequestDTO.token);
            return variantDTO;
        }
    }

    private void attachGeneDTO(VariantDTO variantDTO) {
        String symbolKey = "info_csq_symbol";
        if (variantDTO.getInfo().containsKey(symbolKey)) {
            String symbol = variantDTO.getInfo().get(symbolKey);
            if (symbol != null) {
                List<PanelIdGene> panelIdGenes = panelApp.getGene(symbol);
                if (panelIdGenes != null && panelIdGenes.size() > 0) {
                    //Gene gene = panelIdGenes.get(0).getGene();
                    //String[] panelNames = panelApp.getGene(symbol).stream().map(g -> panelApp.getPanel(g.getId()).getPanel().name).toArray(String[]::new);
                    //new IndexedGeneDTO(gene, panelNames)
                    variantDTO.setIndexedGeneDTOs(panelIdGenes.stream().map(p -> new IndexedGeneDTO(p.getGene(), Collections.singletonList(panelApp.getPanel(p.getId()).getPanel().name).toArray(new String[1]))).collect(Collectors.toList()));
                }
            }
        }
    }

    private void attachIsoforms(String sample, VariantDTO variantDTO) {
        variantDTO.setIsoforms(variantProvider.getIsoforms(sample, variantDTO.getVid()).stream().map(Variant::convertToDTO).collect(Collectors.toList()));
    }

    private void attachIgv(String sample, VariantDTO variantDTO, String token) {
        String igvPath = studyService.getSampleByName(sample).getIgvPath();
        String igvIndexPath;
        String format;
        if (igvPath.endsWith(".bam")) {
            igvIndexPath = igvPath.replace(".bam", ".bai");
            format = "bam";
        } else {
            igvIndexPath = igvPath + ".crai";
            format = "cram";
        }

        String igvUrl, igvIndexUrl;
        if (useGoogleBucket) {
            igvUrl = BucketAccessProvider.generateGoogleBucketGetObjectSignedUrl(googleCloudCredentials, googleProjectId, googleBucketName, igvPath);
            igvIndexUrl = BucketAccessProvider.generateGoogleBucketGetObjectSignedUrl(googleCloudCredentials, googleProjectId, googleBucketName, igvIndexPath);
        } else {
            igvUrl = BucketAccessProvider.generateSignedUrl(sample, igvPath, token);
            igvIndexUrl = BucketAccessProvider.generateSignedUrl(sample, igvIndexPath, token);
        }

        variantDTO.setIgvIndexUrl(igvIndexUrl);
        variantDTO.setIgvUrl(igvUrl);
        variantDTO.setIgvFormat(format);
    }

    @CrossOrigin
    @GetMapping("variant/loadreads/{sample}")
    public @ResponseBody
    FileSystemResource loadReads(@PathVariable String sample, @RequestParam String path, @RequestParam String token, HttpServletResponse response) throws IOException {
        User user = authenticationService.getUserForToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        if (!variantProvider.isValidSampleId(sample)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        if (!authenticationService.userCanAccessSample(user, sample)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        String storedPath = studyService.getSampleByName(sample).getIgvPath();
        //check if path matches stored path to avoid users accessing any file they want to
        if (!replaceReadsSpecificFileFormats(storedPath).equals(replaceReadsSpecificFileFormats(path))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        if (!new File(path).exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "file not found");
        }

        return new FileSystemResource(new File(path));
    }

    private String replaceReadsSpecificFileFormats(String path) {
        return path.replaceAll(".bam", "").replaceAll(".bai", "").replaceAll(".cram", "").replaceAll(".crai", "");
    }

    @SneakyThrows
    @CrossOrigin
    @PostMapping("variant/loadmeta")
    public List<FieldMetaDataDTO> loadMetaData(@RequestBody LoadMetaDataDTO loadMetaDataDTO) {
        User user = authenticationService.getUserForToken(loadMetaDataDTO.token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        String sample = loadMetaDataDTO.sample;

        if (!authenticationService.userCanAccessSample(user, sample)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        return convertJsonToListOfMetaData(sample);
    }

    @SneakyThrows
    @CrossOrigin
    @PostMapping("variant/loadview")
    public String loadView(@RequestBody LoadMetaDataDTO loadMetaDataDTO) {
        User user = authenticationService.getUserForToken(loadMetaDataDTO.token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        String sample = loadMetaDataDTO.sample;

        if (!authenticationService.userCanAccessSample(user, sample)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        return ConfigurationService.read("view.json");
    }

    @SneakyThrows
    @CrossOrigin
    @PostMapping("variant/loadfilter")
    public List<FieldMetaDataDTO> loadFilterableMetaData(@RequestBody LoadMetaDataDTO loadMetaDataDTO) {
        User user = authenticationService.getUserForToken(loadMetaDataDTO.token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        String sample = loadMetaDataDTO.sample;

        if (!authenticationService.userCanAccessSample(user, sample)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not authorized");
        }

        List<FieldMetaDataDTO> filterable = new LinkedList<>();
        List<FieldMetaDataDTO> fieldMetaDataDTOs = convertJsonToListOfMetaData(sample);
        HashMap<String, FieldMetaDataDTO> fieldMetaDataDTOById = new HashMap<>();
        for (FieldMetaDataDTO fieldMetaDataDTO : fieldMetaDataDTOs) {
            fieldMetaDataDTOById.put(fieldMetaDataDTO.id, fieldMetaDataDTO);
        }

        String columnsMetaDataString = ConfigurationService.read("columns.json");
        JSONArray filterableIds = new JSONObject(columnsMetaDataString).getJSONArray("filterable");
        for (int i = 0; i < filterableIds.length(); i++) {
            String filterId = filterableIds.getString(i);
            filterable.add(fieldMetaDataDTOById.get(filterId));
        }

        return filterable;
    }

    @SneakyThrows
    private List<FieldMetaDataDTO> convertJsonToListOfMetaData(String sample) {
        List<FieldMetaDataDTO> result = new LinkedList<>();
        HashSet<String> addedFields = new HashSet<>();

        List<Column> columns = variantProvider.getColumns(sample);
        HashMap<String, String> dataTypeByName = new HashMap<>();
        for (Column column : columns) {
            dataTypeByName.put(column.getName(), column.getDatatype());
        }

        //the order within columns has to be obeyed
        String columnsMetaDataString = ConfigurationService.read("columns.json");
        JSONArray columnsMetaData = new JSONObject(columnsMetaDataString).getJSONArray("columns");
        for (int i = 0; i < columnsMetaData.length(); i++) {
            JSONObject columnMetaData = columnsMetaData.getJSONObject(i);
            FieldMetaDataDTO fieldMetaDataDTO = convertJSONObjectToFieldMetaDataDTO(columnMetaData, dataTypeByName.getOrDefault(columnMetaData.getString("id"), "string"));
            result.add(fieldMetaDataDTO);
            addedFields.add(fieldMetaDataDTO.id);
        }

        long rowCount = variantProvider.estimateAllVariantsCount(sample);

        for (Column column : columns) {
            if (!addedFields.contains(column.getName())) {
                FieldMetaDataDTO fieldMetaDataDTO = new FieldMetaDataDTO();
                fieldMetaDataDTO.id = column.getName();
                fieldMetaDataDTO.name = column.getName();
                fieldMetaDataDTO.type = column.getDatatype();
                fieldMetaDataDTO.displayable = true;
                fieldMetaDataDTO.from = 0.0;
                fieldMetaDataDTO.to = 1.0;
                //TODO read type from database
                result.add(fieldMetaDataDTO);
            }
        }

        for (FieldMetaDataDTO fieldMetaDataDTO : result) {
            if (fieldMetaDataDTO.discreetvalues == null) {
                fieldMetaDataDTO.discreetvalues = columnValuesProvider.getColumnValues(sample, fieldMetaDataDTO.id);
            }
            //add non null count
            fieldMetaDataDTO.nonnullrows = columnValuesProvider.getNonNullColumnCount(sample, fieldMetaDataDTO.id);
            fieldMetaDataDTO.overallrows = rowCount;
        }

        return result;
    }

    private FieldMetaDataDTO convertJSONObjectToFieldMetaDataDTO(JSONObject columnMetaData, String type) {
        FieldMetaDataDTO fieldMetaDataDTO = new FieldMetaDataDTO();
        fieldMetaDataDTO.id = columnMetaData.getString("id");
        fieldMetaDataDTO.name = (String) getJsonFieldOrDefault(columnMetaData, "name", fieldMetaDataDTO.id);
        fieldMetaDataDTO.description = (String) getJsonFieldOrDefault(columnMetaData, "description", null);
        fieldMetaDataDTO.type = type;
        fieldMetaDataDTO.link = (String) getJsonFieldOrDefault(columnMetaData, "link", null);
        fieldMetaDataDTO.sample = (String) getJsonFieldOrDefault(columnMetaData, "sample", null);
        fieldMetaDataDTO.samplecomparator = (String) getJsonFieldOrDefault(columnMetaData, "samplecomparator", null);
        fieldMetaDataDTO.range = (String) getJsonFieldOrDefault(columnMetaData, "range", null);
        fieldMetaDataDTO.normalizationfunction = (String) getJsonFieldOrDefault(columnMetaData, "normalizationfunction", null);
        fieldMetaDataDTO.displaytype = (String) getJsonFieldOrDefault(columnMetaData, "displaytype", null);
        fieldMetaDataDTO.displayable = (Boolean) getJsonFieldOrDefault(columnMetaData, "displayable", true);
        fieldMetaDataDTO.from = (Double) getJsonFieldOrDefault(columnMetaData, "from", 0.0);
        fieldMetaDataDTO.to = (Double) getJsonFieldOrDefault(columnMetaData, "to", 1.0);

        if (columnMetaData.has("discreetvalues")) {
            fieldMetaDataDTO.discreetvalues = columnMetaData.getJSONArray("discreetvalues").toList().stream().map(o -> (String) o).collect(Collectors.toList());
        }

        return fieldMetaDataDTO;
    }

    private Object getJsonFieldOrDefault(JSONObject jsonObject, String name, Object defaultValue) {
        if (jsonObject.has(name)) {
            if (defaultValue instanceof String || defaultValue == null) {
                return jsonObject.getString(name);
            } else if (defaultValue instanceof Double) {
                return jsonObject.getDouble(name);
            } else if (defaultValue instanceof Boolean) {
                return jsonObject.getBoolean(name);
            }
        }
        return defaultValue;
    }

}
