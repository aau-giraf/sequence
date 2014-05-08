package dk.aau.cs.giraf.zebra;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import java.util.List;

import dk.aau.cs.giraf.gui.GButton;
import dk.aau.cs.giraf.gui.GComponent;
import dk.aau.cs.giraf.gui.GDialog;
import dk.aau.cs.giraf.gui.GDialogMessage;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Frame;
import dk.aau.cs.giraf.zebra.PictogramView.OnDeleteClickListener;
import dk.aau.cs.giraf.zebra.SequenceAdapter.OnAdapterGetViewListener;
import dk.aau.cs.giraf.zebra.SequenceViewGroup.OnNewButtonClickedListener;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;
import dk.aau.cs.giraf.oasis.lib.models.Pictogram;

public class SequenceActivity extends Activity {

    private boolean isInEditMode;
    private boolean isNew;
    private boolean assumeMinimize = true;
    public static boolean choiceMode = false;
	private int guardianId;
    private int profileId;
    private int sequenceId;
    private int pictogramEditPos = -1;
	private Sequence originalSequence = new Sequence();
	public static Sequence sequence;
    public static Sequence choice = new Sequence();
    private SequenceAdapter adapter;
    private SequenceAdapter choiceAdapter;
    private List<Frame> tempFrameList;
    private List<Pictogram> tempPictogramList = new ArrayList<Pictogram>();
	private GButton cancelButton;
    private GButton saveButton;
    private GButton addButton;
    private GButton previewButton;
	private EditText sequenceTitleView;
	private ImageView sequenceImageView;
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

        activityToKill = this;

		Bundle extras = getIntent().getExtras();
		profileId = extras.getInt("profileId");
        sequenceId = extras.getInt("sequenceId");
        Log.d("DebugYeah", "[SequenceActivity] Activity launched for SequenceId " + Integer.toString(sequenceId));
		guardianId = extras.getInt("guardianId");
		isNew = extras.getBoolean("new");
		isInEditMode = extras.getBoolean("editMode");

        try {
            helper = new Helper(this);
        } catch (Exception e) {
        }

        if (sequenceId != 0) {
            originalSequence = helper.sequenceController.getSequenceById(sequenceId);
        }

		// Get a clone of the sequence so the original sequence is not modified
		sequence = originalSequence;
        Log.d("DebugYeah", "[SequenceActivity] Sequence currently has " + Integer.toString(sequence.getFramesList().size()) + " frames");

		// Create Adapter
		adapter = setupAdapter();

		// Create Sequence Group
		SequenceViewGroup sequenceViewGroup = setupSequenceViewGroup(adapter);
		sequenceTitleView = (EditText) findViewById(R.id.sequence_title);

        LinearLayout backgroundLayout = (LinearLayout) findViewById(R.id.parent_container);
        RelativeLayout topbarLayout = (RelativeLayout) findViewById(R.id.sequence_bar);
        backgroundLayout.setBackgroundColor(GComponent.GetBackgroundColor());
        topbarLayout.setBackgroundColor(GComponent.GetBackgroundColor());

		initializeTopBar();

		sequenceImageView = (ImageView) findViewById(R.id.sequence_image);

		if (sequence.getPictogramId() == 0) {
			sequenceImageView.setImageDrawable(getResources().getDrawable(
					R.drawable.ic_launcher));
		} else {
            try {
                helper = new Helper(this);
            } catch (Exception e) {
            }
            sequenceImageView.setImageBitmap(helper.pictogramHelper.getPictogramById(sequence.getPictogramId()).getImage());
		}

		sequenceImageView.setOnClickListener(new ImageView.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (isInEditMode) {
					callPictoAdmin(PICTO_SEQUENCE_IMAGE_CALL);
				}
			}
		});

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

        if (isNew == true) {
            sequence.setProfileId(MainActivity.selectedChild.getId());
            sequence.setSequenceType(Sequence.SequenceType.SEQUENCE);
            //TODO: Figure out why frames are not saved. Database problem?
            Log.d("DebugYeah", "[SequenceActivity] Sequence  has " + Integer.toString(sequence.getFramesList().size()) + " frames before save");
            helper.sequenceController.insertSequenceAndFrames(sequence);
            //Log.d("DebugYeah", "[SequenceActivity] Sequence has " + Integer.toString(helper.sequenceController.getSequenceById(17).getFramesList().size()) + " frames after save");

        } else {
            helper.sequenceController.modifySequenceAndFrames(sequence);
        }
        finishActivity();
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
                        intent.putExtra("currentGuardianID", guardianId);
                        //TODO: Revert to int after sprint end
                        intent.putExtra("currentChildID", profileId);
                        startActivity(intent);
                        isInEditMode = true;
                        //TODO: Get chosen sequence from MainActivity.nestedSequenceId

                        Frame frame = new Frame();
                        frame.setNestedSequence(MainActivity.nestedSequenceId);

                        if (pictogramEditPos == -1) {
                            sequence.addFrame(frame);
                        }
                        else {
                            tempFrameList = sequence.getFramesList();
                            tempFrameList.set(pictogramEditPos, frame);
                            sequence.setFramesList(tempFrameList);
                        }

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
                        tempFrameList.add(frame);
                        pictogramEditPos = tempFrameList.size()-1;
                    } else {
                        tempFrameList.set(pictogramEditPos, frame);
                    }

                    Log.d("DebugYeah", "[SequenceActivity] tempFrameList has " + Integer.toString(tempFrameList.size()) + " frames before setFramesList");
                    //TODO: Figure out why this does not work setFramesList empties sequence.
                    sequence.setFramesList(tempFrameList);

                    Log.d("DebugYeah", "[SequenceActivity] sequence has " + Integer.toString(sequence.getFramesList().size()) + "frames after setFramesList");
                    adapter.notifyDataSetChanged();
                    choiceMode = false;
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
                        //TODO It seems one can't change views when rearranging. Figure out an alternative.
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
									//sequence.deletePictogram(position);
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
                                    //choice.deletePictogram(position);
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

			default:
				break;
			}
		}
	}

	private void OnNewPictogramResult(Intent data) {
		int[] checkoutIds = data.getExtras().getIntArray(
				PICTO_INTENT_CHECKOUT_ID);

        //If no pictures are returned, assume user canceled and nothing is supposed to change.
        if (checkoutIds == null) {
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
                try {
                    helper = new Helper(this);
                } catch (Exception e) {
                }
                sequenceImageView.setImageBitmap(helper.pictogramHelper.getPictogramById(sequence.getPictogramId()).getImage());
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
		sequenceImageView.setImageBitmap(helper.pictogramHelper.getPictogramById(sequence.getPictogramId()).getImage());
	}

	private void initializeTopBar() {

        Buttons();

		sequenceTitleView.setEnabled(isInEditMode);
            sequenceTitleView.setText(sequence.getName());

		// Create listener to remove focus when "Done" is pressed on the keyboard
		sequenceTitleView
				.setOnEditorActionListener(new OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId,
                                                  KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            EditText editText = (EditText) findViewById(R.id.sequence_title);
                            editText.clearFocus();
                        }
                        return false;
                    }
                });

		// Create listeners on every view to remove focus from the EditText when touched
		createClearFocusListeners(findViewById(R.id.parent_container));

		// Create listener to hide the keyboard and save when the EditText loses focus
		sequenceTitleView.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					hideSoftKeyboardFromView(sequenceTitleView);
				}
			}
		});

		TextView childName = (TextView) findViewById(R.id.child_name);
		childName.setText(MainActivity.selectedChild.getName());
	}

	public void hideSoftKeyboardFromView(View view) {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	/*
	   Creates listeners to remove focus from EditText when something else is
	   touched (to hide the softkeyboard)

	   @param view
	              The parent container. The function runs recursively on its children
	 */
	public void createClearFocusListeners(View view) {
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
				createClearFocusListeners(innerView);
			}
		}
	}

	private void callPictoAdmin(int modeId) {
        assumeMinimize = false;
		Intent intent = new Intent();
		intent.setComponent(new ComponentName(PICTO_ADMIN_PACKAGE, PICTO_ADMIN_CLASS));
		intent.putExtra("currentChildID", MainActivity.selectedChild.getId());
		intent.putExtra("currentGuardianID", guardianId);
		
		if (modeId == PICTO_NEW_PICTOGRAM_CALL)
			intent.putExtra("purpose", "multi");
		else
			intent.putExtra("purpose", "single");
		
		startActivityForResult(intent, modeId);
	}

    private void Buttons() {

        cancelButton = (GButton) findViewById(R.id.cancel_button);
        saveButton = (GButton) findViewById(R.id.save_button);
        addButton = (GButton) findViewById(R.id.add_button);
        previewButton = (GButton) findViewById(R.id.preview_button);

        saveButton.setOnClickListener(new ImageButton.OnClickListener() {

            @Override
            public void onClick(View v) {
                showSaveDialog(v);
            }
        });
        cancelButton.setOnClickListener(new ImageButton.OnClickListener() {

            @Override
            public void onClick(View v) {
                showExitDialog(v);
            }
        });
        addButton.setOnClickListener(new ImageButton.OnClickListener(){

            @Override
            public void onClick(View v) {
                showAddDialog(v);
            }
        });
        previewButton.setOnClickListener(new ImageButton.OnClickListener()
        {
            //TODO: CREATE FUNCTIONALITY HERE (CALL SEQUENCEVIEWER)
            @Override
            public void onClick(View v) {
                finishActivity();
            }
        });
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
        //assumeMinimize kills the entire application if minimized
        // in any other ways than opening Pictosearch or inserting a nested Sequence
        if (assumeMinimize) {
            MainActivity.activityToKill.finish();
            finishActivity();
        }
        else {
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