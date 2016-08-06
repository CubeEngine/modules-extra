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
