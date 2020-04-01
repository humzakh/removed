package com.humzaman.removed.model;

import java.util.List;

/*
 * Pushshift returns an array called data containing comment data.
 */
public class PushshiftDataObject {
    private List<CommentData> data;

    public List<CommentData> getData() {
        return data;
    }

    public void setData(List<CommentData> data) {
        this.data = data;
    }

}
