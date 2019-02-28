package com.atr.tedit.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.atr.tedit.R;
import com.atr.tedit.TEditActivity;
import com.atr.tedit.mainstate.Browser;

import java.io.File;

public class PossibleBinary extends DialogFragment {
    String filePath;

    public static PossibleBinary getInstance(String filePath) {
        Bundle bundle = new Bundle();
        bundle.putString("pBinary.filePath", filePath);

        PossibleBinary pBin = new PossibleBinary();
        pBin.setArguments(bundle);

        return pBin;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            filePath = getArguments().getString("pBinary.filePath", "");
        } else {
            filePath = savedInstanceState.getString("pBinary.filePath", "");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.alert).setMessage(R.string.alert_binaryfile)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                })
                .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                        ((Browser)((TEditActivity)getActivity()).getFrag())
                                .openFile(filePath.isEmpty() ? null : new File(filePath), true);
                    }
                });;

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("pBinary.filePath", filePath);
    }
}
