package at.ac.meduniwien.vcfvisualize.data.variantcache;

import at.ac.meduniwien.vcfvisualize.model.Filter;
import at.ac.meduniwien.vcfvisualize.model.Variant;
import at.ac.meduniwien.vcfvisualize.model.VariantIdentifier;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

class CacheElement {

    static final int STORED_FILTERS = 5; //the last 5 filters for every user sample combination are stored

    private FilterStore[] storedFilters;
    private int nextFree;
    private HashMap<VariantIdentifier, Variant> singleCalledVariants;
    private HashMap<CountKey, Long> storedCounts;

    public CacheElement() {
        this.storedFilters = new FilterStore[STORED_FILTERS];
        this.nextFree = 0;
        this.singleCalledVariants = new HashMap<>();
        this.storedCounts = new HashMap<>();
    }

    /**
     * stores a filter. If there are STORED_FILTERS or more filters stored allreay,
     * the oldest is overwritten
     * @param filter the filter to store
     * @param filterResult the result of the filter
     */
    public void storeFilter(Filter filter, List<Variant> filterResult) {
        FilterStore filterStore = new FilterStore();
        filterStore.filter = filter;
        filterStore.filterResult = filterResult;
        filterStore.variantsByIdentifier = new HashMap<>();

        for (Variant variant : filterResult) {
            filterStore.variantsByIdentifier.put(variant.getVariantIdentifier(), variant);
        }

        this.storedFilters[this.nextFree] = filterStore;
        this.nextFree = (this.nextFree + 1) % this.storedFilters.length;
    }

    /**
     * stores a single variant (e. g. if called via a link a colleague sent me)
     * @param variantIdentifier the identifier of the variant
     * @param variant the variant
     */
    public void storeSingle(VariantIdentifier variantIdentifier, Variant variant) {
        this.singleCalledVariants.put(variantIdentifier, variant);
    }

    /**
     * stores count
     * @param filter filter
     * @param limit the limit used
     * @param count the result
     */
    public void storeCount(Filter filter, long limit, long count) {
        CountKey countKey = new CountKey(filter, limit);
        this.storedCounts.put(countKey, count);
    }

    /**
     * searches for the stored filter.
     * Returns null if it was not found
     * @param filter the filter
     * @return the list of variants
     */
    public List<Variant> findFilter(Filter filter) {
        for (FilterStore filterStore : this.storedFilters) {
            if (filterStore != null && filterStore.filter.equals(filter)) {
                return filterStore.filterResult;
            }
        }
        return null;
    }

    /**
     * finds a single variant
     * @param variantIdentifier the 'id' of the variant
     * @return the variant
     */
    public Variant findVariant(VariantIdentifier variantIdentifier) {
        //check if it is singles
        if (singleCalledVariants.containsKey(variantIdentifier)) {
            return singleCalledVariants.get(variantIdentifier);
        }

        //search every filter for the variant
        for (FilterStore filterStore : this.storedFilters) {
            if (filterStore != null && filterStore.variantsByIdentifier.containsKey(variantIdentifier)) {
                return filterStore.variantsByIdentifier.get(variantIdentifier);
            }
        }

        return null;
    }

    /**
     * retrieves count
     * @param filter filter
     * @param limit the limit used
     */
    public Long findCount(Filter filter, long limit) {
        CountKey countKey = new CountKey(filter, limit);
        if (storedCounts.containsKey(countKey)) {
            return storedCounts.get(countKey);
        }

        return null;
    }
}

class FilterStore {
    Filter filter;
    List<Variant> filterResult;
    HashMap<VariantIdentifier, Variant> variantsByIdentifier;
}

class CountKey {

    public CountKey(Filter filter, long limit) {
        this.filter = filter;
        this.limit = limit;
    }

    Filter filter;
    long limit;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CountKey countKey = (CountKey) o;
        return limit == countKey.limit &&
                filter.equals(countKey.filter);
    }

    @Override
    public int hashCode() {
        return (int) (filter.toSqlString().hashCode() + limit);
    }
}
