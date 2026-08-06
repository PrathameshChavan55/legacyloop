/**
 * Database tables (JPA {@code @Entity} here; Mongo {@code @Document} in social-service).
 * Entities never leave the service layer — map them to a DTO before returning.
 *
 * <p>Classes expected here (from the API contract in {@code docs/API.md}):
 * <ul>
 *   <li>User</li>
 *   <li>RefreshToken</li>
 *   <li>OtpToken</li>
 *   <li>StudentProfile</li>
 *   <li>AlumniProfile</li>
 *   <li>Institution</li>
 *   <li>AcademicUnit</li>
 *   <li>Skill</li>
 *   <li>Plan</li>
 *   <li>Subscription</li>
 *   <li>PaymentOrder</li>
 * </ul>
 *
 * <p>Owners: Member 1 (auth) + Member 2 (profiles, institution, academics) + Member 6 (billing, admin). Add your own files — do not edit a file another member owns.
 */
package com.legacyloop.user.entity;
