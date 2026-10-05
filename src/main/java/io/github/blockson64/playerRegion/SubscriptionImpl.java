package io.github.blockson64.playerRegion;

import io.github.blockson64.playerRegion.api.PlayerRegionSubscription;
import io.github.blockson64.playerRegion.listener.PlayerRegionManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SubscriptionImpl implements PlayerRegionSubscription {

    public final Plugin owner;
    private final PlayerRegionGrid grid;
    private final PlayerRegionManager manager;
    private boolean active = true;

    public SubscriptionImpl(Plugin owner, PlayerRegionGrid grid, PlayerRegionManager manager) {
        this.owner = owner;
        this.grid = grid;
        this.manager = manager;
    }

    @Override
    public int cellSize() {
        return grid.cellSize();
    }

    @Override
    public List<Player> getPlayersNear(World world, int x, int z) {
        List<Player> out = new ArrayList<>();
        if (active) out = grid.getNear(world, x, z);
        return out;
    }

    @Override
    public void forEachNear(World world, int x, int z, Consumer<Player> action) {
        if (active) grid.forEachNear(world, x, z, action);
    }

    @Override
    public boolean anyNear(World world, int x, int z) {
        return active && grid.anyNear(world, x, z);
    }

    @Override
    public List<Player> getPlayersWithinBounds(World world, int x, int z, int radius) {
        List<Player> out = new ArrayList<>();
        if (active) out = grid.getWithinBounds(world, x, z, radius);
        return out;
    }

    @Override
    public void forEachWithinBounds(World world, int x, int z, int radius, Consumer<Player> action) {
        if(active) grid.forEachWithinBounds(world, x, z, radius, action);
    }

    @Override
    public boolean anyWithinBounds(World world, int x, int z, int radius) {
        return active && grid.anyWithinBounds(world, x, z, radius);
    }

    @Override
    public List<Player> getPlayersWithinRadius(World world, double x, double z, double radius) {
        List<Player> out = new ArrayList<>();
        if (active) out = grid.getWithinRadius(world, x, z, radius);
        return out;
    }

    @Override
    public void forEachWithinRadius(World world, double x, double z, double radius, Consumer<Player> action) {
        if(active) grid.forEachWithinRadius(world, x, z, radius, action);
    }

    @Override
    public boolean anyWithinRadius(World world, double x, double z, double radius) {
        return active && grid.anyWithinRadius(world, x, z, radius);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void close() {
        if (!active) return;
        active = false;
        manager.release(this, grid);
    }

    //unimplemented



















}
