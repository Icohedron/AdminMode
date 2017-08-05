package io.github.icohedron.adminmode;

import com.flowpowered.math.vector.Vector2i;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.data.manipulator.mutable.PotionEffectData;
import org.spongepowered.api.data.manipulator.mutable.entity.FoodData;
import org.spongepowered.api.data.manipulator.mutable.entity.IgniteableData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentInventory;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.item.inventory.type.GridInventory;
import org.spongepowered.api.world.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ConfigSerializable
class AMPlayerData {

    /* The inventory API is a mess. I bet that this will need updating in future Sponge API versions */

    @Setting private String name;

    @Setting private UUID uuid;
    @Setting private Location location;

    // Experience data will not be cached, due to bugs with how Sponge handles experience data
//    @Setting private ExperienceHolderData experienceData;
    @Setting private PotionEffectData potionEffectData;
    @Setting private IgniteableData igniteableData;

    // For some reason Sponge doesn't like deserializing FoodData (tries to cast float to double)
//    @Setting private FoodData foodData;
    // So I will store each of its values on their own
    @Setting private double exhaustion;
    @Setting private double saturation;
    @Setting private int foodLevel;

    @Setting private Map<Integer, ItemStackSnapshot> equipment;
    @Setting private Map<Integer, ItemStackSnapshot> hotbar;
    @Setting private Map<Integer, ItemStackSnapshot> enderChest;
    @Setting private Map<Integer, ItemStackSnapshot> main;
    @Setting private ItemStackSnapshot offhand;

    AMPlayerData() {}

    AMPlayerData(Player player) {
        this.name = player.getName();
        this.uuid = player.getUniqueId();
        this.location = player.getLocation();

//        this.experienceData = player.get(ExperienceHolderData.class).get();

        this.potionEffectData = null;
        Optional<PotionEffectData> potionEffectData = player.get(PotionEffectData.class);
        if (potionEffectData.isPresent()) {
            this.potionEffectData = potionEffectData.get();
        }

        this.igniteableData = null;
        Optional<IgniteableData> igniteableData = player.get(IgniteableData.class);
        if (igniteableData.isPresent()) {
            this.igniteableData = igniteableData.get();
        }

//        this.foodData = player.getFoodData().copy();

        FoodData foodData = player.getFoodData().copy();
        this.exhaustion = foodData.exhaustion().get();
        this.saturation = foodData.saturation().get();
        this.foodLevel = foodData.foodLevel().get();

        this.equipment = new HashMap<>();
        this.hotbar = new HashMap<>();
        this.enderChest = new HashMap<>();
        this.main = new HashMap<>();
        this.offhand = null;

        PlayerInventory inventory = (PlayerInventory) player.getInventory();

        for (Inventory slot : inventory.getEquipment().slots()) {
            if (slot.peek().isPresent()) {
                SlotIndex slotIndex = slot.getProperty(SlotIndex.class, "slotindex").get();
                equipment.put(slotIndex.getValue(), slot.peek().get().createSnapshot());
            }
        }

        for (Inventory slot : inventory.getHotbar().slots()) {
            if (slot.peek().isPresent()) {
                SlotIndex slotIndex = slot.getProperty(SlotIndex.class, "slotindex").get();
                hotbar.put(slotIndex.getValue(), slot.peek().get().createSnapshot());
            }
        }

        // Notes: Vector2i serialization works, but deserialization doesn't work apparently.
        // So I'll just store the vector as an integer. We know the grid inventory is 3*9, which are both single digits.
        // So the format will by xy. (e.g. x=2,y=4 becomes 24)

        GridInventory ecInv = player.getEnderChestInventory().query(GridInventory.class);
        Vector2i ecDim = ecInv.getDimensions();
        for (int y = 0; y < ecDim.getY(); y++) {
            for (int x = 0; x < ecDim.getX(); x++) {
                if (ecInv.peek(x, y).isPresent()) {
                    enderChest.put(10 * x + y, ecInv.peek(x, y).get().createSnapshot());
                }
            }
        }

        GridInventory mainInv = inventory.getMain();
        Vector2i mainDim = mainInv.getDimensions();
        for (int y = 0; y < mainDim.getY(); y++) {
            for (int x = 0; x < mainDim.getX(); x++) {
                if (mainInv.peek(x, y).isPresent()) {
                    main.put(10 * x + y, mainInv.peek(x, y).get().createSnapshot());
                }
            }
        }

        if (player.getItemInHand(HandTypes.OFF_HAND).isPresent()) {
            offhand = player.getItemInHand(HandTypes.OFF_HAND).get().createSnapshot();
        }
    }

    String getName() {
        return name;
    }

    static void clear(Player player) {
        player.getInventory().clear();
        player.getEnderChestInventory().clear();
        player.setItemInHand(HandTypes.OFF_HAND, null);

//        ExperienceHolderData minExp = player.get(ExperienceHolderData.class).get();
//        minExp.set(minExp.experienceSinceLevel().set(minExp.experienceSinceLevel().getMinValue()));
//        minExp.set(minExp.level().set(minExp.level().getMinValue()));
//        minExp.set(minExp.totalExperience().set(minExp.totalExperience().getMinValue()));
//        player.offer(minExp);

        Optional<PotionEffectData> effects = player.get(PotionEffectData.class);
        if (effects.isPresent()) {
            PotionEffectData noEffects = effects.get();
            noEffects.set(noEffects.effects().removeAll(noEffects.effects()));
            player.offer(noEffects);
        }

        Optional<IgniteableData> igniteable = player.get(IgniteableData.class);
        if (igniteable.isPresent()) {
            IgniteableData noFire = igniteable.get();
            noFire.set(noFire.fireDelay().set(noFire.fireDelay().getMaxValue()));
            noFire.set(noFire.fireTicks().set(noFire.fireTicks().getMinValue()));
            player.offer(noFire);
        }

        FoodData maxFood = player.getFoodData();
        maxFood.set(maxFood.exhaustion().set(maxFood.exhaustion().getDefault()));
        maxFood.set(maxFood.saturation().set(maxFood.saturation().getDefault()));
        maxFood.set(maxFood.foodLevel().set(maxFood.foodLevel().getDefault()));
        player.offer(maxFood);
    }

    void restore(Player player) {
        clear(player);

        player.setLocation(location);
//        player.offer(experienceData);

        if (potionEffectData != null) {
            player.offer(potionEffectData);
        }

        if (igniteableData != null) {
            player.offer(igniteableData);
        }

        restoreFoodData(player);
        restoreInventory(player);
    }

    private void restoreInventory(Player player) {
        player.getInventory().clear();
        player.getEnderChestInventory().clear();

        PlayerInventory playerInventory = (PlayerInventory) player.getInventory();
        playerInventory.clear();

        EquipmentInventory equipment = playerInventory.getEquipment();
        Hotbar hotbar = playerInventory.getHotbar();
        GridInventory enderChest = player.getEnderChestInventory().query(GridInventory.class);
        GridInventory main = playerInventory.getMain();

        if (this.equipment != null) {
            for (int slotIndex : this.equipment.keySet()) {
                equipment.query(new SlotIndex(slotIndex)).set(this.equipment.get(slotIndex).createStack());
            }
        }

        if (this.hotbar != null) {
            for (int slotIndex : this.hotbar.keySet()) {
                hotbar.query(new SlotIndex(slotIndex)).set(this.hotbar.get(slotIndex).createStack());
            }
        }

        if (this.enderChest != null) {
            for (int pos : this.enderChest.keySet()) {
                int x = pos / 10;
                int y = pos % 10;
                enderChest.set(x, y, this.enderChest.get(pos).createStack());
            }
        }

        if (this.main != null) {
            for (int pos : this.main.keySet()) {
                int x = pos / 10;
                int y = pos % 10;
                main.set(x, y, this.main.get(pos).createStack());
            }
        }

        if (this.offhand != null) {
            player.setItemInHand(HandTypes.OFF_HAND, offhand.createStack());
        }
    }

    private void restoreFoodData(Player player) {
        FoodData foodData = player.getFoodData();
        foodData.set(foodData.exhaustion().set(this.exhaustion));
        foodData.set(foodData.saturation().set(this.saturation));
        foodData.set(foodData.foodLevel().set(this.foodLevel));
        player.offer(foodData);
    }

    UUID getUniqueId() {
        return uuid;
    }
}
