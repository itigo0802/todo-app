package jp.itigotti;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;

public class TodoDAO {
    private static final Logger LOGGER = Logger.getLogger(TodoDAO.class.getName());

    private static final String DB_URL;
    private static final Path DB_FILE;

    static {
        try {
            URL location = TodoDAO.class.getProtectionDomain().getCodeSource().getLocation();
            Path locationPath = Paths.get(location.toURI());

            DB_FILE = locationPath.resolve("todo.db").toAbsolutePath();
            DB_URL = "jdbc:sqlite:" + DB_FILE;

        } catch(URISyntaxException e) {
            throw new RuntimeException("データベースパスの取得に失敗しました", e);
        }
    }

    public void initializeDB() {
        createDbFileForOwnerOnly();
        try(Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "create table if not exists todo_items ("
                + "id integer primary key autoincrement, "
                + "task text not null, "
                + "expiration_date date, "
                + "is_completed boolean not null default false);";

            try(PreparedStatement pStmt = conn.prepareStatement(sql)) {
                pStmt.executeUpdate();
            }
        } catch(SQLException e) {
            throw new RuntimeException("DBの初期化に失敗しました", e);
        }
    }

    private static void createDbFileForOwnerOnly() {
        try {
            Path parent = DB_FILE.getParent();
            if(parent != null) {
                Files.createDirectories(parent);
            }
            if(Files.notExists(DB_FILE)) {
                Files.createFile(DB_FILE);
            }
            if(Files.getFileStore(DB_FILE).supportsFileAttributeView(PosixFileAttributeView.class)) {
                Files.setPosixFilePermissions(DB_FILE, PosixFilePermissions.fromString("rw-------"));
            }
        } catch(IOException | UnsupportedOperationException e) {
            LOGGER.log(Level.WARNING, "DBファイルのアクセス権の設定に失敗しました", e);
        }
    }

    public List<TodoItemModel> findAll() {
        List<TodoItemModel> todoList = new ArrayList<>();
        try(Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "select * from todo_items";

            try(PreparedStatement pStmt = conn.prepareStatement(sql);
                ResultSet rs = pStmt.executeQuery()) {

                while(rs.next()) {
                    TodoItemModel item = new TodoItemModel();
                    item.setId(rs.getInt("id"));
                    item.setTask(rs.getString("task"));
                    item.setExpirationDate(rs.getObject("expiration_date", LocalDate.class));
                    item.setCompleted(rs.getBoolean("is_completed"));
                    todoList.add(item);
                }
            }
        } catch(SQLException e) {
            throw new RuntimeException("DBの読み込みに失敗しました", e);
        }
        return todoList;
    }

    public TodoItemModel create(TodoItemModel item) {
        try(Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "insert into todo_items (task, expiration_date) values ("
                + "?, "
                + "?);";
            
            try(PreparedStatement pStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pStmt.setString(1, item.getTask());

                if(item.getExpirationDate() != null) {
                    pStmt.setDate(2, Date.valueOf(item.getExpirationDate()));
                } else {
                    pStmt.setNull(2, Types.DATE);
                }

                if(pStmt.executeUpdate() == 1) {
                    try(ResultSet rs = pStmt.getGeneratedKeys()) {
                        if(rs.next()) {
                            item.setId(rs.getInt(1));
                        }
                    }
                    return item;
                } else {
                    return null;
                }
            }
        } catch(SQLException e) {
            throw new RuntimeException("DBへの登録に失敗しました", e);
        }
    }

    public TodoItemModel update(TodoItemModel item) {
        try(Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "update todo_items set task = ?, "
                + "expiration_date = ?, "
                + "is_completed = ? "
                + "where id = ?;";
            
            try(PreparedStatement pStmt = conn.prepareStatement(sql)) {
                pStmt.setString(1, item.getTask());
                if(item.getExpirationDate() != null) {
                    pStmt.setDate(2, Date.valueOf(item.getExpirationDate()));
                } else {
                    pStmt.setNull(2, Types.DATE);
                }
                pStmt.setBoolean(3, item.isCompleted());
                pStmt.setInt(4, item.getId());

                if(pStmt.executeUpdate() == 1) {
                    return item;
                } else {
                    return null;
                }
            }
        } catch(SQLException e) {
            throw new RuntimeException("DBへの更新に失敗しました", e);
        }
    }

    public boolean delete(TodoItemModel item) {
        try(Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "delete from todo_items where id = ?;";

            try(PreparedStatement pStmt = conn.prepareStatement(sql)) {
                pStmt.setInt(1, item.getId());

                return pStmt.executeUpdate() == 1;
            }
        } catch(SQLException e) {
            throw new RuntimeException("DBへの削除に失敗しました", e);
        }
    }
}
