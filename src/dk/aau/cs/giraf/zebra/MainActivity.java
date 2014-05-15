package dk.aau.cs.giraf.zebra;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import dk.aau.cs.giraf.gui.GComponent;
import dk.aau.cs.giraf.gui.GDialog;
import dk.aau.cs.giraf.gui.GGridView;
import dk.aau.cs.giraf.gui.GMultiProfileSelector;
import dk.aau.cs.giraf.gui.GProfileSelector;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;

public class MainActivity extends Activity {
    private boolean isInEditMode = false;
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
    public static Profile selectedChild;
    public static Activity activityToKill;
    private int childId;
    private Helper helper;
    private Profile guardian;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupGridView();
        setButtons();
        loadIntents();
        setColors();

        if (childId != -1) {
            setChild();
        }
    }

    private void setupGridView() {
        //Sets the GridView and adapter to display Sequences
        sequenceGrid = (GGridView) findViewById(R.id.sequence_grid);
        sequenceAdapter = new SequenceListAdapter(this, sequences);
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

        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                assumeMinimize = false;
                Intent intent = new Intent(getApplication(), SettingsActivity.class);
                intent.putExtra("childId", childId);

                startActivity(intent);
            }
        });

        logoutButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final GProfileSelector childSelector = new GProfileSelector(v.getContext(), guardian, null, false);
                childSelector.show();

                childSelector.setOnListItemClick(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                        selectedChild = helper.profilesHelper.getProfileById((int) id);
                        ((TextView) findViewById(R.id.child_name)).setText(selectedChild.getName());
                        updateSequences();
                        childSelector.dismiss();
                    }
                });
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
        int guardianId = extras.getInt("currentGuardianID");
        childId = extras.getInt("currentChildID");
        Log.d("DebugYeah", "[Main] Application launched with ChildId " + Integer.toString(childId));

        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }

        guardian = helper.profilesHelper.getProfileById(guardianId);
        Log.d("DebugYeah", Integer.toString(guardianId));

        //TODO: if childId == -1 the profileSelector containing only children connected to the current guardianId should appear. The guardian should be forced to chose a child.
        if (childId == -1) {
            setupChildMode();
            setupPickChild();

            return;
        } else {
            childIsSet = true;
        }

        //Makes the activity killable from SequenceActivity and (Nested) MainActivity
        if (extras.getBoolean("insertSequence") == false) {
            activityToKill = this;
        }

        //Set up user mode depending on extras
        if (extras.getBoolean("insertSequence")) {
            nestedMode = true;
            Log.d("DebugYeah", "[Main] NestedMode entered");
            setupNestedMode();
        }  else if (guardian.getRole() == Profile.Roles.GUARDIAN) {
            Log.d("DebugYeah", "[Main] User is Guardian");
            setupGuardianMode();
        } else {
            Log.d("DebugYeah", "[Main] User is Child");
            setupChildMode();
        }
    }

    private void setChild() {
        //Creates helper to get the relevant profiles from their ID's
        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }
        selectedChild = helper.profilesHelper.getProfileById(childId);
        ((TextView) findViewById(R.id.child_name)).setText(selectedChild.getName());
        updateSequences();
    }

    private void setColors() {

        LinearLayout backgroundLayout = (LinearLayout) findViewById(R.id.parent_container);
        RelativeLayout topbarLayout = (RelativeLayout) findViewById(R.id.sequence_bar);
        backgroundLayout.setBackgroundDrawable(GComponent.GetBackground(GComponent.Background.SOLID));
        topbarLayout.setBackgroundDrawable(GComponent.GetBackground(GComponent.Background.SOLID));
    }
    //TODO: create this functionality when database sync is ready.
    private boolean deleteSequenceDialog(final int position) {
        return true;

    }

    public void updateSequences() {
        tempSequenceList.clear();

        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }

        sequences = helper.sequenceController.getSequencesAndFramesByProfileIdAndType(selectedChild.getId(), Sequence.SequenceType.SEQUENCE);
        sequenceAdapter = new SequenceListAdapter(this, sequences);
        sequenceGrid.setAdapter(sequenceAdapter);
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

        public deletingSequencesDialog(final Context context) {

            super(context);

            this.SetView(LayoutInflater.from(this.getContext()).inflate(R.layout.deleting_sequences,null));

            copyAdapter = new SequenceListAdapter(this.getContext(), sequences);
            copyGrid = (GGridView) findViewById(R.id.existing_sequences);
            copyGrid.setAdapter(copyAdapter);
            setCopyGridItemClickListener(copyGrid);

            pasteAdapter = new SequenceListAdapter(this.getContext(), tempSequenceList);
            pasteGrid = (GGridView) findViewById(R.id.empty_sequences);
            pasteGrid.setAdapter(pasteAdapter);
            setPasteGridItemClickListener(pasteGrid);

            GButton popupDelete = (GButton) findViewById(R.id.popup_accept);
            GButton popupBack = (GButton) findViewById(R.id.popup_back);

            popupDelete.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    for (Sequence seq : tempSequenceList) {
                        try {
                            helper = new Helper(context);
                        } catch (Exception e) {
                        }
                        helper.sequenceController.removeSequence(seq);
                    }
                    updateSequences();
                    dismiss();
                }
            });

            popupBack.setOnClickListener(new GButton.OnClickListener() {

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

            try {
                helper = new Helper(context);
            } catch (Exception e) {
            }

            copyAdapter = new SequenceListAdapter(this.getContext(), sequences);
            copyGrid = (GGridView) findViewById(R.id.existing_sequences);
            copyGrid.setAdapter(copyAdapter);
            setCopyGridItemClickListener(copyGrid);

            pasteAdapter = new SequenceListAdapter(this.getContext(), tempSequenceList);
            pasteGrid = (GGridView) findViewById(R.id.empty_sequences);
            pasteGrid.setAdapter(pasteAdapter);
            setPasteGridItemClickListener(pasteGrid);

            GButton popupCopy = (GButton) findViewById(R.id.popup_copy_accept);
            GButton popupBack = (GButton) findViewById(R.id.popup_copy_back);


            List<Profile> children = new ArrayList<Profile>();

            final GMultiProfileSelector childSelector = new GMultiProfileSelector(context, helper.profilesHelper.getChildrenByGuardian(guardian), children);

            childSelector.setMyOnCloseListener(new GMultiProfileSelector.onCloseListener() {
                @Override
                public void onClose(List<Profile> selectedProfiles) {
                    for (Profile p : selectedProfiles){
                        for (Sequence s: tempSequenceList) {
                            helper.sequenceController.copySequenceAndFrames(s, p);

                        }
                    }
                }
            });

            popupCopy.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    childSelector.show();
                }
            });

            popupBack.setOnClickListener(new GButton.OnClickListener() {

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
                Log.d("DebugYeah", "[Main] Selected sequence has " + Integer.toString(sequenceAdapter.getItem(arg2).getFramesList().size()) + " frames");
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
        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                ((PictogramView) arg1).liftUp();
                assumeMinimize = false;

                SharedPreferences settings = getSharedPreferences(SettingsActivity.class.getName() + Integer.toString(MainActivity.selectedChild.getId()), MODE_PRIVATE);
                int pictogramSetting = settings.getInt("pictogramSetting", 5);
                boolean landscapeSetting = settings.getBoolean("landscapeSetting", true);

                Log.d("DebugYeah", Integer.toString(pictogramSetting));
                Log.d("DebugYeah", Boolean.toString(landscapeSetting));

                Intent intent = new Intent();
                intent.setComponent(new ComponentName("dk.aau.cs.giraf.sequenceviewer", "dk.aau.cs.giraf.sequenceviewer.MainActivity"));
                intent.putExtra("sequenceId", sequenceAdapter.getItem(arg2).getId());
                intent.putExtra("landscapeMode", landscapeSetting);
                intent.putExtra("visiblePictogramCount", pictogramSetting);
                intent.putExtra("callerType", "Zebra");
                startActivityForResult(intent, 0);
            }
        });
    }

    private void setupNestedMode() {

        //OnClickListener saves ID of selected Sequence so it can be picked up and finishes activity.
        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                ((PictogramView) arg1).liftUp();
                Sequence sequence = sequenceAdapter.getItem(arg2);
                //nestedSequenceId = sequence.getId();

                Intent intent = new Intent();
                intent.putExtra("nestedSequenceId",sequence.getId());
                setResult(RESULT_OK,intent);
                finishActivity();
            }
        });


    }

    private void setupPickChild(){

        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }

        final GProfileSelector childSelector = new GProfileSelector(this, guardian, null, false);
        try{childSelector.backgroundCancelsDialog(false);}
        catch (Exception e)
        {}
        childSelector.show();

        childSelector.setOnListItemClick(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                selectedChild = helper.profilesHelper.getProfileById((int) id);
                childIsSet = true;
                ((TextView) findViewById(R.id.child_name)).setText(selectedChild.getName());
                updateSequences();
                setupGuardianMode();
                childSelector.dismiss();
            }
        });
    }

    private void enterSequence(Sequence sequence, boolean isNew) {
        assumeMinimize = false;
        Intent intent = new Intent(getApplication(), SequenceActivity.class);
        intent.putExtra("profileId", selectedChild.getId());
        intent.putExtra("guardianId", guardian.getId());
        intent.putExtra("editMode", isInEditMode);
        intent.putExtra("new", isNew);
        intent.putExtra("sequenceId", sequence.getId());
        Log.d("DebugYeah", "[Main] Entering SequenceActivity for SequenceId " + Integer.toString(sequence.getId()));

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
        if (childIsSet) {
            updateSequences();
        }
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