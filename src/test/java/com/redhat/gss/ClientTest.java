package com.redhat.gss;

import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.UrlAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(Arquillian.class)
public class ClientTest {

  private static final String[] endpoints = {"a","b","c","d","e"};
  // private static final String[] endpoints = {"e"};
  private static Logger log = Logger.getLogger("com.redhat.gss.ClientTest");
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

  @Deployment
  public static JavaArchive createDeployment() {
    return ShrinkWrap.create(JavaArchive.class, "endpoint.jar")
      .addClass(SecureEndpoint.class)
      .addClass(SecureEndpointA.class)
      .addClass(SecureEndpointB.class)
      .addClass(SecureEndpointC.class)
      .addClass(SecureEndpointD.class)
      .addClass(SecureEndpointE.class)
      .add(new UrlAsset(ClientTest.class.getResource("/jboss-deployment-structure.xml")), "/META-INF/jboss-deployment-structure.xml");
  }

  public static void main(String[] args) throws Exception {
    System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    System.setProperty("logging.configuration", "file:/home/remote/klape/work/dev/maven-projects/method-level-auth-tests/logging.properties");
    new ClientTest().test();
  }

  @Test
  public void test() throws Exception {
    List<Boolean> wsResults = new ArrayList<Boolean>();
    List<Boolean> ejbResults = new ArrayList<Boolean>();
    for(String endpoint : endpoints) {
      wsResults.addAll(invokeWebservice(endpoint));
    }
    for(String endpoint : endpoints) {
      ejbResults.addAll(invokeEjb(endpoint));
    }
    printResults(wsResults, ejbResults);
  }

  public void printResults(List<Boolean> wsResults, List<Boolean> ejbResults) {
    StringBuffer endpointLine = new StringBuffer();
    StringBuffer roleLine = new StringBuffer();
    StringBuffer methodLine = new StringBuffer();
    for(String endpoint : endpoints) {
      for(int i=0; i<9; i++) {
        endpointLine.append(endpoint.toUpperCase());
      }
      endpointLine.append(" ");
      roleLine.append("AAABBBXXX ");
      methodLine.append("ABCABCABC ");
    }
    log.warn("Endpoint:    " + endpointLine.toString());
    log.warn("Role: X=NONE " + roleLine.toString());
    log.warn("Method:      " + methodLine.toString());
    log.warn("==============================================================");
    StringBuffer buffer = new StringBuffer("WS results:  ");
    for(int i=0; i<wsResults.size(); i++) {
      if(i % 9 == 0 && i > 0) {
        buffer.append(" ");
      }
      Boolean result = wsResults.get(i);
      buffer.append(result.booleanValue() ? "1" : "0");
    }
    log.warn(buffer.toString());
    buffer = new StringBuffer("EJB results: ");
    for(int i=0; i<ejbResults.size(); i++) {
      if(i % 9 == 0 && i > 0) {
        buffer.append(" ");
      }
      Boolean result = ejbResults.get(i);
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
      log.debug("================================================================================");
      log.debug("Invoking webservice " + endpoint.toUpperCase() + " with " + role.getUser());
      log.debug("================================================================================");
      ((BindingProvider)port).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, role.getUser());
      ((BindingProvider)port).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "RedHat13#");
      results.addAll(invokeClient(port, role, endpoint, "b".equals(endpoint) ? false : true));
    }
    return results;
  }

  public List<Boolean> invokeEjb(String endpoint) throws Exception {
    List<Boolean> results = new ArrayList<Boolean>();
    for(Role role : Role.class.getEnumConstants()) {
      log.debug("================================================================================");
      log.debug("Invoking EJB " + endpoint.toUpperCase() + " with " + role.getUser());
      log.debug("================================================================================");

      InitialContext ctx = null;
      try {
        ctx = new InitialContext();
        Object obj = ctx.lookup("java:global/endpoint/SecureEndpoint" + endpoint.toUpperCase() + "!com.redhat.gss.SecureEndpoint");
        SecureEndpoint ejbObject = (SecureEndpoint) obj;
        results.addAll(invokeClient(ejbObject, role, endpoint, "b".equals(endpoint) ? false : true));
      } finally {
        try {
          ctx.close();
        } catch(Exception e) {
        }
      }
    }
    return results;
  }

  public List<Boolean> invokeClient(SecureEndpoint e, Role role, String endpointName, boolean noRoleAllowed) {
    List<Boolean> results = new ArrayList<Boolean>(3);
    Boolean result = null;
    if(role == Role.A) {
      log.debug("Invoking a(). Expecting success.");
      try {
        e.a();
        log.debug("Success");
        result = Boolean.TRUE;
      } catch(Exception ex) {
        log.error("Unexpected Failure");
        result = Boolean.FALSE;
        ex.printStackTrace();
      }
      assertTrue("Unexpected result \"" + result + "\" when invoking SecureEndpoint" + endpointName.toUpperCase() + ".a() with role " + role, result.booleanValue());
      results.add(result);
      log.debug("Invoking b(). Expecting failure.");
      try {
        e.b();
        log.error("Unexpected Success");
        result = Boolean.TRUE;
      } catch(Exception ex) {
        log.debug("Failure");
        result = Boolean.FALSE;
      }
      assertFalse("Unexpected result \"" + result + "\" when invoking SecureEndpoint" + endpointName.toUpperCase() + ".b() with role " + role, result.booleanValue());
      results.add(result);
      invokeMethodC(e, results, endpointName, role, noRoleAllowed);
    } else if(role == Role.B) {
      log.debug("Invoking a(). Expecting failure.");
      try {
        e.a();
        log.error("Unexpected Success");
        result = Boolean.TRUE;
      } catch(Exception ex) {
        log.debug("Failure");
        result = Boolean.FALSE;
      }
      assertFalse("Unexpected result \"" + result + "\" when invoking SecureEndpoint" + endpointName.toUpperCase() + ".a() with role " + role, result.booleanValue());
      results.add(result);
      log.debug("Invoking b(). Expecting success.");
      try {
        e.b();
        log.debug("Success");
        result = Boolean.TRUE;
      } catch(Exception ex) {
        log.error("Unexpected Failure");
        result = Boolean.FALSE;
      }
      assertTrue("Unexpected result \"" + result + "\" when invoking SecureEndpoint" + endpointName.toUpperCase() + ".b() with role " + role, result.booleanValue());
      results.add(result);
      invokeMethodC(e, results, endpointName, role, noRoleAllowed);
    } else if(role == Role.NONE) {
      log.debug("Invoking a(). Expecting failure.");
      try {
        e.a();
        log.error("Unexpected Success");
        result = Boolean.TRUE;
      } catch(Exception ex) {
        log.debug("Failure");
        result = Boolean.FALSE;
      }
      assertFalse("Unexpected result \"" + result + "\" when invoking SecureEndpoint" + endpointName.toUpperCase() + ".a() with role " + role, result.booleanValue());
      results.add(result);
      log.debug("Invoking b(). Expecting failure.");
      try {
        e.b();
        log.error("Unexpected Success");
        result = Boolean.TRUE;
      } catch(Exception ex) {
        log.debug("Failure");
        result = Boolean.FALSE;
      }
      assertFalse("Unexpected result \"" + result + "\" when invoking SecureEndpoint" + endpointName.toUpperCase() + ".b() with role " + role, result.booleanValue());
      results.add(result);
      invokeMethodC(e, results, endpointName, role, noRoleAllowed);
    }
    return results;
  }

  public void invokeMethodC(SecureEndpoint e, List<Boolean> results, String endpointName, Role role, boolean noRoleAllowed) {
    Boolean result = null;
    if(noRoleAllowed) {
      log.debug("Invoking c(). Expecting success.");
      try {
        e.c();
        log.debug("Success");
        result = Boolean.TRUE;
      } catch(Exception ex) {
        log.error("Unexpected Failure");
        result = Boolean.FALSE;
      }
      assertTrue("Unexpected result \"" + result + "\" when invoking SecureEndpoint" + endpointName.toUpperCase() + ".c() with role " + role, result.booleanValue());
    } else {
      log.debug("Invoking c(). Expecting failure.");
      try {
        e.c();
        log.error("Unexpected Success");
        result = Boolean.TRUE;
      } catch(Exception ex) {
        log.debug("Failure");
        result = Boolean.FALSE;
      }
      assertFalse("Unexpected result \"" + result + "\" when invoking SecureEndpoint" + endpointName.toUpperCase() + ".c() with role " + role, result.booleanValue());
    }
    results.add(result);
  }
}
