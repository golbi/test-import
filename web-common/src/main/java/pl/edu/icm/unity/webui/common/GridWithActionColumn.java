/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.components.grid.DetailsGenerator;
import com.vaadin.ui.components.grid.GridRowDragger;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * Grid with actions column.
 * 
 * @author P.Piernik
 *
 * @param <T>
 */
public class GridWithActionColumn<T> extends Grid<T>
{
	private UnityMessageSource msg;
	private List<T> contents;
	private ListDataProvider<T> dataProvider;
	private Column<T, HorizontalLayout> actionColumn;

	private List<SingleActionHandler<T>> actionHandlers;
	private List<SingleActionHandler<T>> hamburgerActionHandlers;
	private boolean heightByRows;
	
	public GridWithActionColumn(UnityMessageSource msg, List<SingleActionHandler<T>> actionHandlers)
	{
		this(msg, actionHandlers, true, true);
	}
	
	public GridWithActionColumn(UnityMessageSource msg, List<SingleActionHandler<T>> actionHandlers, boolean enableDrag)
	{
		this(msg, actionHandlers, enableDrag, true);
	}

	public GridWithActionColumn(UnityMessageSource msg, List<SingleActionHandler<T>> actionHandlers,
			boolean enableDrag, boolean heightByRows)
	{
		this.msg = msg;
		this.actionHandlers = actionHandlers;
		this.hamburgerActionHandlers = new ArrayList<>();
		this.heightByRows = heightByRows;
		
		contents = new ArrayList<>();
		dataProvider = DataProvider.ofCollection(contents);
		setDataProvider(dataProvider);
		setSizeFull();
		
		refreshActionColumn();
		if (enableDrag)
		{
			new GridRowDragger<>(this);
		}

		setSelectionMode(SelectionMode.NONE);
		setStyleName("u-gridWithAction");
		refreshHeight();
	}

	public void setMultiSelect(boolean multi)
	{
		setSelectionMode(multi ? SelectionMode.MULTI : SelectionMode.SINGLE);
		GridSelectionSupport.installClickListener(this);
		if (multi)
		{
			addStyleName("u-gridWithActionMulti");
		}else
		{
			removeStyleName("u-gridWithActionMulti");
		}
	}
	
	public void replaceElement(T old, T newElement)
	{
		contents.set(contents.indexOf(old), newElement);
		dataProvider.refreshItem(newElement);
		deselectAll();
	}

	public void addElement(T el)
	{
		contents.add(el);
		dataProvider.refreshAll();
		deselectAll();
		refreshHeight();
	}

	@Override
	public void setItems(Collection<T> items)
	{
		contents.clear();
		if (items != null)
		{
			contents.addAll(items);
		}

		dataProvider.refreshAll();
		deselectAll();
		refreshHeight();
	}

	public List<T> getElements()
	{
		return contents;
	}

	public void removeElement(T el)
	{
		contents.remove(el);
		dataProvider.refreshAll();
		deselectAll();
		refreshHeight();
	}

	public void setHeightByRows(boolean byRow)
	{
		heightByRows = byRow;
		refreshHeight();
	}
	
	private void refreshHeight()
	{
		if (heightByRows)
		{
			setHeightByRows(contents.size() > 2 ? contents.size() : 2);
		}
	}

	public GridWithActionColumn<T> addColumn(ValueProvider<T, String> valueProvider, String caption,
			int expandRatio)
	{
		addColumn(valueProvider).setCaption(caption).setExpandRatio(expandRatio).setResizable(false)
				.setSortable(false);
		refreshActionColumn();
		return this;
	}

	public GridWithActionColumn<T> addComponentColumn(ValueProvider<T, Component> valueProvider, String caption,
			int expandRatio)
	{
		addComponentColumn(valueProvider).setCaption(caption).setExpandRatio(expandRatio).setResizable(false)
				.setSortable(false);
		refreshActionColumn();
		return this;
	}

	public void addActionHandler(SingleActionHandler<T> actionHandler)
	{
		actionHandlers.add(actionHandler);
		refreshActionColumn();
	}

	public void addDetailsComponent(DetailsGenerator<T> generator)
	{
		setDetailsGenerator(generator);
		addItemClickListener(e -> {
			setDetailsVisible(e.getItem(), !isDetailsVisible(e.getItem()));
		});
	}

	public void refreshActionColumn()
	{
		if (actionColumn != null)
		{
			removeColumn(actionColumn);
		}

		actionColumn = addComponentColumn(t -> getButtonComponent(new HashSet<>(Arrays.asList(t))))
				.setCaption(msg.getMessage("actions"));
		actionColumn.setResizable(false);
		actionColumn.setExpandRatio(0);
		actionColumn.setSortable(false);

	}
	
	public void addHamburgerActions(List<SingleActionHandler<T>> handlers)
	{	
		handlers.forEach(h -> this.hamburgerActionHandlers.add(h));
		refreshActionColumn();
	}

	private HorizontalLayout getButtonComponent(Set<T> target)
	{
		HorizontalLayout actions = new HorizontalLayout();
		actions.setMargin(false);
		actions.setSpacing(false);
		actions.setWidth(100, Unit.PERCENTAGE);

		for (SingleActionHandler<T> handler : actionHandlers)
		{
			Button actionButton = new Button();
			actionButton.setStyleName(Styles.vButtonSmall.toString());
			actionButton.setIcon(handler.getIcon());
			actionButton.setDescription(handler.getCaption());
			actionButton.addClickListener(e -> handler.handle(target));
			actionButton.setEnabled(handler.isEnabled(target));
			actions.addComponent(actionButton);
			actions.setComponentAlignment(actionButton, Alignment.TOP_LEFT);
		}
		if (hamburgerActionHandlers != null && !hamburgerActionHandlers.isEmpty())
		{	HamburgerMenu<T> menu = new HamburgerMenu<T>();
			menu.setTarget(target);
			menu.addActionHandlers(hamburgerActionHandlers);
			menu.addStyleName(SidebarStyles.sidebar.toString());
			actions.addComponent(menu);
			actions.setComponentAlignment(menu, Alignment.TOP_LEFT);
		}
		
		return actions;
	}

}