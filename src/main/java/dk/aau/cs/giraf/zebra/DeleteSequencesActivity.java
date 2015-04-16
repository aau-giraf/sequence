package dk.aau.cs.giraf.zebra;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.List;

import dk.aau.cs.giraf.activity.GirafActivity;
import dk.aau.cs.giraf.gui.GirafButton;
import dk.aau.cs.giraf.gui.GirafInflatableDialog;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;


public class DeleteSequencesActivity extends GirafActivity {

    private Profile selectedChild;
    private int childId;

    private GridView sequenceGrid;
    private SequenceListAdapter sequenceAdapter;
    private List<Sequence> sequences = new ArrayList<Sequence>();
    private Helper helper;
    private final String DELETE_SEQUENCES = "DELETE_SEQUENCES";
    private List<Sequence> selectedSequences = new ArrayList<Sequence>();

    GirafInflatableDialog acceptDeleteDialog;

    // Initialize buttons
    private GirafButton acceptButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_sequences);

        // Set top-bar title
        this.setActionBarTitle(getResources().getString(R.string.delete_sequences));

        // Creating buttons
        acceptButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_accept));

        // Adding buttons
        addGirafButtonToActionBar(acceptButton, RIGHT);

        // Setup additional content
        setupSequenceGridView();
        setupButtons();
        loadIntents();
        loadProfiles();
        setChild();
    }

    private void setupSequenceGridView() {
        //Sets the GridView and adapter to display Sequences
        sequenceGrid = (GridView) findViewById(R.id.sequence_grid);
        sequenceAdapter = new SequenceListAdapter(this, sequences);
        sequenceGrid.setAdapter(sequenceAdapter);
    }

    // Button to accept delete of sequences
    public void deleteClick(View v) {
        acceptDeleteDialog.dismiss();
        // Delete all selected items
        for (Sequence seq : selectedSequences) {
            helper = new Helper(getApplicationContext());
            helper.sequenceController.removeSequence(seq);
        }
        // Go back to main Activity
        finish();
    }

    // Button to cancel delete of sequences
    public void dontDeleteClick(View v) {
        acceptDeleteDialog.dismiss();
    }

    private void setupButtons(){
        //Creates all buttons in Activity and their listeners.

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedSequences.size() > 0){
                    acceptDeleteDialog = GirafInflatableDialog.newInstance(
                            getApplicationContext().getString(R.string.delete_sequences),
                            getApplicationContext().getString(R.string.delete_these) + " "
                                    + selectedSequences.size() + " "
                                    + getApplicationContext().getString(R.string.marked_sequences),
                            R.layout.dialog_delete);
                    acceptDeleteDialog.show(getSupportFragmentManager(), DELETE_SEQUENCES);
                }
                else{
                    finish();
                }
            }
        });

        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                Sequence sequence = sequenceAdapter.getItem(position);

                if(selectedSequences.contains(sequence)){
                    ((PictogramView) view).deleteModeUnmarked();
                    selectedSequences.remove(sequence);
                }
                else {
                    ((PictogramView) view).deleteModeMarked();
                    selectedSequences.add(sequence);
                }
            }
        });
    }

    private void loadIntents() {
        Bundle extras = getIntent().getExtras();
        childId = extras.getInt("childId");
    }

    private void loadProfiles() {
        //Create helper to load Child from Database
        helper = new Helper(this);
        selectedChild = helper.profilesHelper.getProfileById(childId);
    }

    // AsyncTask. Used to fetch data from the database in another thread which is NOT the GUI thread
    public class AsyncFetchDatabase extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            helper = new Helper(DeleteSequencesActivity.this);
            sequences = helper.sequenceController.getSequencesAndFramesByProfileIdAndType(selectedChild.getId(), Sequence.SequenceType.SEQUENCE);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            sequenceAdapter = new SequenceListAdapter(DeleteSequencesActivity.this, sequences);
            sequenceGrid.setAdapter(sequenceAdapter);
        }
    }

    private synchronized void setChild() {
        //Creates helper to fetch data from the Database
        helper = new Helper(this);

        //Save Child locally and update relevant information for application
        selectedChild = helper.profilesHelper.getProfileById(childId);

        // AsyncTask thread
        AsyncFetchDatabase fetchDatabaseSetChild = new AsyncFetchDatabase();
        fetchDatabaseSetChild.execute();
    }





    /*
    OLD DELETE CODE
    DO NOT DELETE


    private class deletingSequencesDialog extends GDialog {
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
                        helper = new Helper(context);
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
    */
}
