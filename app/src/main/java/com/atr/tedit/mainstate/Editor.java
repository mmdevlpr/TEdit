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
package com.atr.tedit.mainstate;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.atr.tedit.R;
import com.atr.tedit.TEditActivity;
import com.atr.tedit.util.ErrorMessage;
import com.atr.tedit.util.TextSearch;
import com.atr.tedit.util.TEditDB;
import com.atr.tedit.utilitybar.UtilityBar;
import com.atr.tedit.utilitybar.state.TextSearchState;

import java.io.File;

/**
 * @author Adam T. Ryder
 * <a href="http://1337atr.weebly.com">http://1337atr.weebly.com</a>
 */

public class Editor extends Fragment {
    private TEditActivity ctx;
    private long key;

    private EditText editText;
    private TextWatcher editorChangeListener;

    private TextSearch searchString;
    private TextSearchState barSearch;

    public static Editor newInstance(long key) {
        Bundle bundle = new Bundle();
        bundle.putLong("Editor.key", key);

        Editor editor = new Editor();
        editor.setArguments(bundle);

        return editor;
    }

    public long getKey() {
        return key;
    }

    public Editable getText() {
        return editText.getText();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        ctx = (TEditActivity)context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            key = getArguments().getLong("Editor.key", -1);
            return;
        }

        key = savedInstanceState.getLong("Editor.key", -1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.editor, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        editText = (EditText)getView().findViewById(R.id.editorText);
        searchString = new TextSearch();

        if (!ctx.dbIsOpen()) {
            ((TextView)view.findViewById(R.id.documentname)).setText(TEditActivity.DEFAULTPATH);
            return;
        }

        Cursor cursor = ctx.getDB().fetchText(key);
        if (cursor == null) {
            ((TextView)view.findViewById(R.id.documentname)).setText(TEditActivity.DEFAULTPATH);
            return;
        }

        if (cursor.getColumnIndex(TEditDB.KEY_PATH) == -1) {
            ((TextView)view.findViewById(R.id.documentname)).setText(TEditActivity.DEFAULTPATH);
            cursor.close();
            return;
        }

        String path = cursor.getString(cursor.getColumnIndex(TEditDB.KEY_PATH));
        cursor.close();
        if (path.equals(TEditActivity.DEFAULTPATH)) {
            ((TextView)view.findViewById(R.id.documentname)).setText(TEditActivity.DEFAULTPATH);
        } else {
            ((TextView) view.findViewById(R.id.documentname)).setText(new File(path).getName());

            File file = new File(path);
            if (!file.canWrite())
                Toast.makeText(ctx, R.string.readonlymode, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        outState.putLong("Editor.key", key);
    }

    @Override
    public void onResume() {
        super.onResume();

        editText.setEnabled(true);
        editText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.setFocusable(true);
                v.setFocusableInTouchMode(true);
                return false;
            }
        });

        if (!ctx.dbIsOpen() || key < 0)
            return;

        Cursor cursor = ctx.getDB().fetchText(key);
        if (cursor == null)
            return;

        if (cursor.getColumnIndex(TEditDB.KEY_BODY) == -1) {
            editText.setText("");
            cursor.close();
            return;
        }

        editText.setText(cursor.getString(cursor.getColumnIndex(TEditDB.KEY_BODY)));
        
        final int scrollPos = cursor.getInt(cursor.getColumnIndex(TEditDB.KEY_SCROLLPOS));
        final int selStart = cursor.getInt(cursor.getColumnIndex(TEditDB.KEY_SELECTION_START));
        final int selEnd = cursor.getInt(cursor.getColumnIndex(TEditDB.KEY_SELECTION_END));
        editText.post(new Runnable() {
            @Override
            public void run() {
                editText.setPressed(true);
                editText.scrollTo(0, scrollPos);
                editText.setSelection(selStart, selEnd);
            }
        });

        boolean searchActive = cursor.getInt(cursor.getColumnIndex(TEditDB.KEY_TEXT_SEARCH_ACTIVE)) != 0;
        String searchPhrase = cursor.getString(cursor.getColumnIndex(TEditDB.KEY_TEXT_SEARCH_PHRASE));
        String replacePhrase = cursor.getString(cursor.getColumnIndex(TEditDB.KEY_TEXT_SEARCH_REPLACE));
        boolean searchWholeWord = cursor.getInt(cursor.getColumnIndex(TEditDB.KEY_TEXT_SEARCH_WHOLEWORD)) != 0;
        boolean searchMatchCase = cursor.getInt(cursor.getColumnIndex(TEditDB.KEY_TEXT_SEARCH_MATCHCASE)) != 0;

        if (!searchActive) {
            if (ctx.getUtilityBar().getState().STATE == UtilityBar.STATE_TEXT) {
                ctx.getUtilityBar().getState().setEnabled(false);
                ctx.getUtilityBar().handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ctx.getUtilityBar().getState().setEnabled(true);
                    }
                }, TEditActivity.SWAP_ANIM_LENGTH);
            } else
                ctx.getUtilityBar().setToText();

            barSearch = new TextSearchState(ctx.getUtilityBar(), searchPhrase, replacePhrase, searchWholeWord,
                    searchMatchCase);
        } else if (ctx.getUtilityBar().getState().STATE == UtilityBar.STATE_TEXT_SEARCH){
            barSearch = (TextSearchState)ctx.getUtilityBar().getState();
            barSearch.setFields(searchPhrase, replacePhrase, searchWholeWord, searchMatchCase);
        } else {
            barSearch = new TextSearchState(ctx.getUtilityBar(), searchPhrase, replacePhrase, searchWholeWord,
                    searchMatchCase);
            activateSearch();
        }

        editorChangeListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                searchString.clearSearchCache();
            }
        };
        editText.addTextChangedListener(editorChangeListener);

        cursor.close();
    }

    @Override
    public void onPause() {
        super.onPause();

        editText.setEnabled(false);
        
        saveToDB();
    }

    public void saveToDB() {
        if (!ctx.dbIsOpen() || key < 0)
            return;

        Cursor cursor = ctx.getDB().fetchText(key);
        if (cursor == null)
            return;

        String path = TEditActivity.DEFAULTPATH;
        if (cursor.getColumnIndex(TEditDB.KEY_PATH) !=  -1)
            path = cursor.getString(cursor.getColumnIndex(TEditDB.KEY_PATH));
        cursor.close();

        ctx.getDB().updateText(key, path, editText.getText().toString());
        ctx.getDB().updateTextState(key, editText.getScrollY(), editText.getSelectionStart(),
                editText.getSelectionEnd(),
                ctx.getUtilityBar().getState().STATE == UtilityBar.STATE_TEXT_SEARCH ? 1 : 0,
                barSearch.getSearchPhrase(), barSearch.getReplacePhrase(),
                barSearch.isWholeWord() ? 1 : 0, barSearch.isMatchCase() ? 1 : 0);
    }

    public void activateSearch() {
        ctx.getUtilityBar().setState(barSearch);
    }

    public void findNext(String phrase) {
        searchString.setSearchPhrase(phrase);
        TextSearch.SearchResult sr;

        try {
            sr = searchString.nextSearchResult(editText.getText(), editText.getSelectionEnd());
        } catch (OutOfMemoryError e) {
            sr = null;
            Log.e("TEdit.Editor", "Out of memory while attempting a Find Next text search: "
                    + e.getMessage());
            ErrorMessage em = ErrorMessage.getInstance(getString(R.string.error),
                    getString(R.string.error_oom));
            em.show(ctx.getSupportFragmentManager(), "dialog");
        }

        if (sr == null) {
            editText.setSelection(editText.getSelectionStart());
            return;
        }

        barSearch.clearFocus();
        editText.setSelection(sr.start, sr.end);
        editText.requestFocus();
    }

    public void findPrevious(String phrase) {
        searchString.setSearchPhrase(phrase);
        TextSearch.SearchResult sr;
        try {
            sr = searchString.previousSearchResult(editText.getText(), editText.getSelectionStart());
        } catch (OutOfMemoryError e) {
            sr = null;
            Log.e("TEdit.Editor", "Out of memory while attempting a Find Previous text search: "
                    + e.getMessage());
            ErrorMessage em = ErrorMessage.getInstance(getString(R.string.error),
                    getString(R.string.error_oom));
            em.show(ctx.getSupportFragmentManager(), "dialog");
        }

        if (sr == null) {
            editText.setSelection(editText.getSelectionStart());
            return;
        }

        barSearch.clearFocus();
        editText.setSelection(sr.start, sr.end);
        editText.requestFocus();
    }

    public void replace(String phrase, String replaceWith) {
        searchString.setSearchPhrase(phrase);
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();

        TextSearch.SearchResult sr;
        try {
            sr = searchString.getSelectedResult(editText.getText(), start, end);
        } catch (OutOfMemoryError e) {
            sr = null;
            Log.e("TEdit.Editor", "Out of memory while attempting to get Selected Search Result: "
                    + e.getMessage());
            ErrorMessage em = ErrorMessage.getInstance(getString(R.string.error),
                    getString(R.string.error_oom));
            em.show(ctx.getSupportFragmentManager(), "dialog");
        }

        if (sr == null)
            return;

        Editable text;
        try {
            text = searchString.replace(editText.getText(), replaceWith, start, end);
        } catch (OutOfMemoryError e) {
            text = null;
            Log.e("TEdit.Editor", "Out of memory while attempting Replace action: "
                    + e.getMessage());
            ErrorMessage em = ErrorMessage.getInstance(getString(R.string.error),
                    getString(R.string.error_oom));
            em.show(ctx.getSupportFragmentManager(), "dialog");
        }

        if (text == null)
            return;

        barSearch.clearFocus();
        editText.removeTextChangedListener(editorChangeListener);
        final int scrollPos = editText.getScrollY();
        final int cstart = sr.start;
        final int cend = sr.start + replaceWith.length();
        editText.setText(text);
        editText.addTextChangedListener(editorChangeListener);

        editText.post(new Runnable() {
            @Override
            public void run() {
                editText.scrollTo(0, scrollPos);
                editText.setSelection(cstart, cend);
                editText.requestFocus();
            }
        });
    }

    public void replaceAll(String phrase, String replaceWith) {
        searchString.setSearchPhrase(phrase);
        Editable text = editText.getText();
        int total = searchString.getCache(text).length;

        if (total == 0) {
            Toast.makeText(ctx, "0 " + ctx.getString(R.string.items_replaced),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            text = searchString.replaceAll(editText.getText(), replaceWith);
        } catch (OutOfMemoryError e) {
            text = null;
            Log.e("TEdit.Editor", "Out of memory while attempting Replace All action: "
                    + e.getMessage());
            ErrorMessage em = ErrorMessage.getInstance(getString(R.string.error),
                    getString(R.string.error_oom));
            em.show(ctx.getSupportFragmentManager(), "dialog");
        }

        if (text == null)
            return;

        barSearch.clearFocus();
        editText.removeTextChangedListener(editorChangeListener);
        final int scrollPos = editText.getScrollY();
        final int cursorPos = editText.getSelectionStart() <= text.length() ? editText.getSelectionStart() : text.length();
        editText.setText(text);
        editText.addTextChangedListener(editorChangeListener);

        editText.post(new Runnable() {
            @Override
            public void run() {
                editText.setSelection(cursorPos);
                editText.scrollTo(0, scrollPos);
                editText.requestFocus();
            }
        });

        Toast.makeText(ctx, Integer.toString(total) + " " + ctx.getString(R.string.items_replaced),
                Toast.LENGTH_SHORT).show();
    }

    public TextSearch getSearchString() {
        return searchString;
    }
}
