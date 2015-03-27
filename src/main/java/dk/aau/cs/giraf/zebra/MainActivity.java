package dk.aau.cs.giraf.zebra;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.GridView;

import dk.aau.cs.giraf.activity.GirafActivity;
import dk.aau.cs.giraf.gui.GGridView;
import dk.aau.cs.giraf.gui.GProfileSelector;
import dk.aau.cs.giraf.gui.GirafButton;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;

public class MainActivity extends GirafActivity {

    private Profile guardian;
    private Profile selectedChild;

    private boolean childIsSet = false;

    private GridView sequenceGrid;

    private SequenceListAdapter sequenceAdapter;

    private List<Sequence> sequences = new ArrayList<Sequence>();
    private List<Sequence> tempSequenceList = new ArrayList<Sequence>();

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
    }

    private void setupSequenceGridView() {
        //Sets the GridView and adapter to display Sequences
        sequenceGrid = (GGridView) findViewById(R.id.sequence_grid);
        sequenceAdapter = new SequenceListAdapter(this, sequences);
        sequenceGrid.setAdapter(sequenceAdapter);
    }

    private void setupButtons() {
        //Creates all buttons for the Activity and their listeners.

        addButton.setOnClickListener(new OnClickListener() {
            //Enter AddEditSequencesActivity when clicking the Add Button
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

        //Fetches intents (from Launcher or AddEditSequencesActivity)
        Bundle extras = getIntent().getExtras();

        //Get GuardianId and ChildId from extras
        int guardianId = extras.getInt("currentGuardianID");
        childId = extras.getInt("currentChildID");

        //Save guardian locally (Fetch from Database by Id)
        guardian = helper.profilesHelper.getProfileById(guardianId);

        //Setup nestedMode if insertSequence extra is present
        if (extras.getBoolean("insertSequence")) {
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

    private synchronized void setChild() {

        //Creates helper to fetch data from the Database
        helper = new Helper(this);
        
        //Save Child locally and update relevant information for application
        selectedChild = helper.profilesHelper.getProfileById(childId);
        this.setActionBarTitle(getResources().getString(R.string.app_name)); // selectedChild.getName() "Child's name code"

        // AsyncTask thread
        AsyncFetchDatabase runner = new AsyncFetchDatabase();
        runner.execute();
    }

    private void setupGuardianMode() {
        //Clicking a Sequence lifts up the view and leads up to entering AddEditSequencesActivity by calling enterSequence
        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ((PictogramView) view).liftUp();
                Sequence sequence = sequenceAdapter.getItem(position);
                // Intent is not stated here, as there are two different modes - if guardian then edit mode, else if citizen then view mode
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

                //Create Intent with relevant Extras
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("dk.aau.cs.giraf.sequenceviewer", "dk.aau.cs.giraf.sequenceviewer.MainActivity"));
                intent.putExtra("sequenceId", sequenceAdapter.getItem(arg2).getId());
                intent.putExtra("callerType", "Zebra");
                intent.putExtra("visiblePictogramCount", 1);
                startActivityForResult(intent, 2);
            }
        });

        addButton.setVisibility(View.INVISIBLE);
        deleteButton.setVisibility(View.INVISIBLE);
        copyButton.setVisibility(View.INVISIBLE);
        changeUserButton.setVisibility(View.INVISIBLE);
    }

    private void setupNestedMode() {
        //On clicking a Sequence, lift up the Sequence, finish Activity and send Id of Sequence as an extra back to AddEditSequencesActivity
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

    //Sets up relevant intents and starts AddEditSequencesActivity
    private void enterSequence(Sequence sequence, boolean isNew) {
        Intent intent = new Intent(getApplication(), AddEditSequencesActivity.class);
        intent.putExtra("childId", selectedChild.getId());
        intent.putExtra("guardianId", guardian.getId());
        intent.putExtra("editMode", true);
        intent.putExtra("isNew", isNew);
        intent.putExtra("sequenceId", sequence.getId());
        startActivity(intent);
    }

    private void finishActivity() {
        //Closes Activity properly by setting assumeMinimize to false. See onStop for explanation on assumeMinimize
        finish();
    }

    // AsyncTask. Used to fetch data from the database NOT in the GUI thread
    public class AsyncFetchDatabase extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            tempSequenceList.clear();
        }

        @Override
        protected Void doInBackground(Void... params) {
            helper = new Helper(MainActivity.this);
            sequences = helper.sequenceController.getSequencesAndFramesByProfileIdAndType(selectedChild.getId(), Sequence.SequenceType.SEQUENCE);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            sequenceAdapter = new SequenceListAdapter(MainActivity.this, sequences);
            sequenceGrid.setAdapter(sequenceAdapter);
        }
    }

    @Override
    protected synchronized void onResume() {
        super.onResume();
        // AsyncTask thread
        AsyncFetchDatabase fetchDatabase = new AsyncFetchDatabase();

        // Removes highlighting from Sequences that might have been lifted up when selected earlier
        for (int i = 0; i < sequenceGrid.getChildCount(); i++) {
            View view = sequenceGrid.getChildAt(i);

            if (view instanceof PictogramView) {
                ((PictogramView) view).placeDown();
            }
        }
        //If a Child is selected at this point, update Sequences for the Child
        if (childIsSet) {
            fetchDatabase.execute();
        }
    }

    @Override
    protected void onStop() {
        /*assumeMinimize makes it possible to kill the entire application if ever minimized.
        onStop is also called when entering other Activities, which is why the assumeMinimize check is needed
        assumeMinimize is set to false every time an Activity is entered and then reset to true here so application is not killed*/
        /*if (assumeMinimize) {
            //If in NestedMode, kill all open Activities. If not Nested, only this Activity needs to be killed
            if (nestedMode) {
                AddEditSequencesActivity.activityToKill.finish();
                MainActivity.activityToKill.finish();
            }
            finishActivity();
        } else {
            //If assumeMinimize was false, reset it to true
            assumeMinimize = true;
        }*/
        super.onStop();
    }
}