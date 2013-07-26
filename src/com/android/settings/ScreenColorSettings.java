/*
   Copyright (c) 2013, The Linux Foundation. All Rights Reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.android.settings;

import java.io.FileNotFoundException;
import java.io.InputStream;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.PopupWindow;

import com.android.display.IPPService;

public class ScreenColorSettings extends Activity implements SeekBar.OnSeekBarChangeListener,
        View.OnClickListener {
    private final static String TAG = "ScreenColorSettings";

    private static final int RESTORE_DEFAULT_PREVIEW = 0;
    private static final int SELECT_NEW_FILE_ITEM = 1;
    private static final int SELECT_FILE_ITEM = 2;
    private static final int RESET = 3;
    private static final int SELECT_FILE_GROUP1 = 0;
    private static final int SELECT_FILE_GROUP2 = 1;
    private static final int SELECT_FILE_ORDER = 0;
    private static final int REQUEST_SELECT_FILE = 100;
    private static final String IMAGE_UNSPECIFIED = "image/*";
    private static final String GALLERY_CLASSNAME = "com.android.gallery3d.app.Wallpaper";
    private static final String GALLERY_PACKAGENAME = "com.android.gallery3d";
    private static final String PREVIEW_STRING_KEY = "screencolor_preview_key";
    private static final String PREVIEW_STRING_NAME = "screencolor_preview_name";
    private static final String COLOR_HUE = "hue";
    private static final String COLOR_SATURATION = "saturation";
    private static final String COLOR_INTENSITY = "intensity";
    private static final String COLOR_CONTRAST = "contrast";

    private static final String KEY_ASPECT_X = "aspectX";
    private static final String KEY_ASPECT_Y = "aspectY";
    private static final String KEY_SPOTLIGHT_X = "spotlightX";
    private static final String KEY_SPOTLIGHT_Y = "spotlightY";
    private static final String KEY_FROME_SCREENCOLOR = "fromScreenColor";
    private static final int ASPECT_X = 480;
    private static final int ASPECT_Y = 800;
    private static final float SPOTLIGHT_X = 0;
    private static final float SPOTLIGHT_Y = 0;

    private ImageView mImageView;
    private RelativeLayout mRLayout;
    private LinearLayout mScreenColorLayout;
    private SeekBar mHBar, mSBar, mCBar, mIBar;
    private TextView mHTv, mSTv, mCTv, mITv;
    private ImageView mReduceH, mIncreaseH, mReduceS, mIncreaseS, mReduceC, mIncreaseC, mReduceI,
            mIncreaseI, mUpdown, mMore;
    private Button mCancelBtn, mSaveBtn, mPreviousBtn, mNewBtn;
    private boolean canRestorePreview;
    private int mHueValue = 0;
    private int mSaturationValue = 0;
    private int mIntensityValue = 0;
    private int mContrastValue = 0;

    /**
     * The Service runs in vendor/qcom/proprietary/mm-core directory.
     *  It is used to update the screen's hue, saturation, contrast, and intensity.
     */
    IPPService mPPService = null;
    PPServiceConnection mPPServiceConn = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initPPService();
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.screencolor_settings);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreSavedHSCI(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mPPServiceConn);
    }

    private void initView() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        mRLayout = (RelativeLayout) findViewById(R.id.background_preview);
        String previewContent = getScreenColorPreviewKey();
        if ("".equals(previewContent)) {
            resotreBackgroundByDefault();
        } else {
            Uri previewUri = Uri.parse(previewContent);
            setBackgroundByUri(previewUri);
        }

        mScreenColorLayout = (LinearLayout) findViewById(R.id.screencolor_control);
        mScreenColorLayout.setBackgroundColor(R.color.screencolor_background);
        mScreenColorLayout.setOnClickListener(this);

        mHBar = (SeekBar) findViewById(R.id.hcontrol);
        mHBar.setOnSeekBarChangeListener(this);
        mSBar = (SeekBar) findViewById(R.id.scontrol);
        mSBar.setOnSeekBarChangeListener(this);
        mIBar = (SeekBar) findViewById(R.id.icontrol);
        mIBar.setOnSeekBarChangeListener(this);
        mCBar = (SeekBar) findViewById(R.id.ccontrol);
        mCBar.setOnSeekBarChangeListener(this);

        mHTv = (TextView) findViewById(R.id.hue);
        mHTv.setText(getString(R.string.hue_str, mHBar.getProgress()));
        mSTv = (TextView) findViewById(R.id.saturation);
        mSTv.setText(getString(R.string.saturation_str, mSBar.getProgress()));
        mITv = (TextView) findViewById(R.id.intensity);
        mITv.setText(getString(R.string.intensity_str, mIBar.getProgress()));
        mCTv = (TextView) findViewById(R.id.contrast);
        mCTv.setText(getString(R.string.contrast_str, mCBar.getProgress()));

        mReduceH = (ImageView) findViewById(R.id.reduce_hue);
        mReduceH.setOnClickListener(this);
        mIncreaseH = (ImageView) findViewById(R.id.increase_hue);
        mIncreaseH.setOnClickListener(this);
        mReduceS = (ImageView) findViewById(R.id.reduce_saturation);
        mReduceS.setOnClickListener(this);
        mIncreaseS = (ImageView) findViewById(R.id.increase_saturation);
        mIncreaseS.setOnClickListener(this);
        mReduceI = (ImageView) findViewById(R.id.reduce_intensity);
        mReduceI.setOnClickListener(this);
        mIncreaseI = (ImageView) findViewById(R.id.increase_intensity);
        mIncreaseI.setOnClickListener(this);
        mReduceC = (ImageView) findViewById(R.id.reduce_contrast);
        mReduceC.setOnClickListener(this);
        mIncreaseC = (ImageView) findViewById(R.id.increase_contrast);
        mIncreaseC.setOnClickListener(this);

        mUpdown = (ImageView) findViewById(R.id.up_down);
        mUpdown.setOnClickListener(this);
        mMore = (ImageView) findViewById(R.id.more);
        mMore.setOnClickListener(this);

        mPreviousBtn = (Button) findViewById(R.id.previous_btn);
        mPreviousBtn.setOnClickListener(this);
        mNewBtn = (Button) findViewById(R.id.new_btn);
        mNewBtn.setOnClickListener(this);
        mCancelBtn = (Button) findViewById(R.id.cancel);
        mCancelBtn.setOnClickListener(this);
        mSaveBtn = (Button) findViewById(R.id.save);
        mSaveBtn.setOnClickListener(this);

        initBtnsStatus();
    }

    private void initPPService() {
        mPPServiceConn = new PPServiceConnection();
        Intent i = new Intent(IPPService.class.getName());
        boolean ret = bindService(i, mPPServiceConn, Context.BIND_AUTO_CREATE);
    }

    private class PPServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPPService = IPPService.Stub.asInterface((IBinder) service);
            Log.d(TAG, "onServiceConnected, service=" + mPPService);
            try {
                if (null != mPPService) {
                    mPPService.startPP();
                    restoreSavedHSCI(true);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "startPP exception");
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (null != mPPService) {
                // end thread
                try {
                    Log.d(TAG, "stopPP");
                    mPPService.stopPP();
                } catch (RemoteException e) {
                    Log.e(TAG, "stopPP exception");
                }
            }
            mPPService = null;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            Log.i(TAG, "bail due to resultCode=" + resultCode);
            return;
        }
        switch (requestCode) {
            case REQUEST_SELECT_FILE:
                changePreviewByData(data);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(SELECT_FILE_GROUP1, SELECT_FILE_ITEM, SELECT_FILE_ORDER,
                R.string.selectfile_menu);
        menu.add(SELECT_FILE_GROUP1, RESET, SELECT_FILE_ORDER, R.string.restore_default_str);
        menu.add(SELECT_FILE_GROUP2, RESTORE_DEFAULT_PREVIEW, SELECT_FILE_ORDER,
                R.string.restore_preview);
        menu.add(SELECT_FILE_GROUP2, SELECT_NEW_FILE_ITEM, SELECT_FILE_ORDER,
                R.string.selectnewfile_menu);
        menu.add(SELECT_FILE_GROUP2, RESET, SELECT_FILE_ORDER, R.string.restore_default_str);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (canRestorePreview) {
            menu.setGroupVisible(SELECT_FILE_GROUP1, false);
            menu.setGroupVisible(SELECT_FILE_GROUP2, true);
        } else {
            menu.setGroupVisible(SELECT_FILE_GROUP1, true);
            menu.setGroupVisible(SELECT_FILE_GROUP2, false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case SELECT_NEW_FILE_ITEM:
                selectPicFromGallery2();
                break;
            case SELECT_FILE_ITEM:
                selectPicFromGallery2();
                break;
            case RESTORE_DEFAULT_PREVIEW:
                resotreBackgroundByDefault();
                break;
            case RESET:
                restoreDefaultHSCI();
                break;
            default:
                return true;
        }
        return false;
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        setNewBtnHighlight();
        int id = seekBar.getId();
        switch (id) {
            case R.id.hcontrol:
                mHueValue = progress;
                //The screen hue display value varies from -180 to 180.
                mHTv.setText(getString(R.string.hue_str, progress - 180));
                break;
            case R.id.scontrol:
                mSaturationValue = progress;
                //Change the saturation display value varies from (-180, 180) to (-1.0, 1.0).
                mSTv.setText(getString(R.string.saturation_str,
                        ((int) (((progress - 180) * 100) / 180.0)) / 100.0));
                break;
            case R.id.ccontrol:
                mContrastValue = progress;
                //The screen contrast display value varies from -255 to 255.
                mCTv.setText(getString(R.string.contrast_str, progress - 255));
                break;
            case R.id.icontrol:
                mIntensityValue = progress;
                //Change the intensity display value varies from (-180, 180) to (-1.0, 1.0).
                mITv.setText(getString(R.string.intensity_str,
                        ((int) (((progress - 180) * 100) / 180.0)) / 100.0));
                break;
            default:
                break;

        }
        Log.i(TAG, "onProgressChanged mHueValue=" + mHueValue + " mSaturationValue="
                + mSaturationValue + " mNewContrastValue=" + mContrastValue + " mIntensityValue="
                + mIntensityValue);
        try {
            if (null != mPPService && mPPService.getPPStatus()) {
                mPPService.updateHSIC(mHueValue, mSaturationValue, mIntensityValue,
                                     mContrastValue);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "updateHSIC exception");
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        setActivated(seekBar, true);
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        setActivated(seekBar, false);
    }

    private void setActivated(SeekBar seekBar, boolean isActivated) {
        int id = seekBar.getId();
        switch (id) {
            case R.id.hcontrol:
                mReduceH.setActivated(isActivated);
                mIncreaseH.setActivated(isActivated);
                break;
            case R.id.scontrol:
                mReduceS.setActivated(isActivated);
                mIncreaseS.setActivated(isActivated);
                break;
            case R.id.icontrol:
                mReduceI.setActivated(isActivated);
                mIncreaseI.setActivated(isActivated);
                break;
            case R.id.ccontrol:
                mReduceC.setActivated(isActivated);
                mIncreaseC.setActivated(isActivated);
                break;
            default:
                break;
        }

    }

    public void onClick(View v) {
        int id = v.getId();
        /**
         * Hue  -> Valid from 0 to 360
         * Saturation -> Valid from 0 to 360
         * Intensity  -> Valid from 0 to 510
         * Contrast  -> Valid from 0 to 360
         */
        int minHueValue = getResources().getInteger(R.integer.minimum_hue_value);
        int maxHueValue = getResources().getInteger(R.integer.maximum_hue_value);
        int minSaturationValue = getResources().getInteger(R.integer.minimum_saturation_value);
        int maxSaturationValue = getResources().getInteger(R.integer.maximum_saturation_value);
        int minIntensityValue = getResources().getInteger(R.integer.minimum_intensity_value);
        int maxIntensityValue = getResources().getInteger(R.integer.maximum_intensity_value);
        int minContrastValue = getResources().getInteger(R.integer.minimum_contrast_value);
        int maxContrastValue = getResources().getInteger(R.integer.maximum_contrast_value);

        switch (id) {
            case R.id.cancel:
                finish();
                break;
            case R.id.save:
                saveHSCI();
                break;
            case R.id.previous_btn:
                previousOrNewHSCI(true);
                break;
            case R.id.new_btn:
                previousOrNewHSCI(false);
                break;
            case R.id.up_down:
                upDownHSCISettingLayout();
                break;
            case R.id.more:
                showMoreMenus();
                break;
            case R.id.reduce_hue:
                mHBar.setProgress((mHueValue - 1 > minHueValue) ? (mHueValue - 1) : minHueValue);
                break;
            case R.id.increase_hue:
                mHBar.setProgress((mHueValue + 1 < maxHueValue) ? (mHueValue + 1) : maxHueValue);
                break;
            case R.id.reduce_saturation:
                mSBar.setProgress((mSaturationValue - 1 > minSaturationValue) ?
                    (mSaturationValue - 1) : minSaturationValue);
                break;
            case R.id.increase_saturation:
                mSBar.setProgress((mSaturationValue + 1 < maxSaturationValue) ?
                    (mSaturationValue + 1) : maxSaturationValue);
                break;
            case R.id.reduce_intensity:
                mIBar.setProgress((mIntensityValue - 1 > minIntensityValue) ?
                    (mIntensityValue - 1) : minIntensityValue);
                break;
            case R.id.increase_intensity:
                mIBar.setProgress((mIntensityValue + 1 < maxIntensityValue) ?
                    (mIntensityValue + 1) : maxIntensityValue);
                break;
            case R.id.reduce_contrast:
                mCBar.setProgress((mContrastValue - 1 > minContrastValue) ?
                    (mContrastValue - 1) : minContrastValue);
                break;
            case R.id.increase_contrast:
                mCBar.setProgress((mContrastValue + 1 < maxContrastValue) ?
                    (mContrastValue + 1) : maxContrastValue);
                break;
            default:
                break;
        }
    }

    private void selectPicFromGallery2() {
        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
        intent.setComponent(new ComponentName(GALLERY_PACKAGENAME, GALLERY_CLASSNAME));
        Bundle bundle = new Bundle();
        // define the width and heigh to crop the image.
        bundle.putInt(KEY_ASPECT_X, ASPECT_X);
        bundle.putInt(KEY_ASPECT_Y, ASPECT_Y);
        bundle.putFloat(KEY_SPOTLIGHT_X, SPOTLIGHT_X);
        bundle.putFloat(KEY_SPOTLIGHT_Y, SPOTLIGHT_Y);
        // send true to set CropImage view's title is not set wallpaper.
        bundle.putBoolean(KEY_FROME_SCREENCOLOR, true);
        intent.putExtras(bundle);
        startActivityForResult(intent, REQUEST_SELECT_FILE);
    }

    private void restoreDefaultHSCI() {
        // TODO restore default HSCI values.
        mHBar.setProgress(0);
        mSBar.setProgress(0);
        mIBar.setProgress(0);
        mCBar.setProgress(0);
        setNewBtnHighlight();
    }

    private void restoreSavedHSCI(boolean setProgress) {
        SharedPreferences share = getSharedPreferences(PREVIEW_STRING_NAME,
                Context.MODE_WORLD_WRITEABLE);
        int hueValue = share.getInt(COLOR_HUE, 0);
        int saturationValue = share.getInt(COLOR_SATURATION, 0);
        int intensityValue = share.getInt(COLOR_INTENSITY, 0);
        int contrastValue = share.getInt(COLOR_CONTRAST, 0);
        if (setProgress) {
            mHBar.setProgress(hueValue);
            mSBar.setProgress(saturationValue);
            mIBar.setProgress(intensityValue);
            mCBar.setProgress(contrastValue);
        }
        Log.i(TAG, "restoreSavedHSCI hueValue=" + hueValue + " saturationValue=" + saturationValue
                + " contrastValue=" + contrastValue + " intensityValue=" + intensityValue);
        try {
            if (null != mPPService && mPPService.getPPStatus()) {
                mPPService.updateHSIC(hueValue, saturationValue, intensityValue, contrastValue);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "updateHSIC exception");
        }
    }

    private void previewNewHSIC() {
        mHBar.setProgress(mHueValue);
        mSBar.setProgress(mSaturationValue);
        mIBar.setProgress(mIntensityValue);
        mCBar.setProgress(mContrastValue);
        Log.i(TAG, "previewNewHSIC mHueValue=" + mHueValue + " mSaturationValue="
                + mSaturationValue + " mContrastValue=" + mContrastValue + " mIntensityValue="
                + mIntensityValue);
        try {
            if (null != mPPService && mPPService.getPPStatus()) {
                mPPService.updateHSIC(mHueValue, mSaturationValue, mIntensityValue,
                        mContrastValue);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "updateHSIC exception");
        }
    }

    private void setNewBtnHighlight() {
        mPreviousBtn.setBackgroundResource(R.drawable.ic_previous_default);
        mNewBtn.setBackgroundResource(R.drawable.ic_new_glow);
        mPreviousBtn.setEnabled(true);
        mNewBtn.setEnabled(true);
        mSaveBtn.setEnabled(true);

    }

    private void initBtnsStatus() {
        mPreviousBtn.setBackgroundResource(R.drawable.ic_previous_dis);
        mNewBtn.setBackgroundResource(R.drawable.ic_new_dis);
        mPreviousBtn.setEnabled(false);
        mNewBtn.setEnabled(false);
        mSaveBtn.setEnabled(false);
    }

    private void previousOrNewHSCI(boolean isPrevious) {
        if (View.GONE != mScreenColorLayout.getVisibility()) {
            mScreenColorLayout.setVisibility(View.GONE);
            mUpdown.setBackgroundResource(R.drawable.up_button);
        }
        if (isPrevious) {
            mPreviousBtn.setBackgroundResource(R.drawable.ic_previous_glow);
            mNewBtn.setBackgroundResource(R.drawable.ic_new_default);
            mUpdown.setVisibility(View.GONE);
            mMore.setVisibility(View.GONE);
            restoreSavedHSCI(false);
        } else {
            mPreviousBtn.setBackgroundResource(R.drawable.ic_previous_default);
            mNewBtn.setBackgroundResource(R.drawable.ic_new_glow);
            mUpdown.setVisibility(View.VISIBLE);
            mMore.setVisibility(View.VISIBLE);
            previewNewHSIC();
        }
    }

    private void saveHSCI() {
        // TODO save HSCI values.
        saveHSICPreference(mHueValue, mSaturationValue, mIntensityValue, mContrastValue);
        finish();
    }

    private void changePreviewByData(Intent data) {
        if (data != null) {
            Uri uri = data.getData();
            setBackgroundByUri(uri);
            savePreviewKey(uri.toString());
        }
    }

    private void setBackgroundByUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bm = BitmapFactory.decodeStream(is);
            BitmapDrawable bd = new BitmapDrawable(bm);
            mRLayout.setBackgroundDrawable(bd);
        } catch (FileNotFoundException e) {
        }
        canRestorePreview = true;
    }

    private void resotreBackgroundByDefault() {
        mRLayout.setBackgroundResource(R.drawable.default_screencolor_setting);
        savePreviewKey("");
        canRestorePreview = false;
    }

    // use sharepreference to save preview image.
    private void savePreviewKey(String value) {
        SharedPreferences share = getSharedPreferences(PREVIEW_STRING_NAME, Context.MODE_PRIVATE);
        Editor editor = share.edit();
        editor.putString(PREVIEW_STRING_KEY, value);
        editor.commit();
    }

    private void saveHSICPreference(int h, int s, int i, int c) {
        SharedPreferences share = getSharedPreferences(PREVIEW_STRING_NAME, Context.MODE_PRIVATE);
        Editor editor = share.edit();
        editor.putInt(COLOR_HUE, h);
        editor.putInt(COLOR_SATURATION, s);
        editor.putInt(COLOR_INTENSITY, i);
        editor.putInt(COLOR_CONTRAST, c);
        editor.commit();
    }

    private String getScreenColorPreviewKey() {
        SharedPreferences share = getSharedPreferences(PREVIEW_STRING_NAME,
                Context.MODE_WORLD_WRITEABLE | Context.MODE_WORLD_READABLE);
        return share.getString(PREVIEW_STRING_KEY, "");
    }

    private void upDownHSCISettingLayout() {
        if (View.GONE != mScreenColorLayout.getVisibility()) {
            mScreenColorLayout.setVisibility(View.GONE);
            mUpdown.setBackgroundResource(R.drawable.up_button);
        } else {
            mScreenColorLayout.setVisibility(View.VISIBLE);
            mUpdown.setBackgroundResource(R.drawable.down_button);
        }
    }

    private void showMoreMenus() {
        PopupMenu popup = new PopupMenu(ScreenColorSettings.this, mMore);
        Menu menu = popup.getMenu();
        popup.getMenuInflater().inflate(R.menu.screencolor_more, menu);

        if (canRestorePreview) {
            menu.removeItem(R.id.selectfile_menu);
        } else {
            menu.removeItem(R.id.restore_preview);
            menu.removeItem(R.id.selectnewfile_menu);
        }

        popup.setOnMenuItemClickListener(new MyMenuItemclick());
        popup.show();
    }

    private class MyMenuItemclick implements OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.restore_preview:
                    resotreBackgroundByDefault();
                    return true;
                case R.id.selectnewfile_menu:
                    selectPicFromGallery2();
                    return true;
                case R.id.selectfile_menu:
                    selectPicFromGallery2();
                    return true;
                case R.id.restore_default_str:
                    restoreDefaultHSCI();
                    return true;
            }
            return true;
        }
    }
}
