package dk.aau.cs.giraf.zebra;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import dk.aau.cs.giraf.oasis.lib.Helper;


/**
 * Contains a pictogram and a title.
 * The view will display a delete button in the corner when the application is in editmode.
 * It also adds support for highlighting.
 */
public class PictogramView extends LinearLayout {

    public final static float NORMAL_SCALE = 0.8f;
    public final static float HIGHLIGHT_SCALE = 0.9f;
    public final static float LOWLIGHT_SCALE = 0.7f;
    private final static float DEFAULT_TEXT_SIZE = 18f;

    private Helper helper;
    private RoundedImageView pictogram;
    private TextView title;
    private ImageButton deleteButton;
    private OnDeleteClickListener onDeleteClickListener;

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

        SquaredRelativeLayout square = new SquaredRelativeLayout(context);
        square.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        square.addView(createImageView(radius));
        square.addView(createDeleteButton());

        setupOnDeleteClickHandler();

        this.addView(square);
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
        deleteButton.setImageResource(R.layout.btn_delete_pictogram);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        deleteButton.setLayoutParams(params);

        deleteButton.setPadding(8, 8, 8, 8);
        deleteButton.setBackgroundColor(Color.TRANSPARENT);

        deleteButton.setFocusable(false);

        setDeleteButtonVisible(false);

        return deleteButton;
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

    public void setEditModeEnabled(boolean editMode) {
        if (editMode != isInEditMode) {
            isInEditMode = editMode;
            setDeleteButtonVisible(editMode);
        }
    }

    public boolean getEditModeEnabled() {
        return isInEditMode;
    }

    public void setImageFromId(int id) {
        helper = new Helper(getContext());
        pictogram.setImageBitmap(helper.pictogramHelper.getPictogramById(id).getImage());
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

    public OnDeleteClickListener getOnDeleteClickListener() {
        return onDeleteClickListener;
    }

    public interface OnDeleteClickListener {
        public void onDeleteClick();
    }
}
