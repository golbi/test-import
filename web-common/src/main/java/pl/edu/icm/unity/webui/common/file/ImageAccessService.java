/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.file;

import java.net.URI;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.server.Resource;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.files.IllegalURIException;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.webui.common.FileStreamResource;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;

@Component
public class ImageAccessService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ImageAccessService.class);
	private static final String UNKNOWN_THEME = "UNKNOWN_THEME";
	
	private final URIAccessService uriAccessService;
	
	@Autowired
	public ImageAccessService(URIAccessService uriAccessService)
	{
		this.uriAccessService = uriAccessService;
	}

	public LocalOrRemoteResource getEditableImageResourceFromUriOrNull(String logoUri, Optional<String> theme)
	{
		if (logoUri == null || logoUri.isEmpty())
			return null;

		URI uri;
		try
		{
			uri = URIHelper.parseURI(logoUri);
		} catch (IllegalURIException e1)
		{
			log.error("Can not parse image uri  " + logoUri);
			return null;
		}

		if (URIHelper.isWebReady(uri))
		{
			return new LocalOrRemoteResource(uri.toString());
		} else
		{
			String rTheme = theme.isPresent() ? theme.get() : UNKNOWN_THEME;
			FileData fileData = null;
			try
			{
				fileData = uriAccessService.readImageURI(uri, rTheme);
			} catch (Exception e)
			{
				log.error("Can not read image from uri: " + logoUri);
			}

			return new LocalOrRemoteResource(fileData != null ?  fileData.getContents() : null, uri.toString());
		}
	}

	public Optional<Resource> getConfiguredImageResourceFromNullableUri(String logoUri)
	{
		if (logoUri == null || logoUri.isEmpty())
			return Optional.empty();

		try
		{
			FileData imageFileData = uriAccessService.readImageURI(URIHelper.parseURI(logoUri), 
					UI.getCurrent().getTheme());
			Resource imageResource = new FileStreamResource(imageFileData.getContents()).getResource();
			return Optional.ofNullable(imageResource);
		} catch (Exception e)
		{
			log.warn("Can not read image from uri: " + logoUri);
			return Optional.empty();
		}
	}
}
