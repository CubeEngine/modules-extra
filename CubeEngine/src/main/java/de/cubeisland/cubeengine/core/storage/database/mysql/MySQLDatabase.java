package de.cubeisland.cubeengine.core.storage.database.mysql;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import de.cubeisland.cubeengine.core.storage.database.AbstractDatabase;
import de.cubeisland.cubeengine.core.storage.database.DatabaseConfiguration;
import de.cubeisland.cubeengine.core.storage.database.querybuilder.QueryBuilder;
import de.cubeisland.cubeengine.core.util.log.LogLevel;
import org.apache.commons.lang.Validate;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * MYSQLDatabase the MYSQL implementation for the database.
 */
public class MySQLDatabase extends AbstractDatabase
{
    private static final char NAME_QUOTE   = '`';
    private static final char STRING_QUOTE = '\'';

    private final MysqlDataSource ds;
    private final String            tablePrefix;
    private final MySQLQueryBuilder queryBuilder;
    private final Thread creationThread = Thread.currentThread();
    private Connection connection;

    public MySQLDatabase(DatabaseConfiguration configuration) throws SQLException
    {
        MySQLDatabaseConfiguration config = (MySQLDatabaseConfiguration)configuration;

        this.ds = new MysqlDataSource();
        this.ds.setServerName(config.host);
        this.ds.setPort(config.port);
        this.ds.setUser(config.user);
        this.ds.setPassword(config.pass);
        this.ds.setDatabaseName(config.database);
        this.ds.setConnectionCollation("utf8_general_ci");
        this.connection = null;

        this.tablePrefix = config.tablePrefix;
        this.queryBuilder = new MySQLQueryBuilder(this);
    }

    public Connection getConnection() throws SQLException
    {
        if (this.connection == null || connection.isClosed())
        {
            this.clearStatementCache();
            this.connection = this.ds.getConnection();
        }
        else if (!this.connection.isValid(500))
        {
            this.clearStatementCache();
            this.connection.close();
            this.connection = this.ds.getConnection();
        }
        return this.connection;
    }

    public DatabaseMetaData getMetaData() throws SQLException
    {
        return this.getConnection().getMetaData();
    }

    @Override
    public String getName()
    {
        return "MySQL";
    }

    @Override
    public QueryBuilder getQueryBuilder()
    {
        if (Thread.currentThread() != this.creationThread)
        {
            throw new IllegalStateException("This method may only be called from the thread the database was created in!");
        }
        return this.queryBuilder;
    }

    @Override
    public String prepareTableName(String name)
    {
        Validate.notNull(name, "The name must not be null!");

        return NAME_QUOTE + this.tablePrefix + name + NAME_QUOTE;
    }

    @Override
    public String prepareFieldName(String name)
    {
        Validate.notNull(name, "The name must not be null!");

        int dotOffset = name.indexOf('.');
        if (dotOffset >= 0)
        {
            return this.prepareTableName(name.substring(0, dotOffset)) + '.' + NAME_QUOTE + name.substring(dotOffset + 1) + NAME_QUOTE;
        }
        return NAME_QUOTE + name + NAME_QUOTE;
    }

    @Override
    public String prepareString(String name)
    {
        return STRING_QUOTE + name + STRING_QUOTE;
    }

    public void shutdown()
    {
        super.shutdown();
        try
        {
            this.ds.getConnection().close();
        }
        catch (SQLException e)
        {
            LOGGER.log(LogLevel.ERROR, e.getMessage(), e);
        }
    }
}
