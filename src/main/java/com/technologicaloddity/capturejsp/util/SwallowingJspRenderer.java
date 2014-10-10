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
	
	public String renderA(String viewName, ModelMap modelMap, HttpServletRequest realRequest, HttpServletResponse realResponse) {
		
		//http://stackoverflow.com/questions/1719254/jsp-programmatically-render/1719549#1719549
		
		StringWriter sout = new StringWriter();
		StringBuffer sbuffer = sout.getBuffer();
		
		HttpServletResponse fakeResponse = new SwallowingHttpServletResponse(realResponse, sout, realResponse.getCharacterEncoding());
    	fakeResponse.setLocale(LocaleContextHolder.getLocale());
    	
    	realRequest.setAttribute(WebContext.ATTRIBUTE_DWR, Boolean.TRUE);
    	addModelAsRequestAttributes(realRequest, modelMap);
    	
    	try {
    		RequestDispatcher dispatcher = Spring.getServletContext().getRequestDispatcher(viewResolver.urlForView(viewName));
    		dispatcher.forward(realRequest, fakeResponse);
		} catch(Exception e) {
			e.printStackTrace();
		}
    	
    	return sbuffer.toString();
	}
	
	public String renderB(String viewName, ModelMap modelMap, HttpServletRequest realRequest, HttpServletResponse realResponse) {		
		
		//http://technologicaloddity.com/2011/10/04/render-and-capture-the-output-of-a-jsp-as-a-string/3/
		
		String result = null;
		Locale locale = Locale.getDefault();
		System.out.println("Current Locale: "+locale.toString());
		
		StringWriter sout = new StringWriter();
        StringBuffer sbuffer = sout.getBuffer();
        
        HttpServletRequest request = new MockIncludedHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        HttpServletResponse swallowingResponse = new SwallowingHttpServletResponse(response, sout, "UTF-8");
		
        LocaleResolver localeResolver = new JspLocaleResolver();
        localeResolver.setLocale(request, swallowingResponse, locale);
        
		try {			
			addModelAsRequestAttributes(request, modelMap);
			
			request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, localeResolver);
			
			LocalizationContext localizationContext = new LocalizationContext(null, locale);
            request.setAttribute(Config.FMT_LOCALIZATION_CONTEXT+".request", localizationContext);
            request.setAttribute(Config.FMT_LOCALE, locale);
            
            swallowingResponse.setContentType("text/html; charset=utf-8");
            
            RequestDispatcher dispatcher = servletContext.getRequestDispatcher(viewResolver.urlForView(viewName));
            
//            String m = Spring.getApplicationContext().getMessage("tasks.menu.models", null, locale);
//            System.out.println(m);
            
            dispatcher.include(request, swallowingResponse);
			
			result = sbuffer.toString();
		} catch(Exception e) {
			e.printStackTrace();
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
