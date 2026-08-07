package com.legacyloop.user.service;

import com.legacyloop.common.ApiException;
import com.legacyloop.common.PageResponse;
import com.legacyloop.user.dto.InstitutionDtos.BrandingResponse;
import com.legacyloop.user.dto.InstitutionDtos.InstitutionRequest;
import com.legacyloop.user.dto.InstitutionDtos.InstitutionResponse;
import com.legacyloop.user.entity.Institution;
import com.legacyloop.user.repository.InstitutionRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tenant records: branding, the identifier rule and the staff role label. */
@Service
@RequiredArgsConstructor
public class InstitutionService {

    private final InstitutionRepository institutions;

    /** Public: the sign-up page lists these before anybody has a token. */
    @Transactional(readOnly = true)
    public List<BrandingResponse> listBranding() {
        return institutions.findByActiveTrueOrderByNameAsc().stream().map(BrandingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BrandingResponse branding(Long institutionId) {
        return BrandingResponse.from(load(institutionId));
    }

    @Transactional(readOnly = true)
    public InstitutionResponse findById(Long institutionId) {
        return InstitutionResponse.from(load(institutionId));
    }

    @Transactional(readOnly = true)
    public InstitutionResponse findByCode(String code) {
        return institutions.findByCode(code.toUpperCase(Locale.ROOT)).map(InstitutionResponse::from)
                .orElseThrow(() -> ApiException.notFound("Institution", code));
    }

    @Transactional(readOnly = true)
    public PageResponse<InstitutionResponse> search(String query, Boolean active, Pageable pageable) {
        String like = query == null || query.isBlank() ? null : "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        return PageResponse.of(institutions.search(like, active, pageable), InstitutionResponse::from);
    }

    @Transactional
    public InstitutionResponse create(InstitutionRequest request) {
        if (institutions.existsByCode(request.code())) {
            throw ApiException.conflict("An institution with that code already exists");
        }
        Institution institution = new Institution();
        apply(institution, request);
        institution.setCode(request.code());
        return InstitutionResponse.from(institutions.save(institution));
    }

    @Transactional
    public InstitutionResponse update(Long institutionId, InstitutionRequest request) {
        Institution institution = load(institutionId);
        apply(institution, request);
        return InstitutionResponse.from(institution);
    }

    @Transactional
    public InstitutionResponse setActive(Long institutionId, boolean active) {
        Institution institution = load(institutionId);
        institution.setActive(active);
        return InstitutionResponse.from(institution);
    }

    /** Blank optional fields keep their previous value rather than being wiped. */
    private void apply(Institution institution, InstitutionRequest request) {
        institution.setName(request.name());
        institution.setShortName(request.shortName());
        institution.setLogoUrl(request.logoUrl());
        institution.setContactEmail(request.contactEmail());
        institution.setCity(request.city());
        institution.setIdentifierPattern(request.identifierPattern());
        if (request.primaryColor() != null && !request.primaryColor().isBlank()) {
            institution.setPrimaryColor(request.primaryColor());
        }
        if (request.identifierLabel() != null && !request.identifierLabel().isBlank()) {
            institution.setIdentifierLabel(request.identifierLabel());
        }
        if (request.staffRoleLabel() != null && !request.staffRoleLabel().isBlank()) {
            institution.setStaffRoleLabel(request.staffRoleLabel());
        }
    }

    private Institution load(Long institutionId) {
        return institutions.findById(institutionId)
                .orElseThrow(() -> ApiException.notFound("Institution", institutionId));
    }
}
