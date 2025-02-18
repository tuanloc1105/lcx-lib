package vn.com.lcx.common.database.pool.entry;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.com.lcx.common.database.type.DBTypeEnum;
import vn.com.lcx.common.utils.DateTimeUtils;
import vn.com.lcx.common.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConnectionEntry implements AutoCloseable {

    @Setter
    private Connection connection;

    @Setter(AccessLevel.PRIVATE)
    private LocalDateTime lastActiveTime;

    @Setter(AccessLevel.PRIVATE)
    private DBTypeEnum dbType;

    @Setter(AccessLevel.PRIVATE)
    private String connectionName;

    @Setter(AccessLevel.PRIVATE)
    private File file;

    @Setter(AccessLevel.PRIVATE)
    private FileChannel channel;

    @Setter(AccessLevel.PRIVATE)
    private FileLock lock;

    @Setter(AccessLevel.PRIVATE)
    private Logger connectionLog;

    @Setter(AccessLevel.PRIVATE)
    private boolean idle;

    @Setter
    private boolean criticalLock;


    public static ConnectionEntry init(Connection connection,
                                       DBTypeEnum dbType,
                                       String connectionName) {
        val folder = new File(FileUtils.pathJoining(System.getProperty("java.io.tmpdir"), "lcx-pool"));
        //noinspection ResultOfMethodCallIgnored
        folder.mkdirs();
        val file = new File(FileUtils.pathJoining(System.getProperty("java.io.tmpdir"), "lcx-pool", String.format("%s.lock", connectionName)));
        val fileIsNotExist = !file.exists();
        if (fileIsNotExist) {
            try {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        val logger = LoggerFactory.getLogger(connectionName);
        val entry = new ConnectionEntry(
                connection,
                DateTimeUtils.generateCurrentTimeDefault(),
                dbType,
                connectionName,
                file,
                null,
                null,
                logger,
                true,
                false
        );

        logger.info("Add new connection entry: {}", entry);
        return entry;
    }

    public synchronized void lock() {
        try {
            if (this.lock != null) {
                throw new RuntimeException("Connection is activating");
            }
            if (this.channel == null) {
                this.channel = FileChannel.open(Paths.get(file.getAbsolutePath()), StandardOpenOption.WRITE);
            }
            this.lock = this.channel.tryLock();
            if (this.lock == null) {
                throw new RuntimeException("Cannot acquire lock on connection");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized boolean isActive() {
        return this.lock != null;
    }

    public synchronized void releaseLock() {
        if (this.lock == null) {
            this.connectionLog.info("Connection is not being locked");
            return;
        }
        try {
            this.lock.release();
            this.lock.close();
            this.channel.close();
            this.lock = null;
            this.channel = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void activate() {
        if (this.isIdle()) {
            this.lock();
            this.lastActiveTime = DateTimeUtils.generateCurrentTimeDefault();
            this.setIdle(false);
            this.getConnectionLog().info("Activated connection entry: {}", this);
            return;
        }
        throw new RuntimeException("Connection is not idling");
    }

    public void deactivate() {
        if (this.isIdle()) {
            throw new RuntimeException("Connection is idling");
        }
        this.lastActiveTime = DateTimeUtils.generateCurrentTimeDefault();
        if (transactionIsOpen()) {
            this.commit();
        }
        this.setIdle(true);
        this.releaseLock();
        this.getConnectionLog().info("Deactivated connection entry: {}", this);
    }

    public boolean transactionIsOpen() {
        boolean result = false;
        try {
            result = !this.connection.getAutoCommit();
        } catch (Exception e) {
            this.connectionLog.error(e.getMessage(), e);
        }
        return result;
    }

    public void openTransaction() {
        try {
            if (this.transactionIsOpen()) {
                return;
            }
            connection.setAutoCommit(false);
            this.connectionLog.info("Open transaction");
        } catch (SQLException e) {
            this.connectionLog.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        try {
            if (this.transactionIsOpen()) {
                this.connection.commit();
                this.connection.setAutoCommit(true);
                this.connectionLog.info("Committed");
            }
        } catch (SQLException e) {
            this.connectionLog.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void rollback() {
        try {
            if (this.transactionIsOpen()) {
                this.connection.rollback();
                this.connection.setAutoCommit(true);
                this.connectionLog.info("Rolled back");
            }
        } catch (SQLException e) {
            this.connectionLog.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public boolean isValid() {
        try {
            return this.connection.isValid(10);
        } catch (SQLException e) {
            this.connectionLog.error(e.getMessage(), e);
            return false;
        }
    }

    public void shutdown() {
        try {
            this.connection.close();
        } catch (SQLException e) {
            this.connectionLog.error(e.getMessage(), e);
        }
        this.connection = null;
    }

    @Override
    public void close() {
        this.deactivate();
    }

}
