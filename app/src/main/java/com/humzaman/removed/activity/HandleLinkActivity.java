package com.humzaman.removed.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.URLUtil;

import androidx.appcompat.app.AppCompatActivity;

import com.humzaman.removed.network.FetchData;

import java.util.List;

public class HandleLinkActivity extends AppCompatActivity {
    private static final String TAG = "HandleLinkActivity";

    private Activity activity;
    private String intentString;
    private String id;
    private String pushshiftUrl;
    //private FetchDataTask fdt; // lol this variable name is excellent. #FDT
    // parsedData: [author, body, score, id, permalink, created_utc, retrieved_on, subreddit, subreddit_id, link_id, parent_id, author_fullname]
    private List<String> parsedData;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            this.activity = this;
            this.intentString = intent.getStringExtra(Intent.EXTRA_TEXT);
            Log.i(TAG, "Intent string: " + intentString);

            checkURL(intentString);
            new FetchData(id);
        }
    }

    private String checkURL(String intentString) {
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

                    //displayAlert(HandleLinkActivity.ResultCode.SUBMISSION, null);
                }
                else if (pathSegments.size() == 6) { // comment
                    id = pathSegments.get(5);
                    Log.i(TAG, "Comment ID: " + id);
                    pushshiftUrl = "https://api.pushshift.io/reddit/search/comment/?ids=" + id;
                    Log.i(TAG, "Pushshift URL: " + pushshiftUrl);

                    //fdt = new HandleLinkActivity.FetchDataTask();
                    //fdt.execute(pushshiftUrl);
                    //****** displayAlert(ResultCode.VALID_COMMENT) will be called in onPostExecute()
                }
                else if (pathSegments.get(0).equals("comments")) {
                    if (pathSegments.size() == 4) { // reddit.com/comments/{submission id}/{submission title}/{comment id}/
                        id = pathSegments.get(3);
                        Log.i(TAG, "Comment ID: " + id);
                        pushshiftUrl = "https://api.pushshift.io/reddit/search/comment/?ids=" + id;
                        Log.i(TAG, "Pushshift URL: " + pushshiftUrl);

                        //fdt = new HandleLinkActivity.FetchDataTask();
                        //fdt.execute(pushshiftUrl);
                        //******* displayAlert(ResultCode.VALID_COMMENT) will be called in onPostExecute()
                    }
                    else {
                        id = pathSegments.get(1);
                        Log.i(TAG, "Submission ID: " + id);
                        //displayAlert(HandleLinkActivity.ResultCode.SUBMISSION, null);
                    }
                }
                else {
                    Log.e(TAG, "Not a valid comment link.");
                    //displayAlert(HandleLinkActivity.ResultCode.INVALID_COMMENT, null);
                }
            }
            else {
                Log.e(TAG, "Not a reddit link. (" + host + ")");
                //displayAlert(HandleLinkActivity.ResultCode.NOT_REDDIT_LINK, null);
            }
        }
        else {
            Log.e(TAG, "Not a valid URL.");
            //displayAlert(HandleLinkActivity.ResultCode.NOT_URL, null);
        }
        return null;
    }


    @Override
    protected void onDestroy() { // handle activity destruction issues (screen rotation)
        super.onDestroy();

        if (progressDialog != null) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();
            progressDialog = null;
        }

        /*
        if (fdt != null && !fdt.isCancelled()) {
            fdt.cancel(true);
        }
        */
    }
}

