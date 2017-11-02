/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

public class I18nTextActionParameterComponent extends I18nTextField implements ActionParameterComponent
{
	public I18nTextActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg)
	{
		super(msg, desc.getName() + ":");
		setDescription(msg.getMessage(desc.getDescriptionKey()));
	}
	
	@Override
	public String getActionValue()
	{
		try
		{
			return Constants.MAPPER.writeValueAsString(getValue().toJson());
		} catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Can't serialize I18nString to JSON", e);
		}
	}

	@Override
	public void setActionValue(String value)
	{
		try
		{
			setValue(Constants.MAPPER.readValue(value, I18nString.class));
		} catch (Exception e)
		{
			throw new IllegalStateException("Can't deserialize I18nString from JSON", e);
		}
	}

	@Override
	public void addValueChangeCallback(ActionParameterValueChangeCallback callback)
	{
		setImmediate(true);
		addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(com.vaadin.data.Property.ValueChangeEvent event)
			{
				callback.refresh();
				
			}
		});		
	}
}
