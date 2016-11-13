/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.translation.TranslationCondition;
import pl.edu.icm.unity.engine.translation.TranslationRuleInstance;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Invokes {@link OutputTranslationAction}.
 *  
 * @author K. Benedyczak
 */
public class OutputTranslationRule extends TranslationRuleInstance<OutputTranslationAction>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, OutputTranslationRule.class);
	
	public OutputTranslationRule(OutputTranslationAction action, TranslationCondition condition)
	{
		super(action, condition);
	}
	
	public void invoke(TranslationInput input, Object mvelCtx, String currentProfile,
			TranslationResult result) throws EngineException
	{
		if (conditionInstance.evaluate(mvelCtx))
		{
			log.debug("Condition OK");
			actionInstance.invoke(input, mvelCtx, currentProfile, result);
		} else
		{
			log.debug("Condition not met");			
		}
	}
}