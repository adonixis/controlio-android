package ru.adonixis.controlio.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTextUtils {

    public static String formatDate(String dateInParse) {
        String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        Date date;
        try {
            date = sdf.parse(dateInParse);
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy, hh:mm a", Locale.getDefault());
            return formatter.format(date);
        }
        catch(Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

}
