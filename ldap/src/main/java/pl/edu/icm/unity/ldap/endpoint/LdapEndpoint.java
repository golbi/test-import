/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import eu.unicore.util.configuration.ConfigurationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.endpoint.AbstractWebEndpoint;
import pl.edu.icm.unity.stdext.credential.PasswordVerificator;

import java.io.StringReader;
import java.util.List;
import java.util.Properties;

/**
 * LDAP endpoint exposes a stripped LDAP protocol interface to Unity's database.
 */
public class LdapEndpoint extends AbstractWebEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(LdapEndpoint.class);

    private LdapServerProperties configuration;
    private String infoServletPath;
    private PasswordVerificator credentialVerificator;
    SessionManagement sessionMan;
    AttributesManagement attributesMan;
    IdentitiesManagement identitiesMan;

    public LdapEndpoint(NetworkServer server,
                        String infoServletPath,
                        PasswordVerificator credentialVerificator,
                        SessionManagement sessionMan,
                        AttributesManagement attributesMan,
                        IdentitiesManagement identitiesMan
                        ) {
        super(server);
        this.infoServletPath = infoServletPath;
        this.credentialVerificator = credentialVerificator;
        this.sessionMan = sessionMan;
        this.attributesMan = attributesMan;
        this.identitiesMan = identitiesMan;
    }


	@Override
	protected void setSerializedConfiguration(String serializedState)
	{
		properties = new Properties();
		try
		{
			properties.load(new StringReader(serializedState));
			configuration = new LdapServerProperties(properties);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the the LDAP"
					+ " endpoint's configuration", e);
		}
	}

	@Override
	public ServletContextHandler getServletContextHandler()
	{
        //
        RawPasswordRetrieval rpr = (RawPasswordRetrieval)(authenticators.get(0).getPrimaryAuthenticator());

        //
        startLdapEmbeddedServer(rpr);

        //
        LdapInfoServlet servlet = new LdapInfoServlet();
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath(description.getContextAddress());
        ServletHolder holder = new ServletHolder(servlet);
        context.addServlet(holder, infoServletPath + "/*");
        return context;
	}

	@Override
	public void updateAuthenticationOptions(List<AuthenticationOption> authenticationOptions)
			throws UnsupportedOperationException
	{
	}

    private void startLdapEmbeddedServer(RawPasswordRetrieval rpr) {
        String host = configuration.getValue("host");
        if (null == host || host.isEmpty()) {
            host = httpServer.getAdvertisedAddress().getHost();
        }
        int port = configuration.getIntValue("ldap_port");

        // TODO temporary directory name
        String workDirectory = "ldap-apacheds-configuration";
        LdapApacheDSInterceptor ladi = new LdapApacheDSInterceptor(
            rpr,
            sessionMan,
            this.description.getRealm(),
            attributesMan,
            identitiesMan,
            configuration
        );
        LdapServerFacade ldf = new LdapServerFacade(
            host, port, ladi, "ldap server interface", workDirectory
        );
        ladi.setLdapServerFacade(ldf);
        try {
            ldf.init(false);
            ldf.changeAdminPasswordBeforeStart(
                configuration.getValue("bind_password")
            );
            ldf.start();

        } catch (Exception e) {
            LOG.error("LDAP embedded server failed to start", e);
        }
    }
}