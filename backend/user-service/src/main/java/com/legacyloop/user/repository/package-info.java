/**
 * Spring Data interfaces. Use {@code @Query} with joins or {@code @EntityGraph}
 * where a list endpoint would otherwise fire one query per row (the N+1 problem).
 *
 * <p>Classes expected here (from the API contract in {@code docs/API.md}):
 * <ul>
 *   <li>UserRepository</li>
 *   <li>RefreshTokenRepository</li>
 *   <li>OtpTokenRepository</li>
 *   <li>StudentProfileRepository</li>
 *   <li>AlumniProfileRepository</li>
 *   <li>InstitutionRepository</li>
 *   <li>AcademicUnitRepository</li>
 *   <li>SkillRepository</li>
 *   <li>PlanRepository</li>
 *   <li>SubscriptionRepository</li>
 *   <li>PaymentOrderRepository</li>
 * </ul>
 *
 * <p>Owners: Member 1 (auth) + Member 2 (profiles, institution, academics) + Member 6 (billing, admin). Add your own files — do not edit a file another member owns.
 */
package com.legacyloop.user.repository;
