package dk.aau.cs.giraf.zebra;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dk.aau.cs.giraf.gui.GButton;
import dk.aau.cs.giraf.gui.GComponent;
import dk.aau.cs.giraf.gui.GDialog;
import dk.aau.cs.giraf.gui.GDialogMessage;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Frame;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.zebra.PictogramView.OnDeleteClickListener;
import dk.aau.cs.giraf.zebra.SequenceAdapter.OnAdapterGetViewListener;
import dk.aau.cs.giraf.zebra.SequenceViewGroup.OnNewButtonClickedListener;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;
import dk.aau.cs.giraf.oasis.lib.models.Pictogram;

public class SequenceActivity extends Activity {

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
	private GButton cancelButton;
    private GButton saveButton;
    private GButton addButton;
    private GButton previewButton;
	private GButton sequenceImageButton;
    private EditText sequenceTitleView;
	private final String PICTO_ADMIN_PACKAGE = "dk.aau.cs.giraf.pictosearch";
	private final String PICTO_ADMIN_CLASS = PICTO_ADMIN_PACKAGE + "." + "PictoAdminMain";
    private final String PICTO_INTENT_CHECKOUT_ID = "checkoutIds";
	private final int PICTO_SEQUENCE_IMAGE_CALL = 345;
	private final int PICTO_EDIT_PICTOGRAM_CALL = 456;
	private final int PICTO_NEW_PICTOGRAM_CALL = 567;
    public static Activity activityToKill;
    private Helper helper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sequence);

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
        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }
        selectedChild = helper.profilesHelper.getProfileById(childId);
        guardian = helper.profilesHelper.getProfileById(guardianId);
    }

    private void loadSequence() {
        //Create helper to load data from Database (Otherwise start working on empty Sequence)
        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }

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
        RelativeLayout topbarLayout = (RelativeLayout) findViewById(R.id.sequence_bar);
        backgroundLayout.setBackgroundDrawable(GComponent.GetBackground(GComponent.Background.SOLID));
        topbarLayout.setBackgroundDrawable(GComponent.GetBackground(GComponent.Background.SOLID));
    }

    private void setupFramesGrid() {
        // Create Adapter for the SequenceViewGroup (The Grid displaying the Sequence)
        adapter = setupAdapter();
        setupSequenceViewGroup(adapter);
    }

    private void setupButtons() {
        //Creates all buttons in Activity and their listeners
        cancelButton = (GButton) findViewById(R.id.cancel_button);
        saveButton = (GButton) findViewById(R.id.save_button);
        addButton = (GButton) findViewById(R.id.add_button);
        previewButton = (GButton) findViewById(R.id.preview_button);
        sequenceImageButton = (GButton) findViewById(R.id.sequence_image);

        saveButton.setOnClickListener(new ImageButton.OnClickListener() {
        //Show Dialog to save Sequence when clicking the Save Button
            @Override
            public void onClick(View v) {
                showSaveDialog(v);
            }
        });

        cancelButton.setOnClickListener(new ImageButton.OnClickListener() {
        //Show Exit Dialog when clicking the Cancel Button
            @Override
            public void onClick(View v) {
                showExitDialog(v);
            }
        });

        addButton.setOnClickListener(new ImageButton.OnClickListener(){
        //Show Add dialog when clicking the Add Button
            @Override
            public void onClick(View v) {
                showAddDialog(v);
            }
        });

        previewButton.setOnClickListener(new ImageButton.OnClickListener() {
            //If no changes has been made to Sequence, call SequenceViewer. Otherwise display Dialog, prompting user to save Sequence first
            @Override
            public void onClick(View v) {
                if (isNew == false && sequence.getFramesList().equals(helper.sequenceController.getSequenceAndFrames(sequence.getId()).getFramesList())) {
                    callSequenceViewer();
                } else {
                    showpreviewDialog(v);
                }
            }
        });

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
            try {
                helper = new Helper(this);
            } catch (Exception e) {
            }
            Drawable d = new BitmapDrawable(getResources(), helper.pictogramHelper.getPictogramById(sequence.getPictogramId()).getImage());
            sequenceImageButton.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
        }
    }

    private void setupTopBar() {
        initializeSequenceTitle();
        initializeChildTitle();
    }

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
                if (!hasFocus) {
                    hideSoftKeyboardFromView(sequenceTitleView);
                }
            }
        });
    }

    private void initializeChildTitle() {
        //Fetches the name of the Child and puts it in the TextView
        TextView childName = (TextView) findViewById(R.id.child_name);
        childName.setText(selectedChild.getName());
    }

    public void createClearFocusListener(View view) {
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
    }

    private void hideSoftKeyboardFromView(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        sequenceTitleView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                EditText sequenceTitle = (EditText) findViewById(R.id.sequence_title);
                if (hasFocus) {

                    // Enforces that the sequenceTitleView can not get larger than parent
                    // For some reason the parent view overlaps with buttons. Therefore the width have to be 200 less than parent.
                    // TODO: Figure our why. Probably an issue with activity_sequence.xml
                    View container = findViewById(R.id.titles_container);
                    int width = container.getWidth();
                    sequenceTitleView.setMaxWidth(width-200);

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
    }

    private void saveChanges() {

        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }

        if (sequence.getFramesList().size()== 0) {
            //TODO: Display message that user is about to try saving an empty sequence.
            return;
        }
        sequence.setName(sequenceTitleView.getText().toString());

        //Set PosX of every frame to save the order in which the frames should be shown.
        for (int i = 0; i < sequence.getFramesList().size(); i++) {
            sequence.getFramesList().get(i).setPosX(i);
        }

        if (isNew) {
            sequence.setProfileId(selectedChild.getId());
            sequence.setSequenceType(Sequence.SequenceType.SEQUENCE);
            helper.sequenceController.insertSequenceAndFrames(sequence);
        }
        else {
            helper.sequenceController.modifySequenceAndFrames(sequence);
        }
	}

    public void showSaveDialog(View v) {
        GDialogMessage saveDialog = new GDialogMessage(v.getContext(),
                R.drawable.ic_launcher,
                "Gem Sekvens",
                "Du er ved at gemme sekvensen",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        saveChanges();
                        finishActivity();
                    }
                });
        saveDialog.show();
    }

    public void showExitDialog(View v) {
        backDialog exitEditting = new backDialog(v.getContext());
        exitEditting.show();
    }

    public void showAddDialog(View v) {
        addDialog addFrame = new addDialog(v.getContext());
        addFrame.show();
    }

    public void showChoiceDialog(View v) {
        choiceDialog ChoiceDialog = new choiceDialog(v.getContext());
        ChoiceDialog.show();
    }

    public void showNestedSequenceDialog(View v) {
        GDialogMessage nestedDialog = new GDialogMessage(v.getContext(),
                R.drawable.ic_launcher,
                "Åbner sekvensvalg",
                "Et nyt vindue åbnes, hvor du kan vælge en anden sekvens at indsætte",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isInEditMode = false;
                        assumeMinimize = false;
                        Intent intent = new Intent(getApplication(), MainActivity.class);
                        intent.putExtra("insertSequence", true);
                        intent.putExtra("currentGuardianID", guardian.getId());
                        intent.putExtra("currentChildID", childId);
                        startActivityForResult(intent, 1);
                        isInEditMode = true;
                    }
                });

        nestedDialog.show();
    }

    public class backDialog extends GDialog {

        public backDialog(Context context) {

            super(context);

            this.SetView(LayoutInflater.from(this.getContext()).inflate(R.layout.exit_sequence_dialog,null));

            GButton saveChanges = (GButton) findViewById(R.id.save_changes);
            GButton discardChanges = (GButton) findViewById(R.id.discard_changes);
            GButton returntoEditting = (GButton) findViewById(R.id.return_to_editting);

            saveChanges.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    SequenceActivity.this.saveChanges();
                    finishActivity();
                }
            });

            discardChanges.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    //TODO: Something leaks here. Figure out what. (See logcat)
                    finishActivity();
                }
            });

            returntoEditting.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
    }

    public class addDialog extends GDialog {

        public addDialog(Context context) {
            super(context);

            this.SetView(LayoutInflater.from(this.getContext()).inflate(R.layout.add_frame_dialog,null));

            GButton getSequence = (GButton) findViewById(R.id.get_sequence);
            GButton getPictogram = (GButton) findViewById(R.id.get_pictogram);
            GButton getChoice = (GButton) findViewById(R.id.get_choice);

            getSequence.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    showNestedSequenceDialog(v);
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
                    showChoiceDialog(v);
                }
            });
        }
    }

    public class choiceDialog extends GDialog {

        public choiceDialog(Context context) {
            super(context);

            choice.getFramesList().clear();
            if ( pictogramEditPos != -1 ) {
                for (int i = 0; i < adapter.getItem(pictogramEditPos).getPictogramList().size(); i++)
                {
                    Frame frame = new Frame();
                    frame.setPictogramId(adapter.getItem(pictogramEditPos).getPictogramList().get(i).getId());
                    choice.addFrame(frame);
                }
            }

            this.SetView(LayoutInflater.from(this.getContext()).inflate(R.layout.choice_dialog,null));

            GButton saveChoice = (GButton) findViewById(R.id.save_choice);
            GButton discardChoice = (GButton) findViewById(R.id.discard_choice);

            //Adapter to display a list of pictograms in the choice dialog
            choiceAdapter = setupchoiceAdapter();

            saveChoice.setOnClickListener(new GButton.OnClickListener(){

                @Override
                public void onClick(View v) {
                    tempFrameList = sequence.getFramesList();
                    Frame frame = new Frame();
                    if(tempPictogramList == null) {
                        //TODO: Display message that user can not save empty choice.
                        return;
                    }
                    frame.setPictogramList(tempPictogramList);
                    frame.setPictogramId(tempPictogramList.get(0).getId());


                    if (pictogramEditPos == -1){
                        sequence.addFrame(frame);
                        pictogramEditPos = tempFrameList.size()-1;
                    } else {
                        sequence.getFramesList().get(pictogramEditPos).setPictogramList(tempPictogramList);
                    }
                    adapter.notifyDataSetChanged();
                    choiceMode = false;
                    pictogramEditPos = -1;
                    dismiss();
                }
            });
            discardChoice.setOnClickListener(new GButton.OnClickListener(){
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

	private SequenceViewGroup setupSequenceViewGroup(final SequenceAdapter adapter) {
		final SequenceViewGroup sequenceGroup = (SequenceViewGroup) findViewById(R.id.sequenceViewGroup);
		sequenceGroup.setEditModeEnabled(isInEditMode);
		sequenceGroup.setAdapter(adapter);

		// Handle rearrange
		sequenceGroup
				.setOnRearrangeListener(new SequenceViewGroup.OnRearrangeListener() {
					@Override
					public void onRearrange(int indexFrom, int indexTo) {
						adapter.notifyDataSetChanged();
					}
				});

		// Handle new view
		sequenceGroup
				.setOnNewButtonClickedListener(new OnNewButtonClickedListener() {
					@Override
					public void onNewButtonClicked() {
						final SequenceViewGroup sequenceGroup = (SequenceViewGroup) findViewById(R.id.sequenceViewGroup);
						sequenceGroup.liftUpAddNewButton();
                        showAddDialog(sequenceGroup);
					}
				});

		sequenceGroup.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view,
					int position, long id) {
				pictogramEditPos = position;

                //Perform action depending on the type of pictogram clicked.
                Frame frame = sequence.getFramesList().get(position);
                checkFrameMode(frame, view);
			}
		});

		return sequenceGroup;
	}

	private SequenceAdapter setupAdapter() {
		final SequenceAdapter adapter = new SequenceAdapter(this, sequence);

		// Setup delete handler.
		adapter.setOnAdapterGetViewListener(new OnAdapterGetViewListener() {
			@Override
			public void onAdapterGetView(final int position, final View view) {
				if (view instanceof PictogramView) {
					PictogramView pictoView = (PictogramView) view;
					pictoView
							.setOnDeleteClickListener(new OnDeleteClickListener() {
								@Override
								public void onDeleteClick() {
                                    sequence.getFramesList().remove(position);
									adapter.notifyDataSetChanged();
								}
							});
				}
			}
		});

		return adapter;
	}

    private SequenceAdapter setupchoiceAdapter() {
        final SequenceAdapter adapter = new SequenceAdapter(this, choice);

        // Setup delete handler.
        adapter.setOnAdapterGetViewListener(new OnAdapterGetViewListener() {
            @Override
            public void onAdapterGetView(final int position, final View view) {
                if (view instanceof PictogramView) {
                    PictogramView pictoView = (PictogramView) view;
                    pictoView
                            .setOnDeleteClickListener(new OnDeleteClickListener() {
                                @Override
                                public void onDeleteClick() {
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

		// Remove the highlight from the add pictogram button
		final SequenceViewGroup sequenceGroup = (SequenceViewGroup) findViewById(R.id.sequenceViewGroup);
		sequenceGroup.placeDownAddNewButton();

		if (resultCode == RESULT_OK) {
			switch (requestCode) {

			    case PICTO_SEQUENCE_IMAGE_CALL:
				    OnEditSequenceImageResult(data);
				    break;

			    case PICTO_EDIT_PICTOGRAM_CALL:
				    OnEditPictogramResult(data);
				    break;

			    case PICTO_NEW_PICTOGRAM_CALL:
				    OnNewPictogramResult(data);
				    break;

                case 1:
                    onNestedSequenceResult(data);
                    break;


			default:
				break;
			}
		}
	}

    private void onNestedSequenceResult(Intent data){
        int id = data.getExtras().getInt("nestedSequenceId");

        if (pictogramEditPos == -1) {
            Frame frame = new Frame();
            frame.setNestedSequence(id);
            try {
                helper = new Helper(getBaseContext());
            } catch (Exception e) {
            }
            frame.setPictogramId(helper.sequenceController.getSequenceById(id).getPictogramId());
            sequence.addFrame(frame);
        }
        else {
            sequence.getFramesList().get(pictogramEditPos).setNestedSequence(id);
            sequence.getFramesList().get(pictogramEditPos).setPictogramId(helper.sequenceController.getSequenceById(id).getPictogramId());
        }
        adapter.notifyDataSetChanged();
    }

	private void OnNewPictogramResult(Intent data) {
		int[] checkoutIds = data.getExtras().getIntArray(
				PICTO_INTENT_CHECKOUT_ID);

        //If no pictures are returned, assume user canceled and nothing is supposed to change.
        if (checkoutIds.length == 0 || checkoutIds == null) {
                checkoutIds = new int[]{1,2,3,4};
           // return;
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
                try {
                    helper = new Helper(this);
                } catch (Exception e) {
                }
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

    private void callSequenceViewer(){
        assumeMinimize = false;

        SharedPreferences settings = getSharedPreferences(SettingsActivity.class.getName() + Integer.toString(selectedChild.getId()), MODE_PRIVATE);
        int pictogramSetting = settings.getInt("pictogramSetting", 5);
        boolean landscapeSetting = settings.getBoolean("landscapeSetting", true);

        Intent intent = new Intent();
        intent.setComponent(new ComponentName("dk.aau.cs.giraf.sequenceviewer", "dk.aau.cs.giraf.sequenceviewer.MainActivity"));
        intent.putExtra("sequenceId", sequence.getId());
        intent.putExtra("landscapeMode", landscapeSetting);
        intent.putExtra("visiblePictogramCount", pictogramSetting);
        intent.putExtra("callerType", "Zebra");
        startActivityForResult(intent, 2);
    }

    private void showpreviewDialog(View v) {
        GDialogMessage previewDialog = new GDialogMessage(v.getContext(),
                "Gem Sekvens",
                "Du bliver nødt til at gemme sekvensen før du kan se den",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v){
                        saveChanges();
                        callSequenceViewer();
                    }
                });
        previewDialog.show();
    }
    private void finishActivity(){
        assumeMinimize = false;
        finish();
    }

    @Override
    public void onBackPressed() {
        if (isInEditMode) {
            cancelButton.performClick();
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
            showNestedSequenceDialog(v);

        } else if (frame.getPictogramList().size() > 0) {
            showChoiceDialog(v);
        } else {
            callPictoAdmin(PICTO_EDIT_PICTOGRAM_CALL);
            }
        }
}