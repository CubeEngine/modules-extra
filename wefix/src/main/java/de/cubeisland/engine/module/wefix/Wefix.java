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
package de.cubeisland.engine.module.wefix;

import java.util.Arrays;
import java.util.List;
import de.cubeisland.engine.core.bukkit.EventManager;
import de.cubeisland.engine.core.module.Module;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class Wefix extends Module implements Listener
{
    private final List<String> blockMe = Arrays.asList("calc", "eval", "solve", "schem", "search", "l");

    @Override
    public void onEnable()
    {
        EventManager em = this.getCore().getEventManager();
        em.registerListener(this, this);
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e)
    {
        Player p = e.getPlayer();
        String command = e.getMessage().trim().split("\\s+", 2)[0].toLowerCase();
        for (String block : blockMe)
        {
            if (command.startsWith("//" + block) || command.startsWith("/worldedit:/" + block))
            {
                e.setCancelled(true);
                p.kickPlayer(ChatColor.RED + "No you don't!");
                return;
            }
        }
    }
}
/*
/worldedit:/calc for(i=0;i<256;i++){for(b=0;b<256;b++){for(h=0;h<256;h++){for(n=0;n<256;n++){}}}}
/worldedit:/solve for(i=0;i<256;i++){for(b=0;b<256;b++){for(h=0;h<256;h++){for(n=0;n<256;n++){}}}}
/worldedit:/eval for(i=0;i<256;i++){for(b=0;b<256;b++){for(h=0;h<256;h++){for(n=0;n<256;n++){}}}}
//calc for(i=0;i<256;i++){for(b=0;b<256;b++){for(h=0;h<256;h++){for(n=0;n<256;n++){}}}}
//solve for(i=0;i<256;i++){for(b=0;b<256;b++){for(h=0;h<256;h++){for(n=0;n<256;n++){}}}}
//eval for(i=0;i<256;i++){for(b=0;b<256;b++){for(h=0;h<256;h++){for(n=0;n<256;n++){}}}}
*/