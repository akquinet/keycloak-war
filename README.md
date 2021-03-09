# keycloak-war

Deploy KeyCloak as WAR in your WildFly server. By default creates demo-realm with users and installs admin user.
Data is kept in H2 database ${jboss.server.data.dir}/keycloak.db

Just copy the WAR into the deployments/ directory of your WildFly/EAP server and start.
Then point your browser to

      http://localhost:8080/auth/admin/master/console/

and login as admin.

## Users

Admin user is kept in file META-INF/keycloak-add-user.json which is copied at startup of the WAR to ${jboss.server.config.dir}/

Username: admin
Password: RcSTU63zAMkpUqhGGuSCmU9wIbXuDSpv+

# Export realm

1. Start Server

   ```./standalone.sh -Dkeycloak.connectionsJpa.url='jdbc:h2:${jboss.server.data.dir}/keycloak'```   
      
2. Configure realm in running KeyCloak and STOP server via Control-C or kill

3. Copy set-export-properties.cli to bin/ directory, edit and run it

   ```sh jboss-cli.sh --file=set-export-properties.cli```

4. Run KeyCloak/WildFly again

    ```./standalone.sh -Dkeycloak.connectionsJpa.url='jdbc:h2:${jboss.server.data.dir}/keycloak'```

After export is done SHUTDOWN the process via Control-C or kill

2. Unset properties via CLI

   ```sh jboss-cli.sh --file=unset-export-properties.cli```

# Run with volatile in-memory database

If you do not want to keep data between restarts use a in-memory data base

   ```./standalone.sh -Dkeycloak.connectionsJpa.url='jdbc:h2:mem:keycloak;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'```

