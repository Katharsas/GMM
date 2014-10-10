/**
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * modified
 * 
 * 
 * dont need this
 * just use spring 4 MockHttpServletRequest and check tzhe dispatchertype (and set it if needed)
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */
package com.technologicaloddity.capturejsp.util;

import javax.servlet.DispatcherType;

import org.springframework.mock.web.MockHttpServletRequest;


public class MockIncludedHttpServletRequest extends MockHttpServletRequest {
	
	public MockIncludedHttpServletRequest() {
		super();
	}
	
	public DispatcherType getDispatcherType() {		
		return DispatcherType.INCLUDE;
	}

	public boolean isAsyncSupported() {
		return false;
	}
}
