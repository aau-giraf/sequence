package dk.aau.cs.giraf.sequence;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import dk.aau.cs.giraf.activity.GirafActivity;
import dk.aau.cs.giraf.gui.GirafButton;
import dk.aau.cs.giraf.gui.GirafInflatableDialog;
import dk.aau.cs.giraf.gui.GirafNotifyDialog;
import dk.aau.cs.giraf.dblib.Helper;
import dk.aau.cs.giraf.dblib.models.Profile;
import dk.aau.cs.giraf.dblib.models.Sequence;
import dk.aau.cs.giraf.gui.GirafProfileSelectorDialog;
import dk.aau.cs.giraf.sequenceviewer.SequenceActivity;
import com.google.analytics.tracking.android.EasyTracker;

/*
 * This is the main activity of the sequence application
 * The activity shows the overview page, of available sequences, for the chosen user
 */
public class MainActivity extends GirafActivity implements SequenceListAdapter.SelectedSequenceAware, GirafNotifyDialog.Notification, GirafProfileSelectorDialog.OnSingleProfileSelectedListener {

    private static final int numColumns = 5;

    private Profile guardian;
    private Profile selectedChild;
    private boolean isChildSet = false;
    private long childId;
    private boolean markingMode = false;
    private long guardianId;

    private GridView sequenceGrid;
    private SequenceListAdapter sequenceAdapter;
    private final Set<Sequence> markedSequences = new HashSet<Sequence>();
    private Helper helper;
    private TextView noSequencesWarning;
    private TextView noSequencesHint;
    private static final String DELETE_SEQUENCES_TAG = "DELETE_SEQUENCES_TAG";
    private static final int CHANGE_USER_DIALOG = 1234;
    private static final int NO_PROFILE_ERROR = 1770;
    private static final int NO_SEQUENCE_MARKED_ERROR = 1771;

    // Initialize buttons
    private GirafInflatableDialog acceptDeleteDialog;
    private GirafButton changeUserButton;
    private GirafButton addButton;
    private GirafButton deleteButton;

    // Initialize dialogs
    private GirafProfileSelectorDialog profileSelectorDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Creating buttons for the action bar
        changeUserButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_change_user));
        addButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_add));
        deleteButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_delete));
        deleteButton.setVisibility(View.GONE);

        // Adding buttons to the action bar
        addGirafButtonToActionBar(changeUserButton, LEFT);
        addGirafButtonToActionBar(addButton, RIGHT);
        addGirafButtonToActionBar(deleteButton, RIGHT);

        helper = new Helper(this);

        // Setup XML components
        setupXmlComponents();

        // Setup the buttons (onClickListener)
        setupButtons();

        // Setup mode, this is either citizen mode or guardian mode
        setupModeFromIntents();

        // Starts the tracker of Google Analytics
        EasyTracker.getInstance(this).activityStart(this);
    }

    /**
     * Setup the requires XML components and initializes the sequenceGrid.
     * <p>
     *     This connects the editable XML elements
     *     with variables in the .java file.
     * </p>
     */
    private void setupXmlComponents() {
        noSequencesWarning = (TextView) findViewById(R.id.noExistingSequencesWarning);
        noSequencesHint = (TextView) findViewById(R.id.noExistingSequencesHint);

        // Setup the sequence grid view used to display sequences
        sequenceGrid = (GridView) findViewById(R.id.sequence_grid);
        sequenceGrid.setColumnWidth(getResources().getDisplayMetrics().widthPixels / numColumns);

        checkExistingSequences(false);
    }

    /**
     * Setup the buttons in the action-bar at the top of the app.
     * <p>
     *     This consists of the:
     *     Change user button,
     *     Add sequence button,
     *     Delete sequence button.
     * </p>
     */
    private void setupButtons() {
        // Creates all buttons for the Activity and their listeners.

        changeUserButton.setOnClickListener(new OnClickListener() {
            //Open Child Selector when pressing the Child Select Button
            @Override
            public void onClick(View v) {
                pickAndSetChild();
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

        deleteButton.setOnClickListener(new OnClickListener() {
            // Opens a dialog to remove the selected sequences
            @Override
            public void onClick(View v) {
                if (markedSequences.size() == 0) {
                    createAndShowErrorDialogNoSequencesMarked();
                } else if (markedSequences.size() <= 1) {
                    acceptDeleteDialog = GirafInflatableDialog.newInstance(
                            getApplicationContext().getString(R.string.delete_sequences),
                            getApplicationContext().getString(R.string.delete_this),
                            R.layout.dialog_delete);
                    acceptDeleteDialog.show(getSupportFragmentManager(), DELETE_SEQUENCES_TAG);
                } else {
                    acceptDeleteDialog = GirafInflatableDialog.newInstance(
                            getApplicationContext().getString(R.string.delete_sequences),
                            getApplicationContext().getString(R.string.marked_sequences_part1) + " " + markedSequences.size() + " " +
                                    getApplicationContext().getString(R.string.marked_sequences_part2),
                            R.layout.dialog_delete);
                    acceptDeleteDialog.show(getSupportFragmentManager(), DELETE_SEQUENCES_TAG);
                }
            }
        });
    }

    /**
     * Sets up either guardian mode or citizen mode, based on the intents.
     * <p>
     *     Create helper to fetch data from database
     *     and fetches intents (from Launcher or
     *     AddEditSequencesActivity).
     * </p>
     */
    private void setupModeFromIntents() {
        // Get GuardianId and ChildId from extras
        Bundle extras = getIntent().getExtras();
        guardianId = extras.getLong("currentGuardianID");
        childId = extras.getLong("currentChildID");

        // Save guardian locally (Fetch from Database by Id)
        guardian = helper.profilesHelper.getById(guardianId);

        // Make user pick a child and set up GuardianMode if ChildId is -1 (= Logged in as Guardian)
        if (childId == -1) {
            pickAndSetChild();
            setupGuardianMode();
        }
        // Else setup application for a Child
        else {
            setupChildMode();
            setChild();
            isChildSet = true;
        }
    }

    /**
     * Sets up guardian mode
     * <p>
     *     Sets up the different kinds of click
     *     commands available to a guardian
     *     profile.
     * </p>
     */
    private void setupGuardianMode() {
        //Clicking a Sequence lifts up the view and leads up to entering AddEditSequencesActivity by calling enterAddEditSequence
        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                final Sequence sequence = sequenceAdapter.getItem(position);

                // In case of markingMode, mark sequences upon normal clicking
                if (!markingMode) {
                    ((PictogramView) view).liftUp();
                    // Intent is not stated here, as there are two different modes - if guardian then edit mode, else if citizen then view mode
                    enterAddEditSequence(sequence, false);
                } else {
                    if (markedSequences.contains(sequence)) {
                        unMarkSequence(sequence, view);
                    } else {
                        markSequence(sequence, view);
                    }
                }
            }
        });
        // Long click listener. Used to enter marking mode.
        sequenceGrid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                markingMode = true;
                Sequence sequence = sequenceAdapter.getItem(position);
                markSequence(sequence, view);
                deleteButton.setVisibility(View.VISIBLE);
                addButton.setVisibility(View.GONE);
                changeUserButton.setVisibility(View.GONE);
                setActionBarTitle(getResources().getString(R.string.delete_sequences) + " - " + selectedChild.getName());
                return true;
            }
        });
    }

    /**
     * Marks the clicked sequence by changing it's bg color and adding it to the list.
     * @param sequence The sequence being added.
     * @param view The view of the sequence.
     */
    private void markSequence(Sequence sequence, View view) {
        markedSequences.add(sequence);
        view.setBackgroundColor(getResources().getColor(R.color.giraf_page_indicator_active));
    }

    /**
     * Unmarks the clicked sequence, previously marked by markSequence().
     * @param sequence The sequence being removed.
     * @param view The view of the sequence.
     */
    private void unMarkSequence(Sequence sequence, View view) {
        markedSequences.remove(sequence);
        view.setBackgroundDrawable(null);
    }

    /**
     * Confirm button on the delete sequence confirmation dialog box.
     * @param v The view belonging to the button.
     */
    public void onConfirmDeleteClick(View v) {
        acceptDeleteDialog.dismiss();
        // Delete all selected items
        for (Sequence seq : markedSequences) {
            helper.sequenceController.remove(seq);
        }

        // Reload the adapter - do this in background
        AsyncFetchDatabase fetchDatabaseAfterDelete = new AsyncFetchDatabase();
        fetchDatabaseAfterDelete.execute();
        sequenceGrid.invalidateViews();
        sequenceGrid.setAdapter(sequenceAdapter);
        onBackPressed();
    }

    /**
     * Delete button on the delete sequence confirmation dialog box.
     * @param v The view belonging to the button.
     */
    public void onCancelDeleteClick(View v) {
        // Button to cancel delete of sequences
        acceptDeleteDialog.dismiss();
    }

    /**
     * Checks if there are any sequences to be shown. Otherwise show a help message.
     * @param showHint Boolean value, determining whether to hide or show the hint.
     */
    void checkExistingSequences(boolean showHint) {
        if (showHint)
        {
            noSequencesWarning.setVisibility(View.VISIBLE);
            noSequencesHint.setVisibility(View.VISIBLE);
        }
        else
        {
            noSequencesWarning.setVisibility(View.GONE);
            noSequencesHint.setVisibility(View.GONE);
        }
    }

    /**
     * Pick a child as a guardian and set it to the current child being configured.
     */
    private void pickAndSetChild() {
        //Create ProfileSelector to make Guardian select Child
        profileSelectorDialog = GirafProfileSelectorDialog.newInstance(this, guardianId, false, false, getString(R.string.change_user_dialog_description), CHANGE_USER_DIALOG);
        profileSelectorDialog.show(getSupportFragmentManager(), "" + CHANGE_USER_DIALOG);
    }

    /**
     *  Sets up child mode - only possible to view sequences
     */
    private void setupChildMode() {
        //When clicking a Sequence, lift up the view, create Intent for SequenceViewer and launch it
        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
                ((PictogramView) arg1).liftUp();

                //Create Intent with relevant Extras
                Intent intent = new Intent(getApplicationContext(), SequenceActivity.class);
                intent.putExtra("sequenceId", sequenceAdapter.getItem(position).getId());
                intent.putExtra("callerType", "sequence");
                startActivityForResult(intent, 2);
            }
        });

        addButton.setVisibility(View.INVISIBLE);
        deleteButton.setVisibility(View.INVISIBLE);
        changeUserButton.setVisibility(View.INVISIBLE);
    }

    /**
     * Collects data about the child - and set actionbar title
     */
    private synchronized void setChild() {
        //Save Child locally and update relevant information for application
        selectedChild = helper.profilesHelper.getById(childId);
        this.setActionBarTitle(getResources().getString(R.string.app_name) + " - " + selectedChild.getName()); // selectedChild.getName() "Child's name code"

        // AsyncTask thread
        AsyncFetchDatabase fetchDatabaseSetChild = new AsyncFetchDatabase();
        fetchDatabaseSetChild.execute();
    }

    /**
     * Sets up intents and starts AddEditSequencesActivity if a profile is selected.
     * @param sequence Sequence being edited.
     * @param isNew Whether or not it's a new sequence.
     */
    private void enterAddEditSequence(Sequence sequence, boolean isNew) {

        helper = new Helper(this);

        selectedChild = helper.profilesHelper.getById(childId);

        // If no profile has been selected, show an error dialog and the profile selector, else start AddEditSequencesActivity
        if (selectedChild == null) {
            createAndShowErrorDialogNoProfileSelected();
        } else {
            Intent intent = new Intent(getApplication(), AddEditSequencesActivity.class);
            intent.putExtra("childId", selectedChild.getId());
            intent.putExtra("guardianId", guardian.getId());
            intent.putExtra("editMode", true);
            intent.putExtra("isNew", isNew);
            intent.putExtra("sequenceId", sequence.getId());
            startActivity(intent);
        }
    }

    /**
     * Checks if the sequence is marked.
     * @param sequence The sequence being checked.
     * @return Boolean value based on the marked state of the sequence.
     */
    @Override
    public boolean isSequenceMarked(Sequence sequence) {
        return markedSequences.contains(sequence);
    }

    /**
     * Receives an ID and runs the pickAndSetChild() if correct ID.
     * @param id The ID being checked.
     */
    @Override
    public void noticeDialog(int id) {
        switch (id) {
            case NO_PROFILE_ERROR:
                pickAndSetChild();
                break;

            default:
                break;
        }
    }

    /**
     * Based on ID received, set the guardian to configure a new child.
     * @param id The ID being checked.
     * @param profile The profile of the new child.
     */
    @Override
    public void onProfileSelected(int id, Profile profile) {
        if (id == CHANGE_USER_DIALOG) {
            childId = profile.getId();
            setChild();
            isChildSet = true;
            profileSelectorDialog.dismiss();
        }
    }

    /**
     * Used to fetch data from the database in a thread which is NOT the GUI thread.
     */
    private class AsyncFetchDatabase extends AsyncTask<Void, Void, List<Sequence>> {

        /**
         * Runs in the background. Upon finishing goes to onPostExecute.
         * @param params The parameters sent along.
         * @return Returns the data that was fetched.
         */
        @Override
        protected List<Sequence> doInBackground(Void... params) {
            return helper.sequenceController.getSequencesAndFramesByProfileIdAndType(selectedChild.getId(), Sequence.SequenceType.SEQUENCE);
        }

        /**
         * Checks the size of the returned data and adds it to the adapter.
         * @param result Updates the sequenceGrid based on the adapter.
         */
        @Override
        protected void onPostExecute(final List<Sequence> result) {
            sequenceAdapter = new SequenceListAdapter(MainActivity.this, result, MainActivity.this);
            if (result.size() == 0)
            {
                checkExistingSequences(true);
            }
            else
            {
                checkExistingSequences(false);
            }
            sequenceGrid.setAdapter(sequenceAdapter);
        }
    }

    /**
     * Occurs when returning to this activity.
     */
    @Override
    protected synchronized void onResume() {
        super.onResume();
        // Create the AsyncTask thread used to fetch database content
        AsyncFetchDatabase fetchDatabaseOnResume = new AsyncFetchDatabase();

        // Removes highlighting from Sequences that might have been lifted up when selected before entering the sequence
        for (int i = 0; i < sequenceGrid.getChildCount(); i++) {
            View view = sequenceGrid.getChildAt(i);
            ((PictogramView) view).placeDown();
        }

        //If a Child is selected at this point, update Sequences for the Child
        if (isChildSet) {
            fetchDatabaseOnResume.execute();
        }
    }

    /**
     * Occurs when the back button, Giraf or physical tablet, is clicked.
     */
    @Override
    public void onBackPressed() {
        if (markingMode) {
            markedSequences.clear();
            sequenceAdapter.notifyDataSetChanged();

            deleteButton.setVisibility(View.GONE);
            addButton.setVisibility(View.VISIBLE);
            changeUserButton.setVisibility(View.VISIBLE);
            setActionBarTitle(getResources().getString(R.string.app_name) + " - " + selectedChild.getName());
            markingMode = false;
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Occurs when the application is shut down.
     */
    @Override
    public void onStop() {
        // Ends the tracker of Google Analytics
        EasyTracker.getInstance(this).activityStop(this);
    }

    /**
     * Create and show the No Profile Selected error dialog.
     * <p>
     *     Used when the users have no selected a profile,
     *     and is trying to do an action that requires
     *     a profile being selected.
     * </p>
     */
    private void createAndShowErrorDialogNoProfileSelected() {
        final String NO_PROFILE_ERROR_TAG = "NO_PROFILE_ERROR_TAG";
        GirafNotifyDialog alertDialog = GirafNotifyDialog.newInstance(this.getString(R.string.error), this.getString(R.string.no_profile_error), NO_PROFILE_ERROR);
        alertDialog.show(getSupportFragmentManager(), NO_PROFILE_ERROR_TAG);
    }

    /**
     * Create and show the No Sequence Marked error dialog.
     * <p>
     *     Used when the user tries to delete sequences,
     *     but no sequences have been marked.
     * </p>
     */
    private void createAndShowErrorDialogNoSequencesMarked() {
        final String NO_SEQUENCE_MARKED_ERROR_TAG = "NO_SEQUENCE_MARKED_ERROR_TAG";
        GirafNotifyDialog alertDialog = GirafNotifyDialog.newInstance(this.getString(R.string.error), this.getString(R.string.no_sequence_marked_error), NO_SEQUENCE_MARKED_ERROR);
        alertDialog.show(getSupportFragmentManager(), NO_SEQUENCE_MARKED_ERROR_TAG);
    }
}
