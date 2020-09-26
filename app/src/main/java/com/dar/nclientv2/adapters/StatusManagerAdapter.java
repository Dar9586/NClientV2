package com.dar.nclientv2.adapters;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.R;
import com.dar.nclientv2.components.status.Status;
import com.dar.nclientv2.components.status.StatusManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import yuku.ambilwarna.AmbilWarnaDialog;

public class StatusManagerAdapter extends RecyclerView.Adapter<StatusManagerAdapter.ViewHolder> {
    private List<Status>statusList;
    private Activity activity;
    public StatusManagerAdapter(Activity activity){
        statusList= StatusManager.toList();
        this.activity=activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int resId= R.layout.entry_status;
        View view=LayoutInflater.from(activity).inflate(resId,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Status status=statusList.get(holder.getAdapterPosition());
        holder.name.setText(status.name);
        holder.color.setBackgroundColor(status.opaqueColor());
        holder.master.setOnClickListener(v -> {
            updateStatus(status);
        });
        holder.cancel.setOnClickListener(v -> {
            StatusManager.remove(status);
            notifyItemRemoved(statusList.indexOf(status));
            statusList.remove(status);
        });
    }

    @Override
    public int getItemCount() {
        return statusList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position==statusList.size()?1:0;
    }
    private int newColor;
    private void updateStatus(Status status){
        MaterialAlertDialogBuilder builder=new MaterialAlertDialogBuilder(activity);
        LinearLayout layout=(LinearLayout) View.inflate(activity,R.layout.dialog_add_status,null);
        EditText name=layout.findViewById(R.id.name);
        Button btnColor=layout.findViewById(R.id.color);
        btnColor.setBackgroundColor(status.opaqueColor());
        name.setText(status.name);
        btnColor.setOnClickListener(v -> new AmbilWarnaDialog(activity, status.color, false, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override public void onCancel(AmbilWarnaDialog dialog) {}

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                if(color== Color.WHITE||color==Color.BLACK){
                    Toast.makeText(activity, R.string.invalid_color_selected, Toast.LENGTH_SHORT).show();
                    return;
                }
                newColor=color;
                btnColor.setBackgroundColor(color);
            }
        }).show());
        builder.setView(layout);
        builder.setTitle(R.string.update_status);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            String newName=name.getText().toString();
            if(newName.length()<2) {
                Toast.makeText(activity, R.string.name_too_short, Toast.LENGTH_SHORT).show();
                return;
            }
            Status newStatus=StatusManager.updateStatus(status,name.getText().toString(),newColor);
            int index=statusList.indexOf(status);
            statusList.set(index,newStatus);
            notifyItemChanged(index);
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout master;
        Button color;
        ImageButton cancel;
        TextView name;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name=itemView.findViewById(R.id.name);
            cancel=itemView.findViewById(R.id.cancelButton);
            color=itemView.findViewById(R.id.color);
            master=itemView.findViewById(R.id.master_layout);
        }
    }
}
