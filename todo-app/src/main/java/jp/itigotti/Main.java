package jp.itigotti;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
	private TodoDAO dao;

	@Override
	public void init() throws Exception {
		super.init();

		dao = new TodoDAO();
		dao.initializeDB();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(Main.class.getResource("scene.fxml"));
		loader.setControllerFactory(clazz -> {
			if (clazz == Controller.class) {
				return new Controller(dao);
			}
			try {
				return clazz.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		Scene scene = new Scene(loader.load());
		primaryStage.setTitle("Todo App");
		primaryStage.setScene(scene);
		scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
		primaryStage.show();
	}

	public static void main(String[] args) {
		System.setProperty("jdk.gtk.version", "3");
		System.setProperty("glass.platform", "Gtk");
		System.setProperty("file.encoding", "UTF-8");
		launch();
	}

}
