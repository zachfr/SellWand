package me.zachary.sellwand.listeners;

import de.tr7zw.changeme.nbtapi.NBTItem;
import me.zachary.sellwand.Sellwand;
import me.zachary.zachcore.utils.MessageUtils;
import me.zachary.zachcore.utils.ReflectionUtils;
import me.zachary.zachcore.utils.StorageUtils;
import me.zachary.zachcore.utils.WorldGuardUtils;
import me.zachary.zachcore.utils.hooks.EconomyManager;
import me.zachary.zachcore.utils.hooks.HologramManager;
import me.zachary.zachcore.utils.hooks.ShopManager;
import nl.rutgerkok.blocklocker.BlockLockerAPIv2;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LeftClickListener implements Listener {
    private Sellwand plugin;

    public LeftClickListener(Sellwand plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent event){
        if(event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;
        if(!ReflectionUtils.getVersion().contains("1_8"))
            if(event.getHand() == EquipmentSlot.OFF_HAND)
                return;
        Player player = event.getPlayer();
        NBTItem item = null;
        double amount = 0D;
        int itemAmount = 0;
        if(event.getItem() != null)
            item = new NBTItem(event.getItem());
        if(item != null && item.getBoolean("Is a sell wand")){
            if(plugin.getConfig().getBoolean("Use WorldGuard protection") &&
                    Bukkit.getPluginManager().getPlugin("WorldGuard") != null &&
                    !WorldGuardUtils.canAccessChest(player))
                return;
            if(Bukkit.getPluginManager().getPlugin("BlockLocker") != null && BlockLockerAPIv2.isProtected(event.getClickedBlock()) && !BlockLockerAPIv2.isOwner(player, event.getClickedBlock()))
                return;
            Inventory contents = StorageUtils.getStorageContents(event.getClickedBlock());
            if(contents == null || contents.getContents() == null)
                return;
            if(!player.hasPermission("sellwand.hologram")){
                MessageUtils.sendMessage(player, plugin.getMessage().getString("No permission"));
                return;
            }
            event.setCancelled(true);
            double multiplier = item.getDouble("Multiplier");
            for (int i = 0; i < contents.getContents().length; i++) {
                ItemStack chestItem = contents.getItem(i);
                Double price = 0D;
                if (chestItem != null)
                    price = ShopManager.getSellPrice(player, chestItem, chestItem.getAmount());
                if (price != -1.0 && chestItem != null) {
                    itemAmount += chestItem.getAmount();
                    amount += price;
                }
            }
            amount = amount * multiplier;
            Location hologramLoc = null;
            if(!ReflectionUtils.getVersion().contains("1_8"))
                hologramLoc = event.getClickedBlock().getLocation().add(getDifferenceX(player), -0.80, getDifferenceZ(player));
            else
                hologramLoc = event.getClickedBlock().getLocation();
            hologramLoc.setDirection(player.getLocation().getDirection());
            HologramManager.removeHologram(hologramLoc);
            HologramManager.createHologram(hologramLoc, getHologramLine(itemAmount, amount));
            Location finalHologramLoc = hologramLoc;
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    HologramManager.removeHologram(finalHologramLoc);
                }
            }, 150L);
        }
    }

    public List<String> getHologramLine(int amount, double price){
        List<String> line = new ArrayList<>();
        for (String i : plugin.getConfig().getStringList("Hologram line")) {
            line.add(i.replace("%amount%", String.valueOf(amount))
                    .replace("%price%", EconomyManager.formatEconomy(price)));
        }
        return line;
    }

    /*private Location getHologramLocation(Player player, Block block){

    }*/

    private double getDifferenceX(Player player){
        double direction = 0;
        switch (player.getFacing().toString()){
            case "WEST":
                direction = 1;
                break;
            case "EAST":
                direction = -1;
                break;
            case "SOUTH":
            case "NORTH":
                direction = 0;
                break;
            default:
                break;
        }
        return direction;
    }

    private double getDifferenceZ(Player player){
        double direction = 0;
        switch (player.getFacing().toString()){
            case "WEST":
            case "EAST":
                direction = 0;
                break;
            case "SOUTH":
                direction = -1;
                break;
            case "NORTH":
                direction = 1;
                break;
            default:
                break;
        }
        return direction;
    }
}
