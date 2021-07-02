/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision.ErrorDetail;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision.SecondFactorDetail;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor.PostAuthenticationStepDecision.UnknownRemoteUserDetail;
import pl.edu.icm.unity.engine.api.authn.LoginSession.RememberMeInfo;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.session.SessionParticipant;
import pl.edu.icm.unity.engine.api.session.SessionParticipantTypesRegistry;
import pl.edu.icm.unity.engine.api.session.SessionParticipants;
import pl.edu.icm.unity.engine.api.utils.CookieHelper;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.EntityParam;

@Component
class InteractiveAuthneticationProcessorImpl implements InteractiveAuthenticationProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, InteractiveAuthneticationProcessorImpl.class);
	private final AuthenticationProcessor basicAuthnProcessor;
	private final EntityManagement entityMan;
	private final SessionManagement sessionMan;
	private final SessionParticipantTypesRegistry participantTypesRegistry;
	private final LoginToHttpSessionBinder sessionBinder;
	private final RememberMeProcessorImpl rememberMeProcessor;
	
	InteractiveAuthneticationProcessorImpl(AuthenticationProcessor basicAuthnProcessor, 
			EntityManagement entityMan,
			SessionManagement sessionMan,
			SessionParticipantTypesRegistry participantTypesRegistry,
			LoginToHttpSessionBinder sessionBinder,
			RememberMeProcessorImpl rememberMeProcessor)
	{
		this.basicAuthnProcessor = basicAuthnProcessor;
		this.entityMan = entityMan;
		this.sessionMan = sessionMan;
		this.participantTypesRegistry = participantTypesRegistry;
		this.sessionBinder = sessionBinder;
		this.rememberMeProcessor = rememberMeProcessor;
	}



	@Override
	public PostAuthenticationStepDecision processFirstFactorResult(AuthenticationResult result,
			AuthenticationStepContext stepContext, LoginMachineDetails machineDetails, boolean setRememberMe,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse)
	{
		PartialAuthnState authnState;
		try
		{
			authnState = basicAuthnProcessor.processPrimaryAuthnResult(result,
					stepContext.selectedAuthnFlow, stepContext.authnOptionId);
		} catch (AuthenticationException e)
		{
			return interpretAuthnException(e, httpRequest, machineDetails.getIp());
		}
		
		LoginSession loginSession = null;
		if (authnState.isSecondaryAuthenticationRequired())
		{
			Optional<LoginSession> loginSessionFromRememberMe = rememberMeProcessor
					.processRememberedSecondFactor(
							httpRequest,
							httpResponse,
							result.getSuccessResult().authenticatedEntity.getEntityId(),
							machineDetails.getIp(), stepContext.realm, getLoginCounter(httpRequest));
			if (!loginSessionFromRememberMe.isPresent())
			{
				return PostAuthenticationStepDecision.goToSecondFactor(new SecondFactorDetail(authnState));
			} else
			{
				loginSession = loginSessionFromRememberMe.get();
				log.debug("Second factor authn is remembered by entity "
						+ loginSession.getEntityId() + ", skipping it");
			}
		} else
		{
			loginSession = getLoginSessionForEntity(
					authnState.getPrimaryResult().getSuccessResult().authenticatedEntity,
					stepContext.realm, authnState.getFirstFactorOptionId(),
					null);
		}
		
		if (loginSession == null)
		{	
			throw new IllegalStateException("BUG: code tried to finalize authentication "
					+ "without login session");	
		}
		
		AuthenticatedEntity authnEntity = basicAuthnProcessor.finalizeAfterPrimaryAuthentication(
				authnState, loginSession.getRememberMeInfo().secondFactorSkipped);

		logged(authnEntity, loginSession, stepContext.realm, machineDetails, setRememberMe,
				AuthenticationProcessor.extractParticipants(result), httpRequest, httpResponse);

		return PostAuthenticationStepDecision.completed(); //TODO KB shall we distinguish users with outdated credential?
	}

	@Override
	public PostAuthenticationStepDecision processSecondFactorResult(PartialAuthnState state,
			AuthenticationResult secondFactorResult, AuthenticationStepContext stepContext,
			LoginMachineDetails machineDetails, boolean setRememberMe, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse)
	{
		AuthenticatedEntity logInfo;
		try
		{
			logInfo = basicAuthnProcessor.finalizeAfterSecondaryAuthentication(state, secondFactorResult);
		} catch (AuthenticationException e)
		{
			return interpretAuthnException(e, httpRequest, machineDetails.getIp());
		}

		LoginSession loginSession = getLoginSessionForEntity(logInfo, stepContext.realm,
				state.getFirstFactorOptionId(), stepContext.authnOptionId);

		logged(logInfo, loginSession, stepContext.realm, machineDetails, setRememberMe,
				AuthenticationProcessor.extractParticipants(state.getPrimaryResult()), 
				httpRequest, httpResponse);

		return PostAuthenticationStepDecision.completed(); //TODO KB shall we distinguish users with outdated credential?
	}
	
	private PostAuthenticationStepDecision interpretAuthnException(AuthenticationException e, HttpServletRequest httpRequest,
			String ip)
	{
		UnsuccessfulAuthenticationCounter counter = getLoginCounter(httpRequest);
		if (e instanceof UnknownRemoteUserException)
		{
			return PostAuthenticationStepDecision.unknownRemoteUser(
					new UnknownRemoteUserDetail(e.getResult().asRemote().getUnknownRemotePrincipalResult()));
		} else
		{
			counter.unsuccessfulAttempt(ip);
			return PostAuthenticationStepDecision.error(new ErrorDetail(new ResolvableError(e.getMessage())));
		}
	}
	
	private LoginSession getLoginSessionForEntity(AuthenticatedEntity authenticatedEntity,
			AuthenticationRealm realm, 
			AuthenticationOptionKey firstFactorAuhtnOptionId,
			AuthenticationOptionKey secondFactorAuhtnOptionId)
	{

		long entityId = authenticatedEntity.getEntityId();
		String label = getLabel(entityId);
		return sessionMan.getCreateSession(entityId, realm, label,
				authenticatedEntity.getOutdatedCredentialId(), 
				new RememberMeInfo(false, false), firstFactorAuhtnOptionId,
				secondFactorAuhtnOptionId);
	}
	
	private void logged(AuthenticatedEntity authenticatedEntity, LoginSession ls, 
			final AuthenticationRealm realm, LoginMachineDetails machineDetails, final boolean rememberMe,
			List<SessionParticipant> participants,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse)
	{	
		sessionMan.updateSessionAttributes(ls.getId(), 
				new SessionParticipants.AddParticipantToSessionTask(
						participantTypesRegistry,
						participants.toArray(new SessionParticipant[participants.size()])));

		
		//prevent session fixation
		//FIXME KB
//		VaadinSession vss = VaadinSession.getCurrent();
//		if (vss == null)
//		{
//			log.error("BUG: Can't get VaadinSession to store authenticated user's data.");
//			throw new AuthenticationException("AuthenticationProcessor.authnInternalError");
//		}
		//VaadinService.reinitializeSession(VaadinService.getCurrentRequest());
		
		final HttpSession httpSession = httpRequest.getSession();
	
		sessionBinder.bindHttpSession(httpSession, ls);

		if (rememberMe)
		{
			rememberMeProcessor.addRememberMeCookieAndUnityToken(httpResponse, realm, machineDetails, ls.getEntityId(),
					ls.getStarted(), ls.getLogin1stFactorOptionId(),
					ls.getLogin2ndFactorOptionId());
		}

		addSessionCookie(getSessionCookieName(realm.getName()), ls.getId(), httpResponse);
		
		ls.addAuthenticatedIdentities(authenticatedEntity.getAuthenticatedWith());
		ls.setRemoteIdP(authenticatedEntity.getRemoteIdP());
		if (ls.isUsedOutdatedCredential())
			log.info("User {} logged with outdated credential", ls.getEntityId());
		
		log.info("Logged with session: {}, first factor authn option: {}, second factor authn option: {}"
				+ ", first factor skipped: {}, second factor skipped: {}",
				ls.toString(), ls.getLogin1stFactorOptionId(), ls.getLogin2ndFactorOptionId(),
				ls.getRememberMeInfo().firstFactorSkipped, ls.getRememberMeInfo().secondFactorSkipped);
	}
	
	
	private static UnsuccessfulAuthenticationCounter getLoginCounter(HttpServletRequest httpRequest)
	{
		HttpSession httpSession = httpRequest.getSession();
		return (UnsuccessfulAuthenticationCounter) httpSession.getServletContext().getAttribute(
				UnsuccessfulAuthenticationCounter.class.getName());
		//TODO KB - most likely this should not be taken from session.
	}
	
	private String getLabel(long entityId)
	{
		try
		{
			return entityMan.getEntityLabel(new EntityParam(entityId));
		} catch (AuthorizationException e)
		{
			log.debug("Not setting entity's label as the client is not authorized to read the attribute",
					e);
		} catch (EngineException e)
		{
			log.error("Can not get the attribute designated with EntityName", e);
		}
		return null;
	}
	
	private static String getSessionCookieName(String realmName)
	{
		return UNITY_SESSION_COOKIE_PFX+realmName;
	}
	
	private void addSessionCookie(String cookieName, String sessionId,
			HttpServletResponse servletResponse)
	{
		servletResponse.addCookie(CookieHelper.setupHttpCookie(cookieName, sessionId, -1));
	}

}