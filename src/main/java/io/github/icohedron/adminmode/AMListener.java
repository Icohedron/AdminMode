package io.github.icohedron.adminmode;

import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

public class AMListener {

    private AdminMode adminMode;

    AMListener() {
        adminMode = AdminMode.getInstance();
    }

    @Listener
    public void onDamageEntity(DamageEntityEvent event) {
        if (!adminMode.hasAttribute("damage_other_entities")) {
            Optional<EntityDamageSource> entityDamageSource = event.getCause().first(EntityDamageSource.class);
            if (entityDamageSource.isPresent()) {
                Entity source = entityDamageSource.get().getSource();
                if (source instanceof Player) {
                    if (adminMode.isInAdminMode((Player) source)) {
                        event.setCancelled(true);
                    }
                }
            }
        }

        if (event.getTargetEntity() instanceof Player) {
            Player player = (Player) event.getTargetEntity();
            if (adminMode.isInAdminMode(player)) {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onItemDrop(DropItemEvent.Pre event, @First Player player) {
        if (!adminMode.hasAttribute("drop_items")) {
            if (adminMode.isInAdminMode(player)) {
                for (ItemStackSnapshot itemStackSnapshot : event.getDroppedItems()) {
                    player.getInventory().offer(itemStackSnapshot.createStack());
                }
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onItemPickup(ChangeInventoryEvent.Pickup event, @First Player player) {
        if (!adminMode.hasAttribute("pickup_items")) {
            if (adminMode.isInAdminMode(player)) {
                event.setCancelled(true);
            }
        }
    }

//    @Listener
//    public void onOpenInventory(InteractInventoryEvent.Open event, @First Player player) {
//        if (!adminMode.hasAttribute("open_inventories")) {
//            if (adminMode.isInAdminMode(player)) {
//                event.setCancelled(true);
//            }
//        }
//    }

    @Listener
    public void onClickInventory(ClickInventoryEvent event, @First Player player) {
        if (!adminMode.hasAttribute("interact_inventories")) {
            if (adminMode.isInAdminMode(player)) {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onBreakBlock(ChangeBlockEvent.Break event, @First Player player) {
        if (!adminMode.hasAttribute("break_blocks")) {
            if (adminMode.isInAdminMode(player)) {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onPlaceBlock(ChangeBlockEvent.Place event, @First Player player) {
        if (!adminMode.hasAttribute("place_blocks")) {
            if (adminMode.isInAdminMode(player)) {
                event.setCancelled(true);
            }
        }
    }

    @Listener
    public void onInteractBlock(InteractBlockEvent.Secondary event, @First Player player) {
        Optional<Location<World>> location = event.getTargetBlock().getLocation();
        if (location.isPresent()) {
            Optional<TileEntity> tileEntity = location.get().getTileEntity();
            if (tileEntity.isPresent() && tileEntity.get() instanceof TileEntityCarrier) {
                if (!adminMode.hasAttribute("interact_tile_entity_carriers")) {
                    if (adminMode.isInAdminMode(player)) {
                        event.setCancelled(true);
                    }
                }
            } else {
                if (!adminMode.hasAttribute("interact_blocks")) {
                    if (adminMode.isInAdminMode(player)) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @Listener
    public void onInteractEntity(InteractEntityEvent.Secondary event, @First Player player) {
        if (!adminMode.hasAttribute("interact_entities")) {
            if (adminMode.isInAdminMode(player)) {
                event.setCancelled(true);
            }
        }
    }
}
