package jp.itigotti;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TodoDAO implements AutoCloseable {
    private static final String DB_URL;
    private static final String DB_PATH;
    private static final Logger log = LoggerFactory.getLogger(TodoDAO.class);
    private final HikariDataSource dSource;

    public TodoDAO() {
        this.dSource = createDataSource(DB_URL);
    }

    public TodoDAO(String customDbUrl) {
        this.dSource = createDataSource(customDbUrl);
    }

    @Override
    public void close() throws Exception {
        if(dSource != null && !dSource.isClosed()) {
            dSource.close();
        }
    }

    private static HikariDataSource createDataSource(String jdbcUrl) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        return new HikariDataSource(config);
    }

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            log.error("SQLite JDBCドライバのロードに失敗しました driverClass={}", "org.sqlite.JDBC", e);
            throw new RuntimeException("SQLite JDBCのロードに失敗しました", e);
        }

        String appMode = System.getProperty("todo.app.mode");

        if("installer".equals(appMode)) {
            String userHome = System.getProperty("user.home");
            if(userHome == null) {
                throw new RuntimeException("ユーザーホームディレクトリが取得できません");
            }

            Path homeDir = Paths.get(userHome, ".todo-app");
            try {
                Files.createDirectories(homeDir);
            } catch (IOException e) {
                throw new RuntimeException("ホームディレクトリを作成できませんでした", e);
            }
            DB_PATH = homeDir.resolve("todo.db").toString();
        } else {
            Path currentDir = Paths.get(".").toAbsolutePath();
            DB_PATH = currentDir.resolve("todo.db").toString();
        }
        
        DB_URL = "jdbc:sqlite:" + DB_PATH;
    }

    public void initializeDB() {
        try (Connection conn = dSource.getConnection()) {
            String sql = "create table if not exists todo_items ("
                    + "id integer primary key autoincrement, "
                    + "task text not null, "
                    + "expiration_date date not null, "
                    + "is_completed boolean not null default false);";

            try (PreparedStatement pStmt = conn.prepareStatement(sql)) {
                pStmt.executeUpdate();
            }

            // 既存DBのNULL行を当日付で補完するマイグレーション
            String migrateSql = "update todo_items set expiration_date = ? where expiration_date is null";
            try(PreparedStatement pStmt = conn.prepareStatement(migrateSql)) {
                pStmt.setDate(1, Date.valueOf(LocalDate.now()));
                int updated = pStmt.executeUpdate();
                if(updated > 0) {
                    log.info("expiration_dateがnullの行を当日付で補完しました count={}", updated);
                }
            }
        } catch (SQLException e) {
            log.error("DBの初期化に失敗しました dbUrl={}, SQLState={}", e.getSQLState(), e);
            throw new RuntimeException("DBの初期化に失敗しました", e);
        }
    }

    public List<TodoItemModel> findAll() {
        List<TodoItemModel> todoList = new ArrayList<>();
        try (Connection conn = dSource.getConnection()) {
            String sql = "select * from todo_items order by expiration_date asc, id asc";

            try (PreparedStatement pStmt = conn.prepareStatement(sql);
                ResultSet rs = pStmt.executeQuery()) {

                while (rs.next()) {
                    TodoItemModel item = new TodoItemModel();
                    item.setId(rs.getInt("id"));
                    item.setTask(rs.getString("task"));
                    item.setExpirationDate(rs.getObject("expiration_date", LocalDate.class));
                    item.setCompleted(rs.getBoolean("is_completed"));
                    todoList.add(item);
                }
            }
        } catch (SQLException e) {
            log.error("Todo一覧の取得に失敗しました SQLState={}", e.getSQLState(), e);
            throw new RuntimeException("Todo一覧の取得に失敗しました", e);
        }
        return todoList;
    }

    public TodoItemModel create(TodoItemModel item) {

        if(item.getExpirationDate() == null) {
            throw new IllegalArgumentException("expiration_dateはnullにできません");
        }

        try (Connection conn = dSource.getConnection()) {
            String sql = "insert into todo_items (task, expiration_date, is_completed) values ("
                    + "?,"
                    + "?, "
                    + "?);";

            try (PreparedStatement pStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pStmt.setString(1, item.getTask());
                pStmt.setDate(2, Date.valueOf(item.getExpirationDate()));
                pStmt.setBoolean(3, false);

                if (pStmt.executeUpdate() == 1) {
                    try (ResultSet rs = pStmt.getGeneratedKeys()) {
                        if (rs.next()) {
                        item.setId(rs.getInt(1));
                        }
                    }
                    return item;
                } else {
                    return null;
                }
            
            }
        } catch (SQLException e) {
            log.error("Todoの登録に失敗しました task={}, expirationDate={}, SQLState={}", StringUtil.truncateForLog(item.getTask()), item.getExpirationDate(), e.getSQLState(), e);
            throw new RuntimeException("Todoの登録に失敗しました", e);
        }
    }

    public TodoItemModel update(TodoItemModel item) {
        try (Connection conn = dSource.getConnection()) {
            String sql = "update todo_items set task = ?, "
                    + "expiration_date = ?, "
                    + "is_completed = ? "
                    + "where id = ?;";

            try (PreparedStatement pStmt = conn.prepareStatement(sql)) {
                pStmt.setString(1, item.getTask());
                pStmt.setDate(2, Date.valueOf(item.getExpirationDate()));
                pStmt.setBoolean(3, item.isCompleted());
                pStmt.setInt(4, item.getId());

                if (pStmt.executeUpdate() == 1) {
                    return item;
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            log.error("Todoの更新に失敗しました id={}, task={}, completed={}, SQLState={}", item.getId(), StringUtil.truncateForLog(item.getTask()), item.isCompleted(), e.getSQLState(), e);
            throw new RuntimeException("DBへの更新に失敗しました", e);
        }
    }

    public boolean delete(TodoItemModel item) {
        try (Connection conn = dSource.getConnection()) {
            String sql = "delete from todo_items where id = ?;";

            try (PreparedStatement pStmt = conn.prepareStatement(sql)) {
                pStmt.setInt(1, item.getId());

                return pStmt.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            log.error("Todoの削除に失敗しました id={}, SQLState={}", item.getId(), e.getSQLState(), e);
            throw new RuntimeException("DBへの削除に失敗しました", e);
        }
    }
}
