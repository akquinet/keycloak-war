package org.keycloak.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;

import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.WelcomeResource;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.util.JsonConfigProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton // Needed, if beans.xml is present in deployment
public class KeyCloakWARApplication extends KeycloakApplication
{
  private static final Logger LOG = LoggerFactory.getLogger(KeyCloakWARApplication.class);
  public static final String USER_JSON = "keycloak-add-user.json";

  public KeyCloakWARApplication()
  {
    classes.add(WelcomeResource.class);
    classes.add(RealmsResource.class);
    classes.add(AdminRoot.class);
  }

  @Override
  protected ExportImportManager migrateAndBootstrap()
  {
    try
    {
      copyAddUserFile();
    }
    catch (final Exception exception)
    {
      throw new RuntimeException("migrateAndBootstrap", exception);
    }

    return super.migrateAndBootstrap();
  }

  @Override
  protected void loadConfig()
  {
    final JsonConfigProviderFactory factory = new JsonConfigProviderFactory()
    {
    };

    Config.init(factory.create().orElseThrow(() -> new NoSuchElementException("No value present")));
  }

  private void copyAddUserFile() throws Exception
  {
    // importUser() method expects file exactly here, while keycloak-server, e.g. may be located in META-INF/ as well. Sigh...
    // Thus we have to copy the data to the expected location
    final String configDir = System.getProperty("jboss.server.config.dir");
    final File addUserFile = new File(configDir + File.separator + USER_JSON);

    try (final InputStream source = Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/" + USER_JSON);
        final OutputStream target = new FileOutputStream(addUserFile))
    {
      if (source != null)
      {
        IOUtils.copy(source, target);
      }
    }
  }

/*
  private void createMyRealm()
  {
    final KeycloakSession session = sessionFactory.create();

    try
    {
      session.getTransactionManager().begin();
      final ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);
      final RealmManager manager = new RealmManager(session);
      //      lookupTransaction();

      applianceBootstrap.createMasterRealmUser("bael-admin", "pass");
      final InputStream stream = KeyCloakWARApplication.class.getResourceAsStream("baeldung-realm.json");

      manager.importRealm(JsonSerialization.readValue(stream, RealmRepresentation.class));
      session.getTransactionManager().commit();
    }
    catch (final Exception ex)
    {
      LOG.warn("Couldn't create realm data:", ex);
      session.getTransactionManager().rollback();
    }
    finally
    {
      session.close();
    }
  }

  private void lookupTransaction()
  {
    final JtaTransactionManagerLookup lookup = (JtaTransactionManagerLookup) sessionFactory
        .getProviderFactory(JtaTransactionManagerLookup.class);
    if (lookup != null)
    {
      if (lookup.getTransactionManager() != null)
      {
        try
        {
          final Transaction transaction = lookup.getTransactionManager().getTransaction();

          if (transaction != null)
          {
            LOG.info("bootstrap current transaction status? {}", transaction.getStatus());
          }
        }
        catch (SystemException e)
        {
          throw new RuntimeException(e);
        }
      }
    }
  }*/
}
