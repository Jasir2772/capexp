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
            openMenu(player);
            return true;
        }
        return false;
    }

    private void openMenu(Player player) {
        int size = getConfig().getInt("menu.size", 45);
        String title = color(getConfig().getString("menu.title", "&cCapMine EXP"));

        Inventory menu = Bukkit.createInventory(null, size, title);

        // 1. Chegaralarni to'ldirish (Sariq oynalar)
        ItemStack border = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
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

        // 2. Configdan level qo'shish
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
                    mat = Material.EXPERIENCE_BOTTLE; // Agar material xato yozilgan bo'lsa
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
                    
                    // Yaltirash effekti (1.19.4 uchun DURABILITY ishlatiladi)
                    meta.addEnchant(Enchantment.DURABILITY, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

                    // Idishga maxsus "imzo" qo'shish
                    meta.getPersistentDataContainer().set(expKey, PersistentDataType.INTEGER, exp);

                    item.setItemMeta(meta);
                    menu.setItem(slot, item);
                }
            }
        }

        // 3. Yopish tugmasi
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(color("&cYopish"));
            close.setItemMeta(closeMeta);
        }
        menu.setItem(size - 5, close);

        player.openInventory(menu);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(color(getConfig().getString("menu.title", "&cCapMine EXP")))) {
            event.setCancelled(true); // Item o'g'irlashni to'xtatish
            if (event.getCurrentItem() == null) return;

            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();

            if (clicked.getType() == Material.BARRIER) {
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1f, 1f);
                return;
            }

            // Agar EXP idishi bosilsa
            if (clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(expKey, PersistentDataType.INTEGER)) {
                int requiredExp = clicked.getItemMeta().getPersistentDataContainer().get(expKey, PersistentDataType.INTEGER);

                if (player.getLevel() < requiredExp) {
                    player.sendMessage(color("&c[CapMine] Sizda yetarli EXP daraja yo'q! Kerak: &e" + requiredExp));
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);
                    return;
                }

                // Shisha borligini tekshirish
                if (!player.getInventory().contains(Material.GLASS_BOTTLE)) {
                    player.sendMessage(color("&c[CapMine] Inventarda bo'sh shisha (Glass Bottle) kerak!"));
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);
                    return;
                }

                // EXPni ayirish va shishani olish
                player.setLevel(player.getLevel() - requiredExp);
                player.getInventory().removeItem(new ItemStack(Material.GLASS_BOTTLE, 1));

                // Yangi EXP idishini yaratish (PDC bilan)
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

                // O'yinchiga berish
                player.getInventory().addItem(expBottle);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                player.sendMessage(color("&a[CapMine] &f" + requiredExp + " level EXP idishga saqlandi!"));
                player.closeInventory();
            }
        }
    }

    // Idishni o'ng tugma bilan bosganda EXPni qaytarish
    @EventHandler
    public void onBottleRedeem(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.hasItemMeta()) {
                if (item.getItemMeta().getPersistentDataContainer().has(expKey, PersistentDataType.INTEGER)) {
                    event.setCancelled(true); // Oddiy EXP tushib ketishini to'xtatish

                    Player player = event.getPlayer();
                    int storedExp = item.getItemMeta().getPersistentDataContainer().get(expKey, PersistentDataType.INTEGER);

                    // Idishni o'chirish
                    item.setAmount(item.getAmount() - 1);

                    // EXPni qaytarish
                    player.giveExpLevels(storedExp);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    player.sendMessage(color("&a[CapMine] &f" + storedExp + " level EXP qaytarildi!"));
                }
            }
        }
    }

    // Ranglarni o'qish uchun kichik yordamchi (HEX va & belgilari)
    private String color(String text) {
        if (text == null) return "";
        // &#FF0000 formatini Minecraftning maxsus formatiga o'girish
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

        // Qolgan & belgilarini o'girish
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
                }
