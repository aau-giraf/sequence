package dk.aau.cs.giraf.zebra;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dk.aau.cs.giraf.activity.GirafActivity;
import dk.aau.cs.giraf.gui.GButton;
import dk.aau.cs.giraf.gui.GDialog;
import dk.aau.cs.giraf.gui.GDialogAlert;
import dk.aau.cs.giraf.gui.GDialogMessage;
import dk.aau.cs.giraf.gui.GirafButton;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Frame;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.zebra.PictogramView.OnDeleteClickListener;
import dk.aau.cs.giraf.zebra.SequenceAdapter.OnAdapterGetViewListener;
import dk.aau.cs.giraf.zebra.SequenceViewGroup.OnNewButtonClickedListener;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;
import dk.aau.cs.giraf.oasis.lib.models.Pictogram;

public class AddEditSequencesActivity extends GirafActivity {

    private Profile guardian;
    private Profile selectedChild;
    private boolean isInEditMode;
    private boolean isNew;

    public static boolean choiceMode = false;
    private int guardianId;
    private int childId;
    private int sequenceId;
    private int pictogramEditPos = -1;
    public static Sequence sequence;
    public static Sequence choice = new Sequence();
    public SequenceAdapter adapter;
    public SequenceAdapter choiceAdapter;
    private List<Frame> tempFrameList;
    private List<Pictogram> tempPictogramList = new ArrayList<Pictogram>();
    private final String PICTO_INTENT_CHECKOUT_ID = "checkoutIds";
    private final int PICTO_SEQUENCE_IMAGE_CALL = 345;
    private final int PICTO_EDIT_PICTOGRAM_CALL = 456;
    private final int PICTO_NEW_PICTOGRAM_CALL = 567;
    private final int SEQUENCE_VIEWER_CALL = 1337;
    private final int NESTED_SEQUENCE_CALL = 40;
    private Helper helper;

    // Initialize buttons
    private GirafButton saveButton;
    private GirafButton deleteButton;
    private GirafButton sequenceImageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_sequences);

        // Create buttons
        saveButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_save));
        deleteButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_delete));
        sequenceImageButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_help));

        // Adding buttons to action-bar
        addGirafButtonToActionBar(saveButton, LEFT);
        addGirafButtonToActionBar(deleteButton, RIGHT);
        addGirafButtonToActionBar(sequenceImageButton, LEFT);

        loadIntents();
        loadProfiles();
        loadSequence();
        setupFramesGrid();
        setupButtons();
        setupTopBar();
    }

    private void loadIntents() {
        Bundle extras = getIntent().getExtras();
        childId = extras.getInt("childId");
        sequenceId = extras.getInt("sequenceId");
        guardianId = extras.getInt("guardianId");
        isNew = extras.getBoolean("isNew");
        isInEditMode = extras.getBoolean("editMode");
    }

    private void loadProfiles() {
        //Create helper to load Child from Database
        helper = new Helper(this);
        selectedChild = helper.profilesHelper.getProfileById(childId);
        guardian = helper.profilesHelper.getProfileById(guardianId);
    }

    private void loadSequence() {
        //Create helper to load data from Database (Otherwise start working on empty Sequence)
        helper = new Helper(this);

        //If SequenceId from intents is valid, get it from the Database
        if (sequenceId != 0) {
            sequence = helper.sequenceController.getSequenceAndFrames(sequenceId);

            // Orders the frames by the X coordinate
            Collections.sort(sequence.getFramesList(), new Comparator<Frame>() {
                public int compare(Frame x, Frame y) {
                    return Integer.valueOf(x.getPosX()).compareTo(y.getPosX());
                }
            });
        } else {
            sequence = new Sequence();
        }
    }

    private void setupFramesGrid() {
        // Create Adapter for the SequenceViewGroup (The Grid displaying the Sequence)
        adapter = setupAdapter();
        setupSequenceViewGroup(adapter);
    }

    private void setupButtons() {

        saveButton.setOnClickListener(new ImageButton.OnClickListener() {
            //Show Dialog to save Sequence when clicking the Save Button
            @Override
            public void onClick(View v) {
                createAndShowSaveDialog(v);
            }
        });


        sequenceImageButton.setOnClickListener(new ImageView.OnClickListener() {
            //If Sequence Image Button is clicked, call PictoAdmin to select an Image for the Sequence
            @Override
            public void onClick(View v) {
                if (isInEditMode) {
                    callPictoSearch(PICTO_SEQUENCE_IMAGE_CALL);
                }
            }
        });

        /*
        //If no Image has been selected or the Sequence, display the Add Sequence Picture. Otherwise load the image for the Button
        if (sequence.getPictogramId() == 0) {
            Drawable d = getResources().getDrawable(R.drawable.add_sequence_picture);
            sequenceImageButton.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
        } else {
            helper = new Helper(this);
            Drawable d = new BitmapDrawable(getResources(), helper.pictogramHelper.getPictogramById(sequence.getPictogramId()).getImage());
            sequenceImageButton.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
        }
        */
    }

    private void setupTopBar() {
        //initializeSequenceTitle();
        initializeChildTitle();
    }

    /* Editable title
    private void initializeSequenceTitle() {
        //Set Sequence name in Title (if any)
        sequenceTitleView = (EditText) findViewById(R.id.sequence_title);
        sequenceTitleView.setText(sequence.getName());

        // Create listener to remove focus when "Done" is pressed on the keyboard
        sequenceTitleView.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    EditText editText = (EditText) findViewById(R.id.sequence_title);
                    editText.clearFocus();
                }
                return false;
            }
        });

        // Create listener on Parent View(s) to remove focus when touched
        createClearFocusListener(findViewById(R.id.parent_container));

        // Create listener to hide the keyboard when the EditText loses focus
        sequenceTitleView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                EditText sequenceTitle = (EditText) findViewById(R.id.sequence_title);
                if (hasFocus) {
                    //Makes the hint text from the SequenceTitle transparent if sequenceTitle is blank
                    if (sequenceTitle.getText().toString().equals("")) {
                        sequenceTitle.setHintTextColor(Color.TRANSPARENT);
                    }
                } else {
                    // Hides the keyboard and reverts hint color when sequenceTitle is not active
                    InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    in.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    sequenceTitle.setHintTextColor(Color.parseColor("#55624319"));
                }
            }
        });
    }*/

    private void initializeChildTitle() {
        //Creates helper to fetch data from the Database
        helper = new Helper(this);

        //Save Child locally and update relevant information for application
        selectedChild = helper.profilesHelper.getProfileById(childId);
        this.setActionBarTitle(selectedChild.getName()); // selectedChild.getName() "Child's name code"
    }

    /*
    private void createClearFocusListener(View view) {
        // Create listener to remove focus from EditText when something else is touched
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    EditText editText = (EditText) findViewById(R.id.sequence_title);
                    editText.clearFocus();
                    return false;
                }
            });
        }

        // If the view is a container, run the function recursively on the children
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                createClearFocusListener(innerView);
            }
        }
    }*/

    private boolean checkSequenceBeforeSave(View v) {
        //Checks if Sequence is empty. If not empty, save it and return
        if (sequence.getFramesList().size() == 0) {
            createAndShowErrorDialog(v);
            return false;
        } else {
            saveChanges();
            return true;
        }
    }

    private void saveChanges() {
        //Create helper to use Database Helpers
        helper = new Helper(this);

        //Save name from Title to the Sequence
        //sequence.setName(sequenceTitleView.getText().toString());
        sequence.setName("placeholder");

        //Set PosX of every frame to save the order in which the frames should be shown.
        for (int i = 0; i < sequence.getFramesList().size(); i++) {
            sequence.getFramesList().get(i).setPosX(i);
        }

        //If Sequence is new, set relevant properties and insert to Database
        if (isNew) {
            sequence.setProfileId(selectedChild.getId());
            sequence.setSequenceType(Sequence.SequenceType.SEQUENCE);
            helper.sequenceController.insertSequenceAndFrames(sequence);
        }
        //If Sequence exists, modify in Database
        else {
            helper.sequenceController.modifySequenceAndFrames(sequence);
        }
    }

    private void createAndShowSaveDialog(View v) {
        //Creates a dialog for saving Sequence. If Sequence is saved succesfully, exit Activity
        GDialogMessage saveDialog = new GDialogMessage(v.getContext(), R.drawable.save,
        getResources().getString(R.string.save_sequence),
        getResources().getString(R.string.save_sequence_desc),
        new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                   boolean sequenceOk;
                sequenceOk = checkSequenceBeforeSave(v);
                if (sequenceOk) {
                    finishActivity();
                }
            }
        });
        saveDialog.show();
    }

    private void createAndShowBackDialog(View v) {
        //Create instance of BackDialog class and display it
        BackDialog backDialog = new BackDialog(v.getContext());
        backDialog.show();
    }

    private void createAndShowAddDialog(View v) {
        //Create instance of AddDialog and display it
        AddDialog addFrame = new AddDialog(v.getContext());
        addFrame.show();
    }

    private void createAndShowChoiceDialog(View v) {
        //Create instance of ChoiceDialog and display it
        ChoiceDialog choiceDialog = new ChoiceDialog(v.getContext());
        choiceDialog.show();
    }

    private void createAndShowNestedDialog(View v) {
        //Creates a Dialog for information. Clicking OK starts MainActivity in nestedMode
        GDialogMessage nestedDialog = new GDialogMessage(v.getContext(),
            //TODO: Find a better icon than the ic_launcher icon
            R.drawable.ic_launcher,
            "Åbner sekvensvalg",
            "Et nyt vindue åbnes, hvor du kan vælge en anden sekvens at indsætte",
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                //Put required Intents to set up Nested Mode
                Intent intent = new Intent(getApplication(), MainActivity.class);
                intent.putExtra("insertSequence", true);
                intent.putExtra("currentGuardianID", guardian.getId());
                intent.putExtra("currentChildID", childId);
                startActivityForResult(intent, NESTED_SEQUENCE_CALL);
                }
            });
        nestedDialog.show();
    }

    private void createAndShowErrorDialog(View v) {
        //Creates alertDialog to display error. Clicking Ok dismisses the Dialog
        GDialogAlert alertDialog = new GDialogAlert(v.getContext(), R.drawable.delete,
            "Fejl",
            "Du kan ikke gemme en tom Sekvens",
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });
        alertDialog.show();
    }

    private SequenceViewGroup setupSequenceViewGroup(final SequenceAdapter adapter) {
        //The SequenceViewGroup class takes care of most of the required functionality, including size properties, dragging and rearranging

        //Set up adapter to display the Sequence
        final SequenceViewGroup sequenceGroup = (SequenceViewGroup) findViewById(R.id.sequenceViewGroup);
        sequenceGroup.setEditModeEnabled(isInEditMode);
        sequenceGroup.setAdapter(adapter);

        //When clicking the big "+", lift up the view and show the Add Dialog
        sequenceGroup.setOnNewButtonClickedListener(new OnNewButtonClickedListener() {
            @Override
            public void onNewButtonClicked() {
                final SequenceViewGroup sequenceGroup = (SequenceViewGroup) findViewById(R.id.sequenceViewGroup);
                sequenceGroup.liftUpAddNewButton();
                createAndShowAddDialog(sequenceGroup);
            }
        });

        //If clicking an item, save the position, save the Frame, and find out what kind of Frame it is. Then perform relevant action
        sequenceGroup.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {

                //Save Frame and Position
                pictogramEditPos = position;
                Frame frame = sequence.getFramesList().get(position);

                //Perform action depending on the type of pictogram clicked.
                checkFrameMode(frame, view);
            }
        });

        //Handle Rearrange
        sequenceGroup.setOnRearrangeListener(new SequenceViewGroup.OnRearrangeListener() {
            @Override
            public void onRearrange(int indexFrom, int indexTo) {
                adapter.notifyDataSetChanged();
            }
        });

        return sequenceGroup;
    }

    private SequenceAdapter setupAdapter() {
        //Sets up the adapter for the Sequence to display
        final SequenceAdapter adapter = new SequenceAdapter(this, sequence);

        //Adds a Delete Icon to all Frames which deletes the relevant Frame on click.
        adapter.setOnAdapterGetViewListener(new OnAdapterGetViewListener() {
            @Override
            public void onAdapterGetView(final int position, final View view) {
                if (view instanceof PictogramView) {
                    //Cast view to PictogramView so the onDeleteClickListener can be set
                    PictogramView v = (PictogramView) view;
                    v.setOnDeleteClickListener(new OnDeleteClickListener() {
                        @Override
                        public void onDeleteClick() {
                            //Remove frame and update Adapter
                            sequence.getFramesList().remove(position);
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
        return adapter;
    }

    private SequenceAdapter setupChoiceAdapter() {
        //Sets up the adapter for the Choice Frames
        final SequenceAdapter adapter = new SequenceAdapter(this, choice);

        //Adds a Delete Icon to all Frames which deletes the relevant Frame on click.
        adapter.setOnAdapterGetViewListener(new OnAdapterGetViewListener() {
            @Override
            public void onAdapterGetView(final int position, final View view) {
                if (view instanceof PictogramView) {
                    //Cast view to PictogramView so the onDeleteClickListener can be set
                    PictogramView v = (PictogramView) view;
                    v.setOnDeleteClickListener(new OnDeleteClickListener() {
                        @Override
                        public void onDeleteClick() {
                            //Remove frame and update Adapter
                            choice.getFramesList().remove(position);
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
        return adapter;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //If Activity Result is OK, call relevant method depending on the RequestCode used to launch Activity with
        if (resultCode == RESULT_OK) {
            switch (requestCode) {

                case PICTO_SEQUENCE_IMAGE_CALL:
                    OnEditSequenceImageResult(data);
                    break;

                case PICTO_EDIT_PICTOGRAM_CALL:
                    OnEditPictogramResult(data);
                    break;

                case PICTO_NEW_PICTOGRAM_CALL:
                    // Remove the highlighting from the add pictogram button
                    final SequenceViewGroup sequenceGroup = (SequenceViewGroup) findViewById(R.id.sequenceViewGroup);
                    sequenceGroup.placeDownAddNewButton();
                    OnNewPictogramResult(data);
                    break;

                default:
                    break;
            }
        }
    }

    private void OnNewPictogramResult(Intent data) {
        int[] checkoutIds = data.getExtras().getIntArray(
                PICTO_INTENT_CHECKOUT_ID);

        //If no pictures are returned, assume user canceled and nothing is supposed to change.
        if (checkoutIds.length == 0 || checkoutIds == null) {
            return;
        }
        if (choiceMode) {

            for (int id : checkoutIds) {
                Pictogram pictogram = new Pictogram();
                pictogram.setId(id);
                tempPictogramList.add(pictogram);

                Frame frame = new Frame();
                frame.setPictogramId(id);
                choice.addFrame(frame);

                if (choice.getPictogramId() == 0) {
                    choice.setPictogramId(checkoutIds[0]);
                }
            }
            choiceAdapter.notifyDataSetChanged();
        }
        else {

            for (int id : checkoutIds) {
                Frame frame = new Frame();
                frame.setPictogramId(id);
                sequence.addFrame(frame);
            }

            if (sequence.getPictogramId() == 0 && checkoutIds.length > 0) {
                sequence.setPictogramId(checkoutIds[0]);
                helper = new Helper(this);
                Drawable d = new BitmapDrawable(getResources(), helper.pictogramHelper.getPictogramById(sequence.getPictogramId()).getImage());
                //sequenceImageButton.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
                sequenceImageButton.setVisibility(View.GONE);
                sequenceImageButton.setVisibility(View.VISIBLE);
            }
            adapter.notifyDataSetChanged();
        }
    }

    private void OnEditPictogramResult(Intent data) {
        if (pictogramEditPos < 0) {
            return;
        }

        int[] checkoutIds = data.getExtras().getIntArray(
                PICTO_INTENT_CHECKOUT_ID);

        if (checkoutIds.length == 0) {
            return;
        }

        Frame frame = sequence.getFramesList().get(pictogramEditPos);
        frame.setPictogramId(checkoutIds[0]);
        adapter.notifyDataSetChanged();
    }

    private void OnEditSequenceImageResult(Intent data) {
        int[] checkoutIds = data.getExtras().getIntArray(
                PICTO_INTENT_CHECKOUT_ID);

        if (checkoutIds.length == 0) {
            return;
        }

        sequence.setPictogramId(checkoutIds[0]);
        //Drawable d = new BitmapDrawable(getResources(), helper.pictogramHelper.getPictogramById(sequence.getPictogramId()).getImage());
        //sequenceImageButton.setCompoundDrawablesWithIntrinsicBounds(0, d, 0, 0);
        sequenceImageButton.setVisibility(View.GONE);
        sequenceImageButton.setVisibility(View.VISIBLE);
    }

    // send an intent to start pictosearch, and returns the result from pictosearch
    private void callPictoSearch(int modeId) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("dk.aau.cs.giraf.pictosearch", "dk.aau.cs.giraf.pictosearch.PictoAdminMain"));
        intent.putExtra("currentChildID", selectedChild.getId());
        intent.putExtra("currentGuardianID", guardian.getId());

        if (modeId == PICTO_NEW_PICTOGRAM_CALL){
            intent.putExtra("purpose", "multi");
        }
        else {
            intent.putExtra("purpose", "single");
        }
        startActivityForResult(intent, modeId);
    }

    // method for opening the sequence viewer with a the sequence chosen.
    private void callSequenceViewer() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("dk.aau.cs.giraf.sequenceviewer", "dk.aau.cs.giraf.sequenceviewer.MainActivity"));
        intent.putExtra("sequenceId", sequence.getId());
        intent.putExtra("callerType", "Zebra");
        startActivityForResult(intent, SEQUENCE_VIEWER_CALL);
    }

    private void finishActivity() {
        finish();
    }

    private void checkFrameMode(Frame frame, View v) {

        if (frame.getPictogramList().size() > 0) {
            createAndShowChoiceDialog(v);
        } else {
            callPictoSearch(PICTO_EDIT_PICTOGRAM_CALL);
        }
    }

    private class AddDialog extends GDialog {

        private AddDialog(Context context) {
            super(context);
            this.SetView(LayoutInflater.from(this.getContext()).inflate(R.layout.add_frame_dialog, null));

            GButton getPictogram = (GButton) findViewById(R.id.get_pictogram);
            GButton getChoice = (GButton) findViewById(R.id.get_choice);

            getPictogram.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    callPictoSearch(PICTO_NEW_PICTOGRAM_CALL);
                    dismiss();
                }
            });
            getChoice.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    choiceMode = true;
                    dismiss();
                    createAndShowChoiceDialog(v);
                }
            });
        }
    }

    private class ChoiceDialog extends GDialog {
        private ChoiceDialog(Context context) {
            super(context);

            choice.getFramesList().clear();
            if (pictogramEditPos != -1) {
                for (int i = 0; i < adapter.getItem(pictogramEditPos).getPictogramList().size(); i++) {
                    Frame frame = new Frame();
                    frame.setPictogramId(adapter.getItem(pictogramEditPos).getPictogramList().get(i).getId());
                    choice.addFrame(frame);
                }
            }

            this.SetView(LayoutInflater.from(this.getContext()).inflate(R.layout.choice_dialog, null));

            GButton saveChoice = (GButton) findViewById(R.id.save_choice);
            GButton discardChoice = (GButton) findViewById(R.id.discard_choice);

            //Adapter to display a list of pictograms in the choice dialog
            choiceAdapter = setupChoiceAdapter();

            saveChoice.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    tempFrameList = sequence.getFramesList();
                    Frame frame = new Frame();
                    if (tempPictogramList == null) {
                        //TODO: Display message that user can not save empty choice.
                        return;
                    }
                    frame.setPictogramList(tempPictogramList);
                    frame.setPictogramId(tempPictogramList.get(0).getId());

                    if (pictogramEditPos == -1) {
                        sequence.addFrame(frame);
                        pictogramEditPos = tempFrameList.size() - 1;
                    } else {
                        sequence.getFramesList().get(pictogramEditPos).setPictogramList(tempPictogramList);
                    }
                    adapter.notifyDataSetChanged();
                    choiceMode = false;
                    pictogramEditPos = -1;
                    dismiss();
                }
            });
            discardChoice.setOnClickListener(new GButton.OnClickListener() {
                @Override
                public void onClick(View v) {
                    choiceMode = false;
                    dismiss();
                }
            });
            setupChoiceGroup(choiceAdapter);
        }

        private SequenceViewGroup setupChoiceGroup(
                final SequenceAdapter adapter) {
            final SequenceViewGroup choiceGroup = (SequenceViewGroup) findViewById(R.id.choice_view_group);
            choiceGroup.setEditModeEnabled(isInEditMode);
            choiceGroup.setAdapter(adapter);

            // Handle rearrange
            choiceGroup.setOnRearrangeListener(new SequenceViewGroup.OnRearrangeListener() {
                        @Override
                        public void onRearrange(int indexFrom, int indexTo) {
                            adapter.notifyDataSetChanged();
                        }
                    });

            // Handle new pictogram added to the view
            choiceGroup.setOnNewButtonClickedListener(new OnNewButtonClickedListener() {
                        @Override
                        public void onNewButtonClicked() {
                            final SequenceViewGroup sequenceGroup = (SequenceViewGroup) findViewById(R.id.choice_view_group);
                            sequenceGroup.liftUpAddNewButton();

                            callPictoSearch(PICTO_NEW_PICTOGRAM_CALL);
                        }
                    });

            // Handle pictogram edit
            choiceGroup.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapter, View view,
                                        int position, long id) {
                    pictogramEditPos = position;
                    callPictoSearch(PICTO_EDIT_PICTOGRAM_CALL);
                }
            });

            return choiceGroup;
        }
    }

    private class BackDialog extends GDialog {

        public BackDialog(Context context) {

            super(context);

            this.SetView(LayoutInflater.from(this.getContext()).inflate(R.layout.exit_sequence_dialog, null));

            GButton saveChanges = (GButton) findViewById(R.id.save_changes);
            GButton discardChanges = (GButton) findViewById(R.id.discard_changes);
            GButton cancel = (GButton) findViewById(R.id.return_to_editting);

            saveChanges.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    boolean sequenceOk;
                    sequenceOk = checkSequenceBeforeSave(v);
                    dismiss();
                    if (sequenceOk) {
                        finishActivity();
                    }
                }
            });

            discardChanges.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    dismiss();
                    finishActivity();
                }
            });

            cancel.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
    }
}