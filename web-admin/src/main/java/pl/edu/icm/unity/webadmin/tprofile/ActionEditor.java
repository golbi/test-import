/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.webadmin.tprofile.ActionParameterComponent.ActionParameterValueChangeCallback;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.LayoutEmbeddable;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.RequiredComboBox;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Responsible for editing of a single {@link TranslationAction}
 * 
 */
public class ActionEditor extends LayoutEmbeddable
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ActionEditor.class);
	private UnityMessageSource msg;
	private TypesRegistryBase<? extends TranslationActionFactory<?>> tc;
	
	private ComboBox actions;
	private Label actionParams;
	private ActionParameterComponentProvider actionComponentProvider;
	private List<ActionParameterComponent> paramComponents = new ArrayList<>();
	private Callback callback;
	
	public ActionEditor(UnityMessageSource msg, TypesRegistryBase<? extends TranslationActionFactory<?>> tc,
			TranslationAction toEdit, ActionParameterComponentProvider actionComponentProvider,
			Callback callback)
	{
		this.msg = msg;
		this.tc = tc;
		this.actionComponentProvider = actionComponentProvider;
		this.callback = callback;
		initUI(toEdit);
	}
	
	
	public ActionEditor(UnityMessageSource msg, TypesRegistryBase<? extends TranslationActionFactory<?>> tc,
			TranslationAction toEdit, ActionParameterComponentProvider actionComponentProvider)
	{
		this(msg, tc, toEdit, actionComponentProvider, null);
	}

	private void initUI(TranslationAction toEdit)
	{
		actions = new RequiredComboBox(msg.getMessage("ActionEditor.ruleAction"), msg);
		tc.getAll().stream().
			map(af -> af.getActionType().getName()).
			sorted().
			forEach(actionName -> actions.addItem(actionName));
		actions.setStyleName(Styles.vTiny.toString());
		
		actions.setNullSelectionAllowed(false);
		actions.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				String action = (String) actions.getValue();
				setParams(action, null);
				if (callback != null)
					callback.refresh(ActionEditor.this);
			}
		});
		
		actionParams = new Label();
		actionParams.setCaption(msg.getMessage("ActionEditor.actionParameters"));

		addComponents(actions, actionParams);
		
		if (toEdit != null)
		{
			setInput(toEdit);
		} else
		{
			if (!actions.getItemIds().isEmpty())
			{
				Object firstItem = actions.getItemIds().iterator().next();
				actions.setValue(firstItem);
				setParams((String) actions.getValue(), null);
			}
		}
	}

	public void setInput(TranslationAction toEdit)
	{
		actions.setValue(toEdit.getName());
		setParams(actions.getValue().toString(), toEdit.getParameters());
	}
	
	private void setParams(String action, String[] values)
	{
		CallbackImplementation paramCallback = new CallbackImplementation();
		removeComponents(paramComponents);
		paramComponents.clear();
		
		TranslationActionFactory<?> factory = getActionFactory(action);
		if (factory == null)
			return;
		
		actions.setDescription(msg.getMessage(factory.getActionType().getDescriptionKey()));
		ActionParameterDefinition[] params = factory.getActionType().getParameters();	
		for (int i = 0; i < params.length; i++)
		{
			ActionParameterComponent p = actionComponentProvider.getParameterComponent(params[i]);
			p.setStyleName(Styles.vTiny.toString());
			p.addValueChangeCallback(paramCallback);
			p.setValidationVisible(false);
			if (values != null && values.length > i)
			{
				p.setActionValue(values[i]);
			}		
			paramComponents.add(p);
			addComponent(p);
		}
		actionParams.setVisible(!paramComponents.isEmpty());
	}

	private String[] getActionParams() throws FormValidationException
	{
		List<String> params = new ArrayList<>();
		boolean errors = false;
		for (ActionParameterComponent tc: paramComponents)
		{
			tc.setValidationVisible(true);
			if (!tc.isValid())
				errors = true;
			else
				params.add(tc.getActionValue());
		}
		if (errors)
			throw new FormValidationException();
		String[] wrapper = new String[params.size()];
		return params.toArray(wrapper);
	}
	
	private TranslationActionFactory<?> getActionFactory(String action)
	{
		TranslationActionFactory<?> factory = null;
		try
		{
			factory = tc.getByName(action);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("ActionEditor.errorGetActions"), e);
		}
		return factory;
	}
	

	public TranslationAction getAction() throws FormValidationException
	{
		String ac = (String) actions.getValue();
		TranslationActionFactory<?> factory = getActionFactory(ac);
		try
		{
			return factory.getInstance(getActionParams());
		} catch (FormValidationException e)
		{
			throw e;
		} catch (Exception e)
		{
			log.debug("Got profile's action validation exception", e);
			String error = msg.getMessage("ActionEditor.parametersError", e.getMessage());
			UserError ue = new UserError(error);
			for (ActionParameterComponent tc: paramComponents)
				((AbstractComponent)tc).setComponentError(ue);
			throw new FormValidationException(error);
		}
	}
	
	public void setReadOnlyStyle(boolean readOnly)
	{
		actions.setReadOnly(readOnly);
		for (Component param: paramComponents)
			param.setReadOnly(readOnly);
	}
	
	
	public void indicateExpressionError(Exception e) 
	{
		for (ActionParameterComponent c: paramComponents)
		{
			if (c instanceof ExpressionActionParameterComponent)
			{
				ExpressionActionParameterComponent extension = (ExpressionActionParameterComponent) c;
				extension.setStyleName(Styles.errorBackground.toString());
				extension.setComponentError(new UserError(NotificationPopup.getHumanMessage(e)));
				extension.setValidationVisible(true);
				break;
			}
		}	
	}
	
	
	public void setStyle(String style)
	{
		actions.setStyleName(style);
		for (ActionParameterComponent c: paramComponents)
			c.setStyleName(style);
	}
	
	public void removeComponentEvaluationStyle()
	{
		actions.removeStyleName(Styles.falseConditionBackground.toString());
		actions.removeStyleName(Styles.trueConditionBackground.toString());
		
		for (ActionParameterComponent c: paramComponents)
		{
			c.removeStyleName(Styles.falseConditionBackground.toString());
			c.removeStyleName(Styles.trueConditionBackground.toString());
			c.removeStyleName(Styles.errorBackground.toString());
			if (c instanceof ExpressionActionParameterComponent)
			{
				ExpressionActionParameterComponent extension = (ExpressionActionParameterComponent) c;
				extension.setComponentError(null);
				extension.setValidationVisible(false);
			}			
		}	
	}
	
	public String getStringRepresentation()
	{
		StringBuilder rep = new StringBuilder();
		rep.append(actions.getValue());
		rep.append(" | ");
		for (ActionParameterComponent tc: paramComponents)
		{
			rep.append(tc.getCaption() + tc.getActionValue());
			rep.append("|");
		}
		
		return rep.substring(0, rep.length() - 1);
	}
	
	private final class CallbackImplementation implements ActionParameterValueChangeCallback
	{

		@Override
		public void refresh()
		{
			callback.refresh(ActionEditor.this);
		}
		
	}
	
	
	public interface Callback
	{
		public void refresh(ActionEditor editor);
		
	}

}
