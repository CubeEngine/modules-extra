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
package de.cubeisland.engine.module.chat;

import java.sql.Date;
import java.util.UUID;
import com.google.common.base.Optional;
import de.cubeisland.engine.module.chat.storage.Muted;
import de.cubeisland.engine.service.database.Database;
import de.cubeisland.engine.service.user.UserAttachment;
import org.jooq.DSLContext;

import static com.google.common.base.Optional.fromNullable;
import static de.cubeisland.engine.module.chat.storage.TableMuted.TABLE_MUTED;

public class ChatAttachment extends UserAttachment
{
    private UUID lastWhisper;

    public void setLastWhisper(UUID lastWhisper)
    {
        this.lastWhisper = lastWhisper;
    }

    public UUID getLastWhisper()
    {
        return lastWhisper;
    }

    @Override
    public void onAttach()
    {
        // TODO async stuffs
        muted = fromNullable(dsl().selectFrom(TABLE_MUTED).where(TABLE_MUTED.ID.eq(
            getHolder().getEntity().getId())).fetchOne());
    }

    private DSLContext dsl()
    {
        return getModule().getModularity().getInstance(Database.class).getDSL();
    }

    private Optional<Muted> muted;

    public Date getMuted()
    {
        return muted.transform(m -> m.getValue(TABLE_MUTED.MUTED)).orNull();
    }

    public void setMuted(Date muted)
    {
        if (muted == null)
        {
            if (this.muted.isPresent())
            {
                this.muted.get().deleteAsync();
                this.muted = Optional.absent();
            }
            return;
        }
        if (!this.muted.isPresent())
        {
            this.muted = Optional.of(dsl().newRecord(TABLE_MUTED).newMuted(getHolder()));
        }
        this.muted.get().setValue(TABLE_MUTED.MUTED, muted);
        this.muted.get().storeAsync();
    }

    private boolean afk;

    public boolean isAfk()
    {
        return afk;
    }

    public void setAfk(boolean afk)
    {
        this.afk = afk;
    }


    private long lastAction = 0;

    public long getLastAction()
    {
        return this.lastAction;
    }

    public long updateLastAction()
    {
        return this.lastAction = System.currentTimeMillis();
    }

    public void resetLastAction()
    {
        this.lastAction = 0;
    }
}
