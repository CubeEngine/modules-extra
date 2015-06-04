/**
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
package de.cubeisland.engine.module.log.commands;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Complete;
import de.cubeisland.engine.butler.parametric.Label;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.module.service.command.CommandContext;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.command.completer.MaterialListCompleter;
import de.cubeisland.engine.module.service.command.completer.PlayerListCompleter;
import de.cubeisland.engine.module.service.command.completer.WorldCompleter;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.core.util.StringUtils;
import de.cubeisland.engine.module.core.util.TimeConversionException;
import de.cubeisland.engine.module.core.util.matcher.Match;
import de.cubeisland.engine.module.log.Log;
import de.cubeisland.engine.module.log.LogAttachment;
import de.cubeisland.engine.module.log.action.ActionManager;
import de.cubeisland.engine.module.log.action.ActionTypeCompleter;
import de.cubeisland.engine.module.log.action.BaseAction;
import de.cubeisland.engine.module.log.action.block.ActionBlock.BlockSection;
import de.cubeisland.engine.module.log.storage.Lookup;
import de.cubeisland.engine.module.log.storage.QueryParameter;
import de.cubeisland.engine.module.log.storage.ShowParameter;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.spongepowered.api.entity.EntityType;

public class LookupCommands
{
    private final Log module;
    private final ActionManager actionManager;

    public LookupCommands(Log module)
    {
        this.module = module;
        this.actionManager = module.getActionManager();
    }

    private void params(CommandSender context, String params)
    {
        if (params != null)
        {
            context.sendMessage("NOT YET DONE");
            // TODO show description
            return;
        }
        context.sendTranslated(NEUTRAL, "Registered ActionTypes:"); //TODO colors
        context.sendMessage(this.module.getActionManager().getActionTypesAsString());
        context.sendMessage("");
        context.sendTranslated(NEUTRAL, "Lookup/Rollback/Redo-Parameters:");
        context.sendMessage("");
        context.sendTranslated(NEUTRAL, " - action <actionType> like a block-break (See full list above)");
        context.sendTranslated(NEUTRAL, " - radius <radius> or sel, global, player:<radius>");
        context.sendTranslated(NEUTRAL, " - player <users> like p Faithcaio ");
        context.sendTranslated(NEUTRAL, " - entity <entities> like e sheep");
        context.sendTranslated(NEUTRAL, " - block <blocks> like b stone");
        context.sendTranslated(NEUTRAL, " - since <time> default is 3 days");
        context.sendTranslated(NEUTRAL, " - before <time>");
        context.sendTranslated(NEUTRAL, " - world <world> default is your current world");

        context.sendMessage("");
        context.sendTranslated(NEUTRAL, "Use {text:!} to exclude the parameters instead of including them.");
    }

    @Command(desc = "Queries a lookup in the database\n    Show availiable parameters with /lookup params")
    // TODO param for filter / chat / command / signtexts
    public void lookup(CommandContext context, @Label("params") @Optional String action,
                       @Named({"action", "a"}) @Complete(ActionTypeCompleter.class) String actions,
                       @Named({"radius", "r"}) String radius,
                       @Named({"player", "p"}) @Complete(PlayerListCompleter.class) String players,
                       @Named({"block", "b"}) @Complete(MaterialListCompleter.class) String blocks,
                       @Named({"entity", "e"}) String entities,
                       @Named({"since", "time", "t"}) String since,
                       @Named({"before"}) String before,
                       @Named({"world", "w", "in"}) @Complete(WorldCompleter.class) String world,
                       @Named({"limit", "pagelimit"}) Integer limit,
                       @Named({"page"}) Integer page,
                       @Named("params") @Complete(ActionTypeCompleter.class) String params,
                       @Flag(longName = "coordinates", name = "coords") boolean showCoord,
                       @Flag(longName = "detailed", name = "det") boolean detailed,
                       @Flag(longName = "nodate", name = "nd") boolean nodate,
                       @Flag(longName = "descending", name = "desc") boolean descending)
    {
        if ("params".equalsIgnoreCase(action) || params != null)
        {
            this.params(context.getSource(), params);
            return;
        }
        if (context.getSource() instanceof User)
        {
            if (!context.hasNamed())
            {
                try
                {
                    context.sendTranslated(NEGATIVE, "You have to provide parameters");
                    //((Dispatcher)context.getCommand()).getCommand("?").execute(context.getInvocation());
                    // TODO show all selected params of last lookup
                }
                catch (Exception e)
                {
                    throw new IllegalStateException(e);
                }
                return;
            }
            User user = (User)context.getSource();
            LogAttachment attachment = user.attachOrGet(LogAttachment.class, this.module);
            ShowParameter show = attachment.getLastShowParameter(); // gets last OR new Showparameter
            Lookup lookup = attachment.getLastLookup();
            if (!this.fillShowOptions(attachment, lookup, show, context.getSource(), showCoord, nodate, detailed, descending, limit, page, action)) // /lookup show / page <page>
            {
                return;
            }
            lookup = attachment.createNewCommandLookup();
            QueryParameter parameters = lookup.getQueryParameter();
            if (!(readActions(parameters, actions, user) &&
                readRadius(parameters, radius, user) &&
                readUser(parameters, players, user) &&
                readBlocks(parameters, blocks, user) &&
                readEntities(parameters, entities, user) &&
                readWorld(parameters, world, radius != null, user) &&
                readTimeSince(parameters, since, user) &&
                readTimeBefore(parameters, before, user)))
            {
                return;
            }
            attachment.queueShowParameter(show);
            this.module.getLogManager().fillLookupAndShow(lookup, user);
        }
        else
        {
            // TODO implement me
            System.out.println("Not implemented yet");
        }
    }

    @Command(desc = "Performs a rollback")
    public void rollback(CommandContext context, @Label("params") @Optional String action,
                         @Named({"action", "a"}) @Complete(ActionTypeCompleter.class) String actions,
                         @Named({"radius", "r"}) String radius, //<radius> OR selection|sel OR global|g OR player|p:<radius>
                         @Named({"player", "p"}) @Complete(PlayerListCompleter.class) String players,
                         @Named({"block", "b"}) @Complete(MaterialListCompleter.class) String blocks,
                         @Named({"entity", "e"}) String entities,
                         @Named({"since", "time", "t"}) String since, // def 3d
                         @Named({"before"}) String before,
                         @Named({"world", "w", "in"}) @Complete(WorldCompleter.class) String world,
                         @Flag(longName = "preview", name = "pre") boolean doPreview)
    {
        if (action != null)
        {
            if ("params".equalsIgnoreCase(action))
            {
                this.params(context.getSource(), action);
            }
        }
        else if (context.getSource() instanceof User)
        {
            if (!context.hasNamed())
            {
                context.sendTranslated(NEGATIVE, "You need to define parameters to rollback!");
                return;
            }
            User user = (User)context.getSource();
            LogAttachment attachment = user.attachOrGet(LogAttachment.class, this.module);
            Lookup lookup = attachment.createNewCommandLookup();
            QueryParameter params = lookup.getQueryParameter();
            if (!(this.readActions(params, actions, user) && this
                .readRadius(params, radius, user) && this
                .readUser(params, players, user) && this
                .readBlocks(params, blocks, user) && this
                .readEntities(params, entities, user) && this
                .readWorld(params, world, radius != null, user) && this
                .readTimeSince(params, since, user) && this
                .readTimeBefore(params, before, user)))
            {
                return;
            }
            if (doPreview)
            {
                this.module.getLogManager().fillLookupAndPreviewRollback(lookup, user);
            }
            else
            {
                this.module.getLogManager().fillLookupAndRollback(lookup, user);
            }
        }
        else
        {
            // TODO implement me
            System.out.println("Not implemented yet");
        }
    }

    @Command(desc = "Performs a rollback")
    public void redo(CommandContext context, @Label("params") @Optional String action,
                     @Named({"action", "a"}) @Complete(ActionTypeCompleter.class) String actions,
                     @Named({"radius", "r"}) String radius, //<radius> OR selection|sel OR global|g OR player|p:<radius>
                     @Named({"player", "p"}) @Complete(PlayerListCompleter.class) String players,
                     @Named({"block", "b"}) @Complete(MaterialListCompleter.class) String blocks,
                     @Named({"entity", "e"}) String entities,
                     @Named({"since", "time", "t"}) String since, // def 3d
                     @Named({"before"}) String before,
                     @Named({"world", "w", "in"}) @Complete(WorldCompleter.class) String world,
                     @Flag(longName = "preview", name = "pre") boolean doPreview)
    {
        if (action != null)
        {
            if ("params".equalsIgnoreCase(action))
            {
                this.params(context.getSource(), action);
            }
        }
        else if (context.getSource() instanceof User)
        {
            if (!context.hasNamed())
            {
                context.sendTranslated(NEGATIVE, "You need to define parameters to redo!");
                return;
            }
            User user = (User)context.getSource();
            LogAttachment attachment = user.attachOrGet(LogAttachment.class, this.module);
            Lookup lookup = attachment.createNewCommandLookup();
            QueryParameter params = lookup.getQueryParameter();
            if (!(this.readActions(params, actions, user) && this
                .readRadius(params, radius, user) && this
                .readUser(params, players, user) && this
                .readBlocks(params, blocks, user) && this
                .readEntities(params, entities, user) && this
                .readWorld(params, world, radius != null, user) && this
                .readTimeSince(params, since, user) && this
                .readTimeBefore(params, before, user)))
            {
                return;
            }
            if (doPreview)
            {
                this.module.getLogManager().fillLookupAndPreviewRedo(lookup, user);
            }
            else
            {
                this.module.getLogManager().fillLookupAndRedo(lookup, user);
            }
        }
        else
        {
            // TODO implement me
            System.out.println("Not implemented yet");
        }
    }

    private boolean readTimeBefore(QueryParameter params, String beforeString, User user)
    {
        try
        { // TODO date too
            if (beforeString == null)
            {
                return true;
            }
            long before = StringUtils.convertTimeToMillis(beforeString);
            params.before(new Date(System.currentTimeMillis() - before));
            return true;
        }
        catch (TimeConversionException e)
        {
            user.sendTranslated(NEGATIVE, "{input#time} is not a valid time value!", beforeString);
            return false;
        }
    }

    private boolean readTimeSince(QueryParameter params, String sinceString, User user)
    {
        try
        {
            if (sinceString != null)
            { // TODO date too
                long since = StringUtils.convertTimeToMillis(sinceString);
                params.since(new Date(System.currentTimeMillis() - since));
            }
            else
            {
                params.since(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30))); // defaulted to last 30 days
            }
            return true;
        }
        catch (TimeConversionException e)
        {
            user.sendTranslated(NEGATIVE, "{input#time} is not a valid time value!", sinceString);
            return false;
        }
    }

    private boolean readWorld(QueryParameter params, String worldString, boolean hasRadius, User user)
    {
        if (worldString == null)
        {
            return true;
        }
        if (hasRadius)
        {
            user.sendTranslated(NEGATIVE, "You cannot define a radius or selection and a world.");
            return false;
        }
        World world = user.getServer().getWorld(worldString);
        if (world == null)
        {
            user.sendTranslated(NEGATIVE, "Unknown world: {input#world}", worldString);
            return false;
        }
        params.setWorld(world);
        return true;
    }

    private boolean readRadius(QueryParameter params, String radiusString, User user)
    {
        if (radiusString == null)
        {
            return true;
        }
        if (radiusString.equalsIgnoreCase("selection") || radiusString.equalsIgnoreCase("sel"))
        {
            LogAttachment logAttachment = user.attachOrGet(LogAttachment.class, this.module);
            if (!logAttachment.applySelection(params))
            {
                user.sendTranslated(NEGATIVE, "You have to select a region first!");
                if (module.hasWorldEdit())
                {
                    user.sendTranslated(NEUTRAL, "Use worldedit to select a cuboid region!");
                }
                else
                {
                    user.sendTranslated(NEUTRAL, "Use this selection wand.");
                    LogCommands.giveSelectionTool(user);
                }
                return false;
            }
        }
        else if (radiusString.equalsIgnoreCase("global") || radiusString.equalsIgnoreCase("g"))
        {
            params.setWorld(user.getWorld());
        }
        else
        {
            User radiusUser = null;
            Integer radius;
            if (radiusString.contains(":"))
            {
                radiusUser = this.module.getCore().getUserManager()
                                        .findUser(radiusString.substring(0, radiusString.indexOf(":")));
                if (radiusUser == null)
                {
                    user.sendTranslated(NEGATIVE, "Invalid radius/location selection");
                    user.sendTranslated(POSITIVE, "The radius parameter can be: <radius> | selection | global | <player>[:<radius>]");
                    return false;
                }
                radiusString = radiusString.substring(radiusString.indexOf(":") + 1);
            }
            try
            {
                radius = Integer.parseInt(radiusString);
                if (radiusUser == null)
                {
                    radiusUser = user;
                }
                params.setLocationRadius(radiusUser.getLocation(), radius);
            }
            catch (NumberFormatException ex)
            {
                radiusUser = this.module.getCore().getUserManager().findUser(radiusString);
                if (radiusUser == null)
                {
                    user.sendTranslated(NEGATIVE, "Invalid radius/location selection");
                    user.sendTranslated(POSITIVE, "The radius parameter can be: <radius> | selection | global | <player>[:<radius>]");
                    return false;
                }
                params.setWorld(radiusUser.getWorld());
            }
        }
        return true;
    }

    private boolean fillShowOptions(LogAttachment attachment, Lookup lookup, ShowParameter show, CommandSender context,
                                    boolean showCoords, boolean nodate, boolean detailed, boolean descending, Integer limit, Integer page,
                                    String action)
    {
        show.showCoords = showCoords;
        show.showDate = !nodate;
        show.compress = !detailed;
        show.reverseOrder = !descending;
        if (limit != null)
        {
            if (limit > 100)
            {
                context.sendTranslated(NEUTRAL, "Your page-limit is to high! Showing 100 logs per page.");
                limit = 100;
            }
            show.pagelimit = limit;
        }
        if (action != null)
        {
            if ("show".equalsIgnoreCase(action))
            {
                if (lookup != null && lookup.queried())
                {
                    attachment.queueShowParameter(show);
                    lookup.show(attachment.getHolder());
                }
                else
                {
                    context.sendTranslated(NEGATIVE, "You have to do a query first!");
                }
                return false;
            }
            context.sendTranslated(NEGATIVE, "Unknown action: {}", action);
            return false;
        }
        if (page != null)
        {
            if (lookup != null && lookup.queried())
            {
                show.page = page;
                attachment.queueShowParameter(show);
                lookup.show(attachment.getHolder());
            }
            else
            {
                context.sendTranslated(NEGATIVE, "You have to do a query first!");
            }
            return false;
        }
        return true;
    }

    private boolean readUser(QueryParameter params, String userString, User sender)
    {
        if (userString == null)
        {
            return true;
        }
        String[] users = StringUtils.explode(",", userString);
        for (String name : users)
        {
            boolean negate = name.startsWith("!");
            if (negate)
            {
                name = name.substring(1);
            }
            User user = this.module.getCore().getUserManager().findExactUser(name);
            if (user == null)
            {
                sender.sendTranslated(NEGATIVE, "User {user} not found!", name);
                return false;
            }
            if (negate)
            {
                params.excludeUser(user.getUniqueId());
            }
            else
            {
                params.includeUser(user.getUniqueId());
            }
        }
        return true;
    }

    private boolean readBlocks(QueryParameter params, String block, User user)
    {
        if (block == null)
        {
            return true;
        }
        String[] names = StringUtils.explode(",", block);
        for (String name : names)
        {
            boolean negate = name.startsWith("!");
            if (negate)
            {
                name = name.substring(1);
            }
            Byte data = null;
            if (name.contains(":"))
            {
                String sub = name.substring(name.indexOf(":") + 1);
                try
                {
                    data = Byte.parseByte(sub);
                }
                catch (NumberFormatException ex)
                {
                    user.sendTranslated(NEGATIVE, "Invalid BlockData: {name#block}", sub);
                    return false;
                }
                name = name.substring(0, name.indexOf(":"));
            }
            Material material = Match.material().material(name);
            if (material == null)
            {
                user.sendTranslated(NEGATIVE, "Unknown Material: {name#material}", name);
                return false;
            }
            BlockSection blockData = new BlockSection(material);
            blockData.data = data == null ? 0 : data;
            if (negate)
            {
                params.excludeBlock(blockData);
            }
            else
            {
                params.includeBlock(blockData);
            }
        }
        return true;
    }

    private boolean readEntities(QueryParameter params, String entity, User user)
    {
        if (entity == null)
        {
            return true;
        }
        String[] names = StringUtils.explode(",", entity);
        for (String name : names)
        {
            boolean negate = name.startsWith("!");
            if (negate)
            {
                name = name.substring(1);
            }
            EntityType entityType = Match.entity().mob(name);
            if (entityType == null)
            {
                user.sendTranslated(NEGATIVE, "Unknown EntityType: {name#entity}", name);
                return false;
            }
            if (negate)
            {
                params.excludeEntity(entityType);
            }
            else
            {
                params.includeEntity(entityType);
            }
        }
        return true;
    }


    private boolean readActions(QueryParameter params, String input, User user)
    {
        if (input == null)
        {
            return true;
        }
        String[] inputs = StringUtils.explode(",", input);
        for (String actionString : inputs)
        {
            boolean negate = actionString.startsWith("!");
            if (negate)
            {
                actionString = actionString.substring(1);
            }
            List<Class<? extends BaseAction>> actions = this.actionManager.getAction(actionString);
            if (actions == null)
            {
                user.sendTranslated(NEGATIVE, "Unknown action-type: {name#action}", actionString);
                return false;
            }
            if (negate)
            {
                for (Class<? extends BaseAction> action : actions)
                {
                    params.excludeAction(action);
                }
            }
            else
            {
                for (Class<? extends BaseAction> action : actions)
                {
                    params.includeAction(action);
                }
            }
        }
        return true;
    }
}

