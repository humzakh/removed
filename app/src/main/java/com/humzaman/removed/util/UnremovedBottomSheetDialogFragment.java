package com.humzaman.removed.util;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.humzaman.removed.R;

public class UnremovedBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private String username;
    private String score;
    private String submitted;
    private String body;

    public UnremovedBottomSheetDialogFragment(String username, String score, String submitted, String body) {
        this.username = username;
        this.score = score;
        this.submitted = submitted;
        this.body = body;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_unremoved_comment, container, false);
        
        TextView tv_username = view.findViewById(R.id.tv_username);
        TextView tv_score = view.findViewById(R.id.tv_score);
        TextView tv_submitted = view.findViewById(R.id.tv_submitted);
        TextView tv_body = view.findViewById(R.id.tv_body);

        tv_username.setText(this.username);
        tv_score.setText(this.score);
        tv_submitted.setText(this.submitted);
        tv_body.setText(this.body);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((View) getView().getParent()).setBackgroundColor(Color.TRANSPARENT);
    }

}