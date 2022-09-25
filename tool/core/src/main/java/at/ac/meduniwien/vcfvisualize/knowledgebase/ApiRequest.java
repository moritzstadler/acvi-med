package at.ac.meduniwien.vcfvisualize.knowledgebase;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public class ApiRequest {

    @Getter
    @Setter
    private String url;

    @Getter
    @Setter
    private ApiRequestBody apiRequestBody;

    public ApiRequest(String url, ApiRequestBody apiRequestBody) {
        this.url = url;
        this.apiRequestBody = apiRequestBody;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiRequest that = (ApiRequest) o;
        return url.equals(that.url) &&
                ((apiRequestBody == null && that.apiRequestBody == null) || apiRequestBody.equals(that.apiRequestBody));
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, apiRequestBody);
    }
}
