package dk.aau.cs.giraf.zebra;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import dk.aau.cs.giraf.activity.GirafActivity;
import dk.aau.cs.giraf.gui.GButton;
import dk.aau.cs.giraf.gui.GDialog;
import dk.aau.cs.giraf.gui.GGridView;
import dk.aau.cs.giraf.gui.GMultiProfileSelector;
import dk.aau.cs.giraf.gui.GirafButton;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;



public class CopySequencesActivity extends GirafActivity {
    // Initialize buttons
    private GirafButton acceptButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_copy_sequences);

        this.setActionBarTitle(getResources().getString(R.string.copy_sequences));

        // Creating buttons
        acceptButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_accept));

        // Adding buttons
        addGirafButtonToActionBar(acceptButton, RIGHT);

        //
        setupButtons();
    }

    private void setupButtons(){
        //Creates all buttons in Activity and their listeners.

        acceptButton.setOnClickListener(new View.OnClickListener() {
            //Show the MultiProfileSelector when clicking the Copy Button
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }







    /*


    OLD COPY MATERIAL



     */




    /*public copyingSequencesDialog(Context context) {
        //Dialog where user can pick Sequences to copy to other Children
        super(context);
        this.SetView(LayoutInflater.from(this.getContext()).inflate(R.layout.copying_sequences, null));

        //Creates helper to fetch data from the Database
        helper = new Helper(context);

        //Set up two GridViews for the Copy operation
        copyAdapter = new SequenceListAdapter(this.getContext(), sequences);
        copyGrid = (GGridView) findViewById(R.id.existing_sequences);
        copyGrid.setAdapter(copyAdapter);
        setCopyGridItemClickListener(copyGrid);

        pasteAdapter = new SequenceListAdapter(this.getContext(), tempSequenceList);
        pasteGrid = (GGridView) findViewById(R.id.empty_sequences);
        pasteGrid.setAdapter(pasteAdapter);
        setPasteGridItemClickListener(pasteGrid);

        //Set up Buttons
        GButton popupCopy = (GButton) findViewById(R.id.popup_copy_accept);
        GButton popupBack = (GButton) findViewById(R.id.popup_copy_back);

        //Create a MultiProfileSelector given a fresh list of Profiles to store chosen profiles in
        List<Profile> children = new ArrayList<Profile>();

        final GMultiProfileSelector childSelector = new GMultiProfileSelector(context, helper.profilesHelper.getChildrenByGuardian(guardian), children);
        childSelector.setMyOnCloseListener(new GMultiProfileSelector.onCloseListener() {
            //When closing the MultiProileSelector, copy all chosen Sequences to all chosen Children
            @Override
            public void onClose(List<Profile> selectedProfiles) {
                for (Profile p : selectedProfiles) {
                    for (Sequence s : tempSequenceList) {
                        helper.sequenceController.copySequenceAndFrames(s, p);
                    }
                }
            }
        });

        popupCopy.setOnClickListener(new GButton.OnClickListener() {
            //Show the MultiProfileSelector when clicking the Copy Button
            @Override
            public void onClick(View v) {
                childSelector.show();
            }
        });

        popupBack.setOnClickListener(new GButton.OnClickListener() {
            //Cancel and close when clicking the Back Button
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    } */
}
