package jp.itigotti;

import java.time.LocalDate;

import javafx.beans.property.*;

public class TodoItemModel {
    private final StringProperty task = new SimpleStringProperty();
    private final  ObjectProperty<LocalDate> expirationDate = new SimpleObjectProperty<>();
    private final BooleanProperty isCompleted = new SimpleBooleanProperty();

    public TodoItemModel(String task, LocalDate expirationDate) {
        this.task.set(task);
        this.expirationDate.set(expirationDate);
        this.isCompleted.set(false);
    }

    public String getTask() {
        return task.get();
    }
    public void setTask(String task) {
        this.task.set(task);
    }
    public StringProperty taskProperty() {
        return task;
    }

    public LocalDate getExpirationDate() {
        return expirationDate.get();
    }
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate.set(expirationDate);
    }
    public ObjectProperty<LocalDate> expirationDateProperty() {
        return expirationDate;
    }

    public boolean getIsCompleted() {
        return isCompleted.get();
    }
    public void setIsCompleted(boolean isCompleted) {
        this.isCompleted.set(isCompleted);
    }
    public BooleanProperty isCompletedProperty() {
        return isCompleted;
    }

}
