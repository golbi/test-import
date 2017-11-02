/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credreq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.Action;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.webadmin.credreq.CredentialRequirementEditDialog.Callback;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;

/**
 * Provides {@link CredentialRequirements} management UI
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CredentialRequirementsComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private CredentialRequirementManagement credReqMan;
	private CredentialManagement credMan;
	private EventsBus bus;
	
	private GenericElementsTable<CredentialRequirements> table;
	private CredentialRequirementViewer viewer;
	private com.vaadin.ui.Component main;
	
	@Autowired
	public CredentialRequirementsComponent(UnityMessageSource msg,
			CredentialRequirementManagement authenticationMan,
			CredentialManagement credMan)
	{
		super();
		this.msg = msg;
		this.credReqMan = authenticationMan;
		this.credMan = credMan;
		this.bus = WebSession.getCurrent().getEventBus();
		
		init();
	}
	
	private void init()
	{
		addStyleName(Styles.visibleScroll.toString());
		setCaption(msg.getMessage("CredentialRequirements.caption"));
		viewer = new CredentialRequirementViewer(msg);
		table =  new GenericElementsTable<CredentialRequirements>(
				msg.getMessage("CredentialRequirements.credentialRequirementsHeader"), 
				new GenericElementsTable.NameProvider<CredentialRequirements>()
				{
					@Override
					public Label toRepresentation(CredentialRequirements element)
					{
						Label ret = new Label(element.getName());
						if (element.isReadOnly())
							ret.addStyleName(Styles.readOnlyTableElement.toString());
						return ret;
					}
				});
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Collection<CredentialRequirements> items = getItems(table.getValue());
				if (items.size() > 1 || items.isEmpty())
				{
					viewer.setInput(null);
					return;
				}	
				CredentialRequirements item = items.iterator().next();	
				viewer.setInput(item);
			}
		});
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new AddActionHandler());
		table.addActionHandler(new EditActionHandler());
		table.addActionHandler(new DeleteActionHandler());
		table.setWidth(90, Unit.PERCENTAGE);
		table.setMultiSelect(true);
		Toolbar toolbar = new Toolbar(table, Orientation.HORIZONTAL);
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);
		
		HorizontalLayout hl = new HorizontalLayout();
		hl.addComponents(tableWithToolbar, viewer);
		hl.setSizeFull();
		hl.setMargin(new MarginInfo(true, false, true, false));
		hl.setSpacing(true);
		main = hl;
		refresh();
	}
	
	public void refresh()
	{
		try
		{
			Collection<CredentialRequirements> crs = credReqMan.getCredentialRequirements();
			table.setInput(crs);
			removeAllComponents();
			addComponent(main);
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("CredentialRequirements.errorGetCredentialRequirements"), e);
			removeAllComponents();
			addComponent(error);
		}
		
	}

	public Collection<CredentialRequirements> getCredentialRequirements()
	{
		try
		{
			return credReqMan.getCredentialRequirements();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialRequirements.errorGetCredentialRequirements"), e);
			return null;
		}
	}

	private Collection<CredentialDefinition> getCredentials()
	{
		try
		{
			return credMan.getCredentialDefinitions();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialRequirements.errorGetCredentials"), e);
			return null;
		}
		
	}

	private boolean updateCR(CredentialRequirements cr)
	{
		try
		{
			credReqMan.updateCredentialRequirement(cr);
			refresh();
			bus.fireEvent(new CredentialRequirementChangedEvent());
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialRequirements.errorUpdate"), e);
			return false;
		}
	}

	private boolean addCR(CredentialRequirements cr)
	{
		try
		{
			credReqMan.addCredentialRequirement(cr);
			refresh();
			bus.fireEvent(new CredentialRequirementChangedEvent());
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialRequirements.errorAdd"), e);
			return false;
		}
	}

	private boolean removeCR(String toRemove, String replacementId)
	{
		try
		{
			credReqMan.removeCredentialRequirement(toRemove, replacementId);
			refresh();
			bus.fireEvent(new CredentialRequirementChangedEvent());
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialRequirements.errorRemove"), e);
			return false;
		}
	}
	
	private Collection<CredentialRequirements> getItems(Object target)
	{
		Collection<?> c = (Collection<?>) target;
	        Collection<CredentialRequirements> items = new ArrayList<CredentialRequirements>();
		for (Object o: c)
		{
			GenericItem<?> i = (GenericItem<?>) o;
			items.add((CredentialRequirements) i.getElement());	
		}	
		return items;
	}

	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("CredentialRequirements.refreshAction"), Images.refresh.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			refresh();
		}
	}
	
	private class AddActionHandler extends SingleActionHandler
	{
		public AddActionHandler()
		{
			super(msg.getMessage("CredentialRequirements.addAction"), Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			Collection<CredentialDefinition> allCredentials = getCredentials();
			if (allCredentials == null)
				return;
			CredentialRequirementEditor editor = new CredentialRequirementEditor(msg, allCredentials);
			CredentialRequirementEditDialog dialog = new CredentialRequirementEditDialog(msg, 
					msg.getMessage("CredentialRequirements.addAction"), editor, 
					new Callback()
					{
						@Override
						public boolean newCredentialRequirement(CredentialRequirements cr)
						{
							return addCR(cr);
						}
					});
			dialog.show();
		}
	}
	
	private class EditActionHandler extends AbstractCredentialReqActionHandler
	{
		public EditActionHandler()
		{
			super(msg.getMessage("CredentialRequirements.editAction"), Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			Collection<CredentialDefinition> allCredentials = getCredentials();	
			if (allCredentials == null)
				return;
			
			GenericItem<?> item = (GenericItem<?>) target;			
			CredentialRequirements cr = (CredentialRequirements) item.getElement();
			CredentialRequirements crClone = new CredentialRequirements();
			crClone.setDescription(cr.getDescription());
			crClone.setName(cr.getName());
			crClone.setRequiredCredentials(new HashSet<String>(cr.getRequiredCredentials()));
			CredentialRequirementEditor editor = new CredentialRequirementEditor(msg, allCredentials, crClone);
			CredentialRequirementEditDialog dialog = new CredentialRequirementEditDialog(msg, 
					msg.getMessage("CredentialRequirements.editAction"), editor, 
					new Callback()
					{
						@Override
						public boolean newCredentialRequirement(CredentialRequirements cr)
						{
							return updateCR(cr);
						}
					});
			dialog.show();
		}
	}
	
	private class DeleteActionHandler extends AbstractCredentialReqActionHandler
	{
		public DeleteActionHandler()
		{
			super(msg.getMessage("CredentialRequirements.deleteAction"), 
					Images.delete.getResource());
			setMultiTarget(true);
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{		
			final Collection<CredentialRequirements> items = getItems(target);			
			HashSet<String> removed = new HashSet<String>();
			for (CredentialRequirements item : items)
			{
				removed.add(item.getName());
				
			}			
			Collection<CredentialRequirements> allCRs = getCredentialRequirements();
			new CredentialRequirementRemovalDialog(msg, removed, allCRs, 
					new CredentialRequirementRemovalDialog.Callback()
			{
				@Override
				public void onConfirm(String replacementCR)
				{
					for (CredentialRequirements item : items)
					{
						removeCR(item.getName(), replacementCR);
					}
				}
			}).show();
		}
	}
	
	private abstract class AbstractCredentialReqActionHandler extends SingleActionHandler
	{

		public AbstractCredentialReqActionHandler(String caption, Resource icon)
		{
			super(caption, icon);
			setNeedsTarget(true);
		}

		@Override
		public Action[] getActions(Object target, Object sender)
		{
			if (target == null)
			{
				return EMPTY;

			} else
			{
				if (target instanceof Collection<?>)
				{
					Collection<CredentialRequirements> items = getItems(target);
					for (CredentialRequirements cr : items)
						if (cr.isReadOnly())
							return EMPTY;
				} else
				{
					GenericItem<?> item = (GenericItem<?>) target;	
					CredentialRequirements cr = (CredentialRequirements) item.getElement();
					if (cr.isReadOnly())
						return EMPTY;
				}
			}
			return super.getActions(target, sender);
		}

	}
	

}
