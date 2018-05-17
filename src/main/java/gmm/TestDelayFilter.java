package gmm;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.web.filter.GenericFilterBean;

public class TestDelayFilter extends GenericFilterBean {
	 
    @Override
    public void doFilter(
      ServletRequest request, 
      ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    	try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {}
        chain.doFilter(request, response);
    }
}