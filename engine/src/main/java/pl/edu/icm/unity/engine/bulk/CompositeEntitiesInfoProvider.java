/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Stopwatch;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.engine.credential.CredentialRepository;
import pl.edu.icm.unity.engine.credential.CredentialReqRepository;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.generic.AttributeClassDB;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;

@Component
class CompositeEntitiesInfoProvider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, CompositeEntitiesInfoProvider.class);
	@Autowired
	private AttributeTypeDAO attributeTypeDAO;
	@Autowired
	private AttributeDAO attributeDAO;
	@Autowired
	private MembershipDAO membershipDAO;
	@Autowired
	private AttributeClassDB acDB;
	@Autowired
	private GroupDAO groupDAO;
	@Autowired
	private EntityDAO entityDAO;
	@Autowired
	private IdentityDAO identityDAO;
	@Autowired
	private CredentialRepository credentialRepository;
	@Autowired
	private CredentialReqRepository credentialReqRepository;

	
	public GroupMembershipData getCompositeGroupContents(String group) throws EngineException
	{
		Stopwatch watch = Stopwatch.createStarted();
		Map<Long, Set<String>> memberships = getMemberships(group);
		GroupMembershipDataImpl ret = GroupMembershipDataImpl.builder(group)
			.withAttributeTypes(attributeTypeDAO.getAllAsMap())
			.withAttributeClasses(acDB.getAllAsMap())
			.withGroups(groupDAO.getAllAsMap())
			.withCredentials(credentialRepository.getCredentialDefinitions())
			.withMemberships(memberships)
			.withEntityInfo(getEntityInfo(group))
			.withIdentities(getIdentities(memberships.keySet(), group))
			.withDirectAttributes(getAttributes(memberships.keySet(), group))
			.withCredentialRequirements(getCredentialRequirements())
			.build();
		log.debug("Bulk group membership data retrieval: {}", watch.toString());
		return ret;
	}
	
	public GroupStructuralData getGroupStructuralContents(String group) throws EngineException
	{
		Stopwatch watch = Stopwatch.createStarted();
		GroupStructuralDataImpl ret = GroupStructuralDataImpl.builder()
			.withGroups(groupDAO.getAllAsMap())
			.build();
		log.debug("Bulk group structural data retrieval: {}", watch.toString());
		return ret;
	}
	
	private Map<String, CredentialRequirements> getCredentialRequirements() throws EngineException
	{
		return credentialReqRepository.getCredentialRequirements().stream()
			.collect(Collectors.toMap(cr -> cr.getName(), cr -> cr));
	}

	private Map<Long, Map<String, Map<String, AttributeExt>>> getAttributes(Set<Long> entities, String group)
	{
		Stopwatch w = Stopwatch.createStarted();
		List<StoredAttribute> all = attributeDAO.getAttributesOfGroupMembers(group);
		log.debug("getAttrs {}", w.toString());
		Map<Long, Map<String, Map<String, AttributeExt>>> ret = new HashMap<>();
		for (Long member: entities)
			ret.put(member, new HashMap<>());
		all.stream() 
			.forEach(sa -> 
			{
				Map<String, Map<String, AttributeExt>> entityAttrs = ret.get(sa.getEntityId());
				Map<String, AttributeExt> attrsInGroup = entityAttrs.get(sa.getAttribute().getGroupPath());
				if (attrsInGroup == null)
				{
					attrsInGroup = new HashMap<>();
					entityAttrs.put(sa.getAttribute().getGroupPath(), attrsInGroup);
				}
				attrsInGroup.put(sa.getAttribute().getName(), sa.getAttribute());
			});
		return ret;
	}

	private Map<Long, EntityInformation> getEntityInfo(String group)
	{
		return entityDAO.getByGroup(group).stream()
			.collect(Collectors.toMap(entity -> entity.getId(), entity->entity));
	}

	private Map<Long, List<Identity>> getIdentities(Set<Long> entities, String group)
	{
		Stopwatch w = Stopwatch.createStarted();
		List<StoredIdentity> all = identityDAO.getByGroup(group);
		log.debug("getIdentities {}", w.toString());
		Map<Long, List<Identity>> ret = new HashMap<>();
		for (Long member: entities)
			ret.put(member, new ArrayList<>());
		all.stream() 
			.forEach(storedIdentity -> ret.get(storedIdentity.getEntityId()).add(storedIdentity.getIdentity()));
		return ret;
	}

	private Map<Long, Set<String>> getMemberships(String group)
	{
		Stopwatch w = Stopwatch.createStarted();
		List<GroupMembership> all = membershipDAO.getMembers(group);
		log.debug("getMembers {}", w.toString());
		Map<Long, Set<String>> ret = new HashMap<>();
		for (GroupMembership gm: all)
			ret.put(gm.getEntityId(), new HashSet<>());
		all.stream()
			.forEach(membership -> ret.get(membership.getEntityId()).add(membership.getGroup()));
		return ret;
	}
}
