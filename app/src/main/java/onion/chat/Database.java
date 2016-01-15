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


        db.execSQL("CREATE TABLE contacts ( _id INTEGER PRIMARY KEY, address TEXT UNIQUE, name TEXT, outgoing INTEGER DEFAULT 0, incoming INTEGER DEFAULT 0, pending INTEGER DEFAULT 0)");

        db.execSQL("CREATE INDEX contactindex ON contacts ( incoming, name, address )");

        db.execSQL("CREATE TABLE messages ( _id INTEGER PRIMARY KEY, sender TEXT, receiver TEXT, content TEXT, time INTEGER, pending INTEGER, UNIQUE(sender, receiver, time) )");


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public String getContactName(String id) {
        String ret = "";
        Cursor cursor = getReadableDatabase().query("contacts", new String[]{"name"}, "address=?", new String[]{id}, null, null, null);
        if (cursor.moveToNext()) {
            ret = cursor.getString(0);
        }
        if (ret == null) ret = "";
        cursor.close();
        return ret;
    }

    public long addMessage(String sender, String sendername, String receiver, String content, long time) {

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

    public void clearPendingMessages(String address) {
        getWritableDatabase().execSQL("UPDATE contacts SET pending=0 WHERE address=?", new String[]{address});
    }

    public boolean addContact(String id, boolean outgoing, boolean incoming, String name) {
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

    public boolean addContact(String id, boolean outgoing, boolean incoming) {
        return addContact(id, outgoing, incoming, "");
    }

    public boolean hasContact(String id) {
        Cursor cursor = getReadableDatabase().query("contacts", null, "address=? AND incoming=0", new String[]{id}, null, null, null);
        boolean ret = cursor.getCount() > 0;
        cursor.close();
        return ret;
    }

    public boolean acceptContact(String id) {
        ContentValues v = new ContentValues();
        v.put("incoming", false);
        v.put("outgoing", false);
        return getWritableDatabase().update("contacts", v, "address=?", new String[]{id}) != 0;
    }

    public boolean removeContact(String id) {
        return getWritableDatabase().delete("contacts", "address=?", new String[]{id}) != 0;
        /*
        ContentValues v = new ContentValues();
        v.put("incoming", true);
        v.put("outgoing", false);
        return getWritableDatabase().update("contacts", v, "address=?", new String[]{id}) != 0;
        */
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
    }

    public String get(String key) {
        return prefs().getString(key, "");
    }

    public void put(String key, String value) {
        prefs().edit().putString(key, value).apply();
    }

    public String getName() {
        return get("name");
    }

    public void setName(String name) {
        put("name", name);
    }

    public int getNotifications() {
        return prefs().getInt("notifications", 0);
    }

    public void setNotifications(int n) {
        prefs().edit().putInt("notifications", n).apply();
    }

    public void addNotification() {
        setNotifications(getNotifications() + 1);
    }

    public void clearNotifications() {
        setNotifications(0);
    }

    public int getNewRequests() {
        return prefs().getInt("requests", 0);
    }

    public void setNewRequests(int n) {
        prefs().edit().putInt("requests", n).apply();
    }

    public void addNewRequest() {
        setNewRequests(getNewRequests() + 1);
    }

    public void clearNewRequests() {
        setNewRequests(0);
    }
}
