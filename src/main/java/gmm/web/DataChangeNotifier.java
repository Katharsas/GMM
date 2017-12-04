package gmm.web;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import gmm.WebSocketConfiguration.WebSocketHandlerImpl;
import gmm.service.data.DataAccess;
import gmm.service.data.DataAccess.DataChangeCallback;
import gmm.service.data.DataChangeEvent;

@Controller
public class DataChangeNotifier implements DataChangeCallback {

	@Autowired WebSocketHandlerImpl handler;
	
	@Autowired
	public DataChangeNotifier(DataAccess data) {
		data.registerForUpdates(this);
	}

	@Override
	public void onEvent(DataChangeEvent event) {
		try {
			synchronized (handler) {
				for (final WebSocketSession session : handler.getSessions()) {
					session.sendMessage(new TextMessage("DataChangeEvent"));
				}
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
