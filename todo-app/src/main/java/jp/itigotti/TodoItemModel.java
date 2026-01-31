package jp.itigotti;

import java.time.LocalDate;

import javafx.beans.property.*;

public class TodoItemModel {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty task = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> expirationDate = new SimpleObjectProperty<>();
    private final BooleanProperty completed = new SimpleBooleanProperty();

    public int getId() {
        return id.get();
    }
    public void setId(int id) {
        this.id.set(id);
    }
    public IntegerProperty idProperty() {
        return id;
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

    public boolean isCompleted() {
        return completed.get();
    }
    public void setCompleted(boolean completed) {
        this.completed.set(completed);
    }
    public BooleanProperty completedProperty() {
        return completed;
    }

}
