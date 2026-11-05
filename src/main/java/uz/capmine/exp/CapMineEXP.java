package uz.capmine.exp;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class CapMineEXP extends JavaPlugin implements CommandExecutor, Listener {

    @Override
    public void onEnable() {
        getLogger().info("CapMine Premium EXP tizimi yoqildi!");
        saveDefaultConfig();
        
        // /capexp buyrug'ini ro'yxatdan o'tkazish
        this.getCommand("capexp").setExecutor(this);
        // Menyudagi bosishlarni (Click) ushlash uchun
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("CapMine EXP tizimi o'chirildi.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Bu buyruqni faqat o'yinchilar ishlata oladi.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("capexp")) {
            // 27 ta slotli menyu yaratish
            Inventory menu = Bukkit.createInventory(null, 27, "§cCapMine §fExᴘ §8• §eSʏsᴛᴇᴍ");

            // 1. Shisha panellarni (Border) to'ldirish
            ItemStack glass = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
            ItemMeta glassMeta = glass.getItemMeta();
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);

            for (int i = 0; i < 27; i++) {
                menu.setItem(i, glass);
            }

            // 2. EXP Idishini qo'shish (Markazga)
            ItemStack expBottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
            ItemMeta expMeta = expBottle.getItemMeta();
            expMeta.setDisplayName("§e10 Level EXP");
            expMeta.setLore(Arrays.asList("§7CapMine Premium EXP", "", "§fBosish uchun bosing"));
            expBottle.setItemMeta(expMeta);
            menu.setItem(13, expBottle); // 13 - markaziy slot

            // 3. Yopish tugmasini qo'shish
            ItemStack closeBtn = new ItemStack(Material.BARRIER);
            ItemMeta closeMeta = closeBtn.getItemMeta();
            closeMeta.setDisplayName("§cYopish");
            closeBtn.setItemMeta(closeMeta);
            menu.setItem(22, closeBtn);

            // Menyuni ochish va ovoz chiqarish
            player.openInventory(menu);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            return true;
        }
        return false;
    }

    // Menyudagi tugmalar bosilganda ishlaydigan qism
    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        // Faqat bizning menyu ochilganda ishlaydi
        if (event.getView().getTitle().equals("§cCapMine §fExᴘ §8• §eSʏsᴛᴇᴍ")) {
            event.setCancelled(true); // O'yinchi itemlarni o'g'irlay olmaydi

            if (event.getCurrentItem() == null) return;

            // Agar Yopish tugmasi bosilsa
            if (event.getCurrentItem().getType() == Material.BARRIER) {
                Player player = (Player) event.getWhoClicked();
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
            }
            
            // Agar EXP idishi bosilsa (Hozircha shunchaki ovoz chiqaradi)
            if (event.getCurrentItem().getType() == Material.EXPERIENCE_BOTTLE) {
                Player player = (Player) event.getWhoClicked();
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.sendMessage("§a[CapMine] §fHozircha test rejimida! Tez orada EXP saqlanadi.");
            }
        }
    }
                }
