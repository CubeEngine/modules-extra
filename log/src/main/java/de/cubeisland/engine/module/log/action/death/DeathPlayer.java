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
package de.cubeisland.engine.module.log.action.death;

import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.log.LoggingConfiguration;
import de.cubeisland.engine.module.log.action.BaseAction;
import de.cubeisland.engine.module.log.action.block.player.ActionPlayerBlock.PlayerSection;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.text.Text;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.module.log.action.ActionCategory.DEATH;

/**
 * Represents a players death
 */
public class DeathPlayer extends ActionDeath
{
    public PlayerSection killed;

    public DeathPlayer()
    {
        super("player", DEATH);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof DeathPlayer && this.killer != null && ((ActionDeath)action).killer != null
            && this.killer.fetch(DeathKill.class).canAttach(((ActionDeath)action).killer.fetch(DeathKill.class));
    }

    @Override
    public Text translateAction(User user)
    {
        DeathKill fetch = this.killer.fetch(DeathKill.class);
        if (fetch.isPlayerKiller())
        {
            if (this.hasAttached())
            {
                return user.getTranslation(POSITIVE, "{amount} players got killed by {user}", this.countAttached(),
                                           fetch.playerKiller.name);
            }
            return user.getTranslation(POSITIVE, "{user} got killed by {user}", this.killed.name,
                                       fetch.playerKiller.name);
        }
        if (fetch.isEntityKiller())
        {
            if (this.hasAttached())
            {
                return user.getTranslation(POSITIVE, "{amount} players could not escape {name#entity}",
                                           this.countAttached(), fetch.entityKiller.name());
            }
            return user.getTranslation(POSITIVE, "{user} could not escape {name#entity}", this.killed.name,
                                       fetch.entityKiller.name());
        }
        if (fetch.isOtherKiller())
        {
            if (this.hasAttached())
            {
                return user.getTranslation(POSITIVE, "{amount} players died of {name#cause}", this.countAttached(),
                                           fetch.otherKiller.name());
            }
            return user.getTranslation(POSITIVE, "{user} died of {name#cause}", this.killed.name,
                                       fetch.otherKiller.name());
        }
        return user.getTranslation(POSITIVE, "{user} died", this.killed.name);
    }

    public void setPlayer(Player player)
    {
        this.killed = new PlayerSection(player);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.death.player;
    }
}
