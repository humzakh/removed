package com.humzaman.removed.network;

import android.util.Log;

import com.humzaman.removed.model.Api;
import com.humzaman.removed.model.CommentData;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FetchData {
    private static final String TAG = "FetchData";

    public FetchData(String id) {
        PushshiftClient pushshiftClient = RetrofitClientInstance.getRetrofitInstance().create(PushshiftClient.class);

        Call<Api> call = pushshiftClient.getCommentData(id);

        // Execute the call asynchronously. Get a positive or negative callback.
        //noinspection NullableProblems
        call.enqueue(new Callback<Api>() {
            @Override
            public void onResponse(Call<Api> call, Response<Api> response) {
                if(response.isSuccessful() && response.body() != null) {
                    CommentData commentData = response.body().getData().get(0);
                    Log.e(TAG, commentData.getBody());
                } else {
                    Log.e(TAG, String.valueOf(response.errorBody()));
                }
            }

            @Override
            public void onFailure(Call<Api> call, Throwable t) {
                Log.e(TAG, "onFailure: ", t);
                // the network call was a failure
                // TODO: handle error
            }
        });
    }
}
