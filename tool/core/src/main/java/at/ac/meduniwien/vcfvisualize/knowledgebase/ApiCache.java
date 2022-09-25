package at.ac.meduniwien.vcfvisualize.knowledgebase;

import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
class ApiCache {

    private HashMap<ApiRequest, ApiResponse> map;

    public ApiCache() {
        map = new HashMap<>();
    }

    public void addElement(ApiRequest key, ApiResponse value) {
        map.put(key, value);
    }

    /**
     * returns the element or null if not found
     * @param key the key
     * @return the element
     */
    public ApiResponse getElement(ApiRequest key) {
        if (map.containsKey(key)) {
            return map.get(key);
        }

        return null;
    }

}
