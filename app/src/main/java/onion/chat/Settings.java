/*
 * Chat.onion - P2P Instant Messenger
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings {

    public static SharedPreferences getPrefs(Context c) {

        c = c.getApplicationContext();

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(c);

        PreferenceManager.setDefaultValues(c, R.xml.prefs, false);

        return p;

    }

}
