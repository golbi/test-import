/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import pl.edu.icm.unity.types.basic.audit.AuditEvent;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * AuditEvent DAO
 * @author R. Ledzinski
 */
public interface AuditEventDAO extends BasicCRUDDAO<AuditEvent>
{
	String DAO_ID = "AuditEventDAO";
	String NAME = "Audit event";

	Set<String> getAllTags();

	List<AuditEvent> getLogs(final Date from, final Date until);
}
