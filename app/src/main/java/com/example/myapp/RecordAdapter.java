package com.example.myapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapp.data.Record;
import com.example.myapp.data.Vehicle;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {
    private ArrayList<Record> recordArrayList;
    private final ArrayList<Vehicle> vehicleArrayList;
    private final static int FADE_DURATION = 350;
    private static final int SELECT_PICTURE = 1;
    private String selectedImagePath;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase mDatabase;
    private DatabaseReference userRef;
    private FirebaseStorage mStorage;
    private StorageReference storageReference;
    private Record record = new Record();
    private final Activity activity;

    public RecordAdapter(ArrayList<Record> recordArrayList, ArrayList<Vehicle> vehicleArrayList, Activity activity) {
        this.recordArrayList = recordArrayList;
        this.vehicleArrayList = vehicleArrayList;
        this.activity = activity;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filterList(ArrayList<Record> filterList) {
        recordArrayList = filterList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.record_view, parent, false);
        MaterialCardView recordView = view.findViewById(R.id.record_material_card);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!recordView.isChecked()) {
                    recordView.setChecked(true);
                }
            }
        });
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return true;
            }
        });
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordAdapter.RecordViewHolder holder, int position) {
        record = recordArrayList.get(holder.getAdapterPosition());
        Date displayDate = null;
        String date = null;
        String vehicleTitle = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            displayDate = format.parse(record.getDate());
            assert displayDate != null;
            date = SimpleDateFormat.getDateInstance().format(displayDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        for (Vehicle vehicle:vehicleArrayList) {
            if (String.valueOf(vehicle.getVehicleId()).equals(record.getVehicle())) {
                vehicleTitle = vehicle.vehicleTitle();
            }
        }

        holder.recordTitle.setText(record.getTitle());
        holder.recordDate.setText(date);
        holder.recordVehicle.setText(vehicleTitle);
        StringBuilder odometerReading = new StringBuilder(record.getOdometer());
        for (int i = odometerReading.length(); i > 0; i -= 3) {
            if (i != odometerReading.length()) odometerReading.insert(i, ",");
        }
        holder.recordOdometer.setText(odometerReading.toString());

        setFadeAnimation(holder.itemView);

        String finalDate = date;
        String finalVehicleTitle = vehicleTitle;
        holder.recordView.setOnCheckedChangeListener(new MaterialCardView.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(MaterialCardView card, boolean isChecked) {
                if (isChecked) {
                    record = recordArrayList.get(holder.getAdapterPosition());
                    initFirebase();
                    MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(card.getContext());
                    AlertDialog dialog;
                    LayoutInflater li = (LayoutInflater) card.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    @SuppressLint("InflateParams") final View detailedRecordView = li.inflate(R.layout.record_detailed_view, null);

                    Button addDocumentBtn = detailedRecordView.findViewById(R.id.add_record_doc_btn);
                    Button addPhotoBtn = detailedRecordView.findViewById(R.id.add_photo_btn);
                    LinearLayout recordPhotosLayout = detailedRecordView.findViewById(R.id.record_photos_layout);
                    LinearLayout recordDocumentsLayout = detailedRecordView.findViewById(R.id.record_detail_docs_layout);
                    ProgressBar photoLoading = detailedRecordView.findViewById(R.id.record_detail_photo_loading);
                    ProgressBar documentLoading = detailedRecordView.findViewById(R.id.record_detail_document_loading);

                    TextView recordTitle = detailedRecordView.findViewById(R.id.record_detail_title);
                    TextView recordDate = detailedRecordView.findViewById(R.id.record_detail_date);
                    TextView recordVehicle = detailedRecordView.findViewById(R.id.record_detail_vehicle);
                    TextView recordOdometer = detailedRecordView.findViewById(R.id.record_detail_odometer);
                    TextView recordNotes = detailedRecordView.findViewById(R.id.record_detail_notes);

                    recordTitle.setText(record.getTitle());
                    recordDate.setText(finalDate);
                    recordVehicle.setText(finalVehicleTitle);
                    StringBuilder odometerReading = new StringBuilder(record.getOdometer());
                    for (int i = odometerReading.length(); i > 0; i -= 3) {
                        if (i != odometerReading.length()) odometerReading.insert(i, ",");
                    }
                    odometerReading = new StringBuilder(odometerReading + " miles");
                    recordOdometer.setText(odometerReading);
                    if (!record.getDescription().isEmpty()) recordNotes.setText(record.getDescription());
                    else recordNotes.setText("-----");

                    dialogBuilder.setView(detailedRecordView);
                    dialog = dialogBuilder.create();
                    Objects.requireNonNull(dialog.getWindow()).getAttributes().windowAnimations = R.style.DialogAnim;
                    dialog.show();
                    dialog.setCancelable(true);

                    storageReference.child("documents").child("records").child("record_" + record.getRecordId()).listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
                        @Override
                        public void onSuccess(ListResult listResult) {
                            if (!listResult.getItems().isEmpty()) {
                                int PADDING = 5;
                                for (StorageReference item : listResult.getItems()) {
                                    documentLoading.setVisibility(View.VISIBLE);
                                    StorageReference documentRef = mStorage.getReference(item.getPath());
                                    documentRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            documentLoading.setVisibility(View.GONE);
                                            int width = ViewGroup.LayoutParams.MATCH_PARENT;
                                            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
                                            lp.setMargins(5, 5, 5, 5);
                                            lp.width = width;
                                            lp.height = height;

                                            TextView textView = new TextView(new ContextThemeWrapper(activity,R.style.MyAppTheme_H3));
                                            textView.setLayoutParams(lp);
                                            textView.setText(item.getName());
                                            recordDocumentsLayout.addView(textView);

                                            textView.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                                                    activity.startActivity(browserIntent);
                                                }
                                            });
                                            textView.setOnLongClickListener(new View.OnLongClickListener() {
                                                @Override
                                                public boolean onLongClick(View view) {
                                                    new MaterialAlertDialogBuilder(activity)
                                                            .setTitle("Delete File")
                                                            .setMessage("Would you like to delete this file?")
                                                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                                    documentRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void aVoid) {
                                                                            Toast.makeText(activity, "File successfully deleted!", Toast.LENGTH_SHORT).show();
                                                                            dialogInterface.dismiss();
                                                                            holder.recordView.setChecked(false);
                                                                            dialog.dismiss();
                                                                        }
                                                                    }).addOnFailureListener(new OnFailureListener() {
                                                                        @Override
                                                                        public void onFailure(@NonNull Exception exception) {
                                                                            Toast.makeText(activity, "Unable to delete file. Try again.", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });
                                                                }
                                                            })
                                                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                                    dialogInterface.dismiss();
                                                                }
                                                            })
                                                            .setCancelable(true)
                                                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                                @Override
                                                                public void onCancel(DialogInterface dialogInterface) {
                                                                    dialogInterface.cancel();
                                                                }
                                                            })
                                                            .show();
                                                    return true;
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        }
                    });
                    storageReference.child("images").child("records").child("record_" + record.getRecordId()).listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
                        @Override
                        public void onSuccess(ListResult listResult) {
                            if (!listResult.getItems().isEmpty()) {
                                int PADDING = 5;
                                for (StorageReference item : listResult.getItems()) {
                                    photoLoading.setVisibility(View.VISIBLE);
                                    StorageReference imageRef = mStorage.getReference(item.getPath());
                                    imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            photoLoading.setVisibility(View.GONE);
                                            int width = ViewGroup.LayoutParams.MATCH_PARENT;
                                            int height = ViewGroup.LayoutParams.MATCH_PARENT;
                                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                            lp.setMargins(5,5,5,5);
                                            lp.height = height;
                                            lp.width = width;
                                            MaterialCardView materialCardView = new MaterialCardView(activity);
                                            materialCardView.setLayoutParams(lp);

                                            ImageView imageView = new ImageView(detailedRecordView.getContext());
                                            imageView.setLayoutParams(lp);
                                            imageView.setAdjustViewBounds(true);
                                            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                            Glide.with(detailedRecordView.getContext())
                                                    .load(uri)
                                                    .placeholder(R.drawable.ic_photo_dark)
                                                    .into(imageView);
                                            materialCardView.addView(imageView);
                                            recordPhotosLayout.addView(materialCardView);

                                            imageView.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    MaterialAlertDialogBuilder imageBuilder = new MaterialAlertDialogBuilder(activity);
                                                    AlertDialog imageDialog;
                                                    LayoutInflater imageLi = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                                    @SuppressLint("InflateParams") final View fullImageView = imageLi.inflate(R.layout.image_viewer, null);

                                                    ImageView fullImage = fullImageView.findViewById(R.id.full_image);
                                                    Glide.with(detailedRecordView.getContext())
                                                            .load(uri)
                                                            .placeholder(R.drawable.ic_photo_dark)
                                                            .dontTransform()
                                                            .into(fullImage);

                                                    imageBuilder.setView(fullImageView);
                                                    imageDialog = imageBuilder.create();
                                                    Objects.requireNonNull(imageDialog.getWindow()).getAttributes().windowAnimations = R.style.DialogAnim;
                                                    int width = ViewGroup.LayoutParams.MATCH_PARENT;
                                                    int height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                                    imageDialog.getWindow().setLayout(width, height);
                                                    imageDialog.show();
                                                    imageDialog.setCancelable(true);

                                                    fullImage.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            imageDialog.dismiss();
                                                        }
                                                    });

                                                    imageDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                        @Override
                                                        public void onCancel(DialogInterface dialogInterface) {
                                                            dialogInterface.cancel();
                                                            imageDialog.dismiss();
                                                        }
                                                    });
                                                }
                                            });
                                            imageView.setOnLongClickListener(new View.OnLongClickListener() {
                                                @Override
                                                public boolean onLongClick(View view) {
                                                    new MaterialAlertDialogBuilder(activity)
                                                            .setTitle("Photo Options")
                                                            .setMessage("What would you like to do with this photo?")
                                                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                                    imageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void aVoid) {
                                                                            Toast.makeText(activity, "Photo successfully deleted!", Toast.LENGTH_SHORT).show();
                                                                            dialogInterface.dismiss();
                                                                            holder.recordView.setChecked(false);
                                                                            dialog.dismiss();
                                                                        }
                                                                    }).addOnFailureListener(new OnFailureListener() {
                                                                        @Override
                                                                        public void onFailure(@NonNull Exception exception) {
                                                                            Toast.makeText(activity, "Unable to delete photo. Try again.", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });
                                                                }
                                                            })
                                                            .setNegativeButton("Download", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialogInterface, int which) {
                                                                    final String[] imageType = {null};
                                                                    item.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                                                                        @Override
                                                                        public void onSuccess(StorageMetadata storageMetadata) {
                                                                            imageType[0] = storageMetadata.getContentType();
                                                                        }
                                                                    });
                                                                    try{
                                                                        DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
                                                                        Uri downloadUri = Uri.parse(String.valueOf(uri));
                                                                        DownloadManager.Request request = new DownloadManager.Request(downloadUri);
                                                                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                                                                                .setAllowedOverRoaming(false)
                                                                                .setTitle(item.getName())
                                                                                .setMimeType(imageType[0])
                                                                                .setVisibleInDownloadsUi(true)
                                                                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                                                .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES,File.separator + item.getName() + ".jpg");
                                                                        dm.enqueue(request);
                                                                        Toast.makeText(activity, "Photo download started.", Toast.LENGTH_SHORT).show();
                                                                    }catch (Exception e){
                                                                        Toast.makeText(activity, "Image download failed. Try again.", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                }
                                                            })
                                                            .setCancelable(true)
                                                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                                @Override
                                                                public void onCancel(DialogInterface dialogInterface) {
                                                                    dialogInterface.cancel();
                                                                }
                                                            })
                                                            .show();
                                                    return true;
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        }
                    });

                    addDocumentBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(activity, FileChooser.class);
                            intent.putExtra("record", record);
                            intent.putExtra("uploadType", "document");
                            activity.startActivity(intent);
                            holder.recordView.setChecked(false);
                            dialog.dismiss();
                        }
                    });
                    addPhotoBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(activity, FileChooser.class);
                            intent.putExtra("record", record);
                            intent.putExtra("uploadType", "photo");
                            activity.startActivity(intent);
                            holder.recordView.setChecked(false);
                            dialog.dismiss();
                        }
                    });

                    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            holder.recordView.setChecked(false);
                            dialogInterface.cancel();
                            dialog.dismiss();
                        }
                    });
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return recordArrayList.size();
    }

    public static class RecordViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView recordView;
        TextView recordTitle, recordDate, recordVehicle, recordOdometer, recordDescription;
        LinearLayout record_notes_layout;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            recordView = itemView.findViewById(R.id.record_material_card);
            recordTitle = itemView.findViewById(R.id.record_title);
            recordDate = itemView.findViewById(R.id.record_date);
            recordVehicle = itemView.findViewById(R.id.record_vehicle);
            recordOdometer = itemView.findViewById(R.id.record_odometer);
            recordDescription = itemView.findViewById(R.id.record_description);
            record_notes_layout = itemView.findViewById(R.id.record_notes_layout);
        }
    }

    private void setFadeAnimation(View view) {
        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(FADE_DURATION);
        view.startAnimation(anim);
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();
        userRef = mDatabase.getReference("users/" + mUser.getUid());
        storageReference = mStorage.getReference().child("users").child(mUser.getUid());
    }
}
