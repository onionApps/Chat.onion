package onion.chat;

import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class Utils {

    private static Charset utf8 = Charset.forName("UTF-8");

    public static String base64encode(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    public static byte[] base64decode(String str) {
        return Base64.decode(str, Base64.NO_WRAP);
    }

    public static byte[] bin(InputStream is) throws IOException {
        try {
            byte[] data = new byte[0];
            while (true) {
                byte[] buf = new byte[1024];
                int n = is.read(buf);
                if (n < 0) return data;
                byte[] newdata = new byte[data.length + n];
                System.arraycopy(data, 0, newdata, 0, data.length);
                System.arraycopy(buf, 0, newdata, data.length, n);
                data = newdata;
            }
        } finally {
            is.close();
        }
    }

    public static String str(InputStream is) throws IOException {
        return new String(bin(is), utf8);
    }

    public static byte[] filebin(File f) {
        try {
            return bin(new FileInputStream(f));
        } catch (IOException ex) {
            return new byte[0];
        }
    }

    public static String filestr(File f) {
        return new String(filebin(f), utf8);
    }

}
