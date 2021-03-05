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
}
