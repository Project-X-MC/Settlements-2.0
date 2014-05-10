package me.projectx.Settlements.Models;

import java.util.ArrayList;
import java.util.List;

import me.projectx.Settlements.Managers.SettlementManager;

public class War {

	public static List<War> instances = new ArrayList<War>();

	private final long id; //Id system same as settlements; ids are cleared on reload
	private final long setA, setB; //Set A is the one who starts the war; Set B is the one who accepts
	private double time  = 0.0; //Time is stored as a double

	/**
	 * Create a new war...
	 *
	 * @param settlementA (The settlement that started the war)
	 * @param settlementB (The settlement that accepted the war)
	 */
	public War(Settlement settlementA, Settlement settlementB){
		this.setA = settlementA.getId();
		this.setB = settlementB.getId();
		id = instances.size() + 1;
		instances.add(this);
	}

	public long getId(){
		return this.id;
	}

	public Settlement getStarter(){
		return SettlementManager.getManager().getSettlement(this.setA);
	}

	public Settlement getAccepter(){
		return SettlementManager.getManager().getSettlement(this.setB);
	}

	public double getWarLength(){
		return this.time;
	}

	public void setWarLength(double value){
		this.time = value;
	}

	public void addWarLength(double value){
		this.time = this.time + value;
	}

}