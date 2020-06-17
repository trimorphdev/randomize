package com.trimorph.randomize;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class Main extends JavaPlugin implements Listener {
    private int startTime;
    private Configuration conf;
    int maxDrops;
    int seed;

    public void onEnable() {
        // Register events.
        getServer().getPluginManager().registerEvents(this, this);

        // Write all of the undefined values from config.yml to ./plugins/Randomize/config.yml
        this.saveDefaultConfig();

        // Get the config data.
        conf = this.getConfig();

        // Set the config data to a variable we can use later.
        maxDrops = conf.getInt("maxDrops");
        seed = conf.getInt("seed");
    }

    public void dropRandomItem(int s, World wld, Location loc) {
        // Create a seeded randomizer.
        Random randomizer = new Random(s);

        // Get a list of all materials.
        final List<Material> values = Collections.unmodifiableList(Arrays.asList(Material.values()));

        // Get an ItemStack with a random material from `values` and then drop it at the provided location. (`loc`)
        ItemStack dropsItem = new ItemStack(values.get(randomizer.nextInt(values.size())), new Random().nextInt(maxDrops) + 1); // The item it drops.
        wld.dropItemNaturally(loc, dropsItem);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Cancel the event so the block doesn't drop anything.
        event.setCancelled(true);

        // Get the block and it's type BEFORE it is set to air so blkType won't always be air.
        Block blk = event.getBlock();
        int blkType = blk.getType().getId();

        blk.setType(Material.AIR);

        // Drop the item at the block's (previous) position.
        dropRandomItem(blkType + seed + blk.getState().getType().getId(), blk.getWorld(), blk.getLocation());
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        // Get the entity.
        Entity ent = event.getEntity();

        // Check if an entity is a creature and not, for example, an arrow.
        if (ent instanceof Creature) {
            // Clear all drops so we can change them later.
            event.getDrops().clear();

            // Create the seeded randomizer.
            Random randomizer = new Random(ent.getType().getTypeId() + seed);

            // Select a random Material and create an ItemStack of said Material.
            final List<Material> values = Collections.unmodifiableList(Arrays.asList(Material.values()));
            ItemStack dropsItem = new ItemStack(values.get(randomizer.nextInt(values.size())), new Random().nextInt(maxDrops) + 1); // The item it drops.

            // Drop our items instead.
            event.getDrops().add(dropsItem);
        }
    }

    @EventHandler
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        // Cancel the event, we'll remove the vehicle later.
        event.setCancelled(true);

        // Get the vehicle.
        Vehicle v = event.getVehicle();

        // Drop the items where the vehicle is and then delete the vehicle.
        dropRandomItem(v.getType().getTypeId() + seed, v.getWorld(), v.getLocation());
        v.remove();
    }
}
