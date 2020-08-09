package com.civrealms.mobcontrol;

import com.civrealms.crgmain.CivRealmsGlue;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public class MobListener implements Listener {
    private CivRealmsGlue plugin;
        
    public MobListener(CivRealmsGlue plugin) {
		this.plugin = plugin;
	}
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event){
        for (Entity e : event.getChunk().getEntities()){
            //load into our own memory if not already, also check livingentity
            //just let minecraft handle the chunk stuff otherwise, this is only for supplementing our knowledge
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onSpawn(CreatureSpawnEvent event){
        
        Location el = event.getLocation();
		CreatureSpawnEvent.SpawnReason sr = event.getSpawnReason();
		EntityType et = event.getEntityType();
        
        if (et == EntityType.CREEPER
                || et == EntityType.ENDERMAN
                || et == EntityType.SKELETON
                || et == EntityType.SPIDER
                || et == EntityType.CAVE_SPIDER
                || et == EntityType.ZOMBIE
                || et == EntityType.WOLF){
            //^ Replace with normal enemy mobs we want: wolves, spiders
        } else if (et == EntityType.SHEEP
                || et == EntityType.COW
                || et == EntityType.HORSE
                || et == EntityType.DONKEY
                || et == EntityType.MULE
                || et == EntityType.CHICKEN
                || et == EntityType.OCELOT
                || et == EntityType.LLAMA
                || et == EntityType.PIG
                || et == EntityType.MUSHROOM_COW
                || et == EntityType.RABBIT){
            
            //playing around with adjusting pig speed as a fture breedable thing lol:
            //if (event.getEntityType() == EntityType.PIG){
            //    ((Pig)event.getEntity()).getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(((Pig)event.getEntity()).getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() + 0.5);
            //}   
    
            //^ ones we want people to breed only
            //so check for spawn egg or food breeding only, cancel the rest
        } else if (et == EntityType.BAT
                || et == EntityType.PARROT
                || et == EntityType.POLAR_BEAR
                || et == EntityType.SQUID){
            //^ unbreedable ones but we want them around sometimes
            //allow to spawn naturally but with imited density, frequency, and monitoring
        } else if (et == EntityType.SNOWMAN
                || et == EntityType.IRON_GOLEM){
            //^ allow construction of them only and track separately
        } else {
            //cancel all others, withers villagers, blaze, zombie horses, husks, witches, ghasts blah blah
        }
            
        //########### comment out until ready, or will kill server:
        //event.getEntity().setRemoveWhenFarAway(false);
        
        //if lobby, cancel all except horsies
        //breeding reason, we want to handle same as we do now -- also needs entitybreedevent I think? maybe not, but that might avoid wasting wheat
        //mostly otherwise just cancel though in most cases and replace with own
    }
    
    //### something for new maps to seed properly
    //### need a loop (sep class) for culling and crowding
    //### also direct collision detect damage

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onMobDeath(EntityDeathEvent event){
        if (event.getEntity() instanceof LivingEntity && !(event.getEntity() instanceof Player)){
            //check reason and try to avoid bullshit ones like system removal or whatever
            //also drops
        }
    }
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onMobDamage(EntityDamageEvent event){
        if (event.getEntity() instanceof LivingEntity && !(event.getEntity() instanceof Player)){
            //record a short term memory of last damage type done, so that death can know to cancel or not or whate to do above.
        }
    }
    
    //Despawn:
    //entity.setRemoveWhenFarAway() make it never happen naturally
    //instead of an event to cancel
    
}