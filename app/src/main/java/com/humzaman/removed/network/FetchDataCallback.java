package com.humzaman.removed.network;

import com.humzaman.removed.model.CommentData;
import com.humzaman.removed.util.ResultCode;

public interface FetchDataCallback {
    void onSuccess(CommentData commentData);
    void onException(ResultCode resultCode);
    void onException(ResultCode resultCode, Throwable throwable);
}
