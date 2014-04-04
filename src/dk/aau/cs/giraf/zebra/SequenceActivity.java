package dk.aau.cs.giraf.zebra;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import dk.aau.cs.giraf.gui.GButton;
import dk.aau.cs.giraf.gui.GDialog;
import dk.aau.cs.giraf.gui.GDialogMessage;
import dk.aau.cs.giraf.gui.GTextView;
import dk.aau.cs.giraf.zebra.PictogramView.OnDeleteClickListener;
import dk.aau.cs.giraf.zebra.SequenceAdapter.OnAdapterGetViewListener;
import dk.aau.cs.giraf.zebra.SequenceViewGroup.OnNewButtonClickedListener;
import dk.aau.cs.giraf.zebra.models.Child;
import dk.aau.cs.giraf.zebra.models.Pictogram;
import dk.aau.cs.giraf.zebra.models.Sequence;
import dk.aau.cs.giraf.zebra.serialization.SequenceFileStore;

public class SequenceActivity extends Activity {

	private long guardianId;
	
	private Sequence originalSequence;
	private Sequence sequence;
	private SequenceAdapter adapter;

//	private GButton okButton;
	private GButton cancelButton;
    private GButton saveButton;
    private GButton addButton;
//    private GButton returnButton;
    private GButton editSequenceNameButton;

	private SequenceViewGroup sequenceViewGroup;
	private EditText sequenceTitleView;

	private ImageView sequenceImageView;

    private int applicationColor;

	private boolean isInEditMode;
	private boolean isNew;

	private final String PICTO_ADMIN_PACKAGE = "dk.aau.cs.giraf.pictosearch";
	private final String PICTO_ADMIN_CLASS = PICTO_ADMIN_PACKAGE + "." + "PictoAdminMain";

	private final int PICTO_SEQUENCE_IMAGE_CALL = 345;
	private final int PICTO_EDIT_PICTOGRAM_CALL = 456;
	private final int PICTO_NEW_PICTOGRAM_CALL = 567;

	private final String PICTO_INTENT_CHECKOUT_ID = "checkoutIds";

	private int pictogramEditPos = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sequence);

		Bundle extras = getIntent().getExtras();
		long profileId = extras.getLong("profileId");
		long sequenceId = extras.getLong("sequenceId");
		guardianId = extras.getLong("guardianId");
		isNew = extras.getBoolean("new");
		isInEditMode = extras.getBoolean("editMode");
        applicationColor = extras.getInt("applicationColor");

		originalSequence = MainActivity.selectedChild.getSequenceFromId(sequenceId);

		// Get a clone of the sequence so the original sequence is not modified
		sequence = originalSequence.getClone();

		// Create Adapter
		adapter = setupAdapter();

		// Create Sequence Group
		sequenceViewGroup = setupSequenceViewGroup(adapter);
		sequenceTitleView = (EditText) findViewById(R.id.sequence_title);

		cancelButton = (GButton) findViewById(R.id.cancel_button);
        saveButton = (GButton) findViewById(R.id.save_button);
        addButton = (GButton) findViewById(R.id.add_button);
        editSequenceNameButton = (GButton) findViewById(R.id.edit_sequence_name_button);

        LinearLayout backgroundLayout = (LinearLayout) findViewById(R.id.parent_container);
        RelativeLayout topbarLayout = (RelativeLayout) findViewById(R.id.sequence_bar);
        backgroundLayout.setBackgroundColor(applicationColor);
        topbarLayout.setBackgroundColor(applicationColor);


		initializeTopBar();




		sequenceImageView = (ImageView) findViewById(R.id.sequence_image);

		if (sequence.getImageId() == 0) {
			sequenceImageView.setImageDrawable(getResources().getDrawable(
					R.drawable.ic_launcher));
		} else {
			sequenceImageView.setImageBitmap(sequence.getImage(this));
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

                    // Forces the keyboard to pop up when using the editSequenceNameButton
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(sequenceTitle, InputMethodManager.SHOW_IMPLICIT);

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
/*
			private void setDeleteButtonVisible(boolean value) {
				// Make buttons transparent
				if (value == false) {
//					okButton.setAlpha(0.3f);
					cancelButton.setAlpha(0.3f);
                    editSequenceNameButton.setAlpha(0.3f);
				} else {
//					okButton.setAlpha(1.0f);
					cancelButton.setAlpha(1.0f);
                    editSequenceNameButton.setAlpha(1.0f);
				}

				// Disable/enable buttons
//				okButton.setEnabled(value);
				cancelButton.setEnabled(value);
                editSequenceNameButton.setEnabled(value);

			} */
		});
	}

	private void saveChanges() {
        //TODO: SAVE IN THE NEW DATABASE
        finish();
	}

    /* TODO:Dette er koden til knappen der skal bruges i sequenceViewer
    public void showReturnDialog(View v) {
        GDialogMessage returnDialog = new GDialogMessage(v.getContext(),
                R.drawable.ic_launcher,
                "Afslut Sekvens",
                "Du er ved at afslutte sekvensen",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });


        returnDialog.show();
    }
*/
    public void showSaveDialog(View v) {
        GDialogMessage saveDialog = new GDialogMessage(v.getContext(),
                R.drawable.ic_launcher,
                "Gem Sekvens",
                "Du er ved at gemme sekvensen",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
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

	@Override
	public void onBackPressed() {
		if (isInEditMode) {
			cancelButton.performClick();
		} else {
			super.onBackPressed();
		}
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
 //                     SequenceActivity.this.saveChanges();
                }
            });

            discardChanges.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    finish();
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
                    dismiss();
                }
            });

            getPictogram.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });

            getChoice.setOnClickListener(new GButton.OnClickListener() {

                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
    }


	private SequenceViewGroup setupSequenceViewGroup(
			final SequenceAdapter adapter) {
		final SequenceViewGroup sequenceGroup = (SequenceViewGroup) findViewById(R.id.sequenceViewGroup);
		sequenceGroup.setEditModeEnabled(isInEditMode);
		sequenceGroup.setAdapter(adapter);

		// Handle rearrange
		sequenceGroup
				.setOnRearrangeListener(new SequenceViewGroup.OnRearrangeListener() {
					@Override
					public void onRearrange(int indexFrom, int indexTo) {
						sequence.rearrange(indexFrom, indexTo);
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

						callPictoAdmin(PICTO_NEW_PICTOGRAM_CALL);
					}
				});

		sequenceGroup.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view,
					int position, long id) {
				pictogramEditPos = position;
				callPictoAdmin(PICTO_EDIT_PICTOGRAM_CALL);
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
									sequence.deletePictogram(position);
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

		for (int id : checkoutIds) {
			Pictogram pictogram = new Pictogram();
			pictogram.setPictogramId(id);
			sequence.addPictogramAtEnd(pictogram);
		}

		if (sequence.getImageId() == 0 && checkoutIds.length > 0) {
			sequence.setImageId(checkoutIds[0]);
			sequenceImageView.setImageBitmap(sequence.getImage(this));
		}

		adapter.notifyDataSetChanged();
	}

	private void OnEditPictogramResult(Intent data) {
		if (pictogramEditPos < 0)
			return;

		int[] checkoutIds = data.getExtras().getIntArray(
				PICTO_INTENT_CHECKOUT_ID);

		if (checkoutIds.length == 0)
			return;
		Pictogram pictogram = sequence.getPictograms().get(pictogramEditPos);
		pictogram.setPictogramId(checkoutIds[0]);
		adapter.notifyDataSetChanged();
	}

	private void OnEditSequenceImageResult(Intent data) {
		int[] checkoutIds = data.getExtras().getIntArray(
				PICTO_INTENT_CHECKOUT_ID);

		if (checkoutIds.length == 0)
			return;
		sequence.setImageId(checkoutIds[0]);
		sequenceImageView.setImageBitmap(sequence.getImage(this));
	}

	private void initializeTopBar() {

		sequenceTitleView.setEnabled(isInEditMode);
		sequenceTitleView.setText(sequence.getTitle());

		// Create listener to remove focus when "Done" is pressed on the
		// keyboard
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

		// Create listeners on every view to remove focus from the EditText when
		// touched
		createClearFocusListeners(findViewById(R.id.parent_container));

		// Create listener to hide the keyboard and save when the EditText loses
		// focus
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
/*
		if (!isInEditMode) {
			okButton.setVisibility(View.INVISIBLE);
            cancelButton.setVisibility(View.INVISIBLE);
            editSequenceNameButton.setVisibility(View.INVISIBLE);
		}
*/

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

/*
        returnButton.setOnClickListener(new ImageButton.OnClickListener() {

            @Override
            public void onClick(View v) {
                showReturnDialog(v);
            }
        });
*/
        //When clicking the button, the cursor is placed in the Sequence title field.
        editSequenceNameButton.setOnClickListener(new ImageButton.OnClickListener(){
            @Override public void onClick (View v) {
                EditText et = (EditText)findViewById(R.id.sequence_title);
                et.requestFocus();
            }
        });

	}

	public void hideSoftKeyboardFromView(View view) {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	/**
	 * Creates listeners to remove focus from EditText when something else is
	 * touched (to hide the softkeyboard)
	 * 
	 * @param view
	 *            The parent container. The function runs recursively on its
	 *            children
	 */
	public void createClearFocusListeners(View view) {
		// Create listener to remove focus from EditText when something else is
		// touched
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

		// If the view is a container, run the function recursively on the
		// children
		if (view instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				View innerView = ((ViewGroup) view).getChildAt(i);
				createClearFocusListeners(innerView);
			}
		}
	}

	private void callPictoAdmin(int modeId) {
		Intent intent = new Intent();
		intent.setComponent(new ComponentName(PICTO_ADMIN_PACKAGE, PICTO_ADMIN_CLASS));
		intent.putExtra("currentChildID", MainActivity.selectedChild.getProfileId());
		intent.putExtra("currentGuardianID", guardianId);
		
		if (modeId == PICTO_NEW_PICTOGRAM_CALL)
			intent.putExtra("purpose", "multi");
		else
			intent.putExtra("purpose", "single");
		
		startActivityForResult(intent, modeId);
	}

}
