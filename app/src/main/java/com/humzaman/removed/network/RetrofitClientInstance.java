package com.humzaman.removed.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

class RetrofitClientInstance {
    private static Retrofit retrofit;
    private static final String PUSHSHIFT_BASE = "https://api.pushshift.io";
    //private static final String REDDIT_BASE = "https://api.reddit.com/api/info/?id=t1_";

    static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(PUSHSHIFT_BASE)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
