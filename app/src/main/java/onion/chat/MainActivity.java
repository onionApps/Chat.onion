package onion.chat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    Database db;
    Tor tor;
    TabLayout tabLayout;
    View contactPage, requestPage;
    RecyclerView contactRecycler, requestRecycler;
    View contactEmpty, requestEmpty;
    Cursor contactCursor, requestCursor;
    ;
    int REQUEST_QR = 12;

    void send() {
        Client.getInstance(this).startSendPendingFriends();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = Database.getInstance(this);
        tor = Tor.getInstance(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/

                /*
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.add_contact)
                        .setItems(new String[]{
                                getString(R.string.show_qr),
                                getString(R.string.scan_qr),
                                getString(R.string.enter_id),
                                getString(R.string.invite_friends),
                        }, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) showQR();
                                if (which == 1) scanQR();
                                if (which == 2) addContact();
                                if (which == 3) inviteFriend();
                            }
                        })
                        .show();
                      */

                final Dialog[] d = new Dialog[1];

                View v = getLayoutInflater().inflate(R.layout.dialog_connect, null);
                ((TextView) v.findViewById(R.id.id)).setText(Tor.getInstance(MainActivity.this).getID());
                v.findViewById(R.id.qr_show).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        d[0].cancel();
                        showQR();
                    }
                });
                v.findViewById(R.id.qr_scan).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        d[0].cancel();
                        scanQR();
                    }
                });
                v.findViewById(R.id.enter_id).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        d[0].cancel();
                        addContact();
                    }
                });
                v.findViewById(R.id.share_id).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        d[0].cancel();
                        inviteFriend();
                    }
                });
                d[0] = new AlertDialog.Builder(MainActivity.this)
                        //.setTitle(R.string.add_contact)
                        .setView(v)
                        .show();

            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        startService(new Intent(this, HostService.class));

        /*
        View empty = findViewById(R.id.nocontacts);
        ((ViewGroup)empty.getParent()).removeView(empty);
        ((ListView)findViewById(R.id.contacts)).setEmptyView(empty);
        */




        /*
        ((ListView)findViewById(R.id.contacts)).setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final String address = ((TextView) view.findViewById(R.id.address)).getText().toString();
                final String name = ((TextView) view.findViewById(R.id.name)).getText().toString();
                contactLongPress(address, name);
                return true;
            }
        });

        ((ListView)findViewById(R.id.contacts)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String address = ((TextView) view.findViewById(R.id.address)).getText().toString();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("chat:" + address), getApplicationContext(), ChatActivity.class));
            }
        });
        */

        findViewById(R.id.myname).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeName();
            }
        });
        /*findViewById(R.id.changename).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeName();
            }
        });*/


        findViewById(R.id.myaddress).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQR();
            }
        });


        contactPage = getLayoutInflater().inflate(R.layout.page_contacts, null);
        requestPage = getLayoutInflater().inflate(R.layout.page_requests, null);

        contactRecycler = (RecyclerView) contactPage.findViewById(R.id.contactRecycler);
        requestRecycler = (RecyclerView) requestPage.findViewById(R.id.requestRecycler);

        contactEmpty = contactPage.findViewById(R.id.contactEmpty);
        requestEmpty = requestPage.findViewById(R.id.requestEmpty);

        contactRecycler.setLayoutManager(new LinearLayoutManager(this));
        contactRecycler.setAdapter(new RecyclerView.Adapter<ContactViewHolder>() {
            @Override
            public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                final ContactViewHolder viewHolder = new ContactViewHolder(getLayoutInflater().inflate(R.layout.item_contact, parent, false));
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("chat:" + viewHolder.address.getText()), getApplicationContext(), ChatActivity.class));
                    }
                });
                viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        contactLongPress(viewHolder.address.getText().toString(), viewHolder.name.getText().toString());
                        return true;
                    }
                });
                return viewHolder;
            }

            @Override
            public void onBindViewHolder(ContactViewHolder holder, int position) {
                contactCursor.moveToPosition(position);
                holder.address.setText(contactCursor.getString(0));
                String name = contactCursor.getString(1);
                if (name == null || name.equals("")) name = "Anonymous";
                holder.name.setText(name);
                long n = contactCursor.getLong(2);
                if (n > 0) {
                    holder.badge.setVisibility(View.VISIBLE);
                    holder.count.setText("" + n);
                } else {
                    holder.badge.setVisibility(View.GONE);
                }
            }

            @Override
            public int getItemCount() {
                return contactCursor != null ? contactCursor.getCount() : 0;
            }
        });

        requestRecycler.setLayoutManager(new LinearLayoutManager(this));
        requestRecycler.setAdapter(new RecyclerView.Adapter<ContactViewHolder>() {
            @Override
            public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                final ContactViewHolder viewHolder = new ContactViewHolder(getLayoutInflater().inflate(R.layout.item_contact_request, parent, false));
                viewHolder.accept.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String addr = viewHolder.address.getText().toString();
                        db.acceptContact(addr);
                        Client.getInstance(getApplicationContext()).startAskForNewMessages(addr);
                        updateContactList();
                    }
                });
                viewHolder.decline.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String address = viewHolder.address.getText().toString();
                        final String name = viewHolder.name.getText().toString();
                        db.removeContact(address);
                        updateContactList();
                        Snackbar.make(findViewById(R.id.drawer_layout), R.string.contact_request_declined, Snackbar.LENGTH_LONG)
                                .setAction(R.string.undo, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        db.addContact(address, false, true, name);
                                        updateContactList();
                                    }
                                })
                                .show();
                    }
                });
                return viewHolder;
            }

            @Override
            public void onBindViewHolder(ContactViewHolder holder, int position) {
                requestCursor.moveToPosition(position);
                holder.address.setText(requestCursor.getString(0));
                String name = requestCursor.getString(1);
                if (name == null || name.equals("")) name = "Anonymous";
                holder.name.setText(name);
            }

            @Override
            public int getItemCount() {
                return requestCursor != null ? requestCursor.getCount() : 0;
            }
        });

        tabLayout = (TabLayout) findViewById(R.id.tabLayout);


        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public Object instantiateItem(final ViewGroup container, int position) {
                View v = position == 0 ? contactPage : requestPage;
                container.addView(v);
                return v;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }
        });
        tabLayout.setupWithViewPager(viewPager);
        //viewPager.addView(contactList);
        //viewPager.addView(requestList);


        //tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_people_white_36dp).setContentDescription("Contacts"));
        //tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_people_outline_white_36dp).setContentDescription("Requests"));

        //tabLayout.getTabAt(0).setIcon(R.drawable.ic_people_white_36dp).setContentDescription("Contacts");
        //tabLayout.getTabAt(1).setIcon(R.drawable.ic_people_outline_white_36dp).setContentDescription("Requests");

        tabLayout.getTabAt(0).setText(R.string.tab_contacts);
        tabLayout.getTabAt(1).setText(R.string.tab_requests);

        /*View v = tabLayout.getTabAt(1).getCustomView();
        Log.i("MainActivity", v.toString());
        tabLayout.getTabAt(1).setCustomView(v);*/

        /*ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.mipmap.ic_launcher);
        tabLayout.getTabAt(1).setCustomView(imageView);*/

        for (int i = 0; i < 2; i++) {
            View v = getLayoutInflater().inflate(R.layout.tab_header, null, false);
            ((TextView) v.findViewById(R.id.text)).setText(tabLayout.getTabAt(i).getText().toString());
            ((TextView) v.findViewById(R.id.badge)).setText("");
            v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            tabLayout.getTabAt(i).setCustomView(v);
        }

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1) {
                    db.clearNewRequests();
                }
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        //tabLayout.getTabAt(0).setIcon(R.drawable.ic_people_white_36dp).setContentDescription("Contacts");
        //tabLayout.getTabAt(1).setIcon(R.drawable.ic_more_horiz_white_36dp).setContentDescription("Requests");

        updateContactList();


        handleIntent();

    }

    void handleIntent() {

        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        Uri uri = intent.getData();
        if (uri == null) {
            return;
        }

        if (!uri.getHost().equals("chat.onion")) {
            return;
        }

        List<String> pp = uri.getPathSegments();
        String address = pp.size() > 0 ? pp.get(0) : null;
        String name = pp.size() > 1 ? pp.get(1) : "";
        if (address == null) {
            return;
        }

        addContact(address, name);

    }

    void updateContactList() {

        if (contactCursor != null) {
            contactCursor.close();
            contactCursor = null;
        }
        contactCursor = db.getReadableDatabase().query("contacts", new String[]{"address", "name", "pending"}, "incoming=0", null, null, null, "name, address");
        contactRecycler.getAdapter().notifyDataSetChanged();
        contactEmpty.setVisibility(contactCursor.getCount() == 0 ? View.VISIBLE : View.INVISIBLE);

        if (requestCursor != null) {
            requestCursor.close();
            requestCursor = null;
        }
        requestCursor = db.getReadableDatabase().query("contacts", new String[]{"address", "name"}, "incoming!=0", null, null, null, "name, address");
        requestRecycler.getAdapter().notifyDataSetChanged();
        requestEmpty.setVisibility(requestCursor.getCount() == 0 ? View.VISIBLE : View.INVISIBLE);


        //updateBadge();

        int newRequests = requestCursor.getCount();
        ((TextView) tabLayout.getTabAt(1).getCustomView().findViewById(R.id.badge)).setText(newRequests > 0 ? "" + newRequests : "");

    }

    /*void updateBadge() {
        int newRequests = db.getNewRequests();
        ((TextView)tabLayout.getTabAt(1).getCustomView().findViewById(R.id.badge)).setText(newRequests > 0 ? "" + newRequests : "");
    }*/

    void contactLongPress(final String address, final String name) {
        View v = getLayoutInflater().inflate(R.layout.dialog_contact, null);
        ((TextView) v.findViewById(R.id.name)).setText(name);
        ((TextView) v.findViewById(R.id.address)).setText(address);
        final Dialog dlg = new AlertDialog.Builder(MainActivity.this)
                .setView(v)
                .create();

        v.findViewById(R.id.openchat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.hide();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("chat:" + address), getApplicationContext(), ChatActivity.class));
            }
        });
        v.findViewById(R.id.changename).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.hide();
                changeContactName(address, name);
            }
        });
        v.findViewById(R.id.copyid).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.hide();
                ((android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setText(address);
                snack(getString(R.string.id_copied_to_clipboard) + address);
            }
        });
        v.findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.hide();
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.delete_contact_q)
                        .setMessage(String.format(getString(R.string.really_delete_contact), address))
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                db.removeContact(address);
                                updateContactList();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                //db.removeContact(address);
                //updateContactList();
            }
        });

        dlg.show();
    }


    /*void updateContactList() {
        ((ListView)findViewById(R.id.contacts)).setAdapter(new CursorAdapter(
                this,
                db.getReadableDatabase().query("contacts", null, null, null, null, null, "name, address")
        ) {
            @Override
            public void bindView(View view, final Context context, Cursor cursor) {

                final String address = cursor.getString(cursor.getColumnIndex("address"));

                String name = cursor.getString(cursor.getColumnIndex("name"));
                if(name == null || name.equals("")) name = "Anonymous";

                ((TextView)view.findViewById(R.id.address)).setText(address);
                ((TextView)view.findViewById(R.id.name)).setText(name);

                if(view.findViewById(R.id.accept) != null) {
                    view.findViewById(R.id.accept).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Database.getInstance(context).acceptContact(address);
                            update();
                        }
                    });
                }

                if(view.findViewById(R.id.decline) != null) {
                    view.findViewById(R.id.decline).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Database.getInstance(context).removeContact(address);
                            update();
                        }
                    });
                }
            }

            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                if(cursor.getInt(cursor.getColumnIndex("incoming")) != 0)
                    return LayoutInflater.from(context).inflate(R.layout.item_contact_request, parent, false);
                else
                    return LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false);
            }
        });
    }*/


    void changeContactName(final String address, final String name) {
        final FrameLayout view = new FrameLayout(this);
        final EditText editText = new EditText(this);
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        editText.setSingleLine();
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        view.addView(editText);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        ;
        view.setPadding(padding, padding, padding, padding);
        editText.setText(name);
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_change_alias)
                .setView(view)
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ContentValues v = new ContentValues();
                        v.put("name", editText.getText().toString());
                        db.getWritableDatabase().update("contacts", v, "address=?", new String[]{address});
                        update();
                        snack(getString(R.string.snack_alias_changed));
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    void update() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.myaddress)).setText(tor.getID());
                ((TextView) findViewById(R.id.myname)).setText(db.getName().trim().isEmpty() ? "Anonymous" : db.getName());
                updateContactList();
                //updateBadge();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Tor.getInstance(this).setListener(new Tor.Listener() {
            @Override
            public void onChange() {
                update();
                send();
            }
        });
        Server.getInstance(this).setListener(new Server.Listener() {
            @Override
            public void onChange() {
                update();
            }
        });
        update();
        send();

        Notifier.getInstance(this).onResumeActivity();

        ((TorStatusView) findViewById(R.id.torStatusView)).update();

        startService(new Intent(this, HostService.class));
    }

    @Override
    protected void onPause() {
        Notifier.getInstance(this).onPauseActivity();
        Tor.getInstance(this).setListener(null);
        Server.getInstance(this).setListener(null);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */

    /*String makeUrl() {
        return "chat.onion://"
    }*/

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        /*if (id == R.id.nav_camara) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/

        if (id == R.id.changealias) {
            changeName();
            return true;
        }

        if (id == R.id.qr_show) {
            showQR();
            return true;
        }

        if (id == R.id.qr_scan) {
            scanQR();
        }

        if (id == R.id.share_id) {
            inviteFriend();
        }

        if (id == R.id.copy_id) {
            ((android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setText(tor.getID());
            snack(getString(R.string.id_copied_to_clipboard) + tor.getID());
            return true;
        }

        if (id == R.id.rate) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
                PackageManager pm = getPackageManager();
                for (ApplicationInfo packageInfo : pm.getInstalledApplications(0)) {
                    if (packageInfo.packageName.equals("com.android.vending"))
                        intent.setPackage("com.android.vending");
                }
                startActivity(intent);
            } catch (Throwable t) {
                Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            }
        }

        if (id == R.id.about) {
            showAbout();
        }

        if (id == R.id.enter_id) {
            addContact();
        }

        if (id == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    void inviteFriend() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);

        String url = "https://play.google.com/store/apps/details?id=" + getPackageName();

        //intent.putExtra(Intent.EXTRA_TEXT, String.format("Add me on Chat.onion!\n\nID: %s\n\n%s", tor.getID(), url));

        //intent.putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.invitation_text), url,  tor.getID()));

        intent.putExtra(Intent.EXTRA_REFERRER, url);
        intent.putExtra("customAppUri", url);

        String msg = String.format(getString(R.string.invitation_text), url, tor.getID(), Uri.encode(db.getName()));

        Log.i("Message", msg.replace('\n', ' '));

        intent.putExtra(Intent.EXTRA_TEXT, msg);
        intent.setType("text/plain");

        /*intent.setType("text/html");
        intent.putExtra(
                Intent.EXTRA_TEXT,
                Html.fromHtml(new StringBuilder()
                        .append(String.format("<p><a href=\"%s\">%s</a></p>\n\n", url, url))
                        .append(String.format("<p>Let's chat securely via Chat.onion!</p>\n\n"))
                        .append(String.format("<p>My Chat.onion ID: %s</p>\n\n", tor.getID()))
                        .toString())
        );*/


        startActivity(intent);
    }

    void scanQR() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, REQUEST_QR);
    }

    void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_name))
                        //.setMessage(BuildConfig.APPLICATION_ID + "\n\nVersion: " + BuildConfig.VERSION_NAME)
                .setMessage("Version: " + BuildConfig.VERSION_NAME)
                .setNeutralButton(R.string.libraries, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showLibraries();
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    void showLibraries() {
        final String[] items;
        try {
            items = getResources().getAssets().list("licenses");
        } catch (IOException ex) {
            throw new Error(ex);
        }
        new AlertDialog.Builder(this)
                .setTitle("Third party software used in this app (click to view license)")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showLicense(items[which]);
                    }
                })
                .show();
    }

    void showLicense(String name) {
        String text;
        try {
            text = Utils.str(getResources().getAssets().open("licenses/" + name));
        } catch (IOException ex) {
            throw new Error(ex);
        }
        new AlertDialog.Builder(this)
                .setTitle(name)
                .setMessage(text)
                .show();
    }

    void showQR() {
        String name = db.getName();

        //String txt = "chat-onion://" + tor.getID();
        //if (!name.isEmpty()) txt += "/" + name;

        String txt = "chat.onion " + tor.getID() + " " + name;

        QRCode qr;

        try {
            //qr = Encoder.encode(txt, ErrorCorrectionLevel.H);
            qr = Encoder.encode(txt, ErrorCorrectionLevel.M);
        } catch (Exception ex) {
            throw new Error(ex);
        }

        ByteMatrix mat = qr.getMatrix();
        int width = mat.getWidth();
        int height = mat.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = mat.get(x, y) != 0 ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 8, bitmap.getHeight() * 8, false);

        ImageView view = new ImageView(this);
        view.setImageBitmap(bitmap);

        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        view.setPadding(pad, pad, pad, pad);

        Rect displayRectangle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
        int s = (int) (Math.min(displayRectangle.width(), displayRectangle.height()) * 0.9);
        view.setMinimumWidth(s);
        view.setMinimumHeight(s);
        new AlertDialog.Builder(this)
                //.setMessage(txt)
                .setView(view)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            return;
        if (requestCode == REQUEST_QR) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");

            int width = bitmap.getWidth(), height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            bitmap.recycle();
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultiFormatReader reader = new MultiFormatReader();

            /*try {
                Result result = reader.decode(bBitmap);
                String id = result.getText();
                Log.i("ID", id);
                if(id.length() == 16) {
                    if (!db.addContact(id, true, false)) {
                        snack("Failed to add contact");
                        return;
                    }
                    snack("Contact added");
                    updateContactList();
                    send();
                    return;
                }
            } catch(NotFoundException ex) {
                ex.printStackTrace();
            }
            snack("QR Code Invalid");
            */


            try {
                Result result = reader.decode(bBitmap);
                String str = result.getText();
                Log.i("ID", str);

                String[] tokens = str.split(" ", 3);

                if (tokens.length < 2 || !tokens[0].equals("chat.onion")) {
                    snack(getString(R.string.qr_invalid));
                    return;
                }

                String id = tokens[1].toLowerCase();

                if (id.length() != 16) {
                    snack(getString(R.string.qr_invalid));
                    return;
                }

                if (db.hasContact(id)) {
                    snack(getString(R.string.contact_already_added));
                    return;
                }

                String name = "";
                if (tokens.length > 2) {
                    name = tokens[2];
                }

                addContact(id, name);

                return;

            } catch (Exception ex) {
                snack(getString(R.string.qr_invalid));
                ex.printStackTrace();
            }


        }
    }

    void snack(String s) {
        //Snackbar.make(findViewById(R.id.toolbar), s, Snackbar.LENGTH_SHORT).show();
        Snackbar.make(findViewById(R.id.drawer_layout), s, Snackbar.LENGTH_SHORT).show();
    }

    void changeName() {
        final FrameLayout view = new FrameLayout(this);
        final EditText editText = new EditText(this);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        editText.setSingleLine();
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        view.addView(editText);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        ;
        view.setPadding(padding, padding, padding, padding);
        editText.setText(db.getName());
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_change_alias)
                .setView(view)
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.setName(editText.getText().toString().trim());
                        update();
                        snack(getString(R.string.snack_alias_changed));
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    void addContact() {
        addContact("", "");
    }

    void addContact(String id, String alias) {

        final View view = getLayoutInflater().inflate(R.layout.dialog_add, null);
        final EditText idEd = (EditText) view.findViewById(R.id.add_id);
        idEd.setText(id);
        final EditText aliasEd = (EditText) view.findViewById(R.id.add_alias);
        aliasEd.setText(alias);
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_add_contact)
                .setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String id = idEd.getText().toString().trim();
                        if (id.length() != 16) {
                            snack(getString(R.string.invalid_id));
                            return;
                        }
                        if (id.equals(tor.getID())) {
                            snack(getString(R.string.cant_add_self));
                            return;
                        }
                        if (!db.addContact(id, true, false, aliasEd.getText().toString().trim())) {
                            snack(getString(R.string.failed_to_add_contact));
                            return;
                        }
                        snack(getString(R.string.contact_added));
                        updateContactList();
                        send();
                        tabLayout.getTabAt(0).select();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();

    }

    class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView address, name;
        View accept, decline;
        View badge;
        TextView count;

        public ContactViewHolder(View view) {
            super(view);
            address = (TextView) view.findViewById(R.id.address);
            name = (TextView) view.findViewById(R.id.name);
            accept = view.findViewById(R.id.accept);
            decline = view.findViewById(R.id.decline);
            badge = view.findViewById(R.id.badge);
            if (badge != null) count = (TextView) view.findViewById(R.id.count);
        }
    }


}
