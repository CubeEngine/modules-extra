/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.cubeisland.engine.locker.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import de.cubeisland.engine.core.storage.database.Database;
import de.cubeisland.engine.core.storage.database.TableCreator;
import de.cubeisland.engine.core.storage.database.mysql.Keys;
import de.cubeisland.engine.core.storage.database.mysql.MySQLDatabaseConfiguration;
import de.cubeisland.engine.core.user.UserEntity;
import de.cubeisland.engine.core.util.Version;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.core.user.TableUser.TABLE_USER;

public class TableLocks extends TableImpl<LockModel> implements TableCreator<LockModel>
{
    public static TableLocks TABLE_GUARD;

    private TableLocks(String prefix)
    {
        super(prefix + "locks");
        IDENTITY = Keys.identity(this, this.ID);
        PRIMARY_KEY = Keys.uniqueKey(this, this.ID);
        UNIQUE_ENTITY_UID = Keys.uniqueKey(this, this.ENTITY_UID_LEAST, this.ENTITY_UID_MOST);
        FOREIGN_OWNER = Keys.foreignKey(TABLE_USER.PRIMARY_KEY, this, this.OWNER_ID);
    }

    public static TableLocks initTable(Database database)
    {
        MySQLDatabaseConfiguration config = (MySQLDatabaseConfiguration)database.getDatabaseConfig();
        TABLE_GUARD = new TableLocks(config.tablePrefix);
        return TABLE_GUARD;
    }

    @Override
    public void createTable(Connection connection) throws SQLException
    {
        connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + this.getName()+ " (\n" +
                                        "`id` int(10) unsigned NOT NULL AUTO_INCREMENT,\n" +
                                        "`owner_id` int(10) unsigned NOT NULL,\n" +
                                        "`flags` smallint NOT NULL,\n" +
                                        "`type` tinyint NOT NULL,\n" +
                                        "`lock_type` tinyint NOT NULL,\n" +
                                        "`password` varbinary(128) NOT NULL,\n" +
                                        "`droptransfer` tinyint(1) NOT NULL,\n" +
                                        "`entity_uid_least` bigint DEFAULT NULL,\n" +
                                        "`entity_uid_most` bigint DEFAULT NULL,\n" +
                                        "`last_access` DATETIME NOT NULL,\n" +
                                        "`created` DATETIME NOT NULL,\n" +
                                        "PRIMARY KEY (`id`),\n" +
                                        "UNIQUE KEY (`entity_uid_least`, `entity_uid_most`),\n" +
                                        "FOREIGN KEY f_owner (`owner_id`) REFERENCES " + TABLE_USER.getName() + " (`key`) ON UPDATE CASCADE ON DELETE CASCADE)\n" +
                                        "ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci\n" +
                                        "COMMENT='1.0.0'").execute();
    }

    private static final Version version = new Version(1);

    @Override
    public Version getTableVersion()
    {
        return version;
    }

    public final Identity<LockModel, UInteger> IDENTITY;
    public final UniqueKey<LockModel> PRIMARY_KEY;
    public final UniqueKey<LockModel> UNIQUE_ENTITY_UID;
    public final ForeignKey<LockModel, UserEntity> FOREIGN_OWNER;

    public final TableField<LockModel, UInteger> ID = createField("id", SQLDataType.INTEGERUNSIGNED, this);
    public final TableField<LockModel, UInteger> OWNER_ID = createField("owner_id", SQLDataType.INTEGERUNSIGNED, this);

    /**
     * Flags see {@link ProtectionFlag}
     */
    public final TableField<LockModel, Short> FLAGS = createField("flags", SQLDataType.SMALLINT, this);

    /**
     * Protected Type see {@link ProtectedType}
     */
    public final TableField<LockModel, Byte> PROTECTED_TYPE = createField("type", SQLDataType.TINYINT, this);

    /**
     * LockType see {@link LockType}
     */
    public final TableField<LockModel, Byte> LOCK_TYPE = createField("lock_type", SQLDataType.TINYINT, this);

    // eg. /cguarded [pass <password>] (flag to create pw book/key?)
    public final TableField<LockModel, byte[]> PASSWORD = createField("password", SQLDataType.VARBINARY.length(128), this);

    // optional for entity protection:
    public final TableField<LockModel, Long> ENTITY_UID_LEAST = createField("entity_uid_least", SQLDataType.BIGINT, this);
    public final TableField<LockModel, Long> ENTITY_UID_MOST = createField("entity_uid_most", SQLDataType.BIGINT, this);

    public final TableField<LockModel, Timestamp> LAST_ACCESS = createField("last_access", SQLDataType.TIMESTAMP, this);
    public final TableField<LockModel, Timestamp> CREATED = createField("created", SQLDataType.TIMESTAMP, this);

    @Override
    public Identity<LockModel, UInteger> getIdentity()
    {
        return IDENTITY;
    }

    @Override
    public UniqueKey<LockModel> getPrimaryKey()
    {
        return PRIMARY_KEY;
    }

    @Override
    public List<UniqueKey<LockModel>> getKeys()
    {
        return Arrays.asList(PRIMARY_KEY, UNIQUE_ENTITY_UID);
    }

    @Override
    public List<ForeignKey<LockModel, ?>> getReferences() {
        return Arrays.<ForeignKey<LockModel, ?>>asList(FOREIGN_OWNER);
    }

    @Override
    public Class<LockModel> getRecordType() {
        return LockModel.class;
    }
}