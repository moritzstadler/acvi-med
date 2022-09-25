package at.ac.meduniwien.vcfvisualize.data;

import at.ac.meduniwien.vcfvisualize.data.variantcache.VariantCache;
import at.ac.meduniwien.vcfvisualize.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
public class VariantProvider {

    @Autowired
    PostgresLoader postgresLoader;

    @Autowired
    VariantCache variantCache;

    /**
     * returns variants for a sample and a given filter
     * @param user only used for caching
     * @param sample the sample id
     * @param filter the filter
     * @return a list of filtered variants
     */
    public List<Variant> getVariants(User user, String sample, Filter filter) {
        List<Variant> cachedVariants = variantCache.get(user, sample, filter);
        if (cachedVariants != null) {
            System.out.println("retrieve cache");
            return cachedVariants;
        }

        List<Variant> variants = postgresLoader.getVariants(sample, filter);
        variantCache.cache(user, sample, filter, variants);
        return variants;
    }

    public long estimateAllVariantsCount(String sample) {
        //TODO does this need caching?
        return postgresLoader.estimateAllVariantsCount(sample);
    }

    public long countVariantsToLimit(User user, String sample, Filter filter, long limit) {
        Long cachedCount = variantCache.get(user, sample, filter, limit);
        if (cachedCount != null) {
            System.out.println("retrieve counting cache");
            return cachedCount;
        }
        long count = postgresLoader.countVariantsToLimit(sample, filter, limit);
        variantCache.cache(user, sample, filter, limit, count);
        return count;
    }

    /**
     * returns a single variant
     * @param user only used for caching
     * @param sample the sample id
     * @param variantIdentifier the 'id' of the variant (chrom - pos - ref - alt)
     * @return a variant or null
     */
    public Variant getVariant(User user, String sample, VariantIdentifier variantIdentifier) {
        Variant cachedVariant = variantCache.get(user, sample, variantIdentifier);
        if (cachedVariant != null) {
            System.out.println("retrieve cache");
            return cachedVariant;
        }

        Variant variant = postgresLoader.getVariant(sample, variantIdentifier);
        variantCache.cache(user, sample, variantIdentifier, variant);
        return variant;
    }

    public boolean isValidSampleId(String sampleId) {
        return postgresLoader.isValidSampleId(sampleId);
    }

    /**
     * loads isoforms with the same vid
     * @param sample the sample
     * @param vid the vid
     * @return a list of pids with similar vids
     */
    public List<Variant> getIsoforms(String sample, long vid) {
        //TODO does this need caching?
        return postgresLoader.getIsoforms(sample, vid);
    }

    public List<Column> getColumns(String sample) {
        return postgresLoader.getColumns(sample);
    }

}
