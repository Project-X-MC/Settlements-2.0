package me.projectx.Settlements.Utils;

import java.sql.SQLException;

import me.projectx.Settlements.Main;
import me.projectx.Settlements.Commands.CommandSettlementAdmin;
import me.projectx.Settlements.Commands.CommandSettlementPlayer;
import me.projectx.Settlements.Events.BlockEvent;
import me.projectx.Settlements.Events.InventoryClick;
import me.projectx.Settlements.Events.MapInitialize;
import me.projectx.Settlements.Events.PlayerChat;
import me.projectx.Settlements.Events.PlayerDamage;
import me.projectx.Settlements.Events.PlayerDeath;
import me.projectx.Settlements.Events.PlayerInteract;
import me.projectx.Settlements.Events.PlayerJoin;
//import me.projectx.Settlements.Events.PlayerJoin;
import me.projectx.Settlements.Events.PlayerMove;
import me.projectx.Settlements.Events.PlayerQuit;
import me.projectx.Settlements.Managers.ChunkManager;
import me.projectx.Settlements.Managers.MapManager;
import me.projectx.Settlements.Managers.PlayerManager;
import me.projectx.Settlements.Managers.SettlementManager;
import me.projectx.Settlements.Models.Players;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;
import org.bukkit.plugin.PluginManager;


public class Startup extends Thread {

	public static void runStartup() throws SQLException{
		PluginManager pm = Bukkit.getPluginManager();

		pm.registerEvents(new BlockEvent(), Main.getInstance());
		pm.registerEvents(new MapInitialize(), Main.getInstance());
		pm.registerEvents(new PlayerChat(), Main.getInstance());
		pm.registerEvents(new PlayerDamage(), Main.getInstance());
		pm.registerEvents(new PlayerDeath(), Main.getInstance());
		pm.registerEvents(new PlayerInteract(), Main.getInstance());
		pm.registerEvents(new PlayerJoin(), Main.getInstance());
		pm.registerEvents(new PlayerQuit(), Main.getInstance());
		pm.registerEvents(new PlayerMove(), Main.getInstance());
		pm.registerEvents(new InventoryClick(), Main.getInstance());

		Main.getInstance().getCommand("s").setExecutor(new CommandSettlementPlayer());
		Main.getInstance().getCommand("sa").setExecutor(new CommandSettlementAdmin());

		CommandType.prepareCommandList();

		DatabaseUtils.setupConnection();
		DatabaseUtils.setupMySQL();

		loadSettlements();

		ChunkManager.getInstance().loadChunks();
		
		for(Player p : Bukkit.getOnlinePlayers()){
			Players pl = PlayerManager.getInstance().addPlayer(p);
			pl.setInt("map", 25);
		}
		
		MapView m = Bukkit.getServer().getMap((short)0);
		for(org.bukkit.map.MapRenderer r : m.getRenderers()){
			m.removeRenderer(r);
		}
		m.addRenderer(MapManager.getInstance().getRenderMap());
	}

	private static void loadSettlements() {
		new Thread() {
			@Override
			public void run() {
				try {
					SettlementManager.getManager().loadSettlmentsFromDB();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
}
