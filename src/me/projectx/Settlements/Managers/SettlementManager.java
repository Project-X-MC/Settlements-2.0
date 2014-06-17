package me.projectx.Settlements.Managers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import me.projectx.Economy.API.EconomyAPI;
import me.projectx.Settlements.Models.ClaimedChunk;
import me.projectx.Settlements.Models.Settlement;
import me.projectx.Settlements.Utils.DatabaseUtils;
import me.projectx.Settlements.Utils.MessageType;
import me.projectx.Settlements.Utils.PlayerUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class SettlementManager extends Thread {

	public ArrayList<Settlement> settlements = new ArrayList<Settlement>();
	private HashMap<String, Settlement> invitedPlayers = new HashMap<String, Settlement>();
	private static SettlementManager sm = new SettlementManager();

	/**
	 * Get an instance of the SettlementManager class
	 * 
	 * @return SettlementManager class instance
	 */
	public static SettlementManager getManager(){
		return sm;
	}

	/**
	 * Load settlements (Call onEnable)
	 * 
	 * @return Returns true if successful, false otherwise
	 * @throws SQLException 
	 */
	public void loadSettlmentsFromDB() throws SQLException{
		new Thread() {
			@Override
			public void run() {
				ResultSet result = null;
				try {
					result = DatabaseUtils.queryIn("SELECT * FROM settlements;");
					while (result.next()){
						String name = result.getString("name");
						Settlement set = new Settlement(name);
						set.setLeader(result.getString(("leader")));
						set.setDescription(result.getString("description"));
						set.setOfficer(result.getString("officers"));
						set.giveCitizenship(result.getString("citizens"));
						set.setBalance(result.getDouble("balance"));
						settlements.add(set);
					}
				} catch(SQLException e) {e.printStackTrace();}
			}
		}.start();
	}

	/**
	 * Save settlements (Call onDisable)
	 * 
	 * @return Returns true if successful, false otherwise
	 * @throws SQLException 
	 */
	public void saveSettlements() throws SQLException{
		new Thread() {
			@Override
			public void run() {
				for (Settlement set : settlements){
					String tempName = set.getName();
					long tempId = set.getId();
					String tempLeader = set.getLeader().toString();
					String tempDesc = set.getDescription();
					String tempCits = set.getCitizens().toString(); 
					String tempOffs = set.getOfficers().toString(); 
					try {
						DatabaseUtils.queryOut("UPDATE settlements"
								+ "SET name='"+ tempName + "', leader='"+ tempLeader 
								+ "', description='"+ tempDesc +"', citizens='"+ tempCits 
								+ "', officers='"+ tempOffs +"'WHERE id='" + tempId + "';");
					} catch(SQLException e) {e.printStackTrace();}
				}
			}
		}.start();
	}

	/**
	 * Get the settlement of a player
	 * 
	 * @param uuid : The UUID of the player to get the settlement for. 
	 * <p><b> UUID-based lookup is faster than name-based lookup! </b>
	 * @return The player's settlement. If they are not a member of a settlement, this will return null
	 */
	public Settlement getPlayerSettlement(UUID uuid){
		for (Settlement s : settlements){
			if (s.hasMember(uuid)) {
				return s;
			}
		}
		return null;
	}
	
	/**
	 * Get the settlement of a player
	 * 
	 * @param name : The name of the player to get the settlement for
	 * <p><b> This is slower than UUID-based lookup! </b>
	 * @return The player's settlement. If they are not a member of a settlement, this will return null
	 */
	public Settlement getPlayerSettlement(String name){
		UUID id = Bukkit.getPlayer(name).getUniqueId();
		for (Settlement s : settlements){
			if (s.hasMember(id)){
				return s;
			}
		}
		return null;
	}

	/**
	 * Determine if a settlement exists
	 * 
	 * @param name : The name of the settlement to check
	 * @return True if the settlement exists
	 */
	public boolean settlementExists(String name){
		for (Settlement s : settlements){
			if (s.getName().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get a settlement by name
	 * 
	 * @param name : The name of the settlement to get
	 * @return The designated settlement. Returns null if it doesn't exist
	 */
	public Settlement getSettlement(String name){
		for (Settlement s : settlements){
			if (s.getName().equalsIgnoreCase(name)) {
				return s;
			}
		}	
		return null;
	}

	/**
	 * Get a settlement by id
	 * 
	 * @param id : The id of the settlement to get
	 * @return The designated settlement. Returns null if it doesn't exist
	 */
	public Settlement getSettlement(long id){
		for (Settlement s : settlements){
			if (s.getId() == id) {
				return s;
			}
		}	
		return null;
	}

	/**
	 * Create a new settlement. The settlement will only get created if the settlement 
	 * doesn't already exist and if the sender isn't a member of a different settlement
	 * 
	 * @param name : The name of the new settlement
	 * @param sender : Who issued the creation of the settlement
	 * @throws SQLException 
	 */
	public void createSettlement(String name, CommandSender sender) throws SQLException{
		if (!settlementExists(name)){
			Player p = (Player)sender;
			if (getPlayerSettlement(sender.getName()) == null){
				final Settlement s = new Settlement(name);
				s.setLeader(p.getUniqueId());
				settlements.add(s);
				DatabaseUtils.queryOut("INSERT INTO settlements (id, name, leader)"
						+ "VALUES ('" + s.getId() + "','" + s.getName() + "','" + s.getLeader().toString() + "');");
				sender.sendMessage(MessageType.PREFIX.getMsg() + ChatColor.GRAY + "Successfully created " + ChatColor.AQUA + s.getName());
				sender.sendMessage(ChatColor.GRAY + "You can now set a description by doing " + ChatColor.AQUA + "/s desc <description>");
				sender.sendMessage(ChatColor.GRAY + "For more things you can do, type " + ChatColor.AQUA + "/s");
			} else {
				sender.sendMessage(MessageType.CREATE_IN_SETTLEMENT.getMsg());
			}
		} else {
			sender.sendMessage(MessageType.SETTLEMENT_EXISTS.getMsg());
		}
	}

	/**
	 * Delete a settlement. Only works if the settlement exists and the sender is the leader of the settlement
	 * 
	 * @param name : The name of the settlement to delete
	 * @param sender : Who issued the deletion of the settlement
	 * @throws SQLException 
	 */
	public void deleteSettlement(final CommandSender sender) throws SQLException{
		Player p = (Player) sender;
		final UUID id = p.getUniqueId();
		Settlement s = getPlayerSettlement(id);
		if (s != null){
			if (s.isLeader(id)){
				new Thread() {
					@Override
					public void run() {
						try {
							Settlement s = getPlayerSettlement(id);
							DatabaseUtils.queryOut("DELETE FROM settlements WHERE id=" + s.getId() + ";");
							DatabaseUtils.queryOut("DELETE FROM chunks WHERE settlement=" + s.getId() + ";");

							List<ClaimedChunk> cc = ChunkManager.getInstance().map.get(s);
							if (cc != null){
								ChunkManager.getInstance().map.remove(s);
								ClaimedChunk.instances.remove(cc);
							}

							if (invitedPlayers.containsValue(s)) 
								invitedPlayers.remove(s);

							settlements.remove(s);

							sender.sendMessage(MessageType.PREFIX.getMsg() + ChatColor.GRAY + "Successfully deleted " + ChatColor.AQUA + s.getName());

						} catch(SQLException e) {e.printStackTrace();} catch (Throwable e) {
							e.printStackTrace();
						}	
					}
				}.start();
			} else 
				sender.sendMessage(MessageType.DELETE_NOT_LEADER.getMsg());
		} else 
			sender.sendMessage(MessageType.NOT_IN_SETTLEMENT.getMsg());
	}

	/**
	 * Delete a settlement. Will work for any settlement, even if the sender isn't the leader
	 * 
	 * @param name : The name of the settlement to delete
	 * @param sender : Who issued the deletion of the settlement
	 * @throws SQLException 
	 */
	public void deleteSettlement(final CommandSender sender, final String name) throws SQLException{
		if (settlementExists(name)){
			new Thread() {
				@Override
				public void run() {
					try {
						Settlement s = getSettlement(name);
						DatabaseUtils.queryOut("DELETE FROM settlements WHERE id=" + s.getId() + ";");
						DatabaseUtils.queryOut("DELETE FROM chunks WHERE settlement=" + s.getId() + ";");

						List<ClaimedChunk> cc = ChunkManager.getInstance().map.get(s);
						if (cc != null){
							ClaimedChunk.instances.removeAll(cc);
							ChunkManager.getInstance().map.remove(s);
						}

						if (invitedPlayers.containsValue(s)) 
							invitedPlayers.remove(s);
					
						settlements.remove(s);

						sender.sendMessage(MessageType.PREFIX.getMsg() + ChatColor.GRAY + "Successfully deleted " + ChatColor.AQUA + s.getName());

					} catch(SQLException e) {e.printStackTrace();} catch (Throwable e) {
						e.printStackTrace();
					}	
				}
			}.start();
		}
	}

	/**
	 * Invite a player to join the Settlement. The command sender must be an Officer or higher in the Settlement
	 * 
	 * @param player : The player to invite
	 * @param sender : Who issued the invite
	 */
	public void inviteCitizen(String player, CommandSender sender){
		if (!invitedPlayers.containsKey(player)){
			Settlement s = getPlayerSettlement(sender.getName());
			if (s != null){	
				Player p = Bukkit.getPlayer(player);
				if (!s.hasMember(p.getUniqueId())){
					if (s.isOfficer(p.getUniqueId()) || s.isLeader(p.getUniqueId())){
						invitedPlayers.put(player, getPlayerSettlement(sender.getName()));		
						sender.sendMessage(MessageType.PREFIX.getMsg() + ChatColor.GRAY + 
								"Invited " + ChatColor.AQUA + player + ChatColor.GRAY + " to your Settlement");
						p.sendMessage(MessageType.PREFIX.getMsg() + 
								ChatColor.AQUA + sender.getName() + ChatColor.GRAY + " invited you to join " + 
								ChatColor.AQUA + getPlayerSettlement(sender.getName()).getName()); //throws NPE if player is null/not online
					} else {
						sender.sendMessage(MessageType.INVITE_NOT_RANK.getMsg());
					}
				} else {
					sender.sendMessage(MessageType.PREFIX.getMsg() + ChatColor.AQUA + player + ChatColor.GRAY + " is already in your Settlement!");
				}
			} else {
				sender.sendMessage(MessageType.NOT_IN_SETTLEMENT.getMsg());
			}
		}
	}

	/**
	 * Accept an invite to a Settlement. Will only work if the player has a pending invite
	 * 
	 * @param player : The player who is accepting the invite
	 * @throws SQLException 
	 */
	public void acceptInvite(final String player) throws SQLException{
		Player p = Bukkit.getPlayer(player);
		UUID id = p.getUniqueId();
		if (hasInvite(player)){
			if (getPlayerSettlement(id) == null){
				final Settlement s = invitedPlayers.get(player);
				s.giveCitizenship(id);
				invitedPlayers.remove(player);
				DatabaseUtils.queryOut("UPDATE settlements SET citizens='"+ s.getCitizens().toString() +"' WHERE id='" + s.getId() + "';");
				p.sendMessage(MessageType.PREFIX.getMsg() + 
						ChatColor.GRAY + "Successfully joined " + ChatColor.AQUA + getPlayerSettlement(id).getName()); 
				s.sendSettlementMessage(MessageType.PREFIX.getMsg() + ChatColor.AQUA + player + ChatColor.GRAY + " joined the Settlement!");	
			} else {
				p.sendMessage(MessageType.CURRENTLY_IN_SETTLEMENT.getMsg());
			}
		} else {
			p.sendMessage(MessageType.NO_INVITE.getMsg());
		}
	}

	/**
	 * Determine if a player has a pending invite
	 * 
	 * @param player : The player to check
	 * @return True if the player has an invite
	 */
	public boolean hasInvite(String player){
		return invitedPlayers.containsKey(player);
	}

	/**
	 * Decline an invite to a Settlement. Player must have a valid invite
	 * 
	 * @param player : The player who is declining the invite
	 */
	public void declineInvite(String player){
		Player p = Bukkit.getPlayer(player);
		if (hasInvite(player)){
			p.sendMessage(MessageType.PREFIX.getMsg() + 
					ChatColor.GRAY + "You declined an invite to join " + ChatColor.AQUA + invitedPlayers.get(player).getName());
			invitedPlayers.remove(player);
		} else {
			p.sendMessage(MessageType.NO_INVITE.getMsg());
		}
	}

	/**
	 * Remove a player from a Settlement
	 * 
	 * @param name : The player to remove from the Settlement
	 * @throws SQLException 
	 */
	public void leaveSettlement(String name) throws SQLException{
		final Settlement s = getPlayerSettlement(name);
		Player p = Bukkit.getPlayer(name);
		if (s != null){
			if (!s.isLeader(p.getUniqueId())){
				p.sendMessage(MessageType.PREFIX.getMsg() + ChatColor.GRAY + "Successfully left " + ChatColor.AQUA + s.getName());
				s.sendSettlementMessage(MessageType.PREFIX.getMsg() + ChatColor.AQUA + name + ChatColor.GRAY + " left the Settlement :(");
				s.revokeCitizenship(p.getUniqueId());
				DatabaseUtils.queryOut("UPDATE settlements SET citizens='"+ s.getCitizens().toString() +"' WHERE id=" + s.getId() + ";");
			} else {
				p.sendMessage(MessageType.MUST_APPOINT_NEW_LEADER.getMsg());
			}
		} else {
			p.sendMessage(MessageType.NOT_IN_SETTLEMENT.getMsg());
		}
	}

	/**
	 * Kick a player from a Settlement
	 * 
	 * @param sender : The sender who issued the command
	 * @param name : The name of the player to kick
	 * @throws SQLException 
	 */
	public void kickPlayer(CommandSender sender, String name) throws SQLException{
		final Settlement s = getPlayerSettlement(sender.getName());
		if (s != null){
			Player p = Bukkit.getPlayer(name);
			if (s.hasMember(p.getUniqueId())){
				if (!s.isLeader(p.getUniqueId())){
					s.revokeCitizenship(p.getUniqueId());
					if (p.isOnline()){
						p.sendMessage(MessageType.PREFIX.getMsg() + 
								ChatColor.GRAY + "You have been kicked from " + ChatColor.AQUA + s.getName());
						s.sendSettlementMessage(MessageType.PREFIX.getMsg() + 
								ChatColor.AQUA + sender.getName() + ChatColor.GRAY + " kicked " + 
								ChatColor.AQUA + name + ChatColor.GRAY + " from the Settlement!");
						DatabaseUtils.queryOut("UPDATE settlements SET citizens='"+ s.getCitizens().toString() +"' WHERE id=" + s.getId() + ";");
						DatabaseUtils.queryOut("UPDATE settlements SET officers='" + s.getOfficers().toString() + "' WHERE id=" + s.getId() + ";");
					}
				} else {
					sender.sendMessage(MessageType.KICK_NOT_LEADER.getMsg());
				}
			} else {
				sender.sendMessage(MessageType.PREFIX.getMsg() + ChatColor.DARK_RED + "That player is not in your Settlement!");
			}
		} else {
			sender.sendMessage(MessageType.NOT_IN_SETTLEMENT.getMsg());
		}
	}

	/**
	 * Set the description for a Settlement
	 * 
	 * @param sender : Who issued the command
	 * @param desc : The description for the Settlement
	 * @throws SQLException
	 */
	public void setDescription(CommandSender sender, final String desc) throws SQLException{
		final Settlement s = getPlayerSettlement(sender.getName());
		if (s != null){
			Player p = (Player) sender;
			if (s.isOfficer(p.getUniqueId()) || s.isLeader(p.getUniqueId())){
				s.setDescription(desc);
				sender.sendMessage(MessageType.PREFIX.getMsg() + ChatColor.GRAY + "Set your Settlement's description to " + desc);
				DatabaseUtils.queryOut("UPDATE settlements SET description='" + desc + "' WHERE id=" + s.getId() + ";");
			} else {
				sender.sendMessage(MessageType.DESCRIPTION_NOT_RANK.getMsg());
			}
		} else {
			sender.sendMessage(MessageType.NOT_IN_SETTLEMENT.getMsg());
		}
	}

	/**
	 * Calculate the power for a Settlement
	 * 
	 * @param s : The settlement to calculate the power for
	 */
	public void calculatePower(Settlement s){
		//int citizens = s.getCitizens().size();
		//int officers = s.getOfficers().size();
		for (ClaimedChunk cc : ClaimedChunk.instances){
			if (cc.getSettlement().equals(s)){
				s.setPower( /*((citizens + officers + 1)/*/ChunkManager.getInstance().map.get(s).size()); //will readd members after more testing
			}
		} 
	}
	
	/**
	 * Ally a Settlement
	 * 
	 * @param s1 : The Settlement that is issuing the request
	 * @param s2 : The name of the Settlement that will be added to s1's allies
	 * @return True if successful
	 */
	public boolean allySettlement(Settlement s1, String s2){
		if (settlementExists(s2)){
			Settlement s = getSettlement(s2);
			s1.addAlly(s);
			s1.sendSettlementMessage(MessageType.PREFIX.getMsg() + ChatColor.AQUA + s2 + ChatColor.GRAY + " has been added to your Settlement's alliance!");
			s.sendSettlementMessage(""); //should an alliance message be sent instead?
			return true;
		}
		return false;
	}
	
	/**
	 * Remove a Settlement's ally
	 * 
	 * @param s1 : The Settlement issuing the request
	 * @param s2 : The name of the Settlement that will be removed
	 * @return True if successful
	 */
	public boolean removeAlly(Settlement s1, String s2){
		if (s1.hasAlly(getSettlement(s2))){
			s1.removeAlly(getSettlement(s2));
			return true;
		}
		return false;
	}
	
	/**
	 * Display the members of a Settlement in a GUI
	 * 
	 * @param player : The player who issued the command & will view the GUI
	 * @param settlement : The Settlement who's members will be listed
	 */
	public void displayMembers(Player player, String settlement){
		Settlement s = getSettlement(settlement);
		if (s != null){
			Inventory inv = Bukkit.createInventory(null, getInventorySize(s.memberSize()), ChatColor.BLUE + "Members of " + s.getName());
			ItemStack is = new ItemStack(Material.SKULL_ITEM, 1, (short)3);
			SkullMeta sm = (SkullMeta) is.getItemMeta();
			
			for (int i = 0; i < s.memberSize(); i++){
				Player p = s.getPlayer(i);
				sm.setOwner(p.getName());
				sm.setDisplayName(p.getDisplayName());
				sm.setLore(Arrays.asList(ChatColor.GREEN + "Rank: " + ChatColor.RED + s.getRank(p), ChatColor.GRAY + "Status: " + ChatColor.AQUA + PlayerUtils.getStatus(p)));
				is.setItemMeta(sm);
				inv.setItem(i, is);
			}
			player.openInventory(inv);
		}else{
			player.sendMessage(MessageType.SETTLEMENT_NOT_EXIST.getMsg());
		}
	}
	
	/**
	 * Get the appropriate size of an inventory based on multiples of 9.
	 * <p>
	 * This ensures than no matter what the number is, a GUI's size will always be set
	 * to a multiple of 9.
	 * 
	 * @param max : The maximum size of the inventory
	 * @return The size of the inventory, based on a multiple of 9
	 */
	private int getInventorySize(int max) {
	    if (max <= 0) return 9;
	    int quotient = (int)Math.ceil(max / 9.0);
	    return quotient > 5 ? 54: quotient * 9;
	}
	
	/**
	 * List all of the existing Settlements in a GUI
	 * 
	 * @param player : The player who issued the command and will view the GUI
	 */
	public void listSettlements(Player player){
		Inventory inv = Bukkit.createInventory(null, getInventorySize(settlements.size()), ChatColor.BLUE + "All Current Settlements:");
		ItemStack is = new ItemStack(Material.DIAMOND);
		ItemMeta im = is.getItemMeta();
		
		for (int i = 0; i < settlements.size(); i++){
			Settlement s = settlements.get(i);
			im.setDisplayName(ChatColor.AQUA + s.getName());
			im.setLore(Arrays.asList(ChatColor.GOLD + "Owner: " + ChatColor.GREEN + Bukkit.getPlayer(s.getLeader()).getName(), 
					ChatColor.DARK_GREEN + "Members: " + ChatColor.RED + s.memberSize())); //TODO add settlement desc, power, & bal
			is.setItemMeta(im);
			inv.setItem(i, is);
		}
		player.openInventory(inv);	
	}
	
	public void depositIntoSettlement(Player player, Settlement s, double amount) throws SQLException{
		EconomyAPI.getAccountManager().withdraw(player, amount);
		s.setBalance(s.getBalance() + amount);
		DatabaseUtils.queryOut("UPDATE settlements SET balance=" + s.getBalance() + " WHERE id=" + s.getId() + ";");
	}
}