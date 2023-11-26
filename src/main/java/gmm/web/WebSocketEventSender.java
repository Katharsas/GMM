package gmm.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import gmm.WebSocketConfiguration.WebSocketHandlerImpl;
import gmm.domain.User;
import gmm.domain.task.Task;
import gmm.service.data.DataAccess;
import gmm.service.data.DataChangeEvent;
import gmm.util.ThrottlingExecutioner;

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
public class WebSocketEventSender {

	private final ThrottlingExecutioner executioner;
	
	public static enum WebSocketEvent {
		TaskDataChangeEvent,
//		TaskPinChangeEvent,
		NotificationChangeEvent,
//		WorkbenchChangeEvent,
//		PinnedListChangeEvent
//		AssetImportRunningEvent
		AssetFileOperationsChangeEvent
	}
	
	private final WebSocketHandlerImpl handler;
	
	@Autowired
	public WebSocketEventSender(DataAccess data, WebSocketHandlerImpl handler) {
		data.<Task>registerForUpdates(this::onEvent, Task.class);
		this.handler = handler;
		
		executioner = new ThrottlingExecutioner(2000);
	}
	
	@PreDestroy
	private void shutdown() {
		executioner.close();
	}

	private void onEvent(DataChangeEvent<? extends Task> event) {
		broadcastEvent(WebSocketEvent.TaskDataChangeEvent);
	}
	
	public void broadcastEvent(WebSocketEvent event) {
		sendEvent(event, Optional.empty());
	}
	
	public void unicastEvent(User target, WebSocketEvent event) {
		sendEvent(event, Optional.of(target));
	}
	
	private void sendEvent(WebSocketEvent event, Optional<User> target) {
		final String targetSuffix = target.isPresent() ? "@" + target.get().getIdLink() : "";
		final String eventText = event.name() + targetSuffix;
		
		executioner.curbYourEnthusiasm(eventText, () -> {
			sendEvent(eventText);
		});
	}
	
	private void sendEvent(String eventText) {
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
