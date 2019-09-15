/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.endpoint;

import java.net.MalformedURLException;
import java.net.URL;

import pl.edu.icm.unity.exceptions.WrongArgumentException;

/**
 * Helper for validation endpoint path
 * 
 * @author P.Piernik
 *
 */
public class EndpointPathValidator
{
	public static void validateEndpointPath(String contextPath) throws WrongArgumentException
	{
		if (contextPath == null || !contextPath.startsWith("/"))
			throw new WrongArgumentException("Context path must start with a leading '/'");
		if (contextPath.indexOf("/", 1) != -1)
			throw new WrongArgumentException("Context path must not possess more then one '/'");
		if (contextPath.length() == 1)
			throw new WrongArgumentException("Context path must be a valid path element of a URL");
		try
		{
			URL tested = new URL("https://localhost:8080" + contextPath);
			if (!contextPath.equals(tested.getPath()))
				throw new WrongArgumentException("Context path must be a valid path element of a URL");
		} catch (MalformedURLException e)
		{
			throw new WrongArgumentException("Context path must be a valid path element of a URL", e);
		}
	}
}
