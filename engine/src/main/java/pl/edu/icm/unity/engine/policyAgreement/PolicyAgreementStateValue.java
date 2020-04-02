/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.policyAgreement;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.exceptions.EngineException;

public class PolicyAgreementStateValue
{
	public final long policyDocumentId;
	public final int policyDocumentRevision;
	public final PolicyAgreementAcceptanceStatus acceptanceStatus;
	public final Date decisionTs;

	public PolicyAgreementStateValue(@JsonProperty("policyDocumentId") Long policyDocumentId,
			@JsonProperty("policyDocumentRevision") Integer policyDocumentRevision,
			@JsonProperty("acceptanceStatus") PolicyAgreementAcceptanceStatus acceptanceStatus,
			@JsonProperty("decisionTs") Date decisionTs)
	{
		this.policyDocumentId = policyDocumentId;
		this.policyDocumentRevision = policyDocumentRevision;
		this.acceptanceStatus = acceptanceStatus;
		this.decisionTs = decisionTs;
	}

	public String toJson() throws EngineException
	{
		try
		{
			return Constants.MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException e)
		{
			throw new EngineException("Can not save policy agreement state value");
		}
	}

	public static PolicyAgreementStateValue fromJson(String jsonConfig) throws EngineException
	{
		try
		{
			return Constants.MAPPER.readValue(jsonConfig, PolicyAgreementStateValue.class);
		} catch (Exception e)
		{
			throw new EngineException("Can not parse policy agreement state value");
		}
	}
	
	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof PolicyAgreementStateValue))
			return false;
		PolicyAgreementStateValue castOther = (PolicyAgreementStateValue) other;
		return Objects.equals(policyDocumentId, castOther.policyDocumentId)
				&& Objects.equals(policyDocumentRevision, castOther.policyDocumentRevision)
				&& Objects.equals(acceptanceStatus, castOther.acceptanceStatus)
				&& Objects.equals(decisionTs, castOther.decisionTs);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(policyDocumentId, policyDocumentRevision, acceptanceStatus, decisionTs);
	}
}
