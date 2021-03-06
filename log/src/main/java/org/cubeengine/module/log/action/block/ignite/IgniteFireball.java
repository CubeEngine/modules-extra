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
package org.cubeengine.module.log.action.block.ignite;


import java.util.UUID;
import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.BaseAction;
import org.cubeengine.module.log.action.block.player.ActionPlayerBlock.PlayerSection;
import org.cubeengine.libcube.service.user.User;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import static org.spongepowered.api.entity.EntityTypes.*;

import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

/**
 * Represents a fireball setting a block on fire
 */
public class IgniteFireball extends ActionBlockIgnite
{
    public UUID shooterUUID;
    public EntityType shooterType;
    public PlayerSection player;

    public IgniteFireball()
    {
        super("fireball");
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof IgniteFireball
            // No Shooter or same Shooter
            && ((this.shooterUUID == null && ((IgniteFireball)action).shooterUUID == null) || (this.shooterUUID != null
            && this.shooterUUID.equals(((IgniteFireball)action).shooterUUID)))
            // No Player or same Player
            && ((this.player == null && ((IgniteFireball)action).player == null) || (this.player != null
            && this.player.equals(((IgniteFireball)action).player)));
    }

    @Override
    public Text translateAction(User user)
    {
        int count = this.countAttached();
        if (shooterType == PLAYER)
        {
            return user.getTranslationN(POSITIVE, count, "{user} shot a fireball setting this block on fire",
                                        "{user} shot fireballs setting {amount} blocks on fire", this.player.name,
                                        count);
        }
        if (shooterType == GHAST)
        {
            if (player == null)
            {
                return user.getTranslationN(POSITIVE, count, "A Ghast shot a fireball setting this block on fire",
                                            "A Ghast shot fireballs setting {amount} blocks on fire", count);
            }
            return user.getTranslationN(POSITIVE, count, "A Ghast shot a fireball at {user} setting this block on fire",
                                        "A Ghast shot fireballs at {user} setting {amount} blocks on fire",
                                        this.player.name, count);
        }
        return user.getTranslationN(POSITIVE, count, "A fire got set by a fireball",
                                    "{amount} fires got set by fireballs", count);
    }

    public void setShooter(Entity entity)
    {
        this.shooterUUID = entity.getUniqueId();
        this.shooterType = entity.getType();
    }

    public void setPlayer(Player player)
    {
        this.player = new PlayerSection(player);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.ignite.fireball;
    }
}
