package me.projectx.Settlements.Managers;

import me.projectx.Settlements.Models.Settlement;
import me.projectx.Settlements.Utils.Fanciful.FancyMessage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ChatManager {
	
	private static ChatManager cm = new ChatManager();
	
	public static ChatManager getManager(){
		return cm;
	}
	
	public void formatMessage(Player sendTo, String playerName, String message){
		Player p = Bukkit.getPlayer(playerName);
		Settlement s = SettlementManager.getManager().getPlayerSettlement(p.getUniqueId());
		if (s != null){
			new FancyMessage("[")
				.color(ChatColor.DARK_GRAY)
			.then(s.getName())
				.tooltip(ChatColor.DARK_GREEN + "Leader: " + Bukkit.getPlayer(s.getLeader()).getDisplayName(), //temp. Will throw NPE if offline
						ChatColor.GOLD + "Description: " + ChatColor.GREEN + s.getDescription())
				.color(getColor(p))
			.then("] ")
				.color(ChatColor.DARK_GRAY)
			.then(p.getDisplayName() + ": ")
			.then(ChatColor.translateAlternateColorCodes('&', message))
			.send(sendTo);
		}
	}
	
	private ChatColor getColor(Player player){
		Settlement s = SettlementManager.getManager().getPlayerSettlement(player.getUniqueId());
		if (s != null){
			switch(s.getRank(player)){
				case "Leader":
					return ChatColor.GOLD;
				case "Officer":
					return ChatColor.YELLOW;
				default:
					return ChatColor.AQUA;
			}
		}
		return null;
	}
	
}
