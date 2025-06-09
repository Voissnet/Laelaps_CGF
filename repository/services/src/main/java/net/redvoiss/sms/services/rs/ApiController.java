package net.redvoiss.sms.services.rs;

import java.util.HashMap;
import java.util.Map;

import java.io.InputStream;

import java.util.jar.Manifest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import javax.servlet.ServletContext;

import javax.annotation.security.RolesAllowed;

@Path("/")
@javax.enterprise.context.RequestScoped
@RolesAllowed("rsUser")
public class ApiController {
 
    /**
     * Handling GET request to retrieve details from MANIFEST.MF file
     * @return implementation details
	 * @see <a href="http://mariuszprzydatek.com/2013/09/18/displaying-git-build-number-hash-in-your-rest-api/">Displaying GIT build number (hash) in your REST API</a>
     */
    @Path("api") //e.g. curl -u joavila -i -X GET https://sms.lanube.cl:8181/services/rest/api
    @GET
	@RolesAllowed("rsUser")
	@Produces({MediaType.APPLICATION_JSON})
    public Map<String, String> getBuildNumber(@Context SecurityContext sc, @Context ServletContext context) throws Exception {
        java.net.URL url = context.getResource("/META-INF/MANIFEST.MF");
		//System.out.println(String.format("%s", String.valueOf(url.toURI())));		
		InputStream manifestStream = url.openStream();
        Manifest manifest = new Manifest(manifestStream);
 
        Map<String, String> response = new HashMap<>();
        response.put("Implementation-Vendor", manifest.getMainAttributes().getValue("Implementation-Vendor"));
        response.put("Implementation-Title", manifest.getMainAttributes().getValue("Implementation-Title"));
        response.put("Implementation-Version", manifest.getMainAttributes().getValue("Implementation-Version"));
        response.put("Implementation-Jdk", manifest.getMainAttributes().getValue("Build-Jdk"));
        response.put("Implementation-Build", manifest.getMainAttributes().getValue("Implementation-Build"));
        response.put("Implementation-Build-Time", manifest.getMainAttributes().getValue("Implementation-Build-Time"));
		response.forEach((k, v) -> System.out.println(k + "=" + v));
        return response;
 
    }
}