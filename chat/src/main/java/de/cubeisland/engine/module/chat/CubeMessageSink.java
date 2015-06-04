package de.cubeisland.engine.module.chat;

import org.spongepowered.api.text.sink.MessageSink;
import org.spongepowered.api.util.command.CommandSource;

public class CubeMessageSink extends MessageSink
{
    @Override
    public Iterable<CommandSource> getRecipients()
    {
        return null;
    }
}
