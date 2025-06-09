package net.redvoiss.sms.upload.beans;

import java.io.File;
import java.util.Objects;

public class CampaignBean {
    private File m_file;
    private String m_name;
    private int m_size;
    private String m_hash;

    public CampaignBean(String name, File file, int size, String hash) {
        m_name = name;
        m_file = file;
        m_size = size;
        m_hash = hash;
    }

    public CampaignBean(String hash) {
        m_hash = hash;
    }

    public File getFile() {
        return m_file;
    }

    public String getName() {
        return m_name;
    }

    public int getSize() {
        return m_size;
    }

    public String getHash() {
        return m_hash;
    }

    @Override
    public boolean equals(Object obj) {
        boolean ret = false;
        if (obj instanceof CampaignBean) {
            final CampaignBean other = (CampaignBean) obj;
            ret = Objects.equals(m_hash, other.m_hash);
        }
        return ret;
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_hash);
    }

}