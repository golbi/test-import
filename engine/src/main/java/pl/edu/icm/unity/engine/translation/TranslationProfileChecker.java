/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.attribute.AttributeValueConverter;
import pl.edu.icm.unity.engine.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.engine.translation.in.InputTranslationProfileRepository;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfileRepository;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Set of helper method for translation profiles
 * No operation in this class performs any authorization.
 * @author P.Piernik
 *
 */
@Component
public class TranslationProfileChecker
{
	private InputTranslationProfileRepository inputRepo;
	private InputTranslationActionsRegistry inputActionReg;
	private OutputTranslationProfileRepository outputRepo;
	private AttributeValueConverter attrConverter;
	private OutputTranslationActionsRegistry outputActionReg;
	
	@Autowired
	public TranslationProfileChecker(InputTranslationProfileRepository inputRepo,
			InputTranslationActionsRegistry inputActionReg,
			OutputTranslationProfileRepository outputRepo,
			AttributeValueConverter attrConverter,
			OutputTranslationActionsRegistry outputActionReg)
	{
		this.inputRepo = inputRepo;
		this.inputActionReg = inputActionReg;
		this.outputRepo = outputRepo;
		this.attrConverter = attrConverter;
		this.outputActionReg = outputActionReg;
	}

	public void checkProfileContent(TranslationProfile profile)
	{
		TranslationProfileInstance<?, ?> instance;	
		if (profile.getProfileType() == ProfileType.INPUT)
			instance = new InputTranslationProfile(profile, inputRepo, inputActionReg);
		else if (profile.getProfileType() == ProfileType.OUTPUT)
			instance = new OutputTranslationProfile(profile, outputRepo, outputActionReg, attrConverter);
		else
			throw new IllegalArgumentException("Unsupported profile type: " + profile.getProfileType());
		if (instance.hasInvalidActions())
			throw new IllegalArgumentException("Profile definition is invalid");
	}
}
