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
package org.cubeengine.module.vote.storage;

import java.sql.Timestamp;
import org.cubeengine.service.database.AsyncRecord;
import org.cubeengine.service.user.User;
import org.jooq.types.UShort;

import static org.cubeengine.module.vote.storage.TableVote.TABLE_VOTE;

public class VoteModel extends AsyncRecord<VoteModel>
{
    public VoteModel()
    {
        super(TABLE_VOTE);
    }

    public VoteModel newVote(User user)
    {
        this.setValue(TABLE_VOTE.USERID, user.getEntity().getId());
        this.setValue(TABLE_VOTE.LASTVOTE, new Timestamp(System.currentTimeMillis()));
        this.setValue(TABLE_VOTE.VOTEAMOUNT, UShort.valueOf(1));
        return this;
    }

    public void setLastNow()
    {
        setValue(TABLE_VOTE.LASTVOTE, new Timestamp(System.currentTimeMillis()));
    }

    public void setVotes(int amount)
    {
        setValue(TABLE_VOTE.VOTEAMOUNT, UShort.valueOf(amount));
        setLastNow();
    }

    public int getVotes()
    {
        return getValue(TABLE_VOTE.VOTEAMOUNT).intValue();
    }

    public void addVote()
    {
        setVotes(getVotes() + 1);
    }

    public boolean timePassed(long duration)
    {
        return System.currentTimeMillis() - getLastVote() > duration;
    }

    public long getLastVote()
    {
        return getValue(TABLE_VOTE.LASTVOTE).getTime();
    }
}
