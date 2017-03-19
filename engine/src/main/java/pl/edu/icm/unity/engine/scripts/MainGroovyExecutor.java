/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.scripts;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import groovy.lang.Binding;
import pl.edu.icm.unity.base.event.Event;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.BulkProcessingManagement;
import pl.edu.icm.unity.engine.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.IdentityTypesManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.UserImportManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.initializers.ScriptConfiguration;
import pl.edu.icm.unity.engine.api.initializers.ScriptType;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Executes GROOVY scripts given by user in
 * {@link UnityServerConfiguration#SCRIPTS} configuration.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
@Component
public class MainGroovyExecutor
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER, MainGroovyExecutor.class);
	
	@Autowired
	private IdentityTypesRegistry idTypesReg;
	@Autowired
	private UnityMessageSource unityMessageSource;
	@Autowired
	private UnityServerConfiguration config;
	@Autowired
	private AttributeTypeSupport attributeTypeSupport;
	@Autowired
	private IdentityTypeSupport identityTypeSupport;
	
	@Autowired
	@Qualifier("insecure")
	private BulkProcessingManagement bulkProcessingManagement;
	@Autowired
	@Qualifier("insecure")
	private PreferencesManagement preferencesManagement;
	@Autowired
	@Qualifier("insecure")
	private UserImportManagement userImportManagement;
	@Autowired
	@Qualifier("insecure")
	private IdentityTypesManagement identityTypesManagement;
	@Autowired
	@Qualifier("insecure")
	private AttributeClassManagement attributeClassManagement;
	@Autowired
	@Qualifier("insecure")
	private AttributesManagement attributesManagement;
	@Autowired
	@Qualifier("insecure")
	private AttributeTypeManagement attributeTypeManagement;
	@Autowired
	@Qualifier("insecure")
	private AuthenticatorManagement authenticatorManagement;
	@Autowired
	@Qualifier("insecure")
	private ConfirmationConfigurationManagement confirmationConfigurationManagement;
	@Autowired
	@Qualifier("insecure")
	private CredentialManagement credentialManagement;
	@Autowired
	@Qualifier("insecure")
	private CredentialRequirementManagement credentialRequirementManagement;
	@Autowired
	@Qualifier("insecure")
	private EndpointManagement endpointManagement;
	@Autowired
	@Qualifier("insecure")
	private EnquiryManagement enquiryManagement;
	@Autowired
	@Qualifier("insecure")
	private EntityCredentialManagement entityCredentialManagement;
	@Autowired
	@Qualifier("insecure")
	private EntityManagement entityManagement;
	@Autowired
	@Qualifier("insecure")
	private GroupsManagement groupsManagement;
	@Autowired
	@Qualifier("insecure")
	private InvitationManagement invitationManagement;
	@Autowired
	@Qualifier("insecure")
	private MessageTemplateManagement messageTemplateManagement;
	@Autowired
	@Qualifier("insecure")
	private NotificationsManagement notificationsManagement;
	@Autowired
	@Qualifier("insecure")
	private RealmsManagement realmsManagement;
	@Autowired
	@Qualifier("insecure")
	private RegistrationsManagement registrationsManagement;
	@Autowired
	@Qualifier("insecure")
	private TranslationProfileManagement translationProfileManagement;
	
	
	@Autowired
	private ApplicationContext applCtx;
	
	private boolean coldStart = false;

	@PostConstruct
	public void initialize()
	{
		coldStart = determineIfColdStart();
	}

	public void run(ScriptConfiguration conf, Event event)
	{
		if (conf == null || conf.getType() != ScriptType.groovy)
			throw new IllegalArgumentException(
					"conf must not be null and must be of " + 
							ScriptType.groovy + " type");
		Reader scriptReader = getFileReader(conf.getFileLocation());
		GroovyRunner.run(conf.getTrigger(), conf.getFileLocation(), scriptReader, 
				getBinding(event));
	}

	private Reader getFileReader(String location)
	{
		Resource resource = applCtx.getResource(location);
		try
		{
			return new InputStreamReader(resource.getInputStream());
		} catch (IOException e)
		{
			throw new ConfigurationException("Error loading script " + location, e);
		}
	}

	Binding getBinding(Event event)
	{
		Binding binding = new Binding();
		binding.setVariable("event", event.getTrigger());
		binding.setVariable("context", event.getContents());
		binding.setVariable("config", config);
		binding.setVariable("attributeClassManagement", attributeClassManagement);
		binding.setVariable("attributesManagement", attributesManagement);
		binding.setVariable("attributeTypeManagement", attributeTypeManagement);
		binding.setVariable("authenticatorManagement", authenticatorManagement);
		binding.setVariable("bulkProcessingManagement", bulkProcessingManagement);
		binding.setVariable("confirmationConfigurationManagement", confirmationConfigurationManagement);
		binding.setVariable("credentialManagement", credentialManagement);
		binding.setVariable("credentialRequirementManagement", credentialRequirementManagement);
		binding.setVariable("endpointManagement", endpointManagement);
		binding.setVariable("enquiryManagement", enquiryManagement);
		binding.setVariable("entityCredentialManagement", entityCredentialManagement);
		binding.setVariable("entityManagement", entityManagement);
		binding.setVariable("groupsManagement", groupsManagement);
		binding.setVariable("identityTypesManagement", identityTypesManagement);
		binding.setVariable("invitationManagement", invitationManagement);
		binding.setVariable("messageTemplateManagement", messageTemplateManagement);
		binding.setVariable("notificationsManagement", notificationsManagement);
		binding.setVariable("preferencesManagement", preferencesManagement);
		binding.setVariable("realmsManagement", realmsManagement);
		binding.setVariable("registrationsManagement", registrationsManagement);
		binding.setVariable("translationProfileManagement", translationProfileManagement);
		binding.setVariable("userImportManagement", userImportManagement);
		binding.setVariable("msgSrc", unityMessageSource);
		binding.setVariable("attributeTypeSupport", attributeTypeSupport);
		binding.setVariable("identityTypeSupport", identityTypeSupport);
		binding.setVariable("isColdStart", coldStart);
		binding.setVariable("log", LOG);
		return binding;
	}
	
	private boolean determineIfColdStart()
	{
		try
		{
			Map<String, IdentityType> defined = identityTypesManagement.getIdentityTypes()
					.stream().collect(Collectors.toMap(IdentityType::getName, type -> type));
			for (IdentityTypeDefinition it: idTypesReg.getAll())
			{
				if (!defined.containsKey(it.getId()))
					return true;
			}
		} catch (EngineException e)
		{
			throw new InternalException("Initialization problem when checking identity types.", e);
		}
		return false;
	}

	public boolean isColdStart()
	{
		return coldStart;
	}
}
