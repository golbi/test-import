/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.identity.IdentityHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.api.generic.CredentialRequirementDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;

/**
 * Credential requirement management implementation.
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
@Transactional
public class CredentialReqManagementImpl implements CredentialRequirementManagement
{
	private LocalCredentialsRegistry localCredReg;
	private CredentialDB credentialDB;
	private CredentialRequirementDB credentialRequirementDB;
	private CredentialReqRepository credReqRepository;
	private IdentityHelper identityHelper;
	private AuthorizationManager authz;
	private EntityCredentialsHelper entityCredHelper;
	
	@Autowired
	public CredentialReqManagementImpl(LocalCredentialsRegistry localCredReg,
			CredentialDB credentialDB, CredentialRequirementDB credentialRequirementDB,
			IdentityHelper identityHelper, AuthorizationManager authz,
			EntityCredentialsHelper entityCredHelper, CredentialReqRepository credReqRepository)
	{
		this.localCredReg = localCredReg;
		this.credentialDB = credentialDB;
		this.credentialRequirementDB = credentialRequirementDB;
		this.identityHelper = identityHelper;
		this.authz = authz;
		this.entityCredHelper = entityCredHelper;
		this.credReqRepository = credReqRepository;
	}


	@Override
	public void addCredentialRequirement(CredentialRequirements toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertIsNotSystemProfile(toAdd.getName());
		assertIsNotReadOnly(toAdd);
		Set<String> existingCreds = credentialDB.getAllNames();
		for (String u: toAdd.getRequiredCredentials())
			if (!existingCreds.contains(u))
				throw new IllegalCredentialException("The credential " + u + " is unknown");
		credentialRequirementDB.create(toAdd);
	}

	
	@Override
	public Collection<CredentialRequirements> getCredentialRequirements()
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return credReqRepository.getCredentialRequirements();
	}
	
	@Override
	public void updateCredentialRequirement(CredentialRequirements updated) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertIsNotSystemProfile(updated.getName());
		assertIsNotReadOnly(updated);
		Map<String, CredentialDefinition> credDefs = credentialDB.getAllAsMap();
		CredentialRequirementsHolder.checkCredentials(updated, credDefs, localCredReg);
		credentialRequirementDB.update(updated);
	}

	@Override
	public void removeCredentialRequirement(String toRemove, String replacementId) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertIsNotSystemProfile(toRemove);
		Set<Long> entities = identityHelper.getEntitiesByRootAttribute(
				CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS,
				Collections.singleton(toRemove));
		if (entities.size() > 0 && replacementId == null)
			throw new IllegalCredentialException("There are entities with the removed credential requirements set and a replacement was not specified.");
		if (replacementId != null)
		{
			for (Long entityId: entities)
				entityCredHelper.setEntityCredentialRequirementsNoCheck(entityId, replacementId);
		}

		credentialRequirementDB.delete(toRemove);
	}
	
	private void assertIsNotSystemProfile(String name)
	{
		if (SystemCredentialRequirements.NAME.equals(name))
			throw new IllegalArgumentException("Credential requirement '" + name + "' is the system credential requirement and cannot be overwrite or remove");
	}
	
	private void assertIsNotReadOnly(CredentialRequirements cred) throws EngineException
	{
		if (cred.isReadOnly())
			throw new IllegalArgumentException("Cannot create read only credential requirement through this API");
	}
}
