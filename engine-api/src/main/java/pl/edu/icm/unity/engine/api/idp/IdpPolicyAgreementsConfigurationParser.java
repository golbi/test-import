/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.idp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.types.I18nString;

/**
 * Maps {@link PolicyAgreementConfiguration} to properties and vice versa
 * 
 * @author P.Piernik
 *
 */
public class IdpPolicyAgreementsConfigurationParser
{
	public static Properties toProperties(UnityMessageSource msg, IdpPolicyAgreementsConfiguration config,
			String prefix)
	{
		Properties ret = new Properties();
		if (config.title != null && !config.title.isEmpty())
		{
			config.title.toProperties(ret, prefix + CommonIdPProperties.POLICY_AGREEMENTS_TITLE, msg);
		}
		if (config.information != null && !config.information.isEmpty())
		{
			config.information.toProperties(ret, prefix + CommonIdPProperties.POLICY_AGREEMENTS_INFO, msg);
		}
		for (PolicyAgreementConfiguration agreement : config.agreements)
		{
			ret.putAll(policyAgreementConfigurationtoProperties(msg,
					prefix + CommonIdPProperties.POLICY_AGREEMENTS_PFX
							+ (config.agreements.indexOf(agreement) + 1) + ".",
					agreement));
		}
		return ret;
	}

	public static IdpPolicyAgreementsConfiguration fromPropoerties(UnityMessageSource msg,
			UnityPropertiesHelper properties)
	{
		I18nString title = properties.getLocalizedStringWithoutFallbackToDefault(msg,
				CommonIdPProperties.POLICY_AGREEMENTS_TITLE);
		I18nString information = properties.getLocalizedStringWithoutFallbackToDefault(msg,
				CommonIdPProperties.POLICY_AGREEMENTS_INFO);
		List<PolicyAgreementConfiguration> agreements = new ArrayList<>();

		for (String key : properties.getStructuredListKeys(CommonIdPProperties.POLICY_AGREEMENTS_PFX))
		{
			PolicyAgreementConfiguration config = policyAgreementConfigurationfromProperties(msg,
					properties, key);
			agreements.add(config);
		}

		return new IdpPolicyAgreementsConfiguration(title, information, agreements);
	}

	private static PolicyAgreementConfiguration policyAgreementConfigurationfromProperties(UnityMessageSource msg,
			UnityPropertiesHelper properties, String prefix)
	{
		String docsP = properties.getValue(prefix + CommonIdPProperties.POLICY_AGREEMENT_DOCUMENTS);
		List<Long> docs = new ArrayList<>();
		if (docsP != null && !docsP.isEmpty())
		{
			docs.addAll(Arrays.asList(docsP.split(" ")).stream().map(s -> Long.valueOf(s))
					.collect(Collectors.toList()));
		}
		PolicyAgreementPresentationType presentationType = properties.getEnumValue(
				prefix + CommonIdPProperties.POLICY_AGREEMENT_PRESENTATION_TYPE,
				PolicyAgreementPresentationType.class);
		I18nString text = properties.getLocalizedStringWithoutFallbackToDefault(msg,
				prefix + CommonIdPProperties.POLICY_AGREEMENT_TEXT);
		return new PolicyAgreementConfiguration(docs, presentationType, text);

	}

	private static Properties policyAgreementConfigurationtoProperties(UnityMessageSource msg, String prefix,
			PolicyAgreementConfiguration config)
	{
		Properties p = new Properties();
		p.put(prefix + CommonIdPProperties.POLICY_AGREEMENT_DOCUMENTS,
				String.join(" ", config.documentsIdsToAccept.stream().map(id -> String.valueOf(id))
						.collect(Collectors.toList())));
		p.put(prefix + CommonIdPProperties.POLICY_AGREEMENT_PRESENTATION_TYPE,
				config.presentationType.toString());
		config.text.toProperties(p, prefix + CommonIdPProperties.POLICY_AGREEMENT_TEXT, msg);
		return p;
	}
}
