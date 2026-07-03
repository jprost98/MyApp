package com.example.myapp.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "vehicles")
public class Vehicle implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    private int vehicleId;
    @ColumnInfo(name = "year")
    private String year;
    @ColumnInfo(name = "make")
    private String make;
    @ColumnInfo(name = "model")
    private String model;
    @ColumnInfo(name = "submodel")
    private String submodel;
    @ColumnInfo(name = "engine")
    private String engine;
    @ColumnInfo(name = "notes")
    private String notes;
    @ColumnInfo(name = "entry_time")
    private Long entryTime;

    public Vehicle(int vehicleId, String year, String make, String model, String submodel, String engine, String notes, Long entryTime) {
        this.vehicleId = vehicleId;
        this.year = year;
        this.make = make;
        this.model = model;
        this.submodel = submodel;
        this.engine = engine;
        this.notes = notes;
        this.entryTime = entryTime;
    }

    @Ignore
    public Vehicle() {
        //
    }

    protected Vehicle(Parcel in) {
        vehicleId = in.readInt();
        year = in.readString();
        make = in.readString();
        model = in.readString();
        submodel = in.readString();
        engine = in.readString();
        notes = in.readString();
        entryTime = in.readLong();
    }

    public static final Creator<Vehicle> CREATOR = new Creator<Vehicle>() {
        @Override
        public Vehicle createFromParcel(Parcel in) {
            return new Vehicle(in);
        }

        @Override
        public Vehicle[] newArray(int size) {
            return new Vehicle[size];
        }
    };

    public int getVehicleId() {
        return vehicleId;
    }
    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getYear() {
        return year;
    }
    public void setYear(String year) {
        this.year = year;
    }

    public String getMake() {
        return make;
    }
    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }
    public void setModel(String model) {
        this.model = model;
    }

    public String getSubmodel() {
        return submodel;
    }
    public void setSubmodel(String submodel) {
        this.submodel = submodel;
    }

    public String getEngine() {
        return engine;
    }
    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getEntryTime() {
        return entryTime;
    }
    public void setEntryTime(Long entryTime) {
        this.entryTime = entryTime;
    }

    public String vehicleTitle() {
        return year + " " + make + " " + model + " " + submodel;
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "vehicleId=" + vehicleId +
                ", year='" + year + '\'' +
                ", make='" + make + '\'' +
                ", model='" + model + '\'' +
                ", submodel='" + submodel + '\'' +
                ", engine='" + engine + '\'' +
                ", notes='" + notes + '\'' +
                ", entryTime='" + entryTime + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(vehicleId);
        dest.writeString(year);
        dest.writeString(make);
        dest.writeString(model);
        dest.writeString(engine);
        dest.writeString(notes);
        dest.writeLong(entryTime);
    }
}
