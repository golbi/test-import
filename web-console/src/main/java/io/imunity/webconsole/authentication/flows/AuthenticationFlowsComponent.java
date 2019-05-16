/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.flows;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Sets;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Shows all authentication flows
 * @author P.Piernik
 *
 */
public class AuthenticationFlowsComponent extends CustomComponent
{
	private AuthenticationFlowsController flowsMan;
	private UnityMessageSource msg;
	private GridWithActionColumn<AuthenticationFlowEntry> flowsList;

	public AuthenticationFlowsComponent(UnityMessageSource msg, AuthenticationFlowsController flowsMan)
	{
		this.msg = msg;
		this.flowsMan = flowsMan;
		initUI();
	}

	private void initUI()
	{
		HorizontalLayout buttonsBar = StandardButtonsHelper
				.buildTopButtonsBar(StandardButtonsHelper.build4AddAction(msg,
						e -> NavigationHelper.goToView(NewAuthenticationFlowView.VIEW_NAME)));

	
		flowsList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
		flowsList.addComponentColumn(
				f -> StandardButtonsHelper.buildLinkButton(f.flow.getName(), e -> gotoEdit(f)),
				msg.getMessage("AuthenticationFlowsComponent.nameCaption"), 10);
		flowsList.addDetailsComponent(flow -> {
			{
				Label endpoints = new Label();
				endpoints.setCaption(msg.getMessage("AuthenticationFlowsComponent.endpointsCaption"));
				endpoints.setValue(String.join(", ", flow.endpoints));
				FormLayout wrapper = new FormLayout(endpoints);
				endpoints.setStyleName(Styles.wordWrap.toString());
				wrapper.setWidth(95, Unit.PERCENTAGE);
				return wrapper;
			}
		});

		flowsList.setItems(getFlows());
		
		VerticalLayout main = new VerticalLayout();
		Label flowCaption = new Label(msg.getMessage("AuthenticationFlowsComponent.caption"));
		flowCaption.setStyleName(Styles.bold.toString());
		main.addComponent(flowCaption);
		main.addComponent(buttonsBar);
		main.addComponent(flowsList);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);
		setCompositionRoot(main);
	}

	private List<SingleActionHandler<AuthenticationFlowEntry>> getActionsHandlers()
	{
		SingleActionHandler<AuthenticationFlowEntry> edit = SingleActionHandler
				.builder4Edit(msg, AuthenticationFlowEntry.class)
				.withHandler(r -> gotoEdit(r.iterator().next())).build();

		SingleActionHandler<AuthenticationFlowEntry> remove = SingleActionHandler
				.builder4Delete(msg, AuthenticationFlowEntry.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();

		return Arrays.asList(edit, remove);

	}

	private void gotoEdit(AuthenticationFlowEntry e)
	{
		NavigationHelper.goToView(EditAuthenticationFlowView.VIEW_NAME + "/" + CommonViewParam.name.toString()
				+ "=" + e.flow.getName());
	}

	private Collection<AuthenticationFlowEntry> getFlows()
	{
		try
		{
			return flowsMan.getFlows();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
		return Collections.emptyList();
	}

	private void remove(AuthenticationFlowEntry flow)
	{
		try
		{
			flowsMan.removeFlow(flow.flow);
			flowsList.removeElement(flow);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	private void tryRemove(AuthenticationFlowEntry flow)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(flow.flow.getName()));
		new ConfirmDialog(msg, msg.getMessage("AuthenticationFlowsComponent.confirmDelete", confirmText),
				() -> remove(flow)).show();
	}
}
