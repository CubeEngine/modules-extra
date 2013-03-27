package de.cubeisland.cubeengine.basics.command.moderation.kit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import de.cubeisland.cubeengine.core.command.CommandManager;
import de.cubeisland.cubeengine.core.command.exception.PermissionDeniedException;
import de.cubeisland.cubeengine.core.command.sender.CommandSender;
import de.cubeisland.cubeengine.core.permission.Permission;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.core.util.InventoryUtil;
import de.cubeisland.cubeengine.core.util.time.Duration;
import de.cubeisland.cubeengine.basics.Basics;
import de.cubeisland.cubeengine.basics.BasicsAttachment;
import de.cubeisland.cubeengine.basics.BasicsPerm;

/**
 * A Kit of Items a User can receive
 */
public class Kit
{
    private String name;
    private List<KitItem> items;
    private boolean giveKitOnFirstJoin;
    private int limitUsagePerPlayer;
    private long limitUsageDelay;
    private Permission permission;
    private String customMessage;
    private List<String> commands;
    private Basics module;

    public Kit(Basics module, final String name, boolean giveKitOnFirstJoin, int limitUsagePerPlayer, long limitUsageDelay, boolean usePermission, String customMessage, List<String> commands, List<KitItem> items)
    {
        this.module = module;
        this.name = name;
        this.items = items;
        this.commands = commands;
        this.customMessage = customMessage;
        if (usePermission)
        {
            this.permission = BasicsPerm.KITS.createChild(name);
        }
        else
        {
            this.permission = null;
        }
        this.giveKitOnFirstJoin = giveKitOnFirstJoin;
        this.limitUsagePerPlayer = limitUsagePerPlayer;
        this.limitUsageDelay = limitUsageDelay;
    }

    public boolean give(CommandSender sender, User user, boolean force)
    {
        //TODO give kit to all players online (not here)
        //TODO starterKit on login (not here)
        if (!force && this.getPermission() != null)
        {
            if (!this.getPermission().isAuthorized(sender))
            {
                sender.sendMessage("basics", "You are not allowed to give this kit.");
                throw new PermissionDeniedException();
            }
        }
        if (module.getKitGivenManager().reachedUsageLimit(user, this.name, this.limitUsagePerPlayer))
        {
            sender.sendMessage("basics", "&cKit-limit reached.");
            throw new PermissionDeniedException();
        }
        //TODO check how many times user got his kit
        if (limitUsageDelay != 0)
        {
            Long lastUsage = user.get(BasicsAttachment.class).getKitUsage(this.name);
            if (lastUsage != null && System.currentTimeMillis() - lastUsage < limitUsageDelay)
            {
                sender.sendMessage("basisc", "&eThis kit not availiable at the moment. &aTry again later!");
                throw new PermissionDeniedException();
            }
        }
        List<ItemStack> list = this.getItems();
        if (InventoryUtil.giveItemsToUser(user, list.toArray(new ItemStack[list.size()])))
        {
            this.executeCommands(user);
            if (limitUsageDelay != 0)
            {
                user.get(BasicsAttachment.class).setKitUsage(this.name);
            }
            return true;
        }
        return false;
    }

    private void executeCommands(User user)
    {
        if (this.commands != null && !this.commands.isEmpty())
        {
            CommandManager cm = user.getCore().getCommandManager();
            for (String cmd : commands)
            {
                cmd = cmd.replace("{PLAYER}", user.getName());
                cm.runCommand(new KitCommandSender(user), cmd);
            }
        }
    }

    public Permission getPermission()
    {
        return this.permission;
    }

    public String getCustomMessage()
    {
        return this.customMessage;
    }

    private List<ItemStack> getItems()
    {
        List<ItemStack> list = new ArrayList<ItemStack>();
        for (KitItem kitItem : this.items)
        {
            ItemStack item = new ItemStack(kitItem.mat, kitItem.amount, kitItem.dura);
            if (kitItem.customName != null)
            {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(kitItem.customName);
                item.setItemMeta(meta);
            }
            list.add(item);
        }
        return list;
    }

    public void applyToConfig(KitConfiguration config)
    {
        config.customReceiveMsg = this.customMessage;
        config.giveOnFirstJoin = this.giveKitOnFirstJoin;
        config.kitCommands = this.commands;
        config.kitItems = this.items;
        config.kitName = this.name;
        config.limitUsage = this.limitUsagePerPlayer;
        config.limitUsageDelay = new Duration(this.limitUsageDelay);
        config.usePerm = this.permission != null;
    }

    public String getKitName()
    {
        return this.name;
    }

    private static class KitCommandSender implements CommandSender
    {
        private static final String NAME_PREFIX = "Kit:";
        private final User user;

        public KitCommandSender(User user)
        {
            this.user = user;
        }

        public User getUser()
        {
            return this.user;
        }

        @Override
        public boolean isAuthorized(Permission perm)
        {
            return this.hasPermission(perm.getName());
        }

        @Override
        public String getLanguage()
        {
            return this.user.getLanguage();
        }

        @Override
        public void sendMessage(String category, String message, Object... params)
        {
            this.user.sendMessage(category, message, params);
        }

        @Override
        public void sendMessage(String string)
        {
            this.user.sendMessage(string);
        }

        @Override
        public void sendMessage(String[] strings)
        {
            this.user.sendMessage(strings);
        }

        @Override
        public Server getServer()
        {
            return this.user.getServer();
        }

        @Override
        public String getName()
        {
            return NAME_PREFIX + this.user.getName();
        }

        @Override
        public String getDisplayName()
        {
            return NAME_PREFIX + this.user.getDisplayName();
        }

        @Override
        public boolean isPermissionSet(String string)
        {
            return true;
        }

        @Override
        public boolean isPermissionSet(org.bukkit.permissions.Permission prmsn)
        {
            return true;
        }

        @Override
        public boolean hasPermission(String string)
        {
            return true;
        }

        @Override
        public boolean hasPermission(org.bukkit.permissions.Permission prmsn)
        {
            return true;
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, String string, boolean bln)
        {
            return null;
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin)
        {
            return null;
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, String string, boolean bln, int i)
        {
            return null;
        }

        @Override
        public PermissionAttachment addAttachment(Plugin plugin, int i)
        {
            return null;
        }

        @Override
        public void removeAttachment(PermissionAttachment pa)
        {}

        @Override
        public void recalculatePermissions()
        {}

        @Override
        public Set<PermissionAttachmentInfo> getEffectivePermissions()
        {
            return new HashSet<PermissionAttachmentInfo>();
        }

        @Override
        public boolean isOp()
        {
            return false;
        }

        @Override
        public void setOp(boolean bln)
        {}
    }
}
