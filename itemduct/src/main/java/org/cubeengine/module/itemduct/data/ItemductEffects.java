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
package org.cubeengine.module.itemduct.data;

import net.kyori.adventure.sound.Sound;
import org.cubeengine.module.itemduct.Network;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleOptions;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.util.Color;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

public class ItemductEffects {

    private static final ParticleEffect badEffect = ParticleEffect.builder().type(ParticleTypes.DUST).option(ParticleOptions.COLOR, Color.RED).build();
    private static final ParticleEffect goodEffect = ParticleEffect.builder().type(ParticleTypes.DUST).option(ParticleOptions.COLOR, Color.GREEN).build();
    private static final ParticleEffect neutralEffect = ParticleEffect.builder().type(ParticleTypes.DUST).option(ParticleOptions.COLOR, Color.YELLOW).build();
    private static final ParticleEffect brokeSmoke = ParticleEffect.builder().type(ParticleTypes.LARGE_SMOKE).build();
    private static final ParticleEffect buildCloud = ParticleEffect.builder().type(ParticleTypes.CLOUD).build();

    public static void playNetworkSound(Vector3d pos, Network network) {
        if (network.errors.isEmpty()) {
            network.world.playSound(Sound.sound(SoundTypes.BLOCK_DISPENSER_DISPENSE, Sound.Source.NEUTRAL, 1, 0), pos);
        } else {
            network.world.playSound(Sound.sound(SoundTypes.BLOCK_PISTON_CONTRACT, Sound.Source.NEUTRAL, 1, 0), pos);
        }
    }

    public static void playNetworkEffects(Network network) {

        //        System.out.print("Network: Pipes " +  network.pipes.size() + " Exits " + network.exitPoints.size() + " Storage " + network.storage.size() + "\n");


        playParticlesAroundBlock(network.world, network.start.toDouble(), network.exitPoints.isEmpty() && network.storage.isEmpty() ? neutralEffect : goodEffect);
        for (Vector3i pipe : network.pipes)
        {
            if (!network.errors.contains(pipe)) {
                playParticlesAroundBlock(network.world, pipe.toDouble(), network.errors.isEmpty() ? goodEffect : neutralEffect);
            }
        }

        for (Vector3i pipe : network.storage)
        {
            if (!network.errors.contains(pipe)) {
                playParticlesAroundBlock(network.world, pipe.toDouble(), network.errors.isEmpty() ? goodEffect : neutralEffect);
            }
        }

        for (Vector3i error : network.errors)
        {
            playParticlesAroundBlock(network.world, error.toDouble(), badEffect);
            network.world.spawnParticles(brokeSmoke, error.toDouble().add(0.5,0.5,0.5));
        }

        for (Vector3i exit : network.exitPoints.keySet())
        {
            playParticlesAroundBlock(network.world, exit.toDouble(), goodEffect);
        }
    }

    public static void playCreateEffect(ServerLocation loc)
    {
        playParticlesAroundBlock(loc.getWorld(), loc.getPosition(), buildCloud);
        loc.getWorld().playSound(Sound.sound(SoundTypes.BLOCK_ANVIL_USE, Sound.Source.NEUTRAL, 1, 0), loc.getPosition());
    }

    private static void playParticlesAroundBlock(ServerWorld world, Vector3d pos, ParticleEffect effect) {
        final Vector3d center = pos.add(0.5, 0.5, 0.5);
        for (Direction effectDir : Direction.values())
        {
            if (effectDir.isCardinal() || effectDir.isUpright())
            {
                world.spawnParticles(effect, center.add(effectDir.asOffset().div(1.9)));
            }
        }
    }
}
