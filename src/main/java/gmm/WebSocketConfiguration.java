package gmm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import gmm.collections.ArrayList;
import gmm.collections.List;

/**
 * http://www.devglan.com/spring-boot/spring-websocket-integration-example-without-stomp
 * 
 * @author Jan Mothes
 */
@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {
	
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(getWebsocketHandler(), "/notifier");
	}
	
	@Bean 
	public WebSocketHandlerImpl getWebsocketHandler() {
		return new WebSocketHandlerImpl();
	}
	
	public static class WebSocketHandlerImpl extends AbstractWebSocketHandler  {
		
		List<WebSocketSession> sessions = new ArrayList<>(WebSocketSession.class);
		
		public List<WebSocketSession> getSessions() {
			return sessions;
		}
		
		@Override
		public synchronized void afterConnectionEstablished(WebSocketSession session) throws Exception {
			sessions.add(session);
		}
		
		@Override
		public synchronized void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
			sessions.remove(session);
		}
	}
}