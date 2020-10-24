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
package org.cubeengine.module.spawn;

import java.util.Optional;
import java.util.UUID;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.audience.Audience;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.ParameterPermission;
import org.cubeengine.libcube.service.command.annotation.Parser;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.StringUtils;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Transform;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.libcube.util.ContextUtil.toSet;

@Singleton
public class SpawnCommands
{
    public static final String ROLESPAWN_WORLD = "rolespawn_world";
    public static final String ROLESPAWN_POSITION = "rolespawn_position";
    public static final String ROLESPAWN_ROTATION = "rolespawn_rotation";

    private I18n i18n;
    private PermissionService pm;

    @Inject
    public SpawnCommands(I18n i18n, PermissionService pm)
    {
        this.i18n = i18n;
        this.pm = pm;
    }

    @Command(desc = "Changes the respawnpoint")
    public void setRoleSpawn(ServerPlayer ctx, @Parser(SubjectParser.class) Subject role, @Default Context context)
    {
        setRoleSpawn(context, ctx.getWorld(), ctx.getTransform(), role);
        i18n.send(ctx, POSITIVE, "The spawn in {world} for the role {name#role} is now set to {vector}",
                            context, role.getIdentifier(),
                            ctx.getLocation().getPosition());
    }

    private void setRoleSpawn(Context context, ServerWorld world, Transform transform, Subject role)
    {
        String[] posStrings = new String[3];
        posStrings[0] = String.valueOf(transform.getPosition().getFloorX());
        posStrings[1] = String.valueOf(transform.getPosition().getFloorY());
        posStrings[2] = String.valueOf(transform.getPosition().getFloorZ());
        String[] rotStrings = new String[3];
        rotStrings[0] = String.valueOf(transform.getYaw());
        rotStrings[1] = String.valueOf(transform.getPitch());
        role.getSubjectData().setOption(toSet(context), ROLESPAWN_WORLD, world.getKey().toString());
        role.getSubjectData().setOption(toSet(context), ROLESPAWN_POSITION, StringUtils.implode(":", posStrings));
        role.getSubjectData().setOption(toSet(context), ROLESPAWN_ROTATION, StringUtils.implode(":", rotStrings));
    }

    // TODO teleport all players to spawn

    @Command(desc = "Teleports a player to the configured rolespawn")
    public void roleSpawn(CommandCause ctx, @Default ServerPlayer player, @Default Context context,
                          @Named({"role", "r"}) @Default @Parser(SubjectParser.class) Subject role,
                          @ParameterPermission @Flag boolean force)
    {
        String name = role.getIdentifier();
        if (pm.getGroupSubjects() == role.getContainingCollection())
        {
            name = Sponge.getServer().getUserManager().get(UUID.fromString(name)).get().getName();
        }

        final Audience ctxAudience = ctx.getAudience();

        final Optional<Vector3d> pos = getSubjectSpawnPos(role);
        final Optional<Vector3d> rot = getSubjectSpawnRotation(role);
        final Optional<ServerWorld> world = getSubjectSpawnWorld(role);
        if (!pos.isPresent() || !rot.isPresent() || !world.isPresent())
        {
            i18n.send(ctxAudience, NEGATIVE, "No rolespawn set for {name} in {ctx}", name, context);
            return;
        }

        if (!this.tpTo(player, world.get(), Transform.of(pos.get(), rot.get()), force))
        {
            i18n.send(ctxAudience, NEGATIVE, "Teleport failed!");
            return;
        }
        if (!ctxAudience.equals(player))
        {
            i18n.send(ctxAudience, POSITIVE, "Teleported {user} to the spawn of the role {name#role} in {ctx}", player, name, context);
        }
        else if (pm.getGroupSubjects() == role.getContainingCollection())
        {
            i18n.send(ctxAudience, POSITIVE, "You are now standing at your role's spawn in {ctx}!", context);
        }
        else
        {
            i18n.send(ctxAudience, POSITIVE, "You are now standing at the spawn of {name#role} in {ctx}!", name, context);
        }
    }

    public static ServerWorld getSpawnWorld(ServerPlayer player)
    {
        return getSubjectSpawnWorld(player).orElse(player.getWorld());
    }

    public static Optional<ServerWorld> getSubjectSpawnWorld(Subject subject)
    {
        return subject.getOption(ROLESPAWN_WORLD)
                      .map(ResourceKey::resolve)
                      .flatMap(k -> Sponge.getServer().getWorldManager().getWorld(k));
    }

    public static Vector3d getSpawnPosition(ServerPlayer player)
    {
        return getSubjectSpawnPos(player).orElse(player.getPosition());
    }

    public static Optional<Vector3d> getSubjectSpawnPos(Subject player)
    {
        return player.getOption(ROLESPAWN_POSITION).map(s -> {
            String[] spawnStrings = StringUtils.explode(":", s);
            int x = Integer.valueOf(spawnStrings[0]);
            int y = Integer.valueOf(spawnStrings[1]);
            int z = Integer.valueOf(spawnStrings[2]);
            return new Vector3d(x, y, z).add(0.5, 0, 0.5);
        });
    }

    public static Vector3d getSpawnRotation(ServerPlayer player)
    {
        return getSubjectSpawnRotation(player).orElse(player.getRotation());
    }

    public static Optional<Vector3d> getSubjectSpawnRotation(Subject subject)
    {
        return subject.getOption(ROLESPAWN_ROTATION).map(s -> {
            String[] spawnStrings = StringUtils.explode(":", s);
            float yaw = Float.valueOf(spawnStrings[0]);
            float pitch = Float.valueOf(spawnStrings[1]);
            return new Vector3d(yaw, pitch, 0).add(0.5, 0.5, 0.5);
        });
    }

    private boolean tpTo(ServerPlayer user, ServerWorld world, Transform transform, boolean force)
    {
        if (force)
        {
            user.setLocation(world.getLocation(transform.getPosition()));
            user.setRotation(transform.getRotation());
            return true;
        }
        if (user.setLocationSafely(transform.getLocation()))
        {
            user.setRotation(transform.getRotation());
            return true;
        }
        return false;
    }
}
