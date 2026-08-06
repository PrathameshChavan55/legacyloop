/**
 * REST endpoints. One controller per feature area. Keep them thin: validate the
 * request, call one service method, wrap the result in {@link com.legacyloop.common.ApiResponse}.
 * Put the role rule on the method with {@code @PreAuthorize}, not in a config file.
 *
 * <p>Classes expected here (from the API contract in {@code docs/API.md}):
 * <ul>
 *   <li>AuthController</li>
 *   <li>ProfileController</li>
 *   <li>InstitutionController</li>
 *   <li>AcademicController</li>
 *   <li>BillingController</li>
 *   <li>AdminUserController</li>
 *   <li>InternalController</li>
 * </ul>
 *
 * <p>Owners: Member 1 (auth) + Member 2 (profiles, institution, academics) + Member 6 (billing, admin). Add your own files — do not edit a file another member owns.
 */
package com.legacyloop.user.controller;
