package net.redvoiss.sms.smpp.client;

public class DLRInfo {
    String id, sub, dlvrd, submitDate, doneDate, err;
    StatEnum stat;

    DLRInfo () {}
    
    public void setId(String id) {
        this.id = id;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public void setDlvrd(String dlvrd) {
        this.dlvrd = dlvrd;
    }

    public void setSubmitDate(String submitDate) {
        this.submitDate = submitDate;
    }

    public void setDoneDate(String doneDate) {
        this.doneDate = doneDate;
    }

    public void setStat(StatEnum stat) {
        this.stat = stat;
    }

    public void setErr(String err) {
        this.err = err;
    }

    public String getId() {
        return id;
    }

    public StatEnum getStat() {
        return stat;
    }

    public String getDoneDate() {
        return doneDate == null || doneDate.length() < 11 ? doneDate : doneDate.substring(0, 10); // SMPP v3.4
    }

    @Override
    public String toString() {
        return org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString(this);
    }
}