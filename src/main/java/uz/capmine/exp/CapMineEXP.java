package uz.capmine.exp;

import org.bukkit.plugin.java.JavaPlugin;

public class CapMineEXP extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("CapMine Premium EXP tizimi yoqildi!");
        saveDefaultConfig(); // config.yml ni yuklaydi
    }

    @Override
    public void onDisable() {
        getLogger().info("CapMine EXP tizimi o'chirildi.");
    }
}
