package io.github.blockson64.playerRegion.api;

import org.bukkit.plugin.Plugin;

/**
 * Entry point of the player region service.
 *
 * <p>Obtain it from Bukkit's services manager in {@code onEnable}:
 * <pre>{@code
 * PlayerRegionApi api = getServer().getServicesManager().load(PlayerRegionApi.class);
 * PlayerRegionSubscription sub = api.subscribe(this, 128);
 * }</pre>
 *
 * <p>This interface only hands out subscriptions. All player queries go through the
 * {@link PlayerRegionSubscription} it returns. Main thread only.
 */
public interface PlayerRegionApi {

    /**
     * Subscribes to the player grid at the given resolution.
     *
     * <p>The cell size is a power of two.
     * Every subscriber of the same resolution shares one grid, which is refreshed once per
     * second while at least one subscription to it is open. The returned handle can only query
     * its own resolution, so subscribe separately for each cell size you need.
     *
     * <p>Close the returned handle when you are done with it, typically in {@code onDisable}.
     * Subscriptions owned by a plugin that gets disabled are closed automatically.
     *
     * @param owner    the plugin that owns the subscription; used for cleanup and diagnostics
     * @param cellSize requested cell edge length in blocks; must be at least 16 and {@code n^2}
     * @return a new subscription handle, never {@code null}
     * @throws IllegalArgumentException if {@code cellSize} is less than 16 or not {@code n^2}
     */
    PlayerRegionSubscription subscribe(Plugin owner, int cellSize);
}
