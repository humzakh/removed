package com.humzaman.removed.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.text.HtmlCompat;
import androidx.core.view.MenuCompat;

import com.humzaman.removed.BuildConfig;
import com.humzaman.removed.R;
import com.humzaman.removed.model.CommentData;

import org.commonmark.node.Node;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;

public class BuildAlert {
    private Activity activity;
    private LayoutInflater inflater;
    private ResultCode resultCode;
    private String intentString;
    private CommentData commentData;
    private Throwable throwable;

    /**
     * Initialize BuildAlert for generic alert dialog.
     * @param activity Context to be passed to AlertDialog.Builder
     * @param resultCode Determines which dialog to display.
     */
    public BuildAlert(Activity activity, ResultCode resultCode) {
        this.activity = activity;
        this.resultCode = resultCode;
        this.inflater = this.activity.getLayoutInflater();
    }

    /**
     * Initialize BuildAlert for submission dialog.
     * @param activity Context to be passed to AlertDialog.Builder
     * @param resultCode Determines which dialog to display.
     * @param intentString Used either for removeddit link (submission) or debugging (error).
     */
    public BuildAlert(Activity activity, ResultCode resultCode, String intentString) {
        this.activity = activity;
        this.resultCode = resultCode;
        this.intentString = intentString;
        this.inflater = this.activity.getLayoutInflater();
    }

    /**
     * Initialize BuildAlert for valid comment dialog or more details dialog.
     * @param activity Context to be passed to AlertDialog.Builder
     * @param resultCode Determines which dialog to display.
     * @param intentString Used for removeddit link.
     * @param commentData Data fetched from Pushshift.
     */
    public BuildAlert(Activity activity, ResultCode resultCode, String intentString, CommentData commentData) {
        this.activity = activity;
        this.resultCode = resultCode;
        this.intentString = intentString;
        this.commentData = commentData;
        this.inflater = this.activity.getLayoutInflater();
    }

    /**
     * Initialize BuildAlert for error dialog (email info to dev).
     * @param activity Context to be passed to AlertDialog.Builder
     * @param intentString Intent string to be emailed to dev for debugging.
     * @param throwable Stack trace to be emailed to dev for debugging.
     */
    public BuildAlert(Activity activity, String intentString, Throwable throwable) {
        this.activity = activity;
        this.intentString = intentString;
        this.throwable = throwable;
        this.inflater = this.activity.getLayoutInflater();
    }

    /**
     * Build AlertDialog based on initialized parameters.
     * @return Return built AlertDialog.
     */
    public AlertDialog build() {
        AlertDialog.Builder builder;
        if (this.throwable == null) {
            switch (this.resultCode) {
                case VALID_COMMENT: {
                    builder = buildValidComment();
                    break;
                }
                case SUBMISSION: {
                    builder = buildSubmission();
                    break;
                }
                case MORE_DETAILS: {
                    builder = buildMoreDetails();
                    break;
                }
                case ABOUT: {
                    builder = buildAbout();
                    break;
                }
                default: { // Error codes
                    builder = buildGeneric();
                    break;
                }
            }
        }
        else {
            builder = buildError();
        }

        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        return alert;
    }

    @SuppressLint("InflateParams")
    private AlertDialog.Builder buildValidComment() {
        View dialogToolbar = inflater.inflate(R.layout.alert_toolbar, null);
        Toolbar toolbar = dialogToolbar.findViewById(R.id.toolbar);
        View dialogView = inflater.inflate(R.layout.alert_unremoved, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCustomTitle(dialogToolbar)
                .setView(dialogView)
                .setPositiveButton("Close", (dialog, id) -> {
                    dialog.dismiss();
                    activity.finish();
                })
                .setNeutralButton("Copy Text", (dialog, i) -> {
                    dialog.dismiss();
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("[removed]", commentData.getBody());
                    if (clipboard != null)
                        clipboard.setPrimaryClip(clip);
                    Toast.makeText(activity.getApplicationContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
                    activity.finish();
                })
                .setOnCancelListener(dialog -> {
                    dialog.dismiss();
                    activity.finish();
                });

        String removeddit = intentString;
        if (removeddit.contains("old.reddit.com"))
            removeddit = removeddit.replaceFirst("old[.]", "");
        final String finalRemoveddit = removeddit.replaceFirst("reddit", "removeddit");

        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.view_on_reddit: {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(commentData.getPermalink()));
                    activity.startActivity(browserIntent);
                    break;
                }
                case R.id.view_on_removeddit: {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalRemoveddit));
                    activity.startActivity(browserIntent);
                    break;
                }
                case R.id.more_details: {
                    (new BuildAlert(activity, ResultCode.MORE_DETAILS, intentString, commentData)).build().show();
                    break;
                }
                case R.id.settings: { break; }
                case R.id.about: {
                    (new BuildAlert(activity, ResultCode.ABOUT)).build().show();
                    break;
                }
            }
            return true;
        });
        toolbar.inflateMenu(R.menu.alert_overflow);
        MenuCompat.setGroupDividerEnabled(toolbar.getMenu(), true);

        TextView authorTV = dialogView.findViewById(R.id.authorTV);
        TextView timeTV = dialogView.findViewById(R.id.timeTV);
        TextView bodyTV = dialogView.findViewById(R.id.bodyTV);

        if (!commentData.getAuthor().equals("[deleted]")) { // open user profile
            authorTV.setMovementMethod(LinkMovementMethod.getInstance());
            String html = "<a href='https://www.reddit.com/user/" + commentData.getAuthor() + "'>/u/" + commentData.getAuthor() + "</a>";
            authorTV.setText(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY));
        }
        else
            authorTV.setText(R.string.deleted);

        String time = (DateUtils.getRelativeDateTimeString(activity,
                Long.parseLong(commentData.getCreated_utc()) * 1000,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE)).toString();
        timeTV.setText(time);

        // archived username [deleted] tells us the comment couldn't be archived in time
        if (commentData.getAuthor().equals("[deleted]"))
            bodyTV.setText(R.string.removed_quick);
        else {
            bodyTV.setMovementMethod(LinkMovementMethod.getInstance()); // make links in body clickable

            // reddit comments use markdown format.
            // obtain an instance of Markwon
            final Markwon markwon = Markwon.builder(activity)
                                           .usePlugin(LinkifyPlugin.create(Linkify.WEB_URLS))
                                           .usePlugin(StrikethroughPlugin.create())
                                           .build();
            // parse markdown to commonmark-java Node
            final Node node = markwon.parse(commentData.getBody());
            // create styled text from parsed Node
            final Spanned markdown = markwon.render(node);
            // use it on a TextView
            markwon.setParsedMarkdown(bodyTV, markdown);

            // linkify subreddits and usernames
            Pattern patternR = Pattern.compile("/?\\b(?=\\w)[rR]/(\\w+)");
            String schemeR = "https://reddit.com/r/";
            Pattern patternU = Pattern.compile("/?\\b(?=\\w)[uU]/(\\w+)");
            String schemeU = "https://reddit.com/u/";
            Linkify.TransformFilter transformFilter = (match, url) -> match.group(1);

            Linkify.addLinks(bodyTV, patternR, schemeR, null, transformFilter);
            Linkify.addLinks(bodyTV, patternU, schemeU, null, transformFilter);
        }

        return builder;
    }

    @SuppressLint("InflateParams")
    private AlertDialog.Builder buildSubmission() {
        View dialogToolbar = inflater.inflate(R.layout.alert_toolbar, null);

        String removeddit = intentString;
        if (removeddit.contains("old.reddit.com")) {
            removeddit = removeddit.replaceFirst("old[.]", "");
        }
        final String finalRemoveddit = removeddit.replaceFirst("reddit", "removeddit");

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCustomTitle(dialogToolbar)
                .setMessage("Submission links are not currently supported.\n\nTap \"Removeddit\" to view the submission on removeddit.com, or try again with a direct link to a comment.")
                .setPositiveButton("OK", (dialog, id) -> {
                    dialog.dismiss();
                    activity.finish();
                })
                .setNeutralButton("Removeddit", (dialog, i) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalRemoveddit));
                    activity.startActivity(browserIntent);
                    dialog.dismiss();
                    activity.finish();
                })
                .setOnCancelListener(dialog -> {
                    dialog.dismiss();
                    activity.finish();
                });

        return builder;
    }

    @SuppressLint("InflateParams")
    private AlertDialog.Builder buildMoreDetails() {
        View dialogToolbar = inflater.inflate(R.layout.alert_toolbar, null);
        Toolbar toolbar = dialogToolbar.findViewById(R.id.toolbar);
        View dialogView = inflater.inflate(R.layout.alert_moredetails, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCustomTitle(dialogToolbar)
                .setView(dialogView)
                .setPositiveButton("Close", (dialog, id) -> dialog.dismiss())
                .setOnCancelListener(DialogInterface::dismiss);
        toolbar.setTitle("More details");

        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        String author = (commentData.getAuthor().equals("[deleted]") ? "[deleted]" : "/u/" + commentData.getAuthor());
        String score =  commentData.getScore() + " " + ((Integer.parseInt(commentData.getScore()) == 1) ? "point" : "points");
        String subreddit = "/r/" + commentData.getSubreddit();
        String submitted = (commentData.getCreated_utc() == null ? "" : sdf.format(new Date(Long.parseLong(commentData.getCreated_utc()) * 1000)));
        String archived = (commentData.getRetrieved_on() == null ? "" : sdf.format(new Date(Long.parseLong(commentData.getRetrieved_on()) * 1000)));
        String commentID = "t1_" + commentData.getId();
        String source = "Data source: https://api.pushshift.io/reddit/search/comment/?ids=" + commentData.getId();

        TextView noteTV = dialogView.findViewById(R.id.md_note_tv);
        noteTV.setMovementMethod(LinkMovementMethod.getInstance());

        TextView authorTV = dialogView.findViewById(R.id.md_author_tv);
        authorTV.setText(author);
        TextView scoreTV = dialogView.findViewById(R.id.md_score_tv);
        scoreTV.setText(score);
        TextView subredditTV = dialogView.findViewById(R.id.md_sub_tv);
        subredditTV.setText(subreddit);

        TextView submittedTV = dialogView.findViewById(R.id.md_submitted_tv);
        submittedTV.setText(submitted);
        TextView archivedTV = dialogView.findViewById(R.id.md_archived_tv);
        archivedTV.setText(archived);

        TextView authorIdTV = dialogView.findViewById(R.id.md_authorID_tv);
        authorIdTV.setText(commentData.getAuthor_fullname());
        TextView commentIdTV = dialogView.findViewById(R.id.md_commentID_tv);
        commentIdTV.setText(commentID);
        TextView parentIdTV = dialogView.findViewById(R.id.md_parentID_tv);
        parentIdTV.setText(commentData.getParent_id());
        TextView linkIdTV = dialogView.findViewById(R.id.md_linkID_tv);
        linkIdTV.setText(commentData.getLink_id());
        TextView subIdTV = dialogView.findViewById(R.id.md_subID_tv);
        subIdTV.setText(commentData.getSubreddit_id());

        TextView sourceTV = dialogView.findViewById(R.id.md_source_tv);
        sourceTV.setText(source);

        return builder;
    }

    private AlertDialog.Builder buildAbout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("About [removed]")
                .setIcon(R.mipmap.ic_launcher)
                .setView(R.layout.alert_about)
                .setPositiveButton("Close", (dialog, id) -> dialog.dismiss())
                .setOnCancelListener(DialogInterface::dismiss);

        return builder;
    }

    @SuppressLint("InflateParams")
    private AlertDialog.Builder buildGeneric() {
        View dialogToolbar = inflater.inflate(R.layout.alert_toolbar, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCustomTitle(dialogToolbar)
                .setPositiveButton("OK", (dialog, id) -> {
                    dialog.dismiss();
                    activity.finish();
                })
                .setOnCancelListener(dialog -> {
                    dialog.dismiss();
                    activity.finish();
                });

        switch (this.resultCode) {
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
            case TIMEOUT:
                builder.setMessage("Error: connection timed out.\n\nPushshift is taking too long to respond.\nTheir servers may be having some issues.\n\nCheck pushshift.io for updates, or try again later.")
                       .setNeutralButton("Pushshift.io", (dialog, i) -> {
                           Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://pushshift.io"));
                           activity.startActivity(browserIntent);
                           dialog.dismiss();
                           activity.finish();
                       });
                break;
            case PUSHSHIFT_404:
                builder.setMessage("Error 404: not found.");
                break;
            case PUSHSHIFT_500:
                builder.setMessage("Error 500: internal server error.\n\nPushshift's servers seem to be having some issues.\n\nCheck pushshift.io for updates, or try again later.")
                        .setNeutralButton("Pushshift.io", (dialog, i) -> {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://pushshift.io"));
                            activity.startActivity(browserIntent);
                            dialog.dismiss();
                            activity.finish();
                        });
                break;
            case PUSHSHIFT_502:
                builder.setMessage("Error 502: bad gateway.\n\nPushshift's servers seem to be having some issues.\n\nCheck pushshift.io for updates, or try again later.")
                       .setNeutralButton("Pushshift.io", (dialog, i) -> {
                           Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://pushshift.io"));
                           activity.startActivity(browserIntent);
                           dialog.dismiss();
                           activity.finish();
                       });
                break;
            case PUSHSHIFT_503:
                builder.setMessage("Error 503: service unavailable.\n\nPushshift's servers seem to be having some issues.\n\nCheck pushshift.io for updates, or try again later.")
                       .setNeutralButton("Pushshift.io", (dialog, i) -> {
                           Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://pushshift.io"));
                           activity.startActivity(browserIntent);
                           dialog.dismiss();
                           activity.finish();
                       });
                break;
            case PUSHSHIFT_504:
                builder.setMessage("Error 504: gateway timeout.\n\nPushshift's servers seem to be having some issues.\n\nCheck pushshift.io for updates, or try again later.")
                       .setNeutralButton("Pushshift.io", (dialog, i) -> {
                           Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://pushshift.io"));
                           activity.startActivity(browserIntent);
                           dialog.dismiss();
                           activity.finish();
                       });
                break;
            case PUSHSHIFT_OTHER:
                builder.setMessage("Error: unknown server error.\n\nPushshift's servers may be having some issues.\n\nCheck pushshift.io for updates, or try again later.")
                       .setNeutralButton("Pushshift.io", (dialog, i) -> {
                           Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://pushshift.io"));
                           activity.startActivity(browserIntent);
                           dialog.dismiss();
                           activity.finish();
                       });
                break;
            default: // invalid link
                builder.setMessage("Error: invalid link.");
                break;
        }

        return builder;
    }

    @SuppressLint("InflateParams")
    private AlertDialog.Builder buildError() {
        View dialogToolbar = inflater.inflate(R.layout.alert_toolbar, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCustomTitle(dialogToolbar)
               .setMessage("Unknown Error: Please contact the developer.")
               .setPositiveButton("OK", (dialog, id) -> {
                   dialog.dismiss();
                   activity.finish();
               })
               .setNeutralButton("Email", (dialog, i) -> {
                   Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                   emailIntent.setData(Uri.parse("mailto:"));
                   emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{activity.getString(R.string.dev_email)});
                   emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[removed] error report");
                   emailIntent.putExtra(Intent.EXTRA_TEXT, getErrorString());
                   activity.startActivity(emailIntent);
                   dialog.dismiss();
                   activity.finish();
               })
               .setOnCancelListener(dialog -> {
                   dialog.dismiss();
                   activity.finish();
               });

        return builder;
    }

    private String getErrorString() {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        String build = "APP VERSION: " + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")" +
                       "\nSDK VERSION: " + Build.VERSION.SDK_INT +
                       "\nMANUFACTURER: " + Build.MANUFACTURER +
                       "\nMODEL: " + Build.MODEL;

        return "Please describe the problem: \n\n" +
               "--WRITE ABOVE THIS LINE--\n" +
               build + "\n\n" +
               "INTENT STRING: " + intentString +
               "\n\nSTACK TRACE:\n" + sw.toString();
    }
}
