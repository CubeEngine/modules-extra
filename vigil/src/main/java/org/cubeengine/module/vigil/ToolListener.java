package org.cubeengine.module.vigil;

import org.cubeengine.module.vigil.storage.QueryManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.permission.PermissionManager;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.manipulator.mutable.DisplayNameData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.world.Location;

import java.util.Optional;

import static org.cubeengine.module.vigil.commands.VigilCommands.toolName;

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
        Optional<Player> player = event.getCause().first(Player.class);
        if (player.isPresent())
        {
            // TODO don't trigger on pressureplates?
            if (player.get().hasPermission(toolPerm.getId()))
            {
                Optional<ItemStack> itemInHand = player.get().getItemInHand();
                if (itemInHand.isPresent())
                {
                    if (itemInHand.get().get(DisplayNameData.class).map(data -> data.displayName().get().equals(toolName)).orElse(false))
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
                        qm.queryAndShow(loc.getPosition(), player.get());
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
