/*
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
package org.cubeengine.module.vote.storage;

import static org.jooq.impl.SQLDataType.INTEGER;
import static org.jooq.impl.SQLDataType.TIMESTAMP;

import org.cubeengine.libcube.service.database.Table;
import org.cubeengine.libcube.util.Version;
import org.jooq.TableField;

import java.sql.Timestamp;
import java.util.UUID;

public class TableVote extends Table<VoteModel>
{
    public static TableVote TABLE_VOTE;

    public final TableField<VoteModel, UUID> ID = createField("userid", UUID_TYPE.nullable(false), this);
    public final TableField<VoteModel, Timestamp> LASTVOTE = createField("lastvote", TIMESTAMP.nullable(false), this);
    public final TableField<VoteModel, Integer> VOTEAMOUNT = createField("voteamount", INTEGER.nullable(false), this);

    public TableVote()
    {
        super(VoteModel.class, "votecount", new Version(1));
        this.setPrimaryKey(ID);
        this.addFields(ID, LASTVOTE, VOTEAMOUNT);
        TABLE_VOTE = this;
    }
}
