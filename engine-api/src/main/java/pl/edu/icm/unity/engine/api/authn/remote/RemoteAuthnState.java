/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;

/**
 * Base class for storing some context information related to external login.
 * @author K. Benedyczak
 */
public class RemoteAuthnState
{
	public static final String CURRENT_REMOTE_AUTHN_OPTION_SESSION_ATTRIBUTE = RemoteAuthnState.class.getName() 
			+ "_authenticatorOptionId";
	
	private final String relayState;
	private final Date creationTime;
	private final AuthenticationStepContext authenticationContext;
	private SandboxAuthnResultCallback sandboxCallback;

	private final Function<RemoteAuthnState, AuthenticationResult> responseHandler;
	
	public RemoteAuthnState(AuthenticationStepContext authenticationContext, 
			Function<RemoteAuthnState, AuthenticationResult> responseHandler)
	{
		this.authenticationContext = authenticationContext;
		this.responseHandler = responseHandler;
		this.relayState = UUID.randomUUID().toString();
		this.creationTime = new Date();
	}

	public String getRelayState()
	{
		return relayState;
	}

	public Date getCreationTime()
	{
		return creationTime;
	}

	public AuthenticationStepContext getAuthenticationStepContext() 
	{
		return authenticationContext;
	}

	public SandboxAuthnResultCallback getSandboxCallback()
	{
		return sandboxCallback;
	}

	public void setSandboxCallback(SandboxAuthnResultCallback sandboxCallback)
	{
		this.sandboxCallback = sandboxCallback;
	}
	
	public AuthenticationResult processAnswer()
	{
		return responseHandler.apply(this);
	}
}