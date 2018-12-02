/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.upman.members;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.webui.common.SidebarStyles;

/**
 * Group combobox - respects space in item captions
 * @author P.Piernik
 *
 */
class GroupIndentCombo extends ComboBox<String>
{

	public GroupIndentCombo(String caption, Map<String, String> groups)
	{	
		setCaption(caption);
		setStyleName(SidebarStyles.indentComboBox.toString());
		List<String> sortedGroups = groups.keySet().stream().sorted()
				.collect(Collectors.toList());
		setItems(sortedGroups);
		setItemCaptionGenerator(i -> groups.get(i));
		setEmptySelectionAllowed(false);
		setWidth(30, Unit.EM);
		
		if (!sortedGroups.isEmpty())
		{
			setValue(sortedGroups.iterator().next());
		}
	}
}