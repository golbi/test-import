/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.Date;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;

/**
 * Applies scheduled operations on entities: removes them or disables.
 * @author K. Benedyczak
 */
@Component
public class EntitiesScheduledUpdater
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, EntitiesScheduledUpdater.class);
	private UnityServerConfiguration config;
	private DBIdentities dbIdentities;
	private DBSessionManager db;
	
	@Autowired
	public EntitiesScheduledUpdater(UnityServerConfiguration config, DBIdentities dbIdentities,
			DBSessionManager db)
	{
		this.config = config;
		this.dbIdentities = dbIdentities;
		this.db = db;
	}
	
	public Date updateEntities()
	{
		log.debug("Performing scheduled operations on entities");
		SqlSession sql = db.getSqlSession(true);
		Date ret;
		try
		{
			ret = dbIdentities.performScheduledEntityOperations(sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		
		long maxAsyncWait = config.getIntValue(UnityServerConfiguration.UPDATE_INTERVAL) * 1000;
		Date maxWait = new Date(System.currentTimeMillis() + maxAsyncWait);
		Date finalRet = ret.after(maxWait) ? maxWait : ret;
		log.debug("Scheduled operations on entities executed, next round scheduled at " + finalRet);
		return finalRet;
	}
}
