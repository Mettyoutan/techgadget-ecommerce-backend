package com.techgadget.ecommerce.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class ErrorResponse {
    @Setter(AccessLevel.NONE)
    private boolean success = false;
    private String code;
    private int status;
    private String message;
    private List<String> details;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Setter(AccessLevel.NONE)
    private LocalDateTime timestamp = LocalDateTime.now();

    public ErrorResponse(String code, int status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public ErrorResponse(String code, int status, String message, List<String> details) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.details = details;
    }
}
