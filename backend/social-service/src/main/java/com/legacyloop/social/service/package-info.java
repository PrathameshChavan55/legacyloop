/**
 * Business logic and transactions. Controllers call these; these call repositories.
 * Throw {@link com.legacyloop.common.ApiException} with an
 * {@link com.legacyloop.common.ErrorCode} instead of returning error strings.
 *
 * <p>Classes expected here (from the API contract in {@code docs/API.md}):
 * <ul>
 *   <li>PostService</li>
 *   <li>ConnectionService</li>
 *   <li>ChatService</li>
 *   <li>NotificationService</li>
 *   <li>PeopleClient</li>
 * </ul>
 *
 * <p>Owners: Member 5 (feed, network) + Member 6 (chat, notifications). Add your own files — do not edit a file another member owns.
 */
package com.legacyloop.social.service;
