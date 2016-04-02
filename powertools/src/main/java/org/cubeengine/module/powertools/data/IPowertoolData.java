package org.cubeengine.module.powertools.data;

import java.util.List;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.ListValue;

public interface IPowertoolData
{
    Key<ListValue<String>> POWERS = KeyFactory.makeListKey(String.class, DataQuery.of("powers"));

    static int compare(IPowertoolData o2, IPowertoolData o1)
    {
        int compare = Integer.compare(o1.getPowers().size(), o2.getPowers().size());
        if (compare != 0)
        {
            return compare;
        }
        for (int i = 0; i < o1.getPowers().size(); i++)
        {
            compare = o1.getPowers().get(i).compareTo(o2.getPowers().get(i));
            if (compare != 0)
            {
                return compare;
            }
        }
        return 0;
    }

    List<String> getPowers();
}
