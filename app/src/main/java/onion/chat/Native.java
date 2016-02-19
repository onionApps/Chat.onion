/*
 * Chat.onion - P2P Instant Messenger
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.chat;

public class Native {

    static
    {
        System.loadLibrary("app");
    }

    native public static void killTor();

}
