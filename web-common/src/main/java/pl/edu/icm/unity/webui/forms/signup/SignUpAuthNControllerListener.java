/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.signup;

import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;

/**
 * Notifications that may happen when during sign up process when authentication
 * used.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public interface SignUpAuthNControllerListener
{
	public void onUnknownUser(AuthenticationResult result);

	public void onUserExists(AuthenticationResult result);

	public void onAuthnError(AuthenticationException e, String error);

	public void onAuthnCancelled();

	public void onAuthnStarted(boolean showProgress);
}