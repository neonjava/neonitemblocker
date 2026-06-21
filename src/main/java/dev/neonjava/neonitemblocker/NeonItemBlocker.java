package dev.neonjava.neonitemblocker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

public class NeonItemBlocker extends JavaPlugin implements Listener, CommandExecutor {
    private final Map<String, Set<Material>> blockedMap = new HashMap<>();
    private String warningMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("neonitemblocker")).setExecutor(this);

        getLogger().info("NeonItemBlocker enabled successfully!");
    }

    public void loadConfigValues() {
        reloadConfig();
        blockedMap.clear();
        warningMessage = getConfig().getString("warning-message", "<red><bold>You cannot use this item in this world!</bold></red>");

        ConfigurationSection section = getConfig().getConfigurationSection("blocked-items");
        if (section != null) {
            for (String worldName : section.getKeys(false)) {
                List<String> materials = section.getStringList(worldName);
                Set<Material> blockedSet = new HashSet<>();
                for (String matName : materials) {
                    try {
                        Material mat = Material.valueOf(matName.toUpperCase().trim());
                        blockedSet.add(mat);
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Invalid Material '" + matName + "' specified for world '" + worldName + "' in config!");
                    }
                }
                if (!blockedSet.isEmpty()) {
                    blockedMap.put(worldName.toLowerCase(), blockedSet);
                }
            }
        }
    }

    private boolean isBlocked(World world, Material material) {
        if (world == null || material == null || material == Material.AIR) return false;
        Set<Material> blocked = blockedMap.get(world.getName().toLowerCase());
        return blocked != null && blocked.contains(material);
    }

    private void warnPlayer(Player player) {
        Component message = MiniMessage.miniMessage().deserialize(warningMessage);
        player.sendActionBar(message);
    }

    // --- Command Handling ---
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("neonitemblocker.admin")) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You do not have permission.</red>"));
                return true;
            }
            loadConfigValues();
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>NeonItemBlocker config reloaded!</green>"));
            return true;
        }
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Usage: /nib reload</yellow>"));
        return true;
    }

    // --- Event Listeners ---

    // 1. Right/Left Click usage check
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        // Check item in main hand
        ItemStack mainHand = event.getItem();
        if (mainHand != null && isBlocked(world, mainHand.getType())) {
            event.setCancelled(true);
            warnPlayer(player);
            return;
        }

        // Check off-hand just in case
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand.getType() != Material.AIR && isBlocked(world, offHand.getType())) {
            event.setCancelled(true);
            warnPlayer(player);
        }
    }

    // 2. Block place check
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        Material placed = event.getBlockPlaced().getType();

        if (isBlocked(world, placed)) {
            event.setCancelled(true);
            warnPlayer(player);
        }
    }

    // 3. Attacking using blocked items
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            World world = player.getWorld();
            Material weapon = player.getInventory().getItemInMainHand().getType();

            if (isBlocked(world, weapon)) {
                event.setCancelled(true);
                warnPlayer(player);
            }
        }
    }

    // 4. Interacting with entities (e.g. shears, spawn eggs)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        
        ItemStack item = player.getInventory().getItemInHand();
        if (item.getType() != Material.AIR && isBlocked(world, item.getType())) {
            event.setCancelled(true);
            warnPlayer(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        
        ItemStack item = player.getInventory().getItemInHand();
        if (item.getType() != Material.AIR && isBlocked(world, item.getType())) {
            event.setCancelled(true);
            warnPlayer(player);
        }
    }

    // 5. Consuming items (e.g. potions, food)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        Material material = event.getItem().getType();

        if (isBlocked(world, material)) {
            event.setCancelled(true);
            warnPlayer(player);
        }
    }
}
