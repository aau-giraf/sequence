package dk.aau.cs.giraf.sequence;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import dk.aau.cs.giraf.dblib.models.Frame;
import dk.aau.cs.giraf.dblib.models.Sequence;

/**
 * Adapter used for sequences, in the context of the given child
 */
public class SequenceAdapter extends BaseAdapter {

    private final Context context;
    private final Sequence sequence;

    private OnAdapterGetViewListener onAdapterGetViewListener;

    public SequenceAdapter(Context context, Sequence sequence) {
        if (sequence == null) {
            this.sequence = new Sequence();
        } else {
            this.sequence = sequence;
        }

        this.context = context;
    }

    @Override
    public int getCount() {
        return sequence.getFramesList().size();
    }

    @Override
    public Frame getItem(int position) {
        if (position >= 0 && position < sequence.getFramesList().size()) {
            return sequence.getFramesList().get(position);
        } else {
            return null;
        }
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        PictogramView view;
        Frame frame = getItem(position);

        if (convertView == null) {
            view = new PictogramView(context, 24f);
        } else {
            view = (PictogramView) convertView;
        }

        view.setImageFromId(frame.getPictogramId());

        if (onAdapterGetViewListener != null)
            onAdapterGetViewListener.onAdapterGetView(position, view);

        return view;
    }

    public void setOnAdapterGetViewListener(OnAdapterGetViewListener onCreateViewListener) {
        this.onAdapterGetViewListener = onCreateViewListener;
    }

    public interface OnAdapterGetViewListener {
        void onAdapterGetView(int position, View view);
    }
}

