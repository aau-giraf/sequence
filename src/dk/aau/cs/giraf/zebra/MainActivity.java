package dk.aau.cs.giraf.zebra;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import dk.aau.cs.giraf.gui.GDialog;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;
import dk.aau.cs.giraf.oasis.lib.models.Frame;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.zebra.PictogramView.OnDeleteClickListener;
import dk.aau.cs.giraf.zebra.SequenceListAdapter.OnAdapterGetViewListener;
import dk.aau.cs.giraf.zebra.serialization.SequenceFileStore;


public class MainActivity extends Activity {
    private boolean isInEditMode = false;
    private boolean assumeMinimize = true;
    private GridView sequenceGrid;
    private SequenceListAdapter sequenceAdapter;
    private SequenceListAdapter copyAdapter;
    private SequenceListAdapter pasteAdapter;
    private GridView copyGrid;
    private GridView pasteGrid;
    private List<Sequence> sequences = new ArrayList<Sequence>();
    private List<Sequence> tempSequenceList = new ArrayList<Sequence>();
    private List<View> tempViewList = new ArrayList<View>();
    public static Profile selectedChild;
    public static int nestedSequenceId;
    private boolean nestedMode;
    private int guardianId;
    private int childId;
    private Helper helper;
    private int applicationColor = Color.parseColor("#8ba4bd");
    public static Activity activityToKill;

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

    private void setupGridView() {
        //Sets the GridView and adapter to display Sequences
        sequenceAdapter = setupAdapter();
        sequenceGrid = (GridView) findViewById(R.id.sequence_grid);
        sequenceGrid.setAdapter(sequenceAdapter);
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
                enterSequence(sequence, true);
            }
        });

        deleteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showDeleteDialog(v);
            }
        });

        copyButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showCopyDialog(v);
            }
        });

        exitButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finishActivity();
            }
        });
    }

    private void loadIntents() {
        //Fetches intents from launcher or SequenceActivity
        Bundle extras = getIntent().getExtras();
        guardianId = extras.getInt("currentGuardianID");
        //TODO: Revert after sprint end
        //childId = extras.getInt("currentChildID");
        long childIdLong = extras.getLong("currentChildID");
        childId = (int) (long) childIdLong;
        Log.d("DebugYeah", Integer.toString(childId));

        //Makes the activity killable from SequenceActivity and (Nested) MainActivity
        if (extras.getBoolean("insertSequence") == false) {
            activityToKill = this;
        }

        //Set up user mode depending on extras
        if (extras.getBoolean("insertSequence")) {
            nestedMode = true;
            setupNestedMode();
        }
        //TODO: find out what the guardianId is if its in childmode.
        else if (guardianId != 100) {
            setupGuardianMode();
        } else {
            setupChildMode();
        }
    }

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
                selectedChild = p;
                ((TextView) findViewById(R.id.child_name)).setText(selectedChild.getName());
            }
        }
        updateSequences();
    }

    private void setColors() {

        LinearLayout backgroundLayout = (LinearLayout) findViewById(R.id.parent_container);
        RelativeLayout topbarLayout = (RelativeLayout) findViewById(R.id.sequence_bar);
        backgroundLayout.setBackgroundColor(applicationColor);
        topbarLayout.setBackgroundColor(applicationColor);
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

    /*private void loadFakeSequences() {
        //TODO createFakeSequences is a temporary fix to generate some Sequences. Delete when done.
        List<Sequence> list = selectedChild.getSequences();
        list = createFakeSequences();
        selectedChild.setSequences(list);
    }

    private List<Sequence> createFakeSequences() {

        Sequence s = new Sequence();
        s.setName("TæstSækvæns");
        s.setPictogramId(10);
        s.setId(5);

        Frame a = new Frame();
        Frame b = new Frame();
        Frame c = new Frame();
        a.setPictogramId(0);
        b.setPictogramId(1);
        c.setPictogramId(2);
        s.addFrame(a);
        s.addFrame(b);
        s.addFrame(c);

        List<Sequence> list = sequences;
        for (int i = 0; i < 12; i++) {
            list.add(s);
        }
        return list;
    }*/

    //TODO: create this functionality when database sync is ready.
    private boolean deleteSequenceDialog(final int position) {
        return true;

    }

    public void updateSequences() {
        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }

        sequences.clear();
        sequences = helper.sequenceController.getSequenceByProfileIdAndType(selectedChild.getId(), Sequence.SequenceType.SEQUENCE);
        sequenceAdapter.notifyDataSetChanged();
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

        public deletingSequencesDialog(Context context) {

            super(context);

            this.SetView(LayoutInflater.from(this.getContext()).inflate(R.layout.deleting_sequences,null));

            copyAdapter = setupAdapter();
            copyGrid = (GridView) findViewById(R.id.existing_sequences);
            copyGrid.setAdapter(copyAdapter);
            setCopyGridItemClickListener(copyGrid);

            pasteAdapter = new SequenceListAdapter(this.getContext(), tempSequenceList);
            pasteGrid = (GridView) findViewById(R.id.empty_sequences);
            pasteGrid.setAdapter(pasteAdapter);
            setPasteGridItemClickListener(pasteGrid);

            GButton popupDelete = (GButton) findViewById(R.id.popup_accept);
            GButton popupDiscard = (GButton) findViewById(R.id.popup_back);
            GButton popupExit = (GButton) findViewById(R.id.popup_exit_button);

            popupDelete.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    //TODO: Make this functionality
                    //  deleteSequences();
                }
            });

            popupDiscard.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });

            popupExit.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });

        }
    }

    public class copyingSequencesDialog extends GDialog {

        public copyingSequencesDialog(Context context) {

            super(context);

            this.SetView(LayoutInflater.from(this.getContext()).inflate(R.layout.copying_sequences, null));

            copyAdapter = setupAdapter();
            copyGrid = (GridView) findViewById(R.id.existing_sequences);
            copyGrid.setAdapter(copyAdapter);
            setCopyGridItemClickListener(copyGrid);

            pasteAdapter = new SequenceListAdapter(this.getContext(), tempSequenceList);
            pasteGrid = (GridView) findViewById(R.id.empty_sequences);
            pasteGrid.setAdapter(pasteAdapter);
            setPasteGridItemClickListener(pasteGrid);

            GButton popupCopy = (GButton) findViewById(R.id.popup_copy_accept);
            GButton popupCopyDiscard = (GButton) findViewById(R.id.popup_copy_back);
            GButton popupExit = (GButton) findViewById(R.id.popup_exit_button);

            popupCopy.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    //TODO: Make this functionality
                    //  CopySequences();
                }
            });

            popupCopyDiscard.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });

            popupExit.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });

        }
    }

    private void setCopyGridItemClickListener(GridView copyGrid) {
        clearTempLists();
        copyGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                for (int i = 0; i < tempSequenceList.size(); i++) {
                    if (copyAdapter.getItem(position).getId() == tempSequenceList.get(i).getId()) {
                        return;
                    }
                }

                tempSequenceList.add(copyAdapter.getItem(position));
                tempViewList.add(copyAdapter.getView(position, view, parent));

                View v = copyAdapter.getView(position, view, parent);
                v.setAlpha(0.2f);
                v.setScaleY(0.85f);
                v.setScaleX(0.85f);

                pasteAdapter.notifyDataSetChanged();
            }
        });

    }

    private void setPasteGridItemClickListener(GridView pasteGrid) {
        clearTempLists();
        pasteGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                for (int i = 0; i < tempSequenceList.size(); i++) {
                    if (pasteAdapter.getItem(position).getId() == tempSequenceList.get(i).getId()) {

                        View v = tempViewList.get(i);
                        v.setAlpha(0.99f);
                        v.setScaleX(0.99f);
                        v.setScaleY(0.99f);

                        tempSequenceList.remove(i);
                        tempViewList.remove(i);

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
                nestedSequenceId = sequence.getId();
                finishActivity();
            }
        });


    }

    private void enterSequence(Sequence sequence, boolean isNew) {
        assumeMinimize = false;
        Intent intent = new Intent(getApplication(), SequenceActivity.class);
        intent.putExtra("profileId", selectedChild.getId());
        intent.putExtra("guardianId", guardianId);
        intent.putExtra("editMode", isInEditMode);
        intent.putExtra("new", isNew);
        intent.putExtra("applicationColor", applicationColor);
        if (isNew = false) {
            intent.putExtra("sequenceId", sequence.getId());
        }

        startActivity(intent);
    }

    private void finishActivity() {
        assumeMinimize = false;
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Removes highlighting from all images
        for (int i = 0; i < sequenceGrid.getChildCount(); i++) {
            View view = sequenceGrid.getChildAt(i);

            if (view instanceof PictogramView) {
                ((PictogramView) view).placeDown();
            }
        }
        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }
        sequences = helper.sequenceController.getSequenceByProfileIdAndType(selectedChild.getId(), Sequence.SequenceType.SEQUENCE);
        setupGridView();
    }

    @Override
    protected void onStop() {
        //assumeMinimize kills the entire application if minimized
        // in any other ways than opening SequenceActivity or inserting a nested Sequence
        if (assumeMinimize) {
            if (nestedMode) {
                SequenceActivity.activityToKill.finish();
                MainActivity.activityToKill.finish();
            }
            finishActivity();
        }
        else {
            assumeMinimize = true;
        }
        super.onStop();
    }
}