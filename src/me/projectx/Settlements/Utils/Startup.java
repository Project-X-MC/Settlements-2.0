package me.projectx.Settlements.Utils;

import java.sql.SQLException;

import me.projectx.Settlements.Main;
import me.projectx.Settlements.Commands.CommandSettlementPlayer;
import me.projectx.Settlements.Events.PlayerChat;
import me.projectx.Settlements.Events.PlayerJoin;
import me.projectx.Settlements.Events.PlayerMove;
import me.projectx.Settlements.Events.PlayerQuit;
import me.projectx.Settlements.Managers.ChunkManager;
import me.projectx.Settlements.Managers.SettlementManager;
//import me.projectx.Settlements.Scoreboard.NameBoard;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

public class Startup extends Thread {

	public static void runStartup() throws SQLException{
		PluginManager pm = Bukkit.getPluginManager();

		pm.registerEvents(new PlayerJoin(), Main.getInstance());
		pm.registerEvents(new PlayerQuit(), Main.getInstance());
		pm.registerEvents(new PlayerChat(), Main.getInstance());
		pm.registerEvents(new PlayerMove(), Main.getInstance());

		Main.getInstance().getCommand("s").setExecutor(new CommandSettlementPlayer());

		CommandType.prepareCommandList();

		DatabaseUtils.setupConnection();
		DatabaseUtils.setupMySQL();

		loadSettlements();

		new ChunkManager(); 
		ChunkManager.getInstance().loadChunks();

		//new NameBoard();
	}

	private static void loadSettlements() {
		new Thread() {
			@Override
			public void run() {
				try {
					SettlementManager.loadSettlmentsFromDB();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
}
