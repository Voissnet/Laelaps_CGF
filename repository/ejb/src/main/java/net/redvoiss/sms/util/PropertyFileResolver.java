package net.redvoiss.sms.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import javax.annotation.Resource;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @see http://stackoverflow.com/questions/27953261/wildfly-reading-properties-from-configuration-directory
 * @see http://javahowto.blogspot.cl/2012/02/how-to-create-simple-string-and.html
 */
@Singleton(name = "PropertyFileResolver")
@Startup
public class PropertyFileResolver {

    private Logger logger = Logger.getLogger(PropertyFileResolver.class.getName());
    private Properties properties = new Properties();
    @Resource(lookup = "resource/application.properties")
    String propertyFile;

    @PostConstruct
    private void init() {
        File file = new File(propertyFile);
        
        try {
            properties.load(new FileInputStream(file));
            System.out.println(String.format("Property values are %s", String.valueOf( properties)));
        } catch (IOException e) {
            logger.log(Level.SEVERE ,"Unable to load properties file", e);
        }

    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}