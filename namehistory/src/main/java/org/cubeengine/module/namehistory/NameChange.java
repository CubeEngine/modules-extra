package org.cubeengine.module.namehistory;

import java.util.Date;
import com.google.common.base.Optional;

public class NameChange
{
    public final String name;
    public final Optional<Date> changedToAt;

    public NameChange(String name, Date changedToAt)
    {
        this.name = name;
        this.changedToAt = Optional.fromNullable(changedToAt);
    }
}
