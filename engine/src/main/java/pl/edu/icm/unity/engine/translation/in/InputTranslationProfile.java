/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.in;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.NDC;
import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.translation.TranslationActionInstance;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.engine.translation.ExecutionBreakException;
import pl.edu.icm.unity.engine.translation.TranslationCondition;
import pl.edu.icm.unity.engine.translation.TranslationProfileInstance;
import pl.edu.icm.unity.engine.translation.TranslationRuleInvocationContext;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Entry point: input translation profile, a list of translation rules annotated with a name and description.
 * @author K. Benedyczak
 */
public class InputTranslationProfile extends TranslationProfileInstance<InputTranslationAction, InputTranslationRule>
{
	public enum ContextKey
	{
		id,
		idType,
		idsByType,
		attrs,
		attr,
		idp,
		groups;
	}

	
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, InputTranslationProfile.class);
	private InputTranslationActionsRegistry registry;
	private InputTranslationProfileRepository profileRepo;
	
	public InputTranslationProfile(TranslationProfile profile,
			InputTranslationProfileRepository profileRepo,
			InputTranslationActionsRegistry registry)
	{
		super(profile, registry);
		this.registry = registry;
		this.profileRepo = profileRepo;
	}	
	
	public MappingResult translate(RemotelyAuthenticatedInput input) throws EngineException
	{
		NDC.push("[TrProfile " + profile.getName() + "]");
		if (log.isDebugEnabled())
			log.debug("Input received from IdP " + input.getIdpName() + ":\n"
					+ input.getTextDump());
		Object mvelCtx = createMvelContext(input);
		try
		{
			int i = 1;
			MappingResult translationState = new MappingResult();
			for (InputTranslationRule rule : ruleInstances)
			{
				NDC.push("[r: " + (i++) + "]");
				try
				{
					TranslationRuleInvocationContext context = rule.invoke(
							input, mvelCtx, translationState,
							profile.getName());
					if (context.getIncludedProfile() != null)
					{
						MappingResult result = invokeInputTranslationProfile(
								context.getIncludedProfile(),
								input);
						translationState.mergeWith(result);
					}

				} catch (ExecutionBreakException e)
				{
					break;
				} finally
				{
					NDC.pop();
				}
			}
			return translationState;
		} finally
		{
			NDC.pop();
		}
	}
	
	public static Map<String, Object> createMvelContext(RemotelyAuthenticatedInput input)
	{
		Map<String, Object> ret = new HashMap<>();
		
		ret.put(ContextKey.idp.name(), input.getIdpName());
		Map<String, Object> attr = new HashMap<String, Object>();
		Map<String, List<Object>> attrs = new HashMap<String, List<Object>>();
		for (RemoteAttribute ra: input.getAttributes().values())
		{
			Object v = ra.getValues().isEmpty() ? "" : ra.getValues().get(0);
			attr.put(ra.getName(), v);
			attrs.put(ra.getName(), ra.getValues());
		}
		ret.put(ContextKey.attr.name(), attr);
		ret.put(ContextKey.attrs.name(), attrs);
		
		if (!input.getIdentities().isEmpty())
		{
			RemoteIdentity ri = input.getIdentities().values().iterator().next();
			ret.put(ContextKey.id.name(), ri.getName());
			ret.put(ContextKey.idType.name(), ri.getIdentityType());
		}
		
		Map<String, List<String>> idsByType = new HashMap<String, List<String>>();
		for (RemoteIdentity ri: input.getIdentities().values())
		{
			List<String> vals = idsByType.get(ri.getIdentityType());
			if (vals == null)
			{
				vals = new ArrayList<String>();
				idsByType.put(ri.getIdentityType(), vals);
			}
			vals.add(ri.getName());
		}
		ret.put(ContextKey.idsByType.name(), idsByType);
		
		ret.put(ContextKey.groups.name(), new ArrayList<String>(input.getGroups().keySet()));
		return ret;
	}
	
	public static Map<String, String> createExpresionValueMap(RemotelyAuthenticatedInput input)
	{
		Map<String, Object> mvelCtx = createMvelContext(input);
		return createExpresionValueMap(mvelCtx);
	}
	
	public static Map<String, String> createExpresionValueMap(Map<String, Object> mvelCtx)
	{
		Map<String, String> exprValMap = new LinkedHashMap<String, String>();

		for (Map.Entry<String, Object> context : mvelCtx.entrySet())
		{
			String contextKey = context.getKey();
			Object contextValue = context.getValue();
			try
			{
				ContextKey.valueOf(contextKey);
			} catch (Exception e)
			{
				throw new IllegalArgumentException("Incorrect MVEL context, unknown context key: " + 
						context.getKey());
			}
			
			if (contextValue instanceof Map)
			{
				@SuppressWarnings("unchecked")
				HashMap<String, Object> value = (HashMap<String, Object>) contextValue;
				for (Map.Entry<String, Object> entry : value.entrySet())
				{
					exprValMap.put(String.format("%s['%s']", contextKey, entry.getKey()), 
							entry.getValue().toString());
				}
			} else if (contextValue instanceof List)
			{
				exprValMap.put(contextKey, contextValue.toString());
				
			} else if (contextValue instanceof String)
			{
				exprValMap.put(contextKey, contextValue.toString());
				
			} else
			{
				throw new IllegalArgumentException("Incorrect MVEL context: unexpected: \"" 
						+ contextValue.getClass() 
						+ "\" type for context key: \"" 
						+ contextKey 
						+ "\"");
			}
		}
		
		return exprValMap;
	}
	
	@Override
	protected InputTranslationRule createRule(TranslationActionInstance action, TranslationCondition condition)
	{
		if (!(action instanceof InputTranslationAction))
		{
			throw new InternalException("The translation action of the input translation "
					+ "profile is not compatible with it, it is " + action.getClass());
		}
		
		return new InputTranslationRule((InputTranslationAction) action, condition);
	}
	
	private MappingResult invokeInputTranslationProfile(String profile, RemotelyAuthenticatedInput input) throws EngineException
	{
		TranslationProfile translationProfile = profileRepo.listAllProfiles().get(profile);
		if (translationProfile == null)
			throw new ConfigurationException("The input translation profile '" + profile + 
					"' included in another profile does not exist");
		InputTranslationProfile profileInstance = new InputTranslationProfile(translationProfile, profileRepo,
				registry);
		MappingResult result = profileInstance.translate(input);
		return result;
	}

}	
