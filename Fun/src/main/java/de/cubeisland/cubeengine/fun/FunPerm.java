package de.cubeisland.cubeengine.fun;

import de.cubeisland.cubeengine.core.permission.PermDefault;
import de.cubeisland.cubeengine.core.permission.Permission;
import java.util.Locale;
import org.bukkit.permissions.Permissible;

public enum FunPerm implements Permission
{
    EXPLOSION_OTHER,
    EXPLOSION_PLAYER_DAMAGE,
    EXPLOSION_BLOCK_DAMAGE,
    EXPLOSION_FIRE,
    HAT_OTHER,
    HAT_ITEM,
    LIGHTNING_PLAYER_DAMAGE,
    LIGHTNING_UNSAFE;

    private String permission;
    private PermDefault def;

    public static final String BASE = "cubeengine.fun.";

    private FunPerm()
    {
        this(PermDefault.OP);
    }

    private FunPerm(PermDefault def)
    {
        this.permission = BASE + this.name().toLowerCase(Locale.ENGLISH).replace('_', '.');
        this.def = def;
    }

    @Override
    public boolean isAuthorized(Permissible player)
    {
        return player.hasPermission(permission);
    }

    @Override
    public String getPermission()
    {
        return this.permission;
    }

    @Override
    public PermDefault getPermissionDefault()
    {
        return this.def;
    }
}
