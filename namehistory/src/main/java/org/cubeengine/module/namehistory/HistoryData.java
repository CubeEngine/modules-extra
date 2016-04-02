package org.cubeengine.module.namehistory;


import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class HistoryData
{
    public final UUID uuid;
    public final List<NameChange> names;

    public HistoryData(UUID uuid, NameChange[] names)
    {
        this.uuid = uuid;
        this.names = Arrays.asList(names);
    }
}

