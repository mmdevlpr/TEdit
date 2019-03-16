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
package com.atr.tedit;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.atr.tedit.dialog.HelpDialog;
import com.atr.tedit.file.AndPath;
import com.atr.tedit.file.FilePath;
import com.atr.tedit.file.descriptor.AndFile;
import com.atr.tedit.file.descriptor.FileDescriptor;
import com.atr.tedit.mainstate.Browser;
import com.atr.tedit.mainstate.Editor;
import com.atr.tedit.mainstate.Tabs;
import com.atr.tedit.util.DataAccessUtil;
import com.atr.tedit.dialog.ErrorMessage;
import com.atr.tedit.util.FontUtil;
import com.atr.tedit.util.TEditDB;
import com.atr.tedit.utilitybar.UtilityBar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Adam T. Ryder
 * <a href="http://1337atr.weebly.com">http://1337atr.weebly.com</a>
 */

public class TEditActivity extends AppCompatActivity {
    public static final int SWAP_ANIM_LENGTH = 300;

    public static final String DEFAULTPATH = "Untitled";
    public static final int INIT_BROWSER_PERMISSION = 0;
    public static final int INIT_TEXT_PERMISSION = 1;
    public static final int SAVE_DOCUMENT_PERMISSION = 2;
    public static final int SAVEAS_DOCUMENT_PERMISSION = 3;
    public static final int OPEN_BROWSER_PERMISSION = 4;

    public static final int SDCARD_PICKER_RESULT = 0;

    private static final int STATE_BROWSE = 0;
    private static final int STATE_TEXT = 1;
    private static final int STATE_TAB = 2;
    private static final int STATE_VOLUME_PICKER = 3;

    private int state = STATE_BROWSE;

    private DisplayMetrics dMetrics;
    private UtilityBar utilityBar;

    private TEditDB db;
    private boolean dbOpen = true;

    private FileDescriptor root;
    private FileDescriptor storageRoot;
    private AndPath currentPath;
    private AndPath savePath;

    private long lastTxt = -1;

    private Fragment frag;

    private Uri tmpUriToOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_tedit);

        FontUtil.init(this);

        dMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dMetrics);

        utilityBar = new UtilityBar((FrameLayout)findViewById(R.id.buttonbar), dMetrics,
                getResources(), this);

        db = new TEditDB(this);
        dbOpen = true;
        try {
            db.open();
        } catch (SQLException e) {
            dbOpen = false;
            Log.e("TEdit", "Unable to open database: " + e.getMessage());
        }
        if (dbOpen && savedInstanceState == null)
            db.deleteAll();

        String mediaState = Environment.getExternalStorageState();
        File rFile = Environment.getRootDirectory();
        root = AndFile.createDescriptor(rFile == null ? new File("/") : rFile);
        if (savedInstanceState == null) {
            super.onCreate(savedInstanceState);

            if (Environment.MEDIA_MOUNTED.equals(mediaState)
                    || Environment.MEDIA_MOUNTED_READ_ONLY.equals(mediaState)) {
                storageRoot = AndFile.createDescriptor(Environment.getExternalStorageDirectory());
                currentPath = new FilePath(storageRoot);
            } else {
                File sRoot = Environment.getExternalStorageDirectory();
                storageRoot = sRoot == null || !sRoot.exists() ? root
                        : AndFile.createDescriptor(Environment.getExternalStorageDirectory());
            }

            if (!dbOpen) {
                initializeToBrowser();
                return;
            }

            Uri data = getIntent().getData();
            if (data == null) {
                if (getIntent().getExtras() == null) {
                    initializeToBrowser();
                    return;
                }
                Object obj = getIntent().getExtras().get(Intent.EXTRA_STREAM);
                if (!(obj instanceof Uri)) {
                    initializeToBrowser();
                    return;
                }
                data = (Uri)obj;
            }

            initializeToText(data, false);

            return;
        }

        if (Environment.MEDIA_MOUNTED.equals(mediaState)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(mediaState)) {
            storageRoot = AndFile.createDescriptor(Environment.getExternalStorageDirectory());
            AndPath tmpPath = null;
            try {
                String strCPath = savedInstanceState.getString("TEdit.currentPath", "");
                if (!strCPath.isEmpty())
                    tmpPath = AndPath.fromJson(this, strCPath);
            } catch (Exception e) {
                tmpPath = null;
            }
            if (tmpPath != null) {
                currentPath = tmpPath;
            } else
                currentPath = new FilePath(storageRoot);

            try {
                String strSPath = savedInstanceState.getString("TEdit.savePath", "");
                if (!strSPath.isEmpty())
                    tmpPath = AndPath.fromJson(this, strSPath);
            } catch (Exception e) {
                tmpPath = null;
            }
            if (tmpPath != null) {
                savePath = tmpPath;
            }
        } else {
            File sRoot = Environment.getExternalStorageDirectory();
            storageRoot = sRoot == null || !sRoot.exists() ? root
                    : AndFile.createDescriptor(Environment.getExternalStorageDirectory());
        }

        super.onCreate(savedInstanceState);

        lastTxt = (!dbOpen) ? -1 : savedInstanceState.getLong("TEdit.lastTxt", -1);
        int lastState = savedInstanceState.getInt("TEdit.state", -1);
        frag = getSupportFragmentManager().findFragmentById(R.id.activitycontent);

        String strUri = savedInstanceState.getString("TEdit.uriToOpen", "");
        if (!strUri.isEmpty()) {
            tmpUriToOpen = Uri.parse(strUri);
            return;
        }

        switch (lastState) {
            case STATE_BROWSE:
                state = STATE_BROWSE;
                break;
            case STATE_TEXT:
                if (lastTxt != -1) {
                    state = STATE_TEXT;
                    break;
                }

                FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
                ft1.remove(frag);
                frag = Browser.newInstance(currentPath.toJson());
                ft1.add(R.id.activitycontent, frag);
                ft1.commit();
                state = STATE_BROWSE;
                break;
            case STATE_TAB:
                state = STATE_TAB;
                break;
            case STATE_VOLUME_PICKER:
                state = STATE_BROWSE;
                ((Browser)getFrag()).launchVolumePicker();
                break;
            default:
                state = STATE_BROWSE;
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.remove(frag);
                frag = Browser.newInstance(currentPath.toJson());
                ft.add(R.id.activitycontent, frag);
                ft.commit();

        }
    }

    public DisplayMetrics getMetrics() {
        return dMetrics;
    }

    public UtilityBar getUtilityBar() {
        return utilityBar;
    }

    private void initializeToBrowser() {
        initializeToBrowser(false);
    }

    private void initializeToBrowser(boolean skipPermissionCheck) {
        if (!skipPermissionCheck
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkWritePermission()) {
                requestPermission(INIT_BROWSER_PERMISSION);
                return;
            }
        }

        state = STATE_BROWSE;
        frag = Browser.newInstance(currentPath.toJson());

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.activitycontent, frag);
        ft.commit();

        if (isFirstRun()) {
            saveVersion();
            displayWhatsNew();
        }
    }

    private void initializeToText(AndFile file) {
        String content = null;
        try {
            content = DataAccessUtil.readFile(file, this);
        } catch (IOException e) {
            Log.e("TEdit", "Unable to initialize on file " + file.getPath() + ":\n" + e.getMessage());
            content = null;
        } finally {
            if (content == null) {
                lastTxt = db.createText(DEFAULTPATH, getString(R.string.error_readfile));
            } else
                lastTxt = db.createText(file.getPathIdentifier(), content);

            state = STATE_TEXT;
            frag = Editor.newInstance(lastTxt);

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.activitycontent, frag);
            ft.commit();

            if (isFirstRun()) {
                saveVersion();
                displayWhatsNew();
            }
        }
    }

    private void initializeToText(Uri uri, boolean skipPermissionCheck) {
        if (!skipPermissionCheck
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkWritePermission()) {
                tmpUriToOpen = uri;
                requestPermission(INIT_TEXT_PERMISSION);
                return;
            }
        }

        File file = DataAccessUtil.getDataFile(this, uri);
        if (file != null && file.exists() && file.isFile()) {
            if (!file.canWrite()) {
                String content = DataAccessUtil.getData(this, uri);
                initializeToText(content);
            } else
                initializeToText(AndFile.createDescriptor(file));
            return;
        }

        String content = DataAccessUtil.getData(this, uri);
        AndFile aFile = AndFile.createDescriptor(DocumentFile.fromSingleUri(this, uri));
        if (content == null) {
            lastTxt = db.createText(DEFAULTPATH, getString(R.string.error_readfile));
        } else
            lastTxt = db.createText(aFile.getPathIdentifier(), content);

        state = STATE_TEXT;
        frag = Editor.newInstance(lastTxt);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.activitycontent, frag);
        ft.commit();
        tmpUriToOpen = null;

        if (isFirstRun()) {
            saveVersion();
            displayWhatsNew();
        }
    }

    private void initializeToText(String content) {
        if (content == null) {
            lastTxt = db.createText(DEFAULTPATH, getString(R.string.error_readfile));
        } else
            lastTxt = db.createText(DEFAULTPATH, content);

        state = STATE_TEXT;
        frag = Editor.newInstance(lastTxt);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.activitycontent, frag);
        ft.commit();

        if (isFirstRun()) {
            saveVersion();
            displayWhatsNew();
        }
    }

    private void initializeToDBText() {
        if (getLastTxt() != -1) {
            Cursor cursor = getDB().fetchText(getLastTxt());
            if (cursor != null) {
                cursor.close();
                state = STATE_TEXT;
                utilityBar.setToText();
                frag = Editor.newInstance(getLastTxt());

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(R.id.activitycontent, frag);
                ft.commit();

                if (isFirstRun()) {
                    saveVersion();
                    displayWhatsNew();
                }
                return;
            }
        }

        Cursor cursor = getDB().fetchAllTexts();
        if (cursor == null || cursor.getCount() <= 0) {
            if (cursor != null)
                cursor.close();
            finish();
            return;
        }

        cursor.moveToFirst();
        if (cursor.getColumnIndex(TEditDB.KEY_ROWID) == -1) {
            cursor.close();
            finish();
            return;
        }
        long id = cursor.getLong(cursor.getColumnIndex(TEditDB.KEY_ROWID));
        cursor.close();
        lastTxt = id;
        state = STATE_TEXT;
        frag = Editor.newInstance(getLastTxt());

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.activitycontent, frag);
        ft.commit();

        if (isFirstRun()) {
            saveVersion();
            displayWhatsNew();
        }
    }

    private boolean isFirstRun() {
        SharedPreferences prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        int lastVer = prefs.getInt("version", 0);
        return lastVer < BuildConfig.VERSION_CODE;
    }

    private void saveVersion() {
        SharedPreferences.Editor prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE).edit();
        prefs.putInt("version", BuildConfig.VERSION_CODE);
        prefs.commit();
    }

    private void displayWhatsNew() {
        HelpDialog hd = HelpDialog.newInstance(R.layout.whats_new, getString(R.string.whatsnew));
        hd.show(getSupportFragmentManager(), "HelpDialog");
    }

    public long getLastTxt() {
        return lastTxt;
    }

    public void setLastTxt(long key) {
        lastTxt = key;
    }

    public void newDocument(String path, String body) {
        if (!dbOpen) {
            ErrorMessage em = ErrorMessage.getInstance(getString(R.string.alert),
                    getString(R.string.error_dbclosed));
            em.show(getSupportFragmentManager(), "dialog");
            return;
        }
        openDocument(db.createText(path, body));
    }

    public void openDocument(long key) {
        if (!dbOpen) {
            ErrorMessage em = ErrorMessage.getInstance(getString(R.string.alert),
                    getString(R.string.error_dbclosed));
            em.show(getSupportFragmentManager(), "dialog");
            return;
        }
        state = STATE_TEXT;
        lastTxt = key;
        swapFragment(Editor.newInstance(lastTxt));
    }

    public void openBrowser(AndPath path) {
        state = STATE_BROWSE;
        swapFragment(Browser.newInstance(path.toJson()));
    }

    public void saveBrowser(AndPath path) {
        state = STATE_BROWSE;
        swapFragment(Browser.newInstance(path.toJson(), lastTxt));
    }

    public void tabs() {
        Cursor cursor = db.fetchAllTexts();
        if (cursor == null || cursor.getCount() <= 0) {
            if (cursor != null)
                cursor.close();
            Toast.makeText(this, getString(R.string.noopendocuments), Toast.LENGTH_SHORT).show();
            return;
        }

        cursor.close();
        state = STATE_TAB;
        swapFragment(new Tabs());
    }

    public void saveFile(View view) {
        if (lastTxt == -1 || !(frag instanceof Browser))
            return;

        if (!dbOpen) {
            ErrorMessage em = ErrorMessage.getInstance(getString(R.string.alert),
                    getString(R.string.error_dbclosed));
            em.show(getSupportFragmentManager(), "dialog");

            return;
        }

        Cursor cursor = db.fetchText(lastTxt);
        if (cursor == null) {
            ErrorMessage em = ErrorMessage.getInstance(getString(R.string.alert),
                    getString(R.string.error_dberror));
            em.show(getSupportFragmentManager(), "dialog");
            Log.e("TEdit", "File not found in database.");

            return;
        }

        Browser browser = (Browser)frag;
        AndPath browserPath = browser.getCurrentPath();
        String body = null;
        if (cursor.getColumnIndex(TEditDB.KEY_BODY) != -1) {
            body = cursor.getString(cursor.getColumnIndex(TEditDB.KEY_BODY));
        }
        cursor.close();

        if (body == null) {
            ErrorMessage em = ErrorMessage.getInstance(getString(R.string.alert),
                    getString(R.string.error_dberror));
            em.show(getSupportFragmentManager(), "dialog");
            Log.e("TEdit", "Body not found in database.");
            return;
        }

        AndFile writtenFile = browser.saveFile(browser.getEnteredFilename(), body);
        if (writtenFile == null)
            return;

        setSavePath(browserPath);

        db.updateText(lastTxt, writtenFile.getPathIdentifier(), body);
        openDocument(lastTxt);
        Toast.makeText(this, getString(R.string.filesaved), Toast.LENGTH_SHORT).show();
    }

    private void swapFragment(Fragment newFrag) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.frag_in, R.anim.frag_out);

        ft.remove(frag);
        ft.add(R.id.activitycontent, newFrag);
        frag = newFrag;

        ft.commit();
    }

    public Fragment getFrag() {
        return frag;
    }

    public void setCurrentPath(AndPath path) {
        currentPath = path.clone();
    }

    public void setSavePath(AndPath path) {
        savePath = path.clone();
    }

    public FileDescriptor getRoot() {
        return root;
    }

    public FileDescriptor getStorageRoot() {
        return storageRoot;
    }

    public AndPath getCurrentPath() {
        return currentPath;
    }

    public AndPath getSavePath() {
        return savePath == null ? currentPath : savePath;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (state == STATE_VOLUME_PICKER) {
            state = STATE_BROWSE;
            ((Browser)getFrag()).launchVolumePicker();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("TEdit.currentPath", currentPath.toJson());
        if (savePath != null)
            outState.putString("TEdit.savePath", savePath.toJson());
        outState.putLong("TEdit.lastTxt", lastTxt);
        outState.putInt("TEdit.state", state);

        if (tmpUriToOpen != null)
            outState.putString("TEdit.uriToOpen", tmpUriToOpen.toString());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (dbOpen) {
            if (isFinishing())
                db.deleteAll();
            db.close();
        }
    }

    public TEditDB getDB() {
        return db;
    }

    public boolean dbIsOpen() {
        return dbOpen;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            switch (state) {
                case STATE_BROWSE:
                    closeBrowser();
                    break;
                case STATE_TEXT:
                    if (utilityBar.getState().STATE == UtilityBar.STATE_TEXT_SEARCH) {
                        if (!utilityBar.isAnimating())
                            utilityBar.setToText();
                    } else
                        closeText();
                    break;
                case STATE_TAB:
                    closeTabs();
                    break;
                default:
                    finish();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_S && state == STATE_TEXT) {
            if (event.isCtrlPressed()) {
                if (event.isAltPressed()) {
                    saveAsDocument(false);
                    return true;
                }

                saveDocument(false);
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    protected void closeBrowser() {
        if (!dbIsOpen()) {
            finish();
            return;
        }

        if (getLastTxt() != -1) {
            if (utilityBar.isAnimating())
                return;

            Cursor cursor = getDB().fetchText(getLastTxt());
            if (cursor != null) {
                cursor.close();
                openDocument(getLastTxt());
                return;
            }
        }

        Cursor cursor = getDB().fetchAllTexts();
        if (cursor == null || cursor.getCount() <= 0) {
            if (cursor != null)
                cursor.close();
            finish();
            return;
        }

        cursor.moveToFirst();
        if (cursor.getColumnIndex(TEditDB.KEY_ROWID) == -1) {
            cursor.close();
            finish();
            return;
        }

        long id = cursor.getLong(cursor.getColumnIndex(TEditDB.KEY_ROWID));
        cursor.close();

        if (utilityBar.isAnimating())
            return;

        openDocument(id);
    }

    protected void closeText() {
        if (utilityBar.isAnimating())
            return;

        if (!dbIsOpen()) {
            setLastTxt(-1);
            openBrowser(getCurrentPath());
            return;
        }

        getDB().deleteText(getLastTxt());
        Cursor cursor = getDB().fetchAllTexts();
        if (cursor == null || cursor.getCount() <= 0) {
            if (cursor != null)
                cursor.close();
            setLastTxt(-1);
            openBrowser(getCurrentPath());
            return;
        }

        cursor.moveToFirst();
        if (cursor.getColumnIndex(TEditDB.KEY_ROWID) == -1) {
            cursor.close();
            setLastTxt(-1);
            openBrowser(getCurrentPath());
            return;
        }
        long id = cursor.getLong(cursor.getColumnIndex(TEditDB.KEY_ROWID));
        cursor.close();
        openDocument(id);
    }

    protected void closeTabs() {
        if (utilityBar.isAnimating())
            return;

        if (!dbIsOpen()) {
            setLastTxt(-1);
            openBrowser(getCurrentPath());
            return;
        }

        if (getLastTxt() != -1) {
            Cursor cursor = getDB().fetchText(getLastTxt());
            if (cursor != null) {
                cursor.close();
                openDocument(getLastTxt());
                return;
            }
        }

        Cursor cursor = getDB().fetchAllTexts();
        if (cursor == null || cursor.getCount() <= 0) {
            if (cursor != null)
                cursor.close();
            setLastTxt(-1);
            openBrowser(getCurrentPath());
            return;
        }

        cursor.moveToFirst();
        if (cursor.getColumnIndex(TEditDB.KEY_ROWID) == -1) {
            cursor.close();
            setLastTxt(-1);
            openBrowser(getCurrentPath());
            return;
        }
        long id = cursor.getLong(cursor.getColumnIndex(TEditDB.KEY_ROWID));
        cursor.close();
        openDocument(id);
    }

    public void saveDocument(boolean skipPermissionCheck) {
        if (!skipPermissionCheck
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !checkWritePermission()) {
            requestPermission(SAVE_DOCUMENT_PERMISSION);
            return;
        }

        if (!dbIsOpen()) {
            Log.e("TEdit", "Unable to save file: Database is not open.");
            ErrorMessage em = ErrorMessage.getInstance(getString(R.string.alert),
                    getString(R.string.error_dbclosed));
            em.show(getSupportFragmentManager(), "dialog");
            return;
        }

        if (getLastTxt() == -1)
            return;

        ((Editor)getFrag()).saveToDB();
        Cursor cursor = getDB().fetchText(getLastTxt());
        if (cursor == null || cursor.getColumnIndex(TEditDB.KEY_PATH) == -1
                || cursor.getColumnIndex(TEditDB.KEY_BODY) ==  -1) {
            if (cursor == null) {
                Log.e("TEdit", "Unable to save file: Database did not contain key.");
            } else
                Log.e("TEdit", "Unable to save file: Database did not contain column");
            ErrorMessage em = ErrorMessage.getInstance(getString(R.string.alert),
                    getString(R.string.error_dberror));
            em.show(getSupportFragmentManager(), "dialog");
            cursor.close();
            return;
        }

        String path = cursor.getString(cursor.getColumnIndex(TEditDB.KEY_PATH));
        if (path.equals(TEditActivity.DEFAULTPATH)) {
            saveBrowser(getSavePath());
            cursor.close();
            return;
        }

        AndFile file = AndFile.createDescriptor(path, this);
        if (!file.canWrite()) {
            saveAsDocument(skipPermissionCheck);
            return;
        }
        
        String mediaState = Environment.getExternalStorageState();
        if (!(Environment.MEDIA_MOUNTED.equals(mediaState)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(mediaState))) {
            ErrorMessage em = ErrorMessage.getInstance(getString(R.string.alert),
                    getString(R.string.error_unmounted));
            em.show(getSupportFragmentManager(), "dialog");
            cursor.close();
            return;
        } else if (!file.canWrite()) {
            ErrorMessage em = ErrorMessage.getInstance(getString(R.string.alert),
                    getString(R.string.error_protectedpath));
            em.show(getSupportFragmentManager(), "dialog");
            cursor.close();
            return;
        }

        String body = cursor.getString(cursor.getColumnIndex(TEditDB.KEY_BODY));
        cursor.close();
        try {
            DataAccessUtil.writeFile(file, this, body);
            Toast.makeText(this, R.string.filesaved, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && !checkWritePermission()) {
                Log.e("TEdit.Browser", "Unable to save file " + file.getPath() + ". Permission denied: "
                        + e.getMessage());
                ErrorMessage em = ErrorMessage.getInstance(getString(R.string.alert),
                        getString(R.string.error_nowritepermission));
                em.show(getSupportFragmentManager(), "dialog");
            } else {
                Log.e("TEdit.Editor", "Unable to save file " + file.getPath() + ": "
                        + e.getMessage());
                ErrorMessage em = ErrorMessage.getInstance(getString(R.string.alert),
                        getString(R.string.error_writefile));
                em.show(getSupportFragmentManager(), "dialog");
            }
        }
    }

    public void saveAsDocument(boolean skipPermissionCheck) {
        if (!skipPermissionCheck
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !checkWritePermission()) {
            requestPermission(SAVEAS_DOCUMENT_PERMISSION);
            return;
        }

        if (!dbIsOpen()) {
            Log.e("TEdit", "Unable to save file: Database is not open.");
            ErrorMessage em = ErrorMessage.getInstance(getString(R.string.alert),
                    getString(R.string.error_dbclosed));
            em.show(getSupportFragmentManager(), "dialog");
            return;
        }

        if (getLastTxt() == -1)
            return;

        ((Editor)getFrag()).saveToDB();
        Cursor cursor = getDB().fetchText(getLastTxt());
        if (cursor == null || cursor.getColumnIndex(TEditDB.KEY_PATH) == -1
                || cursor.getColumnIndex(TEditDB.KEY_BODY) ==  -1) {
            if (cursor == null) {
                Log.e("TEdit", "Unable to save file: Database did not contain key.");
            } else
                Log.e("TEdit", "Unable to save file: Database did not contain column");
            ErrorMessage em = ErrorMessage.getInstance(getString(R.string.alert),
                    getString(R.string.error_dberror));
            em.show(getSupportFragmentManager(), "dialog");
            cursor.close();
            return;
        }

        saveBrowser(getSavePath());
    }

    public void requestOpenBrowser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !checkWritePermission()) {
            requestPermission(SAVEAS_DOCUMENT_PERMISSION);
            return;
        }

        openBrowser(getCurrentPath());
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean checkWritePermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermission(int requestState) {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestState);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[]results) {
        switch(requestCode) {
            case SAVE_DOCUMENT_PERMISSION:
                if (results.length > 0
                        && results[0] == PackageManager.PERMISSION_GRANTED) {
                    saveDocument(true);
                } else {
                    ErrorMessage em = ErrorMessage.getInstance(getString(R.string.alert),
                            getString(R.string.error_nowritepermission));
                    em.show(getSupportFragmentManager(), "dialog");
                }
                break;
            case SAVEAS_DOCUMENT_PERMISSION:
                if (results.length > 0
                        && results[0] == PackageManager.PERMISSION_GRANTED) {
                    saveAsDocument(true);
                } else {
                    ErrorMessage em = ErrorMessage.getInstance(getString(R.string.alert),
                            getString(R.string.error_nowritepermission));
                    em.show(getSupportFragmentManager(), "dialog");
                }
                break;
            case OPEN_BROWSER_PERMISSION:
                if (results.length > 0
                        && results[0] == PackageManager.PERMISSION_GRANTED) {
                    openBrowser(getCurrentPath());
                } else {
                    ErrorMessage em = ErrorMessage.getInstance(getString(R.string.alert),
                            getString(R.string.error_nowritepermission));
                    em.show(getSupportFragmentManager(), "dialog");
                }
                break;
            case INIT_BROWSER_PERMISSION:
                initializeToBrowser(true);
                break;
            case INIT_TEXT_PERMISSION:
                if (results.length > 0
                        && results[0] == PackageManager.PERMISSION_GRANTED) {
                    if (tmpUriToOpen == null) {
                        initializeToBrowser(true);
                    } else
                        initializeToText(tmpUriToOpen, true);
                } else if (!dbIsOpen()) {
                    finish();
                } else if (tmpUriToOpen != null && DocumentFile.fromSingleUri(this, tmpUriToOpen).canRead()) {
                    initializeToText(tmpUriToOpen, true);
                } else {
                    initializeToDBText();
                }
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public Uri[] getPermittedUris() {
        List<UriPermission> uriPermissions = getContentResolver().getPersistedUriPermissions();
        ArrayList<Uri> uris = new ArrayList<>();
        for (UriPermission p : uriPermissions) {
            if (p.isReadPermission())
                uris.add(p.getUri());
        }

        Uri[] volumes;
        if (!uris.isEmpty()) {
            volumes = uris.toArray(new Uri[uris.size()]);
        } else
            volumes = new Uri[0];

        return volumes;
    }

    public void launchSDcardIntent() {
        Log.i("TEdit", "Launching SDcard locater intent.");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, TEditActivity.SDCARD_PICKER_RESULT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == SDCARD_PICKER_RESULT) {
            state = STATE_VOLUME_PICKER;
            if (resultCode != RESULT_OK)
                return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                processExternalVolumeAccess(resultData.getData());
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void processExternalVolumeAccess(Uri treeUri) {
        getContentResolver().takePersistableUriPermission(treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }
}
