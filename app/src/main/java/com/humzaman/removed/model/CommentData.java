package com.humzaman.removed.model;

public class CommentData {
    private String author;
    private String body;
    private String score;
    private String id;
    private String permalink;
    private String created_utc;
    private String retrieved_on;
    private String subreddit;
    private String subreddit_id;
    private String link_id;
    private String parent_id;
    private String author_fullname;

    public CommentData(String author, String body, String score, String id, String permalink, String created_utc, String retrieved_on, String subreddit, String subreddit_id, String link_id, String parent_id, String author_fullname) {
        this.author = author;
        this.body = body;
        this.score = score;
        this.id = id;
        this.permalink = permalink;
        this.created_utc = created_utc;
        this.retrieved_on = retrieved_on;
        this.subreddit = subreddit;
        this.subreddit_id = subreddit_id;
        this.link_id = link_id;
        this.parent_id = parent_id;
        this.author_fullname = author_fullname;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPermalink() {
        return permalink;
    }

    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    public String getCreated_utc() {
        return created_utc;
    }

    public void setCreated_utc(String created_utc) {
        this.created_utc = created_utc;
    }

    public String getRetrieved_on() {
        return retrieved_on;
    }

    public void setRetrieved_on(String retrieved_on) {
        this.retrieved_on = retrieved_on;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }

    public String getSubreddit_id() {
        return subreddit_id;
    }

    public void setSubreddit_id(String subreddit_id) {
        this.subreddit_id = subreddit_id;
    }

    public String getLink_id() {
        return link_id;
    }

    public void setLink_id(String link_id) {
        this.link_id = link_id;
    }

    public String getParent_id() {
        return parent_id;
    }

    public void setParent_id(String parent_id) {
        this.parent_id = parent_id;
    }

    public String getAuthor_fullname() {
        return author_fullname;
    }

    public void setAuthor_fullname(String author_fullname) {
        this.author_fullname = author_fullname;
    }
}
