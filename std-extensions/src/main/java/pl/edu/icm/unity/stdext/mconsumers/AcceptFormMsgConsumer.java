/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.mconsumers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

/**
 * 
 * @author P. Piernik
 * 
 */
@Component
public class AcceptFormMsgConsumer extends BaseFormMsgConsumer
{
	@Autowired
	public AcceptFormMsgConsumer(UnityMessageSource msg)
	{
		super(msg);
	}

	@Override
	public String getDescription()
	{
		return msg.getMessage("MessageTemplateConsumer.AcceptForm.desc");
	}

	@Override
	public String getName()
	{
		return "AcceptForm";
	}

}
