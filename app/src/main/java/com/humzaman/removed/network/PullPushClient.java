package com.humzaman.removed.network;

import com.humzaman.removed.model.PullPushDataObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PullPushClient {
    @GET ("/reddit/comment/search")
    Call<PullPushDataObject> getCommentData(@Query("ids") String id);

}
