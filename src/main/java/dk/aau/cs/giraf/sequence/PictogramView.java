package dk.aau.cs.giraf.sequence;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import dk.aau.cs.giraf.dblib.Helper;
import dk.aau.cs.giraf.dblib.controllers.PictogramController;


/**
 * Contains a pictogram and a title.
 * The view will display a delete button in the corner when the application is in editmode.
 * It also adds support for highlighting.
 */
public class PictogramView extends LinearLayout {

    public final static float NORMAL_SCALE = 0.8f;
    public final static float HIGHLIGHT_SCALE = 0.9f;
    private final static float LOWLIGHT_SCALE = 0.7f;
    private final static float DEFAULT_TEXT_SIZE = 18f;

    private RoundedImageView pictogram;
    private TextView title;
    private ImageButton deleteButton;
    private ImageButton editButton;
    private OnDeleteClickListener onDeleteClickListener;
    private OnEditClickListener onEditClickListener;

    private boolean isInEditMode = false;

    public PictogramView(Context context) {
        super(context);
        initialize(context, 0);
    }

    public PictogramView(Context context, float radius) {
        super(context);
        initialize(context, radius);
    }

    private void initialize(Context context, float radius) {
        // Disable hardware accelleration to improve performance
        this.setLayerType(LAYER_TYPE_SOFTWARE, null);
        this.setOrientation(LinearLayout.VERTICAL);

        SquaredRelativeLayout squareRelLayout = new SquaredRelativeLayout(context);
        squareRelLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        squareRelLayout.addView(createImageView(radius));
        squareRelLayout.addView(createDeleteButton());
        squareRelLayout.addView(createEditButton());

        setupOnDeleteClickHandler();
        setupOnEditClickHandler();

        this.addView(squareRelLayout);
        this.addView(createTextView());
    }

    private View createImageView(float radius) {
        pictogram = new RoundedImageView(getContext(), radius);
        pictogram.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        pictogram.setScaleX(NORMAL_SCALE);
        pictogram.setScaleY(NORMAL_SCALE);

        return pictogram;
    }

    private View createTextView() {
        title = new TextView(getContext());
        title.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setTextSize(DEFAULT_TEXT_SIZE);

        return title;
    }

    private View createDeleteButton() {
        deleteButton = new ImageButton(getContext());
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.icon_delete);
        deleteButton.setImageBitmap(bm);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        deleteButton.setLayoutParams(params);

        deleteButton.setPadding(8, 8, 8, 8);
        deleteButton.setBackgroundColor(Color.TRANSPARENT);

        deleteButton.setFocusable(false);

        setDeleteButtonVisible(false);

        return deleteButton;
    }

    private View createEditButton() {
        editButton = new ImageButton(getContext());
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.icon_edit);
        editButton.setImageBitmap(bm);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        editButton.setLayoutParams(params);

        editButton.setPadding(8, 8, 8, 8);
        editButton.setBackgroundColor(Color.TRANSPARENT);

        editButton.setFocusable(false);

        setEditButtonVisible(false);

        return editButton;
    }

    public void liftUp() {
        pictogram.setScaleX(HIGHLIGHT_SCALE);
        pictogram.setScaleY(HIGHLIGHT_SCALE);
        this.setAlpha(0.7f);
        setDeleteButtonVisible(false);
        invalidate();
    }

    public void placeDown() {
        pictogram.setScaleX(NORMAL_SCALE);
        pictogram.setScaleY(NORMAL_SCALE);
        this.setAlpha(1.0f);
        setDeleteButtonVisible(isInEditMode);
        invalidate();
    }

    public void setSelected() {
        pictogram.setScaleX(HIGHLIGHT_SCALE);
        pictogram.setScaleY(HIGHLIGHT_SCALE);
        this.setAlpha(1f);

        this.invalidate();
    }

    public void setLowlighted() {
        pictogram.setScaleX(LOWLIGHT_SCALE);
        pictogram.setScaleY(LOWLIGHT_SCALE);
        this.setAlpha(0.4f);

        this.invalidate();
    }

    private void setDeleteButtonVisible(boolean visible) {
        deleteButton.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        invalidate();
    }

    private void setEditButtonVisible(boolean visible) {
        editButton.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        invalidate();
    }

    public void setEditModeEnabled(boolean editMode) {
        if (editMode != isInEditMode) {
            isInEditMode = editMode;
            setDeleteButtonVisible(editMode);
            setEditButtonVisible(editMode);
        }
    }

    public void setImageFromId(long id) {
        Helper helper = new Helper(getContext());

        /* If the first element in a sequence is a choice - pick the choose icon -
         * this leads to NullPointerException otherwise, as errors occur when it tries
         * to first the first element, if this is split in many. (as the choose element is)
         */
        if (id == 0) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.icon_choose);
            pictogram.setImageBitmap(bm);
        } else {
            // Get the bitmap of the pictogram using the helper
            pictogram.setImageBitmap(helper.pictogramHelper.getImage(helper.pictogramHelper.getById(id)));
        }
    }

    public void setTitle(String newTitle) {
        title.setText(newTitle);
    }

    public void setupOnDeleteClickHandler() {
        deleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInEditMode && onDeleteClickListener != null)
                    onDeleteClickListener.onDeleteClick();
            }
        });
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        onDeleteClickListener = listener;
    }

    public interface OnDeleteClickListener {
        void onDeleteClick();
    }

    //Listener and handler setup for edit button
    public void setupOnEditClickHandler() {
        editButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInEditMode && onEditClickListener != null)
                    onEditClickListener.onEditClick();
            }
        });
    }

    public void setOnEditClickListener(OnEditClickListener listener){
        onEditClickListener = listener;
    }

    public interface OnEditClickListener {
        void onEditClick();
    }
}
