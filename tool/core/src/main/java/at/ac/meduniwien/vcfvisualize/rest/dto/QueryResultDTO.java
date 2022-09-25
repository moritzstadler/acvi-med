package at.ac.meduniwien.vcfvisualize.rest.dto;

import java.util.List;

public class QueryResultDTO {

    public List<VariantDTO> variants;
    public long resultCountLimit;
    public long resultCount;
    public long allVariantsCount;
    public long elapsedMilliseconds;

}
