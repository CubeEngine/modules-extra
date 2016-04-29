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
package org.cubeengine.module.log.action.block.player;

import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.BaseAction;
import org.cubeengine.libcube.service.user.User;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.libcube.util.StringUtils;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.log.action.ActionCategory.SIGN;

/**
 * Represents a player changing the text on a sign
 */
public class SignChange extends ActionPlayerBlock
{
    public String[] oldLines;
    public String[] newLines;

    public SignChange()
    {
        super("change", SIGN);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return false;
    }

    @Override
    public Text translateAction(User user)
    {
        String delim = ChatFormat.GREY + " | " + ChatFormat.GOLD;
        if (oldLines == null || oldLines.length == 0)
        {
            return user.getTranslation(POSITIVE, "{user} wrote {input#signtext} on a sign", this.player.name,
                                       StringUtils.implode(delim, newLines));
        }
        return Texts.of(user.getTranslation(POSITIVE, "{user} wrote {input#signtext} on a sign", this.player.name,
                                   StringUtils.implode(delim, newLines)) , "\n" ,
            user.getTranslation(POSITIVE, "    The old signtext was {input#signtext}", StringUtils.implode(delim,
                                                                                                           oldLines)));
    }

    public void setNewLines(String[] newLines)
    {
        this.newLines = newLines;
    }

    public void setOldLines(String[] oldLines)
    {
        this.oldLines = oldLines;
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.block.signChange;
    }
}
