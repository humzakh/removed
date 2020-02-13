package com.humzaman.removed;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.commonmark.node.Node;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import io.noties.markwon.Markwon;

public class HandleLinkActivity extends AppCompatActivity {

    private static final String TAG = "HandleLinkActivity";
    // [author, body, score, id, permalink, created_utc, retrieved_on, subreddit, subreddit_id, link_id, parent_id, author_fullname]
    private List<String> parsedData;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        parsedData = new ArrayList<>();
        handleLink();
    }

    private void handleLink() {
        Intent intent = getIntent();
        String intentString = intent.getStringExtra(intent.EXTRA_TEXT);
        Log.i(TAG, "Intent string: " + intentString);

        if (URLUtil.isValidUrl(intentString)) {
            Uri validUrl = Uri.parse(intentString);
            String host = validUrl.getHost();

            if (host.contains("reddit.com")) {
                List<String> pathSegments = validUrl.getPathSegments();

                // URL structure: reddit.com/r/{subreddit}/comments/{submission id}/{submission title}/{comment id}/
                if (pathSegments.size() == 5) { // submission
                    String id = pathSegments.get(3);
                    Log.i(TAG, "Submission ID: " + id);

                    // TODO: submission stuff
                    // I'll work on this later.
                    // ...maybe

                    displayAlert(-1);
                }
                else if (pathSegments.size() == 6) { // comment
                    String id = pathSegments.get(5);
                    Log.i(TAG, "Comment ID: " + id);
                    String pushshiftUrl = "https://api.pushshift.io/reddit/search/comment/?ids=" + id;

                    new FetchDataTask().execute(pushshiftUrl);
                    // displayAlert(0) is called in onPostExecute()
                }
                else {
                    Log.i(TAG, "Not a valid submission or comment link.");
                    displayAlert(1);
                }
            }
            else {
                Log.i(TAG, "Not a reddit link. (" + host + ")");
                displayAlert(2);
            }
        }
        else {
            Log.i(TAG, "Not a valid URL.");
            displayAlert(3);
        }
    }

    private void displayAlert(int code) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (code == -1) {
            builder.setMessage("Submission links are not currently supported.\n\nTry again with a direct link to a comment.")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    })
                    .setTitle("[removed]");
        }
        else if (code == 0) {
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.alert_view, null);
            View dialogTitle = inflater.inflate(R.layout.alert_title, null);

            builder.setView(dialogView)
                    .setCancelable(false)
                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    })
                    .setNeutralButton("Copy Text", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("[removed]", parsedData.get(1));
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(getApplicationContext(),"Copied to clipboard", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .setCustomTitle(dialogTitle);

            Toolbar toolbar = dialogTitle.findViewById(R.id.toolbar);
            // Set an OnMenuItemClickListener to handle menu item clicks
            toolbar.setOnMenuItemClickListener(
                    new Toolbar.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {

                            switch (item.getItemId()) {
                                case R.id.view_on_reddit: {
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(parsedData.get(4)));
                                    startActivity(browserIntent);
                                    break;
                                }
                                case R.id.more_details: {
                                    displayAlert(-9);
                                    break;
                                }
                                case R.id.settings: {

                                    break;
                                }

                            }


                            return true;
                        }
                    });

            toolbar.inflateMenu(R.menu.alert_overflow);

            TextView authorTV = dialogView.findViewById(R.id.authorTV);
            TextView timeTV = dialogView.findViewById(R.id.timeTV);
            TextView bodyTV = dialogView.findViewById(R.id.bodyTV);

            String author = "/u/" + parsedData.get(0);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String time = sdf.format(new Date(Long.parseLong(parsedData.get(5))*1000)) + " (UTC)";

            authorTV.setText(author);
            timeTV.setText(time);

            // obtain an instance of Markwon
            final Markwon markwon = Markwon.create(this);

            // parse markdown to commonmark-java Node
            final Node node = markwon.parse(parsedData.get(1));

            // create styled text from parsed Node
            final Spanned markdown = markwon.render(node);

            // use it on a TextView
            markwon.setParsedMarkdown(bodyTV, markdown);
        }
        else if (code == -9) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            String note = "NOTE: Due to Pushshift's nature as an archive tool, information displayed here may not accurately reflect what is currently displayed on reddit.\n" +
                    "(see https://reddit.com/bcxguf for more info)\n\n\n";

            String message =
                    "Author: /u/" + parsedData.get(0) + "\n" +
                    "Score: " + parsedData.get(2) + " " + ((Integer.valueOf(parsedData.get(2)) == 1) ? "point" : "points") + "\n" +
                    "Subreddit: /r/" + parsedData.get(7) + "\n\n" +
                    "Submitted (UTC): " + sdf.format(new Date(Long.parseLong(parsedData.get(5))*1000)) + "\n" +
                    "Archived (UTC): " + sdf.format(new Date(Long.parseLong(parsedData.get(6))*1000)) + "\n\n" +
                    "Author ID: " + parsedData.get(11) + "\n" +
                    "Comment ID: t1_" + parsedData.get(3) + "\n" +
                    "Parent ID: " + parsedData.get(10) + "\n" +
                    "Subreddit ID: " + parsedData.get(8);

            SpannableString str = new SpannableString(note + message);
            str.setSpan(new StyleSpan(Typeface.BOLD), 0, note.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            str.setSpan(new RelativeSizeSpan(0.8f), 0, note.length(), 0);

            builder.setMessage(str)
                    .setCancelable(false)
                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    })
                    .setTitle("More details");
        }
        else {
            builder.setMessage("Invalid link")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    })
                    .setTitle("[removed]");
        }

        AlertDialog alert = builder.create();
        alert.show();
    }

    private class FetchDataTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(HandleLinkActivity.this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Unremoving...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setProgress(0);
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    cancel(true);
                    finish();
                }
            });
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {

            String result;
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(params[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = urlConnection.getInputStream();

                // convert inputstream to string
                if(inputStream != null) {
                    result = convertInputStreamToString(inputStream);
                    Log.i(TAG, "Data received: " + result);
                }
                else
                    result = "Failed to fetch data";

                return result;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String dataFetched) {
            //parse the JSON data and then display
            parseJSON(dataFetched);

            progressDialog.dismiss();
            displayAlert(0);
        }

        private String convertInputStreamToString (InputStream inputStream) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            String result = "";
            while ((line = bufferedReader.readLine()) != null)
                result += line;

            inputStream.close();
            return result;
        }

        private void parseJSON(String data) {

            try {
                JSONObject mainObject = new JSONObject(data);
                Log.i(TAG, "Main object: " + mainObject.toString());
                JSONArray dataArray = mainObject.getJSONArray("data");
                Log.i(TAG, "data object: " + dataArray.toString());

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);

                    parsedData.add(item.getString("author")); // 0
                    parsedData.add(item.getString("body")); // 1
                    parsedData.add(item.getString("score")); // 2
                    parsedData.add(item.getString("id")); // 3
                    parsedData.add("https://reddit.com" + item.getString("permalink")); // 4
                    parsedData.add(item.getString("created_utc")); // 5
                    parsedData.add(item.getString("retrieved_on")); // 6
                    parsedData.add(item.getString("subreddit")); // 7
                    parsedData.add(item.getString("subreddit_id")); // 8
                    parsedData.add(item.getString("link_id")); // 9
                    parsedData.add(item.getString("parent_id")); // 10
                    parsedData.add(item.getString("author_fullname")); // 11
                }

                Log.i(TAG, parsedData.toString());
            } catch(Exception e) {
                Log.i(TAG, "Error parsing data: " + e.getMessage());
            }
        }
    }
}

