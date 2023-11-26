package gmm;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import gmm.service.data.DataBaseInitNotifier;

/**
 * @author Jan Mothes
 */
@Component(value = "blockUntilReadyFilter")
public class BlockUntilReadyFilter extends GenericFilterBean {
	
	private DataBaseInitNotifier dataInit;
	
	@Autowired
	public void setDataInit(DataBaseInitNotifier dataInit) {
		this.dataInit = dataInit;
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		if (dataInit == null) {
			throw new IllegalStateException("Filter injection failed!");
		}
//		testDelay();
		while (!dataInit.isInitDone()) {
			try {
				System.out.println("Filter waiting...");
				synchronized (dataInit) {
					dataInit.wait();
				}
			} catch (final InterruptedException e) {}
		}
		chain.doFilter(request, response);
	}
	
	@SuppressWarnings("unused")
	private void testDelay() {
		try {
			Thread.sleep(3000);
		} catch (final InterruptedException e) {}
	}
}