/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.session;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.LoginSession.RememberMeInfo;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.session.SessionParticipant;
import pl.edu.icm.unity.engine.api.session.SessionParticipantTypesRegistry;
import pl.edu.icm.unity.engine.api.session.SessionParticipants;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;

/**
 * Implementation of {@link SessionManagement}
 * @author K. Benedyczak
 */
@Component
public class SessionManagementImpl implements SessionManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, SessionManagementImpl.class);
	public static final long DB_ACTIVITY_WRITE_DELAY = 3000;
	public static final String SESSION_TOKEN_TYPE = "session";
	private TokensManagement tokensManagement;
	private LoginToHttpSessionBinder sessionBinder;
	private SessionParticipantTypesRegistry participantTypesRegistry;
	private EntityDAO entityDAO;
	private AttributesHelper attributeHelper;
	
	/**
	 * map of timestamps indexed by session ids, when the last activity update was written to DB.
	 */
	private Map<String, Long> recentUsageUpdates = new WeakHashMap<>();
	
	@Autowired
	public SessionManagementImpl(TokensManagement tokensManagement, ExecutorsService execService,
			LoginToHttpSessionBinder sessionBinder, 
			SessionParticipantTypesRegistry participantTypesRegistry,
			EntityDAO entityDAO, AttributesHelper attributeHelper)
	{
		this.tokensManagement = tokensManagement;
		this.sessionBinder = sessionBinder;
		this.participantTypesRegistry = participantTypesRegistry;
		this.entityDAO = entityDAO;
		this.attributeHelper = attributeHelper;
		execService.getService().scheduleWithFixedDelay(new TerminateInactiveSessions(), 
				20, 30, TimeUnit.SECONDS);
	}

	@Override
	@Transactional
	public LoginSession getCreateSession(long loggedEntity, AuthenticationRealm realm, String entityLabel, 
				String outdatedCredentialId, Date absoluteExpiration, RememberMeInfo rememberMeInfo, String auhtnOptionId)
	{
		try
		{
			try
			{
				LoginSession ret = getOwnedSessionInternal(new EntityParam(loggedEntity), 
						realm.getName());
				if (ret != null)
				{
					ret.setLastUsed(new Date());
					byte[] contents = ret.getTokenContents();
					tokensManagement.updateToken(SESSION_TOKEN_TYPE, ret.getId(), null, 
							contents);
					
					if (log.isDebugEnabled())
						log.debug("Using existing session " + ret.getId() + " for logged entity "
							+ ret.getEntityId() + " in realm " + realm.getName());
					return ret;
				}
			} catch (EngineException e)
			{
				throw new InternalException("Can't retrieve current sessions of the "
						+ "authenticated user", e);
			}
			
			LoginSession ret = createSession(loggedEntity, realm, entityLabel, outdatedCredentialId,
					absoluteExpiration, rememberMeInfo, auhtnOptionId);
			if (log.isDebugEnabled())
				log.debug("Created a new session " + ret.getId() + " for logged entity "
					+ ret.getEntityId() + " in realm " + realm.getName());
			return ret;
		} finally
		{
			clearScheduledRemovalStatus(loggedEntity);
		}
	}
	
	/**
	 * If entity is in the state {@link EntityState#onlyLoginPermitted} this method clears the 
	 *  removal of the entity: state is set to enabled and user ordered removal is removed.
	 * @param entityId
	 * @param sqlMap
	 */
	private void clearScheduledRemovalStatus(long entityId) 
	{
		EntityInformation info = entityDAO.getByKey(entityId);
		if (info.getState() != EntityState.onlyLoginPermitted)
			return;
		log.debug("Removing scheduled removal of an account [as the user is being logged] for entity " + 
			entityId);
		info.setState(EntityState.valid);
		info.setRemovalByUserTime(null);
		entityDAO.updateByKey(entityId, info);
	}
	
	private LoginSession createSession(long loggedEntity, AuthenticationRealm realm, String entityLabel, 
				String outdatedCredentialId, Date absoluteExpiration, RememberMeInfo rememberMeInfo, String auhtnOptionId)
	{
		UUID randomid = UUID.randomUUID();
		String id = randomid.toString();
		LoginSession ls = new LoginSession(id, new Date(), absoluteExpiration,
				realm.getMaxInactivity()*1000, loggedEntity, 
				realm.getName(), rememberMeInfo, auhtnOptionId);
		ls.setOutdatedCredentialId(outdatedCredentialId);
		ls.setEntityLabel(entityLabel);
		try
		{
			tokensManagement.addToken(SESSION_TOKEN_TYPE, id, new EntityParam(loggedEntity), 
					ls.getTokenContents(), ls.getStarted(), ls.getExpires());
			updateLoginAttributes(loggedEntity, ls.getStarted());
		} catch (Exception e)
		{
			throw new InternalException("Can't create a new session", e);
		}
		return ls;
	}

	@Transactional
	@Override
	public void updateSessionAttributes(String id, AttributeUpdater updater) 
	{
		Token token = tokensManagement.getTokenById(SESSION_TOKEN_TYPE, id);
		LoginSession session = token2session(token);
		
		updater.updateAttributes(session.getSessionData());

		byte[] contents = session.getTokenContents();
		tokensManagement.updateToken(SESSION_TOKEN_TYPE, id, null, contents);
	}

	@Override
	public void removeSession(String id, boolean soft)
	{
		sessionBinder.removeLoginSession(id, soft);
		try
		{
			tokensManagement.removeToken(SESSION_TOKEN_TYPE, id);
			if (log.isDebugEnabled())
				log.debug("Removed session with id " + id);
		} catch (IllegalArgumentException e)
		{
			//not found - ok
		}
	}

	@Override
	public LoginSession getSession(String id)
	{
		Token token = tokensManagement.getTokenById(SESSION_TOKEN_TYPE, id);
		return token2session(token);
	}

	
	private LoginSession getOwnedSessionInternal(EntityParam owner, String realm)
			throws EngineException
	{
		List<Token> tokens = tokensManagement.getOwnedTokens(SESSION_TOKEN_TYPE, owner);
		for (Token token: tokens)
		{
			LoginSession ls = token2session(token);
			if (realm.equals(ls.getRealm()))
				return ls;
		}
		return null;
	}
	
	@Override
	@Transactional
	public LoginSession getOwnedSession(EntityParam owner, String realm)
			throws EngineException
	{
		LoginSession ret = getOwnedSessionInternal(owner, realm);
		if (ret == null)
			throw new WrongArgumentException("No session for this owner in the given realm");
		return ret;
	}
	
	@Override
	@Transactional
	public void updateSessionActivity(String id)
	{
		Long lastWrite = recentUsageUpdates.get(id);
		if (lastWrite != null)
		{
			if (System.currentTimeMillis() < lastWrite + DB_ACTIVITY_WRITE_DELAY)
				return;
		}
		
		Token token = tokensManagement.getTokenById(SESSION_TOKEN_TYPE, id);
		LoginSession session = token2session(token);
		session.setLastUsed(new Date());
		byte[] contents = session.getTokenContents();
		tokensManagement.updateToken(SESSION_TOKEN_TYPE, id, null, contents);
		log.trace("Updated in db session activity timestamp for " + id);
		recentUsageUpdates.put(id, System.currentTimeMillis());
	}
	
	@Override
	public void addSessionParticipant(SessionParticipant... participant)
	{
		InvocationContext invocationContext = InvocationContext.getCurrent();
		LoginSession ls = invocationContext.getLoginSession();
		try
		{
			SessionParticipants.AddParticipantToSessionTask addTask = 
					new SessionParticipants.AddParticipantToSessionTask(participantTypesRegistry,
					participant); 
			updateSessionAttributes(ls.getId(), addTask);
		} catch (IllegalArgumentException e)
		{
			throw new InternalException("Can not add session participant to the existing session?", e);
		}
	}
	
	private LoginSession token2session(Token token)
	{
		LoginSession session = new LoginSession();
		session.deserialize(token);
		return session;
	}
	
	private void updateLoginAttributes(long entityId, Date started) throws EngineException
	{
		String loginTime = LocalDateTime.ofInstant(
				started.toInstant().truncatedTo(ChronoUnit.SECONDS), ZoneId.systemDefault())
				.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		Attribute lastAuthn = StringAttribute.of(
				LastAuthenticationAttributeTypeProvider.LAST_AUTHENTICATION, "/", loginTime);
		attributeHelper.addSystemAttribute(entityId, lastAuthn, true);
	}
	
	private class TerminateInactiveSessions implements Runnable
	{
		@Override
		public void run()
		{
			List<Token> tokens = tokensManagement.getAllTokens(SESSION_TOKEN_TYPE);
			long now = System.currentTimeMillis();
			for (Token t: tokens)
			{
				if (t.getExpires() != null)
					continue;
				LoginSession session = token2session(t);
				long inactiveFor = now - session.getLastUsed().getTime(); 
				if (inactiveFor > session.getMaxInactivity())
				{
					log.debug("Expiring login session " + session + " inactive for: " + 
							inactiveFor);
					try
					{
						removeSession(session.getId(), false);
					} catch (Exception e)
					{
						log.error("Can't expire the session " + session, e);
					}
				}
			}
		}
	}
}
