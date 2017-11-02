/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.TranslationProfileViewer;

/**
 * Simple extension of the {@link TranslationProfileViewer}, hiding name and description 
 * of the profile, which is not relevant in the profile used in a registration form.
 * @author K. Benedyczak
 */
public class RegistrationTranslationProfileViewer extends TranslationProfileViewer
{

	public RegistrationTranslationProfileViewer(UnityMessageSource msg)
	{
		super(msg);
	}

	protected void initUI()
	{
		super.initUI();
		name.setVisible(false);
		description.setVisible(false);
		mode.setVisible(false);
	}
}
