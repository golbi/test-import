/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.server.Resource;
import com.vaadin.ui.UI;

import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.upman.common.ServerFaultException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.webui.common.FileStreamResource;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for project management
 * 
 * @author P.Piernik
 *
 */
@Component
public class ProjectController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, ProjectController.class);

	private UnityMessageSource msg;
	private DelegatedGroupManagement delGroupMan;
	private FileStorageService fileStorageService;

	@Autowired
	public ProjectController(UnityMessageSource msg, DelegatedGroupManagement delGroupMan, FileStorageService fileStorageService)
	{
		this.msg = msg;
		this.delGroupMan = delGroupMan;
		this.fileStorageService = fileStorageService;
	}

	Map<String, String> getProjectForUser(long entityId) throws ControllerException
	{

		List<DelegatedGroup> projects;
		try
		{
			projects = delGroupMan.getProjectsForEntity(entityId);
		} catch (Exception e)
		{
			log.debug("Can not get projects for entity " + entityId, e);
			throw new ServerFaultException(msg);
		}

		if (projects.isEmpty())
			throw new ControllerException(
					msg.getMessage("ProjectController.noProjectAvailable"),
					null);

		return projects.stream().collect(Collectors.toMap(p -> p.path, p -> p.displayedName));
	}

	public Resource getProjectLogoSafe(String projectPath)
	{
		Resource logo = Images.logoSmall.getResource();
		DelegatedGroup group;
		try
		{
			group = delGroupMan.getContents(projectPath, projectPath).group;
		} catch (Exception e)
		{
			return logo;
		}
		GroupDelegationConfiguration config = group.delegationConfiguration;
		String logoUrl = config.logoUrl;
		if (logoUrl != null && !logoUrl.isEmpty())
		{
			try
			{
				logo = new FileStreamResource(
						fileStorageService
								.readImageURI(URIHelper.parseURI(logoUrl),
										UI.getCurrent().getTheme())
								.getContents()).getResource();
			} catch (Exception e)
			{
				throw new ConfigurationException("Can not load configured image " + logoUrl, e);
			}
		}

		return logo;
	}
}
