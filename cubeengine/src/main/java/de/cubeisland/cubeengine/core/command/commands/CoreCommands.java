package de.cubeisland.cubeengine.core.command.commands;

import org.bukkit.plugin.PluginManager;

import de.cubeisland.cubeengine.core.Core;
import de.cubeisland.cubeengine.core.CorePerms;
import de.cubeisland.cubeengine.core.bukkit.BukkitCore;
import de.cubeisland.cubeengine.core.bukkit.BukkitUtils;
import de.cubeisland.cubeengine.core.command.CommandContext;
import de.cubeisland.cubeengine.core.command.ContainerCommand;
import de.cubeisland.cubeengine.core.command.parameterized.Flag;
import de.cubeisland.cubeengine.core.command.parameterized.ParameterizedContext;
import de.cubeisland.cubeengine.core.command.reflected.Command;
import de.cubeisland.cubeengine.core.command.CommandSender;
import de.cubeisland.cubeengine.core.command.sender.ConsoleCommandSender;
import de.cubeisland.cubeengine.core.permission.PermDefault;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.core.user.UserManager;

import static java.util.Arrays.asList;

public class CoreCommands extends ContainerCommand
{

    private final BukkitCore core;

    public CoreCommands(Core core)
    {
        super(core.getModuleManager().getCoreModule(), "cubeengine", "These are the basic commands of the CubeEngine.", asList("ce"));
        this.core = (BukkitCore)core;
    }

    @Command(desc = "Reloads the whole CubeEngine")
    public void reload(CommandContext context)
    {
        PluginManager pm = this.core.getServer().getPluginManager();
        pm.disablePlugin(this.core);
        pm.enablePlugin(this.core);
    }

    @Command(desc = "Reloads all of the modules!")
    public void reloadmodules(CommandContext context)
    {
        this.core.getModuleManager().unloadModules();
        this.core.getModuleManager().loadModules(this.core.getFileManager().getModulesDir());
    }

    @Command(names = {
        "setpassword", "setpw"
    }, desc = "Sets your password.", min = 1, max = 2, usage = "<password> [player]")
    public void setPassword(CommandContext context)
    {
        CommandSender sender = context.getSender();
        User target = null;
        if (context.hasArg(1))
        {
            target = context.getUser(1);
            if (target == null)
            {
                sender.sendTranslated("&cUser %s not found!");
                return;
            }
        }
        else if (sender instanceof User)
        {
            target = (User)sender;
        }
        else
        {
            sender.sendTranslated("&cNo user given!");
            return;
        }

        if (target == sender && !sender.isAuthorized(CorePerms.COMMAND_SETPASSWORD_OTHER))
        {
            context.sendTranslated("&cYou are not allowed to change the password of an other user!");
            return;
        }
        core.getUserManager().setPassword(target, context.getString(0));
        if (sender == target)
        {
            sender.sendTranslated("&aThe user's password has been set!");
        }
        else
        {
            sender.sendTranslated("&aYour password has been set!");
        }
    }

    @Command(names = {
        "clearpassword", "clearpw"
    }, desc = "Clears your password.", max = 1, usage = "[<player>|-a]", flags = @Flag(longName = "all", name = "a"))
    public void clearPassword(ParameterizedContext context)
    {
        CommandSender sender = context.getSender();
        if (context.hasFlag("a"))
        {
            if (CorePerms.COMMAND_CLEARPASSWORD_ALL.isAuthorized(context.getSender()))
            {
                final UserManager um = this.getModule().getCore().getUserManager();
                um.resetAllPasswords();
                for (User user : um.getLoadedUsers())
                {
                    user.passwd = null; //update loaded users
                }
                sender.sendTranslated("&All passwords reset!");
            }
            else
            {
                context.sendTranslated("&cYou are not allowed to clear all passwords!");
            }
        }
        else if (context.hasArg(0))
        {
            if (!CorePerms.COMMAND_CLEARPASSWORD_OTHER.isAuthorized(context.getSender()))
            {
                context.sendTranslated("&cYou are not allowed to clear the password of other users!");
                return;
            }
            User target = context.getUser(0);
            if (target != null)
            {
                this.core.getUserManager().resetPassword(target);
                sender.sendTranslated("&aThe user's password has been reset!");
            }
            else
            {
                context.sendTranslated("&cUser &c not found!");
            }
        }
        else if (sender instanceof User)
        {
            this.core.getUserManager().resetPassword((User)sender);
            sender.sendTranslated("Your password has been reset!");
        }
    }

    @Command(desc = "Logs you in with your password!", usage = "<password>", min = 1, max = 1, permDefault = PermDefault.TRUE)
    public void login(CommandContext context)
    {
        CommandSender sender = context.getSender();
        if (sender instanceof User)
        {
            User user = (User)sender;
            if (user.isLoggedIn())
            {
                context.sendTranslated("&aYou are already logged in!");
                return;
            }
            boolean isLoggedIn = core.getUserManager().login(user, context.getString(0));
            if (isLoggedIn)
            {
                user.sendTranslated("&aYou logged in successfully!");
            }
            else
            {
                user.sendTranslated("&cWrong password!");
            }
        }
        else
        {
            sender.sendTranslated("&cOnly players can log in!");
        }
    }

    @Command(desc = "Logs you out!", max = 0)
    public void logout(CommandContext context)
    {
        CommandSender sender = context.getSender();
        if (sender instanceof User)
        {
            User user = (User)sender;
            if (!user.isLoggedIn())
            {
                sender.sendTranslated("&eYou're not logged in!");
            }
            else
            {
                user.logout();
                sender.sendTranslated("&aYou're now logged out.");
            }
        }
        else if (sender instanceof ConsoleCommandSender)
        {
            sender.sendTranslated("&eYou might use /stop for this.");
        }
    }

    @Command(desc = "Toggles the online mode")
    public void onlinemode(CommandContext context)
    {
        final boolean newState = !this.core.getServer().getOnlineMode();
        BukkitUtils.setOnlineMode(newState);

        if (newState)
        {
            context.sendTranslated("&aThe server is now in online-mode.");
        }
        else
        {
            context.sendTranslated("&aThe server is not in offline-mode.");
        }
    }
}