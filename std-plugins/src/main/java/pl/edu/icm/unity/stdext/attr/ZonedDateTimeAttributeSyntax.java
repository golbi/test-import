/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.attr;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.attributes.AbstractAttributeValueSyntaxFactory;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * DateTime attribute sytax. Accept datetime in various formats
 * 
 * @author P.Piernik
 *
 */
public class ZonedDateTimeAttributeSyntax implements AttributeValueSyntax<ZonedDateTime>
{
	public static final String ID = "zonedDatetime";
	public static List<String> acceptableFormats = 
			pl.edu.icm.unity.stdext.attr.DateTimeAttributeSyntax.acceptableFormats.stream()
			.map(f -> f + "xxx['['VV']']").collect(Collectors.toList());	

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
	public void validate(ZonedDateTime value) throws IllegalAttributeValueException
	{
		if (value == null)
			throw new IllegalAttributeValueException("null value is illegal");

	}

	@Override
	public boolean areEqual(ZonedDateTime value, Object another)
	{
		return value == null ? null == another : value.equals(another);
	}

	@Override
	public int hashCode(Object value)
	{
		return value.hashCode();
	}

	@Override
	public ZonedDateTime convertFromString(String stringRepresentation)
	{
		for (String format : acceptableFormats)
		{
			try
			{
				ZonedDateTime date = ZonedDateTime.parse(stringRepresentation,
						DateTimeFormatter.ofPattern(format));

				return date;

			} catch (Exception e)
			{
				// OK
			}
		}

		throw new InternalException("Can not parse zoned datetime " + stringRepresentation
				+ " using standart datetime formats");
	}

	@Override
	public String convertToString(ZonedDateTime value)
	{
		return value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
	}

	@Override
	public boolean isVerifiable()
	{
		return false;
	}

	@Component
	public static class Factory extends AbstractAttributeValueSyntaxFactory<ZonedDateTime>
	{
		public Factory()
		{
			super(ZonedDateTimeAttributeSyntax.ID, ZonedDateTimeAttributeSyntax::new);
		}
	}

}
