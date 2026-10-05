package io.github.blockson64.playerRegion.listener;

import io.github.blockson64.playerRegion.PlayerRegionGrid;
import io.github.blockson64.playerRegion.SubscriptionImpl;
import io.github.blockson64.playerRegion.api.PlayerRegionApi;
import io.github.blockson64.playerRegion.api.PlayerRegionSubscription;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class PlayerRegionManager implements PlayerRegionApi, Listener {

    private static final long REFRESH_TICKS = 20L;     // 1 Hz
    private static final int MAX_CELL_SIZE = 1 << 30;
    private static final int MIN_CELL_SIZE = 1 << 4;

    private final Plugin host;
    private final Map<Integer, PlayerRegionGrid> grids = new HashMap<>();   // shift -> grid
    private PlayerRegionGrid[] active = new PlayerRegionGrid[0];                  // grids that have subscribers

    public PlayerRegionManager(Plugin host) {
        this.host = host;
    }
    public void start() {
        Bukkit.getScheduler().runTaskTimer(host, this::refresh, REFRESH_TICKS, REFRESH_TICKS);
    }
    public void shutdown() {
        for (PlayerRegionGrid grid : new ArrayList<>(grids.values())) {
            for (SubscriptionImpl subscription : new ArrayList<>(grid.subscribers)) subscription.close();
        }
    }

    // === API ===

    @Override
    public PlayerRegionSubscription subscribe(Plugin owner, int cellSize) {

        if (cellSize < MIN_CELL_SIZE || cellSize > MAX_CELL_SIZE) {
            throw new IllegalArgumentException("cellSize must be between " + MIN_CELL_SIZE + " and " + MAX_CELL_SIZE);
        }

        if ((int) Math.log(cellSize) / Math.log(2) == (int)(Math.floor(Math.log(cellSize) / Math.log(2)))) {
            throw new IllegalArgumentException("cellSize must be between a power of 2. was " + cellSize);
        }
        int shift = (int) (Math.log(cellSize) / Math.log(2));

        PlayerRegionGrid grid = grids.get(shift);
        if (grid == null) {
            grid = new PlayerRegionGrid((int) shift);
            grids.put(shift, grid);
        }
        boolean firstSubscriber = grid.subscribers.isEmpty();

        SubscriptionImpl sub = new SubscriptionImpl(owner, grid, this);
        grid.subscribers.add(sub);

        if (firstSubscriber) {
            populate(grid);           // so the first query is not empty for up to a second
            rebuildActive();
        }
        return sub;

    }

    void release(SubscriptionImpl sub, PlayerRegionGrid grid) {
        grid.subscribers.remove(sub);
        if (grid.subscribers.isEmpty()) {
            grid.clear();
            grids.remove(grid.shift);   // last subscriber gone: drop the grid and stop refreshing it
            rebuildActive();
        }
    }

    // ---- Refresh ----

    private void refresh() {
        PlayerRegionGrid[] grids = this.active;
        if (grids.length == 0) return;

        for (PlayerRegionGrid g : grids) g.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID world = p.getWorld().getUID();
            int x = (int) Math.floor(p.getX());      // read the position once, share it across all grids
            int z = (int) Math.floor(p.getZ());
            for (PlayerRegionGrid g : grids) g.add(p, world, x, z);
        }
    }

    private void populate(PlayerRegionGrid g) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            g.add(p, p.getWorld().getUID(), (int) Math.floor(p.getX()), (int) Math.floor(p.getZ()));
        }
    }

    private void rebuildActive() {
        List<PlayerRegionGrid> list = new ArrayList<>();
        for (PlayerRegionGrid g : grids.values()) {
            if (!g.subscribers.isEmpty()) list.add(g);
        }
        active = list.toArray(new PlayerRegionGrid[0]);
    }

    // ---- Cleanup for plugins that forget to close ----

    @EventHandler
    public void onPluginDisable(PluginDisableEvent e) {
        Plugin disabled = e.getPlugin();
        for (PlayerRegionGrid g : new ArrayList<>(grids.values())) {
            for (SubscriptionImpl s : new ArrayList<>(g.subscribers)) {
                if (s.owner.equals(disabled)) s.close();
            }
        }
    }

}
