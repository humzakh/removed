package com.humzaman.removed.network;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RedditClient {
    @GET("/api/info")
    Call<JsonObject> getScore(@Query("id") String id);
}
