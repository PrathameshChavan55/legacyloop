package com.legacyloop.career.service;

import com.legacyloop.career.dto.ResumeDtos.ResumeResponse;
import com.legacyloop.career.entity.Resume;
import com.legacyloop.career.repository.ResumeRepository;
import com.legacyloop.common.ApiException;
import com.legacyloop.common.AuthUser;
import com.legacyloop.common.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Uploaded CVs: store the file, pull the text out of it, hand it back on download.
 *
 * <p>PDFBox extracts the text synchronously on upload so that later job-fit and AI calls have it
 * ready in the database without re-parsing the file.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain");

    private static final long MAX_FILE_BYTES = 5 * 1024 * 1024; // 5 MB

    private final ResumeRepository resumes;

    @Value("${legacyloop.storage.resumes:uploads/resumes}")
    private String storageDir;

    @Transactional
    public ResumeResponse upload(MultipartFile file, String label, Long ownerUserId) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Please choose a file to upload");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw ApiException.badRequest("Resumes must be 5 MB or smaller");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Upload a PDF, Word document (.doc, .docx) or plain text file");
        }

        if (resumes.countByOwnerUserId(ownerUserId) >= 5) {
            throw ApiException.badRequest("You may have at most 5 resumes saved. Delete an older one first.");
        }

        String originalName = file.getOriginalFilename() == null ? "resume" : file.getOriginalFilename();
        String effectiveLabel = (label == null || label.isBlank())
                ? stripExtension(originalName) : label.trim();

        Path stored = store(file, ownerUserId, originalName);
        String extracted = extractText(stored, contentType);

        boolean firstResume = resumes.countByOwnerUserId(ownerUserId) == 0;

        Resume resume = Resume.builder()
                .ownerUserId(ownerUserId)
                .label(effectiveLabel)
                .originalFilename(originalName)
                .contentType(contentType)
                .sizeBytes(file.getSize())
                .storedPath(stored.toString())
                .extractedText(extracted)
                .primary(firstResume)
                .build();

        resume = resumes.save(resume);
        log.info("User {} uploaded resume {}", ownerUserId, resume.getId());
        return ResumeResponse.from(resume);
    }

    @Transactional(readOnly = true)
    public List<ResumeResponse> list(Long ownerUserId) {
        return resumes.findByOwnerUserIdOrderByIdDesc(ownerUserId).stream()
                .map(ResumeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ResumeResponse findById(Long resumeId, AuthUser user) {
        Resume resume = resumes.findById(resumeId)
                .orElseThrow(() -> ApiException.notFound("Resume", resumeId));
        boolean isOwner = resume.getOwnerUserId().equals(user.id());
        boolean canReview = user.isAdmin() || user.isStaff() || user.hasRole("ALUMNI");
        if (!isOwner && !canReview) {
            throw ApiException.forbidden("You do not have permission to view this resume");
        }
        return ResumeResponse.from(resume);
    }

    /** The bytes plus the original name, so the controller can set a sensible download header. */
    @Transactional(readOnly = true)
    public DownloadPayload download(Long resumeId, AuthUser user) {
        Resume resume = resumes.findById(resumeId)
                .orElseThrow(() -> ApiException.notFound("Resume", resumeId));
        boolean isOwner = resume.getOwnerUserId().equals(user.id());
        boolean canReview = user.isAdmin() || user.isStaff() || user.hasRole("ALUMNI");
        if (!isOwner && !canReview) {
            throw ApiException.forbidden("You do not have permission to view this resume");
        }
        Resource resource = new FileSystemResource(Path.of(resume.getStoredPath()));
        if (!resource.exists()) {
            throw ApiException.notFound("The stored file for this resume is missing");
        }
        return new DownloadPayload(resource, resume.getOriginalFilename(), resume.getContentType());
    }

    public record DownloadPayload(Resource resource, String filename, String contentType) {
    }

    /** Exactly one resume per user is primary; setting a new one clears the old. */
    @Transactional
    public ResumeResponse makePrimary(Long resumeId, Long ownerUserId) {
        Resume resume = load(resumeId, ownerUserId);
        resumes.findByOwnerUserIdOrderByIdDesc(ownerUserId)
                .forEach(candidate -> candidate.setPrimary(false));
        resume.setPrimary(true);
        return ResumeResponse.from(resume);
    }

    @Transactional
    public ResumeResponse rename(Long resumeId, Long ownerUserId, String label) {
        Resume resume = load(resumeId, ownerUserId);
        resume.setLabel(label.trim());
        return ResumeResponse.from(resume);
    }

    @Transactional
    public void delete(Long resumeId, Long ownerUserId) {
        Resume resume = load(resumeId, ownerUserId);
        try {
            Files.deleteIfExists(Path.of(resume.getStoredPath()));
        } catch (IOException ex) {
            // The row is the record that matters; a stray file is not worth failing the request.
            log.warn("Could not delete {}: {}", resume.getStoredPath(), ex.getMessage());
        }
        resumes.delete(resume);

        if (resume.isPrimary()) {
            resumes.findByOwnerUserIdOrderByIdDesc(ownerUserId).stream().findFirst()
                    .ifPresent(next -> next.setPrimary(true));
        }
    }

    /** The text an AI analysis works from. */
    @Transactional(readOnly = true)
    public String textOf(Long resumeId, Long ownerUserId) {
        Resume resume = load(resumeId, ownerUserId);
        String text = resume.getExtractedText();
        if (text == null || text.isBlank()) {
            throw ApiException.badRequest("We could not read any text from that file. "
                    + "Upload a text-based PDF rather than a scan.");
        }
        return text;
    }

    @Transactional(readOnly = true)
    public Resume load(Long resumeId, Long ownerUserId) {
        return resumes.findByIdAndOwnerUserId(resumeId, ownerUserId)
                .orElseThrow(() -> ApiException.notFound("Resume", resumeId));
    }

    /** Stored under a random name so two people uploading "resume.pdf" cannot collide. */
    private Path store(MultipartFile file, Long ownerUserId, String originalName) {
        try {
            Path directory = Path.of(storageDir, String.valueOf(ownerUserId));
            Files.createDirectories(directory);
            Path target = directory.resolve(UUID.randomUUID() + extensionOf(originalName));
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } catch (IOException ex) {
            log.error("Could not store an upload for user {}", ownerUserId, ex);
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "We could not save that file");
        }
    }

    private String extractText(Path path, String contentType) {
        try {
            if ("text/plain".equals(contentType)) {
                return Files.readString(path);
            }
            if (!"application/pdf".equals(contentType)) {
                // Word documents are stored and downloadable; only the AI features need the text.
                return null;
            }
            try (PDDocument document = Loader.loadPDF(path.toFile())) {
                return new PDFTextStripper().getText(document);
            }
        } catch (Exception ex) {
            log.warn("Could not read text from {}: {}", path, ex.getMessage());
            return null;
        }
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot);
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot <= 0 ? filename : filename.substring(0, dot);
    }
}
