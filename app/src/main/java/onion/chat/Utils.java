/*
 * Chat.onion - P2P Instant Messenger
 *
 * http://play.google.com/store/apps/details?id=onion.chat
 * http://onionapps.github.io/Chat.onion/
 * http://github.com/onionApps/Chat.onion
 *
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.chat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Base64;
import android.view.View;

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

    public static CharSequence linkify(final Context context, String s) {
        SpannableStringBuilder b = new SpannableStringBuilder(s);
        SpannableStringBuilder r = new SpannableStringBuilder(s);
        Linkify.addLinks(b, Linkify.WEB_URLS);
        URLSpan[] urls = b.getSpans(0, b.length(), URLSpan.class);
        for(int i = 0; i < urls.length && i < 8; i++) {
            URLSpan span = urls[i];
            int start = b.getSpanStart(span);
            int end = b.getSpanEnd(span);
            int flags = b.getSpanFlags(span);
            final String url = span.getURL();
            ClickableSpan s2 = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    new AlertDialog.Builder(context)
                            .setTitle(url)
                            .setMessage("Open link in external app?")
                            .setNegativeButton("No", null)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                                }
                            })
                            .show();
                }
            };
            r.setSpan(s2, start, end, flags);
        }
        return r;
    }

}
