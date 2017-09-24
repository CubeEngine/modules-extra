package org.cubeengine.module.tablist;

import static org.spongepowered.api.text.serializer.TextSerializers.FORMATTING_CODE;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import javax.inject.Singleton;

@Singleton
@Module(dependencies = @Dependency("cubeengine-bigdata"))
public class Tablist extends CubeEngineModule
{
    @ModuleConfig private TablistConfig config;

    @Listener
    public void onEnable(GameInitializationEvent event)
    {
        
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join event)
    {
        Player player = event.getTargetEntity();
        String prefix = player.getOption("tablist-prefix").orElse("");

        if (this.config.header != null && !this.config.header.isEmpty())
        {
            player.getTabList().setHeader(FORMATTING_CODE.deserialize(this.config.header));
        }
        for (TabListEntry tle : player.getTabList().getEntries())
        {
            if (tle.getProfile().equals(player.getProfile()))
            {
                tle.setDisplayName(Text.of(FORMATTING_CODE.deserialize(prefix + tle.getDisplayName().orElse(Text.EMPTY)).toPlain()));
            }
        }

    }
}
