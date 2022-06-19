package com.example.myapp.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "records")
public class Record implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    private int recordId;
    @ColumnInfo(name = "title")
    private String title;
    @ColumnInfo(name = "description")
    private String description;
    @ColumnInfo(name = "odometer")
    private String odometer;
    @ColumnInfo(name = "date")
    private String date;
    @ColumnInfo(name = "vehicle")
    private String vehicle;

    public Record(String title, String description, String odometer, String date, String vehicle) {
        this.title = title;
        this.description = description;
        this.odometer = odometer;
        this.date = date;
        this.vehicle = vehicle;
    }

    @Ignore
    public Record() {
        //
    }

    protected Record(Parcel in) {
        recordId = in.readInt();
        title = in.readString();
        description = in.readString();
        odometer = in.readString();
        date = in.readString();
        vehicle = in.readString();
    }

    public static final Creator<Record> CREATOR = new Creator<Record>() {
        @Override
        public Record createFromParcel(Parcel in) {
            return new Record(in);
        }
        @Override
        public Record[] newArray(int size) {
            return new Record[size];
        }
    };

    public int getRecordId() {
        return recordId;
    }
    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getOdometer() {
        return odometer;
    }
    public void setOdometer(String odometer) {
        this.odometer = odometer;
    }

    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }

    public String getVehicle() {
        return vehicle;
    }
    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    @Override
    public String toString() {
        return "Record{" +
                "recordId=" + recordId +
                ", title='" + title + '\'' +
                ", date='" + date + '\'' +
                ", vehicle='" + vehicle + '\'' +
                ", odometer='" + odometer + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(recordId);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(odometer);
        dest.writeString(date);
        dest.writeString(vehicle);
    }
}
