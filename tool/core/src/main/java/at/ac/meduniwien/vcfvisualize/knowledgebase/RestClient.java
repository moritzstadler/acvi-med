package at.ac.meduniwien.vcfvisualize.knowledgebase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class RestClient {

    public static final int SECONDS_RETAINED_IN_API_CACHE = 12 * 60 * 60;
    public static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    @Autowired
    ApiCache apiCache;

    //TODO try Class<?> as parameter
    //else try parametizedlisttypes
    public ApiResponse performRequest(ApiRequest apiRequest, ApiResponseBody apiResponseBody) {
        ApiResponse response = apiCache.getElement(apiRequest);

        if (response == null || response.getRequestTime().plusSeconds(SECONDS_RETAINED_IN_API_CACHE).isBefore(LocalDateTime.now())) {
            response = callApi(apiRequest, apiResponseBody);
            apiCache.addElement(apiRequest, response);
        }

        return response;
    }

    public ApiResponse performRequestSkipCache(ApiRequest apiRequest, ApiResponseBody apiResponseBody) {
        ApiResponse response = callApi(apiRequest, apiResponseBody);
        apiCache.addElement(apiRequest, response);
        return response;
    }

    private ApiResponse callApi(ApiRequest apiRequest, ApiResponseBody apiResponseBody) {
        WebClient webClient = WebClient.builder()
                .baseUrl(apiRequest.getUrl())
                .exchangeStrategies(ExchangeStrategies.builder()
                .codecs(config -> config
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024))
                .build())
                .build();


        ApiResponseBody actualResponse = webClient
                .get()
                .retrieve()
                .bodyToMono(apiResponseBody.getClass())
                .block(REQUEST_TIMEOUT);

        return new ApiResponse(LocalDateTime.now(), actualResponse);
    }

    public String callApiRaw(ApiRequest apiRequest) {
        WebClient webClient = WebClient.builder()
                .baseUrl(apiRequest.getUrl())
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(config -> config
                                .defaultCodecs()
                                .maxInMemorySize(16 * 1024 * 1024))
                        .build())
                .build();


        String stringResponse = webClient
                .get()
                .retrieve()
                .bodyToMono(String.class)
                .block(REQUEST_TIMEOUT);

        return stringResponse;
    }

}
