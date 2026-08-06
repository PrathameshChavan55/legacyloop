/**
 * REST endpoints. One controller per feature area. Keep them thin: validate the
 * request, call one service method, wrap the result in {@link com.legacyloop.common.ApiResponse}.
 * Put the role rule on the method with {@code @PreAuthorize}, not in a config file.
 *
 * <p>Classes expected here (from the API contract in {@code docs/API.md}):
 * <ul>
 *   <li>FeedController</li>
 *   <li>NetworkController</li>
 *   <li>ChatController</li>
 *   <li>NotificationController</li>
 * </ul>
 *
 * <p>Owners: Member 5 (feed, network) + Member 6 (chat, notifications). Add your own files — do not edit a file another member owns.
 */
package com.legacyloop.social.controller;
