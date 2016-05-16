/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.impl.StorageLimits;
import pl.edu.icm.unity.store.rdbms.GenericRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;


/**
 * RDBMS storage of {@link Attribute}
 * @author K. Benedyczak
 */
@Repository(AttributeRDBMSStore.BEAN)
public class AttributeRDBMSStore extends GenericRDBMSCRUD<StoredAttribute, AttributeBean> 
					implements AttributeDAO
{
	public static final String BEAN = DAO_ID + "rdbms";

	private GroupDAO groupDAO;
	
	@Autowired
	public AttributeRDBMSStore(AttributeRDBMSSerializer dbSerializer, GroupDAO groupDAO)
	{
		super(AttributesMapper.class, dbSerializer, NAME);
		this.groupDAO = groupDAO;
	}

	@Override
	public void updateAttribute(StoredAttribute a)
	{
		AttributesMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesMapper.class);
		AttributeBean toUpdate = jsonSerializer.toDB(a);
		StorageLimits.checkContentsLimit(toUpdate.getContents());

		List<AttributeBean> old = mapper.getAttributes(toUpdate);
		if (old.isEmpty())
			throw new IllegalArgumentException(elementName + " [" + a.getAttribute().getName() + 
					"] does not exist");
		AttributeBean oldSingle = old.get(0);
		preUpdateCheck(oldSingle, a);
		toUpdate.setId(oldSingle.getId());
		mapper.updateByKey(toUpdate);		
	}

	@Override
	public void deleteAttribute(String attribute, long entityId, String group)
	{
		AttributesMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesMapper.class);
		List<AttributeBean> existing = getAttributesFiltering(attribute, entityId, group);
		if (existing.isEmpty())
			throw new IllegalArgumentException(elementName + " [" + attribute + 
					"] does not exist");
		mapper.deleteByKey(existing.get(0).getId());
	}

	@Override
	public void deleteAttributesInGroup(long entityId, String group)
	{
		AttributesMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		AttributeBean param = new AttributeBean();
		param.setEntityId(entityId);
		param.setGroupId(groupId);
		mapper.deleteAttributesInGroup(param);
	}

	@Override
	public List<AttributeExt> getAttributes(String attribute, long entityId, String group)
	{
		List<AttributeBean> existing = getAttributesFiltering(attribute, entityId, group);
		List<AttributeExt> ret = new ArrayList<>(existing.size());
		for (AttributeBean ab: existing)
			ret.add(jsonSerializer.fromDB(ab).getAttribute());
		return ret;
	}
	
	
	private List<AttributeBean> getAttributesFiltering(String attribute, Long entityId, String group)
	{
		AttributesMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesMapper.class);
		AttributeBean param = new AttributeBean();
		param.setEntityId(entityId);
		param.setName(attribute);
		param.setGroup(group);
		return mapper.getAttributes(param);
	}
}
