package com.dar.nclientv2;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.dar.nclientv2.utility.files.MasterFileManager;

import java.io.IOException;
import java.io.OutputStream;

public class FileManagerActivity extends AppCompatActivity {
    private Button grantAccess,writeFolder,writeFile;
    private DocumentFile folder=null;
    DocumentFile pickedDir = null;
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager);
        grantAccess=findViewById(R.id.grantAccess);
        writeFile=findViewById(R.id.createFile);
        writeFolder=findViewById(R.id.createFolder);
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP) {
            grantAccess.setVisibility(View.GONE);
        } else {
            grantAccess.setOnClickListener(v -> {
                grantSdAccess();
            });
        }

        writeFolder.setOnClickListener(v -> {
            if(pickedDir==null)return;
            folder=pickedDir.createDirectory("NClientV2");
        });
        writeFile.setOnClickListener(v -> {
            if(folder==null)return;
            DocumentFile file = folder.createFile(null,"Ciao");
            try {
                OutputStream stream= getContentResolver().openOutputStream(file.getUri());
                stream.write("CIAONE".getBytes());
                stream.flush();
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    //@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void grantSdAccess() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent,10);
// Check for the freshest data.
        //getContentResolver().takePersistableUriPermission(uri, takeFlags);
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            if (data != null) {
                assert data.getData()!=null;
                pickedDir = DocumentFile.fromTreeUri(this, data.getData());
                MasterFileManager.makePersistentStorage(this,data.getData());
            }
        }
    }

}
