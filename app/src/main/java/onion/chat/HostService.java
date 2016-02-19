/*
 * Chat.onion - P2P Instant Messenger
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.chat;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class HostService extends Service {

    String TAG = "HostService";
    Timer timer;
    Client client;
    Server server;
    Tor tor;
    WifiManager.WifiLock wifiLock;
    PowerManager.WakeLock wakeLock;

    public HostService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Server.getInstance(this);
        Tor.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Server.getInstance(this);
        Tor.getInstance(this);
        return START_STICKY;
    }

    void log(String s) {
        Log.i(TAG, s);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        log("onCreate");

        server = Server.getInstance(this);
        tor = Tor.getInstance(this);
        client = Client.getInstance(this);

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                log("update");
                client.doSendPendingFriends();
                client.doSendAllPendingMessages();
            }
        }, 0, 1000 * 60 * 60);

        /*
        WifiManager wMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //wifiLock = wMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "WifiLock");
        wifiLock = wMgr.createWifiLock(WifiManager.WIFI_MnODE_FULL, "WifiLock");
        wifiLock.acquire();
        */

        PowerManager pMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock");
        wakeLock.acquire();

    }

    @Override
    public void onDestroy() {

        log("onDestroy");

        timer.cancel();
        timer.purge();

        if (wifiLock != null) {
            wifiLock.release();
            wifiLock = null;
        }

        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }

        super.onDestroy();

    }
}
