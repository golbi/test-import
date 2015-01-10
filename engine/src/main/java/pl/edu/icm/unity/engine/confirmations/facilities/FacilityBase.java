/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.types.VerifiableElement;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.ConfirmationData;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Contains methods used in all facilities
 * @author P. Piernik
 *
 */
public abstract class FacilityBase
{

	private boolean confirmSingleElement(VerifiableElement verifiable, String value)
	{
		if (verifiable.getValue().equals(value))
		{
			ConfirmationData confirmationData = verifiable.getConfirmationData();
			confirmationData.setConfirmed(true);
			Date today = new Date();
			confirmationData.setConfirmationDate(today.getTime());
			return true;
		}
		return false;
	}

	
	protected Collection<Attribute<?>> confirmAttribute(Collection<Attribute<?>> attrs, String attrName,
			String group, String value) throws EngineException
	{
		List<Attribute<?>> confirmed = new ArrayList<Attribute<?>>();
		for (Attribute<?> attr : attrs)
		{
			if (attr.getName().equals(attrName) && attr.getGroupPath().equals(group)
					&& attr.getValues() != null)
			{
				for (Object el : attr.getValues())
				{
					if (el instanceof VerifiableElement)
					{
						VerifiableElement verifiable = (VerifiableElement) el;
						if (confirmSingleElement(verifiable, value))
							confirmed.add(attr);
					}
				}

			}
		}
		return confirmed;
	}

	protected Collection<IdentityParam> confirmIdentity(IdentityTypesRegistry identityTypesRegistry,
			Collection<IdentityParam> identities, String type, String value)
			throws EngineException
	{
		

		ArrayList<IdentityParam> confirmed = new ArrayList<IdentityParam>();
		for (IdentityParam id : identities)
		{
			if (id.getTypeId().equals(type) && id.getValue().equals(value))
			{
				ConfirmationData confirmationData = id.getConfirmationData();
				confirmationData.setConfirmed(true);
				Date today = new Date();
				confirmationData.setConfirmationDate(today.getTime());
				confirmed.add(id);
			}
		}
		return confirmed;
	}

}
