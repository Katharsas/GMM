package gmm.web.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * from tutorial:
 * http://www.concretepage.com/spring-4/spring-4-websocket-sockjs-stomp-tomcat-example
 * 
 * @author Jan Mothes
 *
 */
@Controller
public class WebSocketController {
	
	@MessageMapping("/add")
	@SendTo("/topic/showResult")
	public Result addNum(CalcInput input) throws Exception {
        Thread.sleep(2000);
        Result result = new Result(input.getNum1()+"+"+input.getNum2()+"="+(input.getNum1()+input.getNum2())); 
        return result;
    }

	@RequestMapping("/start")
	public String start() {
		return "start";
	}
	
	public static class CalcInput {
	    private int num1;
	    private int num2;
		public int getNum1() {
			return num1;
		}
		public void setNum1(int num1) {
			this.num1 = num1;
		}
		public int getNum2() {
			return num2;
		}
		public void setNum2(int num2) {
			this.num2 = num2;
		}    
	}
	
	public static class Result {
	    private String result;
	    public Result(String result) {
	        this.result = result;
	    }
		public String getResult() {
			return result;
		}
	}
}
