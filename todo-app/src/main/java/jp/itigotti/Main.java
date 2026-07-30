package jp.itigotti;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

public class Main extends Application {
	private static final Logger LOGGER = System.getLogger(Main.class.getName());

	@Override
	public void init() throws Exception {
		super.init();
		
		TodoDAO dao = new TodoDAO();
		dao.initializeDB();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> showFatalError(throwable));

		try {
			URL sceneUrl = Main.class.getResource("scene.fxml");
			if(sceneUrl == null) {
				throw new IllegalStateException("画面定義ファイル scene.fxml が見つかりません");
			}

			FXMLLoader loader = new FXMLLoader(sceneUrl);
			Scene scene = new Scene(loader.load());
			primaryStage.setTitle("Todo App");
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) {
			showFatalError(e);
			throw e;
		}
	}

	private static void showFatalError(Throwable throwable) {
		LOGGER.log(Level.ERROR, "予期しないエラーが発生しました", throwable);

		var alert = new Alert(AlertType.ERROR);
		alert.setTitle("エラー");
		alert.setHeaderText("予期しないエラーが発生しました");
		alert.setContentText(throwable.getMessage());
		alert.showAndWait();
	}

	public static void main(String[] args) {
		launch();
	}

}
