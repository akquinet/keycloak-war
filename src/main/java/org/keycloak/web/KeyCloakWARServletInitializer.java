package org.keycloak.web;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.HandlesTypes;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.core.AsynchronousDispatcher;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.keycloak.services.resources.KeycloakApplication;

/**
 * Remove weird restrictions from ResteasyServletInitializer to be operational
 */
@HandlesTypes({ Application.class, Path.class, Provider.class })
public class KeyCloakWARServletInitializer implements ServletContainerInitializer
{
  private static final Set<String> ignoredPackages = new HashSet<>();
  private static final Set<String> ignoredClasses = new HashSet<>();

  static
  {
    ignoredPackages.add(AsynchronousDispatcher.class.getPackage().getName());
    ignoredClasses.add(KeycloakApplication.class.getName());
  }

  @Override
  public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException
  {
    final Set<Class<?>> appClasses = new HashSet<Class<?>>();
    final Set<Class<?>> providers = new HashSet<Class<?>>();
    final Set<Class<?>> resources = new HashSet<Class<?>>();

    for (Class<?> clazz : classes)
    {
      if (clazz.isInterface() || ignored(clazz))
      { continue; }

      if (clazz.isAnnotationPresent(Path.class))
      {
        resources.add(clazz);
      }
      else if (clazz.isAnnotationPresent(Provider.class))
      {
        providers.add(clazz);
      }
      else
      {
        appClasses.add(clazz);
      }
    }

//    for (Class<?> app : appClasses)
//    {
//      register(app, providers, resources, servletContext);
//    }
  }

  private boolean ignored(final Class<?> clazz)
  {
    return ignoredPackages.contains(clazz.getPackage().getName()) || ignoredClasses
        .contains(clazz.getName());
  }

  protected void register(Class<?> applicationClass, Set<Class<?>> providers, Set<Class<?>> resources,
      ServletContext servletContext)
  {
    ApplicationPath path = applicationClass.getAnnotation(ApplicationPath.class);
    if (path == null)
    {
      // todo we don't support this yet, i'm not sure if partial deployments are supported in all servlet containers
      return;
    }
    ServletRegistration.Dynamic reg = servletContext.addServlet(applicationClass.getName(), HttpServlet30Dispatcher.class);
    reg.setLoadOnStartup(1);
    reg.setAsyncSupported(true);
    reg.setInitParameter("javax.ws.rs.Application", applicationClass.getName());

    if (path != null)
    {
      String mapping = path.value();
      if (!mapping.startsWith("/"))
      { mapping = "/" + mapping; }
      String prefix = mapping;
      if (!prefix.equals("/") && prefix.endsWith("/"))
      { prefix = prefix.substring(0, prefix.length() - 1); }
      if (!mapping.endsWith("/*"))
      {
        if (mapping.endsWith("/"))
        { mapping += "*"; }
        else
        { mapping += "/*"; }
      }
      // resteasy.servlet.mapping.prefix
      reg.setInitParameter("resteasy.servlet.mapping.prefix", prefix);
      reg.addMapping(mapping);
    }

    if (resources.size() > 0)
    {
      StringBuilder builder = new StringBuilder();
      boolean first = true;
      for (Class resource : resources)
      {
        if (first)
        {
          first = false;
        }
        else
        {
          builder.append(",");
        }

        builder.append(resource.getName());
      }
      reg.setInitParameter(ResteasyContextParameters.RESTEASY_SCANNED_RESOURCES, builder.toString());
    }
    if (providers.size() > 0)
    {
      StringBuilder builder = new StringBuilder();
      boolean first = true;
      for (Class provider : providers)
      {
        if (first)
        {
          first = false;
        }
        else
        {
          builder.append(",");
        }
        builder.append(provider.getName());
      }

      reg.setInitParameter(ResteasyContextParameters.RESTEASY_SCANNED_PROVIDERS, builder.toString());
    }
  }
}
