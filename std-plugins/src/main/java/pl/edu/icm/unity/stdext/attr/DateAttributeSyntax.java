/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Date attribute sytax. Accept date in various formats
 * 
 * @author P.Piernik
 *
 */
public class DateAttributeSyntax implements AttributeValueSyntax<LocalDate>
{
	public static final String ID = "date";
	public static List<String> acceptableFormats = Arrays.asList("yyyy-MM-dd", "dd-MM-yyyy",
			"ddMMyy", "dd.MM.yyyy", "ddMMyyyy", "dd/MM/yyyy");

	@Override
	public String getValueSyntaxId()
	{
		return ID;
	}

	@Override
	public JsonNode getSerializedConfiguration()
	{
		return Constants.MAPPER.createObjectNode();
	}

	@Override
	public void setSerializedConfiguration(JsonNode json)
	{
		// OK
	}

	@Override
	public void validate(LocalDate value) throws IllegalAttributeValueException
	{
		if (value == null)
			throw new IllegalAttributeValueException("null value is illegal");

	}

	@Override
	public boolean areEqual(LocalDate value, Object another)
	{
		return value == null ? null == another : value.equals(another);
	}

	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	@Override
	public LocalDate convertFromString(String stringRepresentation)
	{
		for (String format : acceptableFormats)
		{
			try
			{
				LocalDate date = LocalDate.parse(stringRepresentation,
						DateTimeFormatter.ofPattern(format));
				return date;

			} catch (Exception e)
			{
				// OK
			}
		}

		throw new InternalException("Can not parse date " + stringRepresentation
				+ " using standart date formats");
	}

	@Override
	public String convertToString(LocalDate value)
	{
		return value.format(DateTimeFormatter.ISO_LOCAL_DATE);
	}

	@Override
	public boolean isVerifiable()
	{
		return false;
	}

	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<LocalDate>
	{
		public Factory()
		{
			super(DateAttributeSyntax.ID, DateAttributeSyntax::new);
		}
	}

}
