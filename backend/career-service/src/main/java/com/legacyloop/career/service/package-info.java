/**
 * Business logic and transactions. Controllers call these; these call repositories.
 * Throw {@link com.legacyloop.common.ApiException} with an
 * {@link com.legacyloop.common.ErrorCode} instead of returning error strings.
 *
 * <p>Classes expected here (from the API contract in {@code docs/API.md}):
 * <ul>
 *   <li>JobService</li>
 *   <li>ApplicationService</li>
 *   <li>ResumeService</li>
 *   <li>AiService</li>
 *   <li>AnalyticsService</li>
 *   <li>GeminiClient</li>
 *   <li>UserClient</li>
 * </ul>
 *
 * <p>Owners: Member 3 (jobs, applications) + Member 4 (resumes, AI, analytics). Add your own files — do not edit a file another member owns.
 */
package com.legacyloop.career.service;
