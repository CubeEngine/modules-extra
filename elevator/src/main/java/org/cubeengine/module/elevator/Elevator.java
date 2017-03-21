package org.cubeengine.module.elevator;

import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.elevator.data.ElevatorData;
import org.cubeengine.module.elevator.data.ElevatorDataBuilder;
import org.cubeengine.module.elevator.data.ImmutableElevatorData;
import org.spongepowered.api.Sponge;

import javax.inject.Inject;

@ModuleInfo(name = "Elevator", description = "Lift up and down using signs")
public class Elevator extends Module
{
    @ModuleConfig private ElevatorConfig config;
    @Inject private EventManager em;
    @Inject private I18n i18n;

    @Enable
    public void onEnable()
    {
        Sponge.getDataManager().register(ElevatorData.class, ImmutableElevatorData.class, new ElevatorDataBuilder());
        em.registerListener(Elevator.class, new ElevatorListener(i18n, config));
    }
}
