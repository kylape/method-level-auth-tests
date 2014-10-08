package com.redhat.gss;

import javax.ejb.Stateless;
import javax.ejb.Remote;
import javax.jws.WebService;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import org.jboss.ws.api.annotation.WebContext;
import org.jboss.security.annotation.SecurityDomain;
import javax.annotation.security.DeclareRoles;

@Stateless
@Remote(SecureEndpoint.class)
@WebService(endpointInterface="com.redhat.gss.SecureEndpoint")
@SecurityDomain("other")
// @DeclareRoles({"a","b"})
@WebContext(contextRoot="/endpoint",urlPattern="/c",authMethod="BASIC")
@PermitAll
public class SecureEndpointC implements SecureEndpoint {
  @RolesAllowed({"a"})
  public String a() {
    return "Success";
  }

  @RolesAllowed({"b"})
  public String b() {
    return "Success";
  }

  @PermitAll
  public String c() {
    return "Success";
  }
}
