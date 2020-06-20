package com.dar.nclientv2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.dar.nclientv2.FileChooserActivity;
import com.dar.nclientv2.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    private FileChooserActivity context;
    @NonNull private File actualFolder;
    private List<File>prevFolder=new ArrayList<>();
    private boolean parentAccess;
    private File[] files;
    public void sortFiles(){
        Arrays.sort(files, File::compareTo);
    }
    public FileAdapter(FileChooserActivity context,@NonNull File actualFolder) {
        this.context=context;
        setActualFolder(actualFolder);
    }
    private boolean validFile(File pathname){
        return pathname!=null &&
                pathname.isDirectory() &&
                !pathname.getName().startsWith(".") &&
                pathname.canWrite() && pathname.canRead();
    }
    public void setActualFolder(@NonNull File actual){
        setActualFolder(actual,true);
    }
    public void setActualFolder(@NonNull File actual,boolean add) {
        if(validFile(actual)) {
            if(add)
                prevFolder.add(actualFolder);
            actualFolder = actual;
        }
        parentAccess=validFile(actualFolder.getParentFile());//validFile(this.actualFolder.getParentFile());
        files=actualFolder.listFiles(this::validFile);
        if(files==null)files=new File[0];
        sortFiles();
        context.runOnUiThread(()->{
            context.setAbsolutePath(actualFolder.getAbsolutePath());
            notifyDataSetChanged();
        });
    }
    public void loadPrevFolder(){
        if(prevFolder.isEmpty())return;
        File f=prevFolder.remove(prevFolder.size()-1);
        setActualFolder(f,false);
    }
    @NonNull
    public File getActualFolder() {
        return actualFolder;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FileAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_history, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final File file;
        int pos=holder.getAdapterPosition();
        if(pos==0)file=actualFolder;
        else file = files[pos-parentOffset()];
        holder.imageButton.setVisibility(View.GONE);
        holder.text.setText(file==actualFolder?"..":file.getName());
        holder.master.setOnClickListener(v -> {
            if(file==actualFolder)setActualFolder(actualFolder.getParentFile());
            else setActualFolder(file);
        });
    }
    private int parentOffset(){
        return parentAccess?1:0;
    }
    @Override
    public int getItemCount() {
        return files.length+parentOffset();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ConstraintLayout master;
        final TextView text;
        final ImageButton imageButton;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.master=itemView.findViewById(R.id.master_layout);
            this.text=itemView.findViewById(R.id.text);
            this.imageButton=itemView.findViewById(R.id.edit);
        }
    }
}
