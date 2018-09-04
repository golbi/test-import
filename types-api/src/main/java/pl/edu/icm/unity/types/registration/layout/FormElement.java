/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.types.registration.layout;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import pl.edu.icm.unity.MessageSource;

/**
 * Used in {@link FormLayout} to represent a form element being placed.
 * 
 * 
 * @author Krzysztof Benedyczak
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.EXISTING_PROPERTY, property="clazz")
public abstract class FormElement
{
	private String clazz;
	private FormLayoutType type;
	private boolean formContentsRelated;

	public FormElement(FormLayoutType type, boolean formContentsRelated)
	{
		this.type = type;
		this.formContentsRelated = formContentsRelated;
		this.clazz = getClass().getName();
	}

	public FormLayoutType getType()
	{
		return type;
	}

	public boolean isFormContentsRelated()
	{
		return formContentsRelated;
	}

	public String getClazz()
	{
		return clazz;
	}

	@JsonIgnore
	public abstract String toString(MessageSource msg);

	@Override
	public boolean equals(final Object other)
	{
		if (this == other)
			return true;
		if (!(other instanceof FormElement))
			return false;
		FormElement castOther = (FormElement) other;
		return Objects.equals(clazz, castOther.clazz) && Objects.equals(type, castOther.type)
				&& Objects.equals(formContentsRelated, castOther.formContentsRelated);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(clazz, type, formContentsRelated);
	}
}
