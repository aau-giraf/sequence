package dk.aau.cs.giraf.zebra;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import dk.aau.cs.giraf.gui.GComponent;
import dk.aau.cs.giraf.gui.GSeekBar;
import dk.aau.cs.giraf.gui.GTextView;
import dk.aau.cs.giraf.oasis.lib.Helper;

public class SettingsActivity extends Activity {
    private boolean assumeMinimize = true;
    private int pictogramSetting = 500;
    private boolean landscapeSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setColors();
        getSettings();
        setupOrientationSetting();
        setupPictogramSetting();
    }

    private void setColors() {

        LinearLayout backgroundLayout = (LinearLayout) findViewById(R.id.parent_container);
        RelativeLayout topbarLayout = (RelativeLayout) findViewById(R.id.settings_bar);
        backgroundLayout.setBackgroundDrawable(GComponent.GetBackground(GComponent.Background.SOLID));
        topbarLayout.setBackgroundDrawable(GComponent.GetBackground(GComponent.Background.SOLID));
    }

    private void getSettings() {
        Helper helper = new Helper(this);

        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }

        //pictogramOption = ;
        //orientationOption = ;
    }

    private void setupOrientationSetting() {
        RadioGroup orientationButtons = (RadioGroup) findViewById(R.id.RadioButtons);
        if (landscapeSetting == true) {
            orientationButtons.check(R.id.landscapeButton);
        }
        else {
            orientationButtons.check(R.id.portraitButton);
        }

    }

    public void onOrientationClick(View v) {
        switch(v.getId()) {
            case R.id.landscapeButton:
                landscapeSetting = true;
                break;
            case R.id.portraitButton:
                landscapeSetting = false;
                break;
        }
        Log.d("DebugYeah", "LandscapeOption is " + Boolean.toString(landscapeSetting));
    }

    private void setupPictogramSetting() {
        final GSeekBar pictogramSlider = (GSeekBar) findViewById(R.id.pictogram_slider);
        final GTextView pictogramSliderText = (GTextView) findViewById(R.id.pictogram_slider_text);
        pictogramSliderText.setText(Integer.toString(pictogramSetting) + " piktogrammer");

        pictogramSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                pictogramSetting = (int) Math.round(progress/14.285714285714285714285714285714);
                pictogramSliderText.setText(pictogramSetting + " piktogrammer");
            }

            @Override
            public void onStartTrackingTouch (SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch (SeekBar seekBar) {

            }

        });
    }

    private void saveSettings() {
        Helper helper = new Helper(this);

        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }

    }

    private void finishActivity(){
        assumeMinimize = false;
        finish();
    }

    @Override
    public void onBackPressed() {

        saveSettings();
        finishActivity();
    }

    @Override
    protected void onStop() {
        //assumeMinimize kills the entire application if minimized
        // in any other ways than opening Pictosearch or inserting a nested Sequence
        if (assumeMinimize) {
            MainActivity.activityToKill.finish();
            finishActivity();
        }
        else {
            assumeMinimize = true;
        }
        super.onStop();
    }
}
