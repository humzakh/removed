package com.humzaman.removed.network;

import com.humzaman.removed.model.Api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PushshiftClient {
    @GET ("/reddit/search/comment")
    Call<Api> getCommentData(@Query("ids") String id);
}
