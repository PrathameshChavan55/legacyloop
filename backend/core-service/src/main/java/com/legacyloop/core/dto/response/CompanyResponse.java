package com.legacyloop.core.dto.response;

public record CompanyResponse(
        Long id,
        String name,
        String industry,
        String website,
        String logoUrl,
        String description,
        String location,
        boolean verified) {
}
