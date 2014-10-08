#Remote EJB vs SLSB JAX-WS Endpoint Authorization

Set `JBOSS_HOME`, start JBoss, run `ant deploy`, and then run `./test.sh`.

Gotta add the users:

```
bin/add-user.sh -a UserA "RedHat13#"
bin/add-user.sh -a UserB "RedHat13#"
bin/add-user.sh -a UserC "RedHat13#"
```

Modify standalone/configuration/application-roles.properties:

```
UserA=a
UserB=b
```

**Note:** EJB tests are *not* working yet.
