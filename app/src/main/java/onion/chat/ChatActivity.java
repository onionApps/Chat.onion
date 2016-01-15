package onion.chat;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class ChatActivity extends AppCompatActivity {

    ChatAdapter adapter;
    RecyclerView recycler;
    Cursor cursor;
    Database db;
    Tor tor;
    String address;
    Client client;

    String myname = "", othername = "";

    long idLastLast = -1;

    long rep = 0;
    Timer timer;

    void update() {
        Cursor oldCursor = cursor;

        myname = db.getName().trim();
        othername = db.getContactName(address).trim();

        //cursor = db.getReadableDatabase().query("messages", null, "((sender=? AND receiver=?) OR (sender=? AND receiver=?)) AND sender != '' AND receiver != ''", new String[] { tor.getID(), address, address, tor.getID() }, null, null, "time ASC");

        String a = tor.getID();
        String b = address;
        if (a.equals("")) a = "fahewmrvahwlejufbaelilrv123425";
        if (b.equals("")) b = "kznmhgauzgmuegwurbakwenu826347";
        //cursor = db.getReadableDatabase().query("messages", null, "(sender=? AND receiver=?) OR (sender=? AND receiver=?)", new String[] { a, b, b, a }, null, null, "time ASC");
        cursor = db.getReadableDatabase().rawQuery("SELECT * FROM (SELECT * FROM messages WHERE ((sender=? AND receiver=?) OR (sender=? AND receiver=?)) ORDER BY time DESC LIMIT 64) ORDER BY time ASC", new String[]{a, b, b, a});

        cursor.moveToLast();
        long idLast = -1;

        int i = cursor.getColumnIndex("_id");
        if (i >= 0 && cursor.getCount() > 0) {
            idLast = cursor.getLong(i);
        }

        //if(oldCursor == null || cursor.getCount() != oldCursor.getCount())
        if (idLast != idLastLast) {
            idLastLast = idLast;

            if (oldCursor == null || oldCursor.getCount() == 0)
                recycler.scrollToPosition(Math.max(0, cursor.getCount() - 1));
            else
                recycler.smoothScrollToPosition(Math.max(0, cursor.getCount() - 1));

            //client.startSendPendingMessages(address);
        }

        adapter.notifyDataSetChanged();

        if (oldCursor != null)
            oldCursor.close();

        findViewById(R.id.noMessages).setVisibility(cursor.getCount() > 0 ? View.GONE : View.VISIBLE);
    }

    void sendPendingAndUpdate() {
        //if(!client.isBusy()) {
        client.startSendPendingMessages(address);
        //}
        update();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        //requestWindowFeature(Window.FEATURE_PROGRESS);

        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        //requestWindowFeature(Window.FEATURE_PROGRESS);


        super.onCreate(savedInstanceState);


        //supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);


        setContentView(R.layout.activity_chat);

        db = Database.getInstance(this);
        tor = Tor.getInstance(this);

        client = Client.getInstance(this);

        address = getIntent().getDataString();

        if (address.contains(":"))
            address = address.substring(address.indexOf(':') + 1);

        Log.i("ADDRESS", address);

        String name = db.getContactName(address);
        if (name.isEmpty()) {
            getSupportActionBar().setTitle(address);
        } else {
            getSupportActionBar().setTitle(name);
            getSupportActionBar().setSubtitle(address);
        }

        recycler = (RecyclerView) findViewById(R.id.recycler);

        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ChatAdapter();
        recycler.setAdapter(adapter);

        final View send = findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sender = tor.getID();
                if (sender == null || sender.trim().equals("")) {
                    sendPendingAndUpdate();
                    return;
                }

                String message = ((EditText) findViewById(R.id.editmessage)).getText().toString();
                message = message.trim();
                if (message.equals("")) return;

                ContentValues v = new ContentValues();
                v.put("sender", sender);
                v.put("receiver", address);
                v.put("content", message);
                v.put("time", System.currentTimeMillis());
                v.put("pending", true);
                db.getReadableDatabase().insert("messages", null, v);

                ((EditText) findViewById(R.id.editmessage)).setText("");

                sendPendingAndUpdate();

                //recycler.scrollToPosition(cursor.getCount() - 1);

                recycler.smoothScrollToPosition(Math.max(0, cursor.getCount() - 1));

                rep = 0;
            }
        });

        startService(new Intent(this, HostService.class));


        final EditText editmessage = (EditText) findViewById(R.id.editmessage);
        final float a = 0.5f;
        send.setAlpha(a);
        send.setClickable(false);
        editmessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() == 0) {
                    send.setAlpha(a);
                    send.setClickable(false);
                } else {
                    send.setAlpha(0.7f);
                    send.setClickable(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        Server.getInstance(this).setListener(new Server.Listener() {
            @Override
            public void onChange() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        update();
                    }
                });
            }
        });

        Tor.getInstance(this).setListener(new Tor.Listener() {
            @Override
            public void onChange() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!client.isBusy()) {
                            sendPendingAndUpdate();
                        }
                    }
                });
            }
        });

        client.setStatusListener(new Client.StatusListener() {
            @Override
            public void onStatusChange(final boolean loading) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("LOADING", "" + loading);
                        //setProgressBarIndeterminateVisibility(loading);
                        //setProgressBarIndeterminate(true);
                        //setProgressBarVisibility(loading);
                        //setProgressBarIndeterminateVisibility(loading);
                        //setProgressBarVisibility(loading);
                        findViewById(R.id.progressbar).setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
                        if (!loading) update();
                    }
                });
            }
        });

        sendPendingAndUpdate();


        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {

                rep++;

                if (rep > 5 && rep % 5 != 0) {
                    log("wait");
                    return;
                }


                log("update");


                if (client.isBusy()) {
                    log("abort update, client busy");
                    return;
                } else {
                    log("do update");
                    client.startSendPendingMessages(address);
                }

            }
        }, 0, 1000 * 60);

        Notifier.getInstance(this).onResumeActivity();

        db.clearPendingMessages(address);

        ((TorStatusView) findViewById(R.id.torStatusView)).update();

        startService(new Intent(this, HostService.class));
    }

    void log(String s) {
        Log.i("Chat", s);
    }

    @Override
    protected void onPause() {
        db.clearPendingMessages(address);
        Notifier.getInstance(this).onPauseActivity();
        timer.cancel();
        timer.purge();
        Server.getInstance(this).setListener(null);
        tor.setListener(null);
        client.setStatusListener(null);
        super.onPause();
    }

    private String date(String str) {
        long t = 0;
        try {
            t = Long.parseLong(str);
        } catch (Exception ex) {
            return "";
        }
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(new Date(t));
    }

    class ChatHolder extends RecyclerView.ViewHolder {
        public TextView message, time, status;
        public View left, right;
        public CardView card;

        public ChatHolder(View v) {
            super(v);
            message = (TextView) v.findViewById(R.id.message);
            //sender = (TextView)v.findViewById(R.id.sender);
            time = (TextView) v.findViewById(R.id.time);
            status = (TextView) v.findViewById(R.id.status);
            left = v.findViewById(R.id.left);
            right = v.findViewById(R.id.right);
            card = (CardView) v.findViewById(R.id.card);
        }
    }

    class ChatAdapter extends RecyclerView.Adapter<ChatHolder> {

        @Override
        public ChatHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ChatHolder(getLayoutInflater().inflate(R.layout.item_message, parent, false));
        }

        @Override
        public void onBindViewHolder(ChatHolder holder, int position) {
            if (cursor == null) return;

            cursor.moveToFirst();
            cursor.moveToPosition(position);

            String content = cursor.getString(cursor.getColumnIndex("content"));
            String sender = cursor.getString(cursor.getColumnIndex("sender"));
            String time = date(cursor.getString(cursor.getColumnIndex("time")));
            boolean pending = cursor.getInt(cursor.getColumnIndex("pending")) > 0;
            boolean tx = sender.equals(tor.getID());

            /*
            if(sender == null) sender = "";
            if(sender.equals(address) && !othername.isEmpty()) sender = othername;
*/
            //if(sender.equals(tor.getID()) && !myname.isEmpty()) sender = myname;

            /*if(sender.equals(tor.getID())) {
                if (pending)
                    sender = "Pending...";
                else
                    sender = "";
            }*/

            if (sender.equals(tor.getID())) sender = "You";

            if (tx) {
                holder.left.setVisibility(View.VISIBLE);
                holder.right.setVisibility(View.GONE);
            } else {
                holder.left.setVisibility(View.GONE);
                holder.right.setVisibility(View.VISIBLE);
            }

            if (pending)
                //holder.card.setCardElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()));
                holder.card.setCardElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));
            else
                holder.card.setCardElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics()));


            String status = "";
            if (sender.equals(address)) {
                if (othername.isEmpty())
                    status = address;
                else
                    status = othername;
            } else {
                if (pending) {
                    status = getString(R.string.message_pending);
                    //status = "...";
                    //status = "Waiting...";
                } else {
                    status = getString(R.string.message_sent);
                    //status = "\u2713";
                    //status = "Sent.";
                }
            }


            int color = pending ? 0xff000000 : 0xff888888;
            holder.time.setTextColor(color);
            //holder.sender.setTextColor(color);
            holder.status.setTextColor(color);


            holder.message.setText(content);
            //holder.sender.setText(sender);
            holder.time.setText(time);

            holder.status.setText(status);

            /*if(tx) {
                if (pending)
                    holder.status.setText("Pending...");
                else
                    holder.status.setText("Sent");
                holder.status.setVisibility(View.VISIBLE);
            } else {
                holder.status.setVisibility(View.GONE);
            }*/
            //holder.status.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return cursor != null ? cursor.getCount() : 0;
        }

    }

}
