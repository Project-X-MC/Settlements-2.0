package me.projectx.settlements.managers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.projectx.settlements.Main;
import me.projectx.settlements.enums.ClaimResult;
import me.projectx.settlements.enums.ClaimType;
import me.projectx.settlements.enums.MessageType;
import me.projectx.settlements.models.ClaimedChunk;
import me.projectx.settlements.models.Settlement;
import me.projectx.settlements.utils.DatabaseUtils;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ChunkManager {
	
	public Map<String, List<ClaimedChunk>> setClaims = new HashMap<String, List<ClaimedChunk>>();
	public List<ClaimedChunk> claimedChunks = new ArrayList<ClaimedChunk>();
	public Map<String, ClaimType> autoClaim = new HashMap<String, ClaimType>();
	private final int BASE_CHUNK_COST = 50;
	private static ChunkManager cm = new ChunkManager();
	private PreparedStatement claim, unclaim, select_chunks;
	
	private ChunkManager(){
		try{
			claim = DatabaseUtils.getConnection().prepareStatement("INSERT INTO chunks(x, z, player, settlement, world, type) VALUES(?,?,?,?,?,?);");
			unclaim = DatabaseUtils.getConnection().prepareStatement("DELETE FROM chunks WHERE x=? AND z=?;");
			select_chunks = DatabaseUtils.getConnection().prepareStatement("SELECT * FROM chunks WHERE x=? AND z=?;");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static ChunkManager getManager(){
		return cm;
	}
	
	private synchronized int claimChunk(final UUID owner, final World world, final int x, final int z, final ClaimType type){
		boolean claimed = isClaimed(x, z, world);
		if (owner != null){
			final Settlement s = SettlementManager.getManager().getPlayerSettlement(owner);
			if (s != null){
				if (!claimed){
					final int bonus = setClaims.get(s.getName()).size();
					if (s.getBalance() >= (bonus + BASE_CHUNK_COST)){
						new BukkitRunnable(){
							public void run(){
								ClaimedChunk cc = new ClaimedChunk(x, z, owner, s.getId(), world.getName(), type);
								claimedChunks.add(cc);
								
								/*
								 * Charge the base chunk cost and tack on an additional amount for the amount of chunks they already own.
								 * More chunks = higher cost
								 */
								
								EconomyManager.getManager().withdrawFromSettlement(s, BASE_CHUNK_COST + bonus);
								
								if (!setClaims.containsKey(s.getName())){
									List<ClaimedChunk> claims = new ArrayList<ClaimedChunk>();
									claims.add(cc);
									setClaims.put(s.getName(), claims);
								}else{
									setClaims.get(s.getName()).add(cc);
								}
								
								try {
									claim.setInt(1, x);
									claim.setInt(2, z);
									claim.setString(3, owner.toString());
									claim.setLong(4, s.getId());
									claim.setString(5, world.getName());
									claim.setString(6, type.name());
									DatabaseUtils.queryOut(claim);
								} catch(SQLException e) {
									e.printStackTrace();
								}
							}
						}.runTaskAsynchronously(Main.getInstance());
						return 2;
					}else{
						return 3;
					}
				}else
					return 1;	
			}else{
				return 0;
			}
		}else{
			if (!claimed){
				new BukkitRunnable(){
					public void run(){
						ClaimedChunk cc = new ClaimedChunk(x, z, owner, -1, world.getName(), type);
						claimedChunks.add(cc);
						if (!setClaims.containsKey(null)){
							List<ClaimedChunk> claims = new ArrayList<ClaimedChunk>();
							claims.add(cc);
							setClaims.put(null, claims);
						}else{
							setClaims.get(null).add(cc);
						}
						
						try {
							claim.setInt(1, x);
							claim.setInt(2, z);
							claim.setString(3, null);
							claim.setInt(4, -1);
							claim.setString(5, world.getName());
							claim.setString(6, type.name());
							DatabaseUtils.queryOut(claim);
						} catch(SQLException e) {
							e.printStackTrace();
						}
					}
				}.runTaskAsynchronously(Main.getInstance());	
				return 2;
			}else{
				return 1;
			}
		}	
	}

	private ClaimResult unclaimChunk(final String player, final int x, final int z, World world, boolean admin){
		if (isClaimed(x, z, world)){
			ClaimedChunk cc = getChunk(x, z, world);
			switch(cc.getType()){
				case NORMAL:
					if (!admin){
						Settlement s = SettlementManager.getManager().getPlayerSettlement(player);
						if (s != null){
							if (cc.getSettlement().getName().equals(s.getName())){
								if (setClaims.containsKey(s.getName())){
									List<ClaimedChunk> l = setClaims.get(s.getName());
									l.remove(cc);
									claimedChunks.remove(cc);
									try {
										unclaim.setInt(1, cc.getX());
										unclaim.setInt(2, cc.getZ());
										DatabaseUtils.queryOut(unclaim);
									} catch(SQLException e) {
										e.printStackTrace();
									}
									cc = null; //maybe this will make things work better
									return ClaimResult.UNCLAIM_SUCESS;
								}else{
									return ClaimResult.UNCLAIM_NO_CLAIMS;
								}
							}else{
								return ClaimResult.UNCLAIM_NOT_OWNER;
							}
						}else{
							return ClaimResult.NOT_IN_SETTLEMENT;
						}
					}else{
						List<ClaimedChunk> list = setClaims.get(cc.getSettlement().getName());
						if (list.contains(cc)){
							list.remove(cc);
							claimedChunks.remove(cc);
							try {
								unclaim.setInt(1, cc.getX());
								unclaim.setInt(2, cc.getZ());
								DatabaseUtils.queryOut(unclaim);
							} catch(SQLException e) {
								e.printStackTrace();
							}
							cc = null;
							return ClaimResult.UNCLAIM_SUCESS;
						}
					}
				case SAFEZONE:
					if (admin){
						List<ClaimedChunk> list = setClaims.get(null);
						System.out.println(list.size());
						if (list.contains(cc)){
							list.remove(cc);
							claimedChunks.remove(cc);
							try {
								unclaim.setInt(1, cc.getX());
								unclaim.setInt(2, cc.getZ());
								DatabaseUtils.queryOut(unclaim);
							} catch(SQLException e) {
								e.printStackTrace();
							}
							return ClaimResult.UNCLAIM_SUCESS;
						}else{
							return ClaimResult.UNCLAIM_ERROR;
						}
					}else{
						return ClaimResult.UNCLAIM_NOT_ADMIN;
					}
				default:
					System.out.println("Unsupported ClaimType");
					break;
			}	
		}
		return ClaimResult.UNCLAIM_FAIL;
	}

	public boolean isClaimed(final int x, final int z, final World world){
		for (ClaimedChunk cc : claimedChunks){
			if (cc.getX() == x && cc.getZ() == z && cc.world.equals(world.getName())){
				return true;
			}
		}
		return false;
	}
	
	public ClaimedChunk getChunk(int x, int z, World world){
		for (ClaimedChunk cc : claimedChunks){
			if (cc.getX() == x && cc.getZ() == z && cc.world.equals(world.getName())){
				return cc;
			}
		}
		return null;
	}
	
	public List<ClaimedChunk> getClaims(Settlement s){
		return setClaims.get(s.getName());
	}
	
	public void loadChunks(){
		new BukkitRunnable(){
			public void run() {
				try {
					verifyClaims();
					ResultSet result = DatabaseUtils.queryIn(select_chunks);
					while (result.next()){
						int x = result.getInt("x");
						int z = result.getInt("z");
						UUID player = null;
						if (!result.getString("player").equals("null")){
							player = UUID.fromString(result.getString("player"));
						}
						long setid = result.getLong("settlement");
						String w = result.getString("world");
						Settlement s = SettlementManager.getManager().getSettlement(setid);
						ClaimedChunk cc = new ClaimedChunk(x, z, player, setid, w, ClaimType.valueOf(result.getString("type")));
						claimedChunks.add(cc);
						if (s != null){
							setClaims.get(s.getName()).add(cc);
						}else{
							if (setClaims.containsKey(null)){
								setClaims.get(null).add(cc);
							}else{
								List<ClaimedChunk> list = new ArrayList<ClaimedChunk>();
								list.add(cc);
								setClaims.put(null, list);
							}
						}
					}
					this.cancel();
				} catch(SQLException e) {
					e.printStackTrace();
				}
			}
		}.runTaskAsynchronously(Main.getInstance());
	}
	
	public void claim(Player player, ClaimType ct){	
		int i = 0;
		Chunk c = player.getLocation().getChunk();
		
		switch(ct){
			case NORMAL:
				i = claimChunk(player.getUniqueId(), player.getWorld(), c.getX(), c.getZ(), ClaimType.NORMAL);
				break;
			case SAFEZONE:
				i = claimChunk(null, player.getWorld(), c.getX(), c.getZ(), ClaimType.SAFEZONE);
				break;
			default:
				break;
		}	
		
		switch(i){
			case 3:
				player.sendMessage(MessageType.PREFIX.getMsg() + ChatColor.RED + "Your Settlement doesn't have enough funds to claim this chunk!");
				break;
			case 2:
				if (ct == ClaimType.SAFEZONE)
					player.sendMessage(MessageType.CHUNK_CLAIM_SAFEZONE.getMsg());
				else if (ct == ClaimType.NORMAL)
					player.sendMessage(MessageType.CHUNK_CLAIM_SUCCESS.getMsg());
				break;
			case 1:
				player.sendMessage(MessageType.CHUNK_CLAIM_OWNED.getMsg());
				break;
			case 0:
				player.sendMessage(MessageType.NOT_IN_SETTLEMENT.getMsg());
				break;
		}

	}
	
	public void unclaim(Player player, int x, int z, World world, boolean admin){
		ClaimResult ct = unclaimChunk(player.getName(), x, z, world, admin);
		switch(ct){
			case NOT_IN_SETTLEMENT:
				player.sendMessage(MessageType.NOT_IN_SETTLEMENT.getMsg());
				break;
			case UNCLAIM_FAIL:
				player.sendMessage(MessageType.CHUNK_UNCLAIM_FAIL.getMsg());
				break;
			case UNCLAIM_NOT_ADMIN:
				player.sendMessage(MessageType.CHUNK_NOT_ADMIN.getMsg());
				break;
			case UNCLAIM_NOT_OWNER:
				player.sendMessage(MessageType.CHUNK_UNCLAIM_FAIL.getMsg());
				break;
			case UNCLAIM_NO_CLAIMS:
				player.sendMessage(MessageType.SETTLEMENT_NO_CLAIMS.getMsg());
				break;
			case UNCLAIM_SUCESS:
				player.sendMessage(MessageType.CHUNK_UNCLAIM_SUCCESS.getMsg());
				break;
			case UNCLAIM_ERROR:
				player.sendMessage(MessageType.CHUNK_UNCLAIM_ERROR.getMsg());
			default:
				break;
		}
	}
	
	public void setAutoClaiming(Player p, ClaimType ct){
		if (autoClaim.containsKey(p.getName())) {
			autoClaim.remove(p.getName());
			switch(ct){
				case NORMAL:
					p.sendMessage(MessageType.CHUNK_AUTOCLAIM_NORMAL_END.getMsg());
					break;
				case SAFEZONE:
					p.sendMessage(MessageType.CHUNK_AUTOCLAIM_SZONE_END.getMsg());
					break;
				default:
					break;
			}
		} else {
			autoClaim.put(p.getName(), ct);
			switch(ct){
			case NORMAL:
				p.sendMessage(MessageType.CHUNK_AUTOCLAIM_NORMAL_START.getMsg());
				break;
			case SAFEZONE:
				p.sendMessage(MessageType.CHUNK_AUTOCLAIM_SZONE_START.getMsg());
				break;
			default:
				break;
			}
		}
	}

	public boolean isAutoClaiming(Player p){
		return autoClaim.containsKey(p.getName());
	}
	
	public ClaimType getAutoclaimType(Player player){
		return autoClaim.get(player.getName());
	}
	
	/**
	 * Make sure that every Settlement is in the setClaims map so no NPEs are thrown
	 */
	public synchronized void verifyClaims(){
		new BukkitRunnable(){
			public void run(){
				System.out.println("[Settlements] Verifying chunk claims...");
				for (Settlement s : SettlementManager.getManager().settlements){
					if (!setClaims.containsKey(s.getName())){
						List<ClaimedChunk> cc = new ArrayList<ClaimedChunk>();
						ChunkManager.getManager().setClaims.put(s.getName(), cc);
						System.out.println("[Settlements] " + s.getName() + "'s claims were not registered. Doing it now...");
					}
				}
				System.out.println("[Settlements] Done!");
				this.cancel();
			}
		}.runTaskAsynchronously(Main.getInstance());
	}
	
	public void unregisterClaims(){
		new Thread(){
			public void run(){
				for (ClaimedChunk cc : claimedChunks){
					setClaims.get(cc.getSettlement().getName()).clear();
					claimedChunks.remove(cc);
					cc = null;
				}
				this.interrupt();
			}
		}.start();
	}
}
