package jp.itigotti;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

	@Override
	public void init() throws Exception {
		super.init();
		
		TodoDAO dao = new TodoDAO();
		dao.initializeDB();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(Main.class.getResource("scene.fxml"));
		Scene scene = new Scene(loader.load());
		primaryStage.setTitle("Todo App");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch();
	}

}
