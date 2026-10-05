package io.github.blockson64.playerRegion;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;

public final class PlayerRegionGrid {

    public final int shift;
    private final int width;
    private final int cellRadius;
    private final int gridLocationBitMask;

    public final Set<SubscriptionImpl> subscribers = new HashSet<>();

    private static final class Cell {
        Player[] players = new Player[4];
        int[] xArr = new int[4], zArr = new int[4];     // parallel to players
        int size;


        void add(Player p, int x, int z) {
            if (size == players.length) grow();
            players[size] = p; xArr[size] = x; zArr[size] = z;
            size++;
        }

        private void grow() {
            players = Arrays.copyOf(players, players.length * 2);
            xArr = Arrays.copyOf(xArr, xArr.length * 2);
            zArr = Arrays.copyOf(zArr, zArr.length * 2);
        }
    }

    private final Map<UUID, Long2ObjectOpenHashMap<Cell>> worlds = new HashMap<>();

    // === constructor ===

    public PlayerRegionGrid(int shift) {
        this.shift = shift;
        this.width = 1 << shift;
        this.cellRadius = 1 << (shift - 1);
        this.gridLocationBitMask = -1 << (32 - shift) >> (32 - shift);
    }

    // === Interface ===

    public int cellSize() {
        return 1 << shift;
    }

    public void clear() {
        worlds.clear();
    }

    public void refresh() {
        for (World world : Bukkit.getWorlds()) {
            Long2ObjectOpenHashMap<Cell> regionMap = new Long2ObjectOpenHashMap<>();
            for (Player player : world.getPlayers()) {
                long key = key(player);
                if (regionMap.containsKey(key)) {
                    regionMap.get(key).add(player, (int) player.getX(), (int) player.getY());
                } else {
                    Cell cell = new Cell();
                    cell.add(player, (int) player.getX(), (int) player.getY());
                    regionMap.put(key, cell);
                }

            }
            worlds.put(world.getUID(), regionMap);
        }
    }

    public void add(Player player, UUID world, int x, int z) {
        Long2ObjectOpenHashMap<Cell> regionMap = new Long2ObjectOpenHashMap<>();
        long key = key(player);
        if (regionMap.containsKey(key)) {
            regionMap.get(key).add(player, (int) x, z);
        } else {
            Cell cell = new Cell();
            cell.add(player, (int) x, z);
            regionMap.put(key, cell);
        }
        worlds.put(world, regionMap);
    }

    public List<Player> getNear(World world, int x, int z) {
        List<Player> out = new ArrayList<>();
        forEachNear(world, x, z, out::add);
        return out;
    }

    public void forEachNear(World world, int x, int z, Consumer<Player> action) {
        Long2ObjectOpenHashMap<Cell> locationMap = worlds.get(world.getUID());
        int gridX = x >> shift;
        int gridZ = z >> shift;
        //temp array to loop through for each cell
        Player[] arr;
        //3x3 area grid around
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                Cell cell = locationMap.get(key(gridX + i, gridZ + j));
                if (cell == null) continue;
                arr = cell.players;
                for (int k = 0, n = cell.size; k < n; k++) action.accept(arr[k]);
            }
        }
    }

    public boolean anyNear(World world, int x, int z) {
        Long2ObjectOpenHashMap<Cell> locationMap = worlds.get(world.getUID());
        int gridX = x >> shift;
        int gridZ = z >> shift;
        //temp array to loop through for each cell
        Cell cell;
        //3x3 area grid around
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                cell = locationMap.get(key(gridX + i, gridZ + j));
                if (cell != null && cell.size > 0) return true;
            }
        }
        return false;
    }

    public List<Player> getWithinBounds(World world, int x, int z, int radius) {
        List<Player> out = new ArrayList<>();
        forEachWithinBounds(world, x, z, radius, out::add);
        return out;
    }

    public void forEachWithinBounds(World world, int x, int z, int radius, Consumer<Player> action) {
        Long2ObjectOpenHashMap<Cell> locationMap = worlds.get(world.getUID());
        int gridX = x >> shift;
        int gridZ = z >> shift;
        boolean encompassCenter = Math.abs(cellRadius - (x & gridLocationBitMask)) < radius - cellRadius &&
                                    Math.abs(cellRadius - (z & gridLocationBitMask)) < radius - cellRadius;
        //temp array to loop through for each cell
        Player[] arr;
        //3x3 area grid around
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                Cell cell = locationMap.get(key(gridX + i, gridZ + j));
                if (cell == null) continue;
                // fast check for center cell cause checks are often near players
                if (i == 0 && j == 0 && encompassCenter) {
                    arr = cell.players;
                    for (int k = 0, n = cell.size; k < n; k++) action.accept(arr[k]);
                }
                for (int k = 0, n = cell.size; k < n; k++) {
                    if (Math.abs(x - cell.xArr[k]) < radius && Math.abs(z - cell.zArr[k]) < radius) {
                        action.accept(cell.players[k]);
                    }
                }
            }
        }
    }

    public boolean anyWithinBounds(World world, int x, int z, int radius) {
        Long2ObjectOpenHashMap<Cell> locationMap = worlds.get(world.getUID());
        int gridX = x >> shift;
        int gridZ = z >> shift;
        //temp array to loop through for each cell
        Player[] arr;
        //3x3 area grid around
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                Cell cell = locationMap.get(key(gridX + i, gridZ + j));
                if (cell == null) continue;
                for (int k = 0, n = cell.size; k < n; k++) {
                    if (Math.abs(x - cell.xArr[k]) < radius && Math.abs(z - cell.zArr[k]) < radius) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public List<Player> getWithinRadius(World world, double x, double z, double radius) {
        List<Player> out = new ArrayList<>();
        forEachWithinRadius(world, x, z, radius, out::add);
        return out;
    }

    public void forEachWithinRadius(World world, double x, double z, double radius, Consumer<Player> action) {
        Long2ObjectOpenHashMap<Cell> locationMap = worlds.get(world.getUID());
        int gridX = (int) x >> shift;
        int gridZ = (int) z >> shift;
        boolean encompassCenter = Math.abs(cellRadius - (x % width)) * Math.abs(cellRadius - (z % width)) < (radius - cellRadius) * (radius - cellRadius);
        //temp array to loop through for each cell
        Player[] arr;
        //3x3 area grid around
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                Cell cell = locationMap.get(key(gridX + i, gridZ + j));
                if (cell == null) continue;
                // fast check for center cell cause checks are often near players
                arr = cell.players;
                if (i == 0 && j == 0 && encompassCenter) {
                    for (int k = 0, n = cell.size; k < n; k++) action.accept(arr[k]);
                }
                for (int k = 0, n = cell.size; k < n; k++) {
                    if (Math.abs(x - arr[k].getX()) < radius && Math.abs(z - arr[k].getZ()) < radius) {
                        action.accept(cell.players[k]);
                    }
                }
            }
        }
    }

    public boolean anyWithinRadius(World world, double x, double z, double radius) {
        Long2ObjectOpenHashMap<Cell> locationMap = worlds.get(world.getUID());
        int gridX = (int) x >> shift;
        int gridZ = (int) z >> shift;
        boolean encompassCenter = Math.abs(cellRadius - (x % width)) * Math.abs(cellRadius - (z % width)) < (radius - cellRadius) * (radius - cellRadius);
        //temp array to loop through for each cell
        Player[] arr;
        //3x3 area grid around
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                Cell cell = locationMap.get(key(gridX + i, gridZ + j));
                if (cell == null) continue;
                // fast check for center cell cause checks are often near players
                arr = cell.players;
                if (i == 0 && j == 0 && encompassCenter) {
                    for (int k = 0, n = cell.size; k < n; k++) return true;
                }
                for (int k = 0, n = cell.size; k < n; k++) {
                    if (Math.abs(x - arr[k].getX()) < radius && Math.abs(z - arr[k].getZ()) < radius) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // === Helper Methods ===

    private long key(Player player) {
        return key((int) player.getX() >> shift, (int) player.getZ() >> shift);
    }

    private long key(int x, int z) {
        return ((long) x << 32) | (z  & 0xFFFFFFFFL);
    }

}
