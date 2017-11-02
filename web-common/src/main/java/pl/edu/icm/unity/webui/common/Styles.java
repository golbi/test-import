/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.themes.ValoTheme;

/**
 * General purpose styles defined for VAADIN components
 * @author K. Benedyczak
 */
public enum Styles
{
	emphasized("u-emphasized"),
	bold("u-bold"),
	immutableAttribute("u-immutableAttribute"),
	readOnlyTableElement("u-readOnlyTableElement"),
	captionBold("bold"),
	trueConditionBackground("u-trueCondition-bg"),
	falseConditionBackground("u-falseCondition-bg"),
	errorBackground("u-error-bg"),
	negativeBottomMarginSmall("u-negativeBottomMarginSmall"),
	negativeTopMargin("u-negativeTopMargin"),
	verticalPaddingSmall("u-verticalPaddingSmall"),
	smallMargins("u-smallMargins"),
	imageWidthTiny("u-imageWidthTiny"),
	imageHeightTiny("u-imageHeightTiny"),
	imageWidthSmall("u-imageWidthSmall"),
	imageHeightSmall("u-imageHeightSmall"),
	maxHeightMedium("u-imageMaxHeightMedium"),
	maxHeightSmall("u-imageMaxHeightSmall"),
	maxHeightTiny("u-imageMaxHeightTiny"),
	maxWidthColumn("u-maxWidthColumn"),
	
	bigTabs("u-bigTabs"),
	bigTab("u-bigTab"),
	bigTabSelected("u-bigTabSelected"),
	textCenter("u-textCenter"),
	formSection("u-formSection"),
	messageBox("u-messageBox"),
	error("u-error"),
	success("u-success"),
	textLarge("u-textLarge"),
	textXLarge("u-textXLarge"),
	textEndpointName("u-textEndpointHeading"),
	textSubHeading("u-textHeading2"),
	toolbarButton("u-toolbarButton"),
	verticalLine("u-verticalLine"),
	horizontalLine("u-horizontalLine"),
	header("u-header"),
	selectedButton("u-selectedButton"),
	minHeightAuthn("u-minHeightAuthenticator"),
	smallMargin("u-smallMargin"),
	smallSpacing("u-smallSpacing"),
	tinySpacing("u-tinySpacing"),
	smallFormSpacing("u-smallFormSpacing"),
	hidden("u-hidden"),
	largeTabsheet("u-bigTabsheet"),
	centeredPanel("u-centeredPanel"),
	visibleScroll("u-visiblescroll"),
	verticalAlignmentMiddle("u-vAlignMiddle"),
	floatRight("u-floatRight"),
	horizontalMarginSmall("u-hMarginSmall"),
	rightMargin("u-rightMargin"),
	bottomMargin("u-bottomMargin"),
	margin("u-margin"),
	idpTile("u-idptile"),
	indent("u-indent"),
	link("u-link"),
	
	//Valo
	vPanelLight(ValoTheme.PANEL_BORDERLESS),
	vBorderLess(ValoTheme.TEXTAREA_BORDERLESS),
	vButtonSmall(ValoTheme.BUTTON_SMALL),
	vTabsheetMinimal(ValoTheme.TABSHEET_COMPACT_TABBAR),
	vTabsheetFramed(ValoTheme.TABSHEET_FRAMED),
	vLabelSmall(ValoTheme.LABEL_SMALL),
	vLabelLarge(ValoTheme.LABEL_LARGE),
	vLabelH1(ValoTheme.LABEL_H1),
	vButtonLink(ValoTheme.BUTTON_LINK),
	vButtonPrimary(ValoTheme.BUTTON_PRIMARY),
	vButtonLinkV(ValoTheme.BUTTON_LINK),
	vSmall(ValoTheme.TEXTFIELD_SMALL),
	vTiny(ValoTheme.TEXTFIELD_TINY),
	vTableNoHorizontalLines(ValoTheme.TABLE_NO_HORIZONTAL_LINES),
	vComboSmall(ValoTheme.COMBOBOX_SMALL);

	/**
	 * Number of columns for wider then regular text fields.
	 */
	public static final int WIDE_TEXT_FIELD = 40;
	
	private String value;
	
	private Styles(String value)
	{
		this.value = value;
	}
	
	public String toString()
	{
		return value;
	}
	
	public static String getFlagBgStyleForLocale(String localeCode)
	{
		switch (localeCode)
		{
		case "en":
		case "pl":
		case "de":
			return "u-flag-bg-" + localeCode;
		}
		return null;
	}
}
