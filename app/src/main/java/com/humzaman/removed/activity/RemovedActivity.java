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
import com.humzaman.removed.util.ResultCode;

public class RemovedActivity extends AppCompatActivity implements FetchDataCallback {
    private static final String TAG = "RemovedActivity";

    private RemovedViewModel viewModel;
    private ProgressDialog progressDialog; // deprecated but whatever, Google's not my mom
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initialize();
    }

    @Override
    protected void onDestroy() { // handle activity destruction issues (screen rotation)
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

        if (viewModel.commentData != null) {
            Log.i(TAG, "initialize: Already initialized, checked URL, and fetched data.");
            buildAndShowAlert(viewModel.resultCode);
        }
        else if (viewModel.resultCode == null) {
            Log.i(TAG, "initialize: Getting intentString and checking URL.");
            Intent intent = getIntent();

            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                viewModel.intentString = intent.getStringExtra(Intent.EXTRA_TEXT);
                Log.i(TAG, "intentString: " + viewModel.intentString);

                CheckURL checkURL = new CheckURL();
                viewModel.id = checkURL.check(viewModel.intentString);
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
            Log.i(TAG, "initialize: Already initialized, checked URL. Showing " +
                    viewModel.resultCode.name() + " alertDialog.");
            buildAndShowAlert(viewModel.resultCode);
        }
    }

    private void buildAndShowAlert(ResultCode resultCode) {
        switch (resultCode) {
            case VALID_COMMENT: {
                Log.i(TAG, "buildAndShowAlert: Showing unremoved comment alertDialog.");
                this.alertDialog = (new BuildAlert(this, resultCode, viewModel.intentString, viewModel.commentData)).build();
                alertDialog.show();
                break;
            }
            case SUBMISSION: {
                Log.i(TAG, "buildAndShowAlert: Showing submission alertDialog.");
                this.alertDialog = (new BuildAlert(this, resultCode, viewModel.intentString)).build();
                alertDialog.show();
                break;
            }
            case ERROR_RESPONSE: {
                Log.i(TAG, "buildAndShowAlert: Showing error alertDialog.");
                this.alertDialog = (new BuildAlert(this, resultCode, viewModel.intentString)).build();
                alertDialog.show();
                break;
            }
            default: {
                Log.i(TAG, "buildAndShowAlert: Showing " + resultCode.name() + " alertDialog.");
                this.alertDialog = (new BuildAlert(this, resultCode)).build();
                alertDialog.show();
                break;
            }
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
            progressDialog.show();
        }
        else {
            progressDialog.show();
        }
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
            removeProgressDialog();
            buildAndShowAlert(viewModel.resultCode);
        }
    }

    @Override // FetchDataCallback
    public void onException(ResultCode resultCode) {
        Log.e(TAG, "onException: Retrofit threw exception.");
        viewModel.resultCode = resultCode;

        if (progressDialog != null) {
            removeProgressDialog();
            buildAndShowAlert(resultCode);
        }
    }
}

