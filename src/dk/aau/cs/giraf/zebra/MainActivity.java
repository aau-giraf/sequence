package dk.aau.cs.giraf.zebra;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.LayoutInflater;

import dk.aau.cs.giraf.gui.GButton;
import dk.aau.cs.giraf.gui.GButtonSettings;
import dk.aau.cs.giraf.gui.GButtonTrash;
import dk.aau.cs.giraf.gui.GComponent;
import dk.aau.cs.giraf.gui.GDialog;
import dk.aau.cs.giraf.gui.GGridView;
import dk.aau.cs.giraf.gui.GMultiProfileSelector;
import dk.aau.cs.giraf.gui.GProfileSelector;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;

public class MainActivity extends Activity {
    private boolean nestedMode;
    private boolean assumeMinimize = true;
    private boolean childIsSet = false;
    private GridView sequenceGrid;
    private GridView copyGrid;
    private GridView pasteGrid;
    private SequenceListAdapter sequenceAdapter;
    private SequenceListAdapter copyAdapter;
    private SequenceListAdapter pasteAdapter;
    private List<Sequence> sequences = new ArrayList<Sequence>();
    private List<Sequence> tempSequenceList = new ArrayList<Sequence>();
    private List<View> tempViewList = new ArrayList<View>();
    public static Profile selectedChild;
    public static Activity activityToKill;
    private int childId;
    private Helper helper;
    private Profile guardian;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupSequenceGridView();
        setupButtons();
        setupModeFromIntents();
        setColors();
    }

    private void setupSequenceGridView() {
        //Sets the GridView and adapter to display Sequences
        sequenceGrid = (GGridView) findViewById(R.id.sequence_grid);
        sequenceAdapter = new SequenceListAdapter(this, sequences);
        sequenceGrid.setAdapter(sequenceAdapter);
    }

    private void setupButtons() {
        //Creates all buttons in Activity and their listeners. Initially they are invisible
        GButton addButton = (GButton) findViewById(R.id.add_button);
        GButtonTrash deleteButton = (GButtonTrash)findViewById(R.id.delete_button);
        GButton copyButton = (GButton) findViewById(R.id.copy_button);
        GButtonSettings settingsButton = (GButtonSettings)findViewById(R.id.settings_button);
        GButton childSelectButton = (GButton) findViewById(R.id.relog_button);
        GButton exitButton = (GButton) findViewById(R.id.exit_button);

        addButton.setVisibility(View.INVISIBLE);
        deleteButton.setVisibility(View.INVISIBLE);
        copyButton.setVisibility(View.INVISIBLE);
        settingsButton.setVisibility(View.INVISIBLE);
        childSelectButton.setVisibility(View.INVISIBLE);
        exitButton.setVisibility(View.INVISIBLE);

        addButton.setOnClickListener(new OnClickListener() {
            //Enter SequenceActivity when clicking the Add Button
            @Override
            public void onClick(View v) {
                Sequence sequence = new Sequence();
                enterSequence(sequence, true);
            }
        });

        deleteButton.setOnClickListener(new OnClickListener() {
            //Open Delete Dialog when clicking the Delete Button
            @Override
            public void onClick(View v) {
                showDeleteDialog(v);
            }
        });

        copyButton.setOnClickListener(new OnClickListener() {
            //Open Copy Dialog when clicking the Copy Button
            @Override
            public void onClick(View v) {
                showCopyDialog(v);
            }
        });

        settingsButton.setOnClickListener(new OnClickListener() {
            //Open SettingsActivity when clicking the Settings Button
            @Override
            public void onClick(View v) {
                //Launches settingsActivity
                assumeMinimize = false;
                Intent intent = new Intent(getApplication(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        childSelectButton.setOnClickListener(new OnClickListener() {
            //Open Child Selector when pressing the Child Select Button
            @Override
            public void onClick(View v) {
                final GProfileSelector childSelector = new GProfileSelector(v.getContext(), guardian, null, false);
                childSelector.show();

                childSelector.setOnListItemClick(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                        //When child is selected, save Child locally and update application accordingly (Title name and Sequences)
                        childId = (int) id;
                        setChild();
                        childSelector.dismiss();
                    }
                });
            }
        });

        exitButton.setOnClickListener(new OnClickListener() {
        //Exit application when pressing the Exit Button
            @Override
            public void onClick(View v) {
                finishActivity();
            }
        });
    }

    private void setupModeFromIntents() {
        //Create helper to fetch data from database
        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }

        //Fetches intents (from Launcher or SequenceActivity)
        Bundle extras = getIntent().getExtras();

        //Makes the Activity killable from SequenceActivity and (Nested) MainActivity
        if (extras.getBoolean("insertSequence") == false) {
            activityToKill = this;
        }

        //Get GuardianId and ChildId from extras
        int guardianId = extras.getInt("currentGuardianID");
        childId = extras.getInt("currentChildID");

        //Save guardian locally (Fetch from Database by Id)
        guardian = helper.profilesHelper.getProfileById(guardianId);

        //Setup nestedMode if insertSequence extra is present
        if (extras.getBoolean("insertSequence")) {
            nestedMode = true;
            setupNestedMode();
        }
        //Make user pick a child and set up GuardianMode if ChildId is -1 (= Logged in as Guardian)
        else if (childId == -1) {
            pickChild();
            setupGuardianMode();
        }
        //Else setup application for a Child
        else {
            setupChildMode();
            setChild();
            childIsSet = true;
        }
    }

    private void setChild() {
        //Creates helper to fetch data from the Database
        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }
        //Save Child locally and update relevant information for application
        selectedChild = helper.profilesHelper.getProfileById(childId);
        ((TextView) findViewById(R.id.child_name)).setText(selectedChild.getName());
        updateSequences();
    }

    private void setColors() {
        //Sets up application colors using colors from GIRAF_Components
        LinearLayout backgroundLayout = (LinearLayout) findViewById(R.id.parent_container);
        RelativeLayout topbarLayout = (RelativeLayout) findViewById(R.id.sequence_bar);
        backgroundLayout.setBackgroundDrawable(GComponent.GetBackground(GComponent.Background.SOLID));
        topbarLayout.setBackgroundDrawable(GComponent.GetBackground(GComponent.Background.SOLID));
    }

    public void updateSequences() {
        //Updates the list of Sequences by clearing and (re)loading a Childs Sequences from the Database
        tempSequenceList.clear();

        //Creates helper to fetch data from the Database
        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }

        sequences = helper.sequenceController.getSequencesAndFramesByProfileIdAndType(selectedChild.getId(), Sequence.SequenceType.SEQUENCE);
        sequenceAdapter = new SequenceListAdapter(this, sequences);
        sequenceGrid.setAdapter(sequenceAdapter);
    }

    public void showDeleteDialog(View v) {
        deletingSequencesDialog deleteDialog = new deletingSequencesDialog(v.getContext());
        deleteDialog.show();
    }

    public void showCopyDialog(View v) {
        copyingSequencesDialog copyDialog = new copyingSequencesDialog(v.getContext());
        copyDialog.show();
    }

    public class deletingSequencesDialog extends GDialog {

        public deletingSequencesDialog(final Context context) {
            //Dialog where user can sort Sequences to delete
            super(context);
            this.SetView(LayoutInflater.from(this.getContext()).inflate(R.layout.deleting_sequences,null));

            //Set up two GridViews for the Delete operation
            copyAdapter = new SequenceListAdapter(this.getContext(), sequences);
            copyGrid = (GGridView) findViewById(R.id.existing_sequences);
            copyGrid.setAdapter(copyAdapter);
            setCopyGridItemClickListener(copyGrid);

            pasteAdapter = new SequenceListAdapter(this.getContext(), tempSequenceList);
            pasteGrid = (GGridView) findViewById(R.id.empty_sequences);
            pasteGrid.setAdapter(pasteAdapter);
            setPasteGridItemClickListener(pasteGrid);

            //Set up Delete and Back Buttons
            GButton popupDelete = (GButton) findViewById(R.id.popup_accept);
            GButton popupBack = (GButton) findViewById(R.id.popup_back);

            popupDelete.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    //Delete all selected Sequences and update the main Sequence Grid
                    for (Sequence seq : tempSequenceList) {
                        try {
                            helper = new Helper(context);
                        } catch (Exception e) {
                        }
                        helper.sequenceController.removeSequence(seq);
                    }
                    updateSequences();
                    dismiss();
                }
            });

            popupBack.setOnClickListener(new GButton.OnClickListener() {
                //Cancel and close Dialog
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
    }

    public class copyingSequencesDialog extends GDialog {

        public copyingSequencesDialog(Context context) {
            //Dialog where user can pick Sequences to copy to other Children
            super(context);
            this.SetView(LayoutInflater.from(this.getContext()).inflate(R.layout.copying_sequences, null));

            //Creates helper to fetch data from the Database
            try {
                helper = new Helper(context);
            } catch (Exception e) {
            }

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
                    for (Profile p : selectedProfiles){
                        for (Sequence s: tempSequenceList) {
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
        }
    }

    private void setCopyGridItemClickListener(GridView copyGrid) {
        //When clicking a Sequence in the CopyGrid (Left Grid), add to temporary list and the PasteGrid (Right Grid)
        clearTempLists();
        copyGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Check if Sequence is already in the PasteGrid. If so, do nothing
                for (int i = 0; i < tempSequenceList.size(); i++) {
                    if (copyAdapter.getItem(position).getId() == tempSequenceList.get(i).getId()) {
                        return;
                    }
                }

                //Else, add Sequence to PasteGrid and save the View which is needed to reverse the operation
                tempSequenceList.add(copyAdapter.getItem(position));
                tempViewList.add(copyAdapter.getView(position, view, parent));

                //Make the Sequence smaller on the CopyGrid to show it has been selected
                View v = copyAdapter.getView(position, view, parent);
                v.setAlpha(0.2f);
                v.setScaleY(0.85f);
                v.setScaleX(0.85f);

                //Update the PasteGrid
                pasteAdapter.notifyDataSetChanged();
            }
        });

    }

    private void setPasteGridItemClickListener(GridView pasteGrid) {
        //When clicking a Sequence in the PasteGrid (Right Grid), remove and remove selection from CopyGrid ( Left Grid)
        clearTempLists();
        pasteGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //Find Sequence in list
                for (int i = 0; i < tempSequenceList.size(); i++) {
                    if (pasteAdapter.getItem(position).getId() == tempSequenceList.get(i).getId()) {

                        //Remove selection in CopyGrid
                        View v = tempViewList.get(i);
                        v.setAlpha(0.99f);
                        v.setScaleX(0.99f);
                        v.setScaleY(0.99f);

                        //Remove Sequence and view from lists
                        tempSequenceList.remove(i);
                        tempViewList.remove(i);

                        //Update PasteGrid
                        pasteAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }

    private void clearTempLists() {
        tempViewList.clear();
        tempSequenceList.clear();
    }

    private void setupGuardianMode() {
        //Clicking a Sequence lifts up the view and leads up to entering SequenceActivity by calling enterSequence
        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ((PictogramView) view).liftUp();
                Sequence sequence = sequenceAdapter.getItem(position);
                enterSequence(sequence, false);
            }
        });

        //Makes adminstrative buttons visible
        final GButton addButton = (GButton) findViewById(R.id.add_button);
        final GButton deleteButton = (GButton) findViewById(R.id.delete_button);
        final GButton copyButton = (GButton) findViewById(R.id.copy_button);
        final GButton settingsButton = (GButton) findViewById(R.id.settings_button);
        final GButton logoutButton = (GButton) findViewById(R.id.relog_button);
        final GButton exitButton = (GButton) findViewById(R.id.exit_button);

        addButton.setVisibility(View.VISIBLE);
        deleteButton.setVisibility(View.VISIBLE);
        copyButton.setVisibility(View.VISIBLE);
        settingsButton.setVisibility(View.VISIBLE);
        logoutButton.setVisibility(View.VISIBLE);
        exitButton.setVisibility(View.VISIBLE);
    }

    private void setupChildMode() {

        //When clicking a Sequence, lift up the view, load Settings from Child, create Intent for SequenceViewer and launch it
        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                ((PictogramView) arg1).liftUp();
                assumeMinimize = false;

                //Load Preferences
                SharedPreferences settings = getSharedPreferences(SettingsActivity.class.getName() + Integer.toString(MainActivity.selectedChild.getId()), MODE_PRIVATE);
                int pictogramSetting = settings.getInt("pictogramSetting", 5);
                boolean landscapeSetting = settings.getBoolean("landscapeSetting", true);

                //Create Intent with relevant Extras
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("dk.aau.cs.giraf.sequenceviewer", "dk.aau.cs.giraf.sequenceviewer.MainActivity"));
                intent.putExtra("sequenceId", sequenceAdapter.getItem(arg2).getId());
                intent.putExtra("landscapeMode", landscapeSetting);
                intent.putExtra("visiblePictogramCount", pictogramSetting);
                intent.putExtra("callerType", "Zebra");
                startActivityForResult(intent, 2);
            }
        });
    }

    private void setupNestedMode() {
        //On clicking a Sequence, lift up the Sequence, finish Activity and send Id of Sequence as an extra back to SequenceActivity
        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ((PictogramView) view).liftUp();
                Intent intent = new Intent();
                intent.putExtra("nestedSequenceId",sequenceAdapter.getItem(position).getId());
                setResult(RESULT_OK, intent);
                finishActivity();
            }
        });
    }

    private void pickChild(){
        //Create helper to fetch data from database
        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }

        //Create ProfileSelector to make Guardian select Child
        final GProfileSelector childSelector = new GProfileSelector(this, guardian, null, false);

        //Make Guardian unable to skip past picking a Child (Guardian can not click beside window to close ProfileSelector)
        //TODO: Pressing the back key can close the ProfileSelector. Find a fix.
        try{childSelector.backgroundCancelsDialog(false);}
        catch (Exception e) {}


        //When child is selected, save Child locally and update application accordingly (Title name and Sequences)
        childSelector.setOnListItemClick(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                childId = (int) id;
                setChild();
                childIsSet = true;
                childSelector.dismiss();
            }
        });
        childSelector.show();
    }

    private void enterSequence(Sequence sequence, boolean isNew) {
        //Sets up relevant intents and starts SequenceActivity
        assumeMinimize = false;
        Intent intent = new Intent(getApplication(), SequenceActivity.class);
        intent.putExtra("profileId", selectedChild.getId());
        intent.putExtra("guardianId", guardian.getId());
        intent.putExtra("editMode", true);
        intent.putExtra("new", isNew);
        intent.putExtra("sequenceId", sequence.getId());

        startActivity(intent);
    }

    private void finishActivity() {
        //Closes Activity properly by setting assumeMinimize to false. See onStop for explanation on assumeMinimize
        assumeMinimize = false;
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Removes highlighting from Sequences that might have been lifted up when selected earlier
        for (int i = 0; i < sequenceGrid.getChildCount(); i++) {
            View view = sequenceGrid.getChildAt(i);

            if (view instanceof PictogramView) {
                ((PictogramView) view).placeDown();
            }
        }
        //If a Child is selected at this point, update Sequences for the Child
        if (childIsSet) {
            updateSequences();
        }
    }

    @Override
    protected void onStop() {
        /*assumeMinimize makes it possible to kill the entire application if ever minimized.
        onStop is also called when entering other Activities, which is why the assumeMinimize check is needed
        assumeMinimize is set to false every time an Activity is entered and then reset to true here so application is not killed*/
        if (assumeMinimize) {
            //If in NestedMode, kill all open Activities. If not Nested, only this Activity needs to be killed
            if (nestedMode) {
                SequenceActivity.activityToKill.finish();
                MainActivity.activityToKill.finish();
            }
            finishActivity();
        } else {
            //If assumeMinimize was false, reset it to true
            assumeMinimize = true;
        }
        super.onStop();
    }
}