package dk.aau.cs.giraf.zebra;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dk.aau.cs.giraf.activity.GirafActivity;
import dk.aau.cs.giraf.gui.GButton;
import dk.aau.cs.giraf.gui.GComponent;
import dk.aau.cs.giraf.gui.GDialog;
import dk.aau.cs.giraf.gui.GDialogAlert;
import dk.aau.cs.giraf.gui.GDialogMessage;
import dk.aau.cs.giraf.gui.GRadioButton;
import dk.aau.cs.giraf.gui.GirafButton;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Frame;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.zebra.PictogramView.OnDeleteClickListener;
import dk.aau.cs.giraf.zebra.SequenceAdapter.OnAdapterGetViewListener;
import dk.aau.cs.giraf.zebra.SequenceViewGroup.OnNewButtonClickedListener;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;
import dk.aau.cs.giraf.oasis.lib.models.Pictogram;

public class SequenceActivity extends GirafActivity {

    private Profile guardian;
    private Profile selectedChild;
    private boolean isInEditMode;
    private boolean isNew;
    private boolean assumeMinimize = true;
    public static boolean choiceMode = false;
    private int guardianId;
    private int childId;
    private int sequenceId;
    private int pictogramEditPos = -1;
    public static Sequence sequence;
    public static Sequence choice = new Sequence();
    public static SequenceAdapter adapter;
    public static SequenceAdapter choiceAdapter;
    private List<Frame> tempFrameList;
    private List<Pictogram> tempPictogramList = new ArrayList<Pictogram>();
    private GButton backButton;
    private GButton sequenceImageButton;
    private EditText sequenceTitleView;
    private final String PICTO_ADMIN_PACKAGE = "dk.aau.cs.giraf.pictosearch";
    private final String PICTO_ADMIN_CLASS = PICTO_ADMIN_PACKAGE + "." + "PictoAdminMain";
    private final String PICTO_INTENT_CHECKOUT_ID = "checkoutIds";
    private final int PICTO_SEQUENCE_IMAGE_CALL = 345;
    private final int PICTO_EDIT_PICTOGRAM_CALL = 456;
    private final int PICTO_NEW_PICTOGRAM_CALL = 567;
    private final int SEQUENCE_VIEWER_CALL = 1337;
    private final int NESTED_SEQUENCE_CALL = 40;
    public static Activity activityToKill;
    private Helper helper;
    private GDialog printAlignmentDialog;
    private File[] file;

    // Initialize buttons
    private GirafButton saveButton;
    private GirafButton deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sequence);

        // Create buttons
        saveButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_save));
        deleteButton = new GirafButton(this, getResources().getDrawable(R.drawable.icon_delete));

        // Adding buttons to action-bar
        addGirafButtonToActionBar(saveButton, LEFT);
        addGirafButtonToActionBar(deleteButton, RIGHT);

        //Make Activity killable
        activityToKill = this;

        loadIntents();
        loadProfiles();
        loadSequence();
        setColors();
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

    private void setColors() {
        //Sets up application colors using colors from GIRAF_Components
        LinearLayout backgroundLayout = (LinearLayout) findViewById(R.id.parent_container);
        backgroundLayout.setBackgroundDrawable(GComponent.GetBackground(GComponent.Background.SOLID));
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

        /* Add button listener
        addButton.setOnClickListener(new ImageButton.OnClickListener(){
        //Show Add dialog when clicking the Add Button
            @Override
            public void onClick(View v) {
                createAndShowAddDialog(v);
            }
        });
        */

        /* Preview button listener
        previewButton.setOnClickListener(new ImageButton.OnClickListener() {
            //If no changes has been made to Sequence, call SequenceViewer. Otherwise display Dialog, prompting user to save Sequence first
            @Override
            public void onClick(View v) {

                if (isNew) {
                    showpreviewDialog(v);
                } else if (!sequence.getFramesList().equals(helper.sequenceController.getSequenceAndFrames(sequence.getId()).getFramesList())) {
                    showpreviewDialog(v);
                } else if (!sequenceTitleView.getText().equals(sequence.getName())) {
                    showpreviewDialog(v);
                } else {
                    callSequenceViewer();
                }
            }
        });
        */

        /* Print button listener
        printButton.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
        public void onClick(View v) {
                SequenceActivity.this.openPrintAlignmentDialogBox();
            }
        });
        */

        /*
        sequenceImageButton.setOnClickListener(new ImageView.OnClickListener() {
            //If Sequence Image Button is clicked, call PictoAdmin to select an Image for the Sequence
            @Override
            public void onClick(View v) {
                if (isInEditMode) {
                    callPictoAdmin(PICTO_SEQUENCE_IMAGE_CALL);
                }
            }
        });

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

    /**
     * Combines all pictograms in a number of bitmaps either horizontally or vertically.
     *
     * @param direction Can be either "horizontal" or "vertical". Determines the direction in which the pictograms will be added.
     * @return An array of bitmaps containing pictograms.
     */
    private Bitmap[] combineFrames(String direction) {

        int frameDimens = 200;
        int numframes = sequence.getFramesList().size();

        // Adjust spacing and offSet to have optimal number of pics / page.
        int spacing = 18;
        float offSet = 35f;

        int totalSeqLengthInPixels = ((frameDimens + spacing) * numframes);

        // Dimensions of pictograms in mm when printed.
        int printedDimens = 30;

        int a4height = (int) ((297.0 / printedDimens) * frameDimens);
        int a4width = (int) ((210.0 / printedDimens) * frameDimens);

        float center = (float) (a4width / 2 - frameDimens / 2);

        int numberOfCanvases = (int) Math.ceil(totalSeqLengthInPixels / (a4height - offSet));
        int numberPicsPerLine = (int) Math.floor((a4height - offSet) / (totalSeqLengthInPixels / numframes));
        int numberOfPicsAdded = 0;

        List<Canvas> comboImage = new ArrayList<Canvas>();

        Bitmap[] combinedSequence = new Bitmap[numberOfCanvases];

        for (int i = 0; i < numberOfCanvases; i++) {

            if (direction == "vertical") {
                combinedSequence[i] = Bitmap.createBitmap(a4width, a4height, Bitmap.Config.RGB_565);
                comboImage.add(i, new Canvas(combinedSequence[i]));

                float offSetTemp = offSet;
                for (int ii = 0; ii < numberPicsPerLine && numberOfPicsAdded < numframes; ii++) {
                    Bitmap bm = helper.pictogramHelper.getPictogramById(sequence.getFramesList().get(ii).getPictogramId()).getImage();
                    bm = Bitmap.createScaledBitmap(bm, frameDimens, frameDimens, false);
                    comboImage.get(i).drawBitmap(bm, center, offSetTemp, null);
                    offSetTemp += frameDimens + spacing;
                    numberOfPicsAdded++;
                }
            } else {
                // Swapped height and width to "turn the paper".
                combinedSequence[i] = Bitmap.createBitmap(a4height, a4width, Bitmap.Config.RGB_565);
                comboImage.add(i, new Canvas(combinedSequence[i]));

                float offSetTemp = offSet;
                for (int ii = 0; ii < numberPicsPerLine && numberOfPicsAdded < numframes; ii++) {
                    Bitmap bm = helper.pictogramHelper.getPictogramById(sequence.getFramesList().get(ii).getPictogramId()).getImage();
                    bm = Bitmap.createScaledBitmap(bm, frameDimens, frameDimens, false);
                    comboImage.get(i).drawBitmap(bm, offSetTemp, center, null);
                    offSetTemp += frameDimens + spacing;
                    numberOfPicsAdded++;
                }
            }
        }

        return combinedSequence;
    }

    public void printSequence(View v) {
        GRadioButton verticalButton = (GRadioButton) printAlignmentDialog.findViewById(R.id.vertical);
        Bitmap[] combinedSequence;

        //Warn the user and stop if trying to print empty sequence.
        if (sequence.getFramesList().size() == 0) {
            //TODO: Display message that it is not possible to print empty Sequence
            printAlignmentDialog.dismiss();
            return;
        }

        if (verticalButton.isChecked())
            combinedSequence = combineFrames("vertical");
        else
            combinedSequence = combineFrames("horizontal");

        // Set debug-email in class variable "debugEmail" when debugging!
        String email = guardian.getEmail();
        String message;
        if (combinedSequence.length == 1) {
            message = "Print det vedhæftede billede som helsidet billede på A4-størrelse papir for 3x3 cm piktogrammer.";
        } else {
            message = "Print de vedhæftede billeder som helsidede billeder på A4-størrelse papir for 3x3 cm piktogrammer.";
        }

        try {
            //TODO: Convert text to ArrayList of CharSequence to display properly. See logcat for info
            sendSequenceToEmail(combinedSequence, email, "Sekvens: " + sequenceTitleView.getText(), message);
        } catch (Exception e) {
            //TODO: Display error message that email could not be sent
        }
    }

    public void openPrintAlignmentDialogBox() {
        printAlignmentDialog = new GDialog(this, LayoutInflater.from(this).inflate(R.layout.dialog_print_alignment, null));
        printAlignmentDialog.show();
    }

    /**
     * Based on: http://stackoverflow.com/questions/15662258/how-to-save-a-bitmap-on-internal-storage
     *
     * @param fileName
     * @return file
     */
    private File[] getOutputMediaFile(String[] fileName, int numOfImages) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + getApplicationContext().getPackageName()
                + "/Files");

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        // Create a media file name
        File mediaFile[] = new File[numOfImages];

        for (int i = 0; i < numOfImages; i++) {
            mediaFile[i] = new File(mediaStorageDir.getPath() + File.separator + fileName[i]);
        }

        return mediaFile;
    }

    public void sendSequenceToEmail(Bitmap[] seqImage, String emailAddress, String subject, String message) {

        int numOfImages = seqImage.length;
        String[] filename = new String[numOfImages];


        for (int i = 0; i < numOfImages; i++) {
            filename[i] = "Sekvens del " + (i + 1) + " af " + numOfImages + ".png";
        }

        file = getOutputMediaFile(filename, numOfImages);

        try {
            for (int i = 0; i < numOfImages; i++) {
                FileOutputStream out = new FileOutputStream(file[i]);
                seqImage[i].compress(Bitmap.CompressFormat.PNG, 90, out);
                out.close();
            }
        } catch (Exception e) {
        }

        ArrayList<Uri> fileUris = new ArrayList<Uri>();

        for (int i = 0; i < numOfImages; i++) {
            fileUris.add(Uri.fromFile(file[i]));
        }

        Intent email = new Intent(Intent.ACTION_SEND_MULTIPLE);
        email.setType("message/rfc822");//("image/jpeg");
        email.putExtra(Intent.EXTRA_EMAIL, new String[]{emailAddress});
        email.putExtra(Intent.EXTRA_SUBJECT, subject);
        email.putExtra(Intent.EXTRA_TEXT, message);
        email.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);

        try {
            startActivity(Intent.createChooser(email, "Vælg en email-klient"));
        } catch (android.content.ActivityNotFoundException ex) {
            //TODO: Display error message that email client could not be found
        }
        printAlignmentDialog.dismiss();
    }

    public void verticalRButtonClicked(View v) {
        GRadioButton radioButton = (GRadioButton) printAlignmentDialog.findViewById(R.id.horizontal);
        radioButton.setChecked(false);
    }

    public void horizontalRButtonClicked(View v) {
        GRadioButton radioButton = (GRadioButton) printAlignmentDialog.findViewById(R.id.vertical);
        radioButton.setChecked(false);
    }

    public void dialogPrintAlignmentCancel(View v) {
        printAlignmentDialog.dismiss();
    }

    private void saveChanges() {
        //Create helper to use Database Helpers
        helper = new Helper(this);

        //Save name from Title to the Sequence
        sequence.setName(sequenceTitleView.getText().toString());

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
                        assumeMinimize = false;

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

                case NESTED_SEQUENCE_CALL:
                    onNestedSequenceResult(data);
                    break;

                default:
                    break;
            }
        }
    }

    private void onNestedSequenceResult(Intent data) {
        //Get the Nested Sequence Id from Extras
        int nestedSequenceId = data.getExtras().getInt("nestedSequenceId");

        //If Nested Sequence was inserted on new position, save in a new Frame in the Sequence
        if (pictogramEditPos == -1) {
            //Create new Frame and set relevant Data
            Frame frame = new Frame();
            frame.setNestedSequence(nestedSequenceId);

            //Create helper to give the Frame the first Image from the Nested Sequence (Used for display)
            helper = new Helper(getBaseContext());

            frame.setPictogramId(helper.sequenceController.getSequenceById(nestedSequenceId).getPictogramId());

            //Add Frame to the Sequence
            sequence.addFrame(frame);
        }
        //If Nested Sequence was inserted on an existing Frame, set the first Image from the Nested Sequence and overwrite Frame.
        else {
            sequence.getFramesList().get(pictogramEditPos).setNestedSequence(nestedSequenceId);
            sequence.getFramesList().get(pictogramEditPos).setPictogramId(helper.sequenceController.getSequenceById(nestedSequenceId).getPictogramId());

            //Reset the position to edit on
            pictogramEditPos = -1;
        }
        //Update adapter with the new Data
        adapter.notifyDataSetChanged();
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
        } else {

            for (int id : checkoutIds) {
                Frame frame = new Frame();
                frame.setPictogramId(id);
                sequence.addFrame(frame);
            }

            if (sequence.getPictogramId() == 0 && checkoutIds.length > 0) {
                sequence.setPictogramId(checkoutIds[0]);
                helper = new Helper(this);
                Drawable d = new BitmapDrawable(getResources(), helper.pictogramHelper.getPictogramById(sequence.getPictogramId()).getImage());
                sequenceImageButton.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
                sequenceImageButton.setVisibility(View.GONE);
                sequenceImageButton.setVisibility(View.VISIBLE);
            }
            adapter.notifyDataSetChanged();
        }
    }

    private void OnEditPictogramResult(Intent data) {
        if (pictogramEditPos < 0)
            return;

        int[] checkoutIds = data.getExtras().getIntArray(
                PICTO_INTENT_CHECKOUT_ID);

        if (checkoutIds.length == 0)
            return;
        Frame frame = sequence.getFramesList().get(pictogramEditPos);
        frame.setPictogramId(checkoutIds[0]);
        adapter.notifyDataSetChanged();
    }

    private void OnEditSequenceImageResult(Intent data) {
        int[] checkoutIds = data.getExtras().getIntArray(
                PICTO_INTENT_CHECKOUT_ID);

        if (checkoutIds.length == 0)
            return;
        sequence.setPictogramId(checkoutIds[0]);
        Drawable d = new BitmapDrawable(getResources(), helper.pictogramHelper.getPictogramById(sequence.getPictogramId()).getImage());
        sequenceImageButton.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
        sequenceImageButton.setVisibility(View.GONE);
        sequenceImageButton.setVisibility(View.VISIBLE);
    }

    private void callPictoAdmin(int modeId) {
        assumeMinimize = false;
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(PICTO_ADMIN_PACKAGE, PICTO_ADMIN_CLASS));
        intent.putExtra("currentChildID", selectedChild.getId());
        intent.putExtra("currentGuardianID", guardian.getId());

        if (modeId == PICTO_NEW_PICTOGRAM_CALL)
            intent.putExtra("purpose", "multi");
        else
            intent.putExtra("purpose", "single");

        startActivityForResult(intent, modeId);
    }

    private void callSequenceViewer() {
        assumeMinimize = false;

        Intent intent = new Intent();
        intent.setComponent(new ComponentName("dk.aau.cs.giraf.sequenceviewer", "dk.aau.cs.giraf.sequenceviewer.MainActivity"));
        intent.putExtra("sequenceId", sequence.getId());
        intent.putExtra("callerType", "Zebra");
        startActivityForResult(intent, SEQUENCE_VIEWER_CALL);
    }

    private void showpreviewDialog(View v) {
        GDialogMessage previewDialog = new GDialogMessage(v.getContext(),
                getResources().getString(R.string.save_sequence),
                getResources().getString(R.string.save_sequence_req),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean sequenceOk;
                        sequenceOk = checkSequenceBeforeSave(v);
                        if (sequenceOk) {
                            callSequenceViewer();
                        }
                    }
                });
        previewDialog.show();
    }

    private void finishActivity() {
        assumeMinimize = false;
        finish();
    }

    @Override
    public void onBackPressed() {
        if (isInEditMode) {
            backButton.performClick();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        /*assumeMinimize makes it possible to kill the entire application if ever minimized.
        onStop is also called when entering other Activities, which is why the assumeMinimize check is needed
        assumeMinimize is set to false every time an Activity is entered and then reset to true here so application is not killed*/
        if (assumeMinimize) {
            MainActivity.activityToKill.finish();
            finishActivity();
        } else {
            //If assumeMinimize was false, reset it to true
            assumeMinimize = true;
        }
        super.onStop();
    }

    private void checkFrameMode(Frame frame, View v) {

        if (frame.getNestedSequence() != 0) {
            createAndShowNestedDialog(v);

        } else if (frame.getPictogramList().size() > 0) {
            createAndShowChoiceDialog(v);
        } else {
            callPictoAdmin(PICTO_EDIT_PICTOGRAM_CALL);
        }
    }

    private class AddDialog extends GDialog {

        private AddDialog(Context context) {
            super(context);

            this.SetView(LayoutInflater.from(this.getContext()).inflate(R.layout.add_frame_dialog, null));

            GButton getSequence = (GButton) findViewById(R.id.get_sequence);
            GButton getPictogram = (GButton) findViewById(R.id.get_pictogram);
            GButton getChoice = (GButton) findViewById(R.id.get_choice);

            getSequence.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    createAndShowNestedDialog(v);
                    dismiss();
                }
            });
            getPictogram.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    callPictoAdmin(PICTO_NEW_PICTOGRAM_CALL);
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
            choiceGroup
                    .setOnRearrangeListener(new SequenceViewGroup.OnRearrangeListener() {
                        @Override
                        public void onRearrange(int indexFrom, int indexTo) {
                            adapter.notifyDataSetChanged();
                        }
                    });


            // Handle new view
            choiceGroup
                    .setOnNewButtonClickedListener(new OnNewButtonClickedListener() {
                        @Override
                        public void onNewButtonClicked() {
                            final SequenceViewGroup sequenceGroup = (SequenceViewGroup) findViewById(R.id.choice_view_group);
                            sequenceGroup.liftUpAddNewButton();

                            callPictoAdmin(PICTO_NEW_PICTOGRAM_CALL);
                        }
                    });

            choiceGroup.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapter, View view,
                                        int position, long id) {
                    pictogramEditPos = position;
                    callPictoAdmin(PICTO_EDIT_PICTOGRAM_CALL);
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