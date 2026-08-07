package com.legacyloop.career.dto;

import com.legacyloop.career.entity.Company;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CompanyDtos {

    private CompanyDtos() {
    }

    public record CompanyRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 2000) String description,
            @Size(max = 300) String website,
            @Size(max = 500) String logoUrl,
            @Size(max = 120) String industry,
            @Size(max = 160) String headquarters,
            @Size(max = 40) String sizeBand,
            @Size(max = 120) String contactName,
            @Email @Size(max = 180) String contactEmail,
            @Size(max = 20) String contactPhone) {

        public CompanyRequest {
            name = name == null ? null : name.trim();
        }
    }

    public record CompanyResponse(Long id, String name, String description, String website, String logoUrl,
                                  String industry, String headquarters, String sizeBand, String contactName,
                                  String contactEmail, String contactPhone, boolean verified, boolean active) {

        public static CompanyResponse from(Company company) {
            return new CompanyResponse(company.getId(), company.getName(), company.getDescription(),
                    company.getWebsite(), company.getLogoUrl(), company.getIndustry(),
                    company.getHeadquarters(), company.getSizeBand(), company.getContactName(),
                    company.getContactEmail(), company.getContactPhone(), company.isVerified(),
                    company.isActive());
        }
    }

    /** The two fields a job card needs. */
    public record CompanySummary(Long id, String name, String logoUrl) {

        public static CompanySummary from(Company company) {
            return new CompanySummary(company.getId(), company.getName(), company.getLogoUrl());
        }
    }
}
