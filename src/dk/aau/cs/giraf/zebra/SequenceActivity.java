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
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
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

	private ImageButton okButton;
	private ImageButton cancelButton;

	private SequenceViewGroup sequenceViewGroup;
	private EditText sequenceTitleView;

	private ImageView sequenceImageView;

	private Child child;

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

		child = ZebraApplication.getChildFromId(profileId);
		originalSequence = child.getSequenceFromId(sequenceId);

		// Get a clone of the sequence so the original sequence is not modified
		sequence = originalSequence.getClone();

		// Create Adapter
		adapter = setupAdapter();

		// Create Sequence Group
		sequenceViewGroup = setupSequenceViewGroup(adapter);
		sequenceTitleView = (EditText) findViewById(R.id.sequence_title);

		okButton = (ImageButton) findViewById(R.id.ok_button);
		cancelButton = (ImageButton) findViewById(R.id.cancel_button);

		initializeTopBar();

		sequenceImageView = (ImageView) findViewById(R.id.sequence_image);

		if (sequence.getImageId() == 0) {
			sequenceImageView.setImageDrawable(getResources().getDrawable(
					R.drawable.sequence_placeholder));
		} else {
			sequenceImageView.setImageDrawable(sequence.getImage(this));
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
				if (hasFocus) {
					setDeleteButtonVisible(false);
				} else {
					// Closing the keyboard when the text field is not active
					// anymore
					InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					in.hideSoftInputFromWindow(v.getWindowToken(),
							InputMethodManager.HIDE_NOT_ALWAYS);

					setDeleteButtonVisible(true);
				}
			}

			private void setDeleteButtonVisible(boolean value) {
				// Make buttons transparent
				if (value == false) {
					okButton.setAlpha(0.3f);
					cancelButton.setAlpha(0.3f);
				} else {
					okButton.setAlpha(1.0f);
					cancelButton.setAlpha(1.0f);
				}

				// Disable/enable buttons
				okButton.setEnabled(value);
				cancelButton.setEnabled(value);
			}
		});
	}

	private void discardChanges() {
		setEditModeEnabled(false);

		// Discarding changes
		sequence = originalSequence.getClone();
		sequenceTitleView.setText(sequence.getTitle());
		adapter.notifyDataSetChanged();
	}

	private void saveChanges() {
		setEditModeEnabled(false);
		
		if (isNew && sequence.getPictograms().size() == 0) {
			child.getSequences().remove(originalSequence);
		}
		else {
			// Saving changes
			sequence.setTitle(sequenceTitleView.getText().toString());
			originalSequence.copyFromSequence(sequence);
	
			SequenceFileStore.writeSequences(this, child, child.getSequences());
		}
	}

	private void setEditModeEnabled(boolean isInEditMode) {
		this.isInEditMode = isInEditMode;

		sequenceTitleView.setEnabled(isInEditMode);

		cancelButton
				.setVisibility(isInEditMode ? View.VISIBLE : View.INVISIBLE);
		okButton.setVisibility(isInEditMode ? View.VISIBLE : View.INVISIBLE);

		sequenceViewGroup.setEditModeEnabled(isInEditMode);
	}

	private void showBackDialog() {

		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.back_dialog_box);
		dialog.getWindow().setBackgroundDrawable(
				new ColorDrawable(Color.TRANSPARENT));

		final Button saveButton = (Button) dialog.findViewById(R.id.btn_save);
		saveButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
				SequenceActivity.this.saveChanges();
				
				finish();
			}
		});

		final Button dontSaveButton = (Button) dialog.findViewById(R.id.btn_dont_save);
		dontSaveButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();

				// If the sequence is new, the sequence will be deleted.
				if (isNew) {
					child.getSequences().remove(originalSequence);
				}
				finish();
			}
		});

		final Button cancelButton = (Button)dialog.findViewById(R.id.btn_back_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		dialog.show();
	}

	private void showDiscardDialog() {

		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.discard_dialog_box);
		dialog.getWindow().setBackgroundDrawable(
				new ColorDrawable(Color.TRANSPARENT));

		final Button undoButton = (Button) dialog.findViewById(R.id.btn_undo_changes);
		undoButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();

				// If the sequence is new, the sequence will be deleted.
				if (isNew) {
					child.getSequences().remove(originalSequence);
					finish();
				} else {
					discardChanges();
				}
			}
		});

		final Button cancelButton = (Button) dialog.findViewById(R.id.btn_discard_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		dialog.show();
	}

	@Override
	public void onBackPressed() {
		if (isInEditMode) {
			showBackDialog();
		} else {
			super.onBackPressed();
		}
	}

    //This will occur when the user presses the home button.
    /*This is commented because it will create a bug where it closes Zebra when returning from Pictosearch

    @Override
    public void onUserLeaveHint (){
        if (isInEditMode) {
            super.onUserLeaveHint();
            finish(); //Kills the activities within this app

        } else {
            super.onUserLeaveHint();
            finish();
        }

    }
    */

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
		long[] checkoutIds = data.getExtras().getLongArray(
				PICTO_INTENT_CHECKOUT_ID);

		for (long id : checkoutIds) {
			Pictogram pictogram = new Pictogram();
			pictogram.setPictogramId(id);
			sequence.addPictogramAtEnd(pictogram);
		}

		if (sequence.getImageId() == 0 && checkoutIds.length > 0) {
			sequence.setImageId(checkoutIds[0]);
			sequenceImageView.setImageDrawable(sequence.getImage(this));
		}

		adapter.notifyDataSetChanged();
	}

	private void OnEditPictogramResult(Intent data) {
		if (pictogramEditPos < 0)
			return;

		long[] checkoutIds = data.getExtras().getLongArray(
				PICTO_INTENT_CHECKOUT_ID);

		if (checkoutIds.length == 0)
			return;
		Pictogram pictogram = sequence.getPictograms().get(pictogramEditPos);
		pictogram.setPictogramId(checkoutIds[0]);
		adapter.notifyDataSetChanged();
	}

	private void OnEditSequenceImageResult(Intent data) {
		long[] checkoutIds = data.getExtras().getLongArray(
				PICTO_INTENT_CHECKOUT_ID);

		if (checkoutIds.length == 0)
			return;
		sequence.setImageId(checkoutIds[0]);
		sequenceImageView.setImageDrawable(sequence.getImage(this));
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
		childName.setText(child.getName());

		if (!isInEditMode) {
			cancelButton.setVisibility(View.INVISIBLE);
			okButton.setVisibility(View.INVISIBLE);
		}

		cancelButton.setOnClickListener(new ImageButton.OnClickListener() {

			@Override
			public void onClick(View v) {
				showDiscardDialog();
			}

		});

		okButton.setOnClickListener(new ImageButton.OnClickListener() {

			@Override
			public void onClick(View v) {
				SequenceActivity.this.saveChanges();
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
		intent.putExtra("currentChildID", child.getProfileId());
		intent.putExtra("currentGuardianID", guardianId);
		
		if (modeId == PICTO_NEW_PICTOGRAM_CALL)
			intent.putExtra("purpose", "multi");
		else
			intent.putExtra("purpose", "single");
		
		startActivityForResult(intent, modeId);
	}
}
