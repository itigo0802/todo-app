package jp.itigotti;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;

public class TodoCell extends ListCell<TodoItemModel> {
    @FXML private HBox hBox;
    @FXML private CheckBox isCompletedCheckBox;
    @FXML private Label taskLabel;
    @FXML private Label expirationLabel;

    private FXMLLoader loader;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public TodoCell() {

        try {
            loader = new FXMLLoader(getClass().getResource("todoItemCell.fxml"));
            loader.setController(this);
            loader.load();
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected void updateItem(TodoItemModel item, boolean empty) {
        super.updateItem(item, empty);

        if(empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            taskLabel.setText(item.getTask());
            if(item.getExpirationDate() != null) {
                expirationLabel.setText(item.getExpirationDate().format(FORMATTER));
            } else {
                expirationLabel.setText("");
            }
            isCompletedCheckBox.setSelected(item.isCompleted());
        }

        setText(null);
        setGraphic(hBox);
        
    }

    @FXML
    private void handleIsCompletedAction() {
        TodoItemModel item = getItem();
        if(item != null) {
            item.setIsCompleted(isCompletedCheckBox.isSelected());
        }
    }
}
