package ega.api.egafinance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActivationResponse {
    private boolean success;
    private String message;
}