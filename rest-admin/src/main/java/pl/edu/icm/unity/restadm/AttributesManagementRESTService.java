/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.ExternalizedAttribute;
import pl.edu.icm.unity.types.basic.GroupPattern;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Component
class AttributesManagementRESTService
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_REST, RESTAdmin.class);
	
	private final AttributesManagement attributesMan;
	private final AttributeValueConverter valueConverter;

	AttributesManagementRESTService(AttributesManagement attributesMan,
			AttributeValueConverter valueConverter)
	{
		this.attributesMan = attributesMan;
		this.valueConverter = valueConverter;
	}

	List<ExternalizedAttribute> getAttributes(EntityParam entity,
			String group,
			boolean effective,
			String idType,
			boolean includeSimpleValues) throws EngineException, JsonProcessingException
	{
		LOG.debug("getAttributes query for " + entity + " in " + group);
		Collection<AttributeExt> attributes = attributesMan.getAllAttributes(
				entity, effective, group, null, true);

		if (includeSimpleValues)
		{
			return attributes.stream()
					.map(this::createWithSimpleValues)
					.collect(Collectors.toList());
		}

		return attributes.stream()
				.map(ExternalizedAttribute::new)
				.collect(Collectors.toList());
	}

	Map<String, List<ExternalizedAttribute>> getAttributesInGroups(EntityParam entity, boolean effective,
			List<String> groupsPattern) throws EngineException
	{
		LOG.debug("getAttributes query for " + entity + " in " + groupsPattern);
		List<GroupPattern> groupsPathsPatterns = groupsPattern.stream()
			.map(GroupPattern::new)
			.collect(Collectors.toList());

		Collection<AttributeExt> attributes = attributesMan.getAllAttributes(
			entity, effective, groupsPathsPatterns, null, true);

		return attributes.stream()
			.map(ExternalizedAttribute::new)
			.collect(groupingBy(Attribute::getGroupPath, toList()));
	}

	private ExternalizedAttribute createWithSimpleValues(AttributeExt attribute)
	{
		List<String> simpleValues = valueConverter.internalValuesToExternal(attribute.getName(), attribute.getValues());
		return new ExternalizedAttribute(attribute, simpleValues);
	}

	Collection<AttributeExt> getAttributes(EntityParam entityParam, String group, String attribute)
			throws EngineException
	{
		return attributesMan.getAttributes(entityParam, group, attribute);
	}

	void removeAttribute(EntityParam ep, String group, String attribute) throws EngineException
	{
		LOG.debug("removeAttribute " + attribute + " of " + ep + " in " + group);
		attributesMan.removeAttribute(ep, group, attribute);
	}

	void setAttribute(Attribute attributeParam, EntityParam entityParam) throws EngineException
	{
		LOG.debug("setAttribute: " + attributeParam.getName() + " in " + attributeParam.getGroupPath());
		attributesMan.setAttributeSuppressingConfirmation(entityParam, attributeParam);
	}

}
