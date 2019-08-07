/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.maintenance.audit;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import io.imunity.webadmin.identities.EntityDetailsDialog;
import io.imunity.webadmin.identities.EntityDetailsPanel;
import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.maintenance.MaintenanceNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.engine.api.AuditEventManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.UnknownIdentityException;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.audit.AuditEventAction;
import pl.edu.icm.unity.types.basic.audit.AuditEventType;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.common.safehtml.HtmlSimplifiedLabel;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.join;
import static java.util.Objects.nonNull;

/**
 * Lists AuditEvent object with ability to filter data.
 *
 * @author R.Ledzinski
 *
 */
@PrototypeComponent
class AuditEventsView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "AuditEvents";

	private final UnityMessageSource msg;
	private final AuditEventManagement eventManagement;
	private final EntityManagement entityMan;
	private final EntityManagement idsMan;
	private final ObjectFactory<EntityDetailsPanel> entityDetailsPanelFactory;

	private final Label titleLabel = new Label();
	private final ChipsWithDropdown<String> typeFilter = new ChipsWithDropdown<>(Objects::toString, Objects::toString, true, false);
	private final ChipsWithDropdown<String> actionFilter = new ChipsWithDropdown<>(Objects::toString, Objects::toString, true, false);
	private final ChipsWithDropdown<String> tagsFilter = new ChipsWithDropdown<>(Objects::toString, Objects::toString, true, false);
	private final ChipsWithTextfield searchFilter;
	private final DateTimeField fromFilter;
	private final DateTimeField untilFilter;
	private GridWithActionColumn<AuditEventEntry> auditEventsGrid;

	@Autowired
	AuditEventsView(UnityMessageSource msg, AuditEventManagement eventManagement,
					EntityManagement entityMan, EntityManagement idsMan,
					ObjectFactory<EntityDetailsPanel> entityDetailsPanelFactory)
	{
		this.msg = msg;
		this.eventManagement = eventManagement;
		this.entityMan = entityMan;
		this.idsMan = idsMan;
		this.entityDetailsPanelFactory = entityDetailsPanelFactory;

		this.fromFilter = new DateTimeField(msg.getMessage("AuditEventsView.from"));
		this.untilFilter = new DateTimeField(msg.getMessage("AuditEventsView.until"));
		this.searchFilter = new ChipsWithTextfield(msg, true, false);
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		auditEventsGrid = new GridWithActionColumn<>(msg, Collections.emptyList(), false, false);

		auditEventsGrid.addHamburgerActions(getActionsHandlers());
		auditEventsGrid.addShowDetailsColumn(this::getDetailsComponent);

		auditEventsGrid.addSortableColumn(AuditEventEntry::formatTimestamp,
				msg.getMessage("AuditEventsView.timestamp"), 10)
				.setId("timestamp");

		auditEventsGrid.addSortableColumn(ae -> ae.getEvent().getType().toString(),
				msg.getMessage("AuditEventsView.type"), 10)
				.setHidable(true).setHidden(false).setId("type");

		auditEventsGrid.addSortableColumn(ae -> ae.getEvent().getAction().toString(),
				msg.getMessage("AuditEventsView.action"), 10)
				.setHidable(true).setHidden(false).setId("action");

		auditEventsGrid.addSortableColumn(AuditEventEntry::getName,
				msg.getMessage("AuditEventsView.name"), 10)
				.setHidable(true).setHidden(false).setId("name");

		auditEventsGrid.addSortableColumn(AuditEventEntry::formatTags,
				msg.getMessage("AuditEventsView.tags"), 10)
				.setHidable(true).setHidden(false).setId("tags");

		auditEventsGrid.addColumn(AuditEventEntry::getSubjectId,
				msg.getMessage("AuditEventsView.subjectId"), 10)
				.setSortable(false).setHidable(true).setHidden(true).setId("subject_id");

		auditEventsGrid.addColumn(AuditEventEntry::getSubjectName,
				msg.getMessage("AuditEventsView.subjectName"), 10)
				.setSortable(true).setHidable(true).setHidden(true).setId("subject_name");

		auditEventsGrid.addColumn(AuditEventEntry::getSubjectEmail,
				msg.getMessage("AuditEventsView.subjectEmail"), 10)
				.setSortable(true).setHidable(true).setHidden(true).setId("subject_email");

		auditEventsGrid.addColumn(AuditEventEntry::getInitiatorId,
				msg.getMessage("AuditEventsView.initiatorId"), 10)
				.setSortable(false).setHidable(true).setHidden(true).setId("initiator_id");

		auditEventsGrid.addColumn(AuditEventEntry::getInitiatorName,
				msg.getMessage("AuditEventsView.initiatorName"), 10)
				.setSortable(true).setHidable(true).setHidden(true).setId("initiator_name");

		auditEventsGrid.addColumn(AuditEventEntry::getInitiatorEmail,
				msg.getMessage("AuditEventsView.initiatorEmail"), 10)
				.setSortable(true).setHidable(true).setHidden(true).setId("initiator_email");

		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.addStyleName("u-auditEvents-filterLayout");
		filterLayout.setWidth("100%");

		fromFilter.setWidth("100%");
		untilFilter.setWidth("100%");

		typeFilter.setItems(Arrays.stream(AuditEventType.values()).map(AuditEventType::toString).collect(Collectors.toList()));
		typeFilter.setWidth("100%");
		typeFilter.setMultiSelectable(true);
		typeFilter.setCaption(msg.getMessage("AuditEventsView.type"));

		actionFilter.setItems(Arrays.stream(AuditEventAction.values()).map(AuditEventAction::toString).collect(Collectors.toList()));
		actionFilter.setWidth("100%");
		actionFilter.setMultiSelectable(true);
		actionFilter.setCaption(msg.getMessage("AuditEventsView.action"));

		searchFilter.setWidth("100%");
		searchFilter.setCaption(msg.getMessage("search"));

		tagsFilter.setItems(eventManagement.getAllTags().stream().sorted().collect(Collectors.toList()));
		tagsFilter.setMultiSelectable(true);
		tagsFilter.setWidth("100%");
		tagsFilter.setCaption(msg.getMessage("AuditEventsView.tags"));

		filterLayout.addComponents(fromFilter, untilFilter, typeFilter, actionFilter, tagsFilter, searchFilter);

		auditEventsGrid.addFilter(this::filterData);
		fromFilter.addValueChangeListener(this::refreshDataSet);
		untilFilter.addValueChangeListener(this::refreshDataSet);
		typeFilter.addValueChangeListener(this::refreshGrid);
		actionFilter.addValueChangeListener(this::refreshGrid);
		searchFilter.addValueChangeListener(this::refreshGrid);
		tagsFilter.addValueChangeListener(this::refreshGrid);

		auditEventsGrid.sort("timestamp", SortDirection.DESCENDING);

		VerticalLayout gridWrapper = new VerticalLayout();
		gridWrapper.setMargin(false);
		gridWrapper.setSpacing(false);
		titleLabel.addStyleName("u-AuditEventsGridTitle");
		gridWrapper.addComponents(titleLabel, filterLayout, auditEventsGrid);
		gridWrapper.setExpandRatio(auditEventsGrid, 2);
		gridWrapper.setSizeFull();

		auditEventsGrid.setSizeFull();
		setCompositionRoot(gridWrapper);
		setSizeFull();

		refreshDataSet(null);
	}

	private FormLayout getDetailsComponent(AuditEventEntry ae)
	{
		HtmlSimplifiedLabel subjectLabel = new HtmlSimplifiedLabel();
		subjectLabel.setCaption(msg.getMessage("AuditEventsView.subject") + ":");
		subjectLabel.setValue(ae.getFormattedSubject());
		subjectLabel.setStyleName(Styles.wordWrap.toString());

		HtmlSimplifiedLabel initiatorLabel = new HtmlSimplifiedLabel();
		initiatorLabel.setCaption(msg.getMessage("AuditEventsView.initiator") + ":");
		initiatorLabel.setValue(ae.getFormattedInitiator());
		initiatorLabel.setStyleName(Styles.wordWrap.toString());

		HtmlSimplifiedLabel detailsLabel = new HtmlSimplifiedLabel();
		detailsLabel.setCaption(msg.getMessage("AuditEventsView.details") + ":");
		detailsLabel.setValue(ae.formatDetails());
		detailsLabel.setStyleName(Styles.wordWrap.toString());

		FormLayout wrapper = new FormLayout(subjectLabel, initiatorLabel, detailsLabel);
		wrapper.setWidth(95, Unit.PERCENTAGE);
		return wrapper;
	}

	private Collection<AuditEventEntry> getAuditEvents()
	{
		try
		{
			Date from = null;
			Date until = null;
			if (nonNull(fromFilter.getValue())) {
				from = Date.from(fromFilter.getValue().atZone(ZoneId.systemDefault()).toInstant());
			}
			if (nonNull(untilFilter.getValue())) {
				until = Date.from(untilFilter.getValue().atZone(ZoneId.systemDefault()).toInstant());
			}
			return eventManagement.getAuditEvents(from, until).stream().map(ae -> new AuditEventEntry(msg, ae))
					.collect(Collectors.toList());
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, new ControllerException("Error", e));
		}

		return Collections.emptyList();
	}

	private List<SingleActionHandler<AuditEventEntry>> getActionsHandlers()
	{
		SingleActionHandler<AuditEventEntry> showSubjectDetails = SingleActionHandler.builder(AuditEventEntry.class)
				.withCaption(msg.getMessage("AuditEventsView.showDetails", msg.getMessage("AuditEventsView.subject")))
				.withIcon(Images.userMagnifier.getResource())
				.withHandler(this::showSubjectDetails)
				.withDisabledPredicate(sel->sel.getEvent().getSubject() == null)
				.build();

		SingleActionHandler<AuditEventEntry> showInitiatorDetails = SingleActionHandler.builder(AuditEventEntry.class)
				.withCaption(msg.getMessage("AuditEventsView.showDetails", msg.getMessage("AuditEventsView.initiator")))
				.withIcon(Images.userMagnifier.getResource())
				.withHandler(this::showInitiatorDetails)
				.withDisabledPredicate(sel->sel.getEvent().getInitiator().getEntityId() == 0)
				.build();

		List<SingleActionHandler<AuditEventEntry>> hamburgerHandlers = new ArrayList<>();
		hamburgerHandlers.add(showSubjectDetails);
		hamburgerHandlers.add(showInitiatorDetails);

		return hamburgerHandlers;
	}

	private void showSubjectDetails(Set<AuditEventEntry> selection)
	{
		showEntityDetails(selection.iterator().next().getEvent().getSubject().getEntityId());
	}

	private void showInitiatorDetails(Set<AuditEventEntry> selection)
	{
		showEntityDetails(selection.iterator().next().getEvent().getInitiator().getEntityId());
	}

	private void showEntityDetails(Long entityId)
	{
		EntityParam param = new EntityParam(entityId);
		Collection<GroupMembership> groups = null;
		Entity entity = null;
		String label = null;
		try
		{
			entity = idsMan.getEntity(param);
			label = idsMan.getEntityLabel(param);
			groups = entityMan.getGroups(new EntityParam(entityId)).values();
		} catch (UnknownIdentityException e) {
			NotificationPopup.showNotice("Not found", join(" ", "Cannot display entity details.\nEntity", Long.toString(entityId), "was removed from the system."));
			return;
		} catch (EngineException e)
		{
			NotificationPopup.showError("Error", "Cannot display entity details.");
			return;
		}
		final EntityDetailsPanel identityDetailsPanel = entityDetailsPanelFactory.getObject();
		identityDetailsPanel.setInput(new EntityWithLabel(entity, label), groups);
		new EntityDetailsDialog(msg, identityDetailsPanel).show();
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.maintenance.auditEvents");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class AuditEventsInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		public AuditEventsInfoProvider(UnityMessageSource msg,
											MaintenanceNavigationInfoProvider parent,
											ObjectFactory<AuditEventsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.maintenance.auditEvents"))
					.withIcon(Images.records.getResource())
					.withPosition(20).build());
		}
	}

	private <T> void refreshDataSet(T newValue)
	{
		auditEventsGrid.setItems(getAuditEvents());
		refreshGrid(null);
	}

	private <T> void refreshGrid(T newValue)
	{
		auditEventsGrid.getDataProvider().refreshAll();
		Query<AuditEventEntry, SerializablePredicate<AuditEventEntry>> query = new Query<>(this::filterData);
		titleLabel.setValue(msg.getMessage("AuditEventsView.gridSummary",
				((ListDataProvider<AuditEventEntry>)auditEventsGrid.getDataProvider()).size(query),
				auditEventsGrid.getElements().size()));
	}

	private boolean filterData(AuditEventEntry entry)
	{
		boolean isMatching = true;
		if(!typeFilter.getSelectedItems().isEmpty()) {
			isMatching = isMatching && typeFilter.getSelectedItems().contains(entry.getEvent().getType().toString());
		}
		if(!actionFilter.getSelectedItems().isEmpty()) {
			isMatching = isMatching && actionFilter.getSelectedItems().contains(entry.getEvent().getAction().toString());
		}
		if (!CollectionUtils.isEmpty(searchFilter.getValue())) {
			isMatching = isMatching && searchFilter.getValue().stream().allMatch(s -> entry.anyFieldContains(s, msg));
		}
		if(!tagsFilter.getSelectedItems().isEmpty()) {
			isMatching = isMatching && tagsFilter.getSelectedItems().stream().anyMatch(t->entry.getEvent().getTags().contains(t));
		}
		return isMatching;
	}
}
