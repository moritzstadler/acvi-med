package at.ac.meduniwien.vcfvisualize.data.variantcache;

import at.ac.meduniwien.vcfvisualize.model.Filter;
import at.ac.meduniwien.vcfvisualize.model.User;
import at.ac.meduniwien.vcfvisualize.model.Variant;
import at.ac.meduniwien.vcfvisualize.model.VariantIdentifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

@Service
public class VariantCache {

    //defines for how long a user sample combination has to have not been accessed in order for it to be deleted
    long secondsRetainedInCache = 24 * 60 * 60; //retention is 24 hours

    PriorityQueue<UserSampleAccess> userSampleAccessQueue;
    HashMap<UserSamplePair, UserSampleAccess> userSampleAccessMap;

    HashMap<UserSamplePair, CacheElement> cache; //TODO comment usage

    public VariantCache() {
        this.userSampleAccessQueue = new PriorityQueue<>();
        this.userSampleAccessMap = new HashMap<>();
        this.cache = new HashMap<>();
    }

    /**
     * stores a variant list in the cache
     * @param user the user
     * @param sample the id of the sample
     * @param filter the filter used to obtain the result
     * @param variants the result
     */
    public void cache(User user, String sample, Filter filter, List<Variant> variants) {
        amortizeCache(user, sample);
        UserSamplePair key = createCacheElementIfNonExistent(user, sample);
        cache.get(key).storeFilter(filter, variants);
    }

    /**
     * stores a single variant in the cache
     * @param user the user
     * @param sample the id of the sample
     * @param variantIdentifier the 'id' of the variant
     * @param variant the variant
     */
    public void cache(User user, String sample, VariantIdentifier variantIdentifier, Variant variant) {
        amortizeCache(user, sample);
        UserSamplePair key = createCacheElementIfNonExistent(user, sample);
        cache.get(key).storeSingle(variantIdentifier, variant);
    }

    /**
     * stores the result of a count the cache
     * @param user the user
     * @param sample the id of the sample
     * @param filter the filter
     * @param limit the limit used in counting
     * @param count the result of the operation
     */
    public void cache(User user, String sample, Filter filter, long limit, long count) {
        amortizeCache(user, sample);
        UserSamplePair key = createCacheElementIfNonExistent(user, sample);
        cache.get(key).storeCount(filter, limit, count);
    }

    /**
     * finds the result of a filter
     * @param user is only used to update the cache (e. g. if this user accesses their data, they are moved to the top of the queue)
     * @param sample the sample id
     * @param filter the filter
     * @return a list of variants or null
     */
    public List<Variant> get(User user, String sample, Filter filter) {
        amortizeCache(user, sample);
        UserSamplePair key = createCacheElementIfNonExistent(user, sample);
        CacheElement cacheElement = cache.get(key);
        return cacheElement.findFilter(filter); //null if none is found
    }

    /**
     * finds a single cached variant
     * @param user the user
     * @param sample the sample id
     * @param variantIdentifier the id of the variant
     * @return a variant or null
     */
    public Variant get(User user, String sample, VariantIdentifier variantIdentifier) {
        amortizeCache(user, sample);
        UserSamplePair key = createCacheElementIfNonExistent(user, sample);
        CacheElement cacheElement = cache.get(key);
        return cacheElement.findVariant(variantIdentifier); //null if none is found
    }

    /**
     * finds a counting result
     * @param user the user
     * @param sample the sample
     * @param filter the filter
     * @param limit the limit used in counting
     * @return the count or null
     */
    public Long get(User user, String sample, Filter filter, long limit) {
        amortizeCache(user, sample);
        UserSamplePair key = createCacheElementIfNonExistent(user, sample);
        CacheElement cacheElement = cache.get(key);
        return cacheElement.findCount(filter, limit); //null if none is found
    }

    /**
     * marks a user - sample combination as active, cleans the cache (deletes old entries)
     * @param user the user accessing the sample
     * @param sample the sample id
     */
    private void amortizeCache(User user, String sample) {
        //store that user has accessed sample
        UserSamplePair key = new UserSamplePair(user, sample);
        if (userSampleAccessMap.containsKey(key)) {
            //find priority queue element
            UserSampleAccess value = userSampleAccessMap.get(key);
            //remove from queue, update time, add to queue again to move to right position
            userSampleAccessQueue.remove(value);
            value.dateTime = LocalDateTime.now();
            userSampleAccessQueue.add(value);
        } else {
            //if this users has never accessed this sample, it needs to be added to the queue
            UserSampleAccess userSampleAccess = new UserSampleAccess(user, sample, LocalDateTime.now());
            userSampleAccessMap.put(new UserSamplePair(user, sample), userSampleAccess);
            userSampleAccessQueue.add(userSampleAccess);
        }

        //find user sample combinations which have not been accessed for some time and remove them from the cachce
        while (userSampleAccessQueue.peek() != null && userSampleAccessQueue.peek().dateTime.plusSeconds(secondsRetainedInCache).isBefore(LocalDateTime.now())) {
            UserSampleAccess userSampleAccess = userSampleAccessQueue.poll();
            deleteFromCache(userSampleAccess.getUser(), userSampleAccess.getSample());
        }
    }

    /**
     * removes a sample from the cache
     * @param user the user
     * @param sample the sample
     */
    private void deleteFromCache(User user, String sample) {
        cache.remove(new UserSamplePair(user, sample));
    }

    /**
     * creates a cache element if it does not exist and returns its key
     * @param user the user
     * @param sample the sample
     * @return the user sample pair
     */
    private UserSamplePair createCacheElementIfNonExistent(User user, String sample) {
        UserSamplePair key = new UserSamplePair(user, sample);
        if (!cache.containsKey(key)) {
            cache.put(key, new CacheElement());
        }
        return key;
    }

}
