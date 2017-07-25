package io.github.icohedron.adminmode;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.gen.ClientConnectionEvent$Disconnect$Impl;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Plugin(id = "adminmode", name = "Admin Mode", description = "Admin mode for survival servers")
public class AdminMode {

    private Set<UUID> active;

    @Listener
    public void onInitialization(GameInitializationEvent event) {
        active = new HashSet<>();

        CommandSpec command = CommandSpec.builder()
                .permission("adminmode.command")
                .executor((src, args) -> {
                    if (!(src instanceof Player)) {
                        src.sendMessage(Text.of(TextColors.RED, "Only a player may execute this command"));
                        return CommandResult.empty();
                    }

                    Player player = ((Player) src);
                    UUID uuid = player.getUniqueId();
                    if (active.contains(uuid)) {
                        return disableAdminMode(player);
                    } else {
                        return enableAdminMode(player);
                    }
                })
                .build();

        Sponge.getCommandManager().register(this, command, "adminmode", "am");
    }

    private CommandResult enableAdminMode(Player player) {
        if (player.supports(Keys.CAN_FLY)) {
            active.add(player.getUniqueId());
            player.offer(Keys.CAN_FLY, true);
            player.sendMessage(Text.of(TextColors.YELLOW, "You are now in admin mode"));
            return CommandResult.success();
        }
        player.sendMessage(Text.of(TextColors.RED, "Unable to enable admin mode"));
        return CommandResult.empty();
    }

    private CommandResult disableAdminMode(Player player) {
        if (player.supports(Keys.CAN_FLY)) {
            active.remove(player.getUniqueId());
            player.offer(Keys.CAN_FLY, false);
            player.sendMessage(Text.of(TextColors.YELLOW, "You are no longer in admin mode"));
            return CommandResult.success();
        }
        player.sendMessage(Text.of(TextColors.RED, "Unable to disable admin mode"));
        return CommandResult.empty();
    }

    @Listener
    public void onDamagePlayer(DamageEntityEvent event) {
        if (event.getTargetEntity() instanceof Player) {
            Player player = (Player) event.getTargetEntity();
            if (active.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event, @First Player player) {
        disableAdminMode(player);
    }
}
