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
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Frame;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.sequence.PictogramView.OnDeleteClickListener;
import dk.aau.cs.giraf.sequence.SequenceAdapter.OnAdapterGetViewListener;
import dk.aau.cs.giraf.sequence.SequenceViewGroup.OnNewButtonClickedListener;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;
import dk.aau.cs.giraf.oasis.lib.models.Pictogram;

@SuppressWarnings("FieldCanBeLocal") // Suppress warnings to keep the similar variables and initializations together
public class AddEditSequencesActivity extends GirafActivity implements GirafNotifyDialog.Notification, GirafInflatableDialog.OnCustomViewCreatedListener {

    private Profile guardian;
    private Profile selectedChild;

    private int childId;
    private int sequenceId;
    private int guardianId;
    private boolean isNew;
    private boolean isInEditMode;

    private int pictogramEditPos = -1;
    private boolean changesSaved = true;
    public static boolean choiceMode = false;
    private boolean choiceListEdited = false;
    private int tempPictogramId;

    // Various tag
    private final String PICTO_INTENT_CHECKOUT_ID = "checkoutIds";
    private final String ADD_PICTOGRAM_OR_CHOICE_TAG = "ADD_PICTOGRAM_OR_CHOICE_TAG";
    private final String SAVE_SEQUENCE_TAG = "SAVE_SEQUENCE_TAG";
    private final String BACK_SEQUENCE_TAG = "BACK_SEQUENCE_TAG";
    private final String EDIT_CHOICE_TAG = "EDIT_CHOICE_TAG";
    private final String EMPTY_SEQUENCE_ERROR_TAG = "EMPTY_SEQUENCE_ERROR_TAG";
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
    private LinearLayout parent_container;

    public static Sequence sequence;
    public static Sequence choice = new Sequence();
    public SequenceAdapter adapter;
    public SequenceAdapter choiceAdapter;
    private List<Pictogram> tempPictogramList = new ArrayList<Pictogram>();

    private SequenceViewGroup choiceGroup;
    private SequenceViewGroup sequenceChoiceGroupTemplate;

    // Initialize buttons
    private GirafButton saveButton;
    private GirafButton deleteButton;
    private GirafButton sequenceThumbnailButton;

    // Initialize dialogs
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
        //helper = new Helper(this);

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
        adapter = setupAdapter(sequence);
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
                checkSequenceBeforeSave(true);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptDeleteDialog = GirafInflatableDialog.newInstance(
                        getApplicationContext().getString(R.string.delete_sequence),
                        getApplicationContext().getString(R.string.delete_this),
                        R.layout.dialog_delete);
                acceptDeleteDialog.show(getSupportFragmentManager(), DELETE_SEQUENCES_TAG);
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
    public void cancelDeleteClick(View v) {
        acceptDeleteDialog.dismiss();
    }

    private void setupActionBar() {
        //Creates helper to fetch data from the Database
        helper = new Helper(this);

        //Save Child locally and update relevant information for application
        selectedChild = helper.profilesHelper.getProfileById(childId);
        if (isNew) {
            this.setActionBarTitle(getResources().getString(R.string.new_sequence));
        } else {
            this.setActionBarTitle(getResources().getString(R.string.edit_sequence));
        }

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

    private boolean checkSequenceBeforeSave(boolean confirmation) {
        //Checks if Sequence is empty. If not empty, save it and return
        if (sequence.getFramesList().size() == 0) {
            createAndShowErrorDialogEmptySequence();
            return false;
        } else if (confirmation) {
            saveChanges();
            createAndShowSaveDialog();
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
    private void createAndShowAddDialog() {
        //Create instance of AddDialog and display it
        choosePictogramOrChoiceDialog = GirafInflatableDialog.newInstance(this.getString(R.string.add_pictogram_choice), this.getString(R.string.add_pictogram_choice_description), R.layout.dialog_add_pictogram_or_choice);
        choosePictogramOrChoiceDialog.show(getSupportFragmentManager(), ADD_PICTOGRAM_OR_CHOICE_TAG);
    }

    // Button to search for pictograms
    public void getPictogramClick(View v) {
        callPictoSearch(PICTO_NEW_PICTOGRAM_CALL);
        choosePictogramOrChoiceDialog.dismiss();
    }

    // Button to search for pictograms, that should be used in a "choice" activity
    public void getChoiceClick(View v) {
        createAndShowChoiceDialog();
        choosePictogramOrChoiceDialog.dismiss();
    }

    // creates the "back dialog" if the backbutton is pressed. Only shows the dialog if there has been changes.
    // The following two methods is connected to girafbuttons in the view
    private void createAndShowBackDialog() {
        //Create instance of AddDialog and display it
        if (!changesSaved) {
            backDialog = GirafInflatableDialog.newInstance(this.getString(R.string.back), this.getString(R.string.back_description), R.layout.dialog_back);
            backDialog.show(getSupportFragmentManager(), BACK_SEQUENCE_TAG);
        } else {
            super.onBackPressed();
        }
    }

    // Button to search for pictograms
    public void backSaveClick(View v) {
        checkSequenceBeforeSave(false);
        backDialog.dismiss();
        if (changesSaved) {
            super.onBackPressed();
        }
    }

    // Button to search for pictograms, that should be used in a "choice" activity
    public void backDontSaveClick(View v) {
        backDialog.dismiss();
        super.onBackPressed();
    }


    private void createAndShowChoiceDialog() {
        //Create instance of ChoiceDialog and display it
        choiceMode = true;
        tempPictogramList.clear();

        choiceDialog = GirafInflatableDialog.newInstance(this.getString(R.string.choice), this.getString(R.string.choice_dialog_subtitle), R.layout.dialog_choice, CHOICE_DIALOG);
        choiceDialog.show(getSupportFragmentManager(), EDIT_CHOICE_TAG);
    }

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
        //choiceListEdited = false;

        tempPictogramList.clear();
        for (int i = 0; i < choice.getFramesList().size(); i++) {
            Pictogram pictogram = new Pictogram();
            pictogram.setId(choice.getFramesList().get(i).getPictogramId());
            tempPictogramList.add(pictogram);
        }

        //Adapter to display a list of pictograms in the choice dialog
        choiceAdapter = setupAdapter(choice);
        setupChoiceGroup(choiceAdapter);
    }

    // SKRIV NOGET HER
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
        if (tempPictogramList == null || tempPictogramList.size() == 0) {
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

    // Button to search for pictograms, that should be used in a "choice" activity
    public void choiceCancelClick(View v) {
        choiceMode = false;
        choiceListEdited = false;
        pictogramEditPos = -1;
        choiceDialog.dismiss();
    }

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
                tempPictogramId = choice.getFramesList().get(pictogramEditPos).getPictogramId();
                callPictoSearch(CHOICE_EDIT_PICTOGRAM_CALL);
            }
        });

        return choiceGroup;
    }


    // creates the "save dialog", when the save button is clicked.
    // The following two methods is connected to girafbuttons in the view
    private void createAndShowSaveDialog() {
        //Create instance of AddDialog and display it
        saveDialog = GirafInflatableDialog.newInstance(this.getString(R.string.save), this.getString(R.string.sequence_saved), R.layout.dialog_save);
        saveDialog.show(getSupportFragmentManager(), SAVE_SEQUENCE_TAG);
    }

    // Button to search for pictograms
    public void savedClick(View v) {
        saveDialog.dismiss();
    }


    /**
     * Error dialogs
     * The error dialogs uses the GirafNotifyDialog (From Giraf components), where only some strings and a tag is needed.
      */
    private void createAndShowErrorDialogEmptySequence() {
        //Creates alertDialog to display error. Clicking Ok dismisses the Dialog
        GirafNotifyDialog alertDialog = GirafNotifyDialog.newInstance(this.getString(R.string.error), this.getString(R.string.empty_sequence_error), ERROR_EMPTY_SEQUENCE);
        alertDialog.show(getSupportFragmentManager(), EMPTY_SEQUENCE_ERROR_TAG);
    }

    private void createAndShowErrorDialogEmptyChoice() {
        //Creates alertDialog to display error. Clicking Ok dismisses the Dialog
        GirafNotifyDialog alertDialog = GirafNotifyDialog.newInstance(this.getString(R.string.error), this.getString(R.string.empty_choice_error), ERROR_EMPTY_CHOICE);
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
                checkFrameMode(frame);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //If Activity Result is OK, call relevant method depending on the RequestCode used to launch Activity with
        if (resultCode == RESULT_OK) {
            switch (requestCode) {

                case PICTO_EDIT_SEQUENCE_THUMBNAIL_CALL:
                    onEditSequenceThumbnailResult(data);
                    break;

                case PICTO_EDIT_PICTOGRAM_CALL:
                    onEditPictogramResult(data);
                    break;

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

                case CHOICE_EDIT_PICTOGRAM_CALL:
                    onEditPictogramResult(data);
                    break;

                default:
                    break;
            }
        }
    }

    private void onNewPictogramResult(Intent data) {
        int[] checkoutIds = data.getExtras().getIntArray(PICTO_INTENT_CHECKOUT_ID);

        //If no pictures are returned, assume user cancelled and nothing is supposed to change.
        if (checkoutIds.length == 0) {
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
            choiceListEdited = true;
            choiceAdapter.notifyDataSetChanged();
            adapter.notifyDataSetChanged();
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

    private void onEditPictogramResult(Intent data) {
        int[] checkoutIds = data.getExtras().getIntArray(PICTO_INTENT_CHECKOUT_ID);

        //
        if (checkoutIds.length == 0) {
            return;
        }
        if (!choiceMode) {
            Frame frame = sequence.getFramesList().get(pictogramEditPos);
            frame.setPictogramId(checkoutIds[0]);
            adapter.notifyDataSetChanged();
            changesSaved = false;
        } else {
            for (int i = 0; i < choice.getFramesList().size(); i++) {
                if (tempPictogramId == choice.getFramesList().get(i).getPictogramId()) {
                    choice.getFramesList().get(i).setPictogramId(checkoutIds[0]);
                }
            }
            choiceListEdited = true;
            choiceAdapter.notifyDataSetChanged();
            adapter.notifyDataSetChanged();
            changesSaved = false;
        }
    }

    private void onEditSequenceThumbnailResult(Intent data) {
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

        if (modeId == PICTO_NEW_PICTOGRAM_CALL || modeId == CHOICE_NEW_PICTOGRAM_CALL) {
            intent.putExtra("purpose", "multi");
        } else {
            intent.putExtra("purpose", "single");
        }
        startActivityForResult(intent, modeId);
    }

    private void checkFrameMode(Frame frame) {

        if (frame.getPictogramList().size() > 0) {
            createAndShowChoiceDialog();
        } else {
            callPictoSearch(PICTO_EDIT_PICTOGRAM_CALL);
        }
    }

    @Override
    public void noticeDialog(int i) {
        switch (i) {
            case ERROR_EMPTY_SEQUENCE:

                break;

            default:
                break;
        }
    }

    private SequenceAdapter setupAdapter(final Sequence seq) {
        //Sets up the adapter for the Sequence to display
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
                            //seq.getFramesList().remove(position);
                            if (choiceMode) {
                                choiceListEdited = true;
                                choice.getFramesList().remove(position);
                            } else {
                                seq.getFramesList().remove(position);
                            }
                            changesSaved = false;
                            adapter.notifyDataSetChanged();
                            choiceAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
        return adapter;
    }

    @Override
    public void onBackPressed() {
        createAndShowBackDialog();
    }

    // This function is used by the GirafInflatableDialog, in order to access components in the xml, which changes dynamically
    @Override
    public void editCustomView(ViewGroup viewGroup, int i) {
        // The choice dialog used a view that is updated according to the sequence. 
        if (i == CHOICE_DIALOG) {
            sequenceChoiceGroupTemplate = (SequenceViewGroup) viewGroup.findViewById(R.id.choice_view_group);
            choiceGroup = sequenceChoiceGroupTemplate;
            // Sets up the dialog, now that the resources are properly found.
            setupChoiceDialog();
        }
    }
}