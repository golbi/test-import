/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.engine.api.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.security.canl.CredentialProperties;
import eu.unicore.security.canl.TruststoreProperties;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.utils.Log;

/**
 * Configures disk based truststores and credentials, which are named and can be referenced.
 * 
 * @author K. Benedyczak
 */
public class UnityPKIConfiguration extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, UnityPKIConfiguration.class);

	@DocumentationReferencePrefix
	public static final String P = "unity.pki.";
	
	public static final String CREDENTIALS = "credentials.";
	public static final String TRUSTSTORES = "truststores.";
	public static final String CERTIFICATES = "certificates.";
	public static final String CERTIFICATE_FILE = "certificateFile";

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> defaults = new HashMap<>();
	
	static
	{
		defaults.put(CREDENTIALS, new PropertyMD().setStructuredList(false).
				setDescription("List of credentials. The subkey defines the credential name."));
		
		for (Map.Entry<String, PropertyMD> e: CredentialProperties.META.entrySet())
			defaults.put(e.getKey(), e.getValue().setStructuredListEntry(CREDENTIALS).setHidden());
		
		defaults.put(TRUSTSTORES, new PropertyMD().setStructuredList(false).
				setDescription("List of truststores. The subkey defines the truststore name."));
		for (Map.Entry<String, PropertyMD> e: TruststoreProperties.META.entrySet())
			defaults.put(e.getKey(), e.getValue().setStructuredListEntry(TRUSTSTORES).setHidden());
		
		defaults.put(CERTIFICATES, new PropertyMD().setStructuredList(false).
				setDescription("List of certificates."));
		defaults.put(CERTIFICATE_FILE, new PropertyMD().setStructuredListEntry(CERTIFICATES).setMandatory().
				setDescription("Certificate file path (PEM format)."));
	}

	public UnityPKIConfiguration(Properties source)
	{
		super(P, source, defaults, log);
	}
	
	public String getCredentialPrefix(String credential)
	{
		return P + credential;
	}

	public String getCredentialName(String listKey)
	{
		return listKey.substring(CREDENTIALS.length(), listKey.length()-1);
	}

	public String getTruststorePrefix(String truststore)
	{
		return P + truststore;
	}

	public String getTruststoreName(String listKey)
	{
		return listKey.substring(TRUSTSTORES.length(), listKey.length()-1);
	}
	
	public String getCertificateName(String listKey)
	{
		return listKey.substring(CERTIFICATES.length(), listKey.length()-1);
	}
	
	public Properties getProperties()
	{
		return properties;
	}
}
