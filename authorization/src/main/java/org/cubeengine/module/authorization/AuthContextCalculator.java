/*
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
package org.cubeengine.module.authorization;

import static org.spongepowered.api.service.permission.PermissionService.SUBJECTS_USER;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;

import java.util.Set;
import java.util.UUID;

/**
 * Adds the Context authorized|pw to any logged in user
 * TODO also handle non-user Subjects
 */
public class AuthContextCalculator implements ContextCalculator<Subject> {

    public static final Context CONTEXT = new Context("authorized", "pw");
    private Authorization module;

    public AuthContextCalculator(Authorization module)
    {
        this.module = module;
    }

    @Override
    public void accumulateContexts(Subject subject, Set<Context> set)
    {
        if (isLoggedIn(subject))
        {
            set.add(CONTEXT);
        }
    }

    private boolean isLoggedIn(Subject subject)
    {
        String type = subject.getContainingCollection().getIdentifier();
        String id = subject.getIdentifier();
        if (SUBJECTS_USER.equals(type))
        {
            return module.isLoggedIn(UUID.fromString(id));
        }
        return false;
    }

    @Override
    public boolean matches(Context context, Subject subject)
    {
        if (CONTEXT.equals(context))
        {
            return isLoggedIn(subject);
        }
        return false;
    }
}
