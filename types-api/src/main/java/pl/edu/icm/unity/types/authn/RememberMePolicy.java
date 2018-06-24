/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.authn;

/**
 * Defines how to remeberinging the user's login
 * @author P.Piernik
 *
 */
public enum RememberMePolicy
{
	disallow, allowFor2ndFactor, allowForWholeAuthn
}
