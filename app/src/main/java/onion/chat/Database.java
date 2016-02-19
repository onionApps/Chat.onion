/*
 * Chat.onion - P2P Instant Messenger
 *
 * http://play.google.com/store/apps/details?id=onion.chat
 * http://onionapps.github.io/Chat.onion/
 *
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.chat;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Database extends SQLiteOpenHelper {

    private static Database instance;
    private Context context;

    public Database(Context context) {
        super(context, "cdb", null, 1);
        this.context = context;
    }

    public static Database getInstance(Context context) {
        if (instance == null)
            instance = new Database(context.getApplicationContext());
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //db.execSQL("CREATE TABLE contacts ( _id INTEGER PRIMARY KEY, address TEXT UNIQUE, name TEXT, outgoing INTEGER, incoming INTEGER, pending INTEGER )");
        //db.execSQL("CREATE TABLE messages ( _id INTEGER PRIMARY KEY, sender TEXT, receiver TEXT, content TEXT, time INTEGER, pending INTEGER, UNIQUE(sender, receiver, content, time) )");


        // contact / contact request table
        // _id: primary key
        // address: 16 character onion address
        // name: nick-name
        // outgoing: pending outgoing friend request
        // incoming: incoing friend request / someone else wants to add us / will be shown on the requests tab instead of the contacts tab
        // pending: the number of unread messages
        db.execSQL("CREATE TABLE contacts ( _id INTEGER PRIMARY KEY, address TEXT UNIQUE, name TEXT, outgoing INTEGER DEFAULT 0, incoming INTEGER DEFAULT 0, pending INTEGER DEFAULT 0)");

        // index contacts by which tab they should appear on and by their names
        db.execSQL("CREATE INDEX contactindex ON contacts ( incoming, name, address )");

        // message table
        // _id: primary key
        // sender: 16 char onion address
        // receiver: 16 char onion address
        // content: message contents
        // time: message timestamp
        // pending: 1 if it is an outgoing message that still has to be sent, 0 if the message has already been sent, or if it has been received from someone else
        db.execSQL("CREATE TABLE messages ( _id INTEGER PRIMARY KEY, sender TEXT, receiver TEXT, content TEXT, time INTEGER, pending INTEGER, UNIQUE(sender, receiver, time) )");


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }


    // messages

    public synchronized long addUnreadIncomingMessage(String sender, String sendername, String receiver, String content, long time) {
        addContact(sender, false, true, sendername);
        long n;
        {
            ContentValues v = new ContentValues();
            v.put("sender", sender);
            v.put("receiver", receiver);
            v.put("content", content);
            v.put("time", time);
            n = getWritableDatabase().insert("messages", null, v);
        }
        if (n > 0) {
            getWritableDatabase().execSQL("UPDATE contacts SET pending=pending+1 WHERE address=?", new String[]{sender});
        }
        return n;
    }

    public synchronized long addPendingOutgoingMessage(String sender, String receiver, String message) {
        ContentValues v = new ContentValues();
        v.put("sender", sender);
        v.put("receiver", receiver);
        v.put("content", message);
        v.put("time", System.currentTimeMillis());
        v.put("pending", true);
        return getReadableDatabase().insert("messages", null, v);
    }

    public synchronized boolean abortOutgoingMessage(long id) {
        int n = getWritableDatabase().delete("messages", "_id=? AND pending=1", new String[]{"" + id});
        return n > 0;
    }

    public synchronized void clearIncomingMessageCount(String address) {
        getWritableDatabase().execSQL("UPDATE contacts SET pending=0 WHERE address=?", new String[]{address});
    }

    public synchronized Cursor getMessages(String a, String b) {
        if (a.equals("")) a = "fahewmrvahwlejufbaelilrv123425";
        if (b.equals("")) b = "kznmhgauzgmuegwurbakwenu826347";
        return getReadableDatabase().rawQuery("SELECT * FROM (SELECT * FROM messages WHERE ((sender=? AND receiver=?) OR (sender=? AND receiver=?)) ORDER BY time DESC LIMIT 64) ORDER BY time ASC", new String[]{a, b, b, a});
    }

    public synchronized void markMessageAsSent(long id) {
        ContentValues v = new ContentValues();
        v.put("pending", false);
        getWritableDatabase().update("messages", v, "_id=?", new String[]{"" + id});
    }


    // contacts

    public synchronized String getContactName(String id) {
        String ret = "";
        Cursor cursor = getReadableDatabase().query("contacts", new String[]{"name"}, "address=?", new String[]{id}, null, null, null);
        if (cursor.moveToNext()) {
            ret = cursor.getString(0);
        }
        if (ret == null) ret = "";
        cursor.close();
        return ret;
    }

    public synchronized boolean addContact(String id, boolean outgoing, boolean incoming, String name) {
        name = name.trim();
        id = id.trim().toLowerCase();
        ContentValues v = new ContentValues();
        v.put("address", id);
        v.put("outgoing", outgoing);
        v.put("incoming", incoming);
        v.put("name", name);
        v.put("pending", 0);
        long n = getWritableDatabase().insertWithOnConflict("contacts", null, v, SQLiteDatabase.CONFLICT_IGNORE);
        if (n >= 0) addNewRequest();
        return n >= 0;
    }

    public synchronized boolean addContact(String id, boolean outgoing, boolean incoming) {
        return addContact(id, outgoing, incoming, "");
    }

    public synchronized boolean hasContact(String id) {
        Cursor cursor = getReadableDatabase().query("contacts", null, "address=? AND incoming=0", new String[]{id}, null, null, null);
        boolean ret = cursor.getCount() > 0;
        cursor.close();
        return ret;
    }

    public synchronized boolean acceptContact(String id) {
        ContentValues v = new ContentValues();
        v.put("incoming", false);
        v.put("outgoing", false);
        return getWritableDatabase().update("contacts", v, "address=?", new String[]{id}) != 0;
    }

    public synchronized boolean removeContact(String id) {
        return getWritableDatabase().delete("contacts", "address=?", new String[]{id}) != 0;
        /*
        // would move a contact back to the contact request tab (e.g. for testing)
        ContentValues v = new ContentValues();
        v.put("incoming", true);
        v.put("outgoing", false);
        return getWritableDatabase().update("contacts", v, "address=?", new String[]{id}) != 0;
        */
    }

    public synchronized void setContactName(String address, String name) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        getWritableDatabase().update("contacts", v, "address=?", new String[]{address});
    }


    // prefs

    synchronized private SharedPreferences prefs() {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
    }

    public synchronized String get(String key) {
        return prefs().getString(key, "");
    }

    public synchronized void put(String key, String value) {
        prefs().edit().putString(key, value).apply();
    }

    public synchronized String getName() {
        return get("name");
    }

    public synchronized void setName(String name) {
        put("name", name);
    }

    public synchronized int getNotifications() {
        return prefs().getInt("notifications", 0);
    }

    public synchronized void setNotifications(int n) {
        prefs().edit().putInt("notifications", n).apply();
    }

    public synchronized void addNotification() {
        setNotifications(getNotifications() + 1);
    }

    public synchronized void clearNotifications() {
        setNotifications(0);
    }

    public synchronized int getNewRequests() {
        return prefs().getInt("requests", 0);
    }

    public synchronized void setNewRequests(int n) {
        prefs().edit().putInt("requests", n).apply();
    }

    public synchronized void addNewRequest() {
        setNewRequests(getNewRequests() + 1);
    }

    public synchronized void clearNewRequests() {
        setNewRequests(0);
    }
}
