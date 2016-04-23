package org.cubeengine.module.spawn;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.DefaultValue;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.option.OptionSubject;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;

public class SubjectReader implements ArgumentReader<OptionSubject>, DefaultValue<OptionSubject>
{
    private PermissionService pm;

    public SubjectReader(PermissionService pm)
    {
        this.pm = pm;
    }

    @Override
    public OptionSubject read(Class aClass, CommandInvocation commandInvocation) throws ReaderException
    {
        String token = commandInvocation.currentToken();
        if (pm.getGroupSubjects().hasRegistered(token))
        {
            return ((OptionSubject)pm.getGroupSubjects().get(token));
        }

        // TODO msg i18n.sendTranslated(ctx, NEGATIVE, "Could not find the role {input}!", role, context);
        return null;
    }

    @Override
    public OptionSubject getDefault(CommandInvocation invocation)
    {
        if (invocation.getCommandSource() instanceof Player)
        {
            return ((OptionSubject)pm.getUserSubjects().get(((Player)invocation.getCommandSource()).getIdentifier()));
        }
        // TODO exception
        return null;
    }
}
