package de.cubeisland.cubeengine.core.bukkit;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.logging.Filter;
import java.util.logging.Logger;

import net.minecraft.server.v1_5_R2.EntityPlayer;
import net.minecraft.server.v1_5_R2.Item;
import net.minecraft.server.v1_5_R2.LocaleLanguage;
import net.minecraft.server.v1_5_R2.PlayerConnection;
import net.minecraft.server.v1_5_R2.RecipesFurnace;
import net.minecraft.server.v1_5_R2.ServerConnection;
import net.minecraft.server.v1_5_R2.TileEntityFurnace;
import org.bukkit.craftbukkit.libs.jline.console.ConsoleReader;
import org.bukkit.craftbukkit.v1_5_R2.CraftServer;
import org.bukkit.craftbukkit.v1_5_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_5_R2.help.SimpleHelpMap;
import org.bukkit.craftbukkit.v1_5_R2.inventory.CraftItemStack;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;

import de.cubeisland.cubeengine.core.CubeEngine;
import de.cubeisland.cubeengine.core.command.CubeCommand;
import de.cubeisland.cubeengine.core.user.User;

import org.apache.commons.lang.Validate;

import static de.cubeisland.cubeengine.core.logger.LogLevel.DEBUG;

/**
 * This class contains various methods to access bukkit-related stuff.
 */
public class BukkitUtils
{
    private static boolean hackSucceeded = false;
    private static final Field LOCALE_STRING_FIELD = findFirstField(String.class, LocaleLanguage.class);
    private static final Field NSH_LIST_FIELD = findFirstField(List.class, ServerConnection.class);
    private static Field handle;

    static
    {
        if (LOCALE_STRING_FIELD != null && NSH_LIST_FIELD != null)
        {
            hackSucceeded = true;
        }
        try
        {
            handle = CraftItemStack.class.getDeclaredField("handle");
            handle.setAccessible(true);
        }
        catch (Exception ignored)
        {}
    }

    private BukkitUtils()
    {}

    public static boolean isCompatible()
    {
        return (hackSucceeded && CraftServer.class == Bukkit.getServer().getClass() && SimplePluginManager.class == Bukkit.getPluginManager().getClass() && SimpleHelpMap.class == Bukkit.getHelpMap().getClass());
    }

    public static String getLanguage(CommandSender sender)
    {
        if (sender instanceof de.cubeisland.cubeengine.core.command.sender.CommandSender)
        {
            return ((de.cubeisland.cubeengine.core.command.sender.CommandSender)sender).getLanguage();
        }
        String language = null;
        if (sender instanceof Player)
        {
            language = getLanguage((Player)sender);
        }
        if (language == null)
        {
            language = CubeEngine.getConfiguration().defaultLanguage;
        }
        return language;
    }

    public static ConsoleReader getConsoleReader(final Server server)
    {
        return ((CraftServer)server).getServer().reader;
    }

    public static CommandMap getCommandMap(final Server server)
    {
        return ((CraftServer)server).getCommandMap();
    }

    /**
     * Returns the locale string of a player.
     *
     * @param player the Player instance
     * @return the locale string of the player
     */
    private static String getLanguage(Player player)
    {
        if (player.getClass() == CraftPlayer.class)
        {
            try
            {
                return (String)LOCALE_STRING_FIELD.get(((CraftPlayer)player).getHandle().getLocale());
            }
            catch (Exception ignored)
            {}
        }
        return null;
    }

    private static Field findFirstField(Class type, Object o)
    {
        return findFirstField(type, o.getClass());
    }

    private static Field findFirstField(Class<?> type, Class clazz)
    {
        for (Field field : clazz.getDeclaredFields())
        {
            if (type.isAssignableFrom(field.getType()))
            {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    public static SimpleCommandMap swapCommandMap(SimpleCommandMap commandMap)
    {
        Validate.notNull(commandMap, "The command map must not be null!");

        final Server server = Bukkit.getServer();
        final PluginManager pm = Bukkit.getPluginManager();

        Field serverField = findFirstField(CommandMap.class, server);
        Field pmField = findFirstField(CommandMap.class, pm);

        SimpleCommandMap oldMap = ((CraftServer)server).getCommandMap();
        if (serverField != null && pmField != null)
        {
            try
            {
                serverField.set(server, commandMap);
                pmField.set(pm, commandMap);
            }
            catch (Exception e)
            {
                CubeEngine.getLog().log(DEBUG, e.getLocalizedMessage(), e);
            }
        }
        return oldMap;
    }

    public static void resetCommandMap()
    {
        SimpleCommandMap current = ((CraftServer)Bukkit.getServer()).getCommandMap();
        if (current instanceof CubeCommandMap)
        {
            CubeCommandMap cubeMap = (CubeCommandMap)current;
            swapCommandMap(current = new SimpleCommandMap(Bukkit.getServer()));

            Collection<Command> commands = cubeMap.getKnownCommands().values();

            for (Command command : commands)
            {
                command.unregister(cubeMap);
                if (command instanceof CubeCommand)
                {
                    continue;
                }
                String prefix = "";
                if (command instanceof PluginCommand)
                {
                    prefix = ((PluginCommand)command).getPlugin().getName();
                }
                else if (command instanceof BukkitCommand)
                {
                    prefix = "bukkit";
                }
                current.register(command.getLabel(), prefix, command);
            }

            reloadHelpMap();
        }
    }

    private static Filter filter = null;
    private static CommandLogFilter commandFilter = null;

    public static void disableCommandLogging()
    {
        if (commandFilter == null)
        {
            commandFilter = new CommandLogFilter();
        }
        Logger logger = Bukkit.getLogger();
        filter = logger.getFilter();
        logger.setFilter(commandFilter);
    }

    public static void resetCommandLogging()
    {
        if (commandFilter != null)
        {
            Logger logger = Bukkit.getLogger();
            if (logger.getFilter() == commandFilter)
            {
                logger.setFilter(filter);
            }
            filter = null;
        }
    }

    private static PacketHookInjector hookInjector = null;
    /**
     * Registers the packet hook injector
     *
     * @param core the BukkitCore
     */
    public static synchronized void registerPacketHookInjector(BukkitCore core)
    {
        if (hookInjector == null)
        {
            hookInjector = new PacketHookInjector();
            core.getServer().getPluginManager().registerEvents(hookInjector, core);

            for (Player player : Bukkit.getOnlinePlayers())
            {
                hookInjector.swap(player);
            }
        }
    }

    public static int getPing(Player onlinePlayer)
    {
        return ((CraftPlayer)onlinePlayer).getHandle().ping;
    }

    private static class PacketHookInjector implements Listener
    {
        public void shutdown()
        {
            HandlerList.unregisterAll(this);

            for (Player player : Bukkit.getOnlinePlayers())
            {
                resetPlayerNetServerHandler(player);
            }
        }

        /**
         * The event listener swaps the joining player's NetServerHandler
         * instance with a custom one including all the magic to make the new
         * NetServerHandler work.
         *
         * @param event the join event object
         */
        @EventHandler(priority = EventPriority.LOW)
        public void onPlayerJoin(PlayerJoinEvent event)
        {
            this.swap(event.getPlayer());
        }

        public void swap(final Player player)
        {
            final EntityPlayer entity = ((CraftPlayer)player).getHandle();

            swapPlayerNetServerHandler(entity, new CubePlayerConnection(player, entity));
        }
    }

    private static final Location helperLocation = new Location(null, 0, 0, 0);

    @SuppressWarnings("unchecked")
    public static void swapPlayerNetServerHandler(EntityPlayer player, PlayerConnection newHandler)
    {
        if (NSH_LIST_FIELD == null)
        {
            return;
        }
        PlayerConnection oldHandler = player.playerConnection;
        try
        {
            if (oldHandler.getClass() != newHandler.getClass())
            {
                Location loc = player.getBukkitEntity().getLocation(helperLocation);
                newHandler.a(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

                ServerConnection sc = player.server.ae();
                ((List<PlayerConnection>)NSH_LIST_FIELD.get(sc)).remove(oldHandler);
                sc.a(newHandler);
                CubeEngine.getLog().log(DEBUG, "Replaced the NetServerHandler of player ''{0}''", player.getName());
                oldHandler.disconnected = true;
            }
        }
        catch (Exception e)
        {
            player.playerConnection = oldHandler;
            CubeEngine.getLog().log(DEBUG, e.getLocalizedMessage(), e);
        }
    }

    public static void resetPlayerNetServerHandler(Player player)
    {
        final EntityPlayer entity = ((CraftPlayer)player).getHandle();

        swapPlayerNetServerHandler(entity, new PlayerConnection(entity.server, entity.playerConnection.networkManager, entity));
    }

    public static void reloadHelpMap()
    {
        SimpleHelpMap helpMap = (SimpleHelpMap)Bukkit.getHelpMap();

        helpMap.clear();
        helpMap.initializeGeneralTopics();
        helpMap.initializeCommands();
    }

    public static boolean isInvulnerable(Player player)
    {
        if (player != null)
        {
            if (player instanceof User)
            {
                player = ((User)player).getOfflinePlayer().getPlayer();
            }
            if (player != null && player instanceof CraftPlayer)
            {
                return ((CraftPlayer)player).getHandle().abilities.isInvulnerable;
            }
        }
        return false;
    }

    public static void setInvulnerable(Player player, boolean state)
    {
        if (player != null && player instanceof User)
        {
            player = ((User)player).getOfflinePlayer().getPlayer();
        }
        if (player != null && player instanceof CraftPlayer)
        {
            ((CraftPlayer)player).getHandle().abilities.isInvulnerable = state;
            ((CraftPlayer)player).getHandle().updateAbilities();
        }
    }

    public static synchronized void cleanup()
    {
        if (hookInjector != null)
        {
            hookInjector.shutdown();
            hookInjector = null;
        }

        resetCommandMap();
        resetCommandLogging();
    }

    public static net.minecraft.server.v1_5_R2.ItemStack getNmsItemStack(ItemStack item)
    {
        if (item instanceof CraftItemStack)
        {
            try
            {
                return (net.minecraft.server.v1_5_R2.ItemStack)handle.get(item);
            }
            catch (Exception ignored)
            {}
        }
        return null;
    }

    public static void setOnlineMode(boolean mode)
    {
        ((CraftServer)Bukkit.getServer()).getServer().setOnlineMode(mode);
        ((CraftServer)Bukkit.getServer()).getServer().getPropertyManager().savePropertiesFile();
    }

    /**
     * Returns true if given material is allowed to be placed in the top brewingstand slot
     *
     * @param material
     * @return
     */
    public static boolean canBePlacedInBrewingstand(Material material)
    {
        return Item.byId[material.getId()].w();
    }

    public static boolean isFuel(ItemStack item) {
        // Create an NMS item stack
        net.minecraft.server.v1_5_R2.ItemStack nmss = CraftItemStack.asNMSCopy(item);
        // Use the NMS TileEntityFurnace to check if the item being clicked is a fuel
        return TileEntityFurnace.isFuel(nmss);
    }

    public static boolean isSmeltable(ItemStack item) {
        net.minecraft.server.v1_5_R2.ItemStack nmss = CraftItemStack.asNMSCopy(item);
        // If the result of that item being cooked is null, it is not cookable
        return RecipesFurnace.getInstance().getResult(nmss.getItem().id) != null;
    }
}
