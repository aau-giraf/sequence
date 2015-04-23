package dk.aau.cs.giraf.sequence;

import android.os.Bundle;
import android.view.View;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.List;

import dk.aau.cs.giraf.activity.GirafActivity;
import dk.aau.cs.giraf.gui.GirafButton;


public class CopySequencesActivity extends GirafActivity {
    // Initialize buttons
    private GirafButton acceptButton;

    private GridView copyGrid;
    private GridView pasteGrid;

    private SequenceListAdapter copyAdapter;
    private SequenceListAdapter pasteAdapter;

    private List<View> tempViewList = new ArrayList<View>();

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
    DO NOT DELETE


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
            //When closing the MultiProfileSelector, copy all chosen Sequences to all chosen Children
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

    private void clearTempLists() {
        tempViewList.clear();
        tempSequenceList.clear();
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


    */
}
