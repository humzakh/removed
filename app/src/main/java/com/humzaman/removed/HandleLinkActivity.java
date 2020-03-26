package com.humzaman.removed;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.text.format.DateUtils;
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
import androidx.core.text.HtmlCompat;
import androidx.core.view.MenuCompat;

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
    private String id;
    private String pushshiftUrl;
    private FetchDataTask fdt; // lol this variable name is excellent. #FDT
    // parsedData: [author, body, score, id, permalink, created_utc, retrieved_on, subreddit, subreddit_id, link_id, parent_id, author_fullname]
    private List<String> parsedData;
    private ProgressDialog progressDialog;
    private enum ResultCode {
        SUBMISSION,
        VALID_COMMENT,
        INVALID_COMMENT,
        NOT_REDDIT_LINK,
        NOT_URL,
        MORE_DETAILS,
        ABOUT,
        FAILED,
        NO_INTERNET,
        NO_DATA_FOUND,
        ERROR_RESPONSE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            activity = this;
            intentString = intent.getStringExtra(Intent.EXTRA_TEXT);
            handleLink();
        }
    }

    @Override
    protected void onDestroy() { // handle activity destruction issues (screen rotation)
        super.onDestroy();

        if (progressDialog != null) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();
            progressDialog = null;
        }

        if (fdt != null && !fdt.isCancelled()) {
            fdt.cancel(true);
        }
    }

    private void handleLink() {
        Log.i(TAG, "Intent string: " + intentString);

        if (URLUtil.isValidUrl(intentString)) {
            Uri validUrl = Uri.parse(intentString);
            String host = validUrl.getHost();

            if (host != null && host.contains("reddit.com")) {
                List<String> pathSegments = validUrl.getPathSegments();

                // URL structure: reddit.com/r/{subreddit}/comments/{submission id}/{submission title}/{comment id}/
                if (pathSegments.size() == 5) { // submission
                    id = pathSegments.get(3);
                    Log.i(TAG, "Submission ID: " + id);

                    // TODO: submission stuff
                    // I'll work on this later.
                    // ...maybe

                    displayAlert(ResultCode.SUBMISSION, null);
                }
                else if (pathSegments.size() == 6) { // comment
                    id = pathSegments.get(5);
                    Log.i(TAG, "Comment ID: " + id);
                    pushshiftUrl = "https://api.pushshift.io/reddit/search/comment/?ids=" + id;
                    Log.i(TAG, "Pushshift URL: " + pushshiftUrl);

                    fdt = new FetchDataTask();
                    fdt.execute(pushshiftUrl);
                    // displayAlert(ResultCode.VALID_COMMENT) will be called in onPostExecute()
                }
                else if (pathSegments.get(0).equals("comments")) {
                    if (pathSegments.size() == 4) { // reddit.com/comments/{submission id}/{submission title}/{comment id}/
                        id = pathSegments.get(3);
                        Log.i(TAG, "Comment ID: " + id);
                        pushshiftUrl = "https://api.pushshift.io/reddit/search/comment/?ids=" + id;
                        Log.i(TAG, "Pushshift URL: " + pushshiftUrl);

                        fdt = new FetchDataTask();
                        fdt.execute(pushshiftUrl);
                        // displayAlert(ResultCode.VALID_COMMENT) will be called in onPostExecute()
                    }
                    else {
                        id = pathSegments.get(1);
                        Log.i(TAG, "Submission ID: " + id);
                        displayAlert(ResultCode.SUBMISSION, null);
                    }
                }
                else {
                    Log.e(TAG, "Not a valid comment link.");
                    displayAlert(ResultCode.INVALID_COMMENT, null);
                }
            }
            else {
                Log.e(TAG, "Not a reddit link. (" + host + ")");
                displayAlert(ResultCode.NOT_REDDIT_LINK, null);
            }
        }
        else {
            Log.e(TAG, "Not a valid URL.");
            displayAlert(ResultCode.NOT_URL, null);
        }
    }

    private void displayAlert(ResultCode code, String resultCode) {
        LayoutInflater inflater = getLayoutInflater();
        @SuppressLint("InflateParams") View dialogTitle = inflater.inflate(R.layout.alert_title, null);
        Toolbar toolbar = dialogTitle.findViewById(R.id.toolbar);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCustomTitle(dialogTitle)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                        finish();
                    }
                });

        switch (code) {
            case VALID_COMMENT: { // valid comment link
                 @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.alert_view, null);
                builder.setView(dialogView)
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
                if (removeddit.contains("old.reddit.com"))
                    removeddit = removeddit.replaceFirst("old[.]", "");
                final String finalRemoveddit = removeddit.replaceFirst("reddit", "removeddit");

                toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
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
                                    case R.id.more_details: {
                                        displayAlert(ResultCode.MORE_DETAILS, null);
                                        break;
                                    }
                                    case R.id.settings: { break; }
                                    case R.id.about: {
                                        displayAlert(ResultCode.ABOUT, null);
                                        break;
                                    }
                                }
                                return true;
                            }});
                toolbar.inflateMenu(R.menu.alert_overflow);
                MenuCompat.setGroupDividerEnabled(toolbar.getMenu(), true);

                TextView authorTV = dialogView.findViewById(R.id.authorTV);
                TextView timeTV = dialogView.findViewById(R.id.timeTV);
                TextView bodyTV = dialogView.findViewById(R.id.bodyTV);

                if (!parsedData.get(0).equals("[deleted]")) { // open user profile
                    authorTV.setMovementMethod(LinkMovementMethod.getInstance());
                    String html = "<a href='https://www.reddit.com/user/" + parsedData.get(0) + "'>/u/" + parsedData.get(0) + "</a>";
                    authorTV.setText(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY));
                }
                else
                    authorTV.setText(R.string.deleted);

                String time = (DateUtils.getRelativeDateTimeString(activity,
                        Long.parseLong(parsedData.get(5)) * 1000,
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.DAY_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE)).toString();
                timeTV.setText(time);

                // archived username [deleted] tells us the comment couldn't be archived in time
                if (parsedData.get(0).equals("[deleted]"))
                    bodyTV.setText(R.string.removed_quick);
                else {
                    bodyTV.setMovementMethod(LinkMovementMethod.getInstance()); // make links in body clickable

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
            case SUBMISSION: { // submission link
                String removeddit = intentString;
                if (removeddit.contains("old.reddit.com")) {
                    removeddit = removeddit.replaceFirst("old[.]", "");
                }
                final String finalRemoveddit = removeddit.replaceFirst("reddit", "removeddit");

                builder.setMessage("Submission links are not currently supported.\n\nTap \"Removeddit\" to view the submission on removeddit.com, or try again with a direct link to a comment.")
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
            case MORE_DETAILS: { // more details dialog
                toolbar.setTitle("More details");

                @SuppressLint("InflateParams") View mdDialogView = inflater.inflate(R.layout.alert_moredetails, null);

                builder.setView(mdDialogView)
                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                dialog.dismiss();
                            }
                        });

                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                String author = (parsedData.get(0).equals("[deleted]") ? "[deleted]" : "/u/" + parsedData.get(0));
                String score =  (parsedData.get(2).equals("null") ? "null" : parsedData.get(2) + " " + ((Integer.parseInt(parsedData.get(2)) == 1) ? "point" : "points"));
                String subreddit = "/r/" + parsedData.get(7);
                String submitted = (parsedData.get(5).equals("null") ? "null" : sdf.format(new Date(Long.parseLong(parsedData.get(5)) * 1000)));
                String archived = (parsedData.get(5).equals("null") ? "null" : sdf.format(new Date(Long.parseLong(parsedData.get(6)) * 1000)));
                String commentID = "t1_" + parsedData.get(3);
                String source = "Data source: " + pushshiftUrl;

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
            case ABOUT: { // about dialog
                builder.setCustomTitle(null)
                        .setTitle("About [removed]")
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
                            }
                        });
                break;
            }

            /* Error result codes */
            case NOT_URL: // not a URL
                builder.setMessage("Error: not a valid URL.\n\nPlease share a direct link, not the comment text.");
                break;
            case FAILED: // failed to retrieve data
                builder.setMessage("Error: failed to retrieve data from pushshift.io");
                break;
            case NO_INTERNET: // no internet
                builder.setMessage("Error: check internet connection.");
                break;
            case NO_DATA_FOUND: // no data found on Pushshift
                builder.setMessage(R.string.not_archived);
                break;
            case ERROR_RESPONSE: // Pushshift error response
                builder.setMessage("Error " + resultCode + ": Could not reach Pushshift.\n\nTheir servers may be down.\nCheck pushshift.io for updates, or try again later.")
                        .setNeutralButton("Pushshift.io", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://pushshift.io"));
                                startActivity(browserIntent);
                                dialog.dismiss();
                                finish();
                            }
                        });
                break;
            default: // invalid link
                builder.setMessage("Error: invalid link.");
                break;
        }

        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }

    // idk the right thing to do about this memory leak warning,
    // so I'm just gonna ignore it and hope nothing bad happens lol
    // Bad Coding Practices: Exhibit A
    @SuppressLint("StaticFieldLeak")
    private class FetchDataTask extends AsyncTask<String, Void, List<String>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(TAG, "onPreExecute() Showing ProgressDialog");
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
        protected List<String> doInBackground(String... params) {
            Log.i(TAG, "doInBackground() Fetching data.");
            List<String> result = new ArrayList<>();
            HttpURLConnection urlConnection = null;
            HttpURLConnection urlConnectionScore = null;

            try {
                URL url = new URL(params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                int pushshiftResponseCode = urlConnection.getResponseCode();

                if (pushshiftResponseCode == HttpURLConnection.HTTP_OK) {
                    Log.i(TAG, "Response from Pushshift: " + pushshiftResponseCode);
                    InputStream inputStream = urlConnection.getInputStream();

                    // convert inputStream to string
                    if (inputStream != null) {
                        result.add(convertInputStreamToString(inputStream)); // 0
                        Log.i(TAG, "Pushshift data received: " + result.get(0));

                        // get current comment score from reddit
                        URL urlScore = new URL("https://api.reddit.com/api/info/?id=t1_" + id);
                        urlConnectionScore = (HttpURLConnection) urlScore.openConnection();
                        int redditResponseCode = urlConnectionScore.getResponseCode();

                        if (redditResponseCode == HttpURLConnection.HTTP_OK) {
                            Log.i(TAG, "Response from reddit: " + redditResponseCode);
                            InputStream inputStreamScore = urlConnectionScore.getInputStream();

                            if (inputStreamScore != null) {
                                result.add(convertInputStreamToString(inputStreamScore)); // 1
                                Log.i(TAG, "reddit data received: " + result.get(1));
                            }
                            else {
                                Log.e(TAG, "Failed to retrieve data from reddit.");
                                result.add("FAILED"); // 1
                            }
                        }
                        else {
                            Log.e(TAG, "Response from reddit: " + redditResponseCode);
                            result.add("FAILED"); // 1
                        }
                    }
                    else {
                        Log.e(TAG, "Failed to retrieve data from Pushshift.");
                        result.add("FAILED"); // 0
                    }

                    return result;
                }
                else {
                    Log.e(TAG, "Response from Pushshift: " + pushshiftResponseCode);
                    result.add("ERROR"); // 0
                    result.add(String.valueOf(pushshiftResponseCode)); // 1
                    return result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }

                if (urlConnectionScore != null) {
                    urlConnectionScore.disconnect();
                }
            }

            Log.e(TAG, "Url connection error");
            return null;
        }

        @Override
        protected void onPostExecute(List<String> dataFetched) {
            Log.i(TAG, "onPostExecute()");

            if (dataFetched == null) {
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
                displayAlert(ResultCode.NO_INTERNET, null);
            }
            else if (dataFetched.get(0).equals("FAILED")) {
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
                displayAlert(ResultCode.FAILED, null);
            }
            else if (dataFetched.get(0).equals("ERROR")) {
                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();
                displayAlert(ResultCode.ERROR_RESPONSE, dataFetched.get(1));
            }
            else {
                //parse the JSON data and then display
                parseJSON(dataFetched);

                if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();

                if (parsedData.size() != 0)
                    displayAlert(ResultCode.VALID_COMMENT, null);
                else {
                    Log.e(TAG, "No data found on pushshift.");
                    displayAlert(ResultCode.NO_DATA_FOUND, null);
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

        private void parseJSON(List<String> data) {
            try {
                JSONArray pushshiftDataArray = (new JSONObject(data.get(0))).getJSONArray("data");
                Log.i(TAG, "Pushshift data array: " + pushshiftDataArray.toString());

                parsedData = new ArrayList<>();

                if (pushshiftDataArray.length() > 0) {
                    JSONObject pushshiftObject = pushshiftDataArray.getJSONObject(0);

                    parsedData.add(pushshiftObject.has("author") ? pushshiftObject.getString("author") : "null");                   // 0
                    parsedData.add(pushshiftObject.has("body") ? pushshiftObject.getString("body") : "null");                       // 1
                    parsedData.add(pushshiftObject.has("score") ? pushshiftObject.getString("score") : "null");                     // 2
                    parsedData.add(pushshiftObject.has("id") ? pushshiftObject.getString("id") : "null");                           // 3
                    parsedData.add(pushshiftObject.has("permalink") ?                                                                     // 4
                            "https://www.reddit.com" + pushshiftObject.getString("permalink") : intentString);
                    parsedData.add(pushshiftObject.has("created_utc") ? pushshiftObject.getString("created_utc") : "null");         // 5
                    parsedData.add(pushshiftObject.has("retrieved_on") ? pushshiftObject.getString("retrieved_on") : "null");       // 6
                    parsedData.add(pushshiftObject.has("subreddit") ? pushshiftObject.getString("subreddit") : "null");             // 7
                    parsedData.add(pushshiftObject.has("subreddit_id") ? pushshiftObject.getString("subreddit_id") : "null");       // 8
                    parsedData.add(pushshiftObject.has("link_id") ? pushshiftObject.getString("link_id") : "null");                 // 9
                    parsedData.add(pushshiftObject.has("parent_id") ? pushshiftObject.getString("parent_id") : "null");             // 10
                    parsedData.add(pushshiftObject.has("author_fullname") ? pushshiftObject.getString("author_fullname") : "null"); // 11
                    Log.i(TAG, "Parsed Pushshift data: " + parsedData.toString());

                    // current comment score from reddit
                    if (!data.get(1).equals("FAILED")) {
                        JSONObject redditObject = (new JSONObject(data.get(1))).getJSONObject("data").getJSONArray("children").getJSONObject(0).getJSONObject("data");
                        String score = redditObject.getString("score");

                        Log.i(TAG, "Score from reddit: " + score);
                        parsedData.set(2, score);
                    }
                }
            } catch(Exception e) {
                Log.e(TAG, "Error parsing data: " + e.getMessage());
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.i(TAG, "FetchDataTask canceled.");
        }
    }
}

