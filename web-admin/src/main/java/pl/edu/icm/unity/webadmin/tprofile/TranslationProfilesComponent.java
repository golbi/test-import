/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.simplefiledownloader.SimpleFileDownloader;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.Action;
import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.engine.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.types.translation.ProfileMode;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webadmin.WebAdminEndpointFactory;
import pl.edu.icm.unity.webadmin.tprofile.dryrun.DryRunWizardProvider;
import pl.edu.icm.unity.webadmin.tprofile.wizard.ProfileWizardProvider;
import pl.edu.icm.unity.webadmin.utils.MessageUtils;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.GenericElementsTable.NameProvider;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.webui.sandbox.wizard.SandboxWizardDialog;

/**
 * Responsible for translation profiles management.
 * @author P. Piernik
 */
@PrototypeComponent
public class TranslationProfilesComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private TranslationProfileManagement profileMan;
	private GenericElementsTable<TranslationProfile> table;
	private TranslationProfileViewer viewer;
	private com.vaadin.ui.Component main;
	private OptionGroup profileType;
	
	private InputTranslationActionsRegistry inputActionsRegistry;
	private OutputTranslationActionsRegistry outputActionsRegistry;

	private SandboxAuthnNotifier sandboxNotifier;
	private String sandboxURL;
	private ActionParameterComponentProvider actionComponentFactory;
	
	@Autowired
	public TranslationProfilesComponent(UnityMessageSource msg, TranslationProfileManagement profileMan,
			EndpointManagement endpointMan,
			InputTranslationActionsRegistry inputTranslationActionsRegistry,
			OutputTranslationActionsRegistry outputTranslationActionsRegistry,
			ActionParameterComponentProvider actionComponentFactory)
	{
		this.msg = msg;
		this.profileMan = profileMan;
		inputActionsRegistry = inputTranslationActionsRegistry;
		outputActionsRegistry = outputTranslationActionsRegistry;
		this.actionComponentFactory = actionComponentFactory;

		setCaption(msg.getMessage("TranslationProfilesComponent.capion"));		
		
		try 
		{
			actionComponentFactory.init();
			establishSandboxURL(endpointMan);
		} catch (EngineException e) 
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("TranslationProfilesComponent.errorGetEndpoints"), e);
			removeAllComponents();
			addComponent(error);
			return;
		}

		buildUI();
		
		refresh();
	}
	
	private void establishSandboxURL(EndpointManagement endpointMan) throws EngineException
	{
			List<ResolvedEndpoint> endpointList = endpointMan.getEndpoints();
			for (ResolvedEndpoint endpoint : endpointList) {
				if (endpoint.getType().getName().equals(WebAdminEndpointFactory.NAME))
				{
					sandboxURL = endpoint.getEndpoint().getContextAddress() + 
							VaadinEndpoint.SANDBOX_PATH_TRANSLATION;
					break;
				}
			}
	}
	
	private GenericElementsTable<TranslationProfile> createTable()
	{
		GenericElementsTable<TranslationProfile> table = new GenericElementsTable<>(
				msg.getMessage("TranslationProfilesComponent.profilesTable"),
				new NameProvider<TranslationProfile>()
				{
					@Override
					public Object toRepresentation(TranslationProfile element)
					{
						Label ret = new Label(element.getName());
						if (element.getProfileMode() == ProfileMode.READ_ONLY)
							ret.addStyleName(Styles.readOnlyTableElement.toString());
						return ret;
					}
				});
		
		table.setMultiSelect(true);
		table.setWidth(90, Unit.PERCENTAGE);
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Collection<TranslationProfile> items = getItems(table.getValue());
				if (items.size() > 1 || items.isEmpty())
				{
					viewer.setInput(null, getCurrentActionsRegistry());
					return;
				}	
				TranslationProfile item = items.iterator().next();
				viewer.setInput(item, getCurrentActionsRegistry());
			}
		});
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new AddActionHandler());
		table.addActionHandler(new EditActionHandler());
		table.addActionHandler(new CopyActionHandler());
		table.addActionHandler(new DeleteActionHandler());
		table.addActionHandler(new WizardActionHandler());
		table.addActionHandler(new DryRunActionHandler());
		table.addActionHandler(new ExportActionHandler());
		return table;
	}
	
	private void buildUI()
	{
		addStyleName(Styles.visibleScroll.toString());
		HorizontalLayout hl = new HorizontalLayout();
		table = createTable();
		
		profileType = new OptionGroup();
		profileType.addItem(ProfileType.INPUT);
		profileType.setItemCaption(ProfileType.INPUT, 
				msg.getMessage("TranslationProfilesComponent.inputProfileType"));
		profileType.addItem(ProfileType.OUTPUT);
		profileType.setItemCaption(ProfileType.OUTPUT, 
				msg.getMessage("TranslationProfilesComponent.outputProfileType"));
		profileType.setNullSelectionAllowed(false);
		profileType.select(ProfileType.INPUT);
		profileType.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				refresh();
			}
		});
		
		
		Toolbar toolbar = new Toolbar(table, Orientation.HORIZONTAL);
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);

		viewer = new TranslationProfileViewer(msg);
		
		VerticalLayout left = new VerticalLayout();
		left.setSpacing(true);
		left.addComponents(profileType, tableWithToolbar);
		
		hl.addComponents(left, viewer);
		hl.setSizeFull();
		hl.setMargin(true);
		hl.setSpacing(true);
		hl.setMargin(new MarginInfo(true, false, true, false));
		main = hl;
		hl.setExpandRatio(left, 0.3f);
		hl.setExpandRatio(viewer, 0.7f);
	}
	
	private void refresh()
	{
		try
		{
			ProfileType pt = (ProfileType) profileType.getValue();
			
			switch (pt)
			{
			case INPUT:
				Collection<TranslationProfile> inprofiles = profileMan.listInputProfiles().values();
				table.setInput(inprofiles);
				break;
			case OUTPUT:
				Collection<TranslationProfile> outprofiles = profileMan.listOutputProfiles().values();
				table.setInput(outprofiles);
				break;
			default:
				throw new IllegalStateException("unknown profile type");
			}
			viewer.setInput(null, getCurrentActionsRegistry());
			removeAllComponents();
			addComponent(main);
			actionComponentFactory.init();
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("TranslationProfilesComponent.errorGetProfiles"), e);
			removeAllComponents();
			addComponent(error);
		}
		
	}
	
	private boolean updateProfile(TranslationProfile updatedProfile)
	{
		try
		{
			profileMan.updateProfile(updatedProfile);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorUpdate"), e);
			return false;
		}
	}
		
	private boolean addProfile(TranslationProfile profile)
	{
		try
		{
			profileMan.addProfile(profile);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorAdd"), e);
			return false;
		}
	}
	
	private boolean removeProfile(String name)
	{
		try
		{
			profileMan.removeProfile((ProfileType) profileType.getValue(), name);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorRemove"), e);
			return false;
		}
	}
	
	private Collection<TranslationProfile> getItems(Object target)
	{
		Collection<?> c = (Collection<?>) target;
		Collection<TranslationProfile> items = new ArrayList<>();
		for (Object o: c)
		{
			GenericItem<?> i = (GenericItem<?>) o;
			items.add((TranslationProfile) i.getElement());	
		}	
		return items;
	}
	
	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("TranslationProfilesComponent.refreshAction"), Images.refresh.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			refresh();
		}
	}
	
	private TranslationProfileEditor getProfileEditor(TranslationProfile toEdit) throws EngineException
	{
		ProfileType pt = (ProfileType) profileType.getValue();
		TranslationProfileEditor editor = new TranslationProfileEditor(msg, getCurrentActionsRegistry(), pt, 
				actionComponentFactory);
		if (toEdit != null)
			editor.setValue(toEdit);
		return editor;
	}
	
	private TypesRegistryBase<? extends TranslationActionFactory<?>> getCurrentActionsRegistry()
	{
		ProfileType pt = (ProfileType) profileType.getValue();
		return pt == ProfileType.INPUT ? inputActionsRegistry : outputActionsRegistry;
	}
	
	private class AddActionHandler extends SingleActionHandler
	{
		public AddActionHandler()
		{
			super(msg.getMessage("TranslationProfilesComponent.addAction"), Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			TranslationProfileEditor editor;
			try
			{
				editor = getProfileEditor(null);				
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorReadData"),
						e);
				return;
			}
			
			TranslationProfileEditDialog dialog = new TranslationProfileEditDialog(msg, 
					msg.getMessage("TranslationProfilesComponent.addAction"), 
					new TranslationProfileEditDialog.Callback()
					{
						@Override
						public boolean handleProfile(TranslationProfile profile)
						{
							return addProfile(profile);
						}
					}, editor);
			dialog.show();
		}
	}
	
	private class EditActionHandler extends AbstractTranslationProfileActionHandler
	{
		public EditActionHandler()
		{
			super(msg.getMessage("TranslationProfilesComponent.editAction"), Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			@SuppressWarnings("unchecked")
			GenericItem<TranslationProfile> item = (GenericItem<TranslationProfile>) target;
			TranslationProfileEditor editor;
			try
			{
				editor = getProfileEditor(item.getElement());
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorReadData"),
						e);
				return;
			}
			TranslationProfileEditDialog dialog = new TranslationProfileEditDialog(msg, 
					msg.getMessage("TranslationProfilesComponent.editAction"), 
					new TranslationProfileEditDialog.Callback()
					{
						@Override
						public boolean handleProfile(TranslationProfile profile)
						{
							return updateProfile(profile);
						}
					}, editor);
			dialog.show();
		}
	}
	
	private class CopyActionHandler extends SingleActionHandler
	{
		public CopyActionHandler()
		{
			super(msg.getMessage("TranslationProfilesComponent.copyAction"), Images.copy.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			@SuppressWarnings("unchecked")
			GenericItem<TranslationProfile> item = (GenericItem<TranslationProfile>) target;
			TranslationProfileEditor editor;
			
			try
			{
				editor = getProfileEditor(item.getElement());
				editor.setCopyMode();
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorReadData"),
						e);
				return;
			}
			TranslationProfileEditDialog dialog = new TranslationProfileEditDialog(msg, 
					msg.getMessage("TranslationProfilesComponent.copyAction"), 
					new TranslationProfileEditDialog.Callback()
					{
						@Override
						public boolean handleProfile(TranslationProfile profile)
						{
							return addProfile(profile);
						}
					}, editor);
			dialog.show();
		}
	}
	
	private class ExportActionHandler extends SingleActionHandler
	{
		public ExportActionHandler()
		{
			super(msg.getMessage("TranslationProfilesComponent.exportAction"), Images.save.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			SimpleFileDownloader downloader = new SimpleFileDownloader();
			addExtension(downloader);
			@SuppressWarnings("unchecked")
			GenericItem<TranslationProfile> item = (GenericItem<TranslationProfile>) target;
			final StreamResource resource = new StreamResource(() -> {
				return new ByteArrayInputStream(item.getElement().toJsonObject().toString().getBytes());
			}, item.getElement().getName() + ".json");
			
			downloader.setFileDownloadResource(resource);
			downloader.download();
		}
	}
	
	private class DeleteActionHandler extends AbstractTranslationProfileActionHandler
	{
		public DeleteActionHandler()
		{
			super(msg.getMessage("TranslationProfilesComponent.deleteAction"),
					Images.delete.getResource());
			setMultiTarget(true);
		}

		@Override
		public void handleAction(Object sender, Object target)
		{
			final Collection<TranslationProfile> items = getItems(target);
			String confirmText = MessageUtils.createConfirmFromNames(msg, items);
			new ConfirmDialog(msg, msg.getMessage(
					"TranslationProfilesComponent.confirmDelete",
					confirmText), new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					for (TranslationProfile item : items)
					{
						removeProfile(item.getName());
					}

				}
			}).show();
		}
	}
	
	private class WizardActionHandler extends SingleActionHandler
	{
		private TranslationProfileEditDialog.Callback addCallback;
		
		public WizardActionHandler()
		{
			super(msg.getMessage("TranslationProfilesComponent.wizardAction"), Images.wizard.getResource());
			setNeedsTarget(false);
			callback = new SingleActionHandler.ActionButtonCallback() 
			{
				@Override
				public boolean showActionButton() 
				{
					return isInputProfileSelection();
				}
			};
			addCallback = new TranslationProfileEditDialog.Callback()
			{
				@Override
				public boolean handleProfile(TranslationProfile profile)
				{
					return addProfile(profile);
				}
			};			
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			TranslationProfileEditor editor;
			try
			{
				editor = (TranslationProfileEditor) getProfileEditor(null);				
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorReadData"),
						e);
				return;
			}

			ProfileWizardProvider wizardProvider = new ProfileWizardProvider(msg, sandboxURL, 
					sandboxNotifier, editor, addCallback);
			SandboxWizardDialog dialog = new SandboxWizardDialog(wizardProvider.getWizardInstance(),
					wizardProvider.getCaption());
			dialog.show();
		}
	}
	
	private class DryRunActionHandler extends SingleActionHandler
	{
		public DryRunActionHandler()
		{
			super(msg.getMessage("TranslationProfilesComponent.dryrunAction"), Images.dryrun.getResource());
			setNeedsTarget(false);
			callback = new SingleActionHandler.ActionButtonCallback() 
			{
				@Override
				public boolean showActionButton() 
				{
					return isInputProfileSelection();
				}
			};
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			DryRunWizardProvider provider = new DryRunWizardProvider(msg, sandboxURL, sandboxNotifier, 
					profileMan, inputActionsRegistry);
			SandboxWizardDialog dialog = new SandboxWizardDialog(provider.getWizardInstance(),
					provider.getCaption());
			dialog.show();
		}
	}
	
	private abstract class AbstractTranslationProfileActionHandler extends SingleActionHandler
	{

		public AbstractTranslationProfileActionHandler(String caption, Resource icon)
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
					Collection<TranslationProfile> items = getItems(target);
					for (TranslationProfile tp : items)
						if (tp.getProfileMode() == ProfileMode.READ_ONLY)
							return EMPTY;
				} else
				{
					GenericItem<?> item = (GenericItem<?>) target;	
					TranslationProfile tp = (TranslationProfile) item.getElement();
					if (tp.getProfileMode() == ProfileMode.READ_ONLY)
						return EMPTY;
				}
			}
			return super.getActions(target, sender);
		}

	}
	
	private boolean isInputProfileSelection()
	{
		boolean isInputProfile = false;
		if (profileType != null) 
		{
			ProfileType pt = (ProfileType) profileType.getValue();
			if (pt == ProfileType.INPUT) 
			{
				isInputProfile = true;
			}
		}
		return isInputProfile;		
	}

	public void setSandboxNotifier(SandboxAuthnNotifier sandboxNotifier) 
	{
		this.sandboxNotifier = sandboxNotifier;
	}
}
