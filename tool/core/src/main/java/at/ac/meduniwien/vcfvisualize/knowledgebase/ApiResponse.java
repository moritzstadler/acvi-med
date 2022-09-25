package at.ac.meduniwien.vcfvisualize.knowledgebase;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class ApiResponse {

    @Getter
    @Setter
    private LocalDateTime requestTime;

    @Getter
    @Setter
    private ApiResponseBody apiResponseBody;

    public ApiResponse(LocalDateTime requestPerformed, ApiResponseBody apiResponseBody) {
        this.requestTime = requestPerformed;
        this.apiResponseBody = apiResponseBody;
    }
}
