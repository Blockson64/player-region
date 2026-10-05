package io.github.blockson64.playerRegion;

import io.github.blockson64.playerRegion.api.PlayerRegionApi;
import io.github.blockson64.playerRegion.listener.PlayerRegionManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerRegion extends JavaPlugin {

    private PlayerRegionManager manager;

    @Override
    public void onEnable() {
        manager = new PlayerRegionManager(this);

        getServer().getServicesManager()
                .register(PlayerRegionApi.class, manager, this, ServicePriority.Normal);

        getServer().getPluginManager().registerEvents(manager, this);
        manager.start();
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.shutdown();
        getServer().getServicesManager().unregisterAll(this);
    }
}
