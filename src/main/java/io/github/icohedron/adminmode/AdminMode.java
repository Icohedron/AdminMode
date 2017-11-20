package io.github.icohedron.adminmode;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.Tristate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Plugin(id = "adminmode", name = "Admin Mode", version = "1.0.0-S7.0",
        description = "Admin mode for survival servers", authors = { "Icohedron" })
public class AdminMode {

    private final Text prefix = Text.of(TextColors.GRAY, "[", TextColors.GOLD, "AdminMode", TextColors.GRAY, "] ");

    private final String configFileName = "adminmode.conf";
    private final String playerDataFileName = "amplayerdata.dat";

    @Inject @ConfigDir(sharedRoot = false) private Path configurationPath;

    @Inject private Logger logger;

    private ConfigurationLoader<CommentedConfigurationNode> amplayerdataConfig;
    private ConfigurationNode amplayerdataNode;

    // A set would be a better data structure for this, but it has trouble being serialized on some Linux machines.
//    private Set<UUID> leftViaDisconnect;
    private List<UUID> leftViaDisconnect;

    private Map<UUID, AMPlayerData> active;
    private List<ItemStackSnapshot> adminModeItems;
    private List<String> adminModePermissions;
    private Map<String, Boolean> attributes;

    private static AdminMode instance;
    private static PermissionService permissionService;

    @Listener
    public void onConstruct(GameConstructionEvent event) {
        instance = this;
    }

    static AdminMode getInstance() {
        if (instance == null) {
            throw new RuntimeException("AdminMode not initialized");
        }
        return instance;
    }

    private static Optional<PermissionService> getPermissionService() {
        return Optional.ofNullable(permissionService);
    }

    @Listener
    public void onInitialization(GameInitializationEvent event) {
        active = new HashMap<>();
//        leftViaDisconnect = new HashSet<>();
        leftViaDisconnect = new LinkedList<>();

        Optional<PermissionService> permissionServiceOptional = Sponge.getServiceManager().provide(PermissionService.class);
        if (permissionServiceOptional.isPresent()) {
            permissionService = permissionServiceOptional.get();
            permissionService.registerContextCalculator(new AMContextCalculator());
        }

        loadConfig();
        loadPlayerData();
        deserializeLeftViaDisconnect();
        registerCommands();
        Sponge.getEventManager().registerListeners(this, new AMListener());
        logger.info("Finished initialization");
    }

    private void loadConfig() {
        File configFile = new File(configurationPath.toFile(), configFileName);
        ConfigurationLoader<CommentedConfigurationNode> configurationLoader = HoconConfigurationLoader.builder().setFile(configFile).build();
        ConfigurationNode rootNode;

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            logger.info("Configuration file '" + configFile.getPath() + "' not found. Creating default configuration");
            try {
                Sponge.getAssetManager().getAsset(this, configFileName).get().copyToFile(configFile.toPath());
            } catch (IOException e) {
                logger.error("Failed to load default configuration file!");
                e.printStackTrace();
            }
        }

        try {
            rootNode = configurationLoader.load();

            adminModePermissions = rootNode.getNode("permissions").getList(TypeToken.of(String.class));
            adminModeItems = new ArrayList<>();

            for (ConfigurationNode node : rootNode.getNode("items").getChildrenList()) {
                String itemID = node.getNode("id").getString();
                int itemQuantity = node.getNode("quantity").getInt();

                Optional<ItemType> itemTypeOptional = Sponge.getRegistry().getType(ItemType.class, itemID);
                if (itemTypeOptional.isPresent()) {
                    adminModeItems.add(ItemStack.builder().itemType(itemTypeOptional.get()).quantity(itemQuantity).build().createSnapshot());
                } else {
                    logger.error("Unknown item type '" + itemID + "'");
                }

                attributes = new HashMap<>();
                rootNode.getNode("attributes").getChildrenMap().forEach((key, value) -> {
                    attributes.put((String) key, value.getBoolean(true));
                });
            }
        } catch (IOException | ObjectMappingException e) {
            logger.error("An error has occurred while reading the configuration file: ");
            e.printStackTrace();
        }
    }

    private void loadPlayerData() {
        File playerDataFile = new File(configurationPath.toFile(), playerDataFileName);
        amplayerdataConfig = HoconConfigurationLoader.builder().setFile(playerDataFile).build();

        if (!playerDataFile.exists()) {
            playerDataFile.getParentFile().mkdirs();
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                logger.error("Failed to create '" + playerDataFile.getPath() + "': ");
                e.printStackTrace();
            }
        }

        try {
            amplayerdataNode = amplayerdataConfig.load();
        } catch (Exception e) {
            logger.error("An error has occurred while reading the amplayerdata.dat file: ");
            e.printStackTrace();
        }
    }

    private void registerCommands() {
        CommandSpec adminmode = CommandSpec.builder()
                .permission("adminmode.command.adminmode")
                .arguments(GenericArguments.optional(GenericArguments.remainingJoinedStrings(Text.of("reason"))))
                .executor((src, args) -> {
                    if (!(src instanceof Player)) {
                        src.sendMessage(Text.of(prefix, TextColors.RED, "Only a player may execute this command"));
                        return CommandResult.empty();
                    }

                    Player player = ((Player) src);
                    if (!isInAdminMode(player)) {
                        String reason = null;
                        Optional<String> reasonOptional = args.getOne("reason");
                        if (reasonOptional.isPresent()) {
                            reason = reasonOptional.get();
                        }

                        return enableAdminMode(player, reason);
                    } else {
                        return disableAdminMode(player);
                    }
                })
                .build();

        CommandSpec adminmodelist = CommandSpec.builder()
                .permission("adminmode.command.list")
                .executor((src, args) -> {
                    List<Text> text = new ArrayList<>(active.size());
                    for (AMPlayerData playerData : active.values()) {
                        text.add(Text.of(playerData.getName()));
                    }
                    PaginationList.builder()
                            .title(Text.of(TextColors.DARK_GREEN, "Players in Admin Mode"))
                            .contents(text)
                            .padding(Text.of(TextColors.DARK_GREEN, "="))
                            .build().sendTo(src);
                    return CommandResult.success();
                })
                .build();

        CommandSpec adminmodekick = CommandSpec.builder()
                .permission("adminmode.command.kick")
                .arguments(GenericArguments.player(Text.of("player")))
                .executor((src, args) -> {
                    Optional<Player> player = args.getOne("player");
                    if (player.isPresent()) {
                        if (isInAdminMode(player.get())) {
                            return disableAdminMode(player.get(), src.getName());
                        } else {
                            src.sendMessage(Text.of(prefix, TextColors.RED, "'", TextColors.GREEN, player.get().getName(), TextColors.RED, "' is currently not in admin mode"));
                            return CommandResult.empty();
                        }
                    }

                    src.sendMessage(Text.of(prefix, TextColors.RED, "Player not found"));
                    return CommandResult.empty();
                })
                .build();

        // Clear permissions command. Necessary due to unreliability of permission service in unsetting permissions on server stop
        CommandSpec clearPermissions = CommandSpec.builder()
                .permission("adminmode.command.clearperms")
                .executor((src, args) -> {
                    clearPermissions();
                    src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Clearing permissions"));
                    return CommandResult.success();
                })
                .build();

        CommandSpec reload = CommandSpec.builder()
                .permission("adminmode.command.reload")
                .executor((src, args) -> {
                    loadConfig();
                    src.sendMessage(Text.of(prefix, TextColors.YELLOW, "Reloaded configuration"));
                    return CommandResult.success();
                })
                .build();

        Sponge.getCommandManager().register(this, adminmode, "adminmode", "am");
        Sponge.getCommandManager().register(this, adminmodelist, "amlist");
        Sponge.getCommandManager().register(this, adminmodekick,  "amkick");
        Sponge.getCommandManager().register(this, clearPermissions, "amclearperms");
        Sponge.getCommandManager().register(this, reload, "amreload");
    }

    private CommandResult enableAdminMode(Player player, String reason) {
        // Create player data
        AMPlayerData playerData = new AMPlayerData(player);

        // Save player data to disk in case of crash
        serializePlayerData(playerData);

        // Add to active admin mode players
        active.put(player.getUniqueId(), playerData);

        // Clears player inventory and other data
        AMPlayerData.clear(player);

        // Gives infinite saturation
        player.offer(Keys.SATURATION, Double.MAX_VALUE);

        // Give items as specified in the config file
        for (ItemStackSnapshot itemSnapshot : adminModeItems) {
            player.getInventory().offer(itemSnapshot.createStack());
        }

        enableFlight(player);

        // Give contextual permissions if the player does not already have them
        Set<Context> contexts = new HashSet<>();
        contexts.add(AMContextCalculator.IN_ADMIN_MODE);
        for (String permission : adminModePermissions) {
            if (!player.hasPermission(contexts, permission)) {
                player.getSubjectData().setPermission(contexts, permission, Tristate.TRUE);
            }
        }

        // Broadcast going into admin mode message
        Text text = Text.of(prefix, TextColors.YELLOW, "'", TextColors.GREEN, player.getName(), TextColors.YELLOW, "' has entered admin mode!");
        if (reason != null) {
            text = Text.of(text, TextColors.GRAY, " Reason: ", reason);
        }
        broadcastToAdmins(text);

        // Tell player that they are in admin mode if they haven't received the global message
        if (!player.hasPermission("adminmode.notify")) {
            player.sendMessage(Text.of(prefix, TextColors.YELLOW, "You are now in admin mode"));
        }
        return CommandResult.success();
    }

    private CommandResult disableAdminMode(Player player) {
        return disableAdminMode(player, null);
    }

    private CommandResult disableAdminMode(Player player, String kicker) {
        // Get player data
        AMPlayerData playerData = active.get(player.getUniqueId());

        // Remove player data from disk
        removePlayerData(player.getUniqueId());

        // Restore player's inventory, food, and experience
        playerData.restore(player);

        // Remove player from admin mode list
        active.remove(player.getUniqueId());

        disableFlight(player);

        // Broadcast leaving admin mode
        if (kicker != null) {
            Text text = Text.of(prefix, TextColors.YELLOW, "'", TextColors.GREEN, player.getName(), TextColors.YELLOW, "' has been kicked out of admin mode by '", TextColors.AQUA, kicker, TextColors.YELLOW, "'!");
            broadcastToAdmins(text);
        } else {
            Text text = Text.of(prefix, TextColors.YELLOW, "'", TextColors.GREEN, player.getName(), TextColors.YELLOW, "' has left admin mode!");
            broadcastToAdmins(text);
        }

        // Tell player that they are no longer in admin mode if they haven't received the global message
        if (!player.hasPermission("adminmode.notify")) {
            player.sendMessage(Text.of(prefix, TextColors.YELLOW, "You are no longer in admin mode"));
        }
        return CommandResult.success();
    }

    private void enableFlight(Player player) {
        player.offer(Keys.CAN_FLY, true);
    }

    private void disableFlight(Player player) {
        player.offer(Keys.CAN_FLY, false);
        player.offer(Keys.IS_FLYING, false);
    }

    private void serializePlayerData(AMPlayerData playerData) {
        try {
            amplayerdataNode.getNode(playerData.getUniqueId().toString()).setValue(TypeToken.of(AMPlayerData.class), playerData);
            amplayerdataConfig.save(amplayerdataNode);
        } catch (IOException | ObjectMappingException e) {
            logger.error("An error has occurred while saving player data for '" + playerData.getUniqueId() + "': ");
            e.printStackTrace();
        }
    }

    private AMPlayerData deserializePlayerData(UUID uuid) {
        try {
            AMPlayerData amPlayerData = amplayerdataNode.getNode(uuid.toString()).getValue(TypeToken.of(AMPlayerData.class));
            removePlayerData(uuid);
            return amPlayerData;
        } catch (ObjectMappingException | NullPointerException e) {
            logger.error("An error has occurred while deserializing player data for '" + uuid.toString() + "': ");
            e.printStackTrace();
        }
        return null;
    }

    private void removePlayerData(UUID uuid) {
        try {
            amplayerdataNode.removeChild(uuid.toString());
            amplayerdataConfig.save(amplayerdataNode);
        } catch (IOException e) {
            logger.error("An error has occurred while removing player data for '" + uuid.toString() + "': ");
            e.printStackTrace();
        }
    }

    private void serializeLeftViaDisconnect() {
        try {
//            amplayerdataNode.getNode("leftViaDisconnect").setValue(new TypeToken<Set<UUID>>() {}, leftViaDisconnect);
            amplayerdataNode.getNode("leftViaDisconnect").setValue(new TypeToken<List<UUID>>() {}, leftViaDisconnect);
            amplayerdataConfig.save(amplayerdataNode);
        } catch (IOException | ObjectMappingException e) {
            logger.error("An error has occurred while writing to amplayerdata.dat file: ");
            e.printStackTrace();
        }
    }

    private void deserializeLeftViaDisconnect() {
        try {
//            Set<UUID> uuidList = amplayerdataNode.getNode("leftViaDisconnect").getValue(new TypeToken<Set<UUID>>() {});
//            if (uuidList != null) {
//                leftViaDisconnect = uuidList;
//            }
            List<UUID> uuidList = amplayerdataNode.getNode("leftViaDisconnect").getValue(new TypeToken<List<UUID>>() {});
            if (uuidList != null) {
                leftViaDisconnect = uuidList;
            }
            amplayerdataNode.removeChild(amplayerdataNode.getNode("leftViaDisconnect"));
            amplayerdataConfig.save(amplayerdataNode);
        } catch (IOException | ObjectMappingException e) {
            logger.error("An error has occurred while writing to amplayerdata.dat file: ");
            e.printStackTrace();
        }
    }

    private void broadcastToAdmins(Text message) {
        for (MessageReceiver messageReceiver : Sponge.getServer().getBroadcastChannel().getMembers()) {
            if (messageReceiver instanceof Player) {
                if (((Player) messageReceiver).hasPermission("adminmode.notify")) {
                    messageReceiver.sendMessage(message);
                }
            } else {
                messageReceiver.sendMessage(message);
            }
        }
    }

    boolean isInAdminMode(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    private void clearPermissions() {
        // Clear contextual permissions
        final Set<Context> contexts = new HashSet<>();
        contexts.add(AMContextCalculator.IN_ADMIN_MODE);
        final Optional<PermissionService> permissionServiceOptional = getPermissionService();
        if (permissionServiceOptional.isPresent()) {
            SubjectCollection userSubjects = permissionServiceOptional.get().getUserSubjects();
            CompletableFuture<Set<String>> completableFuture = userSubjects.getAllIdentifiers();
            // Wait for the future to complete. Then for each identifier, get the identifier's Subject, then get the Subject's SubjectData, then clear the permissions of the permission contexts
            completableFuture.thenAcceptAsync(identifiers -> identifiers.forEach(identifier -> userSubjects.getSubject(identifier).ifPresent(subject -> subject.getSubjectData().clearPermissions(contexts))));
        }
    }

    boolean hasAttribute(String attribute) {
        try {
            return attributes.get(attribute);
        } catch (NullPointerException e) {
            return true;
        }
    }

    Logger getLogger() {
        return logger;
    }

    @Listener
    public void onPlayerConnect(ClientConnectionEvent.Join event, @First Player player) {
        // Restore player data upon reconnect (in the case of a crash, the player's data should be here)
        try {
            final ConfigurationNode node = amplayerdataNode.getNode(player.getUniqueId().toString());
            if (!node.isVirtual()) {
                AMPlayerData playerData = deserializePlayerData(player.getUniqueId());
                playerData.restore(player);
                active.remove(player.getUniqueId());
                disableFlight(player);
            }
        } catch (Exception e) {
            logger.error("An error has occurred while restoring player data for player '" + player.getName() + "', UUID of '" + player.getUniqueId().toString() + "': ");
            e.printStackTrace();
            logger.error("The player's data should still be available in /config/adminmode/amplayerdata.dat in a mostly human-readable format. You can use the information in there to restore the player's items, and other data manually (e.g. using '/give')");
            logger.error("After manually recovering the player's items and data from amplayerdata.dat (or saving it somewhere else for later), the player should re-run admin mode. This will overwrite their corrupted data in amplayerdata.dat so that this error should no longer occur");

            player.sendMessage(Text.of(prefix, TextColors.RED, "An error has occurred while restoring your player data. If you have lost items or any other player-related data, contact a server operator to have your data restored"));
            player.sendMessage(Text.of(prefix, TextColors.RED, TextStyles.BOLD, "If you need data to be recovered, DO NOT enter admin mode for the time being. ", TextStyles.RESET, "Doing so will result in your old player data being overwritten and unrecoverable"));
            player.sendMessage(Text.of(prefix, TextColors.RED, "If you have no (more) data that has to be recovered, then you can safely enter admin mode so that this message will not be displayed again"));

            active.remove(player.getUniqueId());
            disableFlight(player);
        }

        // Necessary since disabling flight immediately before a player disconnection / server restart may not always finish
        if (leftViaDisconnect.contains(player.getUniqueId())) {
            leftViaDisconnect.remove(player.getUniqueId());
            disableFlight(player);
        }
    }

    @Listener
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event, @First Player player) {
        if (isInAdminMode(player)) {
            leftViaDisconnect.add(player.getUniqueId());
            disableAdminMode(player, "<Disconnect>");
        }
    }

    @Listener
    public void onReload(GameReloadEvent event) {
        loadConfig();
        logger.info("Reloaded configuration");
    }

    @Listener
    public void onGameStoppingServerEvent(GameStoppingServerEvent event) {
        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            if (isInAdminMode(player)) {
                leftViaDisconnect.add(player.getUniqueId());
                disableAdminMode(player, "<Server Stopping>");
            }
        }
        serializeLeftViaDisconnect();
    }
}
