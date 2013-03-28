/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Factory creating endpoints exposing {@link WebAdminUI}.
 * @author K. Benedyczak
 */
@Component
public class WebAdminEndpointFactory implements EndpointFactory
{
	public static final String NAME = "WebAdminUI";
	public static final String SERVLET_PATH = "/admin";

	private EndpointTypeDescription description;
	private ApplicationContext applicationContext;
	
	@Autowired
	public WebAdminEndpointFactory(ApplicationContext applicationContext)
	{
		this.applicationContext = applicationContext;
		
		Set<String> supportedAuthn = new HashSet<String>();
		supportedAuthn.add(VaadinAuthentication.NAME);
		description = new EndpointTypeDescription(NAME, 
				"Web administrative user interface", supportedAuthn);
	}
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return description;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new VaadinEndpoint(getDescription(), applicationContext, 
				WebAdminUI.class.getSimpleName(), SERVLET_PATH);
	}
}
