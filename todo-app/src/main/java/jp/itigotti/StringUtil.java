package jp.itigotti;

public class StringUtil {
    public static String truncateForLog(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() > 100) {
            return str.substring(0, 97) + "...";
        }
        return str;
    }
}
