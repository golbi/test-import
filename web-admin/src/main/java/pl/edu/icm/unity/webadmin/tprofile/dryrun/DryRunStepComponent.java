/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.dryrun;

import pl.edu.icm.unity.sandbox.SandboxAuthnResultEvent;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.MappingResultComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.annotations.AutoGenerated;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * UI Component used by {@link DryRunStep}.
 * 
 * @author Roman Krysinski
 */
public class DryRunStepComponent extends CustomComponent 
{

	/*- VaadinEditorProperties={"grid":"RegularGrid,20","showGrid":true,"snapToGrid":true,"snapToObject":true,"movingGuides":false,"snappingDistance":10} */

	@AutoGenerated
	private VerticalLayout mainLayout;
	@AutoGenerated
	private VerticalLayout resultWrapper;
	@AutoGenerated
	private Label capturedLogs;
	@AutoGenerated
	private Label logsLabel;
	@AutoGenerated
	private Hr hr_2;
	@AutoGenerated
	private VerticalLayout mappingResultWrap;
	@AutoGenerated
	private Hr hr_1;
	@AutoGenerated
	private VerticalLayout remoteIdpWrap;
	@AutoGenerated
	private Hr hr_3;
	@AutoGenerated
	private Label authnResultLabel;
	@AutoGenerated
	private VerticalLayout progressWrapper;
	private UnityMessageSource msg;
	private MappingResultComponent mappingResult;
	private RemotelyAuthenticatedInputComponent remoteIdpInput;
	/**
	 * The constructor should first build the main layout, set the
	 * composition root and then do any custom initialization.
	 *
	 * The constructor will not be automatically regenerated by the
	 * visual editor.
	 * @param msg 
	 * @param sandboxURL 
	 */
	public DryRunStepComponent(UnityMessageSource msg, String sandboxURL) 
	{
		buildMainLayout();
		setCompositionRoot(mainLayout);

		this.msg = msg;
		
		capturedLogs.setContentMode(ContentMode.PREFORMATTED);
		capturedLogs.setValue("");
		
		logsLabel.setValue("");
		logsLabel.setContentMode(ContentMode.HTML);
		
		mappingResult = new MappingResultComponent(msg);
		mappingResultWrap.addComponent(mappingResult);
		
		remoteIdpInput = new RemotelyAuthenticatedInputComponent(msg);
		remoteIdpWrap.addComponent(remoteIdpInput);
		
		progressWrapper.addComponent(new Image("", Images.loader.getResource()));
		indicateProgress();
	}

	public void handle(SandboxAuthnResultEvent event) 
	{
		if (event.getAuthnResult().getStatus() == Status.success)
		{
			authnResultLabel.setValue(msg.getMessage("DryRun.DryRunStepComponent.authnResultLabel.success"));
			authnResultLabel.setStyleName(Styles.success.toString());
		} else
		{
			authnResultLabel.setValue(msg.getMessage("DryRun.DryRunStepComponent.authnResultLabel.error"));
			authnResultLabel.setStyleName(Styles.error.toString());
		}
		logsLabel.setValue(msg.getMessage("DryRun.DryRunStepComponent.logsLabel"));
		RemotelyAuthenticatedContext remoteAuthnContext = event.getAuthnResult().getRemoteAuthnContext();
		if (remoteAuthnContext != null)
		{
			remoteIdpInput.displayAuthnInput(remoteAuthnContext.getAuthnInput());
			mappingResult.displayMappingResult(remoteAuthnContext.getMappingResult(), 
				remoteAuthnContext.getInputTranslationProfile());
			capturedLogs.setValue(event.getCapturedLogs().toString());
		}
		hideProgressShowResult();
	}

	public void indicateProgress()
	{
		resultWrapper.setVisible(false);
		progressWrapper.setVisible(true);
	}
	
	private void hideProgressShowResult()
	{
		progressWrapper.setVisible(false);
		resultWrapper.setVisible(true);
	}
	
	@AutoGenerated
	private VerticalLayout buildMainLayout() {
		// common part: create layout
		mainLayout = new VerticalLayout();
		mainLayout.setImmediate(false);
		mainLayout.setWidth("100%");
		mainLayout.setHeight("100%");
		mainLayout.setMargin(true);
		mainLayout.setSpacing(true);
		
		// top-level component properties
		setWidth("100.0%");
		setHeight("100.0%");
		
		// progressWrapper
		progressWrapper = new VerticalLayout();
		progressWrapper.setImmediate(false);
		progressWrapper.setWidth("-1px");
		progressWrapper.setHeight("-1px");
		progressWrapper.setMargin(false);
		mainLayout.addComponent(progressWrapper);
		
		// resultWrapper
		resultWrapper = buildResultWrapper();
		mainLayout.addComponent(resultWrapper);
		
		return mainLayout;
	}

	@AutoGenerated
	private VerticalLayout buildResultWrapper() {
		// common part: create layout
		resultWrapper = new VerticalLayout();
		resultWrapper.setImmediate(false);
		resultWrapper.setWidth("100.0%");
		resultWrapper.setHeight("-1px");
		resultWrapper.setMargin(false);
		resultWrapper.setSpacing(true);
		
		// authnResultLabel
		authnResultLabel = new Label();
		authnResultLabel.setImmediate(false);
		authnResultLabel.setWidth("-1px");
		authnResultLabel.setHeight("-1px");
		authnResultLabel.setValue("Label");
		resultWrapper.addComponent(authnResultLabel);
		
		// hr_3
		hr_3 = new Hr();
		hr_3.setImmediate(false);
		hr_3.setWidth("100.0%");
		hr_3.setHeight("-1px");
		resultWrapper.addComponent(hr_3);
		
		// remoteIdpWrap
		remoteIdpWrap = new VerticalLayout();
		remoteIdpWrap.setImmediate(false);
		remoteIdpWrap.setWidth("-1px");
		remoteIdpWrap.setHeight("-1px");
		remoteIdpWrap.setMargin(false);
		resultWrapper.addComponent(remoteIdpWrap);
		
		// hr_1
		hr_1 = new Hr();
		hr_1.setImmediate(false);
		hr_1.setWidth("100.0%");
		hr_1.setHeight("-1px");
		resultWrapper.addComponent(hr_1);
		
		// mappingResultWrap
		mappingResultWrap = new VerticalLayout();
		mappingResultWrap.setImmediate(false);
		mappingResultWrap.setWidth("-1px");
		mappingResultWrap.setHeight("-1px");
		mappingResultWrap.setMargin(false);
		resultWrapper.addComponent(mappingResultWrap);
		
		// hr_2
		hr_2 = new Hr();
		hr_2.setImmediate(false);
		hr_2.setWidth("100.0%");
		hr_2.setHeight("-1px");
		resultWrapper.addComponent(hr_2);
		
		// logsLabel
		logsLabel = new Label();
		logsLabel.setImmediate(false);
		logsLabel.setWidth("-1px");
		logsLabel.setHeight("-1px");
		logsLabel.setValue("Label");
		resultWrapper.addComponent(logsLabel);
		
		// capturedLogs
		capturedLogs = new Label();
		capturedLogs.setImmediate(false);
		capturedLogs.setWidth("-1px");
		capturedLogs.setHeight("-1px");
		capturedLogs.setValue("Label");
		resultWrapper.addComponent(capturedLogs);
		
		return resultWrapper;
	}

}
