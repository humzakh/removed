package com.humzaman.removed;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.commonmark.node.Node;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.noties.markwon.Markwon;

public class HandleLinkActivity extends AppCompatActivity {

    private static final String TAG = "HandleLinkActivity";
    private List<String> parsedData; // [author, body, score, id, permalink]
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
            String author = "/u/" + parsedData.get(0);
            String score = parsedData.get(2) + " " + ((Integer.valueOf(parsedData.get(2)) == 1) ? "point" : "points");

            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.alert_view, null);

            builder.setView(dialogView)
                    .setCancelable(false)
                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    })
                    .setNeutralButton("View on reddit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(parsedData.get(4)));
                            startActivity(browserIntent);
                            finish();
                        }
                    })
                    .setNegativeButton("Copy Text", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("[removed]", parsedData.get(1));
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(getApplicationContext(),"Copied to clipboard", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .setTitle("[removed]");

            TextView authorTV = dialogView.findViewById(R.id.authorTV);
            TextView scoreTV = dialogView.findViewById(R.id.scoreTV);
            TextView bodyTV = dialogView.findViewById(R.id.bodyTV);

            authorTV.setText(author);
            scoreTV.setText(score);

            // obtain an instance of Markwon
            final Markwon markwon = Markwon.create(this);

            // parse markdown to commonmark-java Node
            final Node node = markwon.parse(parsedData.get(1));

            // create styled text from parsed Node
            final Spanned markdown = markwon.render(node);

            // use it on a TextView
            markwon.setParsedMarkdown(bodyTV, markdown);
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

                    parsedData.add(item.getString("author"));
                    parsedData.add(item.getString("body"));
                    parsedData.add(item.getString("score"));
                    parsedData.add(item.getString("id"));
                    parsedData.add("https://reddit.com" + item.getString("permalink"));
                }

                Log.i(TAG, parsedData.toString());
            } catch(Exception e) {
                Log.i(TAG, "Error parsing data: " + e.getMessage());
            }
        }
    }
}

