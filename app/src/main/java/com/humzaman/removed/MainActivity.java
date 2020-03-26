package com.humzaman.removed;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_about) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("About [removed]")
                    .setIcon(R.mipmap.ic_launcher)
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            dialog.dismiss();
                            finish();
                        }
                    })
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
        intent.putExtra(Intent.EXTRA_TEXT,"https://www.reddit.com/r/removed_test/comments/f3pm08/_/fhk7w3k/");

        // test link for [removed too quickly to be archived]
        //intent.putExtra(Intent.EXTRA_TEXT,"https://www.reddit.com/r/removed_test/comments/f3mebi/_/fis1ckk/");

        intent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(intent, null);
        startActivity(shareIntent);
        Toast.makeText(getApplicationContext(), "Choose [removed] from the list", Toast.LENGTH_LONG).show();
    }
}
