package jp.itigotti;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
	private TodoDAO dao;
	private static final Logger log = LoggerFactory.getLogger(Main.class);

	public void setDao(TodoDAO dao) {
		this.dao = dao;
	}

	@Override
	public void init() throws Exception {
		super.init();

		if(dao == null) {
			String appMode = System.getProperty("todo.app.mode");
			if("installer".equals(appMode)) {
				String logDir = System.getProperty("user.home") + "/.todo-app/logs";
				System.setProperty("todo.log.dir", logDir);
			}

			dao = new TodoDAO();
			dao.initializeDB();
		}
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
				log.error("コントローラーの初期化に失敗しました clazz={}", clazz.getName(), e);
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
		launch();
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		if(dao != null) {
			try {
				dao.close();
			} catch(Exception e) {
				log.error("DBコネクションのクローズに失敗しました", e);
			}
		}
	}

}
