/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package gr.uoa.magdik.cloudslim;

import org.apache.commons.math3.analysis.function.Power;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * PowerHost class enables simulation of power-aware hosts.
 * <p>
 * If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:
 * <p>
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 *
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 2.0
 */
public class HyperPowerHost extends PowerHost implements Comparable {

    public Synchronizer getSynchronizer() {
        return synchronizer;
    }

    public void setSynchronizer(Synchronizer synchronizer) {
        this.synchronizer = synchronizer;
        if(synchronizer != null)
        this.synchronizer.host = this;
    }

    public HashMap<Integer, Double> cyclepowers;

    /**
     * The power model.

     */

    private PowerModel p;
    public boolean tobeoff = false;
    public boolean tobeon = false;
    public int waitmigrate = 0;
    public Synchronizer synchronizer;
    /**
     * Contains the hypercube neighbors of this host.
     */
    private Map<Integer, HyperPowerHost> neighbors;
    public Map<HyperPowerHost, ArrayList<Integer>> nodesh;
    /**
     * The utilization mips.
     */
    private double utilizationMips;

    /**
     * The previous utilization mips.
     */
    private double previousUtilizationMips;

    /**
     * The state history.
     */
    private final List<HostStateHistoryEntry> stateHistory = new LinkedList<HostStateHistoryEntry>();
    //public HostPowerProfile E;
    public double Pidle;
    public double Pmin;
    public double Pmax;
    public List<NetworkPacket> packetTosendLocal;
    public List<NetworkPacket> packetTosendGlobal;
    public List<NetworkPacket> packetrecieved;
    public double memory;
    public Switch sw; // Edge switch in general
    public double bandwidth;// latency
    /** time when last job will finish on CPU1 **/
    /**
     * time when last job will finish on CPU1
     **/
    public List<Double> CPUfinTimeCPU = new ArrayList<Double>();

    public double fintime = 0;
    public int vmsaftercycle = 0;
    SortedSet<Map.Entry<HyperPowerHost, ArrayList<Integer>>> descendsortedneighbors;

    @Override
    public int compareTo(Object o) {
        PowerHost p = (PowerHost) o;
        return Double.compare(this.getPower(), p.getPower());
    }

    public enum PowerState {
        OFF, IDLE, UNDERU, OK, OVERU
    }

    PowerState s;


    /**
     * Instantiates a new host.
     *
     * @param id             the id
     * @param ramProvisioner the ram provisioner
     * @param bwProvisioner  the bw provisioner
     * @param storage        the storage
     * @param peList         the pe list
     * @param vmScheduler    the VM scheduler
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
        p = powerModel;
        setPowerModel(powerModel);
        neighbors = new HashMap<>();
        //sw =new Switch("vmswitch",-1,(NetworkDatacenter)this.getDatacenter());
        packetrecieved = new ArrayList<NetworkPacket>();
        packetTosendGlobal = new ArrayList<NetworkPacket>();
        packetTosendLocal = new ArrayList<NetworkPacket>();
        Pidle = 160;
        Pmin = 180;
        Pmax = 250;
        s = PowerState.IDLE;
        nodesh = new HashMap<HyperPowerHost, ArrayList<Integer>>();
        cyclepowers = new HashMap<>();
        //synchronizer = new Synchronizer();
        //synchronizer.setHost(this);
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
                getPowerState() != PowerState.OFF);

        return smallerTime;
    }


    @Override
    public boolean isSuitableForVm(Vm vm) {
        boolean pemips = getVmScheduler().getPeCapacity() >= vm.getCurrentRequestedMaxMips();
        boolean avaliablerequested = getVmScheduler().getAvailableMips() >= vm.getCurrentRequestedTotalMips();
        boolean ram = getRamProvisioner().isSuitableForVm(vm, vm.getCurrentRequestedRam());
        boolean bw = getBwProvisioner().isSuitableForVm(vm, vm.getCurrentRequestedBw());

        return (pemips
                && avaliablerequested
                && ram && bw);
    }

    /**
     * Gets the power. For this moment only consumed by all PEs.
     *
     * @return the power
     */
    @Override
    public double getPower() {
        if (s == PowerState.OFF) {
            return 0;
        }
        double p = Pidle;

        for (Vm vm : getVmList()) {
            HyperPowerVm pvm = (HyperPowerVm) vm;
            vm.setHost(this);
            p += pvm.getPower();
        }
        if (tobeoff || tobeon) {
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
     * @param toUtilization   the to utilization
     * @param time            the time
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
     * @param host
     */


    public void setNeighbor(Integer dimension, HyperPowerHost host) {
        neighbors.put(dimension, host);
        ArrayList<Integer> pathId = new ArrayList<>();
        pathId.add(host.getId());
        nodesh.put(host, pathId);
        cyclepowers.put(host.getId() - 2, host.getPower());
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

    public PowerState getPowerState() {

        if (getVmList() != null) {
            //System.out.println("GGCURRHOSTPOWER ");
            double currentpower = getPower();// E.getPower(getVmList());
            //System.out.println("CURRHOSTPOWER = " + currentpower);
            if (currentpower == 0) {
                return PowerState.OFF;
            } else if (currentpower <= Pidle) {
                return PowerState.IDLE;
            } else if (currentpower >= Pidle && currentpower <= Pmin) {
                return PowerState.UNDERU;
            } else if (currentpower >= Pmin && currentpower < Pmax) {
                return PowerState.OK;
            } else {
                return PowerState.OVERU;
            }
        }
        return PowerState.OFF;
    }

    public double getTempPower() {
        return 160.0 + this.vmsaftercycle * 5;
    }

    public PowerState getPowerStatebyPower(double temppower) {

        if (getVmList() != null) {
            if (temppower == 0) {
                return PowerState.OFF;
            } else if (temppower <= Pidle) {
                return PowerState.IDLE;
            } else if (temppower >= Pidle && temppower <= Pmin) {
                return PowerState.UNDERU;
            } else if (temppower >= Pmin && temppower < Pmax) {
                return PowerState.OK;
            } else {
                return PowerState.OVERU;
            }
        }
        return PowerState.OFF;
    }


    public void setState(PowerState st) {
        s = st;
    }

    public void switchOff() {

        this.setState(PowerState.OFF);
        tobeoff = false;
    }

    public void switchOn() {

        this.setState(PowerState.IDLE);
        tobeon = false;
    }

    @Override
    public void startEntity() {

        Log.printLine(getName() + " is starting...");
        // this resource should register to regional GIS.
        // However, if not specified, then register to system GIS (the
        // default CloudInformationService) entity.
        int gisID = getId();
        if (gisID == -1) {
            gisID = CloudSim.getCloudInfoServiceEntityId();

            // send the registration to GIS
            sendNow(gisID, CloudSimTags.REGISTER_RESOURCE, getId());
        }

    }


    @Override
    public void processEvent(SimEvent ev) {
        int srcId = -1;
        switch (ev.getTag()) {
            case HyperCloudSimTags.HEARTBEAT:
                processhearbeat(ev);
            case HyperCloudSimTags.FORWARD_MSG:


        }
    }

    @Override
    public void shutdownEntity() {
        Log.printLine(getName() + " is shutting down...");
    }

    public void processhearbeat(SimEvent ev) {
        Map<HyperPowerHost, ArrayList<Integer>> heartnodes = (Map<HyperPowerHost, ArrayList<Integer>>) ev.getData();
        for (Map.Entry<HyperPowerHost, ArrayList<Integer>> entry : heartnodes.entrySet()) {
            if (!this.nodesh.containsKey(entry.getKey()) && entry.getKey().getId() != getId()) {
                nodesh.put(entry.getKey(), entry.getValue());
                nodesh.get(entry.getKey()).add(ev.getSource());
                cyclepowers.put(entry.getKey().getId() - 2, entry.getKey().getPower());
            }
            cyclepowers.put(entry.getKey().getId() - 2, entry.getKey().getPower());
        }
    }

    public void sendheartbeats() {
        SimEvent ev = new SimEvent();
        ev.setSource(this.getId());
        for (HyperPowerHost h : neighbors.values())
            sendNow(h.getId(), HyperCloudSimTags.HEARTBEAT, nodesh);
    }

    public void forwardmessage(SimEvent event) {

    }

    public void sendmessagebypath() {

    }

    //public void setId(int id)
    {
        this.id = id;
    }

    public void sortNeighbors()
    {
        descendsortedneighbors = new TreeSet<Map.Entry<HyperPowerHost, ArrayList<Integer>>>(
                new Comparator<Map.Entry<HyperPowerHost, ArrayList<Integer>>>() {
                    @Override
                    public int compare(Map.Entry<HyperPowerHost, ArrayList<Integer>> e1,
                                       Map.Entry<HyperPowerHost, ArrayList<Integer>> e2) {
                        int ord = Double.compare(e1.getKey().getPower(), e2.getKey().getPower());
                        if (ord == 1)
                            return -1;
                        return 1;
                    }
                });
        descendsortedneighbors.addAll((Collection<? extends Map.Entry<HyperPowerHost, ArrayList<Integer>>>) nodesh.entrySet());
    }

    public SortedSet<Map.Entry<HyperPowerHost, ArrayList<Integer>>> sortcachebyproximity() {
        SortedSet<Map.Entry<HyperPowerHost, ArrayList<Integer>>> sortedcache = new TreeSet<>(
                new Comparator<Map.Entry<HyperPowerHost, ArrayList<Integer>>>() {
                    @Override
                    public int compare(Map.Entry<HyperPowerHost, ArrayList<Integer>> e1,
                                       Map.Entry<HyperPowerHost, ArrayList<Integer>> e2) {
                        int ord = Integer.compare(e1.getValue().size(), e2.getValue().size());
                        if (ord == 1)
                            return -1;
                        return 1;
                    }
                });
        sortedcache.addAll((Collection<? extends Map.Entry<HyperPowerHost, ArrayList<Integer>>>) nodesh.entrySet());
        return sortedcache;
    }


    public List<Map<String, Object>> partialVmMigration() {
        //o algorithnos einai lathos den prepei na kanei olous tous geitones, mporei se kapoion na sthlei dyo fores5
        //System.out.println("PAPARTM1");
        //List<Map<String, Object>> migrationMap =

        CopyOnWriteArrayList vms = (CopyOnWriteArrayList) getVmList();
            int idx = 0;
            int vmcount = vms.size();
            boolean con = true;
            PowerState s;
            while (con) {
                s = getPowerStatebyPower(getTempPower());
                if (idx == vms.size()) {
                    con = false;
                    break;
                }
                //HyperPowerVm vm = (HyperPowerVm) vms.get(idx++);
                for(Vm rvm : getVmList()) {
                    HyperPowerVm vm = (HyperPowerVm) rvm;
                    if (vm.isInMigration()) continue;
                    for (Map.Entry<HyperPowerHost, ArrayList<Integer>> entry : descendsortedneighbors) {
                        double npower = cyclepowers.get(entry.getKey().getId() - 2);
                        PowerState ps = getPowerStatebyPower(npower);
                        if (vmcount == 0 || s != PowerState.OVERU) {
                            return null;
                        }
                        if (vm.getPower() >= entry.getKey().Pmax - entry.getKey().getTempPower()) {
                            continue;
                        }
                        if (entry.getKey().isSuitableForVm(vm)) {
                            Map<String, Object> migrate = new HashMap<String, Object>();
                            migrate.put("vm", vm);
                            migrate.put("host", entry.getKey());
                            entry.getKey().waitmigrate++;
                            vm.setInMigration(true);
                            //migrationMap.add(migrate);

                            if (entry.getKey().getPowerState() == PowerState.OFF) {
                                entry.getKey().tobeon = true;  // switchOn();
                                //offHosts.remove(entry.getKey());
                                send(
                                        getDatacenter().getId(),
                                        10,// + (vm.getRam() / ((double) entry.getKey().getBw() / (2 * 8000))),
                                        HyperCloudSimTags.HOST_ON_MIGRATE,
                                        migrate);
                            } else {
                                send(
                                        getDatacenter().getId(),
                                        10,// + (vm.getRam() / ((double) entry.getKey().getBw() / (2 * 8000))),
                                        HyperCloudSimTags.VM_MIGRATE,
                                        migrate);
                            }

                            vmcount--;

                            entry.getKey().vmsaftercycle += 1;
                            vmsaftercycle -= 1;
                            //System.out.println("from Host" + (h.getId() - 2) + " with " +
                            //      "" + h.vmsaftercycle + " VMs to Host" + (entry.getKey().getId() - 2) + " with " + entry.getKey().vmsaftercycle + " Vms");
                            System.out.println("--partial-- from Host" + (getId() - 2) + " with " +
                                    "" + getVmList().size() + " VMs to Host" + (entry.getKey().getId() - 2) + " with " + entry.getKey().getVmList().size() + " Vms");
                            break;
                        }
                    }
                }
            }
            return null;
        }

    public List<Map<String, Object>> fullVmMigration()
    {
        CopyOnWriteArrayList vms = (CopyOnWriteArrayList) getVmList();
        //System.out.println("HOST" + (getId() - 2) + "UNDER, MIGRATING");
        int idx = 0;
        int vmcount = vms.size();
        //HyperPowerVm vm;
        PowerState s;
        while (true) {
            s = getPowerStatebyPower(getTempPower());
            /*if (idx == vms.size()) {
                break;
            }
            vm = (HyperPowerVm) vms.get(idx++);*/
            for(Vm rvm : getVmList()) {
                HyperPowerVm vm = (HyperPowerVm) rvm;
                if (vm.isInMigration())
                {
                    System.out.println("INHOST" + (getId() - 2) +" ONMIGR vm" + vm.getId());
                    continue;
                }
                for (Map.Entry<HyperPowerHost, ArrayList<Integer>> entry : descendsortedneighbors) {
                    double npower = cyclepowers.get(entry.getKey().getId() - 2);
                    PowerState ps = getPowerStatebyPower(npower);//entry.getKey().getPowerState();
                    if (ps == HyperPowerHost.PowerState.OVERU || ps == PowerState.OFF || ps == PowerState.IDLE)// entry.getKey().getVmList().size() == 0) {
                    {    System.out.println("INHOST" + (getId() - 2) + "OVER OFF host" + (entry.getKey().getId() - 2));

                        continue;
                    }
                    else if(ps == PowerState.UNDERU && npower < getPower() )//entry.getKey().getVmList().size() < getVmList().size())
                    {
                        System.out.println("INHOST" + (getId() - 2) +"UNDERU host" + (entry.getKey().getId() - 2));
                        //continue;
                    }
                    if (getVmList().size() == 0 || s == PowerState.OVERU) {
                        break;
                    }

                    if (vm.getPower() >= entry.getKey().Pmax - npower){//entry.getKey().getTempPower()) {
                        System.out.println("INHOST" + (getId() - 2) +"LIMIT POWER" + (entry.getKey().getId() - 2));
                        continue;
                    }

                    //test if condition are met and migrate
                    if (entry.getKey().isSuitableForVm(vm)) {
                        HyperPowerHost mh = entry.getKey();
                        Map<String, Object> migrate = new HashMap<String, Object>();
                        migrate.put("vm", vm);
                        migrate.put("host", mh);
                        mh.waitmigrate++;
                        mh.vmsaftercycle += 1;
                        vmsaftercycle -= 1;
                        vmcount--;
                        vm.setInMigration(true);
                        System.out.println("from Host" + (getId() - 2) + " with " +
                                "" + getVmList().size() + " VMs to Host" + (entry.getKey().getId() - 2) + " with " + entry.getKey().getVmList().size() + " Vms");
                        send(
                                getDatacenter().getId(),
                                0,// + (vm.getRam() / ((double) entry.getKey().getBw() / (2 * 8000))),
                                HyperCloudSimTags.VM_MIGRATE,
                                migrate);
                        break;
                    }


                }
            }
            /*if (vmcount == 0 && waitmigrate == 0) {

                send(
                        getId(),
                        10,
                        HyperCloudSimTags.HOST_OFF,
                        this);
            }*/
            break;
        }
        return null;
    }


}
