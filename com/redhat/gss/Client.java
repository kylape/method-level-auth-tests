package com.redhat.gss;

import java.lang.Exception; //HUH?!? why is this necessary?!?
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.jboss.logging.Logger;
import java.util.ArrayList;

public class Client {

  public Logger log = Logger.getLogger("com.redhat.gss.Client");
  private InitialContext ctx = null;

  public enum Role {
    A("UserA"), B("UserB"), NONE("UserC");

    public String user;

    private Role(String user) {
      this.user = user;
    }

    public String getUser() {
      return user;
    }
  }

  public static void main(String[] args) throws Exception {
    System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    System.setProperty("logging.configuration", "file:/home/remote/klape/work/dev/support-examples/jaxws/permitAll.jar/logging.properties");
    Client client = new Client();
    String[] endpoints = {"a","b","c","d"};
    List<Boolean> wsResults = new ArrayList<Boolean>();
    List<Boolean> ejbResults = new ArrayList<Boolean>();
    for(String endpoint : endpoints) {
      wsResults.addAll(client.invokeWebservice(endpoint));
    }
    for(String endpoint : endpoints) {
      ejbResults.addAll(client.invokeEjb(endpoint));
    }
    printResults(wsResults, ejbResults, client.log);
  }

  public static void printResults(List<Boolean> wsResults, List<Boolean> ejbResults, Logger log) {
    StringBuffer buffer = new StringBuffer("WS results:  ");
    for(Boolean result : wsResults) {
      buffer.append(result.booleanValue() ? "1" : "0");
    }
    log.warn(buffer.toString());
    buffer = new StringBuffer("EJB results: ");
    for(Boolean result : ejbResults) {
      buffer.append(result.booleanValue() ? "1" : "0");
    }
    log.warn(buffer.toString());
  }

  public List<Boolean> invokeWebservice(String endpoint) throws Exception {
    URL wsdl = new URL("http://localhost:8080/endpoint/" + endpoint + "?wsdl");
    QName qname = new QName("http://gss.redhat.com/", "SecureEndpoint" + endpoint.toUpperCase() + "Service");
    Service service = Service.create(wsdl, qname);
    SecureEndpoint port = service.getPort(SecureEndpoint.class);

    List<Boolean> results = new ArrayList<Boolean>();
    for(Role role : Role.class.getEnumConstants()) {
      log.info("");
      log.debug("================================================================================");
      log.debug("Invoking webservice " + endpoint.toUpperCase() + " with " + role.getUser());
      log.debug("================================================================================");
      ((BindingProvider)port).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, role.getUser());
      ((BindingProvider)port).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "RedHat13#");
      results.addAll(invokeClient(port, role));
    }
    return results;
  }

  public List<Boolean> invokeEjb(String endpoint) throws Exception {
    if(ctx == null) {
      Properties p = new Properties();
      p.put("remote.connections", "default");
      p.put("remote.connection.default.host", "localhost");
      p.put("remote.connection.default.port", "4447");
      p.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
      p.put("remote.connection.default.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
      p.put("remote.connection.default.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
      p.put("remote.connection.default.username", "klape");
      p.put("remote.connection.default.password", "RedHat13#");
      Hashtable<String, String> env = new Hashtable<String, String>();
      env.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");

      EJBClientConfiguration cc = new PropertiesBasedEJBClientConfiguration(p);
      ContextSelector<EJBClientContext> selector = new ConfigBasedEJBClientContextSelector(cc);
      EJBClientContext.setSelector(selector);
      ctx = new InitialContext(env);
    }

    List<Boolean> results = new ArrayList<Boolean>();
    for(Role role : Role.class.getEnumConstants()) {
      log.info("");
      log.debug("================================================================================");
      log.debug("Invoking EJB " + endpoint.toUpperCase() + " with " + role.getUser());
      log.debug("================================================================================");
      Object obj = ctx.lookup("ejb:/permiatAll//SecureEndpoint" + endpoint.toUpperCase() + "!com.redhat.gss.SecureEndpoint");
      SecureEndpoint ejbObject = (SecureEndpoint) obj;
      results.addAll(invokeClient(ejbObject, role));
    }
    return results;
  }

  public List<Boolean> invokeClient(SecureEndpoint e, Role role) {
    List<Boolean> results = new ArrayList<Boolean>(3);
    if(role == Role.A) {
      log.info("Invoking a(). Expecting success.");
      try {
        e.a();
        log.info("Success");
        results.add(Boolean.TRUE);
      } catch(Exception ex) {
        log.error("Fail");
        results.add(Boolean.FALSE);
      }
      log.info("Invoking b(). Expecting failure.");
      try {
        e.b();
        log.error("Success");
        results.add(Boolean.TRUE);
      } catch(Exception ex) {
        log.info("Fail");
        results.add(Boolean.FALSE);
      }
      log.info("Invoking c(). Expecting success.");
      try {
        e.c();
        log.info("Success");
        results.add(Boolean.TRUE);
      } catch(Exception ex) {
        log.error("Fail");
        results.add(Boolean.FALSE);
      }
    } else if(role == Role.B) {
      log.info("Invoking a(). Expecting failure.");
      try {
        e.a();
        log.error("Success");
        results.add(Boolean.TRUE);
      } catch(Exception ex) {
        log.info("Fail");
        results.add(Boolean.FALSE);
      }
      log.info("Invoking b(). Expecting success.");
      try {
        e.b();
        log.info("Success");
        results.add(Boolean.TRUE);
      } catch(Exception ex) {
        log.error("Fail");
        results.add(Boolean.FALSE);
      }
      log.info("Invoking c(). Expecting success.");
      try {
        e.c();
        log.info("Success");
        results.add(Boolean.TRUE);
      } catch(Exception ex) {
        log.error("Fail");
        results.add(Boolean.FALSE);
      }
    } else if(role == Role.NONE) {
      log.info("Invoking a(). Expecting failure.");
      try {
        e.a();
        log.error("Success");
        results.add(Boolean.TRUE);
      } catch(Exception ex) {
        log.info("Fail");
        results.add(Boolean.FALSE);
      }
      log.info("Invoking b(). Expecting failure.");
      try {
        e.b();
        log.error("Success");
        results.add(Boolean.TRUE);
      } catch(Exception ex) {
        log.info("Fail");
        results.add(Boolean.FALSE);
      }
      log.info("Invoking c(). Expecting success.");
      try {
        e.c();
        log.info("Success");
        results.add(Boolean.TRUE);
      } catch(Exception ex) {
        log.error("Fail");
        results.add(Boolean.FALSE);
      }
    }
    return results;
  }
}
