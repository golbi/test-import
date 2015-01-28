/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations.states;

import pl.edu.icm.unity.exceptions.WrongArgumentException;


/**
 * Contains necessary informations used during the confirmation a identity in
 * registration request
 * 
 * @author P. Piernik
 * 
 */
public class RegistrationReqIdentityConfirmationState extends RegistrationConfirmationState
{
	public static final String FACILITY_ID = "RegistrationReqIdentityFacility";

	public RegistrationReqIdentityConfirmationState(String serializedState) throws WrongArgumentException
	{
		super(serializedState);
	}

	public RegistrationReqIdentityConfirmationState(String requestId, String type,
			String value, String locale, String successUrl, String errorUrl)
	{
		super(FACILITY_ID, type, value, locale, successUrl, errorUrl, requestId);
	}
	
	public RegistrationReqIdentityConfirmationState(String requestId, String type,
			String value, String locale)
	{
		super(FACILITY_ID, type, value, locale, requestId);
	}
}
