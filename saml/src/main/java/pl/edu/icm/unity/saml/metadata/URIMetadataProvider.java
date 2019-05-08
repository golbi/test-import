/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.exceptions.EngineException;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorDocument;

/**
 * Returns metadata read from uri - local file or remote url. The contents is parsed to check syntax. The uri content is automatically 
 * checked for updates.
 * @author  P.Piernik
 */
public class URIMetadataProvider implements MetadataProvider
{
	private Logger log = Log.getLogger(Log.U_SERVER_SAML, URIMetadataProvider.class);
	private Date lastModification;
	private URI uri;
	private FileStorageService fileService;
	private FileData lastLoaded;
	private ScheduledExecutorService scheduler;
	private EntityDescriptorDocument document;
	private boolean stopped = false;
	
	private Runnable task;
	
	public URIMetadataProvider(ExecutorsService executorsService, FileStorageService fileStorageService, String uriRaw) throws EngineException
	{
		this.fileService = fileStorageService;
		this.uri = URIHelper.parseURI(uriRaw);
		scheduler = executorsService.getService();
		load(fileStorageService.readURI(uri, Optional.empty()));
		
		task = () -> {
			reloadTask();
		};
		
		reschedule();
	}

	private synchronized void reloadTask()
	{
		try
		{
			FileData data = fileService.readURI(uri, Optional.empty());
			if (!lastLoaded.equalsContent(data))
			{
				log.info("Metadata file modification detected, reloading " + uri.toString());
				load(data);
			}			
		} catch (EngineException e)
		{
			log.error("Can not load the metadata from the configured uri " + uri.toString(), e);
		}
			
		reschedule();
	}
	
	private synchronized void load(FileData fileData) throws EngineException
	{
		try
		{
			document = EntityDescriptorDocument.Factory.parse(new ByteArrayInputStream(fileData.getContents()));
			lastLoaded = fileData;
			lastModification = new Date();
			log.trace("Load metadata from " + fileData.getName() + ":" + new String(fileData.getContents()));
		} catch (Exception e)
		{
			throw new EngineException("Metadata file can not be loaded", e);
		}
	}
	
	@Override
	public synchronized EntityDescriptorDocument getMetadata()
	{
		return document;
	}

	@Override
	public synchronized Date getLastmodification()
	{
		return lastModification;
	}

	@Override
	public synchronized void stop()
	{
		this.stopped = true;
	}
	
	private synchronized boolean isStopped()
	{
		return stopped ;
	}
	
	private void reschedule()
	{
		if (!isStopped())
			scheduler.schedule(task, 20, TimeUnit.SECONDS);
	}
}
