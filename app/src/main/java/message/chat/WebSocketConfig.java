package message.chat;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라가 구독(subscribe)할 경로 prefix
        registry.enableSimpleBroker("/sub");
        // 클라가 서버로 보낼(send) 경로 prefix
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ws 접속 엔드포인트
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
        // SockJS 쓰고 싶으면 아래도 추가 (테스트 HTML에 sockjs 쓰면 편함)
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}