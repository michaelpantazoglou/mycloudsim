/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package gr.uoa.magdik.cloudslim;

import gr.uoa.di.madgik.cloudsim.HostPowerProfile;
import gr.uoa.di.madgik.cloudsim.HyperEdgeSwitch;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.network.datacenter.NetworkDatacenter;
import org.cloudbus.cloudsim.network.datacenter.NetworkPacket;
import org.cloudbus.cloudsim.network.datacenter.Switch;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;

import java.util.*;

/**
 * PowerHost class enables simulation of power-aware hosts.
 * 
 * If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:
 * 
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 2.0
 */
public class HyperPowerHost extends HostDynamicWorkload implements Comparable{

	/** The power model. */
	private PowerModel p;
    public boolean tobeoff = false;
    public boolean tobeon = false;
    public int waitmigrate = 0;
	/**
	 * Contains the hypercube neighbors of this host.
	 */
	private Map<Integer, PowerHost> neighbors;
    /** The utilization mips. */
	private double utilizationMips;

	/** The previous utilization mips. */
	private double previousUtilizationMips;

	/** The state history. */
	private final List<HostStateHistoryEntry> stateHistory = new LinkedList<HostStateHistoryEntry>();

	//public HostPowerProfile E;

	public double Pidle;
	public double Pmin;
	public double Pmax;



	@Override
	public int compareTo(Object o) {
		PowerHost p = (PowerHost) o;
		//return Double.compare(this.E.getPower(getVmList()), p.E.getPower(p.getVmList()));
		return Double.compare(this.getPower(), p.getPower());
	}

	public enum State {
		OFF,IDLE, UNDERU, OK, OVERU
	}

	State s;


	public List<NetworkPacket> packetTosendLocal;

	public List<NetworkPacket> packetTosendGlobal;

	public List<NetworkPacket> packetrecieved;

	public double memory;

	public Switch sw; // Edge switch in general

	public double bandwidth;// latency

	/** time when last job will finish on CPU1 **/
	/** time when last job will finish on CPU1 **/
	public List<Double> CPUfinTimeCPU = new ArrayList<Double>();

	public double fintime = 0;



	/**
	 * Instantiates a new host.
	 *
	 * @param id the id
	 * @param ramProvisioner the ram provisioner
	 * @param bwProvisioner the bw provisioner
	 * @param storage the storage
	 * @param peList the pe list
	 * @param vmScheduler the VM scheduler
	 */
	public HyperPowerHost(
            int id,
            RamProvisioner ramProvisioner,
            BwProvisioner bwProvisioner,
            long storage,
            List<? extends Pe> peList,
            VmScheduler vmScheduler,
            PowerModel powerModel) {
		super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler);
		//powerModel = new HostPowerProfile(0,0,0);
		//E = (HostPowerProfile)powerModel;
		p =  powerModel;
		setPowerModel(powerModel);
		neighbors = new HashMap<Integer, PowerHost>();
		sw =new Switch("vmswitch",-1,(NetworkDatacenter)this.getDatacenter());
		packetrecieved = new ArrayList<NetworkPacket>();
		packetTosendGlobal = new ArrayList<NetworkPacket>();
		packetTosendLocal = new ArrayList<NetworkPacket>();
		Pidle = 160;
		Pmin = 180;
		Pmax = 250;
        s = State.IDLE;
	}

	/**
	 * Gets the power. For this moment only consumed by all PEs.
	 * 
	 * @return the power
	 */
	public double getPower() {
        if(s == State.OFF)
        {
            return 0;
        }
		double p = Pidle;

		for(Vm vm: getVmList())
		{
			PowerVm pvm = (PowerVm) vm;
            vm.setHost(this);
			p += pvm.getPower();
 		}
        if(tobeoff || tobeon)
        {
            p += 20;
        }

		//return getPower(getUtilizationOfCpu());
		return p;
	}

	/**
	 * Gets the power. For this moment only consumed by all PEs.
	 * 
	 * @param utilization the utilization
	 * @return the power
	 */
	protected double getPower(double utilization) {
        /*
		double power = 0;
		try {
			//System.out.println("UtilP = " + utilization);
			power = getPowerModel().getPower(utilization);
			//
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}*/
		return getPower();
	}



	/**
	 * Gets the max power that can be consumed by the host.
	 * 
	 * @return the max power
	 */
	public double getMaxPower() {
		/*double power = 0;
		try {
			power = getPowerModel().getPower(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}*/
		return Pmax;
	}

	/**
	 * Gets the energy consumption using linear interpolation of the utilization change.
	 * 
	 * @param fromUtilization the from utilization
	 * @param toUtilization the to utilization
	 * @param time the time
	 * @return the energy
	 */
	public double getEnergyLinearInterpolation(double fromUtilization, double toUtilization, double time) {
		//System.out.println("fromu " + fromUtilization + " tu " + toUtilization);
		/*if (fromUtilization == 0) {
			return 0;
		}
		double fromPower = getPower(fromUtilization);
		double toPower = getPower(toUtilization);
		//System.out.println("fromp " + fromPower + " tp " + toPower);
		return (fromPower + (toPower - fromPower) / 2) * time;*/
        return getPower();
	}

	/**
	 * Sets the power model.
	 * 
	 * @param powerModel the new power model
	 */
	protected void setPowerModel(PowerModel powerModel) {
		this.p = powerModel;
	}

	/**
	 * Gets the power model.
	 * 
	 * @return the power model
	 */
	public PowerModel getPowerModel() {
		return p;
	}

	/**
	 * Sets the neighbor at the specified dimension.
	 *
	 * @param dimension
	 * @param hostId
	 */


	public void setNeighbor(Integer dimension, PowerHost hostId) {
		neighbors.put(dimension, hostId);
	}

	/**
	 * Gets the neighbor at the specified dimension.
	 *
	 * @param dimension
	 * @return
	 */
	public PowerHost getNeighbor(Integer dimension) {
		return neighbors.get(dimension);
	}

	/**
	 * Gets all neighbors.
	 *
	 * @return
	 */
	public Map<Integer, PowerHost> getNeighbors() {
		return neighbors;
	}

	public State getState() {

		if (getVmList() != null) {
			//System.out.println("GGCURRHOSTPOWER ");
			double currentpower = getPower();// E.getPower(getVmList());
			//System.out.println("CURRHOSTPOWER = " + currentpower);
			if (currentpower == 0) {
				return State.OFF;
			} else if (currentpower <= Pidle) {
				return State.IDLE;
			} else if (currentpower >= Pidle && currentpower <= Pmin) {
				return State.UNDERU;
			} else if (currentpower >= Pmin && currentpower <= Pmax) {
				return State.OK;
			} else {
				return State.OVERU;
			}
			//return State.OFF;
		}
		return State.OFF;
	}

	public void setState(State st) {
		s = st;
        //if(s == State.OFF)
        //{
        //    switchOff();
        //}
	}

    public void switchOff()
    {

        this.setState(State.OFF);
        tobeoff = false;
        //System.out.println("TURNOFF");
        //System.exit(-1);
    }

    public void switchOn()
    {

        this.setState(State.IDLE);
        tobeon = false;
        //System.out.println("SWITXHON");
        //System.exit(-1);
    }

}
