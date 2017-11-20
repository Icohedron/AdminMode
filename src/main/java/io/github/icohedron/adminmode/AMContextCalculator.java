package io.github.icohedron.adminmode;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;

import java.util.Optional;
import java.util.Set;

public class AMContextCalculator implements ContextCalculator<Subject> {

    static final Context IN_ADMIN_MODE = new Context("adminmode-inAdminMode", "true");

    @Override
    public void accumulateContexts(Subject calculable, Set<Context> accumulator) {
        final Optional<CommandSource> commandSource = calculable.getCommandSource();
        if (commandSource.isPresent() && commandSource.get() instanceof Player) {
            final Player player = (Player) commandSource.get();
            if (AdminMode.getInstance().isInAdminMode(player)) {
                accumulator.add(IN_ADMIN_MODE);
            }
        }
    }

    @Override
    public boolean matches(Context context, Subject subject) {
        final Optional<CommandSource> commandSource = subject.getCommandSource();
        if (!commandSource.isPresent() || !(commandSource.get() instanceof Player)) {
            return false;
        }

        final Player player = (Player) commandSource.get();
        return context.equals(IN_ADMIN_MODE) && AdminMode.getInstance().isInAdminMode(player);
    }
}
