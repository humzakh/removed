package com.humzaman.removed.network;

import android.util.Log;

import com.google.gson.JsonObject;
import com.humzaman.removed.model.CommentData;
import com.humzaman.removed.model.PushshiftDataObject;
import com.humzaman.removed.util.ResultCode;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FetchData {
    private static final String TAG = "FetchData";
    private String id;
    private static Retrofit pushshiftRetrofit;
    private static Retrofit redditRetrofit;
    private CommentData commentData;
    private static final String PUSHSHIFT_BASE = "https://api.pushshift.io";
    private static final String REDDIT_BASE = "https://api.reddit.com";

    /**
     * Initialize FetchData with the id of the comment to be fetched from Pushshift.
     * @param id reddit comment id
     */
    public FetchData(String id) {
        this.id = id;
    }

    /**
     * Fetch data from Pushshift and reddit (for current score) using Retrofit.
     * @param callback Callback to calling class when Retrofit finishes its job.
     */
    public void fetch(FetchDataCallback callback) {
        PushshiftClient pushshiftClient = getPushshiftRetrofitInstance().create(PushshiftClient.class);
        Call<PushshiftDataObject> pushshiftCall = pushshiftClient.getCommentData(id);

        // Execute the call asynchronously. Get a positive or negative callback.
        //noinspection NullableProblems
        pushshiftCall.enqueue(new Callback<PushshiftDataObject>() {
            @Override
            public void onResponse(Call<PushshiftDataObject> callP, Response<PushshiftDataObject> responseP) {
                if (responseP.isSuccessful() && responseP.body() != null) {
                    List<CommentData> commentDataList = responseP.body().getData();

                    if (commentDataList.size() > 0) {
                        commentData = commentDataList.get(0);
                        commentData.setPermalink("https://reddit.com" + commentData.getPermalink());

                        RedditClient redditClient = getRedditRetrofitInstance().create(RedditClient.class);
                        Call<JsonObject> redditCall = redditClient.getScore("t1_" + id);

                        //noinspection NullableProblems
                        redditCall.enqueue(new Callback<JsonObject>() {
                            @Override
                            public void onResponse(Call<JsonObject> callR, Response<JsonObject> responseR) {
                                if (responseR.isSuccessful() && responseR.body() != null) {
                                    JsonObject redditObject = responseR.body().getAsJsonObject("data").getAsJsonArray("children").get(0).getAsJsonObject().getAsJsonObject("data");
                                    String score = redditObject.get("score").getAsString();
                                    commentData.setScore(score);
                                }
                                else {
                                    switch (responseR.code()) {
                                        case 404:
                                            Log.e(TAG, "onResponse: reddit 404");
                                            break;
                                        case 500:
                                            Log.e(TAG, "onResponse: reddit 500");
                                            break;
                                        default:
                                            Log.e(TAG, "onResponse: reddit unknown error.");
                                            break;
                                    }
                                }
                                callback.onSuccess(commentData);
                            }

                            public void onFailure(Call<JsonObject> callR, Throwable t) {
                                Log.e(TAG, "onFailure: ", t);
                                callback.onSuccess(commentData);
                            }

                        });
                    }
                    else { // no archived data found
                        Log.e(TAG, "onResponse: [no archived data found]");
                        callback.onException(ResultCode.NO_DATA_FOUND);
                    }
                }
                else {
                    switch (responseP.code()) {
                        case 404:
                            Log.e(TAG, "onResponse: Pushshift 404");
                            callback.onException(ResultCode.PUSHSHIFT_404);
                            break;
                        case 500:
                            Log.e(TAG, "onResponse: Pushshift 500");
                            callback.onException(ResultCode.PUSHSHIFT_500);
                            break;
                        default:
                            Log.e(TAG, "onResponse: Pushshift unknown error.");
                            callback.onException(ResultCode.ERROR_RESPONSE);
                            break;
                    }
                }
            }

            @Override
            public void onFailure(Call<PushshiftDataObject> call, Throwable t) {
                if (t instanceof IOException) {
                    Log.e(TAG, "No internet connection.");
                    callback.onException(ResultCode.NO_INTERNET);
                }
                else {
                    Log.e(TAG, "onFailure: ", t);
                    callback.onException(ResultCode.ERROR_RESPONSE);
                }
            }
        });
    }

    private static Retrofit getPushshiftRetrofitInstance() {
        if (pushshiftRetrofit == null) {
            pushshiftRetrofit = new Retrofit.Builder()
                    .baseUrl(PUSHSHIFT_BASE)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return pushshiftRetrofit;
    }

    private static Retrofit getRedditRetrofitInstance() {
        if (redditRetrofit == null) {
            redditRetrofit = new Retrofit.Builder()
                    .baseUrl(REDDIT_BASE)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return redditRetrofit;
    }
}
