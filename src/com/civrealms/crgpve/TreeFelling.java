package com.civrealms.crgpve;

import com.civrealms.crgmain.CivRealmsGlue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.material.MaterialData;

/**
 * @author caucow + crimeo
 */
public class TreeFelling implements Listener {
    
    private CivRealmsGlue plugin;
    
    public TreeFelling(CivRealmsGlue plugin) {
		this.plugin = plugin;
	}
        
    // should only trigger if chopped log was actually broken.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent evt) {
        Block hitBlock = evt.getBlock();
        BlockState hitState = hitBlock.getState();
        if (!isLog(hitState.getType())) {
            return;
        }
        if (Math.random() > 0.05 && !evt.getPlayer().getInventory().getItemInMainHand().getType().name().contains("_AXE")){
            evt.setCancelled(true);
            return;
        }
        
        Player p = evt.getPlayer();
        // Only player-based blockbreaks, not tnt etc.
        if (p == null) {
            return;
        }
        // I've wanted silktouch axes to allow vanilla-like tree farming/cutting
        // for a while. Not terribly useful, maybe it'll just be a curse to silk
        // axes, but the option is nice. Comment to remove I guess. Maybe ask
        // around if that's a thing that should be added.
        if (p.getInventory().getItemInMainHand()
                .getEnchantments().containsKey(Enchantment.SILK_TOUCH)) {
            return;
        }
        
        // possibly set these per log/leaf type (accounting for worldgen
        // log/tree types)?
        int logSearchRadius = 16; // only horizontal (trees r tall)
        int logSearchUp = 40; // should be at least 32 due to tall spruce
        int logSearchDown = 5;
        int leafDecayRadius = 12; // used for dist on all 3 axis
        // if a "tree" will be felled, this is how much of a gap can exist
        // between the top "this tree" block in the column and logs that may
        // have been found above it that should also fall (this combats floating
        // log problems caused by tick-perfect chopping times, ex. e4 dia axe)
        int columnGapTolerance = 16;
        // Log count tolerance before it's decided the "tree" is actually a
        // "build"; With overlapping worldgen trees, I've seen the size of a
        // single "tree" hitting 600ish logs, 1000 should be a safe number.
        int maxLogsPerTree = 1200;
        
        // Depending on the shape of the """""tree""""" the floodfill stuff
        // could take a while. As long as nothing is being *modified* async,
        // this should be safe. It does schedule sync tasks to make the actual
        // changes to the world.
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> chopTreeAsyncFunc(p, hitBlock, hitState,
                        logSearchRadius, logSearchUp, logSearchDown,
                        leafDecayRadius, columnGapTolerance, maxLogsPerTree));
    }
    
    private static boolean isLog(Material mat) {
        return mat == Material.LOG | mat == Material.LOG_2;
    }
    
    private static boolean isLeaves(Material mat) {
        return mat == Material.LEAVES | mat == Material.LEAVES_2;
    }
    
    private static boolean isGrowth(Material mat) {
        // Add material types here that are often found "growing" on trees that
        // should fall with the tree. AFAIK this is just pumpkins from the SW
        // jungle and mushroom blocks from the northern Taiga wasteland.
        return mat == Material.PUMPKIN | mat == Material.HUGE_MUSHROOM_1
                | mat == Material.HUGE_MUSHROOM_2 | mat == Material.FENCE;
    }
    
    /**
     * Called in chopTreeAsyncFunc to check if a log can be felled. Add
     * reinforcement checks here if wanted, it should be running async anyways.
     * @param b "log" block
     * @return can log be broken/felled
     */
    private boolean canFellLog(Block b) {
        if (com.civrealms.crgmain.CitadelChecks.hasReinforcementAtAll(b)){
            return false;
        }
        return true;
    }
    
    private void chopTreeAsyncFunc(Player p, Block hitBlock, BlockState hitState,
            int logSearchRadius, int logSearchUp, int logSearchDown,
            int leafDecayRadius, int columnGapTolerance, int maxLogsPerTree) {
        if (!isLog(hitState.getType())) {
            return;
        }
        
        ////////////////////////////////////////////////////////////////////////
        // <async> because the floodfills could potentially become very time
        //   consuming on large trees/log filled builds, this method should* be
        //   run async, though it can* be run synchronously. The two methods it
        //   calls that actually modify the world are queued as sync tasks and
        //   do a sanity check to make sure the block they are expecting is the
        //   block that is still in the world
        //
        // = WTF? =
        //
        // flood-search for logs (logSearchRadius/Up/Down)
        // flood-search from logs for leaves and other logs (logSR+leafDR*2)
        // flood-search from diffTree logs and remove leaves from set if closer
        //   to diffTree log
        // "tree" defined for purpose of worldgen leaf decay
        // <sync> &= leaf data typebits, removing the decay preventing bits
        //
        // = ALSO =
        //
        // flood-search for logs (logSearchRadius/Up/Down)
        // search for leaves in a + shape above chopped log (logSUp+leafDR)
        //   (improvement over CivRealms, around 10 leaves minimum need to decay
        //   before logs get stuck floating (esp in 2x2 spruce))
        // queue immediate log column (and logs floating up to 16 above) to fall
        // prune logs in tree reachable from logs at y level of hit block from
        //   map
        // queue remaining logs to fall
        // "tree" defined for purpose of falling logs
        // <sync> turn queued logs to fallingblock
        ////////////////////////////////////////////////////////////////////////
        
        // Set up for flood fill. Create flood queue and list for logs.
        Queue<FloodPos> searchQueue;
        searchQueue = new ArrayDeque<>();
        List<FloodPos> logs = new ArrayList<>();
        FloodPos startPos = new FloodPos(hitBlock, hitBlock, 0);
        searchQueue.add(startPos);
        logs.add(startPos);
        int logSRSqr = (int)((logSearchRadius + 0.5) * (logSearchRadius + 0.5));
        
        // autism because I need to change the variable from an inner class
        // 0 - found leaves w/ non-vanilla-tree trees
        // 1 - found leaves above chopped log (for felling check)
        boolean[] hasleaf_setleaf = new boolean[2];
        
        // Flood from the log or coal block broken, searching for other logs
        // or coal blocks OF THE SAME TYPE.
        // <editor-fold desc="Flood Call">
        try {
            flood(searchQueue,
                    (curPos, nextPos) -> {
                        Block nextBlock = nextPos.b;
                        int distX = curPos.start.getX() - nextBlock.getX();
                        int distZ = curPos.start.getZ() - nextBlock.getZ();
                        if (distX * distX + distZ * distZ < logSRSqr
                                & nextBlock.getType() == hitState.getType()) {
                            searchQueue.add(nextPos);
                            logs.add(nextPos);
                            if (logs.size() > maxLogsPerTree) {
                                throw new IllegalStateException("Too many logs, killing flood.");
                            }
                        }
                        hasleaf_setleaf[0] |=
                                isLeaves(nextBlock.getType())
                                & (nextBlock.getData() & 0b1100) != 0;
                    },
                    hitBlock.getX() - logSearchRadius,
                    hitBlock.getY() - logSearchDown,
                    hitBlock.getZ() - logSearchRadius,
                    hitBlock.getX() + logSearchRadius,
                    hitBlock.getY() + logSearchUp,
                    hitBlock.getZ() + logSearchRadius);
        } catch (IllegalStateException ise) {
            // Too many logs found to be considered a "tree", stop and return.
            return;
        }
        // </editor-fold>
        
        // <editor-fold desc="Check for Leaves/Fell Tree">
        {
            int sx = startPos.b.getX(),
                    sy = startPos.b.getY(),
                    sz = startPos.b.getZ();
            // find leaves (in + shape) if NO non-vanillatree leaves were found
            if (!hasleaf_setleaf[0]) {
                int[][] leafDirs = {{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
                findLeaf:
                for (int i = 0; i <= logSearchUp + leafDecayRadius + 32 && sy + i <= 255; i++) {
                    for (int[] leafDir : leafDirs) {
                        Block b = startPos.b.getRelative(leafDir[0], i + leafDir[1], leafDir[2]);
                        if (isLeaves(b.getType())) {
                            hasleaf_setleaf[1] = true;
                            break findLeaf;
                        }
                    }
                }
            }
            // if leaves were found (either method), try to fell tree
            if (hasleaf_setleaf[0] | hasleaf_setleaf[1]) {
                List<Block> fell = new ArrayList<>();
                HashMap<Long, FloodPos> logMap = new HashMap<>();
                // add logs at/above chop level to map
                // add logs at chop level to flood queue
                for (FloodPos fp : logs) {
                    Block b = fp.b;
                    if (b.getY() >= sy) {
                        logMap.put(hashCoords(b.getX(), b.getY(), b.getZ()), fp);
                    }
                    Block below = fp.b.getRelative(0, -1, 0);
                    if (b.getY() == sy && (fp.b.getX() != sx || fp.b.getZ() != sz) && below.getType().isSolid() && !isLeaves(below.getType())) {
                        searchQueue.add(new FloodPos(fp.b, fp.b, 0));
                    }
                }
                // Add immediate log column to fell list, remove from flood's
                // search map. Include tolerance to help with floating left by
                // both players and falling block tick delay.
                for (int airGap = 0, y = 0, maxY = Math.min(255, startPos.b.getY() + logSearchUp + leafDecayRadius); airGap <= columnGapTolerance && y <= maxY; y++) {
                    FloodPos columnPos;
                    if ((columnPos = logMap.remove(hashCoords(sx, sy + y, sz))) != null
                            && columnPos.b.getType() == hitState.getType()) {
                        fell.add(columnPos.b);
                        airGap = 0;
                    } else {
                        Block nextb = startPos.b.getRelative(0, y, 0);
                        airGap++;
                        if (nextb.getType() == hitState.getType()) {
                            fell.add(nextb);
                        } else if (!isLeaves(nextb.getType()) & nextb.getType() != Material.AIR) {
                            break;
                        }
                    }
                }
                
                // flood to remove logs reachable from the base/stable/solid
                // logs (these logs should NOT fall since they are supported)
                // <editor-fold desc="Flood Call">
                flood(searchQueue,
                        (curPos, nextPos) -> {
                            FloodPos old;
                            if ((old = logMap.remove(hashCoords(nextPos.b.getX(), nextPos.b.getY(), nextPos.b.getZ()))) != null
                                    && nextPos.b.getY() >= curPos.b.getY()) {
                                searchQueue.add(nextPos);
                            }
                        },
                        hitBlock.getX() - logSearchRadius,
                        hitBlock.getY() - logSearchDown,
                        hitBlock.getZ() - logSearchRadius,
                        hitBlock.getX() + logSearchRadius,
                        hitBlock.getY() + logSearchUp,
                        hitBlock.getZ() + logSearchRadius);
                // </editor-fold>
                
                // add remaining logs to fell list
                for (FloodPos fp : logMap.values()) {
                    fell.add(fp.b);
                }
                // Flood to find pumpkins because jungles are aids.
                // Edit: also large mushroom blocks in Taiga M trees.
                for (Block b : fell) {
                    searchQueue.add(new FloodPos(b, b, 0));
                }
                // <editor-fold desc="Flood Call">
                flood(searchQueue,
                        (curPos, nextPos) -> {
                            if (isGrowth(nextPos.b.getType())) {
                                fell.add(nextPos.b);
                            }
                        },
                        hitBlock.getX() - logSearchRadius - 1,
                        hitBlock.getY() - logSearchDown - 1,
                        hitBlock.getZ() - logSearchRadius - 1,
                        hitBlock.getX() + logSearchRadius + 1,
                        hitBlock.getY() + logSearchUp + 1,
                        hitBlock.getZ() + logSearchRadius + 1);
                // </editor-fold>
                
                ListIterator<Block> litBlox = fell.listIterator();
                while (litBlox.hasNext()) {
                    if (!canFellLog(litBlox.next())) {
                        litBlox.remove();
                    }
                }
                
                // Schedule sync task to make the tree actually fall.
                Bukkit.getScheduler().runTask(plugin, () -> fellTreeSyncFunc(p, sy, fell));
            }
        }
        // </editor-fold>
        
        // <editor-fold desc="Fix Leaves">
        if (hasleaf_setleaf[0]) {
            // Add all the logs found to the search queue, resetting their search
            // direction and the start point of the flood to the log itself.
            for (FloodPos fp : logs) {
                searchQueue.add(new FloodPos(fp.b, fp.b, 0));
            }
            // Set up to find logs from other trees as well as leaves that could be
            // in this tree or the other trees.
            List<FloodPos> otherLogs = new ArrayList<>();
            List<FloodPos> leaves = new ArrayList<>();
            int maxLeafDistSq = (int)((leafDecayRadius * 2 + 0.5) * (leafDecayRadius * 2 + 0.5));

            // Flood outward from the logs, searching for leaves and other logs.
            // <editor-fold desc="Flood Call">
            flood(searchQueue,
                    (curPos, nextPos) -> {
                        Block nextBlock = nextPos.b;
                        if (nextPos.dist < maxLeafDistSq) {
                            if (nextBlock.getType() == Material.LEAVES
                                    | nextBlock.getType() == Material.LEAVES_2) {
                                searchQueue.add(nextPos);
                                leaves.add(nextPos);
                            }
                            if (nextBlock.getType() == Material.LOG
                                    | nextBlock.getType() == Material.LOG_2) {
                                otherLogs.add(new FloodPos(nextBlock, curPos.start, 0));
                            }
                        }
                    },
                    hitBlock.getX() - logSearchRadius - leafDecayRadius * 2,
                    hitBlock.getY() - logSearchDown - leafDecayRadius * 2,
                    hitBlock.getZ() - logSearchRadius - leafDecayRadius * 2,
                    hitBlock.getX() + logSearchRadius + leafDecayRadius * 2,
                    hitBlock.getY() + logSearchUp + leafDecayRadius * 2,
                    hitBlock.getZ() + logSearchRadius + leafDecayRadius * 2);
            // </editor-fold>

            // Add all the other logs found to the search queue, resetting their
            // search direction and the start point of the flood to the log itself
            for (FloodPos fp : otherLogs) {
                searchQueue.add(new FloodPos(fp.b, fp.b, 0));
            }
            // Set up to remove unwanted and duplicate leaves 
            HashMap<Long, FloodPos> leafMap = new HashMap<>();
            for (FloodPos fp : leaves) {
                Block b = fp.b;
                leafMap.put(hashCoords(b.getX(), b.getY(), b.getZ()), fp);
            }

            // <editor-fold desc="Flood Call">
            flood(searchQueue,
                    (curPos, nextPos) -> {
                        Block nextBlock = nextPos.b;
                        if (nextPos.dist < maxLeafDistSq
                                & (nextBlock.getType() == Material.LEAVES
                                | nextBlock.getType() == Material.LEAVES_2)) {
                            long nextCoordHash = hashCoords(nextBlock.getX(), nextBlock.getY(), nextBlock.getZ());
                            FloodPos stored = leafMap.get(nextCoordHash);
                            if (stored != null && nextPos.dist < stored.dist) {
                                leafMap.remove(nextCoordHash);
                                searchQueue.add(nextPos);
                            }
                        }
                    },
                    hitBlock.getX() - logSearchRadius - leafDecayRadius * 2,
                    hitBlock.getY() - logSearchDown - leafDecayRadius * 2,
                    hitBlock.getZ() - logSearchRadius - leafDecayRadius * 2,
                    hitBlock.getX() + logSearchRadius + leafDecayRadius * 2,
                    hitBlock.getY() + logSearchUp + leafDecayRadius * 2,
                    hitBlock.getZ() + logSearchRadius + leafDecayRadius * 2);
            // </editor-fold>

            leaves.clear();
            for (Map.Entry<Long, FloodPos> e : leafMap.entrySet()) {
                leaves.add(e.getValue());
            }
            Bukkit.getScheduler().runTask(plugin, () -> fixLeavesSyncFunc(leaves));
        }
        // </editor-fold>
    }
    
    private void fellTreeSyncFunc(Player p, int bottomY, List<Block> logs) {
        Collections.sort(logs, (a, b) -> a.getY() - b.getY());
        HashMap<Integer, Integer> checked = new HashMap<>();
        if (!logs.isEmpty()) {
        }
        logs.forEach((b) -> {
            if (isLog(b.getType()) | isGrowth(b.getType())) {
                // remove leaves below log so it can fall
                int key = (b.getX() & 0xFFFF) << 16 | (b.getZ() & 0xFFFF);
                Integer checkY = checked.get(key);
                if (checkY == null) {
                    // start 1 block below log, end at just below chopped log Y level, destroy leaves.
                    for (int maxi = b.getY() - bottomY + 6, i = 1; i < maxi; i++) {
                        Block checkBlock = b.getRelative(0, -i, 0);
                        if (isLeaves(checkBlock.getType())) {
                            checkBlock.breakNaturally();
                        }
                        if (checkBlock.getType().isSolid()) {
                            break;
                        }
                    }
                } else if (checkY + 1 < b.getY()) {
                    // start 1 block below log, end at Y of last log, destroy leaves
                    for (int maxi = b.getY() - checkY, i = 1; i < maxi; i++) {
                        Block checkBlock = b.getRelative(0, -i, 0);
                        if (isLeaves(checkBlock.getType())) {
                            checkBlock.breakNaturally();
                        }
                        if (checkBlock.getType().isSolid()) {
                            break;
                        }
                    }
                }
                checked.put(key, b.getY());
                // spawn falling sand and remove log if not on solid
                Block down = b.getRelative(0, -1, 0);
                if (down != null && !down.getType().isSolid()) {
                    MaterialData stateData = b.getState().getData();
                    double rand = Math.random();
                    Location loc = null;
                    if (rand < 0.01){
                        int randShimmy = (int)(Math.random()*16)+1;
                        switch(randShimmy) {
                            case 1: loc = shimmyCheck(p,b,-3,0,-1); break;
                            case 2: loc = shimmyCheck(p,b,-3,0,0); break;
                            case 3: loc = shimmyCheck(p,b,-3,0,1); break;
                            case 4: loc = shimmyCheck(p,b,3,0,-1); break;
                            case 5: loc = shimmyCheck(p,b,3,0,0); break;
                            case 6: loc = shimmyCheck(p,b,3,0,1); break;
                            case 7: loc = shimmyCheck(p,b,-1,0,3); break;
                            case 8: loc = shimmyCheck(p,b,-1,0,-3); break;
                            case 9: loc = shimmyCheck(p,b,0,0,3); break;
                            case 10: loc = shimmyCheck(p,b,0,0,-3); break;
                            case 11: loc = shimmyCheck(p,b,1,0,3); break;
                            case 12: loc = shimmyCheck(p,b,1,0,-3); break;
                            default: loc = b.getLocation().add(0.5,0.0,0.5);
                          }
                    } else if (rand < 0.05){
                        int randShimmy = (int)(Math.random()*16)+1;
                        switch(randShimmy) {
                            case 1: loc = shimmyCheck(p,b,-2,0,-2); break;
                            case 2: loc = shimmyCheck(p,b,-2,0,-1); break;
                            case 3: loc = shimmyCheck(p,b,-2,0,0); break;
                            case 4: loc = shimmyCheck(p,b,-2,0,1); break;
                            case 5: loc = shimmyCheck(p,b,-2,0,2); break;
                            case 6: loc = shimmyCheck(p,b,-1,0,-2); break;
                            case 7: loc = shimmyCheck(p,b,-1,0,2); break;
                            case 8: loc = shimmyCheck(p,b,0,0,-2); break;
                            case 9: loc = shimmyCheck(p,b,0,0,2); break;
                            case 10: loc = shimmyCheck(p,b,1,0,-2); break;
                            case 11: loc = shimmyCheck(p,b,1,0,2); break;
                            case 12: loc = shimmyCheck(p,b,2,0,-2); break;
                            case 13: loc = shimmyCheck(p,b,2,0,-1); break;
                            case 14: loc = shimmyCheck(p,b,2,0,0); break;
                            case 15: loc = shimmyCheck(p,b,2,0,1); break;
                            case 16: loc = shimmyCheck(p,b,2,0,2); break;
                            default: loc = b.getLocation().add(0.5,0.0,0.5);
                          }
                    } else if (rand < 0.15){
                        int randShimmy = (int)(Math.random()*8)+1;
                        switch(randShimmy) {
                            case 1: loc = shimmyCheck(p,b,-1,0,-1); break;
                            case 2: loc = shimmyCheck(p,b,-1,0,0); break;
                            case 3: loc = shimmyCheck(p,b,-1,0,1); break;
                            case 4: loc = shimmyCheck(p,b,0,0,-1); break;
                            case 5: loc = shimmyCheck(p,b,0,0,1); break;
                            case 6: loc = shimmyCheck(p,b,1,0,-1); break;
                            case 7: loc = shimmyCheck(p,b,1,0,0); break;
                            case 8: loc = shimmyCheck(p,b,1,0,1); break;
                            default: loc = b.getLocation().add(0.5,0.0,0.5);
                          }
                    } else {
                        loc = b.getLocation().add(0.5,0.0,0.5);
                    }
                    if (loc != null){
                        b.setType(Material.AIR, true);
                        clearLeaves(loc);
                        FallingBlock fallentity = b.getWorld().spawnFallingBlock(loc, stateData);
                        fallentity.setDropItem(false);
                        fallentity.setHurtEntities(true);
                    } else {
                        plugin.getLogger().info("Debug null location B " + b.getX() + ", " + b.getY() + ", " + b.getZ());
                    }
                }
            }
        });
    }
    
    private void clearLeaves(Location loc){
        for (int y = -1; y > -31; y--){
            if (loc.getBlockY()+y < 1){
                return;
            } else {
                Block relBlock = loc.getBlock().getRelative(0,y,0);
                if (relBlock.getType() == Material.LEAVES || relBlock.getType() == Material.LEAVES_2){
                    relBlock.setType(Material.AIR);
                }
            }
        }
    }
    
    private Location shimmyCheck(Player p, Block b, int x, int y, int z){
        Block shimmyBlock = b.getRelative(x,y,z);
        if (com.civrealms.crgmain.CitadelChecks.hasReinforcementAtAll(shimmyBlock)){
            plugin.getLogger().info("Debug null location A " + shimmyBlock.getX() + ", " + b.getY() + ", " + b.getZ());
            return null;
        }
        if (shimmyBlock.getType() == Material.AIR || shimmyBlock.getType() == Material.LEAVES || shimmyBlock.getType() == Material.LEAVES_2){
            if (shimmyBlock.getRelative(0,-1,0).getType() == Material.AIR || shimmyBlock.getRelative(0,-1,0).getType() == Material.LEAVES || shimmyBlock.getRelative(0,-1,0).getType() == Material.LEAVES_2){
                b.setType(Material.AIR,true);
                return shimmyBlock.getLocation().add(x+0.5,y,z+0.5);
            }
        }
        return b.getLocation().add(0.5, 0.0, 0.5);
    }
    
    private void fixLeavesSyncFunc(Collection<FloodPos> leaves) {
        leaves.forEach((fp) -> {
            Block b = fp.b;
            if (isLeaves(b.getType())) {
                b.setData((byte)(b.getData() & 0b0011 | 0b1000), true);
                //b.setData((byte)(b.getData() & 0b0011), true);
            }
        });
    }
    
    // block offsets relative to "current block being flooded from"
    public static final int[][] relativeDirs = {
        // "here"
        {0, 0, 0}, // 0
        // axial
        {0, -1, 0}, // 1
        {-1, 0, 0},
        {0, 0, -1},
        {0, 0, 1},
        {1, 0, 0},
        {0, 1, 0}, // 6
        // edge diagonl
        {-1, -1, 0}, // 7
        {0, -1, -1},
        {0, -1, 1},
        {1, -1, 0},
        {-1, 0, -1},
        {-1, 0, 1},
        {1, 0, -1},
        {1, 0, 1},
        {-1, 1, 0},
        {0, 1, -1},
        {0, 1, 1},
        {1, 1, 0}, // 18
        // corner diagonal
        {-1, -1, -1}, // 19
        {-1, -1, 1},
        {1, -1, -1},
        {1, -1, 1},
        {1, 1, -1},
        {1, 1, 1},
        {-1, 1, -1},
        {-1, 1, 1}, // 26
    };
    
    // relativeDirs to use for any given search direction
    // ex. if searching up, it can be assumed the blocks to the
    // sides have already been searched, so don't try to test them
    // Each number here corresponds to the index of a relativeDirection in the
    // above array.
    public static final int[][] searchDirs = {
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26},
        {1, 7, 8, 9, 10, 19, 20, 21, 22},
        {2, 7, 11, 12, 15, 19, 20, 25, 26},
        {3, 8, 11, 13, 16, 19, 21, 23, 25},
        {4, 9, 12, 14, 17, 20, 22, 24, 26},
        {5, 10, 13, 14, 18, 21, 22, 23, 24},
        {6, 15, 16, 17, 18, 23, 24, 25, 26},
        {1, 2, 7, 8, 9, 10, 11, 12, 15, 19, 20, 21, 22, 25, 26},
        {1, 3, 7, 8, 9, 10, 11, 13, 16, 19, 20, 21, 22, 23, 25},
        {1, 4, 7, 8, 9, 10, 12, 14, 17, 19, 20, 21, 22, 24, 26},
        {1, 5, 7, 8, 9, 10, 13, 14, 18, 19, 20, 21, 22, 23, 24},
        {2, 3, 7, 8, 11, 12, 13, 15, 16, 19, 20, 21, 23, 25, 26},
        {2, 4, 7, 9, 11, 12, 14, 15, 17, 19, 20, 22, 24, 25, 26},
        {3, 5, 8, 10, 11, 13, 14, 16, 18, 19, 21, 22, 23, 24, 25},
        {4, 5, 9, 10, 12, 13, 14, 17, 18, 20, 21, 22, 23, 24, 26},
        {2, 6, 7, 11, 12, 15, 16, 17, 18, 19, 20, 23, 24, 25, 26},
        {3, 6, 8, 11, 13, 15, 16, 17, 18, 19, 21, 23, 24, 25, 26},
        {4, 6, 9, 12, 14, 15, 16, 17, 18, 20, 22, 23, 24, 25, 26},
        {5, 6, 10, 13, 14, 15, 16, 17, 18, 21, 22, 23, 24, 25, 26},
        {1, 2, 3, 7, 8, 9, 10, 11, 12, 13, 15, 16, 19, 20, 21, 22, 23, 25, 26},
        {1, 2, 4, 7, 8, 9, 10, 11, 12, 14, 15, 17, 19, 20, 21, 22, 24, 25, 26},
        {1, 3, 5, 7, 8, 9, 10, 11, 13, 14, 16, 18, 19, 20, 21, 22, 23, 24, 25},
        {1, 4, 5, 7, 8, 9, 10, 12, 13, 14, 17, 18, 19, 20, 21, 22, 23, 24, 26},
        {3, 5, 6, 8, 10, 11, 13, 14, 15, 16, 17, 18, 19, 21, 22, 23, 24, 25, 26},
        {4, 5, 6, 9, 10, 12, 13, 14, 15, 16, 17, 18, 20, 21, 22, 23, 24, 25, 26},
        {2, 3, 6, 7, 8, 11, 12, 13, 15, 16, 17, 18, 19, 20, 21, 23, 24, 25, 26},
        {2, 4, 6, 7, 9, 11, 12, 14, 15, 16, 17, 18, 19, 20, 22, 23, 24, 25, 26}
    };
    
    /**
     * Does a floodfill using the given (pre-populated) queue and taking the
     * given actions for each block reached. Logic to add to the queue or store
     * any found results must be in <code>visitAction</code>.
     * @param queue queue used to continue flooding, also contains the starting
     * point for the flood fill
     * @param visitAction action to take for every block visited
     * @param minX
     * @param minY
     * @param minZ
     * @param maxX
     * @param maxY
     * @param maxZ
     */
    
    private void flood(Queue<FloodPos> queue, BiConsumer<FloodPos, FloodPos> visitAction,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        FloodPos[][][] shadow = new FloodPos[maxX - minX + 1][maxY - minY + 1][maxZ - minZ + 1];
        // add starting points to shadow matrix
        for (FloodPos fp : queue) {
            Block b = fp.b;
            shadow[b.getX() - minX][b.getY() - minY][b.getZ() - minZ] = fp;
        }
        while (!queue.isEmpty()) {
            FloodPos cur = queue.poll();
            Block b = cur.b;
            // the array of direction indices we should use to continue flooding
            // given the direction used to flood to the current position
            int[] nextDirs = searchDirs[cur.dir];
            for (int dir = 0; dir < nextDirs.length; dir++) {
                // relative direction of next block to flood to
                int[] relDir = relativeDirs[nextDirs[dir]];
                int nextX = b.getX() + relDir[0];
                int nextY = b.getY() + relDir[1];
                int nextZ = b.getZ() + relDir[2];
                int nextShadowX = nextX - minX;
                int nextShadowY = nextY - minY;
                int nextShadowZ = nextZ - minZ;
                // shadow matrix sanity check
                if (nextX < minX | nextX > maxX
                        | nextY < minY | nextY > maxY
                        | nextZ < minZ | nextZ > maxZ) {
                    continue;
                }
                FloodPos shadowPos = shadow[nextShadowX][nextShadowY][nextShadowZ];
                // using math method rather than locations cuz MICROOPTIMIZATION
                double newDist = square(nextX - cur.start.getX()) + square(nextY - cur.start.getY()) + square(nextZ - cur.start.getZ());
                if (shadowPos != null) {
                    if (shadowPos.dist > newDist) {
                        // >this was included due to some blocks being left out
                        // during debugging, but it was only noticeable in the
                        // outermost leaves after flooding around a few bends
                        // >including it causes some blocks to be accepted by
                        // floodAction TWICE (or more?), so storing any of these
                        // in something besides a set/map may not be preferable
                        // as it could include duplicates of the same block
                        shadowPos.dist = newDist;
                        shadowPos.start = cur.start;
                        visitAction.accept(cur, shadowPos);
                    }
                    continue;
                }
                FloodPos next = new FloodPos(b.getRelative(relDir[0], relDir[1], relDir[2]), cur.start, nextDirs[dir]);
                next.dist = newDist;
                shadow[nextShadowX][nextShadowY][nextShadowZ] = next;
                visitAction.accept(cur, next);
            }
        }
    }
    
    private int square(int x) {
        return x * x;
    }
    
    private long hashCoords(int x, int y, int z) {
        return ((long)x & 0xFFFFFFF) << 36 | ((long)y & 0xFF) << 28 | ((long)z & 0xFFFFFFF);
    }
    
    private static class FloodPos {
        public final Block b;
        public Block start;
        public final int dir;
        public double dist;
        
        public FloodPos(Block b, Block start, int dir) {
            this.b = b;
            this.start = start;
            this.dir = dir;
        }
    }
}