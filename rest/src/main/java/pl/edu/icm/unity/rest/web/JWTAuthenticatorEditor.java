/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.web;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.Set;

import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.rest.jwt.JWTAuthenticationProperties;
import pl.edu.icm.unity.rest.jwt.authn.JWTVerificator;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.BaseAuthenticatorEditor;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * 
 * @author P.Piernik
 *
 */
public class JWTAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private Binder<JWTConfiguration> configBinder;
	private Set<String> credentials;

	public JWTAuthenticatorEditor(UnityMessageSource msg, Set<String> credentials)
	{
		super(msg);
		this.credentials = credentials;
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher switcher, boolean forceNameEditable)
	{
		boolean editMode = toEdit != null;
		setName(editMode ? toEdit.id : msg.getMessage("JWTAuthenticatorEditor.defaultName"));
		setNameReadOnly(editMode && !forceNameEditable);

		ComboBox<String> credential = new ComboBox<>();
		credential.setCaption(msg.getMessage("JWTAuthenticatorEditor.signingCredential"));
		credential.setEmptySelectionAllowed(false);
		credential.setItems(credentials);

		TextField ttl = new TextField();
		ttl.setCaption(msg.getMessage("JWTAuthenticatorEditor.tokenTTL"));

		configBinder = new Binder<>(JWTConfiguration.class);
		configBinder.forField(credential).asRequired(msg.getMessage("fieldRequired")).bind("credential");
		configBinder.forField(ttl)
				.withConverter(new StringToIntegerConverter(
						msg.getMessage("JWTAuthenticatorEditor.tokenTTL.notANumber")))

				.asRequired(msg.getMessage("fieldRequired")).bind("ttl");

		JWTConfiguration config = new JWTConfiguration();
		if (!credentials.isEmpty())
		{
			config.setCredential(credentials.iterator().next());
		}
		config.setTtl(3600);
		
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, msg);
		}

		configBinder.setBean(config);

		FormLayoutWithFixedCaptionWidth main = new FormLayoutWithFixedCaptionWidth();
		main.setMargin(true);
		main.addComponents(name);
		main.addComponent(credential);
		main.addComponent(ttl);

		return main;
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefiniton() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), JWTVerificator.NAME, getConfiguration(), null);
	}

	private String getConfiguration() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
			throw new FormValidationException();

		return configBinder.getBean().toProperties();
	}

	public static class JWTConfiguration
	{
		private String credential;
		private int ttl;

		public JWTConfiguration()
		{
		}

		public String getCredential()
		{
			return credential;
		}

		public void setCredential(String credential)
		{
			this.credential = credential;
		}

		public int getTtl()
		{
			return ttl;
		}

		public void setTtl(int ttl)
		{
			this.ttl = ttl;
		}

		public String toProperties()
		{
			Properties raw = new Properties();

			raw.put(JWTAuthenticationProperties.PREFIX + JWTAuthenticationProperties.SIGNING_CREDENTIAL,
					credential);
			raw.put(JWTAuthenticationProperties.PREFIX + JWTAuthenticationProperties.TOKEN_TTL,
					String.valueOf(ttl));

			JWTAuthenticationProperties prop = new JWTAuthenticationProperties(raw);
			return prop.getAsString();

		}

		public void fromProperties(String properties, UnityMessageSource msg)
		{
			Properties raw = new Properties();
			try
			{
				raw.load(new StringReader(properties));
			} catch (IOException e)
			{
				throw new InternalException("Invalid configuration of the jwt verificator", e);
			}

			JWTAuthenticationProperties jwtProp = new JWTAuthenticationProperties(raw);

			credential = jwtProp.getValue(JWTAuthenticationProperties.SIGNING_CREDENTIAL);
			ttl = jwtProp.getIntValue(JWTAuthenticationProperties.TOKEN_TTL);

		}
	}
}
