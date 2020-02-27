package com.humzaman.removed;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    SharedPreferences prefs;
    private static final String theme = "com.humzaman.removed.theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = this.getSharedPreferences("com.humzaman.removed", Context.MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(prefs.getInt(theme, AppCompatDelegate.MODE_NIGHT_NO));
        themeSetting();

        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void themeSetting() {
        AppCompatSpinner themeSpinner = this.findViewById(R.id.spinner);

        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                switch (i) {
                    case 0:
                        prefs.edit().putInt(theme, AppCompatDelegate.MODE_NIGHT_NO).apply();
                        AppCompatDelegate.setDefaultNightMode(prefs.getInt(theme, AppCompatDelegate.MODE_NIGHT_NO));
                        break;
                    case 1:
                        prefs.edit().putInt(theme, AppCompatDelegate.MODE_NIGHT_YES).apply();
                        AppCompatDelegate.setDefaultNightMode(prefs.getInt(theme, AppCompatDelegate.MODE_NIGHT_YES));
                        break;
                    case 2:
                        prefs.edit().putInt(theme, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM).apply();
                        AppCompatDelegate.setDefaultNightMode(prefs.getInt(theme, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // disable Follow System option below Q
            final List<String> list = new ArrayList<>(Arrays.asList(this.getResources().getStringArray(R.array.app_theme)));
            final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item,list) {
                @Override
                public boolean isEnabled(int position){
                    return position != 2;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View view = super.getDropDownView(position, convertView, parent);
                    TextView tv = (TextView) view;
                    if (position == 2)
                        tv.setTextColor(Color.GRAY);
                    return view;
                }
            };
            themeSpinner.setAdapter(spinnerArrayAdapter);
        }

        int t = prefs.getInt(theme, AppCompatDelegate.MODE_NIGHT_NO);
        themeSpinner.setSelection((t == -1) ? 2 : t - 1);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (id == R.id.action_about) {
            builder.setTitle("About [removed]")
                    .setIcon(R.mipmap.ic_launcher)
                    .setView(R.layout.alert_about)
                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            dialog.dismiss();
                            //finish();
                        }
                    });

            AlertDialog alert = builder.create();
            alert.setCanceledOnTouchOutside(false);
            alert.show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void buttonClick(View view) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT,"https://www.reddit.com/r/removed_test/comments/f3mebi/title/fhk7w3k/");

        // test link for [removed too quickly to be archived]
        //intent.putExtra(Intent.EXTRA_TEXT,"https://www.reddit.com/r/removed_test/comments/f3mebi/title/fis1ckk/");

        intent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(intent, null);
        startActivity(shareIntent);
        Toast.makeText(getApplicationContext(), "Choose [removed] from the list", Toast.LENGTH_LONG).show();
    }
}
