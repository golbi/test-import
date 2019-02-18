/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.pki.Certificate;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;

/**
 * For tests - returns only a single MAIN credential
 * @author K. Benedyczak
 */
public class SimplePKIManagement implements PKIManagement
{
	@Override
	public Set<String> getValidatorNames() throws EngineException
	{
		return new HashSet<>();
	}
	
	@Override
	public X509CertChainValidatorExt getValidator(String name) throws EngineException
	{
		throw new WrongArgumentException("No such validator " + name);
	}
	
	@Override
	public Set<String> getCredentialNames() throws EngineException
	{
		return Collections.singleton("MAIN");
	}
	@Override
	public X509Credential getCredential(String name) throws EngineException
	{
		if (name.equals("MAIN"))
			try
			{
				return DBIntegrationTestBase.getDemoCredential();
			} catch (Exception e)
			{
				throw new InternalException("error loading credential", e);
			}
		throw new WrongArgumentException("No such validator " + name);				
	}

	@Override
	public IAuthnAndTrustConfiguration getMainAuthnAndTrust()
	{
		return null;
	}

	@Override
	public Set<String> getAllCertificateNames() throws EngineException
	{
		return null;
	}

	@Override
	public Certificate getCertificate(String name) throws EngineException
	{
		return null;
	}

	@Override
	public void updateVolatileCertificate(String name, X509Certificate updated)
			throws EngineException
	{
	}

	@Override
	public void removeVolatileCertificate(String name) throws EngineException
	{
	}

	@Override
	public void addVolatileCertificate(String name, X509Certificate updated)
			throws EngineException
	{
	}

	@Override
	public Certificate getVolatileCertificate(String name)
	{
		return null;
	}

	@Override
	public void addPersistedCertificate(Certificate toAdd) throws EngineException
	{
		
	}

	@Override
	public Certificate getPersistedCertificate(String name) throws EngineException
	{
		return null;
	}

	@Override
	public List<Certificate> getPersistedCertificates() throws EngineException
	{
		return null;
	}

	@Override
	public void removePersistedCertificate(String toRemove) throws EngineException
	{
		
	}

	@Override
	public void updatePersistedCertificate(Certificate toUpdate) throws EngineException
	{
		
	}

	@Override
	public void loadCertificatesFromConfigFile()
	{
		
	}

	@Override
	public List<Certificate> getVolatileCertificates() throws EngineException
	{
		return null;
	}
}
