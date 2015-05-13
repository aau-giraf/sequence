package dk.aau.cs.giraf.sequence;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.inputmethod.EditorInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dk.aau.cs.giraf.activity.GirafActivity;
import dk.aau.cs.giraf.gui.GirafButton;
import dk.aau.cs.giraf.gui.GirafInflatableDialog;
import dk.aau.cs.giraf.gui.GirafNotifyDialog;
import dk.aau.cs.giraf.dblib.Helper;
import dk.aau.cs.giraf.dblib.models.Frame;
import dk.aau.cs.giraf.dblib.models.Profile;
import dk.aau.cs.giraf.sequence.PictogramView.OnDeleteClickListener;
import dk.aau.cs.giraf.sequence.SequenceAdapter.OnAdapterGetViewListener;
import dk.aau.cs.giraf.sequence.SequenceViewGroup.OnNewButtonClickedListener;
import dk.aau.cs.giraf.dblib.models.Sequence;
import dk.aau.cs.giraf.dblib.models.Pictogram;
import dk.aau.cs.giraf.showcaseview.ShowcaseManager;
import dk.aau.cs.giraf.showcaseview.ShowcaseView;
import dk.aau.cs.giraf.showcaseview.targets.ViewTarget;

@SuppressWarnings("FieldCanBeLocal")
// Suppress warnings to keep the similar variables and initializations together
public class AddEditSequencesActivity extends GirafActivity implements GirafNotifyDialog.Notification, GirafInflatableDialog.OnCustomViewCreatedListener, ShowcaseManager.ShowcaseCapable {

    private Profile guardian;
    private Profile selectedChild;

    private long childId;
    private long sequenceId;
    private long guardianId;
    private boolean isNew;
    private boolean isInEditMode;

    private int pictogramEditPos = -1;
    private long tempPictogramId;
    private boolean choiceListEdited = false;
    private boolean changesSaved = true;
    private boolean deleteNewCreatedSequence = false;
    public static boolean choiceMode = false;

    // Various tag
    private final String PICTO_INTENT_CHECKOUT_ID = "checkoutIds";
    private final String ADD_PICTOGRAM_OR_CHOICE_TAG = "ADD_PICTOGRAM_OR_CHOICE_TAG";
    private final String SAVE_SEQUENCE_TAG = "SAVE_SEQUENCE_TAG";
    private final String BACK_SEQUENCE_TAG = "BACK_SEQUENCE_TAG";
    private final String EDIT_CHOICE_TAG = "EDIT_CHOICE_TAG";
    private final String EMPTY_SEQUENCE_ERROR_TAG = "EMPTY_SEQUENCE_ERROR_TAG";
    private final String EMPTY_CHOICE_ERROR_TAG = "EMPTY_CHOICE_ERROR_TAG";
    private final String DELETE_SEQUENCES_TAG = "DELETE_SEQUENCES_TAG";
    private final int PICTO_EDIT_SEQUENCE_THUMBNAIL_CALL = 123;
    private final int PICTO_EDIT_PICTOGRAM_CALL = 234;
    private final int PICTO_NEW_PICTOGRAM_CALL = 345;
    private final int CHOICE_DIALOG = 456;
    private final int CHOICE_NEW_PICTOGRAM_CALL = 567;
    private final int CHOICE_EDIT_PICTOGRAM_CALL = 678;
    private final int ERROR_EMPTY_SEQUENCE = 789;
    private final int ERROR_EMPTY_CHOICE = 890;

    private Helper helper;
    private EditText sequenceName;
    private ShowcaseManager showcaseManager;
    private LinearLayout parent_container;
    private SequenceViewGroup sequenceViewGroup;

    public static Sequence sequence;
    public static final Sequence choice = new Sequence();
    private SequenceAdapter adapter;
    private SequenceAdapter choiceAdapter;
    private final List<Pictogram> tempPictogramList = new ArrayList<Pictogram>();

    private SequenceViewGroup choiceGroup;
    private SequenceViewGroup sequenceChoiceGroupTemplate;

    // Initialize buttons
    private GirafButton saveButton;
    private GirafButton deleteButton;
    private GirafButton helpButton;
    private GirafButton sequenceThumbnailButton;

    // Initialize dialogs
    private GirafInflatableDialog choosePictogramOrChoiceDialog;
    private GirafInflatableDialog backDialog;
    private GirafInflatableDialog saveDialog;
    private GirafInflatableDialog acceptDeleteDialog;
    private GirafInflatableDialog choiceDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_sequences);

        // Initialize XML components
        sequenceName = (EditText) findViewById(R.id.sequenceName);
        parent_container = (LinearLayout) findViewById(R.id.parent_container);
        sequenceViewGroup = (dk.aau.cs.giraf.sequence.SequenceViewGroup) findViewById(R.id.sequenceViewGroup);

                //Create helper to load data from Database
        helper = new Helper(this);

        loadIntents();
        loadProfiles();
        loadSequence();
        setupFramesGrid();
        setupButtons();
        setupActionBar();
        clearFocus();
    }

    /**
     * Loads the intents from the MainActivity
     */
    private void loadIntents() {
        Bundle extras = getIntent().getExtras();
        childId = extras.getLong("childId");
        sequenceId = extras.getLong("sequenceId");
        guardianId = extras.getLong("guardianId");
        isNew = extras.getBoolean("isNew");
        isInEditMode = extras.getBoolean("editMode");
    }

    /**
     * Using a helper, load the profiles from the database, based on intents
     */
    private void loadProfiles() {
        selectedChild = helper.profilesHelper.getById(childId);
        guardian = helper.profilesHelper.getById(guardianId);
    }

    /**
     * Loads the selected sequence.
     */
    private void loadSequence() {
        // If the sequence already exists, load that sequence
        if (!isNew) {
            sequence = helper.sequenceController.getSequenceAndFrames(sequenceId);
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

    /**
     * Create Adapter for the SequenceViewGroup (The Grid displaying the Sequence)
     */
    private void setupFramesGrid() {
        // Create Adapter for the SequenceViewGroup (The Grid displaying the Sequence)
        adapter = setupAdapter(sequence);
        setupSequenceViewGroup(adapter);
    }

    /**
     * Creates the buttons, and set onClickListeners
     */
    private void setupButtons() {
        deleteButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_delete));
        helpButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_help));
        saveButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_save));
        sequenceThumbnailButton = (GirafButton) findViewById(R.id.sequenceThumbnail);

        // Adding buttons to action-bar
        addGirafButtonToActionBar(deleteButton, LEFT);
        addGirafButtonToActionBar(helpButton, RIGHT);
        addGirafButtonToActionBar(saveButton, RIGHT);

        saveButton.setOnClickListener(new ImageButton.OnClickListener() {
            //Show Dialog to save Sequence when clicking the Save Button
            @Override
            public void onClick(View v) {
                checkSequenceBeforeSave(true);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Prevents the save unsaved changes from appearing when deleting a newly created sequence that has not yet been saved.
                deleteNewCreatedSequence = true;

                acceptDeleteDialog = GirafInflatableDialog.newInstance(
                        getApplicationContext().getString(R.string.delete_sequence),
                        getApplicationContext().getString(R.string.delete_this),
                        R.layout.dialog_delete);
                acceptDeleteDialog.show(getSupportFragmentManager(), DELETE_SEQUENCES_TAG);
            }
        });

        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleShowcase();
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
            Drawable d = new BitmapDrawable(getResources(), helper.pictogramHelper.getById(sequence.getPictogramId()).getImage());
            sequenceThumbnailButton.setIcon(d);
        }
    }

    /**
     * Add an appropriate title to the action bar
     */
    private void setupActionBar() {
        if (isNew) {
            this.setActionBarTitle(getResources().getString(R.string.new_sequence));
        } else {
            this.setActionBarTitle(getResources().getString(R.string.edit_sequence));
        }
    }

    /**
     * Removed focus away from the keybroad
     */
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

    /**
     * onClick method for accept delete click, if the delete button is clicked
     *
     * @param v The view that was clicked.
     */
    public void onConfirmDeleteClick(View v) {
        // This method is used, even though android says that it is not.
        acceptDeleteDialog.dismiss();
        helper.sequenceController.remove(sequenceId);
        onBackPressed();
    }

    /**
     * onClick method for cancel delete
     *
     * @param v The view that was clicked.
     */
    public void onCancelDeleteClick(View v) {
        acceptDeleteDialog.dismiss();
    }

    /**
     * Checks the sequence before it is saved.
     * <p>
     * Can result in an error dialog if sequence is empty. Otherwise, two different ways
     * to save: by clicking save or being reminded when returning, that changes has been made
     * </p>
     *
     * @param confirmation boolean value, if whether a "confirmed" dialog should appear
     */
    private void checkSequenceBeforeSave(boolean confirmation) {
        //Checks if Sequence is empty. If not empty, save it and return
        if (sequence.getFramesList().size() == 0) {
            createAndShowErrorDialogEmptySequence();
        } else if (confirmation) {
            saveChanges();
            createAndShowSaveDialog();
            changesSaved = true;
        } else {
            saveChanges();
            changesSaved = true;
        }
    }

    /**
     * Method for saving the changes made to the sequence.
     */
    private void saveChanges() {
        //Save name from sequenceName to the Sequence
        if (sequenceName.getEditableText() == null || sequenceName.getEditableText().length() == 0) {
            sequence.setName(getResources().getString(R.string.unnamed_sequence));
        } else {
            sequence.setName(sequenceName.getText().toString());
        }

        //Set PosX of every frame to save the order in which the frames should be shown.
        for (int i = 0; i < sequence.getFramesList().size(); i++) {
            sequence.getFramesList().get(i).setPosX(i);
        }

        //If Sequence is new, set relevant properties and insert into Database
        if (isNew) {
            sequence.setProfileId(selectedChild.getId());
            sequence.setSequenceType(Sequence.SequenceType.SEQUENCE);
            helper.sequenceController.insertSequenceAndFrames(sequence);
            isNew = false;
        }
        //If Sequence exists, modify in Database
        else {
            helper.sequenceController.modifySequenceAndFrames(sequence);
        }
        changesSaved = true;
    }

    /**
     * Shows a dialog used to add either a pictogram og choice to a pictogram
     */
    private void createAndShowAddDialog() {
        //Create instance of the add dialog and display it with the two choices: Add Pictogram or Add Choice
        choosePictogramOrChoiceDialog = GirafInflatableDialog.newInstance(this.getString(R.string.add_pictogram_choice), this.getString(R.string.add_pictogram_choice_description), R.layout.dialog_add_pictogram_or_choice);
        choosePictogramOrChoiceDialog.show(getSupportFragmentManager(), ADD_PICTOGRAM_OR_CHOICE_TAG);
    }

    /**
     * Shows a dialog, if the user clicks return, where changesSaved == false.
     */
    private void createAndShowBackDialog() {
        // Creates the "back dialog" if the back button is pressed. Only shows the dialog if there has been changes.
        if (!changesSaved && !deleteNewCreatedSequence) {
            backDialog = GirafInflatableDialog.newInstance(this.getString(R.string.back), this.getString(R.string.back_description), R.layout.dialog_back);
            backDialog.show(getSupportFragmentManager(), BACK_SEQUENCE_TAG);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Shows the choice dialog, when a choice element is clicked on
     */
    private void createAndShowChoiceDialog() {
        choiceMode = true;
        tempPictogramList.clear();

        // Creates the dialog
        choiceDialog = GirafInflatableDialog.newInstance(this.getString(R.string.choice), this.getString(R.string.choice_dialog_subtitle), R.layout.dialog_choice, CHOICE_DIALOG);
        choiceDialog.show(getSupportFragmentManager(), EDIT_CHOICE_TAG);
    }

    /**
     * Shows a dialog, if the save button i clicked
     */
    private void createAndShowSaveDialog() {
        //Create instance of the save dialog and display it
        saveDialog = GirafInflatableDialog.newInstance(this.getString(R.string.save), this.getString(R.string.sequence_saved), R.layout.dialog_save);
        saveDialog.show(getSupportFragmentManager(), SAVE_SEQUENCE_TAG);
    }

    /**
     * Calls the picto search application
     *
     * @param v The view that was clicked.
     */
    public void onPictogramClick(View v) {
        callPictoSearch(PICTO_NEW_PICTOGRAM_CALL);
        choosePictogramOrChoiceDialog.dismiss();
    }

    /**
     * Creates an empty choice dialog
     *
     * @param v The view that was clicked
     */
    public void onChoiceClick(View v) {
        createAndShowChoiceDialog();
        choosePictogramOrChoiceDialog.dismiss();
    }

    /**
     * Click method, after the back dialog is open, and the save button is clicked
     *
     * @param v The view that was clicked
     */
    public void onBackSaveButton(View v) {
        checkSequenceBeforeSave(false);
        backDialog.dismiss();
        if (changesSaved) {
            super.onBackPressed();
        }
    }

    /**
     * Click method, after the back dialog is open, and the cancel button is clicked
     *
     * @param v The view that was clicked
     */
    public void onBackNoSaveButton(View v) {
        backDialog.dismiss();
        super.onBackPressed();
    }

    /**
     * Click method, after the back dialog is open, and the save button is clicked
     *
     * @param v The view that was clicked
     */
    public void onBackCancelSaveButton(View v) {
        backDialog.dismiss();
    }

    /**
     * Creates the choice dialog and fills it with the elements
     */
    private void setupChoiceDialog() {
        if (!choiceListEdited) {
            choice.getFramesList().clear();
            if (pictogramEditPos != -1) {
                for (int i = 0; i < adapter.getItem(pictogramEditPos).getPictogramList().size(); i++) {
                    Frame frame = new Frame();
                    frame.setPictogramId(adapter.getItem(pictogramEditPos).getPictogramList().get(i).getId());
                    choice.addFrame(frame);
                }
            }
        }

        tempPictogramList.clear();
        for (int i = 0; i < choice.getFramesList().size(); i++) {
            Pictogram pictogram = new Pictogram();
            pictogram.setId(choice.getFramesList().get(i).getPictogramId());
            tempPictogramList.add(pictogram);
        }

        // Creates and set the adapter to display a list of pictograms in the choice dialog
        choiceAdapter = setupAdapter(choice);
        setupChoiceGroup(choiceAdapter);
    }

    /**
     * Method to be called, when the save button is clicked in a choice dialog
     *
     * @param v The view that was clicked
     */
    public void choiceSaveClick(View v) {
        if (!choiceListEdited) {
            choice.getFramesList().clear();
            if (pictogramEditPos != -1) {
                for (int i = 0; i < adapter.getItem(pictogramEditPos).getPictogramList().size(); i++) {
                    Frame frame = new Frame();
                    frame.setPictogramId(adapter.getItem(pictogramEditPos).getPictogramList().get(i).getId());
                    choice.addFrame(frame);
                }
            }
        }
        choiceListEdited = false;

        tempPictogramList.clear();
        for (int i = 0; i < choice.getFramesList().size(); i++) {
            Pictogram pictogram = new Pictogram();
            pictogram.setId(choice.getFramesList().get(i).getPictogramId());
            tempPictogramList.add(pictogram);
        }

        Frame frame = new Frame();
        if (tempPictogramList.size() == 0) {
            createAndShowErrorDialogEmptyChoice();
            return;
        }

        frame.setPictogramList(tempPictogramList);
        // By setting the pictogramId to 0, the sequenceAdapter ensures that the "choose icon" is used for a choice
        frame.setPictogramId(0);

        if (pictogramEditPos == -1) {
            sequence.addFrame(frame);
            pictogramEditPos = sequence.getFramesList().size() - 1;
        } else {
            sequence.getFramesList().get(pictogramEditPos).setPictogramList(tempPictogramList);
        }

        adapter.notifyDataSetChanged();
        choiceAdapter.notifyDataSetChanged();
        choiceMode = false;
        pictogramEditPos = -1;
        choiceDialog.dismiss();
    }

    /**
     * Cancel the save of a changes to a choice
     *
     * @param v The view that was clicked
     */
    public void choiceCancelClick(View v) {
        choiceMode = false;
        choiceListEdited = false;
        pictogramEditPos = -1;
        choiceDialog.dismiss();
    }

    /**
     * Sets up the SequenceViewGroup, for a choice, with features such as click, add "+", and rearrange
     *
     * @param adapter The adapter that should be set to the SequenceViewGroup
     * @return The SequenceViewGroup, with attached adapter and features
     */
    private SequenceViewGroup setupChoiceGroup(final SequenceAdapter adapter) {
        choiceGroup.setEditModeEnabled(isInEditMode);
        choiceGroup.setAdapter(adapter);

        // Handle rearrange
        choiceGroup.setOnRearrangeListener(new SequenceViewGroup.OnRearrangeListener() {
            @Override
            public void onRearrange(int indexFrom, int indexTo) {
                adapter.notifyDataSetChanged();
                changesSaved = false;
            }
        });

        // Handle new pictogram added to the view (Clicking the big "+")
        choiceGroup.setOnNewButtonClickedListener(new OnNewButtonClickedListener() {
            @Override
            public void onNewButtonClicked() {
                SequenceViewGroup sequenceGroup = sequenceChoiceGroupTemplate;
                sequenceGroup.liftUpAddNewButton();
                callPictoSearch(CHOICE_NEW_PICTOGRAM_CALL);
                adapter.notifyDataSetChanged();
            }
        });

        // Handle pictogram edit inside a the choice dialog, if a clicking on a pictogram
        choiceGroup.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view,
                                    int position, long id) {
                pictogramEditPos = position;
                choiceMode = true;
                tempPictogramId = choice.getFramesList().get(pictogramEditPos).getPictogramId();
                callPictoSearch(CHOICE_EDIT_PICTOGRAM_CALL);
            }
        });

        return choiceGroup;
    }

    /**
     * On click attached to the saved button in the save dialog
     *
     * @param v The view that was clicked
     */
    public void savedClick(View v) {
        saveDialog.dismiss();
    }

    /**
     * Shows an error dialog, if the user tried to save an empty sequence
     */
    private void createAndShowErrorDialogEmptySequence() {
        //Creates alertDialog to display error. Clicking Ok dismisses the Dialog
        GirafNotifyDialog alertDialog = GirafNotifyDialog.newInstance(this.getString(R.string.error), this.getString(R.string.empty_sequence_error), ERROR_EMPTY_SEQUENCE);
        alertDialog.show(getSupportFragmentManager(), EMPTY_SEQUENCE_ERROR_TAG);
    }

    /**
     * Shows an error dialog, if the user tries to save an empty choice
     */
    private void createAndShowErrorDialogEmptyChoice() {
        //Creates alertDialog to display error. Clicking Ok dismisses the Dialog
        GirafNotifyDialog alertDialog = GirafNotifyDialog.newInstance(this.getString(R.string.error), this.getString(R.string.empty_choice_error), ERROR_EMPTY_CHOICE);
        alertDialog.show(getSupportFragmentManager(), EMPTY_CHOICE_ERROR_TAG);
    }

    /**
     * Sets up the SequenceViewGroup, for a the sequence, with features
     * <p>
     * The SequenceViewGroup class takes care of most of the required functionality,
     * including size properties, dragging and rearranging
     * </p>
     *
     * @param adapter The adapter that should be attached to the SequenceViewGroup
     * @return The SequenceViewGroup, with attached adapter and features
     */
    private SequenceViewGroup setupSequenceViewGroup(final SequenceAdapter adapter) {
        //Initialize the view and set up adapter to display the Sequence
        final SequenceViewGroup sequenceGroup = (SequenceViewGroup) findViewById(R.id.sequenceViewGroup);
        sequenceGroup.setEditModeEnabled(isInEditMode);
        sequenceGroup.setAdapter(adapter);

        //When clicking the big "+", lift up the view and show the Add Dialog
        sequenceGroup.setOnNewButtonClickedListener(new OnNewButtonClickedListener() {
            @Override
            public void onNewButtonClicked() {
                final SequenceViewGroup sequenceGroup = (SequenceViewGroup) findViewById(R.id.sequenceViewGroup);
                sequenceGroup.liftUpAddNewButton();
                createAndShowAddDialog();
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
                if (frame.getPictogramList().size() > 0) {
                    createAndShowChoiceDialog();
                } else {
                    callPictoSearch(PICTO_EDIT_PICTOGRAM_CALL);
                }
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

    /**
     * onActivityResult handles what to do, when starting another activity and waiting for the result
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult(), allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity through its setResult().
     * @param data        An Intent, which can return result data to the caller: data here is the returned ID's from picto search
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //If Activity Result is OK, call relevant method depending on the RequestCode used to launch Activity with
        if (resultCode == RESULT_OK) {
            switch (requestCode) {

                case PICTO_NEW_PICTOGRAM_CALL:
                    // Remove the highlighting from the add pictogram button
                    final SequenceViewGroup sequenceGroup = (SequenceViewGroup) findViewById(R.id.sequenceViewGroup);
                    sequenceGroup.placeDownAddNewButton();
                    onNewPictogramResult(data);
                    break;

                case CHOICE_NEW_PICTOGRAM_CALL:
                    //final SequenceViewGroup choiceGroup = sequenceChoiceGroupTemplate;
                    choiceGroup.placeDownAddNewButton();
                    onNewPictogramResult(data);
                    break;

                case PICTO_EDIT_PICTOGRAM_CALL:
                    onEditPictogramResult(data);
                    break;

                case CHOICE_EDIT_PICTOGRAM_CALL:
                    onEditPictogramResult(data);
                    break;

                case PICTO_EDIT_SEQUENCE_THUMBNAIL_CALL:
                    onEditSequenceThumbnailResult(data);
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * If picto search was started with the *_NEW_PICTOGRAM_CALL, run this method
     *
     * @param data Data returned from the picto search activity: ID's of chosen pictograms
     */
    private void onNewPictogramResult(Intent data) {
        long[] checkoutIds = data.getExtras().getLongArray(PICTO_INTENT_CHECKOUT_ID);

        // If no pictograms are returned (Assume user cancelled and nothing is supposed to change)
        if (checkoutIds.length == 0) {
            return;
        }
        if (choiceMode) {
            for (long id : checkoutIds) {
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
            choiceListEdited = true;
            choiceAdapter.notifyDataSetChanged();
            adapter.notifyDataSetChanged();
            changesSaved = false;
        } else {
            for (long id : checkoutIds) {
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

    /**
     * If picto search was started with the *_EDIT_PICTOGRAM_CALL, run this method
     *
     * @param data Data returned from the picto search activity: ID's of chosen pictograms
     */
    private void onEditPictogramResult(Intent data) {
        long[] checkoutIds = data.getExtras().getLongArray(PICTO_INTENT_CHECKOUT_ID);

        // If no pictograms are returned (Assume user cancelled and nothing is supposed to change)
        if (checkoutIds.length == 0) {
            return;
        }
        if (choiceMode) {
            for (int i = 0; i < choice.getFramesList().size(); i++) {
                if (tempPictogramId == choice.getFramesList().get(i).getPictogramId()) {
                    choice.getFramesList().get(i).setPictogramId(checkoutIds[0]);
                }
            }
            choiceListEdited = true;
            choiceAdapter.notifyDataSetChanged();
            adapter.notifyDataSetChanged();
            changesSaved = false;
        } else {
            Frame frame = sequence.getFramesList().get(pictogramEditPos);
            frame.setPictogramId(checkoutIds[0]);
            adapter.notifyDataSetChanged();
            changesSaved = false;
        }
    }

    /**
     * If picto search was started with the PICTO_EDIT_SEQUENCE_THUMBNAIL_CALL, run this method
     *
     * @param data Data returned from the picto search activity: ID's of chosen pictograms
     */
    private void onEditSequenceThumbnailResult(Intent data) {
        long[] checkoutIds = data.getExtras().getLongArray(PICTO_INTENT_CHECKOUT_ID);

        if (checkoutIds.length == 0) {
            return;
        }

        sequence.setPictogramId(checkoutIds[0]);
        Drawable d = new BitmapDrawable(getResources(), helper.pictogramHelper.getById(sequence.getPictogramId()).getImage());
        sequenceThumbnailButton.setIcon(d);
        changesSaved = false;
    }

    /**
     * Sends an intent to start the picto search activity, and returns the result
     *
     * @param modeId The tag associated to the different calls, e.g. PICTO_NEW_PICTOGRAM_CALL
     */
    private void callPictoSearch(int modeId) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("dk.aau.cs.giraf.pictosearch", "dk.aau.cs.giraf.pictosearch.PictoAdminMain"));
        intent.putExtra("currentChildID", selectedChild.getId());
        intent.putExtra("currentGuardianID", guardian.getId());

        if (modeId == PICTO_NEW_PICTOGRAM_CALL || modeId == CHOICE_NEW_PICTOGRAM_CALL) {
            intent.putExtra("purpose", "multi");
        } else {
            intent.putExtra("purpose", "single");
        }
        startActivityForResult(intent, modeId);
    }

    /**
     * Sets up the adapter, that should be attached to a SequenceViewGroup
     *
     * @param seq The sequence, that the adapter should contain
     * @return The newly created adapter
     */
    private SequenceAdapter setupAdapter(final Sequence seq) {
        final SequenceAdapter adapter = new SequenceAdapter(this, seq);

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
                            if (choiceMode) {
                                choiceListEdited = true;
                                choice.getFramesList().remove(position);
                                choiceAdapter.notifyDataSetChanged();
                            } else {
                                seq.getFramesList().remove(position);
                            }
                            changesSaved = false;
                            adapter.notifyDataSetChanged();

                        }
                    });
                }
            }
        });
        return adapter;
    }

    /**
     * Showcase is used to highlight buttons when using the help button
     */
    @Override
    public synchronized void showShowcase() {

        // Targets for the Showcase
        final ViewTarget saveSequence = new ViewTarget(saveButton, 1.5f);
        final ViewTarget deleteSequence = new ViewTarget(deleteButton, 1.5f);
        final ViewTarget editSequenceThumbnail = new ViewTarget(sequenceThumbnailButton, 1.5f);
        final ViewTarget editSequenceName = new ViewTarget(sequenceName, 0.6f);
        final ViewTarget addPictograms = new ViewTarget(sequenceViewGroup.getChildAt(sequence.getFramesList().size()), 1.35f);

        // Create a relative location for the next button
        final RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        final int margin = ((Number) (getResources().getDisplayMetrics().density * 12)).intValue();
        lps.setMargins(margin, margin, margin, margin);

        // Calculate position for the help text
        final int textX = getResources().getDisplayMetrics().widthPixels / 2 + margin;
        final int textY = getResources().getDisplayMetrics().heightPixels / 2 + margin;
        final int textYHigh = getResources().getDisplayMetrics().heightPixels / 6 + margin;

        showcaseManager = new ShowcaseManager();

        showcaseManager.addShowCase(new ShowcaseManager.Showcase() {
            @Override
            public void configShowCaseView(final ShowcaseView showcaseView) {
                showcaseView.setShowcase(saveSequence, true);
                showcaseView.setContentTitle(getString(R.string.sc_save_sequence));
                showcaseView.setContentText(getString(R.string.sc_save_sequence_text));
                showcaseView.setStyle(R.style.GirafCustomShowcaseTheme);
                showcaseView.setButtonPosition(lps);
                showcaseView.setTextPostion(textX, textY);
            }
        });

        showcaseManager.addShowCase(new ShowcaseManager.Showcase() {
            @Override
            public void configShowCaseView(final ShowcaseView showcaseView) {
                showcaseView.setShowcase(deleteSequence, true);
                showcaseView.setContentTitle(getString(R.string.sc_delete_sequence));
                showcaseView.setContentText(getString(R.string.sc_delete_sequence_text_add_edit));
                showcaseView.setStyle(R.style.GirafCustomShowcaseTheme);
                showcaseView.setButtonPosition(lps);
                showcaseView.setTextPostion(textX, textY);
            }
        });

        showcaseManager.addShowCase(new ShowcaseManager.Showcase() {
            @Override
            public void configShowCaseView(final ShowcaseView showcaseView) {
                showcaseView.setShowcase(editSequenceThumbnail, true);
                showcaseView.setContentTitle(getString(R.string.sc_edit_thumbnail));
                showcaseView.setContentText(getString(R.string.sc_edit_thumbnail_text));
                showcaseView.setStyle(R.style.GirafCustomShowcaseTheme);
                showcaseView.setButtonPosition(lps);
                showcaseView.setTextPostion(textX, textY);
            }
        });

        showcaseManager.addShowCase(new ShowcaseManager.Showcase() {
            @Override
            public void configShowCaseView(final ShowcaseView showcaseView) {
                showcaseView.setShowcase(editSequenceName, true);
                showcaseView.setContentTitle(getString(R.string.sc_edit_sequence_name));
                showcaseView.setContentText(getString(R.string.sc_edit_sequence_name_text));
                showcaseView.setStyle(R.style.GirafCustomShowcaseTheme);
                showcaseView.setButtonPosition(lps);
                showcaseView.setTextPostion(textX, textY);
            }
        });

        showcaseManager.addShowCase(new ShowcaseManager.Showcase() {
            @Override
            public void configShowCaseView(final ShowcaseView showcaseView) {
                showcaseView.setShowcase(addPictograms, true);
                showcaseView.setContentTitle(getString(R.string.sc_add_pictograms));
                showcaseView.setContentText(getString(R.string.sc_add_pictograms_text));
                showcaseView.setStyle(R.style.GirafCustomShowcaseTheme);
                showcaseView.setButtonPosition(lps);
                showcaseView.setTextPostion(textX, textYHigh);
            }
        });

        showcaseManager.setOnDoneListener(new ShowcaseManager.OnDoneListener() {
            @Override
            public void onDone(ShowcaseView showcaseView) {
                showcaseManager = null;
            }
        });

        showcaseManager.start(this);
    }

    /**
     * Hide the showcasing by stopping it
     */
    @Override
    public synchronized void hideShowcase() {

        if (showcaseManager != null) {
            showcaseManager.stop();
            showcaseManager = null;
        }
    }

    /**
     * Toggles the showcase to either show or hide it
     */
    @Override
    public synchronized void toggleShowcase() {

        if (showcaseManager != null) {
            hideShowcase();
        } else {
            showShowcase();
        }
    }

    /**
     * Override the onBackPressed, as the back dialog should pop up, if changes were made
     */
    @Override
    public void onBackPressed() {
        createAndShowBackDialog();
    }

    /**
     * Occurs when moving to a new activity from this activity
     */
    @Override
    public void onPause() {
        super.onPause();

        if (showcaseManager != null) {
            showcaseManager.stop();
        }
    }

    /**
     * Will be called whenever a notification dialog is handled
     *
     * @param id The method id (Response code)
     */
    @Override
    public void noticeDialog(int id) {
        switch (id) {
            case ERROR_EMPTY_SEQUENCE:
                break;

            case ERROR_EMPTY_CHOICE:
                break;

            default:
                break;
        }
    }

    /**
     * Will be called whenever a custom dialog box requests content (ie when the choice dialog is opened)
     *
     * @param viewGroup Is the current view
     * @param id        The method id (Response code)
     */
    @Override
    public void editCustomView(ViewGroup viewGroup, int id) {
        // The choice dialog used a view that is updated according to the sequence. 
        if (id == CHOICE_DIALOG) {
            sequenceChoiceGroupTemplate = (SequenceViewGroup) viewGroup.findViewById(R.id.choice_view_group);
            choiceGroup = sequenceChoiceGroupTemplate;
            // Sets up the dialog, now that the resources are properly found.
            setupChoiceDialog();
        }
    }
}