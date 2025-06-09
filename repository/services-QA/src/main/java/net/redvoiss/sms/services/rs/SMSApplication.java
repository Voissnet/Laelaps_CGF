package net.redvoiss.sms.services.rs;

import java.util.Set;
import java.util.HashSet;

import javax.ws.rs.core.Application;

public class SMSApplication extends Application {

    /**
     * Enrolls REST components and allows to return a map as JSON
     * @return implementation details
	 * @see <a href="http://stackoverflow.com/questions/18926505/jersey-jax-rs-return-a-map-as-xml-json">Jersey/JAX-RS: Return a Map as XML/JSON</a>
     */
    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(SMSResource.class);
        classes.add(ApiController.class);
		try {
            Class jsonProvider = Class.forName("org.glassfish.jersey.jackson.JacksonFeature");
            // Class jsonProvider = Class.forName("org.glassfish.jersey.moxy.json.MoxyJsonFeature");
            // Class jsonProvider = Class.forName("org.glassfish.jersey.jettison.JettisonFeature");
            classes.add(jsonProvider);
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        return classes;
    }
 
    /**
     * Fixes GLASSFISH-21141
     * @return implementation details
	 * @see <a href="https://java.net/jira/browse/GLASSFISH-21141">Missing jackson-module-jaxb-annotations JAR causes error on first Jersey/Jackson JSON response</a>
	 * @see <a href="http://www.trajano.net/2014/10/predictiability-and-versioning-jax-rs-rest-api/">PREDICTIABILITY AND VERSIONING JAX-RS REST API</a>
     */
	@Override
	public Set<Object> getSingletons() {
		final Set<Object> classes = new HashSet<Object>();
	    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
	    mapper.registerModule(new com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule());
		classes.add(new com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider(mapper,
				 com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS));
	    return classes;
	}
}