/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.service;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import io.imunity.fido.credential.FidoCredentialInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * Implements CredentialRepository to provide users data required by yubico library.
 *
 * @author R. Ledzinski
 */
class UnityFidoRegistrationStorage implements CredentialRepository
{
	private static final org.apache.logging.log4j.Logger log = Log.getLogger(Log.U_SERVER_FIDO, UnityFidoRegistrationStorage.class);

	private final FidoEntityHelper entityHelper;
	private final IdentityResolver identityResolver;
	private final String credentialName;

	public UnityFidoRegistrationStorage(final FidoEntityHelper entityHelper, final IdentityResolver identityResolver, final String credentialName)
	{
		this.entityHelper = entityHelper;
		this.identityResolver = identityResolver;
		this.credentialName = credentialName;
	}

	@Override
	public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(final String username)
	{
		log.debug("Enter getCredentialIdsForUsername({})", username);
		return getFidoCredentialInfoForUsername(username).stream()
				.map(i -> PublicKeyCredentialDescriptor.builder()
						.id(i.getCredentialId())
						.build())
				.collect(Collectors.toSet());
	}

	@Override
	public Optional<String> getUsernameForUserHandle(final ByteArray userHandle)
	{
		FidoUserHandle uh = new FidoUserHandle(userHandle.getBytes());
		log.debug("getUsernameForUserHandle({})", uh.asString());
		return entityHelper.getUsernameForUserHandle(uh.asString());
	}

	@Override
	public Optional<ByteArray> getUserHandleForUsername(final String username)
	{
		log.debug("getUserHandleForUsername({})", username);
		return entityHelper.getUserHandleForUsername(username).map(uh -> new ByteArray(FidoUserHandle.fromString(uh).getBytes()));
	}

	List<FidoCredentialInfo> getFidoCredentialInfoForUserHandle(final String userHandle)
	{
		log.debug("getFidoCredentialInfoForUserHandle({})", userHandle);
		Optional<String> username = entityHelper.getUsernameForUserHandle(userHandle);
		if (!username.isPresent())
			return Collections.emptyList();
		return getFidoCredentialInfoForEntity(entityHelper.resolveUsername(null, username.get()).orElseThrow(() -> new NoEntityException("No entity - should not happen!")));
	}

	List<FidoCredentialInfo> getFidoCredentialInfoForUsername(final String username)
	{
		log.debug("getFidoCredentialInfoForUsername({})", username);
		Identities ids;
		try
		{
			ids = entityHelper.resolveUsername(null, username).orElseThrow(() -> new NoEntityException("No entity - should not happen!"));
		} catch (FidoException e) {
			return Collections.emptyList();
		}
		return getFidoCredentialInfoForEntity(ids);
	}

	private List<FidoCredentialInfo> getFidoCredentialInfoForEntity(final Identities identities)
	{
		if (isNull(identities))
			return Collections.emptyList();

		EntityParam entityParam = identities.getEntityParam();
		EntityWithCredential entity;
		try
		{
			entity = identityResolver.resolveIdentity(entityParam.getIdentity().getValue(),
					new String[]{entityParam.getIdentity().getTypeId()},
					credentialName);
		} catch (EngineException e)
		{
			log.error("Failed to resolve identity", e);
			return Collections.emptyList();
		}

		return FidoCredentialInfo.deserializeList(entity.getCredentialValue());
	}

	@Override
	public Optional<RegisteredCredential> lookup(final ByteArray credentialId, final ByteArray userHandle)
	{
		log.debug("Enter lookup()");
		return getFidoCredentialInfoForUserHandle(new FidoUserHandle(userHandle.getBytes()).asString()).stream()
				.filter(info -> info.getCredentialId().equals(credentialId))
				.map(info -> info.getCredentialWithHandle(userHandle))
				.findFirst();
	}

	@Override
	public Set<RegisteredCredential> lookupAll(final ByteArray credentialId)
	{
		log.debug("Enter lookupAll()");
		// FIXME used to make sure no other credential with given ID exists
		return Collections.emptySet();
	}

	/**
	 * Factory and cache that creates Fido registration storage used mainly by Yubico library.
	 */
	@Component
	public static class UnityFidoRegistrationStorageCache
	{
		private Map<String, UnityFidoRegistrationStorage> cache = new ConcurrentHashMap<>();
		private FidoEntityHelper entityHelper;
		private IdentityResolver identityResolver;

		@Autowired
		public UnityFidoRegistrationStorageCache(final FidoEntityHelper entityHelper, final IdentityResolver identityResolver)
		{
			this.entityHelper = entityHelper;
			this.identityResolver = identityResolver;
		}

		UnityFidoRegistrationStorage getInstance(final String credentialName)
		{
			return cache.computeIfAbsent(credentialName, (name) -> new UnityFidoRegistrationStorage(entityHelper, identityResolver, credentialName));
		}
	}

}
