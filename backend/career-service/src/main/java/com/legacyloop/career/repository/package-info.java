/**
 * Spring Data interfaces. Use {@code @Query} with joins or {@code @EntityGraph}
 * where a list endpoint would otherwise fire one query per row (the N+1 problem).
 *
 * <p>Classes expected here (from the API contract in {@code docs/API.md}):
 * <ul>
 *   <li>JobRepository</li>
 *   <li>JobApplicationRepository</li>
 *   <li>CompanyRepository</li>
 *   <li>ResumeRepository</li>
 *   <li>ResumeAnalysisRepository</li>
 *   <li>ReferralRequestRepository</li>
 * </ul>
 *
 * <p>Owners: Member 3 (jobs, applications) + Member 4 (resumes, AI, analytics). Add your own files — do not edit a file another member owns.
 */
package com.legacyloop.career.repository;
