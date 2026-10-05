package io.github.blockson64.playerRegion.api;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

/**
 * A handle to one resolution of the shared player grid.
 *
 * <p>The world is divided into square cells of {@link #cellSize()} blocks. The grid records which
 * players are in which cell and is refreshed by the manager at a fixed interval (1 Hz by default).
 * Every subscriber of the same cell size shares the same underlying grid, so subscribing is cheap.
 * Queries are only possible through a subscription, and a subscription only answers for its own
 * resolution.
 *
 * <table>
 *     <caption>Query tiers</caption>
 *   <tr><th>Tier</th><th>Precision</th><th>Cost</th></tr>
 *   <tr><td>{@code Near}  </td>          <td>Cell-granular: the 3x3 cells around the point, no distance check  </td>  <td>Lowest</td></tr>
 *   <tr><td>{@code WithinBounds}  </td>  <td>Exact square check on each candidate  </td>                              <td>Medium</td></tr>
 *   <tr><td>{@code WithinRadius}  </td>  <td>Exact circular distance check on each candidate  </td>                   <td>Highest</td></tr>
 * </table>
 */
public interface PlayerRegionSubscription extends AutoCloseable {

    /**
     * Returns the cell edge length of this grid in blocks.
     *
     * <p>Always a power of two.
     *
     * @return cell edge length in blocks
     */
    int cellSize();

    // ------------------------------------------------------------------
    // Tier 1: Near (cell-granular, fastest)
    // ------------------------------------------------------------------

    /**
     * Returns every player in the 3x3 block of cells centered on the cell containing (x, z).
     *
     * <p>No distance check is performed. Every player within {@link #cellSize()} blocks of the
     * point on both axes is guaranteed to be included.
     *
     * <p>Allocates a new list on every call.
     *
     * @param world world to search
     * @param x     block X coordinate of the center
     * @param z     block Z coordinate of the center
     * @return a new, caller-owned list, empty if no players are found (never {@code null})
     */
    List<Player> getPlayersNear(World world, int x, int z);

    /**
     * Visits every player in the 3x3 block of cells centered on the cell containing (x, z).
     *
     * @param world  world to search
     * @param x      block X coordinate of the center
     * @param z      block Z coordinate of the center
     * @param action called once per player found; must not modify the grid
     */
    void forEachNear(World world, int x, int z, Consumer<Player> action);

    /**
     * Returns whether at least one player is in the 3x3 block of cells around (x, z).
     *
     * <p>Same selection as {@link #getPlayersNear}. Stops at the first player found.
     *
     * @param world world to search
     * @param x     block X coordinate of the center
     * @param z     block Z coordinate of the center
     * @return {@code true} if any player is in range, {@code false} otherwise
     */
    boolean anyNear(World world, int x, int z);

    // ------------------------------------------------------------------
    // Tier 2: WithinBounds (exact square check, medium)
    // ------------------------------------------------------------------

    /**
     * Returns every player whose X and Z are both within {@code radius} blocks of (x, z), that is,
     * inside a square of half-width {@code radius}.
     *
     * <p>Allocates a new list on every call.
     *
     * @param world  world to search
     * @param x      block X coordinate of the center
     * @param z      block Z coordinate of the center
     * @param radius half-width of the square in blocks; values below zero and above the cell width return an empty list
     * @return a new, caller-owned list, empty if no players are found (never {@code null})
     */
    List<Player> getPlayersWithinBounds(World world, int x, int z, int radius);

    /**
     * Visits every player inside the square of half-width {@code radius} around (x, z).
     *
     * @param world  world to search
     * @param x      block X coordinate of the center
     * @param z      block Z coordinate of the center
     * @param radius half-width of the square in blocks; values below zero and above the cell width are ignored
     * @param action called once per player found; must not modify the grid
     */
    void forEachWithinBounds(World world, int x, int z, int radius, Consumer<Player> action);

    /**
     * Returns whether at least one player is inside the square of half-width {@code radius}
     * around (x, z).
     *
     * @param world  world to search
     * @param x      block X coordinate of the center
     * @param z      block Z coordinate of the center
     * @param radius half-width of the square in blocks; values below zero and above the cell width return {@code false}
     * @return {@code true} if any player is inside the square, {@code false} otherwise
     */
    boolean anyWithinBounds(World world, int x, int z, int radius);

    // ------------------------------------------------------------------
    // Tier 3: WithinRadius (exact circle check, slowest)
    // ------------------------------------------------------------------

    /**
     * Returns every player within {@code radius} blocks of (x, z) on the horizontal plane.
     *
     * <p>Allocates a new list on every call.
     *
     * @param world  world to search
     * @param x      center X coordinate
     * @param z      center Z coordinate
     * @param radius radius in blocks; values below zero and above the cell width return an empty list
     * @return a new, caller-owned list, empty if no players are found (never {@code null})
     */
    List<Player> getPlayersWithinRadius(World world, double x, double z, double radius);

    /**
     * Visits every player within {@code radius} blocks of (x, z) on the horizontal plane.
     *
     * @param world  world to search
     * @param x      center X coordinate
     * @param z      center Z coordinate
     * @param radius radius in blocks; values below zero and above the cell width are ignored
     * @param action called once per player found; must not modify the grid
     */
    void forEachWithinRadius(World world, double x, double z, double radius, Consumer<Player> action);

    /**
     * Returns whether at least one player is within {@code radius} blocks of (x, z) on the
     * horizontal plane.
     *
     * @param world  world to search
     * @param x      center X coordinate
     * @param z      center Z coordinate
     * @param radius radius in blocks; values below zero and above the cell width return {@code false}
     * @return {@code true} if any player is in range, {@code false} otherwise
     */
    boolean anyWithinRadius(World world, double x, double z, double radius);


    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Returns whether this subscription is still open.
     *
     * <p>A subscription is active from creation until {@link #close()} is called or its owning
     * plugin is disabled.
     *
     * @return {@code true} if queries return live results, {@code false} once closed
     */
    boolean isActive();

    /**
     * Closes this subscription and releases its claim on the grid.
     *
     * <p>When the last subscription to a given cell size is closed, that grid stops being refreshed
     * and its data is dropped.
     */
    @Override
    void close();
}