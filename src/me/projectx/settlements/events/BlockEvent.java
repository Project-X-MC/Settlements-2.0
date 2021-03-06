package me.projectx.settlements.events;

import me.projectx.settlements.enums.ClaimType;
import me.projectx.settlements.enums.MessageType;
import me.projectx.settlements.managers.ChunkManager;
import me.projectx.settlements.managers.PlayerManager;
import me.projectx.settlements.managers.SettlementManager;
import me.projectx.settlements.models.ClaimedChunk;
import me.projectx.settlements.models.Settlement;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockEvent implements Listener{

	@EventHandler
	public void onBreak(final BlockBreakEvent e){
		Chunk c = e.getBlock().getLocation().getChunk();
		int x = c.getX();
		int z = c.getZ();

		if (ChunkManager.getManager().isClaimed(x, z, c.getWorld())){
			ClaimedChunk chunk = ChunkManager.getManager().getChunk(x, z, c.getWorld());
			if (chunk.getType() == ClaimType.SAFEZONE && !PlayerManager.getInstance().hasAdminOverride(e.getPlayer())){
				e.setCancelled(true);
				e.getPlayer().sendMessage(MessageType.PREFIX.getMsg() + ChatColor.GOLD + "You cannot break blocks in a SafeZone");
				return;
			}
			
			if (chunk.getType() != ClaimType.SAFEZONE){
				Settlement s = SettlementManager.getManager().getPlayerSettlement(e.getPlayer().getUniqueId());
				if (s != null){
					if (chunk.getSettlement().getId() != s.getId()){
						if (!PlayerManager.getInstance().hasAdminOverride(e.getPlayer())){
							//Player p = Bukkit.getServer().getPlayer(chunk.getOwner());
							//if (p.getUniqueId() != e.getPlayer().getUniqueId()){
								e.setCancelled(true);
								e.getPlayer().sendMessage(MessageType.PREFIX.getMsg() + ChatColor.RED + "You are not allowed to build here!");;
							//}
						}
					}
				}else{
					if (chunk.getType() == ClaimType.NORMAL){
						if (!PlayerManager.getInstance().hasAdminOverride(e.getPlayer())){
							e.setCancelled(true);
							e.getPlayer().sendMessage(MessageType.PREFIX.getMsg() + ChatColor.RED + "You are not allowed to build here!");
						}
					}
				}
			}
		}
		
		if (e.getBlock().getWorld().getName().equalsIgnoreCase("Varym") && !PlayerManager.getInstance().hasAdminOverride(e.getPlayer()))
			e.setCancelled(true);
	}

	@EventHandler
	public void onPlace(final BlockPlaceEvent e){
		Chunk c = e.getBlock().getLocation().getChunk();
		int x = c.getX();
		int z = c.getZ();

		if (ChunkManager.getManager().isClaimed(x, z, c.getWorld())){
			ClaimedChunk chunk = ChunkManager.getManager().getChunk(x, z, c.getWorld());
			if (chunk.getType() == ClaimType.SAFEZONE && !e.getPlayer().hasPermission("settlements.safezone.build")){
				e.setCancelled(true);
				e.getPlayer().sendMessage(MessageType.PREFIX.getMsg() + ChatColor.GOLD + "You cannot place blocks in a SafeZone");
				return;
			}

			if (chunk.getType() != ClaimType.SAFEZONE){
				Settlement s = SettlementManager.getManager().getPlayerSettlement(e.getPlayer().getUniqueId());
				if (s != null){
					if (!s.isInWar()){
						if (chunk.getSettlement().getId() != s.getId()){
							if (!PlayerManager.getInstance().hasAdminOverride(e.getPlayer())){
							//Player p = Bukkit.getServer().getPlayer(chunk.getOwner());
							//if (p.getUniqueId() != e.getPlayer().getUniqueId()){
								e.setCancelled(true);
								e.getPlayer().sendMessage(MessageType.PREFIX.getMsg() + ChatColor.RED + "You are not allowed to build here!");
							}
						}
					}
				}else{
					if (chunk.getType() == ClaimType.NORMAL){
						if (!PlayerManager.getInstance().hasAdminOverride(e.getPlayer())){
							e.setCancelled(true);
							e.getPlayer().sendMessage(MessageType.PREFIX.getMsg() + ChatColor.RED + "You are not allowed to build here!");
						}
					}
				}
			}	
		}
		
		if (e.getBlock().getWorld().getName().equalsIgnoreCase("Varym") && !PlayerManager.getInstance().hasAdminOverride(e.getPlayer()))
				e.setCancelled(true);
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e){
		if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK){
			if (e.getClickedBlock() != null){
				Chunk c = e.getClickedBlock().getLocation().getChunk();
				int x = c.getX();
				int z = c.getZ();

				if (ChunkManager.getManager().isClaimed(x, z, c.getWorld())){
					ClaimedChunk chunk = ChunkManager.getManager().getChunk(x, z, c.getWorld());
					//Player p = Bukkit.getServer().getPlayer(chunk.getOwner());

					Settlement s = SettlementManager.getManager().getPlayerSettlement(e.getPlayer().getUniqueId());
					if (s != null){
						//if (!SettlementManager.getManager().getPlayerSettlement(e.getPlayer().getName()).isInWar()){
							//if (!(p.getUniqueId() == e.getPlayer().getUniqueId())){
						/*if (chunk.getType() == ClaimType.SAFEZONE){
							e.setCancelled(true);
							e.getPlayer().sendMessage(MessageType.PREFIX.getMsg() + ChatColor.RED + "You cannot interact with blocks in this territory!");
						}*/
							
						if (chunk.getType() == ClaimType.NORMAL){
							if (!PlayerManager.getInstance().hasAdminOverride(e.getPlayer())){
								if (chunk.getSettlement().getId() != s.getId()){
									e.setCancelled(true);
									e.getPlayer().sendMessage(MessageType.PREFIX.getMsg() + ChatColor.RED + "You cannot interact with blocks in this territory");
								}
							}
						}
					}else{
						if (chunk.getType() == ClaimType.NORMAL){
							if (!PlayerManager.getInstance().hasAdminOverride(e.getPlayer())){
								e.setCancelled(true);
								e.getPlayer().sendMessage(MessageType.PREFIX.getMsg() + ChatColor.RED + "You can't interact with blocks in this territory");
							}
						}
					}
				}
			}
		}
	}
}
