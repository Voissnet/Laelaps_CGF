package net.redvoiss.sms.upload.beans;

import java.io.File;

import java.util.Arrays;
import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;

import static net.redvoiss.sms.upload.beans.util.MailToStringStyle.STYLE;

public class UploadedFileBean {
    private String m_userName; 
    private String m_fileName;
    private String m_fileEncoding;
    private File m_file;
    private Date m_sendDate;
    private byte[] m_hashCode;
    
    public UploadedFileBean(String filename, String fileEncoding, String username, File file, Date sendDate) {
        m_fileName = filename;
        m_fileEncoding = fileEncoding;
        m_userName = username;
        m_file = file;
        m_sendDate = sendDate;
    }
    
    public UploadedFileBean(String filename) {
        m_fileName = filename;
    }
    
    public void setFile( File f ) {
        m_file = f;
    }
    
    public File getFile() {
        return m_file;
    }
    
    public void setUserName( String userName ) {
        m_userName = userName;
    }
    
    public String getUserName() {
        return m_userName;
    }
    
    public String getFileName() {
        return m_fileName;
    }

	public String getFileEncoding() {
		return m_fileEncoding;
	}
    
    public void setHashCode(byte [] hashCode) {
        m_hashCode = hashCode;
    }
    
    public byte [] getHashCode() {
        return m_hashCode;
    }

    public Date getSendDate() {
        return m_sendDate;
    }
        
    @Override
    public String toString() {
        return new ToStringBuilder(this, STYLE).
            append("\tFile Name", m_fileName).
            append("\tUser Name", m_userName).
            append("\tDetails", super.toString()).
            toString();
	}
    
    @Override
    public boolean equals( Object o ) {
        if( o instanceof UploadedFileBean ) {
            UploadedFileBean urb = ((UploadedFileBean)o);
            return Arrays.equals( urb.m_hashCode, m_hashCode);
        }
        return false;
    }
    
    @Override
    public int hashCode() {        
        return Arrays.hashCode(m_hashCode);
    }
}