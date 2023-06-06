package com.example.myapp.data;

public class Task {
    private String taskName, taskVehicle, taskLastDone, taskDueDate, taskFrequency, taskNotes, taskType;
    private Long entryTime;

    public Task(String taskName, String taskVehicle, String taskLastDone, String taskDueDate, String taskFrequency, String taskNotes, String taskType, Long entryTime) {
        this.taskName = taskName;
        this.taskVehicle = taskVehicle;
        this.taskLastDone = taskLastDone;
        this.taskDueDate = taskDueDate;
        this.taskFrequency = taskFrequency;
        this.taskNotes = taskNotes;
        this.entryTime = entryTime;
        this.taskType = taskType;
    }

    public Task() {
        //
    }

    public String getTaskName() {
        return taskName;
    }
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    public String getTaskVehicle() {
        return taskVehicle;
    }
    public void setTaskVehicle(String taskVehicle) {
        this.taskVehicle = taskVehicle;
    }
    public String getTaskLastDone() {
        return taskLastDone;
    }
    public void setTaskLastDone(String taskLastDone) {
        this.taskLastDone = taskLastDone;
    }
    public String getTaskDueDate() {
        return taskDueDate;
    }
    public void setTaskDueDate(String taskDueDate) {
        this.taskDueDate = taskDueDate;
    }
    public String getTaskFrequency() {
        return taskFrequency;
    }
    public void setTaskFrequency(String taskFrequency) {
        this.taskFrequency = taskFrequency;
    }
    public String getTaskNotes() {
        return taskNotes;
    }
    public void setTaskNotes(String taskNotes) {
        this.taskNotes = taskNotes;
    }
    public String getTaskType() {
        return taskType;
    }
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
    public Long getEntryTime() {
        return entryTime;
    }
    public void setEntryTime(Long entryTime) {
        this.entryTime = entryTime;
    }

    @Override
    public String toString() {
        return "Task{" +
                "name='" + taskName + '\'' +
                ", vehicle='" + taskVehicle + '\'' +
                ", last_done='" + taskLastDone + '\'' +
                ", due_date='" + taskDueDate + '\'' +
                ", frequency='" + taskFrequency + '\'' +
                ", notes='" + taskNotes + '\'' +
                ", type='" + taskType + '\'' +
                ", entry_time=" + entryTime +
                '}';
    }
}
