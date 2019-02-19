/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.settings.pki.cert;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Sets;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.pki.Certificate;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.ActionColumn;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.ActionColumn.Position;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.Column;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Lists all certificates
 * 
 * @author P.Piernik
 *
 */

public class CertificatesComponent extends CustomComponent
{
	private UnityMessageSource msg;
	private CertificatesController certController;
	private ListOfElementsWithActions<Certificate> certList;
	
	public CertificatesComponent(UnityMessageSource msg, CertificatesController controller)
	{
		this.msg = msg;
		this.certController = controller;
		initUI();
	}

	private  void initUI()
	{
		certList = new ListOfElementsWithActions<>(
				Arrays.asList(new Column<>(msg.getMessage("PkiView.certificateNameCaption"),
						c -> StandardButtonsHelper.getLinkButton(c.name, e -> gotoEdit(c)), 2)),
				new ActionColumn<>(msg.getMessage("actions"), getActionsHandlers(), 0, Position.Right));

		certList.setAddSeparatorLine(true);

		for (Certificate cert : getCertificates())
		{
			certList.addEntry(cert);
		}

		VerticalLayout main = new VerticalLayout();
		Label trustedCertCaption = new Label(msg.getMessage("TrustedCertificates.caption"));
		trustedCertCaption.setStyleName(Styles.bold.toString());
		main.addComponent(trustedCertCaption);
		main.addComponent(getButtonsBar());
		main.addComponent(certList);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);
		setCompositionRoot(main);
	}

	private List<SingleActionHandler<Certificate>> getActionsHandlers()
	{
		SingleActionHandler<Certificate> edit = SingleActionHandler.builder4Edit(msg, Certificate.class)
				.withHandler(r -> gotoEdit(r.iterator().next())).build();

		SingleActionHandler<Certificate> remove = SingleActionHandler.builder4Delete(msg, Certificate.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();
		return Arrays.asList(edit, remove);
	}

	private HorizontalLayout getButtonsBar()
	{
		Button newCert = StandardButtonsHelper.build4AddAction(msg, e -> 
			NavigationHelper.goToView(NewCertificateView.VIEW_NAME));
		return StandardButtonsHelper.buildButtonsBar(newCert);
	}

	private Collection<Certificate> getCertificates()
	{
		try
		{
			return certController.getCertificates();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
		return Collections.emptyList();
	}

	private void remove(Certificate cert)
	{
		try
		{
			certController.removeCertificate(cert);
			certList.removeEntry(cert);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	private void tryRemove(Certificate cert)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(cert.name));
		new ConfirmDialog(msg, msg.getMessage("PkiView.confirmDeleteCertificate", confirmText),
				() -> remove(cert)).show();
	}

	private void gotoEdit(Certificate cert)
	{
		NavigationHelper.goToView(EditCertificateView.VIEW_NAME + "/" + CommonViewParam.name.toString() + "="
				+ cert.name);
	}
}