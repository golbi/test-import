/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import java.security.cert.X509Certificate;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalVerificator;
import pl.edu.icm.unity.engine.api.authn.local.LocalSandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;

/**
 * Trivial verificator of certificates. It is assumed that the certificate was previously authenticated.
 * Therefore the only operation is resolving of the certificate user.
 * <p>
 * There is no local credential associated with this verificator. Therefore it always returns 
 * the correct credential state. Storing credential of this type makes no sense, but works (empty string is stored).
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class CertificateVerificator extends AbstractLocalVerificator implements CertificateExchange
{ 	
	private static final Logger log = Log.getLogger(Log.U_SERVER, CertificateVerificator.class);
	private static final String[] IDENTITY_TYPES = {X500Identity.ID};
	public static final String NAME = "certificate";
	public static final String DESC = "Verifies certificates";

	
	public CertificateVerificator()
	{
		super(NAME, DESC, CertificateExchange.ID, false);
	}

	@Override
	public String getSerializedConfiguration()
	{
		return "";
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
	}

	@Override
	public String prepareCredential(String rawCredential, String previousCredential, 
			String currentCredential, boolean verify)
			throws IllegalCredentialException
	{
		return "";
	}

	@Override
	public CredentialPublicInformation checkCredentialState(String currentCredential)
	{
		return new CredentialPublicInformation(LocalCredentialState.correct, "");
	}

	@Override
	public AuthenticationResult checkCertificate(X509Certificate[] chain, 
			SandboxAuthnResultCallback sandboxCallback)
	{
		String identity = chain[0].getSubjectX500Principal().getName();
		try
		{
			EntityWithCredential resolved = identityResolver.resolveIdentity(identity, 
				IDENTITY_TYPES, credentialName);
			AuthenticatedEntity entity = new AuthenticatedEntity(resolved.getEntityId(), 
					X500NameUtils.getReadableForm(identity), false);
			AuthenticationResult ret = new AuthenticationResult(Status.success, entity);
			if (sandboxCallback != null)
				sandboxCallback.sandboxedAuthenticationDone(new LocalSandboxAuthnContext(ret));
			return ret;
		} catch (Exception e)
		{
			log.debug("Checking certificate failed", e);
			AuthenticationResult ret = new AuthenticationResult(Status.deny, null);
			if (sandboxCallback != null)
				sandboxCallback.sandboxedAuthenticationDone(
						new LocalSandboxAuthnContext(ret));
			return ret;
		}
	}

	@Override
	public String invalidate(String currentCredential)
	{
		throw new IllegalStateException("This credential doesn't support invalidation");
	}

	@Override
	public String prepareCredential(String rawCredential, String currentCredential, boolean verify)
			throws IllegalCredentialException, InternalException
	{
		return prepareCredential(rawCredential, null, currentCredential, verify);
	}
	
	@Component
	public static class Factory extends AbstractLocalCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<CertificateVerificator> factory)
		{
			super(NAME, DESC, false, factory);
		}
	}
}





