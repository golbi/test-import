/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.uiproviders;

import com.vaadin.server.Resource;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.providers.HomeUITabProvider;

/**
 * Provides OAuthToken UI component to home UI
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class OAuthTokenHomeUIProvider implements HomeUITabProvider
{	
	public static final String ID = "oauthTokens";
	
	private SecuredTokensManagement tokenMan;
	private UnityMessageSource msg;
	private AttributeSupport attrProcessor;
	private AttributesManagement attrMan;
	
	
	
	public OAuthTokenHomeUIProvider(SecuredTokensManagement tokenMan, UnityMessageSource msg,
			AttributeSupport attrProcessor, AttributesManagement attrMan)
	{
		this.tokenMan = tokenMan;
		this.msg = msg;
		this.attrProcessor = attrProcessor;
		this.attrMan = attrMan;
	}



	@Override
	public Component getUI()
	{
		return new UserHomeTokensComponent(tokenMan, msg, attrProcessor, attrMan);
	}



	@Override
	public String getLabelKey()
	{
		return  "OAuthTokenUserHomeUI.tokensLabel";
	}



	@Override
	public String getDescriptionKey()
	{
		return "OAuthTokenUserHomeUI.tokensDesc";
	}



	@Override
	public String getId()
	{
		return ID;
	}



	@Override
	public Resource getIcon()
	{
		return Images.usertoken64.getResource();
	}

}
