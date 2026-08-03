package com.legacyloop.core.entity;

import jakarta.persistence.*;
import lombok.*;

/** The file itself lives in S3; only the pointer and the extracted text live here. */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "resume_metadata", indexes = @Index(name = "idx_resume_user", columnList = "user_id"))
public class ResumeMetadata extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "file_name", nullable = false, length = 260)
    private String fileName;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /** Extracted once at upload so the AI worker never re-downloads the PDF. */
    @Lob
    @Column(name = "extracted_text", columnDefinition = "LONGTEXT")
    private String extractedText;

    @Column(name = "is_primary", nullable = false)
    private boolean primaryResume;
}
