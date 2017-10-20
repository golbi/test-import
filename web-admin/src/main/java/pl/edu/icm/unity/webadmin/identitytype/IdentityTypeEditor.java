/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identitytype;

import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.data.util.converter.StringToIntegerConverter;
import com.vaadin.v7.data.validator.IntegerRangeValidator;
import com.vaadin.v7.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.v7.ui.Label;
import com.vaadin.v7.ui.TextArea;
import com.vaadin.v7.ui.TextField;

import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.FormValidator;
import pl.edu.icm.unity.webui.common.boundededitors.IntegerBoundEditor;

/**
 * Allows to edit an identity type. It is only possible to edit description and self modifiable flag. 
 * 
 * @author K. Benedyczak
 */
public class IdentityTypeEditor extends FormLayout
{
	private UnityMessageSource msg;
	
	private IdentityType original;
	private Label name;
	private TextArea description;
	private CheckBox selfModifiable;
	private TextField min;
	private TextField minVerified;
	private IntegerBoundEditor max;
	private FormValidator validator;

	private IdentityTypeSupport idTypeSupport;
	
	public IdentityTypeEditor(UnityMessageSource msg, IdentityTypeSupport idTypeSupport, IdentityType toEdit)
	{
		super();
		this.msg = msg;
		this.idTypeSupport = idTypeSupport;
		original = toEdit;
		
		initUI(toEdit);
	}

	private void initUI(IdentityType toEdit)
	{
		setWidth(100, Unit.PERCENTAGE);

		name = new Label(toEdit.getIdentityTypeProvider());
		name.setCaption(msg.getMessage("IdentityType.name"));
		addComponent(name);
		
		description = new TextArea(msg.getMessage("IdentityType.description"));
		description.setWidth(100, Unit.PERCENTAGE);
		addComponent(description);
		
		selfModifiable = new CheckBox(msg.getMessage("IdentityType.selfModificable"));
		selfModifiable.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				boolean state = selfModifiable.getValue();
				min.setEnabled(state);
				max.setEnabled(state);
				minVerified.setEnabled(state);
			}
		});
		addComponent(selfModifiable);

		Label limInfo = new Label(msg.getMessage("IdentityType.limitsDescription"));
		addComponent(limInfo);
		
		min = new TextField(msg.getMessage("IdentityType.min"));
		min.setConverter(new StringToIntegerConverter());
		min.setConvertedValue(toEdit.getMinInstances());
		min.setNullRepresentation("");
		min.addValidator(new IntegerRangeValidator(msg.getMessage("IdentityType.invalidNumber"), 
				0, Integer.MAX_VALUE));
		addComponent(min);
		
		minVerified = new TextField(msg.getMessage("IdentityType.minVerified"));
		minVerified.setConverter(new StringToIntegerConverter());
		minVerified.setNullRepresentation("");
		minVerified.addValidator(new IntegerRangeValidator(msg.getMessage("IdentityType.invalidNumber"), 
				0, Integer.MAX_VALUE));
		minVerified.setConvertedValue(toEdit.getMinVerifiedInstances());
		addComponent(minVerified);
		IdentityTypeDefinition typeDefinition = idTypeSupport.getTypeDefinition(toEdit.getName());
		if (!typeDefinition.isVerifiable())
			minVerified.setVisible(false);
		
		max = new IntegerBoundEditor(msg, msg.getMessage("IdentityType.maxUnlimited"), 
				msg.getMessage("IdentityType.max"), Integer.MAX_VALUE);
		max.setValue(toEdit.getMaxInstances());
		max.setMin(0);
		addComponent(max);

		validator = new FormValidator(this);
		
		setInitialValues(toEdit);
	}
	
	private void setInitialValues(IdentityType aType)
	{
		description.setValue(aType.getDescription());
		selfModifiable.setValue(aType.isSelfModificable());
	}
	
	public IdentityType getIdentityType() throws FormValidationException
	{
		validator.validate();
		
		IdentityType ret = new IdentityType(original.getName(),
				original.getIdentityTypeProvider());
		ret.setDescription(description.getValue());
		ret.setSelfModificable(selfModifiable.getValue());
		ret.setExtractedAttributes(original.getExtractedAttributes());
		ret.setMinInstances((Integer) min.getConvertedValue());
		ret.setMaxInstances(max.getValue());
		ret.setMinVerifiedInstances((Integer) minVerified.getConvertedValue());
		return ret;
	}
}
