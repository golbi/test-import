/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import java.util.Optional;

import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Processes {@link RemotelyAuthenticatedInput} by applying a translation profile to it and 
 * returns {@link RemotelyAuthenticatedPrincipal} or {@link AuthenticationResult} depending whether 
 * caller wants to have a possibility to postprocess the translation profile output or not.
 * 
 * @author K. Benedyczak
 */
public interface RemoteAuthnResultProcessor
{
	/**
	 * This method is calling {@link #processRemoteInput(RemotelyAuthenticatedInput)} and then
	 * {@link #assembleAuthenticationResult(RemotelyAuthenticatedPrincipal)}.
	 * Usually it is the only one that is used, when {@link RemotelyAuthenticatedInput} 
	 * is obtained in an implementation specific way.
	 * 
	 * @param input
	 * @return
	 * @throws EngineException 
	 */
	AuthenticationResult getResult(RemotelyAuthenticatedInput input, String profile, 
			boolean dryRun, Optional<IdentityTaV> identity) 
			throws AuthenticationException;
	
	/**
	 * Equivalent
	 * to{@link #getResult(RemotelyAuthenticatedInput, String, boolean, Optional)}
	 * but translation profile is given directly
	 * 
	 * @param input
	 * @return
	 * @throws EngineException
	 */
	AuthenticationResult getResult(RemotelyAuthenticatedInput input, TranslationProfile profile, boolean dryRun,
			Optional<IdentityTaV> identity) throws AuthenticationException;
	
	/**
	 * Tries to resolve the primary identity from the previously created {@link RemotelyAuthenticatedPrincipal}
	 * (usually via {@link #processRemoteInput(RemotelyAuthenticatedInput)}) and returns a 
	 * final {@link AuthenticationResult} depending on the success of this action.
	 * 
	 * @param remoteContext
	 * @return
	 * @throws EngineException 
	 */
	AuthenticationResult assembleAuthenticationResult(RemotelyAuthenticatedPrincipal remoteContext) 
			throws AuthenticationException;
	
	/**
	 * Invokes the configured translation profile on the remotely obtained authentication input. Then assembles  
	 * the {@link RemotelyAuthenticatedPrincipal} from the processed input containing the information about what 
	 * from the remote data is or can be meaningful in the local DB.
	 * 
	 * @param input
	 * @param identity if not empty then fixes the identity for which the profile is executed.
	 * @return
	 * @throws EngineException
	 */
	RemotelyAuthenticatedPrincipal processRemoteInput(RemotelyAuthenticatedInput input, 
			TranslationProfile profile, boolean dryRun, Optional<IdentityTaV> identity) throws EngineException;
}
