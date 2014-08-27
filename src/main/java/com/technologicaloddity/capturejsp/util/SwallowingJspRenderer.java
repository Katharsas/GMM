/*
 * MODIFIED by Jan Mothes
 * 
 * Updated to benefit from Spring 4 changes (Servlet 3.0 compability):
 * - Removed custom MockIncludedHttpServletRequest class
 * - Removed custom Reslovers
 * - Removed locale
 * 
 */
package com.technologicaloddity.capturejsp.util;

import gmm.service.Spring;
import gmm.util.EnumerationIterator;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;

import org.directwebremoting.WebContext;
import org.directwebremoting.util.SwallowingHttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;

@Component
public class SwallowingJspRenderer implements ServletContextAware {
	
	@Autowired
	private TechOddViewResolver viewResolver;
//	@Autowired
//	private LocaleResolver localeResolver;
	
	private ServletContext servletContext;
	
	public String render(String viewName, ModelMap modelMap, HttpServletRequest realRequest, HttpServletResponse realResponse) throws IOException  {		
		String result = null;
		
		// These String objects are used to capture the output
		// of SwallowingHttpServletResponse
		StringWriter sout = new StringWriter();
		StringBuffer sbuffer = sout.getBuffer();
		
		// Set up a fake request and response.  We need the mock response
		// so that we can create the Swallowing response
//		MockHttpServletRequest request = new MockHttpServletRequest();
//		request.setDispatcherType(DispatcherType.INCLUDE);
//		if(!DispatcherType.INCLUDE.equals(request.getDispatcherType())) {
//			throw new IllegalStateException("Wrong DispatcherType! Type is " + request.getDispatcherType().toString());
//		}
		
//		HttpServletResponse response = new MockHttpServletResponse();
//		HttpServletResponse swallowingResponse = new SwallowingHttpServletResponse(response, sout, "UTF-8");
		
		
		//Stackoverflow


//    	HttpServletResponse realResponse = response;
    	HttpServletResponse fakeResponse = new SwallowingHttpServletResponse(realResponse, sout, realResponse.getCharacterEncoding());

//    	HttpServletRequest realRequest = request;
    	realRequest.setAttribute(WebContext.ATTRIBUTE_DWR, Boolean.TRUE);

    	

//    	result = buffer.toString();
    	//stackoverflow end
		

		// Use our own LocaleResolver here, or Spring will try to meddle with it
		//TODO remove this if possible?
//		Locale locale = LocaleContextHolder.getLocale();
//		LocaleResolver localeResolver = new JspLocaleResolver();
//		localeResolver.setLocale(request, swallowingResponse, locale);
		
		try {			
			//Add the modelMap to the request as attributes
//			addModelAsRequestAttributes(request, modelMap);
			addModelAsRequestAttributes(realRequest, modelMap);
			
//			request.setParameters(originalRequest.getParameterMap());
//			for(String attributeName : (new EnumerationIterator<String>(originalRequest.getAttributeNames()))) {
//				request.setAttribute(attributeName, originalRequest.getAttribute(attributeName));
//			}
//			request.setDispatcherType(DispatcherType.INCLUDE);
			
			// Push our LocaleResolver into the request
//			request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, localeResolver);
//			request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, originalRequest.getAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE));
			
			// Push our Locale into the request
//			LocalizationContext localizationContext = new LocalizationContext(null, locale);
//			request.setAttribute(Config.FMT_LOCALIZATION_CONTEXT+".request", localizationContext);
//			request.setAttribute(Config.FMT_LOCALE, locale);
//			request.setAttribute(Config.FMT_LOCALIZATION_CONTEXT+".request", originalRequest.getAttribute(Config.FMT_LOCALIZATION_CONTEXT+".request"));
//			request.setAttribute(Config.FMT_LOCALE, originalRequest.getAttribute(Config.FMT_LOCALE));
			
			// Make sure we are using UTF-8 for the rendered JSP
//			swallowingResponse.setContentType("text/html; charset=utf-8");
			
			// "include" the file (but not really an include) with the dispatcher
			// The resulting rendering will come out in swallowing response,
			// via sbuffer
//			RequestDispatcher dispatcher = servletContext.getRequestDispatcher(viewResolver.urlForView(viewName));
			
//			dispatcher.include(request, swallowingResponse);
//			dispatcher.include(originalRequest, swallowingResponse);
			
			
			//stackoverflow
			Spring.getServletContext().getRequestDispatcher(viewResolver.urlForView(viewName)).forward(realRequest, fakeResponse);
			//stackoverflow end
			
			result = sbuffer.toString();
		} catch(Exception e) {
			throw new IOException(e);
		}
		
		return result;
	}

	/*
	 * Moves the items in the map to be request.attributes
	 */
	private void addModelAsRequestAttributes(ServletRequest request, ModelMap modelMap) {
		if(modelMap != null && request != null) {
			for (Map.Entry<String, Object> entry : modelMap.entrySet()) {
				String modelName = entry.getKey();
				Object modelValue = entry.getValue();
				if(modelValue != null) {
					request.setAttribute(modelName, modelValue);
				} else {
					request.removeAttribute(modelName);
				}				
			}
		}
	}
	
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;		
	}
}
