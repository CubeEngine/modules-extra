package org.cubeengine.module.vigil;

import org.cubeengine.module.vigil.commands.VigilCommands;
import org.cubeengine.module.vigil.storage.QueryManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.i18n.formatter.MessageType;
import org.cubeengine.service.permission.PermissionManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.vigil.commands.VigilCommands.toolName;
import static org.spongepowered.api.data.key.Keys.DISPLAY_NAME;

public class ToolListener
{
    private Game game;
    private I18n i18n;
    private final PermissionManager pm;
    private QueryManager qm;
    private final PermissionDescription toolPerm;

    public ToolListener(Vigil module, I18n i18n, PermissionManager pm, QueryManager qm, Game game)
    {
        this.i18n = i18n;
        this.pm = pm;
        this.qm = qm;
        this.game = game;
        toolPerm = pm.register(module, "use-logtool", "Allows using log-tools", null);
    }

    @Listener
    public void onClick(InteractBlockEvent event)
    {
        event.getCause().first(Player.class).ifPresent(player -> {
            // TODO don't trigger on pressureplates?
            if (player.hasPermission(toolPerm.getId()))
            {
                player.getItemInHand().ifPresent(item -> item.get(DISPLAY_NAME).ifPresent(display -> {
                    if (display.equals(toolName))
                    {
                        Location loc;
                        if (event instanceof InteractBlockEvent.Primary)
                        {
                            loc = event.getTargetBlock().getLocation().get();
                        }
                        else
                        {
                            loc = event.getTargetBlock().getLocation().get().getRelative(event.getTargetSide());
                        }
                        // TODO generate and pass Lookup with parameters and show settings
                        // TODO set lookuplocation to Block ; left: clicked block ; righ: would placed block
                        qm.queryAndShow(loc.getPosition(), player);
                        event.setCancelled(true);
                    }
                }));
            }
        });
    }
}
