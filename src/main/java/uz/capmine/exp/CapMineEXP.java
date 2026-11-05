package uz.capmine.exp;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CapMineEXP extends JavaPlugin implements CommandExecutor, Listener {

    private NamespacedKey expKey;

    @Override
    public void onEnable() {
        getLogger().info("CapMine Premium EXP tizimi yoqildi!");
        saveDefaultConfig();
        
        this.expKey = new NamespacedKey(this, "capmine_exp_amount");
        
        this.getCommand("capexp").setExecutor(this);
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("capexp")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("capmine.exp.admin")) {
                    reloadConfig();
                    sender.sendMessage(color("&a[CapMine] &fConfig muvaffaqiyatli yangilandi!"));
                    if (sender instanceof Player) {
                        playSound((Player) sender, "click");
                    }
                } else {
                    sender.sendMessage(color("&c[CapMine] Sizda ruxsat yo'q!"));
                }
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage("Faqat o'yinchilar uchun!");
                return true;
            }

            openMenu((Player) sender);
            return true;
        }
        return false;
    }

    private void openMenu(Player player) {
        int size = getConfig().getInt("menu.size", 45);
        String title = color(getConfig().getString("menu.title", "&cCapMine EXP"));
        String borderMatName = getConfig().getString("menu.border-material", "LIME_STAINED_GLASS_PANE");
        
        Material borderMat;
        try {
            borderMat = Material.valueOf(borderMatName);
        } catch (Exception e) {
            borderMat = Material.LIME_STAINED_GLASS_PANE;
        }

        Inventory menu = Bukkit.createInventory(null, size, title);

        // Chegaralarni to'ldirish
        ItemStack border = new ItemStack(borderMat);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(" ");
            border.setItemMeta(borderMeta);
        }

        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9) {
                menu.setItem(i, border);
            }
        }

        // Level qo'shish
        if (getConfig().getConfigurationSection("levels") != null) {
            for (String key : getConfig().getConfigurationSection("levels").getKeys(false)) {
                int exp = getConfig().getInt("levels." + key + ".exp");
                int slot = getConfig().getInt("levels." + key + ".slot");
                String matName = getConfig().getString("levels." + key + ".material", "EXPERIENCE_BOTTLE");
                String name = color(getConfig().getString("levels." + key + ".name", ""));

                Material mat;
                try {
                    mat = Material.valueOf(matName);
                } catch (Exception e) {
                    mat = Material.EXPERIENCE_BOTTLE;
                }

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                
                if (meta != null) {
                    meta.setDisplayName(name);

                    List<String> lore = new ArrayList<>();
                    lore.add(color("&8CapMine Premium EXP"));
                    lore.add(" ");
                    lore.add(color("&fTalab qilinadi: &e" + exp + " level"));
                    lore.add(color("&fSizda: &a" + player.getLevel() + " level"));
                    lore.add(" ");
                    lore.add(color("&e● &fSaqlash uchun bosing!"));

                    meta.setLore(lore);
                    meta.addEnchant(Enchantment.DURABILITY, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    meta.getPersistentDataContainer().set(expKey, PersistentDataType.INTEGER, exp);

                    item.setItemMeta(meta);
                    menu.setItem(slot, item);
                }
            }
        }

        // Yopish
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(color("&cYopish"));
            close.setItemMeta(closeMeta);
        }
        menu.setItem(size - 5, close);

        player.openInventory(menu);
        playSound(player, "menu-open");
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(color(getConfig().getString("menu.title", "&cCapMine EXP")))) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();

            if (clicked.getType() == Material.BARRIER) {
                player.closeInventory();
                playSound(player, "click");
                return;
            }

            if (clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(expKey, PersistentDataType.INTEGER)) {
                int requiredExp = clicked.getItemMeta().getPersistentDataContainer().get(expKey, PersistentDataType.INTEGER);

                if (player.getLevel() < requiredExp) {
                    player.sendMessage(color("&c[CapMine] Sizda yetarli EXP daraja yo'q! Kerak: &e" + requiredExp));
                    playSound(player, "error");
                    return;
                }

                if (getConfig().getBoolean("settings.require-bottle", true) && !player.getInventory().contains(Material.GLASS_BOTTLE)) {
                    player.sendMessage(color("&c[CapMine] Inventarda bo'sh shisha (Glass Bottle) kerak!"));
                    playSound(player, "error");
                    return;
                }

                player.setLevel(player.getLevel() - requiredExp);
                if (getConfig().getBoolean("settings.require-bottle", true)) {
                    player.getInventory().removeItem(new ItemStack(Material.GLASS_BOTTLE, 1));
                }

                ItemStack expBottle = new ItemStack(clicked.getType());
                ItemMeta meta = expBottle.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(clicked.getItemMeta().getDisplayName());

                    List<String> lore = new ArrayList<>();
                    lore.add(color("&8CapMine Premium EXP"));
                    lore.add(" ");
                    lore.add(color("&fSaqlangan: &e" + requiredExp + " level"));
                    lore.add(color("&fOchish uchun &eO'ng tugmani &fbosing!"));
                    meta.setLore(lore);

                    meta.addEnchant(Enchantment.DURABILITY, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    meta.getPersistentDataContainer().set(expKey, PersistentDataType.INTEGER, requiredExp);

                    expBottle.setItemMeta(meta);
                }

                player.getInventory().addItem(expBottle);
                player.sendMessage(color("&a[CapMine] &f" + requiredExp + " level EXP idishga saqlandi!"));
                playSound(player, "save-success");
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onBottleRedeem(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.hasItemMeta()) {
                if (item.getItemMeta().getPersistentDataContainer().has(expKey, PersistentDataType.INTEGER)) {
                    event.setCancelled(true);

                    Player player = event.getPlayer();
                    int storedExp = item.getItemMeta().getPersistentDataContainer().get(expKey, PersistentDataType.INTEGER);

                    item.setAmount(item.getAmount() - 1);

                    player.giveExpLevels(storedExp);
                    playSound(player, "redeem-success");
                    player.sendMessage(color("&a[CapMine] &f" + storedExp + " level EXP qaytarildi!"));
                }
            }
        }
    }

    // Ovozlarni configdan o'qish funksiyasi
    private void playSound(Player player, String soundPath) {
        String soundName = getConfig().getString("sounds." + soundPath);
        if (soundName != null) {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, 1f, 1f);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    // HEX ranglarini o'qish
    private String color(String text) {
        if (text == null) return "";
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append("§").append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
            }
