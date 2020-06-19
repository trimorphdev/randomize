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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.swing.plaf.multi.MultiSeparatorUI;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.stream.IntStream;

class dropRandomItem implements Runnable {
    private int Max;
    private int Seed;
    private World Wld;
    private Location Loc;
    private Plugin plugin;
    public dropRandomItem(int max, int s, World wld, Location loc, Plugin plug) {
        Max = max;
        Seed = s;
        Wld = wld;
        Loc = loc;
        plugin = plug;
    }
    public void run() {
        // Get a list of all materials.
        final List<Material> values = Collections.unmodifiableList(Arrays.asList(Material.values()));
        ItemStack dropsItem = null;
        // Get an ItemStack with a random material from `values` and then drop it at the provided location. (`loc`)
        try {
            dropsItem = new ItemStack(values.get(getRandom(values.size(),Seed)), getRandom(Max,Seed)+1); // The item it drops.
        } catch (IOException e) {
            e.printStackTrace();
        }
        ItemStack finalDropsItem = dropsItem;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Wld.dropItemNaturally(Loc, finalDropsItem);
        });

    }
    private int getRandom(int max, int s) throws IOException {
        int ret = 0;
        if(Main.useConfig) {
            System.out.println("getrandom");
            String temp = "https://www.random.org/integers/?num=1&min=0&max="+max+"&col=1&base=10&format=plain&rnd=new";
            ret= request(temp);
        }
        else {
            System.out.println("notrandom");

            Random random = new Random(s);
            ret = random.nextInt(max);
        }
        return ret;
    }
    private static int request(String URL) throws IOException {
        java.net.URL url = new URL(URL);
        URLConnection urlConn = url.openConnection();
        urlConn.addRequestProperty("User-Agent", "Mozilla");

        InputStream inStream = urlConn.getInputStream();
        int recieved = new Scanner(new InputStreamReader(inStream)).nextInt();
        inStream.close();
        return (recieved);
    }

}
    public class Main extends JavaPlugin implements Listener {
    private int startTime;
    private Configuration conf;
    int maxDrops;
    int seed;
    public static boolean useConfig;
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
        useConfig = conf.getBoolean("useRandomOrg");

    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) throws IOException {
        // Cancel the event so the block doesn't drop anything.
        event.setCancelled(true);

        // Get the block and it's type BEFORE it is set to air so blkType won't always be air.
        Block blk = event.getBlock();
        int blkType = blk.getType().getId();

        blk.setType(Material.AIR);

        // Drop the item at the block's (previous) position.
        Runnable r = new dropRandomItem(maxDrops,blkType + seed + blk.getState().getType().getId(), blk.getWorld(), blk.getLocation(),this);
        new Thread(r).start();
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
    public void onVehicleDestroy(VehicleDestroyEvent event) throws IOException {
        // Cancel the event, we'll remove the vehicle later.
        event.setCancelled(true);

        // Get the vehicle.
        Vehicle v = event.getVehicle();

        // Drop the items where the vehicle is and then delete the vehicle.
        Runnable r = new dropRandomItem(maxDrops,v.getType().getTypeId() + seed, v.getWorld(), v.getLocation(),this);
        new Thread(r).start();

        v.remove();
    }
}
