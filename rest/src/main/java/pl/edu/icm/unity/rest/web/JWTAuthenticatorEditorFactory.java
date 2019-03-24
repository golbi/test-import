/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.rest.jwt.authn.JWTVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;

/**
 * 
 * @author P.Piernik
 *
 */
@Component
public class JWTAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private UnityMessageSource msg;
	private PKIManagement pkiMan;

	@Autowired
	JWTAuthenticatorEditorFactory(UnityMessageSource msg, PKIManagement pkiMan)
	{
		this.msg = msg;
		this.pkiMan = pkiMan;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return JWTVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new JWTAuthenticatorEditor(msg, pkiMan.getCredentialNames());
	}
}
