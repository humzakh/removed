package com.humzaman.removed;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void buttonClick(View view) {
        Intent intent = new Intent(this, HandleLinkActivity.class);
        intent.putExtra(intent.EXTRA_TEXT,"https://www.reddit.com/r/AskHistorians/comments/f2d91n/in_1986_microsoft_windows_advertising_stipulated/fhc3z72/");

        startActivity(intent);
    }
}
