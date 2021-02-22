package com.dar.nclientv2.components.classes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.R;
import com.dar.nclientv2.utility.LogUtility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public abstract class MultichoiceAdapter<D, T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<MultichoiceAdapter.MultichoiceViewHolder<T>> {
    private final List<MultichoiceListener> listeners = new ArrayList<>(3);
    private Mode mode = Mode.NORMAL;
    private final HashMap<Long, D> map = new HashMap<Long, D>() {
        @Nullable
        @Override
        public D put(Long key, D value) {
            D res = super.put(key, value);
            if (size() == 1) startSelecting();
            changeSelecting();
            return res;
        }

        @Nullable
        @Override
        public D remove(@Nullable Object key) {
            D res = super.remove(key);
            if (isEmpty()) endSelecting();
            changeSelecting();
            return res;
        }

        @Override
        public void clear() {
            super.clear();
            endSelecting();
            changeSelecting();
        }
    };

    public MultichoiceAdapter() {
        setHasStableIds(true);
    }

    private void changeSelecting() {
        for (MultichoiceListener listener : listeners)
            listener.choiceChanged();
    }

    /**
     * Used only to do a put
     */
    protected abstract D getItemAt(int position);

    protected abstract ViewGroup getMaster(T holder);

    protected abstract void defaultMasterAction(int position);

    protected abstract void onBindMultichoiceViewHolder(T holder, int position);

    @NonNull
    protected abstract T onCreateMultichoiceViewHolder(@NonNull ViewGroup parent, int viewType);

    @Override
    public abstract long getItemId(int position);

    private void startSelecting() {
        setMode(Mode.SELECTING);
        for (MultichoiceListener listener : listeners)
            listener.firstChoice();
    }

    private void endSelecting() {
        setMode(Mode.NORMAL);
        for (MultichoiceListener listener : listeners)
            listener.noMoreChoices();
    }

    public void addListener(MultichoiceListener listener) {
        this.listeners.add(listener);
    }

    @NonNull
    @Override
    public final MultichoiceViewHolder<T> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        T innerLayout = onCreateMultichoiceViewHolder(parent, viewType);
        ViewGroup master = getMaster(innerLayout);
        ConstraintLayout multiLayout = (ConstraintLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.multichoice_adapter, master, true);
        return new MultichoiceViewHolder<>(multiLayout, innerLayout);
    }

    @Override
    public final void onBindViewHolder(@NonNull MultichoiceViewHolder<T> holder, final int position) {
        boolean isSelected = map.containsKey(getItemId(holder.getAdapterPosition()));
        View master = getMaster(holder.innerHolder);
        updateLayoutParams(master, holder.censor, isSelected);

        master.setOnClickListener(v -> {
            switch (mode) {
                case SELECTING:
                    toggleSelection(holder.getAdapterPosition());
                    break;
                case NORMAL:
                    defaultMasterAction(holder.getAdapterPosition());
                    break;
            }
        });
        master.setOnLongClickListener(v -> {
            map.put(getItemId(holder.getAdapterPosition()), getItemAt(holder.getAdapterPosition()));
            notifyItemChanged(holder.getAdapterPosition());
            return true;
        });

        holder.censor.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.checkmark.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.censor.setOnClickListener(v -> toggleSelection(holder.getAdapterPosition()));
        onBindMultichoiceViewHolder(holder.innerHolder, holder.getAdapterPosition());
    }

    private void updateLayoutParams(View master, View multichoiceHolder, boolean isSelected) {
        if (master == null) return;
        int margin = isSelected ? 8 : 0;
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) master.getLayoutParams();
        params.setMargins(margin, margin, margin, margin);
        master.setLayoutParams(params);

        if (isSelected && multichoiceHolder != null) {
            master.post(() -> {
                ViewGroup.LayoutParams multiParam = multichoiceHolder.getLayoutParams();
                multiParam.width = master.getWidth();
                multiParam.height = master.getHeight();
                LogUtility.d("Multiparam: " + multiParam.width + ", " + multiParam.height);
                multichoiceHolder.setLayoutParams(multiParam);
            });
        }
    }

    private void toggleSelection(int position) {
        long id = getItemId(position);
        if (map.containsKey(id))
            map.remove(id);
        else
            map.put(id, getItemAt(position));
        notifyItemChanged(position);
    }

    public Mode getMode() {
        return mode;
    }

    private void setMode(Mode mode) {
        this.mode = mode;
    }

    public void selectAll() {
        final int count = getItemCount();
        for (int i = 0; i < count; i++)
            map.put(getItemId(i), getItemAt(i));
        notifyItemRangeChanged(0, count);
    }

    public Collection<D> getSelected() {
        return map.values();
    }

    public void deselectAll() {
        map.clear();
        notifyItemRangeChanged(0, getItemCount());
    }

    public enum Mode {NORMAL, SELECTING}

    public interface MultichoiceListener {
        void firstChoice();

        void noMoreChoices();

        void choiceChanged();
    }

    public static class DefaultMultichoiceListener implements MultichoiceListener {

        @Override
        public void firstChoice() {

        }

        @Override
        public void noMoreChoices() {

        }

        @Override
        public void choiceChanged() {

        }
    }

    public static class MultichoiceViewHolder<T extends RecyclerView.ViewHolder> extends RecyclerView.ViewHolder {
        final T innerHolder;
        final View censor;
        final ImageView checkmark;
        final ConstraintLayout multichoiceHolder;

        public MultichoiceViewHolder(@NonNull ConstraintLayout multichoiceHolder, T holder) {
            super(holder.itemView);
            this.multichoiceHolder = multichoiceHolder;
            this.innerHolder = holder;
            this.censor = multichoiceHolder.findViewById(R.id.censor);
            this.checkmark = multichoiceHolder.findViewById(R.id.checkmark);
        }
    }

}
