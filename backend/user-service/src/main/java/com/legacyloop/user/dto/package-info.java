/**
 * Request and response records. One file per feature area holding several nested records,
 * e.g. {@code JobDtos.CreateRequest}, {@code JobDtos.Summary}, {@code JobDtos.Detail}.
 *
 * <p>Classes expected here (from the API contract in {@code docs/API.md}):
 * <ul>
 *   <li>AuthDtos</li>
 *   <li>UserDtos</li>
 *   <li>ProfileDtos</li>
 *   <li>InstitutionDtos</li>
 *   <li>AcademicDtos</li>
 *   <li>BillingDtos</li>
 * </ul>
 *
 * <p>Owners: Member 1 (auth) + Member 2 (profiles, institution, academics) + Member 6 (billing, admin). Add your own files — do not edit a file another member owns.
 */
package com.legacyloop.user.dto;
