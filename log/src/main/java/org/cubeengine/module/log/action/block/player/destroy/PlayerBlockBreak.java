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
package org.cubeengine.module.log.action.block.player.destroy;

import org.cubeengine.libcube.service.user.User;
import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.ActionCategory;
import org.cubeengine.module.log.action.BaseAction;
import org.cubeengine.module.log.action.ReferenceHolder;
import org.cubeengine.module.log.action.block.player.ActionPlayerBlock;
import org.cubeengine.reflect.codec.mongo.Reference;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.log.action.ActionCategory.BLOCK;

/**
 * Represents a player breaking a block
 * <p>SubTypes:
 * {@link PlayerNoteBlockBreak}
 * {@link PlayerSignBreak}
 * {@link PlayerJukeboxBreak}
 * {@link PlayerContainerBreak}
 */
public class PlayerBlockBreak extends ActionPlayerBlock implements ReferenceHolder
{
    public Reference<ActionPlayerBlock> reference;

    public PlayerBlockBreak()
    {
        super("break", BLOCK);
    }

    public PlayerBlockBreak(String name, ActionCategory... categories)
    {
        super(name, categories);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof PlayerBlockBreak && this.player.equals(((PlayerBlockBreak)action).player)
            && ((PlayerBlockBreak)action).oldBlock.material == this.oldBlock.material
            && ((this.reference == null && ((PlayerBlockBreak)action).reference == null) ||
            (this.reference != null && this.reference.equals(((PlayerBlockBreak)action).reference)));
    }

    @Override
    public Text translateAction(User user)
    {
        int count = this.countAttached();
        if (this.reference == null)
        {
            return user.getTranslationN(POSITIVE, count,"{user} broke {name#block}",
                                        "{user} broke {name#block} x{amount}", this.player.name, this.oldBlock.name(),
                                        count);
        }
        // TODO better
        return user.getTranslationN(POSITIVE, count,
                                    "{user} broke {name#block} indirectly",
                                    "{user} broke {name#block} x{amount} indirectly",
                                    this.player.name, this.oldBlock.name(), count);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.block.destroyByPlayer;
    }
}
