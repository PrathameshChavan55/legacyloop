/**
 * Database tables (JPA {@code @Entity} here; Mongo {@code @Document} in social-service).
 * Entities never leave the service layer — map them to a DTO before returning.
 *
 * <p>Classes expected here (from the API contract in {@code docs/API.md}):
 * <ul>
 *   <li>Job</li>
 *   <li>JobApplication</li>
 *   <li>Company</li>
 *   <li>Resume</li>
 *   <li>ResumeAnalysis</li>
 *   <li>ReferralRequest</li>
 *   <li>Enums</li>
 * </ul>
 *
 * <p>Owners: Member 3 (jobs, applications) + Member 4 (resumes, AI, analytics). Add your own files — do not edit a file another member owns.
 */
package com.legacyloop.career.entity;
