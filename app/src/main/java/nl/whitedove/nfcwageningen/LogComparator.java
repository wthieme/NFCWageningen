package nl.whitedove.nfcwageningen;

import java.util.Comparator;

class LogComparator implements Comparator<LogItem> {
    public int compare(LogItem left, LogItem right) {
        return right.getDatum().compareTo(left.getDatum());
    }
}
