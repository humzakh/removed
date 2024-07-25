package com.humzaman.removed.network;

import android.util.Log;

import com.google.gson.JsonObject;
import com.humzaman.removed.model.CommentData;
import com.humzaman.removed.model.PullPushDataObject;
import com.humzaman.removed.util.ResultCode;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FetchData {
    private static final String TAG = "FetchData";
    private String subreddit;
    private String link_id;
    private String id;
    private static Retrofit pullpushRetrofit;
    private static Retrofit redditRetrofit;
    private CommentData commentData;
    private static final String PULLPUSH_BASE = "https://api.pullpush.io";
    private static final String REDDIT_BASE = "https://api.reddit.com";

    /**
     * Initialize FetchData with the id of the comment to be fetched from PullPush.
     * @param id reddit comment id (do not include t1_ prefix)
     */
    //public FetchData(String id) {this.id = id;}
    public FetchData(String id) {
//        this.subreddit = subreddit;
//        this.link_id = link_id;
        this.id = id;
    }

    /**
     * Fetch data from PullPush and reddit (for current score) using Retrofit.
     * @param callback Callback to calling class when Retrofit finishes its job.
     */
    public void fetch(FetchDataCallback callback) {
        PullPushClient pullpushClient = getPullPushRetrofitInstance().create(PullPushClient.class);
        Call<PullPushDataObject> pullpushCall = pullpushClient.getCommentData(id);

        // Execute the call asynchronously. Get a positive or negative callback.
        //noinspection NullableProblems
        pullpushCall.enqueue(new Callback<PullPushDataObject>() {
            @Override
            public void onResponse(Call<PullPushDataObject> callP, Response<PullPushDataObject> responseP) {
                if (responseP.isSuccessful() && responseP.body() != null) {
                    List<CommentData> commentDataList = responseP.body().getData();

                    if (!commentDataList.isEmpty()) {
                        Log.i(TAG, "onResponse: PullPush " + responseP.code());

                        for(int i = 0; i < commentDataList.size(); i++) { // find the right comment
                            if (commentDataList.get(i).getId().equals(id)) {
                                commentData = commentDataList.get(i);
                                break;
                            }
                        }

                        if (commentData == null) {
                            callback.onException(ResultCode.NO_DATA_FOUND);
                        }
                        else {
                            commentData.setPermalink("https://reddit.com" + commentData.getPermalink());

                            RedditClient redditClient = getRedditRetrofitInstance().create(RedditClient.class);
                            Call<JsonObject> redditCall = redditClient.getScore("t1_" + id);

                            // get current score from reddit
                            //noinspection NullableProblems
                            redditCall.enqueue(new Callback<JsonObject>() {
                                @Override
                                public void onResponse(Call<JsonObject> callR, Response<JsonObject> responseR) {
                                    if (responseR.isSuccessful() && responseR.body() != null) {
                                        Log.i(TAG, "onResponse: reddit " + responseR.code());
                                        try {
                                            JsonObject redditObject = responseR.body().getAsJsonObject("data").getAsJsonArray("children").get(0).getAsJsonObject().getAsJsonObject("data");
                                            String score = redditObject.get("score").getAsString();
                                            Log.i(TAG, "onResponse: reddit score: " + score);
                                            commentData.setScore(score);
                                        } catch (Exception e) {
                                            Log.e(TAG, "onResponse: ", e);
                                        }
                                    }
                                    else {
                                        Log.e(TAG, "onResponse: reddit " + responseR.code());
                                    }
                                    callback.onSuccess(commentData);
                                }

                                // even if redditCall fails or returns null,
                                // we'll show the archived score from PullPush, so we call onSuccess.
                                public void onFailure(Call<JsonObject> callR, Throwable throwable) {
                                    Log.e(TAG, "onFailure: ", throwable);
                                    callback.onSuccess(commentData);
                                }

                            });
                        }
                    }
                    else { // comment not archived by PullPush
                        Log.e(TAG, "onResponse: [no archived data found]");
                        callback.onException(ResultCode.NO_DATA_FOUND);
                    }
                }
                else { // PullPush server error codes
                    Log.e(TAG, "onResponse: PullPush " + responseP.code());
                    switch (responseP.code()) {
                        case 404:
                            callback.onException(ResultCode.PULLPUSH_404);
                            break;
                        case 500:
                            callback.onException(ResultCode.PULLPUSH_500);
                            break;
                        case 502:
                            callback.onException(ResultCode.PULLPUSH_502);
                            break;
                        case 503:
                            callback.onException(ResultCode.PULLPUSH_503);
                            break;
                        case 504:
                            callback.onException(ResultCode.PULLPUSH_504);
                            break;
                        default:
                            callback.onException(ResultCode.PULLPUSH_OTHER);
                            break;
                    }
                }
            }

            @Override
            public void onFailure(Call<PullPushDataObject> call, Throwable throwable) {
                if (throwable instanceof IOException) {
                    if (Objects.equals(throwable.getMessage(), "timeout")) {
                        Log.e(TAG, "onFailure: PullPush timeout", throwable);
                        callback.onException(ResultCode.TIMEOUT);
                    }
                    else {
                        Log.e(TAG, "onFailure: No internet connection.", throwable);
                        callback.onException(ResultCode.NO_INTERNET);
                    }
                }
                else {
                    callback.onException(ResultCode.UNKNOWN_ERROR, throwable);
                }
            }
        });
    }

    /*
    public void fetch2(FetchDataCallback callback) {
        PushshiftClient pushshiftClient = getPushshiftRetrofitInstance().create(PushshiftClient.class);
        Call<JsonObject> pushshiftCall = pushshiftClient.getJson(subreddit, link_id, id);

        // Execute the call asynchronously. Get a positive or negative callback.
        //noinspection NullableProblems
        pushshiftCall.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> callP, Response<JsonObject> responseP) {
                if (responseP.isSuccessful() && responseP.body() != null) {
                    //List<CommentData> commentDataList = responseP.body().getData();
                    JsonArray pObject = responseP.body().getAsJsonArray("data");
                    Log.i(TAG, pObject.getAsString());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

            }
        });
    }
     */

    private static Retrofit getPullPushRetrofitInstance() {
        if (pullpushRetrofit == null) {
            pullpushRetrofit = new Retrofit.Builder()
                    .baseUrl(PULLPUSH_BASE)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return pullpushRetrofit;
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
