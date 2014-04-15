package me.projectx.Settlements.Commands;

import me.projectx.Settlements.Utils.MessageType;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public abstract class CommandModel implements CommandExecutor{

	private String perms, usage;
	
	public CommandModel(String command, String permission, String usage){
		this.perms = permission;
		this.usage = usage;
	}
	
	public abstract boolean onCmd(CommandSender sender, String cml, String[] args);
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if (!(sender.hasPermission(perms))){
			sender.sendMessage(MessageType.NO_PERM.getMsg());
			return false;
		}
		
		if (!(onCmd(sender, commandLabel, args))){
			sender.sendMessage(MessageType.PREFIX.getMsg() + ChatColor.GRAY + "Correct usage: " + ChatColor.AQUA + usage);
			return false;
		}
		return true;
	}
}