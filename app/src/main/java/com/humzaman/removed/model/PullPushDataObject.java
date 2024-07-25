package com.humzaman.removed.model;

import java.util.List;

/*
 * PullPush returns an array called data containing comment data.
 */
public class PullPushDataObject {
    private List<CommentData> data;

    public List<CommentData> getData() {
        return data;
    }

    public void setData(List<CommentData> data) {
        this.data = data;
    }

}
