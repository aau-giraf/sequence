package dk.aau.cs.giraf.sequence;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import dk.aau.cs.giraf.dblib.models.Sequence;

/**
 * Adapter for a List of Sequences typically associated with a child
 */
class SequenceListAdapter extends BaseAdapter {
    private final List<Sequence> items;
    private final Context context;
    private final SelectedSequenceAware selectedSequenceAware;

    public interface SelectedSequenceAware {
        boolean isSequenceMarked(Sequence sequence);
    }

    public SequenceListAdapter(Context context, List<Sequence> items, SelectedSequenceAware selectedSequenceAware) {
        this.selectedSequenceAware = selectedSequenceAware;
        this.items = items;
        this.context = context;
    }

    /*
    public static class SequencePictogramViewPair {
        private final Sequence sequence;
        private PictogramView pictogramView;

        public SequencePictogramViewPair(final Sequence sequence, final PictogramView pictogramView) {
            this.sequence = sequence;
            this.pictogramView = pictogramView;
        }

        private void setPictogramView(final PictogramView pictogramView) {
            this.pictogramView = pictogramView;
        }

        public Sequence getSequence() {
            return sequence;
        }

        public PictogramView getPictogramView() {
            return pictogramView;
        }

        @Override
        public boolean equals(final Object o)
        {
            if(o instanceof Sequence)
            {
                return sequence.equals(o);
            }

            return sequence.equals(((SequencePictogramViewPair)o).sequence);
        }
    }*/

    public View getView(final int position, final View convertView, final ViewGroup parent) {
        PictogramView v;

        if (convertView == null) {
            v = new PictogramView(context, 16f);
        } else {
            v = (PictogramView) convertView;
        }

        //final SequencePictogramViewPair sequenceViewPair = items.get(position);

        final Sequence sequence = items.get(position);

        //sequenceViewPair.setPictogramView(v);

        v.setTitle(sequence.getName());
        v.setImageFromId(sequence.getPictogramId());

        // Check if the user provided a SelectedCategoryAware
        if (selectedSequenceAware != null) {
            final boolean isSequenceMarked = selectedSequenceAware.isSequenceMarked(sequence);

            // Check if the view is selected
            //sequence.getId() == selectedSequencePictogramViewPair.sequence.getId()
            if (isSequenceMarked) {
                // Set the background-color for the selected item
                v.setBackgroundColor(context.getResources().getColor(R.color.giraf_page_indicator_active));
            }
            else
            {
                v.setBackgroundDrawable(null);
            }
        }

        return v;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Sequence getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
        //return position;
    }
}
