/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.pass;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.webui.common.ComponentWithLabel;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;

/**
 * Contains masked text fields along with password strength and fulfillment of
 * credential settings.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class PasswordEditorComponent extends CustomComponent implements ComponentWithLabel
{

	private PasswordFieldsComponent fieldsComponent;
	private PasswordQualityComponent qualityComponent;
	
	public PasswordEditorComponent(UnityMessageSource msg, CredentialEditorContext context, PasswordCredential config)
	{
		super();
		qualityComponent = new PasswordQualityComponent(msg, config, context);
		fieldsComponent = new PasswordFieldsComponent(msg, context, config, qualityComponent::onNewPassword);
		
		HorizontalLayout root = new HorizontalLayout();
		root.setMargin(false);
		root.setSpacing(true);
		root.addComponents(fieldsComponent, qualityComponent);
		setCompositionRoot(root);
	}

	@Override
	public void setLabel(String label)
	{
		fieldsComponent.setLabel(label);
	}

	public String getValue() throws IllegalCredentialException
	{
		return fieldsComponent.getValue();
	}

	public void setCredentialError(EngineException error)
	{
		fieldsComponent.setCredentialError(error);
	}

	public void disablePasswordRepeat()
	{
		fieldsComponent.disablePasswordRepeat();
	}
}