package com.dar.nclientv2.adapters;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.dar.nclientv2.R;
import com.dar.nclientv2.SearchActivity;
import com.dar.nclientv2.components.History;
import com.dar.nclientv2.settings.Global;

import java.util.HashSet;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<History> history;
    private SearchActivity context;

    public HistoryAdapter(SearchActivity context) {
        this.context = context;
        if(!Global.isKeepHistory())context.getSharedPreferences("History",0).edit().clear().apply();
        history=Global.isKeepHistory()?History.setToList(context.getSharedPreferences("History",0).getStringSet("history",new HashSet<>())):null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new HistoryAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_history, parent, false));
    }
    int remove=-1;
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Global.loadImage(remove==holder.getAdapterPosition()?R.drawable.ic_close:R.drawable.ic_mode_edit,holder.imageButton);
        String entry=history.get(holder.getAdapterPosition()).getValue();
        holder.text.setText(entry);
        holder.master.setOnClickListener(v -> context.setQuery(entry,true));
        holder.imageButton.setOnLongClickListener(v -> {
            context.runOnUiThread(() -> {
                if(remove==holder.getAdapterPosition()){
                    remove=-1;
                    notifyItemChanged(holder.getAdapterPosition());
                }else{
                    if(remove!=-1){
                        int l=remove;
                        remove=-1;
                        notifyItemChanged(l);
                    }
                    remove=holder.getAdapterPosition();
                    notifyItemChanged(holder.getAdapterPosition());
                }
            });
            return true;
        });
        holder.imageButton.setOnClickListener(v -> {
            if(remove==holder.getAdapterPosition()){
                removeHistory(remove);
                remove=-1;
            }else{
                context.setQuery(entry,false);
            }
        });
    }

    @Override
    public int getItemCount() {
        return history==null?0:history.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout master;
        TextView text;
        ImageButton imageButton;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.master=itemView.findViewById(R.id.master_layout);
            this.text=itemView.findViewById(R.id.text);
            this.imageButton=itemView.findViewById(R.id.edit);
        }
    }
    public void addHistory(String value){
        if(!Global.isKeepHistory())return;
        History history=new History(value,false);
        int pos=this.history.indexOf(history);
        if(pos>=0)this.history.set(pos,history);
        else this.history.add(history);
        context.getSharedPreferences("History",0).edit().putStringSet("history",History.listToSet(this.history)).apply();
    }
    public void removeHistory(int pos){
        history.remove(pos);
        context.getSharedPreferences("History",0).edit().putStringSet("history",History.listToSet(this.history)).apply();
        context.runOnUiThread(() -> notifyItemRemoved(pos));
    }
}
