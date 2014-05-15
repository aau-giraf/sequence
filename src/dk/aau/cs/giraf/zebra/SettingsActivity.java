package dk.aau.cs.giraf.zebra;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import dk.aau.cs.giraf.gui.GComponent;
import dk.aau.cs.giraf.gui.GSeekBar;
import dk.aau.cs.giraf.gui.GTextView;

public class SettingsActivity extends Activity {
    private boolean assumeMinimize = true;
    private Integer pictogramSetting;
    private Boolean landscapeSetting;
    private String childId = Integer.toString(MainActivity.selectedChild.getId());
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setColors();
        getSettingsByChild();
        setupScreenSetting();
        setupShownPictogramSetting();
    }

    private void setColors() {
        LinearLayout backgroundLayout = (LinearLayout) findViewById(R.id.parent_container);
        RelativeLayout topbarLayout = (RelativeLayout) findViewById(R.id.settings_bar);
        backgroundLayout.setBackgroundDrawable(GComponent.GetBackground(GComponent.Background.SOLID));
        topbarLayout.setBackgroundDrawable(GComponent.GetBackground(GComponent.Background.SOLID));
    }

    private void getSettingsByChild() {
        //Get settings from "SettingsActivity<childId>. If not available, load standard values"
        settings = getSharedPreferences(SettingsActivity.class.getName() + childId, MODE_PRIVATE);
        pictogramSetting = settings.getInt("pictogramSetting", 5);
        landscapeSetting = settings.getBoolean("landscapeSetting", true);
    }

    private void setupScreenSetting() {
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
    }

    private void setupShownPictogramSetting() {
        final GSeekBar pictogramSlider = (GSeekBar) findViewById(R.id.pictogram_slider);
        final GTextView pictogramSliderText = (GTextView) findViewById(R.id.pictogram_slider_text);

        //Set slider and text to initial values. Slider progress works in percent, but is divided to limit the option between 1-7
        pictogramSliderText.setText(Integer.toString(pictogramSetting) + " piktogrammer");
        pictogramSlider.setProgress((int) Math.round(pictogramSetting*14.285714285714285714285714285714));

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
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("pictogramSetting", pictogramSetting);
        editor.putBoolean("landscapeSetting", landscapeSetting);
        editor.commit();
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
        //assumeMinimize kills the entire application if minimized while assumeMinimize is true
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
