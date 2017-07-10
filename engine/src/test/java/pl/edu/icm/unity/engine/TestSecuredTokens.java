/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Date;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
import pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * 
 * @author P.Piernik
 *
 */
public class TestSecuredTokens extends DBIntegrationTestBase
{
	@Autowired 
	private SecuredTokensManagement securedTokensMan;
	
	@Autowired
	protected TokensManagement tokensMan;
	
	private void addRegularUsers() throws Exception
	{
		IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "u1");

		Identity id = idsMan.addEntity(toAdd,
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT,
				EntityState.valid, false);
		toAdd.setValue("u2");
		Identity id2 = idsMan.addEntity(toAdd,
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT,
				EntityState.valid, false);
		attrsMan.setAttribute(new EntityParam(id),
				EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE, "/",
						AuthorizationManagerImpl.USER_ROLE),
				false);
		attrsMan.setAttribute(new EntityParam(id2),
				EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE, "/",
						AuthorizationManagerImpl.USER_ROLE),
				false);

	}

	private void addTokens() throws Exception
	{
		EntityParam ep1 = new EntityParam(new IdentityParam(UsernameIdentity.ID, "u1"));
		EntityParam ep2 = new EntityParam(new IdentityParam(UsernameIdentity.ID, "u2"));
		byte[] c = new byte[] { 'a' };
		Date exp = new Date(System.currentTimeMillis() + 500000);
		tokensMan.addToken("t", "1234", ep1, c, new Date(), exp);
		tokensMan.addToken("t", "12345", ep1, c, new Date(), exp);
		tokensMan.addToken("t", "123456", ep2, c, new Date(), exp);
	}

	@Test
	public void shouldReturnTokenByType() throws Exception
	{
		addRegularUsers();
		addTokens();
		setupAdmin();
		Collection<Token> admTokens = securedTokensMan.getAllTokens("t");
		assertEquals(3, admTokens.size());
	}

	@Test
	public void shouldReturnOnlyOwnedTokenByType() throws Exception
	{
		addRegularUsers();
		addTokens();

		setupUserContext("u1", false);
		Collection<Token> u1Tokens = securedTokensMan.getAllTokens("t");
		assertEquals(2, u1Tokens.size());

		setupUserContext("u2", false);
		Collection<Token> u2Tokens = securedTokensMan.getAllTokens("t");
		assertEquals(1, u2Tokens.size());
	}

	@Test
	public void shouldDeniedGetNotOwnedTokenByType() throws Exception
	{
		addRegularUsers();
		addTokens();
		EntityParam ep1 = new EntityParam(new IdentityParam(UsernameIdentity.ID, "u1"));

		setupUserContext("u2", false);
		catchException(securedTokensMan).getOwnedTokens("t", ep1);
		assertThat(caughtException(), isA(AuthorizationException.class));
	}

	@Test
	public void shouldReturnOwnedTokenByType() throws Exception
	{
		addRegularUsers();
		addTokens();
		EntityParam ep1 = new EntityParam(new IdentityParam(UsernameIdentity.ID, "u1"));

		setupUserContext("u1", false);
		Collection<Token> u1Tokens = securedTokensMan.getOwnedTokens("t", ep1);
		assertEquals(2, u1Tokens.size());
	}

	@Test
	public void shouldReturnOnlyOwnedToken() throws Exception
	{
		addRegularUsers();
		addTokens();

		setupUserContext("u1", false);
		Collection<Token> u1Tokens = securedTokensMan.getAllTokens(null);
		assertTrue(u1Tokens.size() >= 2);

	}

	@Test
	public void shouldDeniedGetNotOwnedToken() throws Exception
	{
		addRegularUsers();
		addTokens();
		EntityParam ep1 = new EntityParam(new IdentityParam(UsernameIdentity.ID, "u1"));

		setupUserContext("u2", false);
		catchException(securedTokensMan).getOwnedTokens(null, ep1);
		assertThat(caughtException(), isA(AuthorizationException.class));
	}

	@Test
	public void shouldRemoveTokenByOwner() throws Exception
	{
		addRegularUsers();
		EntityParam ep1 = new EntityParam(new IdentityParam(UsernameIdentity.ID, "u1"));
		byte[] c = new byte[] { 'a' };
		Date exp = new Date(System.currentTimeMillis() + 500000);
		tokensMan.addToken("t", "1234", ep1, c, new Date(), exp);

		setupUserContext("u1", false);
		securedTokensMan.removeToken("t", "1234");

		setupAdmin();
		assertEquals(0, securedTokensMan.getAllTokens("t").size());
	}

	@Test
	public void shouldDeniedRemoveTokenByNotOwner() throws Exception
	{
		addRegularUsers();
		EntityParam ep1 = new EntityParam(new IdentityParam(UsernameIdentity.ID, "u1"));
		byte[] c = new byte[] { 'a' };
		Date exp = new Date(System.currentTimeMillis() + 500000);
		tokensMan.addToken("t", "1234", ep1, c, new Date(), exp);

		setupUserContext("u2", false);
		catchException(securedTokensMan).removeToken("t", "1234");
		assertThat(caughtException(), isA(AuthorizationException.class));

		setupAdmin();
		assertEquals(1, securedTokensMan.getAllTokens("t").size());
	}
}
