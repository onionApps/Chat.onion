package onion.chat;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class SettingsActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getPrefs(this);

        setContentView(R.layout.prefs);

        //android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        //getFragmentManager().beginTransaction().replace(R.id.content_frame, new SettingsFragment()).commit();

        //addPreferencesFromResource(R.xml.prefs);

        //setContentView(new SettingsFragment().onCreateView(getLayoutInflater(), null, savedInstanceState));

        //setContentView(R.layout.prefs);

        getFragmentManager().beginTransaction().add(R.id.content, new SettingsFragment()).commit();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.prefs_menu, menu);
        return true;
    }

    void doreset() {

        Settings.getPrefs(SettingsActivity.this).edit().clear().commit();
        Settings.getPrefs(SettingsActivity.this);

        Intent intent = getIntent();
        finish();
        startActivity(intent);
        overridePendingTransition(0, 0);

        Snackbar.make(findViewById(R.id.content), "All settings reset", Snackbar.LENGTH_SHORT).show();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_reset) {

            doreset();

            /*new AlertDialog.Builder(this)
                    .setTitle(R.string.dlg_title_reset)
                    .setMessage(R.string.dlg_msg_reset)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            doreset();
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();

            return true;*/


        }

        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs);

            if(getPreferenceManager().findPreference("keyexport") != null) {
                getPreferenceManager().findPreference("keyexport").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Log.i("Private Key", Tor.getInstance(getActivity()).readPrivateKeyFile());
                        return true;
                    }
                });
            }
        }
    }

}
