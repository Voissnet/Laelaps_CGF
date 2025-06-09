package net.redvoiss.sms.util;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import java.util.logging.Logger;

public class ApplicationPropertyProducer {
    private Logger logger = Logger.getLogger(ApplicationPropertyProducer.class.getName());

    @Inject
    private PropertyFileResolver fileResolver;

    @Produces
    @ApplicationProperty(name = "")
    public String getPropertyAsString(InjectionPoint injectionPoint) {

        String propertyName = injectionPoint.getAnnotated().getAnnotation(ApplicationProperty.class).name();
        String value = fileResolver.getProperty(propertyName);

        if (value == null || propertyName.trim().length() == 0) {
            throw new IllegalArgumentException(String.format("No property found with name {%s}", propertyName));
        }
        logger.fine(String.format("%s -> %s", propertyName, value));
        return value;
    }

    @Produces
    @ApplicationProperty(name="")
    public Integer getPropertyAsInteger(InjectionPoint injectionPoint) {

        String value = getPropertyAsString(injectionPoint);
        return value == null ? null : Integer.valueOf(value);
    }
}