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
import dk.aau.cs.giraf.gui.GProfileSelector;
import dk.aau.cs.giraf.gui.GirafButton;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;

/*
 * This is the main activity of the sequence application
 * The activity shows the overview page, of available sequences, for the chosen user
 */
public class MainActivity extends GirafActivity {

    private Profile guardian;
    private Profile selectedChild;
    private boolean childIsSet = false;
    //private int guardianId;
    private int childId;

    private GridView sequenceGrid;

    private SequenceListAdapter sequenceAdapter;
    private List<Sequence> sequences = new ArrayList<Sequence>();
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

        // Creating buttons for the action bar
        changeUserButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_change_user));
        addButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_add));
        copyButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_copy));
        deleteButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_delete));

        // Adding buttons to the action bar
        addGirafButtonToActionBar(changeUserButton, LEFT);
        addGirafButtonToActionBar(addButton, RIGHT);
        addGirafButtonToActionBar(deleteButton, RIGHT);

        // Setup the sequence grid view used to display sequences
        setupSequenceGridView();
        // Setup the buttons (onClickListener)
        setupButtons();
        // Setup mode, this is either citizen mode or guardian mode
        setupModeFromIntents();
    }

    //Sets the GridView and adapter to display Sequences
    private void setupSequenceGridView() {
        sequenceGrid = (GridView) findViewById(R.id.sequence_grid);
        sequenceAdapter = new SequenceListAdapter(this, sequences);
        sequenceGrid.setAdapter(sequenceAdapter);
    }

    // Creates all buttons for the Activity and their listeners.
    private void setupButtons() {

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

        addButton.setOnClickListener(new OnClickListener() {
            //Open AddEditSequencesActivity when clicking the Add Button
            @Override
            public void onClick(View v) {
                Sequence sequence = new Sequence();
                enterAddEditSequence(sequence, true);
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

        deleteButton.setOnClickListener(new OnClickListener() {
            //Open DeleteSequencesActivity when clicking the delete Button
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplication(), DeleteSequencesActivity.class);
                intent.putExtra("childId", selectedChild.getId());
                startActivity(intent);
            }
        });
    }

    // Sets up either guardian mode or citizen mode, based on the intents
    private void setupModeFromIntents() {
        //Create helper to fetch data from database and fetches intents (from Launcher or AddEditSequencesActivity)
        helper = new Helper(this);
        Bundle extras = getIntent().getExtras();
        int guardianId;

        //Get GuardianId and ChildId from extras
        guardianId = extras.getInt("currentGuardianID");
        childId = extras.getInt("currentChildID");

        //Save guardian locally (Fetch from Database by Id)
        guardian = helper.profilesHelper.getProfileById(guardianId);

        //Make user pick a child and set up GuardianMode if ChildId is -1 (= Logged in as Guardian)
        if (childId == -1) {
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

    // Sets up guardian mode - all functions are enabled
    private void setupGuardianMode() {
        //Clicking a Sequence lifts up the view and leads up to entering AddEditSequencesActivity by calling enterAddEditSequence
        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ((PictogramView) view).liftUp();
                Sequence sequence = sequenceAdapter.getItem(position);
                // Intent is not stated here, as there are two different modes - if guardian then edit mode, else if citizen then view mode
                enterAddEditSequence(sequence, false);
            }
        });
    }

    // Used to select a child
    private void pickAndSetChild() {
        //Create helper to fetch data from database
        helper = new Helper(this);

        //Create ProfileSelector to make Guardian select Child
        final GProfileSelector childSelector = new GProfileSelector(this, guardian, null, false);

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

    // Sets up child mode - only possible to view sequences
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
                intent.putExtra("landscapeMode", false);
                intent.putExtra("visiblePictogramCount", sequenceAdapter.getCount());
                startActivityForResult(intent, 2);
            }
        });

        addButton.setVisibility(View.INVISIBLE);
        deleteButton.setVisibility(View.INVISIBLE);
        copyButton.setVisibility(View.INVISIBLE);
        changeUserButton.setVisibility(View.INVISIBLE);
    }

    // Collects data about the child - and set actionbar title
    private synchronized void setChild() {
        //Creates helper to fetch data from the Database
        helper = new Helper(this);

        //Save Child locally and update relevant information for application
        selectedChild = helper.profilesHelper.getProfileById(childId);
        this.setActionBarTitle(getResources().getString(R.string.app_name)); // selectedChild.getName() "Child's name code"

        // AsyncTask thread
        AsyncFetchDatabase fetchDatabaseSetChild = new AsyncFetchDatabase();
        fetchDatabaseSetChild.execute();
    }

    //Sets up relevant intents and starts AddEditSequencesActivity
    private void enterAddEditSequence(Sequence sequence, boolean isNew) {

        Intent intent = new Intent(getApplication(), AddEditSequencesActivity.class);
        intent.putExtra("childId", selectedChild.getId());
        intent.putExtra("guardianId", guardian.getId());
        intent.putExtra("editMode", true);
        intent.putExtra("isNew", isNew);
        intent.putExtra("sequenceId", sequence.getId());
        startActivity(intent);
    }

    // AsyncTask. Used to fetch data from the database in another thread which is NOT the GUI thread
    public class AsyncFetchDatabase extends AsyncTask<Void, Void, Void> {

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
        // Create the AsyncTask thread used to fetch database content
        AsyncFetchDatabase fetchDatabase = new AsyncFetchDatabase();

        // Removes highlighting from Sequences that might have been lifted up when selected before entering the sequence
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
    public void onBackPressed() {

    }
}