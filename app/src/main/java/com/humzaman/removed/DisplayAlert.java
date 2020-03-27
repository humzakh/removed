package com.humzaman.removed;

/*
public class DisplayAlert {
    private Activity activity;
    private ResultCode resultCode;
    private String responseCode;

    public enum ResultCode {
        SUBMISSION,
        VALID_COMMENT,
        INVALID_COMMENT,
        NOT_REDDIT_LINK,
        NOT_URL,
        MORE_DETAILS,
        ABOUT,
        FAILED,
        NO_INTERNET,
        NO_DATA_FOUND,
        ERROR_RESPONSE
    }

    public DisplayAlert(Activity activity, ResultCode resultCode) {
        this.activity = activity;
        this.resultCode = resultCode;
        this.responseCode = null;
    }

    public DisplayAlert(Activity activity, ResultCode resultCode, String responseCode) {
        this.activity = activity;
        this.resultCode = resultCode;
        this.responseCode = responseCode;
    }

    public void display() {
        LayoutInflater inflater = activity.getLayoutInflater();
        @SuppressLint("InflateParams") View dialogTitle = inflater.inflate(R.layout.alert_title, null);
        Toolbar toolbar = dialogTitle.findViewById(R.id.toolbar);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCustomTitle(dialogTitle)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        activity.finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                        activity.finish();
                    }
                });

        switch (this.resultCode) {
            case VALID_COMMENT: { // valid comment link
                @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.alert_view, null);
                builder.setView(dialogView)
                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                activity.finish();
                            }
                        })
                        .setNeutralButton("Copy Text", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("[removed]", parsedData.get(1));
                                if (clipboard != null)
                                    clipboard.setPrimaryClip(clip);
                                Toast.makeText(activity.getApplicationContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
                                activity.finish();
                            }
                        });

                String removeddit = intentString;
                if (removeddit.contains("old.reddit.com"))
                    removeddit = removeddit.replaceFirst("old[.]", "");
                final String finalRemoveddit = removeddit.replaceFirst("reddit", "removeddit");

                toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.view_on_reddit: {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(parsedData.get(4)));
                                activity.startActivity(browserIntent);
                                break;
                            }
                            case R.id.view_on_removeddit: {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalRemoveddit));
                                activity.startActivity(browserIntent);
                                break;
                            }
                            case R.id.more_details: {
                                display(ResultCode.MORE_DETAILS, null);
                                break;
                            }
                            case R.id.settings: { break; }
                            case R.id.about: {
                                display(ResultCode.ABOUT, null);
                                break;
                            }
                        }
                        return true;
                    }});
                toolbar.inflateMenu(R.menu.alert_overflow);
                MenuCompat.setGroupDividerEnabled(toolbar.getMenu(), true);

                TextView authorTV = dialogView.findViewById(R.id.authorTV);
                TextView timeTV = dialogView.findViewById(R.id.timeTV);
                TextView bodyTV = dialogView.findViewById(R.id.bodyTV);

                if (!parsedData.get(0).equals("[deleted]")) { // open user profile
                    authorTV.setMovementMethod(LinkMovementMethod.getInstance());
                    String html = "<a href='https://www.reddit.com/user/" + parsedData.get(0) + "'>/u/" + parsedData.get(0) + "</a>";
                    authorTV.setText(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY));
                }
                else
                    authorTV.setText(R.string.deleted);

                String time = (DateUtils.getRelativeDateTimeString(activity,
                        Long.parseLong(parsedData.get(5)) * 1000,
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.DAY_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE)).toString();
                timeTV.setText(time);

                // archived username [deleted] tells us the comment couldn't be archived in time
                if (parsedData.get(0).equals("[deleted]"))
                    bodyTV.setText(R.string.removed_quick);
                else {
                    bodyTV.setMovementMethod(LinkMovementMethod.getInstance()); // make links in body clickable

                    // reddit comments use markdown format.
                    // obtain an instance of Markwon
                    final Markwon markwon = Markwon.create(activity);
                    // parse markdown to commonmark-java Node
                    final Node node = markwon.parse(parsedData.get(1));
                    // create styled text from parsed Node
                    final Spanned markdown = markwon.render(node);
                    // use it on a TextView
                    markwon.setParsedMarkdown(bodyTV, markdown);
                }

                break;
            }
            case SUBMISSION: { // submission link
                String removeddit = intentString;
                if (removeddit.contains("old.reddit.com")) {
                    removeddit = removeddit.replaceFirst("old[.]", "");
                }
                final String finalRemoveddit = removeddit.replaceFirst("reddit", "removeddit");

                builder.setMessage("Submission links are not currently supported.\n\nTap \"Removeddit\" to view the submission on removeddit.com, or try again with a direct link to a comment.")
                        .setNeutralButton("Removeddit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalRemoveddit));
                                activity.startActivity(browserIntent);
                                dialog.dismiss();
                                activity.finish();
                            }
                        });
                break;
            }
            case MORE_DETAILS: { // more details dialog
                toolbar.setTitle("More details");

                @SuppressLint("InflateParams") View mdDialogView = inflater.inflate(R.layout.alert_moredetails, null);

                builder.setView(mdDialogView)
                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                dialog.dismiss();
                            }
                        });

                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                String author = (parsedData.get(0).equals("[deleted]") ? "[deleted]" : "/u/" + parsedData.get(0));
                String score =  (parsedData.get(2).equals("null") ? "null" : parsedData.get(2) + " " + ((Integer.parseInt(parsedData.get(2)) == 1) ? "point" : "points"));
                String subreddit = "/r/" + parsedData.get(7);
                String submitted = (parsedData.get(5).equals("null") ? "null" : sdf.format(new Date(Long.parseLong(parsedData.get(5)) * 1000)));
                String archived = (parsedData.get(5).equals("null") ? "null" : sdf.format(new Date(Long.parseLong(parsedData.get(6)) * 1000)));
                String commentID = "t1_" + parsedData.get(3);
                String source = "Data source: " + pushshiftUrl;

                TextView noteTV = mdDialogView.findViewById(R.id.md_note_tv);
                noteTV.setMovementMethod(LinkMovementMethod.getInstance());

                TextView authorTV = mdDialogView.findViewById(R.id.md_author_tv);
                authorTV.setText(author);
                TextView scoreTV = mdDialogView.findViewById(R.id.md_score_tv);
                scoreTV.setText(score);
                TextView subredditTV = mdDialogView.findViewById(R.id.md_sub_tv);
                subredditTV.setText(subreddit);

                TextView submittedTV = mdDialogView.findViewById(R.id.md_submitted_tv);
                submittedTV.setText(submitted);
                TextView archivedTV = mdDialogView.findViewById(R.id.md_archived_tv);
                archivedTV.setText(archived);

                TextView authorIdTV = mdDialogView.findViewById(R.id.md_authorID_tv);
                authorIdTV.setText(parsedData.get(11));
                TextView commentIdTV = mdDialogView.findViewById(R.id.md_commentID_tv);
                commentIdTV.setText(commentID);
                TextView parentIdTV = mdDialogView.findViewById(R.id.md_parentID_tv);
                parentIdTV.setText(parsedData.get(10));
                TextView linkIdTV = mdDialogView.findViewById(R.id.md_linkID_tv);
                linkIdTV.setText(parsedData.get(9));
                TextView subIdTV = mdDialogView.findViewById(R.id.md_subID_tv);
                subIdTV.setText(parsedData.get(8));

                TextView sourceTV = mdDialogView.findViewById(R.id.md_source_tv);
                sourceTV.setText(source);

                break;
            }
            case ABOUT: { // about dialog
                builder.setCustomTitle(null)
                        .setTitle("About [removed]")
                        .setIcon(R.mipmap.ic_launcher)
                        .setView(R.layout.alert_about)
                        .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                dialog.dismiss();
                            }
                        });
                break;
            }

             Error result codes
            case NOT_URL: // not a URL
                builder.setMessage("Error: not a valid URL.\n\nPlease share a direct link, not the comment text.");
                break;
            case FAILED: // failed to retrieve data
                builder.setMessage("Error: failed to retrieve data from pushshift.io");
                break;
            case NO_INTERNET: // no internet
                builder.setMessage("Error: check internet connection.");
                break;
            case NO_DATA_FOUND: // no data found on Pushshift
                builder.setMessage(R.string.not_archived);
                break;
            case ERROR_RESPONSE: // Pushshift error response
                builder.setMessage("Error " + responseCode + ": Could not reach Pushshift.\n\nTheir servers may be down.\nCheck pushshift.io for updates, or try again later.")
                        .setNeutralButton("Pushshift.io", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://pushshift.io"));
                                activity.startActivity(browserIntent);
                                dialog.dismiss();
                                activity.finish();
                            }
                        });
                break;
            default: // invalid link
                builder.setMessage("Error: invalid link.");
                break;
        }

        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }




}
*/