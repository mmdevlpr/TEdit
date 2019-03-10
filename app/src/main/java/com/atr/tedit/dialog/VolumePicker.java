/*
 * Free Public License 1.0.0
 * Permission to use, copy, modify, and/or distribute this software
 * for any purpose with or without fee is hereby granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL
 * WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR
 * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
 * LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 * CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.atr.tedit.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.text.Html;

import com.atr.tedit.R;
import com.atr.tedit.TEditActivity;
import com.atr.tedit.file.descriptor.AndFile;
import com.atr.tedit.mainstate.Browser;

public class VolumePicker extends DialogFragment {
    private AndFile[] volumes;
    private AndFile currentChoice;

    public static VolumePicker newInstance(String currentVolume) {
        Bundle bundle = new Bundle();
        bundle.putString("TEdit.volumePicker.currentChoice", currentVolume);

        VolumePicker vp = new VolumePicker();
        vp.setArguments(bundle);

        return vp;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        final TEditActivity ctx = (TEditActivity)getActivity();
        int choice = -1;
        if (savedInstanceState == null) {
            String strChoice = getArguments().getString("TEdit.volumePicker.currentChoice", "");
            if (strChoice.isEmpty()) {
                currentChoice = ctx.getStorageRoot();
            } else
                currentChoice = AndFile.createDescriptorFromTree(strChoice, ctx);
        } else {
            String strChoice = savedInstanceState.getString("TEdit.volumePicker.currentChoice", "");
            if (strChoice.isEmpty()) {
                currentChoice = ctx.getStorageRoot();
            } else
                currentChoice = AndFile.createDescriptorFromTree(strChoice, ctx);
        }

        Uri[] vols = ctx.getPermittedUris();
        volumes = new AndFile[vols.length + 2];
        for (int i = 0; i < vols.length; i++) {
            volumes[i] = AndFile.createDescriptor(DocumentFile.fromTreeUri(ctx, vols[i]));
        }
        volumes[volumes.length - 2] = ctx.getStorageRoot();
        volumes[volumes.length - 1] = ctx.getRoot();

        CharSequence[] options = new CharSequence[volumes.length];
        for (int i = 0; i < volumes.length - 2; i++) {
            AndFile f = volumes[i];
            options[i] = Html.fromHtml("<font color='#97bf80'>" + f.getName() + "</font>");
            if (currentChoice.getPathIdentifier().equals(f.getPathIdentifier()))
                choice = i;
        }
        options[volumes.length - 2] = Html.fromHtml("<font color='#97bf80'>" + getString(R.string.internal) + "</font>");
        options[volumes.length - 1] = Html.fromHtml("<font color='#97bf80'>" + getString(R.string.root) + "</font>");

        if (choice < 0) {
            if (currentChoice.getPathIdentifier().equals(ctx.getRoot().getPathIdentifier())) {
                choice = volumes.length - 1;
            } else
                choice = volumes.length - 2;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.volumePicker).setSingleChoiceItems(options, choice,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i < volumes.length) {
                            currentChoice = volumes[i];
                        } else
                            currentChoice = volumes[volumes.length - 2];
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                }).setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        ((Browser)ctx.getFrag()).setVolume(currentChoice);
                        dismiss();
                    }
                });

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("TEdit.volumePicker.currentChoice", currentChoice.getPathIdentifier());
    }
}
