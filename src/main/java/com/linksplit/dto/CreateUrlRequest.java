package com.linksplit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUrlRequest {
    @NotBlank(message = "URL cannot be empty")
    @Size(max = 2048, message = "URL is too long (max 2048 characters)")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    private String longUrl;
}