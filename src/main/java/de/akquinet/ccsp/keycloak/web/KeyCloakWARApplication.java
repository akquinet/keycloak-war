package de.akquinet.ccsp.keycloak.web;

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
  public static final String REALM_JSON = "keycloak-add-realm.json";
  public static final String CONFIG_DIR = System.getProperty("jboss.server.config.dir", ".");

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
      copyFileToConfigDirectory(USER_JSON);

      final String realmFile = copyFileToConfigDirectory(REALM_JSON);

      // See importRealms()
      if (realmFile != null)
      {
        System.setProperty("keycloak.import", realmFile);
      }
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

  /**
   * Every import/add method shows a different behaviour:
   * <p>
   * - keycloak-server.json, e.g. may be located in META-INF/
   * - keycloak-add-user.json, MUST be located in ${jboss.server.config.dir}
   * - Import of realm may be located anywhere, but has to mentioned in System property "keycloak.import"
   * <p>
   * Thus we have to copy the data from META-INF/ in the WAR to the expected location.
   *
   * @param fileName Name of the (JSON) file in config directory and META-INF, respectively
   * @return path to the copied file
   */
  protected String copyFileToConfigDirectory(final String fileName) throws Exception
  {
    final File file = new File(CONFIG_DIR + File.separator + fileName);

    try (final InputStream source = Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/" + fileName);
        final OutputStream target = new FileOutputStream(file))
    {
      if (source != null)
      {
        final int bytes = IOUtils.copy(source, target);

        LOG.info("Copied " + bytes + " bytes to " + file);
        return file.getAbsolutePath();
      }
    }

    return null;
  }
}
