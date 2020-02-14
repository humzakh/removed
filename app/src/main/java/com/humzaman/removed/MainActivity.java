package com.humzaman.removed;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void buttonClick(View view) {
        Intent intent = new Intent(this, HandleLinkActivity.class);
        intent.putExtra(Intent.EXTRA_TEXT,"https://www.reddit.com/r/removed_test/comments/f3mebi/title/fhjprlg/");

        startActivity(intent);
    }
}
