package dk.aau.cs.giraf.zebra;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import dk.aau.cs.giraf.gui.GirafInflatableDialog;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;

/*
 * This is the main activity of the sequence application
 * The activity shows the overview page, of available sequences, for the chosen user
 */
public class MainActivity extends GirafActivity implements SequenceListAdapter.SelectedSequenceAware {

    private static final int numColumns = 5;

    private Profile guardian;
    private Profile selectedChild;
    private boolean isChildSet = false;
    private int childId;
    private boolean markingMode = false;

    private GridView sequenceGrid;
    private SequenceListAdapter sequenceAdapter;
    private Set<Sequence> markedSequences = new HashSet<Sequence>();
    private Helper helper;
    private final String DELETE_SEQUENCES_TAG = "DELETE_SEQUENCES_TAG";

    // Initialize buttons
    GirafInflatableDialog acceptDeleteDialog;
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
        deleteButton.setVisibility(View.GONE);

        // Adding buttons to the action bar
        addGirafButtonToActionBar(changeUserButton, LEFT);
        addGirafButtonToActionBar(addButton, RIGHT);
        addGirafButtonToActionBar(deleteButton, RIGHT);

        helper = new Helper(this);

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
        sequenceGrid.setEmptyView(findViewById(R.id.empty_sequences));
        sequenceGrid.setColumnWidth(getResources().getDisplayMetrics().widthPixels / numColumns);
        //sequenceAdapter = new SequenceListAdapter(MainActivity.this, sequences, MainActivity.this);
        //sequenceGrid.setAdapter(sequenceAdapter);
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
                //equenceListAdapter.SequencePictogramViewPair sequenceViewPair = new SequenceListAdapter.SequencePictogramViewPair(sequence, null);
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
            // Opens a dialog to remove the selected sequences
            @Override
            public void onClick(View v) {
                acceptDeleteDialog = GirafInflatableDialog.newInstance(
                        getApplicationContext().getString(R.string.delete_sequences),
                        getApplicationContext().getString(R.string.delete_this) + " "
                                + getApplicationContext().getString(R.string.marked_sequences),
                        R.layout.dialog_delete);
                acceptDeleteDialog.show(getSupportFragmentManager(), DELETE_SEQUENCES_TAG);
            }
        });
    }

    // Sets up either guardian mode or citizen mode, based on the intents
    private void setupModeFromIntents() {
        //Create helper to fetch data from database and fetches intents (from Launcher or AddEditSequencesActivity)

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
            isChildSet = true;
        }
    }

    // Sets up guardian mode - all functions are enabled
    private void setupGuardianMode() {
        //Clicking a Sequence lifts up the view and leads up to entering AddEditSequencesActivity by calling enterAddEditSequence
        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                final Sequence sequence = sequenceAdapter.getItem(position);

                if (!markingMode) {
                    ((PictogramView) view).liftUp();
                    // Intent is not stated here, as there are two different modes - if guardian then edit mode, else if citizen then view mode
                    enterAddEditSequence(sequence, false);
                } else
                {
                    if (markedSequences.contains(sequence))
                    {
                        unMarkSequence(sequence, view);
                    } else {
                        markSequence(sequence, view);
                    }
                }
            }
        });
        sequenceGrid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                markingMode = true;
                Sequence sequence = sequenceAdapter.getItem(position);
                markSequence(sequence, view);
                deleteButton.setVisibility(View.VISIBLE);
                addButton.setVisibility(View.GONE);
                return true;
            }
        });
    }

    private void markSequence(Sequence sequence, View view) {
        markedSequences.add(sequence);
        view.setBackgroundColor(getResources().getColor(R.color.giraf_page_indicator_active));
    }

    private void unMarkSequence(Sequence c, View view) {
        markedSequences.remove(c);
        view.setBackgroundDrawable(null);
    }

    public void deleteClick(View v) {
        // Button to accept delete of sequences
        acceptDeleteDialog.dismiss();
        // Delete all selected items
        for (Sequence seq : markedSequences) {
            helper.sequenceController.removeSequence(seq);
        }
        sequenceAdapter.notifyDataSetChanged(); // Needs fixing
    }

    public void cancelDeleteClick(View v) {
        // Button to cancel delete of sequences
        acceptDeleteDialog.dismiss();
    }

    // Used to select a child
    private void pickAndSetChild() {
        //Create ProfileSelector to make Guardian select Child
        final GProfileSelector childSelector = new GProfileSelector(this, guardian, null, false);

        //When child is selected, save Child locally and update application accordingly (Title name and Sequences)
        childSelector.setOnListItemClick(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                childId = (int) id;
                setChild();
                isChildSet = true;
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
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
                ((PictogramView) arg1).liftUp();

                //Create Intent with relevant Extras
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("dk.aau.cs.giraf.sequenceviewer", "dk.aau.cs.giraf.sequenceviewer.MainActivity"));
                intent.putExtra("sequenceId", sequenceAdapter.getItem(position).getId());
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
        //Save Child locally and update relevant information for application
        selectedChild = helper.profilesHelper.getProfileById(childId);
        this.setActionBarTitle(getResources().getString(R.string.app_name) + " - " + selectedChild.getName()); // selectedChild.getName() "Child's name code"

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

    @Override
    public boolean isSequenceMarked(Sequence sequence) {
        return markedSequences.contains(sequence);
    }

    // AsyncTask. Used to fetch data from the database in another thread which is NOT the GUI thread
    public class AsyncFetchDatabase extends AsyncTask<Void, Void, List<Sequence>> {

        @Override
        protected List<Sequence> doInBackground(Void... params) {
            return helper.sequenceController.getSequencesAndFramesByProfileIdAndType(selectedChild.getId(), Sequence.SequenceType.SEQUENCE);
        }

        @Override
        protected void onPostExecute(final List<Sequence> result) {
            sequenceAdapter = new SequenceListAdapter(MainActivity.this, result, MainActivity.this);
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

            ((PictogramView) view).placeDown();
        }

        //If a Child is selected at this point, update Sequences for the Child
        if (isChildSet) {
            fetchDatabase.execute();
        }
    }

    @Override
    public void onBackPressed() {
        if (markingMode) {
            markedSequences.clear();
            sequenceAdapter.notifyDataSetChanged();

            deleteButton.setVisibility(View.GONE);
            addButton.setVisibility(View.VISIBLE);
            markingMode = false;
        } else {
            super.onBackPressed();
        }
    }
}
