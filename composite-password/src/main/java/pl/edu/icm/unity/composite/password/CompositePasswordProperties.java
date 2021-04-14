/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.composite.password;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;

/**
 * Configuration of the composite-password verificator
 * 
 * @author P.Piernik
 *
 */
public class CompositePasswordProperties extends UnityPropertiesHelper
{
	public enum VerificatorTypes
	{
		password, pam, ldap
	};

	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG,
			CompositePasswordProperties.class);

	@DocumentationReferencePrefix
	public static final String PREFIX = "compositePassword.";

	@DocumentationReferenceMeta
	public static final Map<String, PropertyMD> META = new HashMap<>();
	public static final String VERIFICATORS = "verificators.";
	public static final String VERIFICATOR_TYPE = "verificatorType";
	public static final String VERIFICATOR_NAME = "verificatorName";
	public static final String VERIFICATOR_CONFIG = "verificatorConfig";
	public static final String VERIFICATOR_CONFIG_EMBEDDED = "verificatorConfigEmbedded";
	public static final String VERIFICATOR_CREDENTIAL = "verificatorCredential";

	static
	{
		META.put(VERIFICATORS, new PropertyMD().setStructuredList(true)
				.setDescription("List of enabled verificators"));
		META.put(VERIFICATOR_TYPE,
				new PropertyMD(VerificatorTypes.password)
						.setStructuredListEntry(VERIFICATORS)
						.setDescription("Verificator type"));
		META.put(VERIFICATOR_NAME,
				new PropertyMD().setStructuredListEntry(VERIFICATORS)
						.setDescription("Verificator name").setHidden());
		
		META.put(VERIFICATOR_CONFIG, new PropertyMD().setStructuredListEntry(VERIFICATORS)
				.setDescription("The configuration file of the remote verificator"));
		

		META.put(VERIFICATOR_CONFIG_EMBEDDED, new PropertyMD().setStructuredListEntry(VERIFICATORS)
				.setDescription("The embedded configuration of the remote verificator").setHidden());
			
		META.put(VERIFICATOR_CREDENTIAL, new PropertyMD()
				.setStructuredListEntry(VERIFICATORS)
				.setDescription("For local verificator the name of the local credential associated with it"));
	}

	public CompositePasswordProperties(Properties properties)
	{
		super(PREFIX, properties, META, log);
	}

	public Properties getProperties()
	{
		return properties;
	}
}
