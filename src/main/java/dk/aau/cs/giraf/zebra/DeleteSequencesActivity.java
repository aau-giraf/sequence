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


public class DeleteSequencesActivity extends GirafActivity implements SequenceListAdapter.SelectedSequenceAware {

    private Profile selectedChild;
    private int childId;

    private GridView sequenceGrid;
    private SequenceListAdapter sequenceAdapter;
    private List<SequenceListAdapter.SequencePictogramViewPair> sequenceViewList = new ArrayList<SequenceListAdapter.SequencePictogramViewPair>();
    private List<SequenceListAdapter.SequencePictogramViewPair> selectedSequenceViewList = new ArrayList<SequenceListAdapter.SequencePictogramViewPair>();
    private Helper helper;
    private final String DELETE_SEQUENCES = "DELETE_SEQUENCES";

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
        sequenceAdapter = new SequenceListAdapter(DeleteSequencesActivity.this, sequenceViewList, DeleteSequencesActivity.this);
        sequenceGrid.setAdapter(sequenceAdapter);
        sequenceGrid.setEmptyView(findViewById(R.id.empty_sequences));
    }

    // Button to accept delete of sequences
    public void deleteClick(View v) {
        acceptDeleteDialog.dismiss();
        // Delete all selected items
        for (SequenceListAdapter.SequencePictogramViewPair seq : selectedSequenceViewList) {
            helper = new Helper(getApplicationContext());
            helper.sequenceController.removeSequence(seq.getSequence());
        }
        // Go back to main Activity
        finish();
    }

    // Button to cancel delete of sequences
    public void cancelDeleteClick(View v) {
        acceptDeleteDialog.dismiss();
    }

    private void setupButtons(){
        //Creates all buttons in Activity and their listeners.

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedSequenceViewList.size() > 0){
                    acceptDeleteDialog = GirafInflatableDialog.newInstance(
                            getApplicationContext().getString(R.string.delete_sequences),
                            getApplicationContext().getString(R.string.delete_these) + " "
                                    + selectedSequenceViewList.size() + " "
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

                SequenceListAdapter.SequencePictogramViewPair sequenceViewPair = sequenceAdapter.getItem(position);

                if(selectedSequenceViewList.contains(sequenceViewPair)){
                    ((PictogramView) view).deleteModeUnmarked();
                    selectedSequenceViewList.remove(sequenceViewPair);
                }
                else {
                    ((PictogramView) view).deleteModeMarked();
                    selectedSequenceViewList.add(sequenceViewPair);
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

    @Override
    public boolean isSequenceMarked(Sequence sequence) {

        for (SequenceListAdapter.SequencePictogramViewPair sequenceViewPair : selectedSequenceViewList)
        {
            if (sequenceViewPair.getSequence().equals(sequence))
            {
                return true;
            }
        }
        return false;
    }

    // AsyncTask. Used to fetch data from the database in another thread which is NOT the GUI thread
    public class AsyncFetchDatabase extends AsyncTask<Void, Void, List<SequenceListAdapter.SequencePictogramViewPair>> {

        @Override
        protected List<SequenceListAdapter.SequencePictogramViewPair> doInBackground(Void... params) {
            helper = new Helper(DeleteSequencesActivity.this);
            List<Sequence> sequenceList = helper.sequenceController.getSequencesAndFramesByProfileIdAndType(selectedChild.getId(), Sequence.SequenceType.SEQUENCE);
            ArrayList<SequenceListAdapter.SequencePictogramViewPair> viewPairList = new ArrayList<SequenceListAdapter.SequencePictogramViewPair>();

            for (Sequence sequence : sequenceList) {
                viewPairList.add(new SequenceListAdapter.SequencePictogramViewPair(sequence, null));
            }
            return viewPairList;
        }

        @Override
        protected void onPostExecute(List<SequenceListAdapter.SequencePictogramViewPair> result) {
            sequenceAdapter = new SequenceListAdapter(DeleteSequencesActivity.this, result, DeleteSequencesActivity.this);
            sequenceGrid.setAdapter(sequenceAdapter);
            sequenceGrid.setEmptyView(findViewById(R.id.empty_sequences));
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
