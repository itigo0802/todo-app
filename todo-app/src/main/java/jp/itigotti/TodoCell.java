package jp.itigotti;

import java.io.IOException;
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

    @Override
    protected void updateItem(TodoItemModel item, boolean empty) {
        super.updateItem(item, empty);

        if(empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            if(loader == null) {
                loader = new FXMLLoader(getClass().getResource("todoItemCell.fxml"));
                loader.setController(this);
                try {
                    loader.load();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }

            taskLabel.setText(item.getTask());
            expirationLabel.setText(item.getExpirationDate().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));

            isCompletedCheckBox.setSelected(item.getIsCompleted());

            setText(null);
            setGraphic(hBox);
        }
    }

    @FXML
    private void handleIsCompletedAction() {
        TodoItemModel item = getItem();
        if(item != null) {
            item.setIsCompleted(isCompletedCheckBox.isSelected());
        }
    }
}
