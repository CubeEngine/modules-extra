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
package org.cubeengine.module.authorization;

import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.InjectService;
import org.cubeengine.libcube.service.command.ModuleCommand;
import org.cubeengine.module.authorization.storage.Auth;
import org.cubeengine.module.authorization.storage.TableAuth;
import org.cubeengine.libcube.util.Triplet;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.service.database.ModuleTables;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.jooq.DSLContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.service.permission.PermissionService;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.cubeengine.module.authorization.HashHelper.*;
import static org.cubeengine.module.authorization.storage.TableAuth.TABLE_AUTH;

@Singleton
@Module(id = "authorization", name = "Authorization", version = "1.0.0",
        description = "Provides password authorization",
        dependencies = @Dependency("cubeengine-core"),
        url = "http://cubeengine.org",
        authors = {"Anselm 'Faithcaio' Brehme", "Phillip Schichtel"})
@ModuleTables(TableAuth.class)
public class Authorization extends CubeEngineModule
{
    @Inject private FileManager fm;
    @InjectService private PermissionService ps;
    @Inject private Database db;
    @Inject private AuthPerms perms;

    @ModuleConfig private AuthConfiguration config;
    @InjectService private BanService banService;

    @Inject @ModuleCommand private AuthCommands authCommands;

    private String staticSalt;
    private final Map<UUID, Triplet<Long, String, Integer>> failedLogins = new ConcurrentHashMap<>();
    private final Map<UUID, Auth> auths = new ConcurrentHashMap<>();
    private final Set<UUID> loggedIn = new CopyOnWriteArraySet<>();

    @Listener
    public void onEnable(GamePostInitializationEvent event)
    {
        this.staticSalt = HashHelper.loadStaticSalt(fm.getDataPath().resolve(".salt"));
        ps.registerContextCalculator(new AuthContextCalculator(this));
    }

    public Triplet<Long, String, Integer> getFailedLogin(Player user)
    {
        return this.failedLogins.get(user.getUniqueId());
    }

    protected void addFailedLogin(Player user)
    {
        Triplet<Long, String, Integer> loginFail = this.getFailedLogin(user);
        if (loginFail == null)
        {
            loginFail = new Triplet<>(System.currentTimeMillis(), user.getConnection().getAddress().getAddress().getHostAddress(), 1);
            this.failedLogins.put(user.getUniqueId(), loginFail);
        }
        else
        {
            loginFail.setFirst(System.currentTimeMillis());
            loginFail.setSecond(user.getConnection().getAddress().getAddress().getHostAddress());
            loginFail.setThird(loginFail.getThird() + 1);
        }
    }

    protected void removeFailedLogins(Player user)
    {
        this.failedLogins.remove(user.getUniqueId());
    }

    public void resetAllPasswords()
    {
        this.db.getDSL().update(TABLE_AUTH).set(TABLE_AUTH.PASSWD, (byte[])null).execute();
        reset();
    }



    public boolean isLoggedIn(UUID player)
    {
        return loggedIn.contains(player);
    }

    public void logout(UUID player)
    {
        loggedIn.remove(player);
    }

    public CompletableFuture<Boolean> isPasswordSet(UUID player)
    {
        return getAuth(player).thenApply(auth -> auth.getValue(TABLE_AUTH.PASSWD) != null);
    }

    public CompletableFuture<Void> resetPassword(UUID player)
    {
        return getAuth(player).thenCompose(auth -> {
            auth.setValue(TableAuth.TABLE_AUTH.PASSWD, null);
            return runAsync(auth::update);
        });
    }

    public CompletableFuture<Void> setPassword(UUID player, String password)
    {
        return getAuth(player).thenCompose(auth -> {
            try
            {
                auth.setValue(TableAuth.TABLE_AUTH.PASSWD, hash(saltPassword(player, password)));
                return runAsync(auth::update);
            }
            catch (NoSuchAlgorithmException e)
            {
                throw new RuntimeException("Failed to generate hash!", e);
            }
        });
    }

    private String saltPassword(UUID player, String password)
    {
        return saltString(password, this.staticSalt, toDynamicSalt(player));
    }

    public CompletableFuture<Boolean> checkPassword(UUID player, String password) {
        return getAuth(player).thenApply(auth -> {
            final byte[] storedHash = auth.getValue(TableAuth.TABLE_AUTH.PASSWD);
            try
            {
                return checkAgainstHash(storedHash, saltPassword(player, password));
            }
            catch (NoSuchAlgorithmException e)
            {
                throw new RuntimeException("Unable to generate hash!", e);
            }
        });
    }

    private CompletableFuture<Auth> getAuth(UUID player)
    {
        return CompletableFuture.supplyAsync(() -> this.auths.computeIfAbsent(player, (id) -> {
            final DSLContext dsl = db.getDSL();
            Auth auth = dsl.selectFrom(TABLE_AUTH).where(TABLE_AUTH.ID.eq(player)).fetchOne();
            if (auth != null)
            {
                auth = dsl.newRecord(TABLE_AUTH).newAuth(player);
                auth.insert();
            }
            return auth;
        }));
    }


    public CompletableFuture<Boolean> login(User user, String password)
    {
        final UUID id = user.getUniqueId();
        if (isLoggedIn(id))
        {
            return completedFuture(true);
        }
        return checkPassword(id, password).thenApply(success -> {
            if (success)
            {
                loggedIn.add(id);
            }
            return success;
        });
    }

    public void reset()
    {
        auths.clear();
        loggedIn.clear();
    }

    public AuthPerms perms()
    {
        return perms;
    }

    public AuthConfiguration getConfig()
    {
        return config;
    }

    public BanService getBanService() {
        return banService;
    }
}
