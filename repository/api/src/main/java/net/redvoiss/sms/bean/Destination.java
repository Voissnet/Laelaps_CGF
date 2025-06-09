package net.redvoiss.sms.bean;

import java.util.regex.Pattern;

public class Destination {
    static final Pattern DESTINATION_NUMBER_PATTERN = Pattern.compile("^\\+{0,1}[1-9]\\d*$");//Allows + before the number
    //CGF, 20200911. New rules for validating authorized MSISDN lengths: 4 or >=11
    static final Pattern DESTINATION_NUMBER_PATTERN_1 = Pattern.compile("^\\+{0,1}[1-9]\\d{4}$");
    static final Pattern DESTINATION_NUMBER_PATTERN_2 = Pattern.compile("^\\+{0,1}[1-9]\\d{10}$");
    static final Pattern DESTINATION_NUMBER_PATTERN_3 = Pattern.compile("^\\+{0,1}[1-9]\\d{11}$");
    static final Pattern DESTINATION_NUMBER_PATTERN_4 = Pattern.compile("^\\+{0,1}[1-9]\\d{12}$");
    static final Pattern DESTINATION_NUMBER_PATTERN_5 = Pattern.compile("^\\+{0,1}[1-9]\\d{13}$");
    
    private final String TARGET;
    
    public Destination( String destination )  {
        TARGET = destination; 
    }
    
    public String getTarget() {
        return TARGET.replaceFirst("^\\+","");//Restores original format
    }
    
    public String getScrambledTarget() {
        return TARGET.replaceFirst(".{4}$","XXXX");
    }
    
    public boolean isOK() {
        return DESTINATION_NUMBER_PATTERN.matcher(TARGET).matches();
    }
    
    public boolean isOK_v2() {
        return ( DESTINATION_NUMBER_PATTERN_1.matcher(TARGET).matches() || 
                 DESTINATION_NUMBER_PATTERN_2.matcher(TARGET).matches() ||
                 DESTINATION_NUMBER_PATTERN_3.matcher(TARGET).matches() || 
                 DESTINATION_NUMBER_PATTERN_4.matcher(TARGET).matches() ||
                 DESTINATION_NUMBER_PATTERN_5.matcher(TARGET).matches() );
    }
        
    @Override public String toString() {
        return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this);
    }
    
    @Override public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) {
            return false;
        }
        return org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals(this, obj);
    }
    
    @Override public int hashCode() {
        return org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode(this);
    }    
}

