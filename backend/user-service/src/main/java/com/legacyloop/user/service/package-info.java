/**
 * Business logic and transactions. Controllers call these; these call repositories.
 * Throw {@link com.legacyloop.common.ApiException} with an
 * {@link com.legacyloop.common.ErrorCode} instead of returning error strings.
 *
 * <p>Classes expected here (from the API contract in {@code docs/API.md}):
 * <ul>
 *   <li>AuthService</li>
 *   <li>ProfileService</li>
 *   <li>EmailService</li>
 *   <li>InstitutionService</li>
 *   <li>AcademicService</li>
 *   <li>BillingService</li>
 *   <li>UserAdminService</li>
 * </ul>
 *
 * <p>Owners: Member 1 (auth) + Member 2 (profiles, institution, academics) + Member 6 (billing, admin). Add your own files — do not edit a file another member owns.
 */
package com.legacyloop.user.service;
