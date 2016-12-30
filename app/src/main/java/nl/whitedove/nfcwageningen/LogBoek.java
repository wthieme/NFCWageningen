package nl.whitedove.nfcwageningen;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

class LogBoek {

    enum StatusLogBoek {
        Ok, ReedsGelogd, Fout
    }

    private String cacheNaam = "";
    private Date datumLogBoek;
    private Boolean ftfTonen = false;
    private ArrayList<LogItem> logLijst;
    private int freeBytes = 0;
    private StatusLogBoek status = StatusLogBoek.Ok;
    private String foutTekst = "";

    String getFoutTekst() {
        return foutTekst;
    }

    void setFoutTekst(String foutTekst) {
        this.foutTekst = foutTekst;
        if (!this.foutTekst.isEmpty()) {
            this.status = StatusLogBoek.Fout;
        }
    }

    StatusLogBoek getStatus() {
        return status;
    }

    void setStatus(StatusLogBoek status) {
        this.status = status;
    }

    int getFreeBytes() {
        return freeBytes;
    }

    void setFreeBytes(int freeBytes) {
        this.freeBytes = freeBytes;
    }

    ArrayList<LogItem> getLogLijst() {
        return logLijst;
    }

    void setLogLijst(ArrayList<LogItem> logLijst) {
        this.logLijst = logLijst;
    }

    String getCacheNaam() {
        return cacheNaam;
    }

    void setCacheNaam(String cacheNaam) {
        this.cacheNaam = cacheNaam;
    }

    private Date getDatumLogBoek() {
        return datumLogBoek;
    }

    void setDatumLogBoek(Date datumLogBoek) {
        this.datumLogBoek = datumLogBoek;
    }

    Boolean getFtfTonen() {
        return ftfTonen;
    }

    void setFtfTonen(Boolean ftfTonen) {
        this.ftfTonen = ftfTonen;
    }

    @SuppressLint("SimpleDateFormat")
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(cacheNaam);
        sb.append(';');
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        sb.append(dateFormat.format(datumLogBoek));
        sb.append(';');
        sb.append(ftfTonen ? 1 : 0);
        sb.append(';');
        for (LogItem li : logLijst) {
            sb.append(li.getCacher());
            sb.append(';');
            long minuten = (li.getDatum().getTime() - datumLogBoek
                    .getTime()) / 60000;
            sb.append(minuten);
            sb.append(';');
        }
        return sb.toString();
    }

    @SuppressLint("SimpleDateFormat")
    static LogBoek LogBoekFromTagInfo(String tagInfo) {
        LogBoek lb = new LogBoek();
        lb.logLijst = new ArrayList<>();
        try {
            String[] strLijst = tagInfo.split(";");
            for (int i = 0; i < Math.min(3, strLijst.length); i++) {
                if (i == 0) {
                    lb.setCacheNaam(strLijst[i]);
                }

                if (i == 1) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(
                            "yyyyMMdd");
                    lb.setDatumLogBoek(dateFormat.parse(strLijst[i]));
                }

                if (i == 2) {
                    Boolean ftfTonen = false;
                    if (Integer.parseInt(strLijst[i]) > 0) {
                        ftfTonen = true;
                    }
                    lb.setFtfTonen(ftfTonen);
                }
            }
            for (int i = 0; i < (strLijst.length - 3) / 2; i++) {
                LogItem li = new LogItem();
                li.setCacher(strLijst[2 * i + 3]);
                int minuten = Integer.parseInt(strLijst[2 * i + 4]);
                Calendar cal = Calendar.getInstance();
                cal.setTime(lb.getDatumLogBoek());
                cal.add(Calendar.MINUTE, minuten);
                li.setDatum(cal.getTime());
                lb.logLijst.add(li);
            }
        } catch (Exception ex) {
            lb.setStatus(LogBoek.StatusLogBoek.Fout);
            lb.setFoutTekst("Fout bij het uitlezen van de NFC Tag");
        }

        return lb;
    }
}
