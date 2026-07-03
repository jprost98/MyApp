package com.example.myapp;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapp.data.Record;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class FileChooser extends BaseActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase mDatabase;
    private DatabaseReference userRef;
    private FirebaseStorage mStorage;
    private StorageReference storageRef;
    private Record record = new Record();
    private ProgressBar progressBar;
    private String uploadType;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    processUri(uri, "images");
                } else {
                    Log.d("Photo Picker", "No media selected");
                    finish();
                }
            });

    private final ActivityResultLauncher<String> mGetContent =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processUri(uri, "documents");
                } else {
                    Log.d("Document Picker", "No file selected");
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_file_chooser);
        
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        setSupportActionBar(findViewById(R.id.image_chooser_tb));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initFirebase();

        progressBar = findViewById(R.id.uploadProgressBar);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            record = extras.getParcelable("record");
            uploadType = extras.getString("uploadType");
        }

        if (uploadType == null) {
            finish();
            return;
        }

        if (uploadType.equals("photo")) {
            if (actionBar != null) actionBar.setTitle("Image Chooser");
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        } else if (uploadType.equals("document")) {
            if (actionBar != null) actionBar.setTitle("File Chooser");
            mGetContent.launch("*/*");
        }
    }

    private void processUri(Uri uri, String typeFolder) {
        progressBar.setVisibility(View.VISIBLE);
        String fileName = "file_" + System.currentTimeMillis();
        
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            Log.e("FileChooser", "Error getting file name", e);
        }

        storageRef = mStorage.getReference("users/" + mUser.getUid() + "/" + typeFolder + "/records/record_" + record.getRecordId() + "/" + fileName);
        startUpload(uri);
    }

    private void startUpload(Uri uri) {
        UploadTask uploadTask = storageRef.putFile(uri);
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            progressBar.setProgress((int) progress);
        }).addOnPausedListener(taskSnapshot -> {
            Toast.makeText(FileChooser.this, "Upload is paused.", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(FileChooser.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            finish();
        }).addOnSuccessListener(taskSnapshot -> {
            Toast.makeText(FileChooser.this, "Upload complete!", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            finish();
        });
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();
        if (mUser != null) {
            userRef = mDatabase.getReference("users/" + mUser.getUid());
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}