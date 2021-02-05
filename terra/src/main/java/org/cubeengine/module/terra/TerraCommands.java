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
package org.cubeengine.module.terra;

import com.google.inject.Inject;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.module.terra.data.TerraItems;
import org.cubeengine.module.terra.data.TerraItems.Essence;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.type.ViewableInventory;

@Command(name = "terra", desc = "Terra Commands")
public class TerraCommands extends DispatcherCommand
{
    private final TerraListener listener;

    @Inject
    public TerraCommands(TerraListener listener)
    {
        this.listener = listener;
    }

    @Command(desc = "Shows Terra status")
    public void status(CommandCause cause)
    {
        this.listener.printStatus(cause.getAudience());
    }

    @Command(desc = "Cancels all current waiting worlds")
    public void cancel(CommandCause cause)
    {
        this.listener.cancelAll(cause.getAudience());
    }

    @Command(desc = "Provides all basic essences")
    public void essence(ServerPlayer player)
    {
        final ViewableInventory view = ViewableInventory.builder().type(ContainerTypes.GENERIC_9X6).completeStructure().build();
        for (Essence value : Essence.values())
        {
            final ItemStack essence = TerraItems.getEssence(value, player);
            view.offer(essence);
        }
        player.openInventory(view);
    }
}
