package org.keycloak.web;

import java.io.InputStream;
import java.util.NoSuchElementException;

import javax.inject.Singleton;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.PublicRealmResource;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.WelcomeResource;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.services.util.JsonConfigProviderFactory;
import org.keycloak.transaction.JtaTransactionManagerLookup;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton // Needed, if beans.xml is present in deployment
public class KeyCloakWARApplication extends KeycloakApplication
{
  private static final Logger LOG = LoggerFactory.getLogger(KeyCloakWARApplication.class);

  public KeyCloakWARApplication()
  {
    classes.add(WelcomeResource.class);
    classes.add(RealmsResource.class);
    classes.add(AdminRoot.class);
  }

  @Override
  protected ExportImportManager migrateAndBootstrap()
  {
    //    createMyRealm();
    return super.migrateAndBootstrap();
  }

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
  }

  protected void loadConfig()
  {
    final JsonConfigProviderFactory factory = new RegularJsonConfigProviderFactory();
    Config.init(factory.create()
        .orElseThrow(() -> new NoSuchElementException("No value present")));
  }
}
