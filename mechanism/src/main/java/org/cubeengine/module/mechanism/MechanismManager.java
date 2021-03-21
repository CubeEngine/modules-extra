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
package org.cubeengine.module.mechanism;

import java.util.HashMap;
import java.util.Map;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.module.mechanism.sign.Gate;
import org.cubeengine.module.mechanism.sign.HiddenButton;
import org.cubeengine.module.mechanism.sign.HiddenLever;
import org.cubeengine.module.mechanism.sign.SignMechanism;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.block.InteractBlockEvent.Secondary;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.server.ServerLocation;

/**
 * A module to edit signs and signed books
 */
@Singleton
public class MechanismManager
{
    @Inject private HiddenButton hiddenButton;
    @Inject private HiddenLever hiddenLever;
    @Inject private Gate gate;

    private Map<Class<? extends SignMechanism>, SignMechanism> signMechanisms = new HashMap<>();
    private Map<String, SignMechanism> signMechanismsByName = new HashMap<>();

    public void init()
    {
        this.signMechanisms.clear();
        this.signMechanisms.put(hiddenButton.getClass(), hiddenButton);
        this.signMechanisms.put(hiddenLever.getClass(), hiddenLever);
        this.signMechanisms.put(gate.getClass(), gate);
        this.signMechanismsByName.put(hiddenLever.getName(), hiddenLever);
        this.signMechanismsByName.put(hiddenButton.getName(), hiddenButton);
        this.signMechanismsByName.put(gate.getName(), gate);
    }

    public ItemStack makeSign(Class<? extends SignMechanism> clazz, ItemStack stack)
    {
        return this.signMechanisms.get(clazz).makeSign(stack);
    }

    public boolean trigger(String mechanism, Secondary event, ServerPlayer player, ServerLocation loc, boolean hidden)
    {
        final SignMechanism signMechanism = signMechanismsByName.get(mechanism);
        if (signMechanism != null)
        {
            return signMechanism.interact(event, player, loc, hidden);
        }
        return false;
    }

    public void initializeSign(String mechanism, BlockEntity sign)
    {
        final SignMechanism signMechanism = signMechanismsByName.get(mechanism);
        if (signMechanism != null)
        {
            signMechanism.initSign(sign);
        }

    }
}
