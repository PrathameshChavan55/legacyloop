/**
 * Database tables (JPA {@code @Entity} here; Mongo {@code @Document} in social-service).
 * Entities never leave the service layer — map them to a DTO before returning.
 *
 * <p>Classes expected here (from the API contract in {@code docs/API.md}):
 * <ul>
 *   <li>Post</li>
 *   <li>Comment</li>
 *   <li>Connection</li>
 *   <li>Conversation</li>
 *   <li>Message</li>
 *   <li>Notification</li>
 * </ul>
 *
 * <p>Owners: Member 5 (feed, network) + Member 6 (chat, notifications). Add your own files — do not edit a file another member owns.
 */
package com.legacyloop.social.entity;
