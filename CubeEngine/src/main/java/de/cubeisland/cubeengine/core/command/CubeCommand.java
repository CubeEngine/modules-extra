package de.cubeisland.cubeengine.core.command;


import java.util.*;

import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import de.cubeisland.cubeengine.core.CubeEngine;
import de.cubeisland.cubeengine.core.command.exception.IncorrectUsageException;
import de.cubeisland.cubeengine.core.command.exception.MissingParameterException;
import de.cubeisland.cubeengine.core.command.exception.PermissionDeniedException;
import de.cubeisland.cubeengine.core.command.sender.BlockCommandSender;
import de.cubeisland.cubeengine.core.command.sender.CommandSender;
import de.cubeisland.cubeengine.core.command.sender.ConsoleCommandSender;
import de.cubeisland.cubeengine.core.command.sender.WrappedCommandSender;
import de.cubeisland.cubeengine.core.module.Module;
import de.cubeisland.cubeengine.core.permission.PermDefault;
import de.cubeisland.cubeengine.core.permission.Permission;
import de.cubeisland.cubeengine.core.permission.PermissionContainer;
import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.core.util.StringUtils;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import org.apache.commons.lang.Validate;

import static de.cubeisland.cubeengine.core.i18n.I18n._;
import static de.cubeisland.cubeengine.core.logger.LogLevel.ERROR;

/**
 * This class is the base for all of our commands
 * it implements the execute() method which provides error handling and calls the
 * run() method which should be implemented by the extending classes
 */
public abstract class CubeCommand extends Command
{
    private CubeCommand parent;
    private final Module module;
    private final Map<String, CubeCommand> children;
    protected final List<String> childrenAliases;
    private final ContextFactory contextFactory;
    private boolean loggable;
    private boolean generatePermission;
    private PermDefault generatedPermissionDefault;

    public CubeCommand(Module module, String name, String description, ContextFactory contextFactory)
    {
        this(module, name, description, "", new ArrayList<String>(0), contextFactory);
    }

    public CubeCommand(Module module, String name, String description, String usage, List<String> aliases, ContextFactory contextFactory)
    {
        super(name, description, usage.trim(), aliases);
        if ("?".equals(name))
        {
            throw new IllegalArgumentException("Invalid command name: " + name);
        }
        this.parent = null;
        this.module = module;
        this.contextFactory = contextFactory;

        this.children = new THashMap<String, CubeCommand>();
        this.childrenAliases = new ArrayList<String>();
        this.loggable = true;
        this.generatePermission = false;
        this.generatedPermissionDefault = PermDefault.DEFAULT;
    }

    public void setLoggable(boolean state)
    {
        this.loggable = state;
    }

    public boolean isLoggable()
    {
        return this.loggable;
    }

    public boolean isGeneratePermission()
    {
        return generatePermission;
    }

    public void setGeneratePermission(boolean generatePermission)
    {
        this.generatePermission = generatePermission;
    }

    public PermDefault getGeneratedPermissionDefault()
    {
        return generatedPermissionDefault;
    }

    public void setGeneratedPermissionDefault(PermDefault generatedPermissionDefault)
    {
        this.generatedPermissionDefault = generatedPermissionDefault;
    }

    protected void registerAlias(String[] names, String[] parents)
    {
        this.registerAlias(names, parents, "", "");
    }

    @Override
    public void setPermission(String permission)
    {
        this.generatePermission = false;
        super.setPermission(permission);
    }

    protected Permission generatePermissionNode()
    {
        Permission commandBase = this.getModule().getModulePermission().createAbstractChild("command");
        LinkedList<String> cmds = new LinkedList<String>();
        CubeCommand cmd = this;
        do
        {
            cmds.addFirst(cmd.getName());
        }
        while ((cmd = cmd.getParent()) != null);
        Permission result = commandBase;
        Iterator<String> iter = cmds.iterator();
        while (iter.hasNext())
        {
            String permString = iter.next();
            if (iter.hasNext())
            {
                result = result.createAbstractChild(permString);
            }
            else
            {
                result = result.createChild(permString, this.getGeneratedPermissionDefault());
            }
        }
        return result;
    }

    public void updateGeneratedPermission()
    {
        if (this.generatePermission)
        {
            PermDefault def = null;
            String node = this.getPermission();
            if (node != null)
            {
                def = this.getModule().getCore().getPermissionManager().getDefaultFor(node);
            }
            if (def == null)
            {
                def = this.getGeneratedPermissionDefault();
            }
            this.setGeneratedPermission(def);
        }
    }

    public void setGeneratedPermission(PermDefault def)
    {
        this.generatePermission = true;
        this.setGeneratedPermissionDefault(def);
        Permission perm = this.generatePermissionNode();
        super.setPermission(perm.getPermission());
        this.getModule().getCore().getPermissionManager().registerPermission(this.getModule(), perm);
    }

    protected void registerAlias(String[] names, String[] parents, String prefix, String suffix)
    {
        if (names.length == 0)
        {
            throw new IllegalArgumentException("You have to specify at least 1 name!");
        }
        List<String> aliases = Collections.emptyList();
        if (names.length > 1)
        {
            aliases = new ArrayList<String>(names.length - 1);
            for (int i = 1; i < names.length; ++i)
            {
                aliases.add(names[i]);
            }
        }
        this.getModule().registerCommand(new AliasCommand(this, names[0], aliases, prefix, suffix), parents);
    }

    public ContextFactory getContextFactory()
    {
        return this.contextFactory;
    }

    /**
     * This method implodes the path of this command, so the name of the command and the name of every parent
     *
     * @param delimiter the delimiter
     * @return the imploded path
     */
    protected final String implodeCommandParentNames(String delimiter)
    {
        LinkedList<String> cmds = new LinkedList<String>();
        CubeCommand cmd = this;
        do
        {
            cmds.addFirst(cmd.getName());
        }
        while ((cmd = cmd.getParent()) != null);

        return StringUtils.implode(delimiter, cmds);
    }

    private static String replaceSemiOptionalArgs(CommandSender sender, String usage)
    {
        if (sender instanceof User)
        {
            return usage.replace('{', '[').replace('}', ']');
        }
        else
        {
            return usage.replace('{', '<').replace('}', '>');
        }
    }

    @Override
    public String getUsage()
    {
        return "/" + this.implodeCommandParentNames(" ") + " " + _(this.module, this.usageMessage);
    }

    /**
     * This overload returns the usage translated for the given CommandSender
     *
     * @param sender the command sender
     * @return the usage string
     */
    public String getUsage(CommandSender sender)
    {
        return "/" + this.implodeCommandParentNames(" ") + " " + replaceSemiOptionalArgs(sender, _(sender, this.module, super.getUsage()));
    }

    /**
     * This overload returns the usage translated for the given CommandContext
     * using the correct labels
     *
     * @param context the command context
     * @return the usage string
     */
    public String getUsage(CommandContext context)
    {
        final CommandSender sender = context.getSender();
        return "/" + StringUtils.implode(" ", context.getLabels()) + " " + replaceSemiOptionalArgs(sender, _(sender, this.module, super.getUsage()));
    }

    /**
     * This overload returns the usage translated for the given CommandSender
     * using the correct labels
     *
     * @param sender       the command sender
     * @param parentLabels a list of labels
     * @return the usage string
     */
    public String getUsage(CommandSender sender, List<String> parentLabels)
    {
        return "/" + StringUtils.implode(" ", parentLabels) + " " + this.getName() + " " + _(sender, this.module, super.getUsage());
    }

    /**
     * Returns a child command by name without typo correction
     *
     * @param name the child name
     * @return the child or null if not found
     */
    public CubeCommand getChild(String name)
    {
        if (name == null)
        {
            return null;
        }

        return this.children.get(name.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Adds a child to this command
     *
     * @param command the command to add
     */
    public void addChild(CubeCommand command)
    {
        Validate.notNull(command, "The command must not be null!");

        if (this == command)
        {
            throw new IllegalArgumentException("You can't register a command as a child of itself!");
        }

        if (command.getParent() != null)
        {
            throw new IllegalArgumentException("The given command is already registered! Use aliases instead!");
        }

        this.children.put(command.getName(), command);
        command.parent = this;
        this.onRegister();
        for (String alias : command.getAliases())
        {
            alias = alias.toLowerCase(Locale.ENGLISH);
            this.children.put(alias, command);
            this.childrenAliases.add(alias);
        }
    }

    /**
     * Checks whether this command has a child with the given name
     *
     * @param name the name to check for
     * @return true if a matching command was found
     */
    public boolean hasChild(String name)
    {
        return name != null && this.children.containsKey(name.toLowerCase());
    }

    /**
     * Checks whether this command has children
     *
     * @return true if that is the case
     */
    public boolean hasChildren()
    {
        return !this.children.isEmpty();
    }

    /**
     * Returns a Set of all children
     *
     * @return a Set of children
     */
    public Set<CubeCommand> getChildren()
    {
        return new THashSet<CubeCommand>(this.children.values());
    }

    /**
     * Returns a Set of the children's names
     *
     * @return a set of children's names
     */
    public Set<String> getChildrenNames()
    {
        return new THashSet<String>(this.children.keySet());
    }

    /**
     * Removes a child from this command
     *
     * @param name the name fo the child
     */
    public void removeChild(String name)
    {
        CubeCommand cmd = this.getChild(name);
        Iterator<Map.Entry<String, CubeCommand>> iter = this.children.entrySet().iterator();

        while (iter.hasNext())
        {
            if (iter.next().getValue() == cmd)
            {
                iter.remove();
            }
        }
        this.childrenAliases.removeAll(cmd.getAliases());
        cmd.onRemove();
        cmd.parent = null;
    }

    private static CommandSender wrapSender(org.bukkit.command.CommandSender bukkitSender)
    {
        if (bukkitSender instanceof CommandSender)
        {
            return (CommandSender)bukkitSender;
        }
        else if (bukkitSender instanceof Player)
        {
            return CubeEngine.getUserManager().getExactUser((Player)bukkitSender);
        }
        else if (bukkitSender instanceof org.bukkit.command.ConsoleCommandSender)
        {
            return new ConsoleCommandSender((org.bukkit.command.ConsoleCommandSender)bukkitSender);
        }
        else if (bukkitSender instanceof org.bukkit.command.BlockCommandSender)
        {
            return new BlockCommandSender((org.bukkit.command.BlockCommandSender)bukkitSender);
        }
        else
        {
            return new WrappedCommandSender(bukkitSender);
        }
    }

    /**
     * This is the entry point from Bukkit into our own command handling code
     *
     * @param bukkitSender the Bukkit command sender which will be wrapped
     * @param label the command label
     * @param args the command arguments
     * @return true if the command succeeded
     */
    @Override
    public final boolean execute(org.bukkit.command.CommandSender bukkitSender, String label, String[] args)
    {
        return this.execute(wrapSender(bukkitSender), args, label, new Stack<String>());
    }

    private boolean execute(CommandSender sender, String[] args, String label, Stack<String> labels)
    {
        try
        {
            labels.push(label);
            if (args.length > 0)
            {
                if ("?".equals(args[0]))
                {
                    this.help(new HelpContext(this, sender, labels, args));
                    return true;
                }
                CubeCommand child = this.getChild(args[0]);
                if (child != null)
                {
                    return child.execute(sender, Arrays.copyOfRange(args, 1, args.length), args[0], labels);
                }
            }
            if (!this.testPermissionSilent(sender))
            {
                throw new PermissionDeniedException();
            }
            final CommandContext ctx = this.getContextFactory().parse(this, sender, labels, args);
            CommandResult result = this.run(ctx);
            if (result != null)
            {
                result.show(ctx);
            }
        }
        catch (MissingParameterException e)
        {
            sender.sendMessage("core", "&cThe parameter &6%s&c is missing!", e.getMessage());
        }
        catch (IncorrectUsageException e)
        {
            sender.sendMessage(e.getMessage());
            sender.sendMessage("core", "&eProper usage: &f%s", this.getUsage(sender));
        }
        catch (PermissionDeniedException e)
        {
            sender.sendMessage("core", "&cYou're not allowed to do this!");
            sender.sendMessage("core", "&cContact an administrator if you think this is a mistake!");
        }
        catch (Exception e)
        {
            sender.sendMessage("core", "&4An unknown error occurred while executing this command!");
            sender.sendMessage("core", "&4Please report this error to an administrator.");
            this.module.getLogger().log(ERROR, e.getLocalizedMessage(), e);
        }

        return true;
    }

    private List<String> tabCompleteFallback(org.bukkit.command.CommandSender bukkitSender, String alias, String[] args) throws IllegalArgumentException
    {
        return super.tabComplete(bukkitSender, alias, args);
    }

    @Override
    public final List<String> tabComplete(org.bukkit.command.CommandSender bukkitSender, String alias, String[] args) throws IllegalArgumentException
    {
        CubeCommand completer = this;
        if (args.length > 0 && !args[0].isEmpty())
        {
            CubeCommand child = this.getChild(args[0]);
            if (child != null)
            {
                args = Arrays.copyOfRange(args, 1, args.length);
                completer = child;
            }
        }
        List<String> result = completer.tabComplete(wrapSender(bukkitSender), alias, args);
        if (result == null)
        {
            result = completer.tabCompleteFallback(bukkitSender, alias, args);
        }
        final int max = this.getModule().getCore().getConfiguration().commandTabCompleteOffers;
        if (result.size() > max)
        {
            result = result.subList(0, max);
        }
        return result;
    }

    public List<String> tabComplete(CommandSender sender, String label, String[] args)
    {
        return null;
    }

    /**
     * Returns the module this command was registered by
     *
     * @return a module
     */
    public Module getModule()
    {
        return this.module;
    }

    /**
     * Returns the parent of this command or null if there is none
     *
     * @return the parent command or null
     */
    public CubeCommand getParent()
    {
        return this.parent;
    }

    /**
     * This method handles the command execution
     *
     * @param context The CommandContext containing all the necessary information
     * @throws Exception if an error occurs
     */
    public abstract CommandResult run(CommandContext context) throws Exception;

    /**
     * This method is called if the help page of this command was requested by the ?-action
     *
     * @param context The CommandContext containing all the necessary information
     * @throws Exception if an error occurs
     */
    public abstract void help(HelpContext context) throws Exception;

    public void onRegister()
    {}

    public void onRemove()
    {}
}
