package dk.aau.cs.giraf.zebra;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.inputmethod.EditorInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dk.aau.cs.giraf.activity.GirafActivity;
import dk.aau.cs.giraf.gui.GButton;
import dk.aau.cs.giraf.gui.GDialog;
import dk.aau.cs.giraf.gui.GirafButton;
import dk.aau.cs.giraf.gui.GirafInflatableDialog;
import dk.aau.cs.giraf.gui.GirafNotifyDialog;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Frame;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.zebra.PictogramView.OnDeleteClickListener;
import dk.aau.cs.giraf.zebra.SequenceAdapter.OnAdapterGetViewListener;
import dk.aau.cs.giraf.zebra.SequenceViewGroup.OnNewButtonClickedListener;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;
import dk.aau.cs.giraf.oasis.lib.models.Pictogram;

public class AddEditSequencesActivity extends GirafActivity implements GirafNotifyDialog.Notification {

    private Profile guardian;
    private Profile selectedChild;
    private boolean isInEditMode;
    private boolean isNew;
    private boolean changesSaved = true;
    private String sequenceStartName;

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
    private final int PICTO_EDIT_SEQUENCE_THUMBNAIL_CALL = 345;
    private final int PICTO_EDIT_PICTOGRAM_CALL = 456;
    private final int PICTO_NEW_PICTOGRAM_CALL = 567;
    private final String ADD_PICTOGRAM_OR_CHOICE = "ADD_PICTOGRAM_OR_CHOICE";
    private final String SAVE_SEQUENCE = "SAVE_SEQUENCE";
    private final String BACK_SEQUENCE = "BACK_SEQUENCE";
    private final int EMPTY_SEQUENCE_ERROR = 1338;
    private final String EMPTY_SEQUENCE_ERROR_TAG = "EMPTY_SEQUENCE_ERROR_TAG";
    private final String CHOICE_SEQUENCE = "CHOICE_SEQUENCE";
    private final int SEQUENCE_VIEWER_CALL = 1337;
    private final int NESTED_SEQUENCE_CALL = 40;
    private Helper helper;
    private EditText sequenceName;
    private LinearLayout parent_container;
    private final String DELETE_SEQUENCES = "DELETE_SEQUENCES";

    // Initialize buttons
    private GirafButton saveButton;
    private GirafButton deleteButton;
    private GirafButton sequenceThumbnailButton;

    GirafInflatableDialog choosePictogramOrChoiceDialog;
    GirafInflatableDialog backDialog;
    GirafInflatableDialog saveDialog;
    GirafInflatableDialog acceptDeleteDialog;
    GirafInflatableDialog choiceDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_sequences);

        // Initialize XML components
        sequenceName = (EditText) findViewById(R.id.sequenceName);
        parent_container = (LinearLayout) findViewById(R.id.parent_container);

        loadIntents();
        loadProfiles();
        loadSequence();
        setupFramesGrid();
        setupButtons();
        setupActionBar();
        clearFocus();
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
            // Set the name of the sequence to previously written name
            sequenceName.setText(sequence.getName(), EditText.BufferType.EDITABLE);

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

        // Create buttons
        saveButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_save));
        deleteButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_delete));
        sequenceThumbnailButton = (GirafButton) findViewById(R.id.sequenceThumbnail);

        // Adding buttons to action-bar
        addGirafButtonToActionBar(saveButton, LEFT);
        addGirafButtonToActionBar(deleteButton, RIGHT);

        saveButton.setOnClickListener(new ImageButton.OnClickListener() {
            //Show Dialog to save Sequence when clicking the Save Button
            @Override
            public void onClick(View v) {
                checkSequenceBeforeSave(v, true);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptDeleteDialog = GirafInflatableDialog.newInstance(
                        getApplicationContext().getString(R.string.delete_sequences),
                        getApplicationContext().getString(R.string.delete_this) + " "
                            + getApplicationContext().getString(R.string.sequence),
                        R.layout.dialog_delete);
                acceptDeleteDialog.show(getSupportFragmentManager(), DELETE_SEQUENCES);
            }
        });

        sequenceThumbnailButton.setOnClickListener(new ImageView.OnClickListener() {
            //If Sequence Image Button is clicked, call PictoAdmin to select an Image for the Sequence
            @Override
            public void onClick(View v) {
                if (isInEditMode) {
                    callPictoSearch(PICTO_EDIT_SEQUENCE_THUMBNAIL_CALL);
                }
            }
        });

        //If no Image has been selected or the Sequence, display the Add Sequence Picture. Otherwise load the image for the Button
        if (sequence.getPictogramId() != 0) {
            helper = new Helper(this);
            Drawable d = new BitmapDrawable(getResources(), helper.pictogramHelper.getPictogramById(sequence.getPictogramId()).getImage());
            sequenceThumbnailButton.setIcon(d);
        }
    }

    // Button to accept delete of sequences
    public void deleteClick(View v) {
        acceptDeleteDialog.dismiss();
        // Delete all selected items
        helper = new Helper(getApplicationContext());
        helper.sequenceController.removeSequence(sequenceId);
        onBackPressed();
    }

    // Button to cancel delete of sequences
    public void dontDeleteClick(View v) {
        acceptDeleteDialog.dismiss();
    }

    private void setupActionBar() {
        //Creates helper to fetch data from the Database
        helper = new Helper(this);

        //Save Child locally and update relevant information for application
        selectedChild = helper.profilesHelper.getProfileById(childId);
        this.setActionBarTitle(selectedChild.getName()); // selectedChild.getName() "Child's name code"
    }

    private void clearFocus() {
        sequenceName.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager hideKeyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    sequenceName.clearFocus();
                    parent_container.requestFocus();
                    hideKeyboard.hideSoftInputFromWindow(sequenceName.getWindowToken(), 0);
                }
                return false;
            }
        });
    }

    private boolean checkSequenceBeforeSave(View v, boolean confirmation) {
        //Checks if Sequence is empty. If not empty, save it and return
        if (sequence.getFramesList().size() == 0) {
            createAndShowErrorDialog(v);
            return false;
        } else if (confirmation == true) {
            saveChanges();
            createAndShowSaveDialog(v);
            changesSaved = true;
            return true;
        } else {
            saveChanges();
            changesSaved = true;
            return true;
        }
    }

    private void saveChanges() {
        //Create helper to use Database Helpers
        helper = new Helper(this);

        //Save name from sequenceName to the Sequence
        if (sequenceName.getEditableText() == null || sequenceName.getEditableText().length() == 0) {
            sequence.setName(getResources().getString(R.string.unnamed_sequence));
        } else {
            sequence.setName(sequenceName.getText().toString());
            sequenceStartName = sequenceName.getText().toString();
        }

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
        changesSaved = true;
    }

    // creates the "add pictogram or choice" view
    // The following two methods is connected to girafbuttons in the xml file
    private void createAndShowAddDialog(View v) {
        //Create instance of AddDialog and display it
        choosePictogramOrChoiceDialog = GirafInflatableDialog.newInstance(this.getString(R.string.add_pictogram_choice), this.getString(R.string.add_pictogram_choice_description), R.layout.dialog_add_pictogram_or_choice);
        choosePictogramOrChoiceDialog.show(getSupportFragmentManager(), ADD_PICTOGRAM_OR_CHOICE);
    }

    // Button to search for pictograms
    public void getPictogramClick(View v) {
        callPictoSearch(PICTO_NEW_PICTOGRAM_CALL);
        choosePictogramOrChoiceDialog.dismiss();
    }

    // Button to search for pictograms, that should be used in a "choice" activity
    public void getChoiceClick(View v) {
        choiceMode = true;
        createAndShowChoiceDialog(v);
        choosePictogramOrChoiceDialog.dismiss();
    }

    // creates the "back dialog" if the backbutton is pressed. Only shows the dialog if there has been changes.
    // The following two methods is connected to girafbuttons in the view
    private void createAndShowBackDialog(View v) {
        //Create instance of AddDialog and display it
        if (changesSaved == false) {
            backDialog = GirafInflatableDialog.newInstance(this.getString(R.string.back), this.getString(R.string.back_description), R.layout.dialog_back);
            backDialog.show(getSupportFragmentManager(), BACK_SEQUENCE);
        } else {
            super.onBackPressed();
        }
    }

    // Button to search for pictograms
    public void backSaveClick(View v) {
        checkSequenceBeforeSave(v, false);
        backDialog.dismiss();
        if (changesSaved == true) {
            super.onBackPressed();
        }
    }

    // Button to search for pictograms, that should be used in a "choice" activity
    public void backDontSaveClick(View v) {
        backDialog.dismiss();
        super.onBackPressed();
    }

    // creates the "save dialog", when the save button is clicked.
    // The following two methods is connected to girafbuttons in the view
    private void createAndShowSaveDialog(View v) {
        //Create instance of AddDialog and display it
        saveDialog = GirafInflatableDialog.newInstance(this.getString(R.string.save), this.getString(R.string.sequence_saved), R.layout.dialog_save);
        saveDialog.show(getSupportFragmentManager(), SAVE_SEQUENCE);
    }

    // Button to search for pictograms
    public void savedClick(View v) {
        saveDialog.dismiss();
    }

    private void createAndShowErrorDialog(View v) {
        //Creates alertDialog to display error. Clicking Ok dismisses the Dialog
        GirafNotifyDialog alertDialog = GirafNotifyDialog.newInstance(this.getString(R.string.error), this.getString(R.string.empty_sequence_error), EMPTY_SEQUENCE_ERROR);
        alertDialog.show(getSupportFragmentManager(), EMPTY_SEQUENCE_ERROR_TAG);
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
                changesSaved = false;
            }
        });

        return sequenceGroup;
    }

    private SequenceAdapter setupAdapter() {
        //Sets up the adapter for the Sequence to display
        final SequenceAdapter adapter = new SequenceAdapter(this, sequence);

        //Adds a Delete & Edit Icon to all Frames which deletes or edits the relevant Frame on click.
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

                case PICTO_EDIT_SEQUENCE_THUMBNAIL_CALL:
                    OnEditSequenceThumbnailResult(data);
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
        int[] checkoutIds = data.getExtras().getIntArray(PICTO_INTENT_CHECKOUT_ID);

        //If no pictures are returned, assume user cancelled and nothing is supposed to change.
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
            changesSaved = false;
        } else {

            for (int id : checkoutIds) {
                Frame frame = new Frame();
                frame.setPictogramId(id);
                sequence.addFrame(frame);
            }

            if (sequence.getPictogramId() == 0 && checkoutIds.length > 0) {
                sequence.setPictogramId(checkoutIds[0]);
            }
            adapter.notifyDataSetChanged();
            changesSaved = false;
        }
    }

    private void OnEditPictogramResult(Intent data) {
        if (pictogramEditPos < 0) {
            return;
        }

        int[] checkoutIds = data.getExtras().getIntArray(PICTO_INTENT_CHECKOUT_ID);

        if (checkoutIds.length == 0) {
            return;
        }

        Frame frame = sequence.getFramesList().get(pictogramEditPos);
        frame.setPictogramId(checkoutIds[0]);
        adapter.notifyDataSetChanged();
        changesSaved = false;
    }

    private void OnEditSequenceThumbnailResult(Intent data) {
        int[] checkoutIds = data.getExtras().getIntArray(PICTO_INTENT_CHECKOUT_ID);

        if (checkoutIds.length == 0) {
            return;
        }

        sequence.setPictogramId(checkoutIds[0]);
        Drawable d = new BitmapDrawable(getResources(), helper.pictogramHelper.getPictogramById(sequence.getPictogramId()).getImage());
        sequenceThumbnailButton.setIcon(d);
        changesSaved = false;
    }

    // send an intent to start pictosearch, and returns the result from pictosearch
    private void callPictoSearch(int modeId) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("dk.aau.cs.giraf.pictosearch", "dk.aau.cs.giraf.pictosearch.PictoAdminMain"));
        intent.putExtra("currentChildID", selectedChild.getId());
        intent.putExtra("currentGuardianID", guardian.getId());

        if (modeId == PICTO_NEW_PICTOGRAM_CALL) {
            intent.putExtra("purpose", "multi");
        } else {
            intent.putExtra("purpose", "single");
        }
        startActivityForResult(intent, modeId);
    }

    private void createAndShowChoiceDialog(View v) {
        //Create instance of ChoiceDialog and display it
        ChoiceDialog choiceDialog = new ChoiceDialog(v.getContext());
        choiceDialog.show();
    }

    private void checkFrameMode(Frame frame, View v) {

        if (frame.getPictogramList().size() > 0) {
            createAndShowChoiceDialog(v);
        } else {
            callPictoSearch(PICTO_EDIT_PICTOGRAM_CALL);
        }
    }

    @Override
    public void noticeDialog(int i) {
        switch (i) {
            case EMPTY_SEQUENCE_ERROR:

                break;

            default:
                break;
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

            this.SetView(LayoutInflater.from(this.getContext()).inflate(R.layout.dialog_choice, null));

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

    @Override
    public void onBackPressed() {
        createAndShowBackDialog(null);
    }
}