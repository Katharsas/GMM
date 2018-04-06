package gmm.web;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import gmm.WebSocketConfiguration.WebSocketHandlerImpl;
import gmm.domain.User;
import gmm.service.data.DataAccess;
import gmm.service.data.DataAccess.DataChangeCallback;
import gmm.service.data.DataChangeEvent;

/**
 * TODO:
 * Send WorkbenchChangeEvent and PinnedListChangeEvent to specific users only!
 * Currently, any user can receive "unicast" events!
 * 
 * Or just move the entire "mutating list state stuff" to the client, would be much easier.
 * 
 * @author Jan Mothes
 */
@Service
public class WebSocketEventSender implements DataChangeCallback {

	public static enum WebSocketEvent {
		TaskDataChangeEvent,
		NotificationChangeEvent,
//		WorkbenchChangeEvent,
//		PinnedListChangeEvent
	}
	
	@Autowired WebSocketHandlerImpl handler;
	
	@Autowired
	public WebSocketEventSender(DataAccess data) {
		data.registerForUpdates(this);
	}

	@Override
	public void onEvent(DataChangeEvent event) {
		broadcastEvent(WebSocketEvent.TaskDataChangeEvent);
	}
	
	public void broadcastEvent(WebSocketEvent event) {
		sendEvent(event.name());
	}
	
	public void unicastEvent(User target, WebSocketEvent event) {
		sendEvent(event.name() + "@" + target.getIdLink());
	}
	
	public void sendEvent(String eventText) {
		try {
			final TextMessage message = new TextMessage(eventText);
			synchronized (handler) {
				for (final WebSocketSession session : handler.getSessions()) {
					session.sendMessage(message);
				}
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
