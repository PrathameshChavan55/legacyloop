package com.legacyloop.social;

import com.legacyloop.common.AuthUser;
import com.legacyloop.common.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * One STOMP endpoint at {@code /ws} for both chat and notifications.
 *
 * <p>The original registered two endpoints with two configurations and two interceptors; a browser
 * then held two sockets open to say the same thing twice. One connection carries both, separated
 * by topic: {@code /topic/conversations/{id}} and {@code /topic/notifications/{userId}}.
 *
 * <p>The token arrives in the STOMP CONNECT frame rather than a query string, because a URL ends
 * up in access logs and browser history and a frame body does not.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // An in-memory broker: fine for one instance, which is what this deployment is.
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String header = accessor.getFirstNativeHeader("Authorization");
                    if (header != null && header.startsWith("Bearer ")) {
                        AuthUser user = jwtService.parse(header.substring(7));
                        if (user != null) {
                            var authorities = user.roles().stream().map(SimpleGrantedAuthority::new).toList();
                            accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, authorities));
                        }
                    }
                }
                return message;
            }
        });
    }
}
