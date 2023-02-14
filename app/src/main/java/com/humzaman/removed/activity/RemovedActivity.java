package com.humzaman.removed.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.humzaman.removed.model.CommentData;
import com.humzaman.removed.model.RemovedViewModel;
import com.humzaman.removed.network.FetchData;
import com.humzaman.removed.network.FetchDataCallback;
import com.humzaman.removed.util.BuildAlert;
import com.humzaman.removed.util.CheckURL;
import com.humzaman.removed.util.ProgressBottomSheetDialogFragment;
import com.humzaman.removed.util.ResultCode;
import com.humzaman.removed.util.UnremovedBottomSheetDialogFragment;

/*
 * RemovedActivity receives an intent, checks whether the intent is a valid reddit link,
 * then fetches archived comment data from Pushshift.
*/
public class RemovedActivity extends AppCompatActivity implements FetchDataCallback {
    private static final String TAG = "RemovedActivity";

    private RemovedViewModel viewModel;
    private UnremovedBottomSheetDialogFragment resultSheet;
    private ProgressBottomSheetDialogFragment progressSheet;
    private ProgressDialog progressDialog; // deprecated but whatever, Google's not my mom
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
    }

    @Override // handle activity destruction issues (e.g. screen rotation)
    protected void onDestroy() {
        super.onDestroy();

        removeProgressDialog();

        if (alertDialog != null) {
            if (alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
        }
    }

    private void initialize() {
        this.viewModel = new ViewModelProvider(this).get(RemovedViewModel.class);

        if (viewModel.throwable != null) {
            Log.i(TAG, "initialize: Already initialized, unknown error in FetchData.");
            buildAndShowError(viewModel.throwable);
        }
        else if (viewModel.commentData != null) {
            Log.i(TAG, "initialize: Already initialized and fetched data.");
            buildAndShowAlert(viewModel.resultCode);
        }
        else if (viewModel.resultCode == null) {
            Log.i(TAG, "initialize: Getting intentString and checking URL.");
            Intent intent = getIntent();

            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                viewModel.intentString = intent.getStringExtra(Intent.EXTRA_TEXT);
                Log.i(TAG, "intentString: " + viewModel.intentString);

                CheckURL checkURL = new CheckURL();
                String[] parsedURLdata = checkURL.check(viewModel.intentString);
                viewModel.id = parsedURLdata[0];
                viewModel.subreddit = parsedURLdata[1];
                viewModel.link_id = parsedURLdata[2];
                viewModel.resultCode = checkURL.getResultCode();

                if (viewModel.resultCode == ResultCode.VALID_COMMENT) {
                    Log.i(TAG, "initialize: intentString is a valid comment. Fetching data.");
                    (new FetchData(viewModel.id)).fetch(this);
                    showProgressDialog();
                }
                else {
                    Log.i(TAG, "initialize: resultCode: " + viewModel.resultCode.name());
                    buildAndShowAlert(viewModel.resultCode);
                }
            }
        }
        else if (viewModel.resultCode == ResultCode.VALID_COMMENT) {
            Log.i(TAG, "initialize: Already initialized and checked URL. Fetching data.");
            (new FetchData(viewModel.id)).fetch(this);
            showProgressDialog();
        }
        else if (viewModel.intentString != null) {
            Log.i(TAG, "initialize: Already initialized and checked URL. Showing " +
                    viewModel.resultCode.name() + " alertDialog.");
            buildAndShowAlert(viewModel.resultCode);
        }
    }

    private void buildAndShowAlert(ResultCode resultCode) {
        switch (resultCode) {
            case VALID_COMMENT: {
                Log.i(TAG, "buildAndShowAlert: Showing unremoved comment alertDialog.");
                this.alertDialog = (new BuildAlert(this, resultCode, viewModel.intentString, viewModel.commentData)).build();
                /*
                String relativeSubmissionTime = (DateUtils.getRelativeDateTimeString(this,
                        Long.parseLong(viewModel.commentData.getCreated_utc()) * 1000,
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.DAY_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE)).toString();

                this.resultSheet = new UnremovedBottomSheetDialogFragment(
                        viewModel.commentData.getAuthor(),
                        viewModel.commentData.getScore().equals("1") ? "1 point" : viewModel.commentData.getScore() + " points",
                        relativeSubmissionTime,
                        viewModel.commentData.getBody());
                 */
                break;
            }
            case SUBMISSION: {
                Log.i(TAG, "buildAndShowAlert: Showing submission alertDialog.");
                this.alertDialog = (new BuildAlert(this, resultCode, viewModel.intentString)).build();
                break;
            }
            case UNKNOWN_ERROR: {
                Log.i(TAG, "buildAndShowAlert: Showing error alertDialog.");
                this.alertDialog = (new BuildAlert(this, resultCode, viewModel.intentString)).build();
                break;
            }
            default: {
                Log.i(TAG, "buildAndShowAlert: Showing " + resultCode.name() + " alertDialog.");
                this.alertDialog = (new BuildAlert(this, resultCode)).build();
                break;
            }
        }
        removeProgressDialog();
        alertDialog.show();
        //resultSheet.show(getSupportFragmentManager(), "unremoved_bottomsheet_dialog");
    }
    
    private void buildAndShowError(Throwable throwable) {
        Log.i(TAG, "buildAndShowError: Showing UNKNOWN_ERROR alertDialog.");
        this.alertDialog = (new BuildAlert(this, viewModel.intentString, throwable)).build();
        removeProgressDialog();
        alertDialog.show();
    }

    private void showProgressSheet() {
        if (progressSheet == null) {
            progressSheet = new ProgressBottomSheetDialogFragment();
        }
        progressSheet.show(getSupportFragmentManager(), "progress_bottomsheet_dialog");
    }

    private void removeProgressSheet() {
        if (progressSheet != null) {
            if (progressSheet.getDialog().isShowing())
                progressSheet.dismiss();
            progressSheet = null;
        }
    }

    private void showProgressDialog() {
        Log.i(TAG, "showProgressDialog: Showing progressDialog.");
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Unremoving...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setProgress(0);
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialog, which) -> finish());
        }
        progressDialog.show();
    }

    private void removeProgressDialog() {
        Log.i(TAG, "removeProgressDialog: Removing progressDialog.");
        if (progressDialog != null) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override // FetchDataCallback
    public void onSuccess(CommentData commentData) {
        Log.i(TAG, "onSuccess: Retrofit returned commentData.");
        viewModel.commentData = commentData;

        if (progressDialog != null) {
            buildAndShowAlert(viewModel.resultCode);
        }
    }

    @Override // FetchDataCallback
    public void onException(ResultCode resultCode) {
        Log.e(TAG, "onException: Retrofit threw exception and returned resultCode: " + resultCode.name());
        viewModel.resultCode = resultCode;

        if (progressDialog != null) {
            buildAndShowAlert(resultCode);
        }
    }

    @Override // FetchDataCallback
    public void onException(ResultCode resultCode, Throwable throwable) {
        Log.e(TAG, "onException: Retrofit threw exception", throwable);
        viewModel.resultCode = resultCode;
        viewModel.throwable = throwable;

        if (progressDialog != null) {
            buildAndShowError(throwable);
        }
    }
}

