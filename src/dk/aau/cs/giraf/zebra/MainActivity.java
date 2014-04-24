package dk.aau.cs.giraf.zebra;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import dk.aau.cs.giraf.gui.GButton;
import dk.aau.cs.giraf.gui.GButtonSettings;
import dk.aau.cs.giraf.gui.GButtonTrash;
import dk.aau.cs.giraf.gui.GDialogMessage;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.zebra.PictogramView.OnDeleteClickListener;
import dk.aau.cs.giraf.zebra.SequenceListAdapter.OnAdapterGetViewListener;
import dk.aau.cs.giraf.zebra.models.Child;
import dk.aau.cs.giraf.zebra.models.Pictogram;
import dk.aau.cs.giraf.zebra.models.Sequence;
import dk.aau.cs.giraf.zebra.serialization.SequenceFileStore;


public class MainActivity extends Activity {
    private boolean isInEditMode = false;
    private GridView sequenceGrid;
    private SequenceListAdapter sequenceAdapter;
    private List<Sequence> sequences = new ArrayList<Sequence>();
    public static Child selectedChild;
    public static Long nestedSequenceId;
    private int guardianId;
    private int childId;
    private Helper helper;
    private int applicationColor = Color.parseColor("#8ba4bd");

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupGridView();
        setButtons();
        loadIntents();
        setChild();
        setColors();
    }

    private void setColors() {

        LinearLayout backgroundLayout = (LinearLayout) findViewById(R.id.parent_container);
        RelativeLayout topbarLayout = (RelativeLayout) findViewById(R.id.sequence_bar);
        backgroundLayout.setBackgroundColor(applicationColor);
        topbarLayout.setBackgroundColor(applicationColor);
    }

    //Finds the child we want to work with. This is given through the extra, "currentChildID".
    private void setChild() {
        //Creates helper to get the relevant profiles from their ID's
        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }

        Profile guardian = helper.profilesHelper.getProfileById(guardianId);
        List<Profile> childProfiles = helper.profilesHelper.getChildrenByGuardian(guardian);

        for (Profile p : childProfiles) {
            //When the correct child is found it is created using a local child class
            if (p.getId() == childId) {
                Child c = new Child(childId, p.getName(), p.getImage());
                selectedChild = c;
                ((TextView) findViewById(R.id.child_name)).setText(selectedChild.getName());
            }
        }
        updateSequences();
    }

    private SequenceListAdapter setupAdapter() {
        final SequenceListAdapter adapter = new SequenceListAdapter(this, sequences);
        adapter.setOnAdapterGetViewListener(new OnAdapterGetViewListener() {
            @Override
            public void onAdapterGetView(final int position, View view) {
                if (view instanceof PictogramView) {
                    PictogramView pictoView = (PictogramView) view;
                    pictoView.setOnDeleteClickListener(new OnDeleteClickListener() {
                        @Override
                        public void onDeleteClick() {
                            deleteSequenceDialog(position);
                        }
                    });
                }
            }
        });
        return adapter;
    }

    private void setupGridView() {
        //Sets the GridView and adapter to display Sequences
        sequenceAdapter = setupAdapter();
        sequenceGrid = (GridView) findViewById(R.id.sequence_grid);
        sequenceGrid.setAdapter(sequenceAdapter);
    }

    private void loadSequences() {
        //TODO createFakeSequences is a temporary fix to generate some Sequences. Delete when done.
        List<Sequence> list; // = SequenceFileStore.getSequences(this, selectedChild);
        list = createFakeSequences();
        selectedChild.setSequences(list);
    }

    private void loadIntents() {
        //Fetches intents from launcher or SequenceActivity and sets up mode accordingly
        Bundle extras = getIntent().getExtras();
        guardianId = extras.getInt("currentGuardianID");
        //TODO: childId from launcher is currently long, but we expect it to be int soon. This is why we parse it here.
        long childIdLong = extras.getLong("currentChildID");
        childId = Integer.parseInt(Long.toString(childIdLong));
        if (extras.getBoolean("insertSequence")) {
            setupNestedMode();
        }
        //TODO: find out what the guardianId is if its in childmode.
        else if (guardianId != 100) {
            setupGuardianMode();
        } else {
            setupChildMode();
        }
    }

    private List<Sequence> createFakeSequences() {

        Sequence s = new Sequence();
        s.setTitle("Testsekvens");
        s.setImageId(10);
        s.setSequenceId(5);

        Pictogram a = new Pictogram();
        Pictogram b = new Pictogram();
        Pictogram c = new Pictogram();
        a.setPictogramId(0);
        b.setPictogramId(1);
        c.setPictogramId(2);
        a.setType("pictogram");
        b.setType("choice");
        c.setType("sequence");
        s.addPictogramAtEnd(a);
        s.addPictogramAtEnd(b);
        s.addPictogramAtEnd(c);

        List<Sequence> list = sequences;
        for (int i = 0; i < 12; i++) {
            list.add(s);
        }
        return list;
    }

    void showMainDeleteDialog(View v) {
        //TODO: Call the deletesequence method here
        GDialogMessage deleteDialog = new GDialogMessage(v.getContext(),
                R.drawable.ic_launcher,
                "Slet Sekvens",
                "Du er ved at slette et antal sekvenser",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                }
        );

        deleteDialog.show();
    }

    //TODO: create this functionality when database sync is ready.
    private boolean deleteSequenceDialog(final int position) {
        return true;

    }

    public void updateSequences() {
        sequences.clear();
        sequences.addAll(selectedChild.getSequences());
        loadSequences();
        sequenceAdapter.notifyDataSetChanged();
    }

    private void setButtons() {
        GButton addButton = (GButton) findViewById(R.id.add_button);
        GButtonTrash deleteButton = (GButtonTrash)findViewById(R.id.delete_button);
        GButton copyButton = (GButton) findViewById(R.id.copy_button);
        GButtonSettings settingsButton = (GButtonSettings)findViewById(R.id.settings_button);
        GButton logoutButton = (GButton) findViewById(R.id.relog_button);
        GButton exitButton = (GButton) findViewById(R.id.exit_button);

        addButton.setVisibility(View.INVISIBLE);
        deleteButton.setVisibility(View.INVISIBLE);
        copyButton.setVisibility(View.INVISIBLE);
        settingsButton.setVisibility(View.INVISIBLE);
        logoutButton.setVisibility(View.INVISIBLE);
        exitButton.setVisibility(View.INVISIBLE);

        addButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                Sequence sequence = new Sequence();
                sequence.setSequenceId(selectedChild.getNextSequenceId());
                selectedChild.getSequences().add(sequence);

                enterSequence(sequence, true);
            }
        });

        deleteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showMainDeleteDialog(v);
            }
        });

        copyButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {


            }
        });

        exitButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setupGuardianMode() {

        isInEditMode = true;

        //OnClickListener leads to SequenceActivity so Guardian can edit Sequence
        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                ((PictogramView) arg1).liftUp();
                Sequence sequence = sequenceAdapter.getItem(arg2);
                enterSequence(sequence, false);
            }
        });

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
    }

    private void setupNestedMode() {

        //OnClickListener saves ID of selected Sequence so it can be picked up and finishes activity.
        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                ((PictogramView) arg1).liftUp();
                Sequence sequence = sequenceAdapter.getItem(arg2);
                nestedSequenceId = sequence.getSequenceId();
                finish();
            }
        });


    }

    private void enterSequence(Sequence sequence, boolean isNew) {
        Intent intent = new Intent(getApplication(), SequenceActivity.class);
        intent.putExtra("profileId", selectedChild.getProfileId());
        intent.putExtra("sequenceId", sequence.getSequenceId());
        intent.putExtra("guardianId", guardianId);
        intent.putExtra("editMode", isInEditMode);
        intent.putExtra("new", isNew);
        intent.putExtra("applicationColor", applicationColor);

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSequences();

        // Removes highlighting from all images
        for (int i = 0; i < sequenceGrid.getChildCount(); i++) {
            View view = sequenceGrid.getChildAt(i);

            if (view instanceof PictogramView) {
                ((PictogramView) view).placeDown();
            }
        }
    }
}