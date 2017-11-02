/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.CredentialRequirementDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.authn.CredentialRequirements;

/**
 * Allows read all credential requirements from DB and one special credential
 * requirements which contains all credentials
 * 
 * @author P.Piernik
 *
 */
@Component
public class CredentialReqRepository
{

	private CredentialRepository credentialRepo;
	private CredentialRequirementDB credentialRequirementDB;
	private UnityMessageSource msg;

	@Autowired
	public CredentialReqRepository(CredentialRepository credentialRepo,
			CredentialRequirementDB credentialRequirementDB, UnityMessageSource msg)
	{

		this.credentialRepo = credentialRepo;
		this.credentialRequirementDB = credentialRequirementDB;
		this.msg = msg;
	}

	@Transactional
	public Collection<CredentialRequirements> getCredentialRequirements() throws EngineException
	{

		List<CredentialRequirements> res = credentialRequirementDB.getAll();
		res.add(new SystemCredentialRequirements(credentialRepo, msg));
		return res;
	}

	@Transactional
	public CredentialRequirements get(String name)
	{
		if (SystemCredentialRequirements.NAME.equals(name))
			return new SystemCredentialRequirements(credentialRepo, msg);
		return credentialRequirementDB.get(name);
	}

	public void assertExist(String name) throws EngineException
	{
		if (!getAllNames().contains(name))
			throw new IllegalArgumentException(
					"There is no required credential set with id " + name);
	}

	private Set<String> getAllNames() throws EngineException
	{
		return getCredentialRequirements().stream().map(cr -> cr.getName())
				.collect(Collectors.toSet());
	}

}
