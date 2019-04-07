/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.ldap.client.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.utils.tprofile.EditInputTranslationProfileSubViewHelper;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.ldap.client.LdapCertVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;

/**
 * Factory for {@link LdapAuthenticatorEditor}
 * 
 * @author P.Piernik
 *
 */
@Component
public class LdapCertAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{

	private UnityMessageSource msg;
	private PKIManagement pkiMan;
	private EditInputTranslationProfileSubViewHelper profileHelper;

	@Autowired
	public LdapCertAuthenticatorEditorFactory(UnityMessageSource msg, PKIManagement pkiMan,
			EditInputTranslationProfileSubViewHelper profileHelper)
	{
		this.msg = msg;
		this.pkiMan = pkiMan;
		this.profileHelper = profileHelper;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return LdapCertVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new LdapAuthenticatorEditor(msg, pkiMan, profileHelper, LdapCertVerificator.NAME);
	}

}
