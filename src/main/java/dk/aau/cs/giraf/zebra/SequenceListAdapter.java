package dk.aau.cs.giraf.zebra;

import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import dk.aau.cs.giraf.oasis.lib.models.Sequence;

/**
 * Adapter for a List of Sequences typically associated with a child
 */
public class SequenceListAdapter extends BaseAdapter {
    private List<SequencePictogramViewPair> items;
    private Context context;
    private boolean isInEditMode;
    private OnAdapterGetViewListener onAdapterGetViewListener;
    private SelectedSequenceAware selectedSequenceAware;

    public interface SelectedSequenceAware {
        boolean isSequenceMarked(Sequence sequence);
    }

    public SequenceListAdapter(Context context, List<SequencePictogramViewPair> items, SelectedSequenceAware selectedSequenceAware) {
        this.selectedSequenceAware = selectedSequenceAware;
        this.items = items;
        this.context = context;
    }

    public static class SequencePictogramViewPair {
        private final Sequence sequence;
        private PictogramView pictogramView;

        public SequencePictogramViewPair(Sequence sequence, PictogramView pictogramView) {
            this.sequence = sequence;
            this.pictogramView = pictogramView;
        }

        private void setPictogramView(PictogramView pictogramView) {
            this.pictogramView = pictogramView;
        }

        public Sequence getSequence() {
            return sequence;
        }

        public PictogramView getPictogramView() {
            return pictogramView;
        }
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        PictogramView v;

        if (convertView == null) {
            v = new PictogramView(context, 16f);
        } else {
            v = (PictogramView) convertView;
        }

        final SequencePictogramViewPair sequenceViewPair = items.get(position);

        final Sequence sequence = sequenceViewPair.getSequence();

        v.setTitle(sequence.getName());
        v.setEditModeEnabled(isInEditMode);
        v.setImageFromId(sequence.getPictogramId());

        // Check if the user provided a SelectedCategoryAware
        if (selectedSequenceAware != null) {
            boolean isSequenceMarked = selectedSequenceAware.isSequenceMarked(sequence);

            // Check if the view is selected
            //sequence.getId() == selectedSequencePictogramViewPair.sequence.getId()
            /*if (isSequenceMarked) {
                // Set the background-color for the selected item
                v.setBackgroundColor(context.getResources().getColor(R.color.giraf_page_indicator_active));
            }*/
            sequenceViewPair.setPictogramView(v);
        }

        //if (onAdapterGetViewListener != null)
        //    onAdapterGetViewListener.onAdapterGetView(position, v);


        return v;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public SequencePictogramViewPair getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getSequence().getId();
        //return position;
    }

    public void setEditModeEnabled(boolean editEnabled) {
        if (isInEditMode != editEnabled) {
            isInEditMode = editEnabled;
        }
    }

    public void setOnAdapterGetViewListener(OnAdapterGetViewListener onCreateViewListener) {
        this.onAdapterGetViewListener = onCreateViewListener;
    }

    public OnAdapterGetViewListener getOnAdapterGetViewListener() {
        return this.onAdapterGetViewListener;
    }

    public interface OnAdapterGetViewListener {
        public void onAdapterGetView(int position, View view);
    }
}
