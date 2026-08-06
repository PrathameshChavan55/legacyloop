/**
 * REST endpoints. One controller per feature area. Keep them thin: validate the
 * request, call one service method, wrap the result in {@link com.legacyloop.common.ApiResponse}.
 * Put the role rule on the method with {@code @PreAuthorize}, not in a config file.
 *
 * <p>Classes expected here (from the API contract in {@code docs/API.md}):
 * <ul>
 *   <li>JobController</li>
 *   <li>ApplicationController</li>
 *   <li>ResumeController</li>
 *   <li>AiController</li>
 *   <li>AnalyticsController</li>
 * </ul>
 *
 * <p>Owners: Member 3 (jobs, applications) + Member 4 (resumes, AI, analytics). Add your own files — do not edit a file another member owns.
 */
package com.legacyloop.career.controller;
