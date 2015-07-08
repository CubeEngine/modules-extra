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
package de.cubeisland.engine.module.log.action.block.player.bucket;

import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.module.log.LoggingConfiguration;
import de.cubeisland.engine.module.log.action.block.player.PlayerBlockPlace;
import org.spongepowered.api.text.Text;

import static de.cubeisland.engine.service.i18n.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.module.log.action.ActionCategory.BUCKET;

/**
 * Represents a player emptying a waterbucket
 * <p>Listener:
 * {@link ListenerBucket}
 */
public class BucketWater extends PlayerBlockPlace
{
    public BucketWater()
    {
        super("water", BUCKET);
    }

    @Override
    public Text translateAction(User user)
    {
        if (this.hasAttached())
        {
            return user.getTranslation(POSITIVE, "{user} emptied {amount} water-buckets", this.player.name,
                                       this.countAttached());
        }
        return user.getTranslation(POSITIVE, "{user} emptied a water-bucket", this.player.name);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.bucket.water;
    }
}
