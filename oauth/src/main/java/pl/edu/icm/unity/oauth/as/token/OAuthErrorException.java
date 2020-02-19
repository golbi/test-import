/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import javax.ws.rs.core.Response;

import pl.edu.icm.unity.exceptions.EngineException;

class OAuthErrorException extends EngineException
{
	Response response;

	OAuthErrorException(Response response)
	{
		this.response = response;
	}
}