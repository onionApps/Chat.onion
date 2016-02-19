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

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {

    private static Client instance;
    Tor tor;
    Database db;

    Context context;
    AtomicInteger counter = new AtomicInteger();
    StatusListener statusListener;

    public Client(Context c) {
        context = c;
        tor = Tor.getInstance(context);
        db = Database.getInstance(context);
    }

    public static Client getInstance(Context context) {
        if (instance == null)
            instance = new Client(context.getApplicationContext());
        return instance;
    }

    private void log(String s) {
        if (!BuildConfig.DEBUG) return;
        Log.i("Client", s);
    }

    private Sock connect(String address) {
        log("connect to " + address);
        Sock sock = new Sock(context, address + ".onion", Tor.getHiddenServicePort());
        return sock;
    }

    private boolean sendAdd(String receiver) {

        String sender = tor.getID();

        String n = db.getName();
        if (n == null || n.trim().isEmpty()) n = " ";
        n = Utils.base64encode(n.getBytes(Charset.forName("UTF-8")));

        return connect(receiver).queryAndClose(
                "add",
                receiver,
                sender,
                n,
                Utils.base64encode(tor.pubkey()),
                Utils.base64encode(tor.sign(("add " + receiver + " " + sender + " " + n).getBytes()))
        );

    }

    private boolean sendMsg(Sock sock, String receiver, String time, String content) {

        if (sock.isClosed()) {
            return false;
        }

        content = Utils.base64encode(content.getBytes(Charset.forName("UTF-8")));
        String sender = tor.getID();
        if (receiver.equals(sender)) return false;

        String n = db.getName();
        if (n == null || n.trim().isEmpty()) n = " ";
        n = Utils.base64encode(n.getBytes(Charset.forName("UTF-8")));

        return sock.queryBool(
                "msg",
                receiver,
                sender,
                n,
                time,
                content,
                Utils.base64encode(tor.pubkey()),
                Utils.base64encode(tor.sign(("msg " + receiver + " " + sender + " " + n + " " + time + " " + content).getBytes()))
        );

    }

    public void startSendPendingFriends() {
        log("start send pending friends");
        start(new Runnable() {
            @Override
            public void run() {
                doSendPendingFriends();
            }
        });
    }

    public void doSendPendingFriends() {
        log("do send pending friends");
        Database db = Database.getInstance(context);
        Cursor cur = db.getReadableDatabase().query("contacts", null, "outgoing=?", new String[]{"1"}, null, null, null);
        while (cur.moveToNext()) {
            log("try to send friend request");
            String address = cur.getString(cur.getColumnIndex("address"));
            if (sendAdd(address)) {
                db.acceptContact(address);
                log("friend request sent");
            }
        }
        cur.close();
    }

    public void doSendAllPendingMessages() {
        log("start send all pending messages");
        log("do send all pending messages");
        Database db = Database.getInstance(context);
        Cursor cur = db.getReadableDatabase().query("contacts", null, "outgoing=0 AND incoming=0", null, null, null, null);
        while (cur.moveToNext()) {
            log("try to send friend request");
            String address = cur.getString(cur.getColumnIndex("address"));
            doSendPendingMessages(address);
        }
        cur.close();
    }

    private void doSendPendingMessages(String address) {
        log("do send pending messages");
        Database db = Database.getInstance(context);
        Cursor cur = db.getReadableDatabase().query("messages", null, "pending=? AND receiver=?", new String[]{"1", address}, null, null, null);
        if (cur.getCount() > 0) {
            Sock sock = connect(address);
            while (cur.moveToNext()) {
                log("try to send message");
                String receiver = cur.getString(cur.getColumnIndex("receiver"));
                String time = cur.getString(cur.getColumnIndex("time"));
                String content = cur.getString(cur.getColumnIndex("content"));
                if (sendMsg(sock, receiver, time, content)) {
                    db.markMessageAsSent(cur.getLong(cur.getColumnIndex("_id")));
                    log("message sent");
                }
            }
            sock.close();
        }
        cur.close();
    }

    public void startSendPendingMessages(final String address) {
        log("start send pending messages");
        start(new Runnable() {
            @Override
            public void run() {
                doSendPendingMessages(address);
            }
        });
    }

    boolean isBusy() {
        return counter.get() > 0;
    }

    private void start(final Runnable runnable) {
        new Thread() {
            @Override
            public void run() {
                {
                    int n = counter.incrementAndGet();
                    StatusListener l = statusListener;
                    if (l != null) l.onStatusChange(n > 0);
                }
                try {
                    runnable.run();
                } finally {
                    int n = counter.decrementAndGet();
                    StatusListener l = statusListener;
                    if (l != null) l.onStatusChange(n > 0);
                }
            }
        }.start();
    }

    public void setStatusListener(StatusListener statusListener) {
        this.statusListener = statusListener;
        if (statusListener != null) {
            statusListener.onStatusChange(counter.get() > 0);
        }
    }

    public interface StatusListener {
        void onStatusChange(boolean loading);
    }


    public boolean testIfServerIsUp() {
        Sock sock = connect(tor.getID());
        boolean ret = sock.isClosed() == false;
        sock.close();
        return ret;
    }

    public void doAskForNewMessages(String receiver) {
        String sender = tor.getID();
        log("ask for new msg");
        String cmd = "newmsg " + receiver + " " + sender + " " + System.currentTimeMillis() / 60000 * 60000;
        connect(receiver).queryAndClose(
                cmd,
                Utils.base64encode(tor.pubkey()),
                Utils.base64encode(tor.sign(cmd.getBytes()))
        );
    }

    public void startAskForNewMessages(final String receiver) {
        start(new Runnable() {
            @Override
            public void run() {
                doAskForNewMessages(receiver);
            }
        });
    }

    public void askForNewMessages() {
        Cursor cur = db.getReadableDatabase().query("contacts", null, "incoming=0", null, null, null, null);
        while (cur.moveToNext()) {
            String receiver = cur.getString(cur.getColumnIndex("address"));
            doAskForNewMessages(receiver);
        }
        cur.close();
    }


}
