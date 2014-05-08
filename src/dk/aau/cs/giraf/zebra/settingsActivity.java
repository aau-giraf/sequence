package dk.aau.cs.giraf.zebra;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import dk.aau.cs.giraf.gui.GComponent;
import dk.aau.cs.giraf.gui.GSeekBar;
import dk.aau.cs.giraf.gui.GTextView;

public class SettingsActivity extends Activity {
    private boolean assumeMinimize = true;
    private int pictogramOption = 500;
    private int orientationOption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setColors();
        getOptions();
        setupOrientationOption();
        setupPictogramOption();
    }

    private void setColors() {

        LinearLayout backgroundLayout = (LinearLayout) findViewById(R.id.parent_container);
        RelativeLayout topbarLayout = (RelativeLayout) findViewById(R.id.settings_bar);
        backgroundLayout.setBackgroundDrawable(GComponent.GetBackground(GComponent.Background.SOLID));
        topbarLayout.setBackgroundDrawable(GComponent.GetBackground(GComponent.Background.SOLID));
    }

    private void getOptions() {
        //pictogramOption = ;
        //orientationOption = ;
    }

    private void setupOrientationOption() {


    }

    private void setupPictogramOption() {
        final GSeekBar pictogramSlider = (GSeekBar) findViewById(R.id.pictogram_slider);
        final GTextView pictogramSliderText = (GTextView) findViewById(R.id.pictogram_slider_text);
        pictogramSliderText.setText(Integer.toString(pictogramOption) + " piktogrammer");

        pictogramSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                pictogramOption = (int) Math.round(progress/14.285714285714285714285714285714);
                pictogramSliderText.setText(pictogramOption + " piktogrammer");
            }

            @Override
            public void onStartTrackingTouch (SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch (SeekBar seekBar) {

            }

        });
    }

    private void finishActivity(){
        assumeMinimize = false;
        finish();
    }

    @Override
    public void onBackPressed() {
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
