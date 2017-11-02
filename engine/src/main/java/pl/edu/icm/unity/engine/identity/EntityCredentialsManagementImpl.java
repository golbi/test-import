/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.credential.CredentialAttributeTypeProvider;
import pl.edu.icm.unity.engine.credential.CredentialRequirementsHolder;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalPreviousCredentialException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Implementation of credential and credential requirement operations on entities.
 * 
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
public class EntityCredentialsManagementImpl implements EntityCredentialManagement
{
	private EntityResolver idResolver;
	private AttributeDAO attributeDAO;
	private AuthorizationManager authz;
	private AttributesHelper attributesHelper;
	private EntityCredentialsHelper credHelper;
	
	@Autowired
	public EntityCredentialsManagementImpl(EntityResolver idResolver, AttributeDAO attributeDAO,
			AuthorizationManager authz, AttributesHelper attributesHelper,
			EntityCredentialsHelper credHelper)
	{
		this.idResolver = idResolver;
		this.attributeDAO = attributeDAO;
		this.authz = authz;
		this.attributesHelper = attributesHelper;
		this.credHelper = credHelper;
	}

	@Override
	@Transactional
	public void setEntityCredentialRequirements(EntityParam entity, String requirementId) throws EngineException
	{
		entity.validateInitialization();
		long entityId = idResolver.getEntityId(entity);
		credHelper.setEntityCredentialRequirements(entityId, requirementId);
	}

	@Override
	@Transactional
	public boolean isCurrentCredentialRequiredForChange(EntityParam entity, String credentialId)
			throws EngineException
	{
		long entityId = idResolver.getEntityId(entity);
		return authorizeCredentialChange(entityId, credentialId);
	}
	
	@Transactional
	@Override
	public void setEntityCredential(EntityParam entity, String credentialId, String rawCredential) 
			throws EngineException
	{
		setEntityCredential(entity, credentialId, rawCredential, null);
	}
	
	@Override
	@Transactional
	public void setEntityCredential(EntityParam entity, String credentialId, String rawCredential,
			String currentRawCredential) throws EngineException
	{
		if (rawCredential == null)
			throw new IllegalCredentialException("The credential can not be null");
		entity.validateInitialization();
		long entityId = idResolver.getEntityId(entity);
		boolean requireCurrent = authorizeCredentialChange(entityId, credentialId);

		if (requireCurrent && currentRawCredential == null)
		{
			throw new IllegalPreviousCredentialException(
					"The current credential must be provided");
		}

		//we don't check it 
		if (!requireCurrent)
			currentRawCredential = null;

		setEntityCredentialInternal(entityId, credentialId, rawCredential, currentRawCredential);
	}

	/**
	 * Performs authorization of attribute change. The method also returns whether a current credential is 
	 * required to change the previous one, what is needed if the current credential is set and the caller 
	 * doesn't have the credentialModify capability set globally.
	 * @param entityId
	 * @param credentialId
	 * @return
	 * @throws EngineException 
	 */
	private boolean authorizeCredentialChange(long entityId, String credentialId) 
			throws EngineException
	{
		try
		{
			authz.checkAuthorization(AuthzCapability.credentialModify);
			return false;
		} catch (AuthorizationException e)
		{
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.credentialModify);
		}
		
		//possible OPTIMIZATION: can get the status of selected credential only 
		CredentialInfo credsInfo = credHelper.getCredentialInfo(entityId);
		CredentialPublicInformation credInfo = credsInfo.getCredentialsState().get(credentialId);
		if (credInfo == null)
			throw new IllegalCredentialException("The credential " + credentialId + 
					" is not allowed for the entity");
		
		if (credInfo.getState() == LocalCredentialState.notSet)
			return false;
		return true;
	}
	
	@Override
	@Transactional
	public void setEntityCredentialStatus(EntityParam entity, String credentialId,
			LocalCredentialState desiredCredentialState) throws EngineException
	{
		entity.validateInitialization();
		if (desiredCredentialState == LocalCredentialState.correct)
			throw new WrongArgumentException("Credential can not be put into the correct state "
					+ "with this method. Use setEntityCredential.");
		long entityId = idResolver.getEntityId(entity);
		authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
		Map<String, AttributeExt> attributes = attributesHelper.getAllAttributesAsMapOneGroup(entityId, "/");

		Attribute credReqA = attributes.get(CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS);
		String credentialRequirements = (String)credReqA.getValues().get(0);
		CredentialRequirementsHolder credReqs = credHelper.getCredentialRequirements(credentialRequirements);
		LocalCredentialVerificator handler = credReqs.getCredentialHandler(credentialId);
		if (handler == null)
			throw new IllegalCredentialException("The credential id is not among the entity's "
					+ "credential requirements: " + credentialId);

		String credentialAttributeName = CredentialAttributeTypeProvider.CREDENTIAL_PREFIX+credentialId;
		Attribute currentCredentialA = attributes.get(credentialAttributeName);
		String currentCredential = currentCredentialA != null ? 
				(String)currentCredentialA.getValues().get(0) : null;

		if (currentCredential == null)
		{ 
			if (desiredCredentialState != LocalCredentialState.notSet)
				throw new IllegalCredentialException("The credential is not set, "
						+ "so it's state can be only notSet");
			return;
		}

		//remove or invalidate
		if (desiredCredentialState == LocalCredentialState.notSet)
		{
			attributeDAO.deleteAttribute(credentialAttributeName, entityId, "/");
			attributes.remove(credentialAttributeName);
		} else if (desiredCredentialState == LocalCredentialState.outdated)
		{
			if (!handler.isSupportingInvalidation())
				throw new IllegalCredentialException("The credential doesn't "
						+ "support the outdated state");
			String updated = handler.invalidate(currentCredential);
			Attribute newCredentialA = StringAttribute.of(credentialAttributeName, "/", updated);
			Date now = new Date();
			AttributeExt added = new AttributeExt(newCredentialA, true, now, now);
			attributes.put(credentialAttributeName, added);
			StoredAttribute updatedA = new StoredAttribute(added, entityId);
			attributeDAO.updateAttribute(updatedA);
		}
	}

	
	/**
	 * Sets entity's credential. This is internal method which doesn't perform any authorization nor
	 * argument initialization checking.
	 * @param entityId
	 * @param credentialId
	 * @param rawCredential
	 * @param sqlMap
	 * @throws EngineException
	 */
	private void setEntityCredentialInternal(long entityId, String credentialId, String rawCredential, 
			String currentRawCredential) throws EngineException
	{
		credHelper.setEntityCredentialInternal(entityId, credentialId, rawCredential, 
				currentRawCredential, true);
	}
}
