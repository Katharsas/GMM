package gmm.service.data;

import org.springframework.stereotype.Service;

@Service
public class DataBaseInitNotifier {
	
	private boolean initDone = false;
	
	public boolean isInitDone() {
		return initDone;
	}
	
	protected void setInitDone() {
		initDone = true;
		synchronized(this) {
			this.notifyAll();
		}
	}
}
