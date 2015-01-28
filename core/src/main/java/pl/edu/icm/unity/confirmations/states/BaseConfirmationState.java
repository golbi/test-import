/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations.states;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.utils.JsonUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Contains common informations used during confirmation
 * @author P. Piernik
 *
 */
public class BaseConfirmationState
{
	protected final ObjectMapper mapper = Constants.MAPPER;
	
	protected String facilityId;
	protected String type;
	protected String value;
	protected String locale;
	protected String successUrl;
	protected String errorUrl;
	
	public BaseConfirmationState(String facilityId, String type, String value, String locale)
	{
		this.type = type;
		this.value = value;
		this.locale = locale;
		this.facilityId = facilityId;
	}
	
	public BaseConfirmationState(String facilityId, String type, String value,
			String locale, String successUrl, String errorUrl)
	{
		this(facilityId, type, value, locale);
		this.errorUrl = errorUrl;
		this.successUrl = successUrl;
	}

	public BaseConfirmationState(String serializedState) throws WrongArgumentException
	{
		setSerializedConfiguration(serializedState);
	}

	protected BaseConfirmationState()
	{
	}

	public String getFacilityId()
	{
		return facilityId;
	}
	public String getType()
	{
		return type;
	}
	public String getValue()
	{
		return value;
	}
	public String getLocale()
	{
		return locale;
	}
	public String getSuccessUrl()
	{
		return successUrl;
	}
	public String getErrorUrl()
	{
		return errorUrl;
	}
	
	public String getSerializedConfiguration() throws InternalException
	{
		return JsonUtil.serialize(createState());
	}
	
	protected ObjectNode createState()
	{
		ObjectNode state = mapper.createObjectNode();
		state.with("confirmationState");
		state.put("value", getValue());
		state.put("type", getType());
		state.put("facilityId", getFacilityId());
		state.put("locale", getLocale());
		if (getSuccessUrl() != null)
			state.put("successUrl", getSuccessUrl());
		if (getErrorUrl() != null)
			state.put("errorUrl", getErrorUrl());
		return state;
	}
	
	protected void setSerializedConfiguration(String json) throws WrongArgumentException
	{
		setSerializedConfiguration(JsonUtil.parse(json));
	}
	
	protected void setSerializedConfiguration(ObjectNode main) throws WrongArgumentException
	{
		try
		{
			type = main.get("type").asText();
			value = main.get("value").asText();
			facilityId = main.get("facilityId").asText();
			locale = main.get("locale").asText();
			if (main.has("successUrl"))
				successUrl = main.get("successUrl").asText();
			if (main.has("errorUrl"))
				errorUrl = main.get("errorUrl").asText();
		} catch (Exception e)
		{
			throw new WrongArgumentException("Can't perform JSON deserialization", e);
		}

	}
}
