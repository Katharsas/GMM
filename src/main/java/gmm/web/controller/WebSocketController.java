package gmm.web.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import gmm.service.UserService;

/**
 * from tutorial:
 * http://www.concretepage.com/spring-4/spring-4-websocket-sockjs-stomp-tomcat-example
 * 
 * @author Jan Mothes
 *
 */
@Controller
public class WebSocketController {
	
	@Autowired UserService users;
//	@Autowired SimpMessageSendingOperations sender;
	
	@MessageMapping("/chat/echo")
	@SendToUser("/queue/echoResult")
	public String sendEcho(String message, Principal p) throws Exception {
        return "To "+p.getName()+": "+message;
        //to send to a specific user return void and use SimpMessageSendingOperations
    }
	
	@MessageMapping("/chat/all")
	@SendTo("/topic/allResult")
	public String sendAll(String message, Principal p) throws Exception {
        return "To all: "+message;
    }

	@RequestMapping("/start")
	public String start() {
		return "start";
	}
}
