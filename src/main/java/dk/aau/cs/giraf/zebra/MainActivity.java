package dk.aau.cs.giraf.zebra;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.view.LayoutInflater;

import dk.aau.cs.giraf.activity.GirafActivity;
import dk.aau.cs.giraf.gui.GButton;
import dk.aau.cs.giraf.gui.GComponent;
import dk.aau.cs.giraf.gui.GDialog;
import dk.aau.cs.giraf.gui.GGridView;
import dk.aau.cs.giraf.gui.GProfileSelector;
import dk.aau.cs.giraf.gui.GirafButton;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;

public class MainActivity extends GirafActivity {

    private Profile guardian;
    private Profile selectedChild;

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

    public static Activity activityToKill;

    private int childId;

    private Helper helper;

    // Initialize buttons
    private GirafButton changeUserButton;
    private GirafButton addButton;
    private GirafButton copyButton;
    private GirafButton deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Creating buttons
        changeUserButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_change_user));
        addButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_add));
        copyButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_copy));
        deleteButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_delete));

        // Adding buttons
        addGirafButtonToActionBar(changeUserButton, LEFT);
        addGirafButtonToActionBar(addButton, RIGHT);
        addGirafButtonToActionBar(copyButton, RIGHT);
        addGirafButtonToActionBar(deleteButton, RIGHT);

        // Setup additional things
        setupSequenceGridView();
        setupButtons();
        setupModeFromIntents();
        //setColors();
    }

    private void setupSequenceGridView() {
        //Sets the GridView and adapter to display Sequences
        sequenceGrid = (GGridView) findViewById(R.id.sequence_grid);
        sequenceAdapter = new SequenceListAdapter(this, sequences);
        sequenceGrid.setAdapter(sequenceAdapter);
    }

    private void setupButtons() {
        //Creates all buttons in Activity and their listeners. Initially they are invisible (Defined in XML)

        addButton.setOnClickListener(new OnClickListener() {
            //Enter AddSequencesActivity when clicking the Add Button
            @Override
            public void onClick(View v) {
                Sequence sequence = new Sequence();
                enterSequence(sequence, true);
            }
        });

        deleteButton.setOnClickListener(new OnClickListener() {
            //Open DeleteSequencesActivity when clicking the delete Button
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplication(), DeleteSequencesActivity.class);
                intent.putExtra("childId", selectedChild.getId());
                intent.putExtra("guardianId", guardian.getId());
                startActivity(intent);
            }
        });

        copyButton.setOnClickListener(new OnClickListener() {
            //Open CopySequencesActivity when clicking the Copy Button
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplication(), CopySequencesActivity.class);
                intent.putExtra("childId", selectedChild.getId());
                intent.putExtra("guardianId", guardian.getId());
                startActivity(intent);
            }
        });

        // Click event for change user button
        changeUserButton.setOnClickListener(new OnClickListener() {
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
    }

    private void setupModeFromIntents() {
        //Create helper to fetch data from database
        helper = new Helper(this);

        //Fetches intents (from Launcher or AddSequencesActivity)
        Bundle extras = getIntent().getExtras();

        //Makes the Activity killable from AddSequencesActivity and (Nested) MainActivity
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
            setChild();
        }
        //Make user pick a child and set up GuardianMode if ChildId is -1 (= Logged in as Guardian)
        else if (childId == -1) {
            pickAndSetChild();
            setupGuardianMode();
        }
        //Else setup application for a Child
        else {
            setupChildMode();
            setChild();
            childIsSet = true;
        }
    }

    //private void setColors() {
        //Sets up application colors using colors from GIRAF_Components
        //LinearLayout backgroundLayout = (LinearLayout) findViewById(R.id.parent_container);
        //backgroundLayout.setBackgroundDrawable(GComponent.GetBackground(GComponent.Background.SOLID));
    //}

    private void setChild() {
        //Creates helper to fetch data from the Database
        helper = new Helper(this);
        
        //Save Child locally and update relevant information for application
        selectedChild = helper.profilesHelper.getProfileById(childId);
        this.setActionBarTitle(getResources().getString(R.string.app_name)); // selectedChild.getName() "Child's name code"
        updateSequences();
    }

    private void updateSequences() {
        //Updates the list of Sequences by clearing and (re)loading a Childs Sequences from the Database
        tempSequenceList.clear();

        //Creates helper to fetch data from the Database
        helper = new Helper(this);

        sequences = helper.sequenceController.getSequencesAndFramesByProfileIdAndType(selectedChild.getId(), Sequence.SequenceType.SEQUENCE);
        sequenceAdapter = new SequenceListAdapter(this, sequences);
        sequenceGrid.setAdapter(sequenceAdapter);
    }

    //private void showDeleteDialog(View v) {
    //    deletingSequencesDialog deleteDialog = new deletingSequencesDialog(v.getContext());
    //    deleteDialog.show();
    //}

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
        //Clicking a Sequence lifts up the view and leads up to entering AddSequencesActivity by calling enterSequence
        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ((PictogramView) view).liftUp();
                Sequence sequence = sequenceAdapter.getItem(position);
                enterSequence(sequence, false);
            }
        });
    }

    private void setupChildMode() {

        //When clicking a Sequence, lift up the view, create Intent for SequenceViewer and launch it
        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                ((PictogramView) arg1).liftUp();
                assumeMinimize = false;

                //Create Intent with relevant Extras
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("dk.aau.cs.giraf.sequenceviewer", "dk.aau.cs.giraf.sequenceviewer.MainActivity"));
                intent.putExtra("sequenceId", sequenceAdapter.getItem(arg2).getId());
                intent.putExtra("callerType", "Zebra");
                startActivityForResult(intent, 2);
            }
        });

        addButton.setVisibility(View.INVISIBLE);
        deleteButton.setVisibility(View.INVISIBLE);
        copyButton.setVisibility(View.INVISIBLE);
        changeUserButton.setVisibility(View.INVISIBLE);
    }

    private void setupNestedMode() {
        //On clicking a Sequence, lift up the Sequence, finish Activity and send Id of Sequence as an extra back to AddSequencesActivity
        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ((PictogramView) view).liftUp();
                Intent intent = new Intent();
                intent.putExtra("nestedSequenceId", sequenceAdapter.getItem(position).getId());
                setResult(RESULT_OK, intent);
                finishActivity();
            }
        });
    }

    private void pickAndSetChild(){
        //Create helper to fetch data from database
        helper = new Helper(this);

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

    //Sets up relevant intents and starts AddSequencesActivity
    private void enterSequence(Sequence sequence, boolean isNew) {
        assumeMinimize = false;
        Intent intent = new Intent(getApplication(), AddSequencesActivity.class);
        intent.putExtra("childId", selectedChild.getId());
        intent.putExtra("guardianId", guardian.getId());
        intent.putExtra("editMode", true);
        intent.putExtra("isNew", isNew);
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
                AddSequencesActivity.activityToKill.finish();
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