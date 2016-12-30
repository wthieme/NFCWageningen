package nl.whitedove.nfcwageningen;

import java.util.Date;

class LogItem {

    private String cacher;
    private Date datum;
    private String ftf = "";

    String getFtf() {
        return ftf;
    }

    void setFtf(String ftf) {
        this.ftf = ftf;
    }

    String getCacher() {
        return cacher;
    }

    void setCacher(String cacher) {
        this.cacher = cacher;
    }

    Date getDatum() {
        return datum;
    }

    void setDatum(Date datum) {
        this.datum = datum;
    }
}