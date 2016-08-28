/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialsChangeDialog;
import pl.edu.icm.unity.webui.common.credentials.CredentialsChangeDialog.Callback;

/**
 * Simple dialog wrapping {@link CredentialsChangeDialog}. It is invoked for users logged with outdated
 * credential. User is informed about invalidated credential and can choose to change it or logout. 
 * After changing the credential user can only logout.  
 * @author K. Benedyczak
 */
public class OutdatedCredentialDialog extends AbstractDialog
{
	private CredentialEditorRegistry credEditorReg;
	private WebAuthenticationProcessor authnProcessor;
	private CredentialManagement credMan;
	private EntityCredentialManagement ecredMan;
	private EntityManagement entityMan;
	private CredentialRequirementManagement credReqMan;
	
	public OutdatedCredentialDialog(UnityMessageSource msg, CredentialManagement credMan, 
			EntityCredentialManagement ecredMan, EntityManagement entityMan,
			CredentialRequirementManagement credReqMan, CredentialEditorRegistry credEditorReg,
			WebAuthenticationProcessor authnProcessor)
	{
		super(msg, msg.getMessage("OutdatedCredentialDialog.caption"), 
				msg.getMessage("OutdatedCredentialDialog.accept"), 
				msg.getMessage("OutdatedCredentialDialog.cancel"));
		this.credMan = credMan;
		this.ecredMan = ecredMan;
		this.entityMan = entityMan;
		this.credReqMan = credReqMan;
		this.credEditorReg = credEditorReg;
		this.authnProcessor = authnProcessor;
		setSizeMode(SizeMode.SMALL);
	}

	@Override
	protected Component getContents() throws Exception
	{
		return new Label(msg.getMessage("OutdatedCredentialDialog.info"));
	}

	@Override
	protected void onConfirm()
	{
		WrappedSession vss = VaadinSession.getCurrent().getSession();
		LoginSession ls = (LoginSession) vss.getAttribute(LoginToHttpSessionBinder.USER_SESSION_KEY);
		CredentialsChangeDialog dialog = new CredentialsChangeDialog(msg, 
				ls.getEntityId(), 
				credMan, 
				ecredMan,
				entityMan,
				credReqMan,
				credEditorReg, true,
				new Callback()
				{
					@Override
					public void onClose(boolean changed)
					{
						afterCredentialUpdate(changed);
					}
				});
		dialog.show();
	}

	@Override
	protected void onCancel()
	{
		close();
		authnProcessor.logout(true);
	}
	
	private void afterCredentialUpdate(final boolean changed)
	{
		new AbstractDialog(msg,	msg.getMessage("OutdatedCredentialDialog.finalCaption"), 
				msg.getMessage("ok"))
		{
			{
				setSizeMode(SizeMode.SMALL);	
			}
			
			@Override
			protected void onConfirm()
			{
				OutdatedCredentialDialog.this.onCancel();
				close();
			}
			
			@Override
			protected Component getContents() throws Exception
			{
				String info = changed ? msg.getMessage("OutdatedCredentialDialog.finalInfo") :
					msg.getMessage("OutdatedCredentialDialog.finalInfoNotChanged");
				return new Label(info);
			}
		}.show();
	}
}
