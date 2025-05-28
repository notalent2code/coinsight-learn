package id.co.bankbsi.coinsight.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response structure used across all microservices
 * in the Coinsight application for consistent error handling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * Timestamp when the error occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * HTTP status code
     */
    private int status;
    
    /**
     * Error type/category
     */
    private String error;
    
    /**
     * Error message describing what went wrong
     */
    private String message;
    
    /**
     * Request path where the error occurred
     */
    private String path;
    
    /**
     * Validation errors for input validation failures
     * Maps field names to error messages
     */
    private Map<String, String> validationErrors;
}
