/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package gr.uoa.magdik.cloudslim;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.network.datacenter.NetworkDatacenter;
import org.cloudbus.cloudsim.network.datacenter.NetworkPacket;
import org.cloudbus.cloudsim.network.datacenter.Switch;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;
import org.cloudbus.cloudsim.util.MathUtil;

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
public class HyperPowerHost extends PowerHost implements Comparable{

	/** The power model. */
	private PowerModel p;
    public boolean tobeoff = false;
    public boolean tobeon = false;
    public int waitmigrate = 0;
	/**
	 * Contains the hypercube neighbors of this host.
	 */
	private Map<Integer, HyperPowerHost> neighbors;
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
		super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler, powerModel);
		p =  powerModel;
		setPowerModel(powerModel);
		neighbors = new HashMap<>();
		sw =new Switch("vmswitch",-1,(NetworkDatacenter)this.getDatacenter());
		packetrecieved = new ArrayList<NetworkPacket>();
		packetTosendGlobal = new ArrayList<NetworkPacket>();
		packetTosendLocal = new ArrayList<NetworkPacket>();
		Pidle = 160;
		Pmin = 180;
		Pmax = 250;
        s = State.IDLE;
	}


    @Override
    public double updateVmsProcessing(double currentTime) {
        double smallerTime = super.updateVmsProcessing(currentTime);
        setPreviousUtilizationMips(getUtilizationMips());
        setUtilizationMips(0);
        double hostTotalRequestedMips = 0;

        for (Vm vm : getVmList()) {
            getVmScheduler().deallocatePesForVm(vm);
        }

        for (Vm vm : getVmList()) {
            getVmScheduler().allocatePesForVm(vm, vm.getCurrentRequestedMips());
        }

        for (Vm vm : getVmList()) {
            double totalRequestedMips = vm.getCurrentRequestedTotalMips();
            double totalAllocatedMips = getVmScheduler().getTotalAllocatedMipsForVm(vm);

            if (!Log.isDisabled()) {
                Log.formatLine(
                        "%.2f: [Host #" + getId() + "] Total allocated MIPS for VM #" + vm.getId()
                                + " (Host #" + vm.getHost().getId()
                                + ") is %.2f, was requested %.2f out of total %.2f (%.2f%%)",
                        CloudSim.clock(),
                        totalAllocatedMips,
                        totalRequestedMips,
                        vm.getMips(),
                        totalRequestedMips / vm.getMips() * 100);

                List<Pe> pes = getVmScheduler().getPesAllocatedForVM(vm);
                StringBuilder pesString = new StringBuilder();
                for (Pe pe : pes) {
                    pesString.append(String.format(" PE #" + pe.getId() + ": %.2f.", pe.getPeProvisioner()
                            .getTotalAllocatedMipsForVm(vm)));
                }
                Log.formatLine(
                        "%.2f: [Host #" + getId() + "] MIPS for VM #" + vm.getId() + " by PEs ("
                                + getNumberOfPes() + " * " + getVmScheduler().getPeCapacity() + ")."
                                + pesString,
                        CloudSim.clock());
            }

            if (getVmsMigratingIn().contains(vm)) {
                Log.formatLine("%.2f: [Host #" + getId() + "] VM #" + vm.getId()
                        + " is being migrated to Host #" + getId(), CloudSim.clock());
            } else {
                if (totalAllocatedMips + 0.1 < totalRequestedMips) {
                    Log.formatLine("%.2f: [Host #" + getId() + "] Under allocated MIPS for VM #" + vm.getId()
                            + ": %.2f", CloudSim.clock(), totalRequestedMips - totalAllocatedMips);
                }

                vm.addStateHistoryEntry(
                        currentTime,
                        totalAllocatedMips,
                        totalRequestedMips,
                        (vm.isInMigration() && !getVmsMigratingIn().contains(vm)));

                if (vm.isInMigration()) {
                    Log.formatLine(
                            "%.2f: [Host #" + getId() + "] VM #" + vm.getId() + " is in migration",
                            CloudSim.clock());
                    totalAllocatedMips /= 0.9; // performance degradation due to migration - 10%
                }
            }

            setUtilizationMips(getUtilizationMips() + totalAllocatedMips);
            hostTotalRequestedMips += totalRequestedMips;
        }

        addStateHistoryEntry(
                currentTime,
                getUtilizationMips(),
                hostTotalRequestedMips,
                 getState() != State.OFF);

        return smallerTime;
    }




	/**
	 * Gets the power. For this moment only consumed by all PEs.
	 * 
	 * @return the power
	 */
	@Override
    public double getPower() {
        if(s == State.OFF)
        {
            return 0;
        }
		double p = Pidle;

		for(Vm vm: getVmList())
		{
			HyperPowerVm pvm = (HyperPowerVm) vm;
            vm.setHost(this);
			p += pvm.getPower();
 		}
        if(tobeoff || tobeon)
        {
            p += 20;
        }
		return p;
	}

	/**
	 * Gets the power. For this moment only consumed by all PEs.
	 * 
	 * @param utilization the utilization
	 * @return the power
	 */
	@Override
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
	 *  @param dimension
	 * @param hostId
     */


	public void setNeighbor(Integer dimension, HyperPowerHost hostId) {
		neighbors.put(dimension, hostId);
	}

	/**
	 * Gets the neighbor at the specified dimension.
	 *
	 * @param dimension
	 * @return
	 */
	public HyperPowerHost getNeighbor(Integer dimension) {
		return neighbors.get(dimension);
	}

	/**
	 * Gets all neighbors.
	 *
	 * @return
	 */
	public Map<Integer, HyperPowerHost> getNeighbors() {
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
		}
		return State.OFF;
	}

	public void setState(State st) {
		s = st;
	}

    public void switchOff()
    {

        this.setState(State.OFF);
        tobeoff = false;
    }

    public void switchOn()
    {

        this.setState(State.IDLE);
        tobeon = false;
    }

}


