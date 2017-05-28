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

import java.util.UUID;
import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Parser;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.libcube.service.command.annotation.ParameterPermission;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.util.ContextUtil.toSet;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

public class SpawnCommands
{
    public static final String ROLESPAWN = "rolespawn";
    private final Spawn module;
    private I18n i18n;
    private PermissionService pm;

    public SpawnCommands(Spawn module, I18n i18n, PermissionService pm)
    {
        this.module = module;
        this.i18n = i18n;
        this.pm = pm;
    }

    @Command(desc = "Changes the respawnpoint")
    public void setRoleSpawn(Player ctx, @Parser(SubjectParser.class) Subject role, @Default Context context)
    {
        setRoleSpawn(context, ctx.getTransform(), role);
        i18n.send(ctx, POSITIVE, "The spawn in {world} for the role {name#role} is now set to {vector}",
                            context, role.getIdentifier(),
                            ctx.getLocation().getPosition());
    }

    private void setRoleSpawn(Context context, Transform<World> transform, Subject role)
    {
        String[] locStrings = new String[6];
        locStrings[0] = String.valueOf(transform.getPosition().getFloorX());
        locStrings[1] = String.valueOf(transform.getPosition().getFloorY());
        locStrings[2] = String.valueOf(transform.getPosition().getFloorZ());
        locStrings[3] = String.valueOf(transform.getYaw());
        locStrings[4] = String.valueOf(transform.getPitch());
        role.getSubjectData().setOption(toSet(context), "rolespawn", StringUtils.implode(":", locStrings));
    }

    // TODO teleport all players to spawn

    @Command(desc = "Teleports a player to the configured rolespawn")
    public void roleSpawn(CommandSource ctx, @Default Player player, @Default Context context,
                      @Named({"role", "r"}) @Default @Parser(SubjectParser.class) Subject role,
                      @ParameterPermission @Flag boolean force)
    {
        java.util.Optional<String> spawnString = role.getOption(toSet(context), ROLESPAWN);
        String name = role.getIdentifier();
        if (pm.getGroupSubjects() == role.getContainingCollection())
        {
            name = Sponge.getServiceManager().provideUnchecked(UserStorageService.class).get(UUID.fromString(name)).get().getName();
        }

        if (!spawnString.isPresent())
        {
            i18n.send(ctx, NEGATIVE, "No rolespawn set for {name} in {ctx}", name, context);
            return;
        }

        Transform<World> spawnPoint = getSpawnLocation(spawnString.get());
        if (!player.isOnline()) // TODO tp users
        {
            i18n.send(ctx, NEGATIVE, "You cannot teleport an offline player to spawn!");
            return;
        }
        if (!this.tpTo(player, spawnPoint, force))
        {
            i18n.send(ctx, NEGATIVE, "Teleport failed!");
            return;
        }
        if (!ctx.equals(player))
        {
            i18n.send(ctx, POSITIVE, "Teleported {user} to the spawn of the role {name#role} in {ctx}", player, name, context);
        }
        else if (pm.getGroupSubjects() == role.getContainingCollection())
        {
            i18n.send(ctx, POSITIVE, "You are now standing at your role's spawn in {ctx}!", context);
        }
        else
        {
            i18n.send(ctx, POSITIVE, "You are now standing at the spawn of {name#role} in {ctx}!", name, context);
        }
    }

    public static Transform<World> getSpawnLocation(String value)
    {
        String[] spawnStrings = StringUtils.explode(":",value);
        int x = Integer.valueOf(spawnStrings[0]);
        int y = Integer.valueOf(spawnStrings[1]);
        int z = Integer.valueOf(spawnStrings[2]);
        float yaw = Float.valueOf(spawnStrings[3]);
        float pitch = Float.valueOf(spawnStrings[4]);
        World world = Sponge.getServer().getWorld(spawnStrings[5]).get();
        return new Transform<>(world, new Vector3d(x + .5, y + .5, z), new Vector3d(yaw, pitch, 0));
    }

    private boolean tpTo(Player user, Transform<World> transform, boolean force)
    {
        if (force)
        {
            user.setTransform(transform);
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
