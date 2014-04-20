package me.projectx.Settlements.Models;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;

public class ClaimedChunk {

	public static List<ClaimedChunk> instances = new ArrayList<ClaimedChunk>();
	private final int x, z;
	private World w;
	private String owner;
	private Settlement set;

	public ClaimedChunk(int x, int z, String owner, Settlement set, World w){
		this.x = x;
		this.z = z;
		this.w = w;
		this.owner = owner;
		this.set = set;
		instances.add(this);
	}

	//Local methods//

	public int getX(){
		return this.x;
	}

	public int getZ(){
		return this.z;
	}

	public World getWorld(){
		return this.w;
	}

	public void setWorld(String world){
		this.w = Bukkit.getWorld(world);
	}

	public void setWorld(World world){
		this.w = world;
	}

	public String getOwner(){
		return this.owner;
	}

	public void setOwner(String owner){
		this.owner = owner;
	}

	public Settlement getSettlement(){
		return set;
	}

	public void setSettlement(Settlement set){
		this.set = set;
	}

	public void deleteChunk() throws Throwable{
		this.finalize();
	}
}
