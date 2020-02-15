package com.humzaman.removed;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
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
    private Activity activity;
    private String intentString;
    // [author, body, score, id, permalink, created_utc, retrieved_on, subreddit, subreddit_id, link_id, parent_id, author_fullname]
    private List<String> parsedData;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //android O fix bug orientation
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        activity = this;

        handleLink();
    }

    private void handleLink() {
        Intent intent = getIntent();
        intentString = intent.getStringExtra(Intent.EXTRA_TEXT);
        Log.i(TAG, "Intent string: " + intentString);

        if (URLUtil.isValidUrl(intentString)) {
            Uri validUrl = Uri.parse(intentString);
            String host = validUrl.getHost();

            if (host != null && host.contains("reddit.com")) {
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
                    Log.i(TAG, "Pushshift URL: " + pushshiftUrl);

                    new FetchDataTask().execute(pushshiftUrl);
                    // displayAlert(0) is called in onPostExecute()
                }
                else if (pathSegments.get(0).equals("comments")) {
                    if (pathSegments.size() == 4) {
                        String id = pathSegments.get(3);
                        Log.i(TAG, "Comment ID: " + id);
                        String pushshiftUrl = "https://api.pushshift.io/reddit/search/comment/?ids=" + id;
                        Log.i(TAG, "Pushshift URL: " + pushshiftUrl);

                        new FetchDataTask().execute(pushshiftUrl);
                        // displayAlert(0) is called in onPostExecute()
                    }
                    else {
                        String id = pathSegments.get(1);
                        Log.i(TAG, "Submission ID: " + id);
                        displayAlert(-1);
                    }
                }
                else {
                    Log.e(TAG, "Not a valid comment link.");
                    displayAlert(-2);
                }
            }
            else {
                Log.e(TAG, "Not a reddit link. (" + host + ")");
                displayAlert(-3);
            }
        }
        else {
            Log.e(TAG, "Not a valid URL.");
            displayAlert(-4);
        }
    }

    private void displayAlert(int code) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        switch (code) {
            case 0: { // valid comment link
                LayoutInflater inflater = getLayoutInflater();
                @SuppressLint("InflateParams") View dialogTitle = inflater.inflate(R.layout.alert_title, null);
                @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.alert_view, null);

                builder.setCustomTitle(dialogTitle)
                        .setView(dialogView)
                        .setCancelable(false)
                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .setNeutralButton("Copy Text", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("[removed]", parsedData.get(1));
                                if (clipboard != null)
                                    clipboard.setPrimaryClip(clip);
                                Toast.makeText(getApplicationContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });

                String removeddit = intentString;
                if (removeddit.contains("old.reddit.com")) {
                    removeddit = removeddit.replaceFirst("old[.]", "");
                }
                final String finalRemoveddit = removeddit.replaceFirst("reddit", "removeddit");

                Toolbar toolbar = dialogTitle.findViewById(R.id.toolbar);
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
                                    case R.id.view_on_removeddit: {
                                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalRemoveddit));
                                        startActivity(browserIntent);
                                        break;
                                    }
                                    case R.id.more_details:
                                        displayAlert(1);
                                        break;

                                    case R.id.settings: {

                                        break;
                                    }

                                    case R.id.about: {
                                        displayAlert(7);
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
                bodyTV.setMovementMethod(LinkMovementMethod.getInstance()); // make links clickable

                String author = "/u/" + parsedData.get(0);
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String time = sdf.format(new Date(Long.parseLong(parsedData.get(5)) * 1000));

                authorTV.setText(author);
                timeTV.setText(time);

                // if the comment was removed to quickly to be archived,
                // disable the More Details option.
                // Pushshift will return the author as [deleted], so that's our check.
                if (parsedData.get(0).equals("[deleted]")) {
                    toolbar.getMenu().getItem(2).setEnabled(false);
                    bodyTV.setText(R.string.removed_quick);
                }
                else {
                    toolbar.getMenu().getItem(2).setEnabled(true);

                    // reddit comments use markdown format.
                    // obtain an instance of Markwon
                    final Markwon markwon = Markwon.create(activity);
                    // parse markdown to commonmark-java Node
                    final Node node = markwon.parse(parsedData.get(1));
                    // create styled text from parsed Node
                    final Spanned markdown = markwon.render(node);
                    // use it on a TextView
                    markwon.setParsedMarkdown(bodyTV, markdown);
                }

                break;
            }

            case 1: { // more details dialog
                LayoutInflater inflater = getLayoutInflater();
                @SuppressLint("InflateParams") View mdDialogView = inflater.inflate(R.layout.alert_moredetails, null);

                builder.setView(mdDialogView)
                        .setCancelable(false)
                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        })
                        .setTitle("More details");

                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                String author = "/u/" + parsedData.get(0);
                String score =  parsedData.get(2) + " " + ((Integer.valueOf(parsedData.get(2)) == 1) ? "point" : "points");
                String subreddit = "/r/" + parsedData.get(7);
                String submitted = sdf.format(new Date(Long.parseLong(parsedData.get(5)) * 1000));
                String archived = sdf.format(new Date(Long.parseLong(parsedData.get(6)) * 1000));
                String commentID = "t1_" + parsedData.get(3);
                String source = "Data retrieved from https://api.pushshift.io/reddit/search/comment/?ids=" + parsedData.get(3);

                TextView noteTV = mdDialogView.findViewById(R.id.md_note_tv);
                noteTV.setMovementMethod(LinkMovementMethod.getInstance());

                TextView authorTV = mdDialogView.findViewById(R.id.md_author_tv);
                authorTV.setText(author);
                TextView scoreTV = mdDialogView.findViewById(R.id.md_score_tv);
                scoreTV.setText(score);
                TextView subredditTV = mdDialogView.findViewById(R.id.md_sub_tv);
                subredditTV.setText(subreddit);

                TextView submittedTV = mdDialogView.findViewById(R.id.md_submitted_tv);
                submittedTV.setText(submitted);
                TextView archivedTV = mdDialogView.findViewById(R.id.md_archived_tv);
                archivedTV.setText(archived);

                TextView authorIdTV = mdDialogView.findViewById(R.id.md_authorID_tv);
                authorIdTV.setText(parsedData.get(11));
                TextView commentIdTV = mdDialogView.findViewById(R.id.md_commentID_tv);
                commentIdTV.setText(commentID);
                TextView parentIdTV = mdDialogView.findViewById(R.id.md_parentID_tv);
                parentIdTV.setText(parsedData.get(10));
                TextView linkIdTV = mdDialogView.findViewById(R.id.md_linkID_tv);
                linkIdTV.setText(parsedData.get(9));
                TextView subIdTV = mdDialogView.findViewById(R.id.md_subID_tv);
                subIdTV.setText(parsedData.get(8));

                TextView sourceTV = mdDialogView.findViewById(R.id.md_source_tv);
                sourceTV.setText(source);

                break;
            }

            case 7: { // about dialog

                builder.setView(R.layout.alert_about)
                        .setCancelable(false)
                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        })
                        .setTitle("About")
                        .setIcon(R.mipmap.ic_launcher);

                break;
            }

            case -1: { // submission link
                String removeddit = intentString;
                if (removeddit.contains("old.reddit.com")) {
                    removeddit = removeddit.replaceFirst("old[.]", "");
                }
                final String finalRemoveddit = removeddit.replaceFirst("reddit", "removeddit");

                builder.setMessage("Submission links are not currently supported.\n\nTap \"Removeddit\" to view the submission on removeddit.com, or try again with a direct link to a comment.")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .setNeutralButton("Removeddit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalRemoveddit));
                                startActivity(browserIntent);

                                dialog.dismiss();
                                finish();
                            }
                        });
                break;
            }

            case -5: // failed to retrieve data
                builder.setMessage("Error: Failed to retrieve data from pushshift.io")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .setTitle("[removed]");
                break;

            case -6: // No internet
                builder.setMessage("Error: Check internet connection")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .setTitle("[removed]");
                break;

            case -7: // No data found on pushshift
                builder.setMessage("Error: No data found for this comment on pushshift.io")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .setTitle("[removed]");
                break;

            default: // invalid link
                builder.setMessage("Error: Invalid link")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .setTitle("[removed]");
                break;
        }

        AlertDialog alert = builder.create();
        alert.show();
    }

    // idk the right thing to do about this memory leak warning,
    // so I'm just gonna ignore it and hope nothing bad happens lol
    private class FetchDataTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(activity);
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

                // convert inputStream to string
                if (inputStream != null) {
                    result = convertInputStreamToString(inputStream);
                    Log.i(TAG, "Data received: " + result);
                }
                else {
                    result = "FAILED";
                    Log.e(TAG, "Failed to retrieve data.");
                }

                return result;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            Log.e(TAG, "Url connection error");
            return null;
        }

        @Override
        protected void onPostExecute(String dataFetched) {
            if (dataFetched == null) {
                progressDialog.dismiss();
                displayAlert(-6);
            }
            else if (dataFetched.equals("FAILED")) {
                progressDialog.dismiss();
                displayAlert(-5);
            }
            else {
                //parse the JSON data and then display
                parseJSON(dataFetched);

                progressDialog.dismiss();

                if (parsedData.size() != 0)
                    displayAlert(0);
                else {
                    Log.e(TAG, "No data found on pushshift.");
                    displayAlert(-7);
                }

            }
        }

        private String convertInputStreamToString (InputStream inputStream) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null)
                result.append(line);

            inputStream.close();
            return result.toString();
        }

        private void parseJSON(String data) {
            try {
                JSONObject mainObject = new JSONObject(data);
                Log.i(TAG, "Main object: " + mainObject.toString());
                JSONArray dataArray = mainObject.getJSONArray("data");
                Log.i(TAG, "Data array: " + dataArray.toString());

                parsedData = new ArrayList<>();

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);

                    parsedData.add(item.has("author") ? item.getString("author") : "null");                   // 0
                    parsedData.add(item.has("body") ? item.getString("body") : "null");                       // 1
                    parsedData.add(item.has("score") ? item.getString("score") : "null");                     // 2
                    parsedData.add(item.has("id") ? item.getString("id") : "null");                           // 3
                    parsedData.add(item.has("permalink") ?
                            "https://www.reddit.com" + item.getString("permalink") : intentString);                  // 4
                    parsedData.add(item.has("created_utc") ? item.getString("created_utc") : "0000000000");   // 5
                    parsedData.add(item.has("retrieved_on") ? item.getString("retrieved_on") : "0000000000"); // 6
                    parsedData.add(item.has("subreddit") ? item.getString("subreddit") : "null");             // 7
                    parsedData.add(item.has("subreddit_id") ? item.getString("subreddit_id") : "null");       // 8
                    parsedData.add(item.has("link_id") ? item.getString("link_id") : "null");                 // 9
                    parsedData.add(item.has("parent_id") ? item.getString("parent_id") : "null");             // 10
                    parsedData.add(item.has("author_fullname") ? item.getString("author_fullname") : "null"); // 11
                }
                Log.i(TAG, "Parsed data: " + parsedData.toString());
            } catch(Exception e) {
                Log.e(TAG, "Error parsing data: " + e.getMessage());
            }
        }
    }
}

