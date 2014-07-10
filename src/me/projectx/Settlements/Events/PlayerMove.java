package me.projectx.Settlements.Events;

import java.sql.SQLException;

import me.projectx.Economy.Main;
import me.projectx.Settlements.Managers.ChunkManager;
import me.projectx.Settlements.Managers.ChunkManagerTEST;
import me.projectx.Settlements.Managers.MapManager;
import me.projectx.Settlements.Models.ClaimedChunkTEST;
import me.projectx.Settlements.Models.Settlement;
import me.projectx.Settlements.Utils.ClaimType;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMove implements Listener{

	@EventHandler
	public void onMove(final PlayerMoveEvent e){
		if (e.getFrom().getChunk() != e.getTo().getChunk()){
			ClaimedChunkTEST c = ChunkManagerTEST.getManager().getChunk(e.getFrom().getChunk().getX(), e.getFrom().getChunk().getZ());
			ClaimedChunkTEST d = ChunkManagerTEST.getManager().getChunk(e.getTo().getChunk().getX(), e.getTo().getChunk().getZ());
			if(c==null&&d==null){
			}else if(c!=null&&d==null){ //Leaving b to non claimed
				e.getPlayer().sendMessage(ChatColor.GREEN + "~Wilderness");
			}else if(d!=null&&c==null){ //Entering a from unclaimed
				Settlement a = d.getSettlement();
				if (d.getType() == ClaimType.NORMAL) {
					e.getPlayer().sendMessage(ChatColor.AQUA + a.getName() + ChatColor.RED + " ~ " + ChatColor.GRAY + a.getDescription());
				} else if (d.getType() == ClaimType.SAFEZONE) {
					e.getPlayer().sendMessage(ChatColor.GOLD + "SafeZone ~ Safe from PvP and Monsters");
				}
			}else if(c!=null&&d!=null){ //Entering one claim to another claim
				Settlement a = c.getSettlement();
				Settlement b = d.getSettlement();

				if (c.getType() == ClaimType.SAFEZONE && d.getType() != ClaimType.SAFEZONE){
					e.getPlayer().sendMessage(ChatColor.GREEN + "Leaving " + ChatColor.GOLD + "SafeZone" + ChatColor.GREEN + ", entering " + b.getName());
				}

				if (a != null && b!= null){
					if(!a.equals(b)){ //NPE
						if (d.getType() == ClaimType.NORMAL) {
							e.getPlayer().sendMessage(ChatColor.GREEN + "Leaving " + a.getName() + " entering " + b.getName());
						} 
					}
				}
				else if (c.getType() == ClaimType.NORMAL && d.getType() == ClaimType.SAFEZONE) {
					e.getPlayer().sendMessage(ChatColor.GREEN + "Entering " + ChatColor.GOLD + "SafeZone");
				}
			} //add for safezone & battleground
			Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getInstance(), new Runnable(){

				@Override
				public void run() {
					MapManager.getInstance().remove(e.getPlayer());
				}
				
			},1);
		}


		if (ChunkManager.getInstance().isAutoClaiming(e.getPlayer())){
			try {
				if (!ChunkManager.getInstance().isClaimed(e.getPlayer().getLocation().getChunk().getX(), e.getPlayer().getLocation().getChunk().getZ()))
				{
					ChunkManager.getInstance().claimChunk(e.getPlayer(), ClaimType.NORMAL); //temp type
				}
			} catch(SQLException ex) {
				ex.printStackTrace();
			}
		}
	}
}
