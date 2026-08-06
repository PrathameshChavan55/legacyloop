/**
 * Spring Data interfaces. Use {@code @Query} with joins or {@code @EntityGraph}
 * where a list endpoint would otherwise fire one query per row (the N+1 problem).
 *
 * <p>Classes expected here (from the API contract in {@code docs/API.md}):
 * <ul>
 *   <li>PostRepository</li>
 *   <li>CommentRepository</li>
 *   <li>ConnectionRepository</li>
 *   <li>ConversationRepository</li>
 *   <li>MessageRepository</li>
 *   <li>NotificationRepository</li>
 * </ul>
 *
 * <p>Owners: Member 5 (feed, network) + Member 6 (chat, notifications). Add your own files — do not edit a file another member owns.
 */
package com.legacyloop.social.repository;
