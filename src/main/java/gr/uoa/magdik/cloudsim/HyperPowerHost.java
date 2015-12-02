/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package gr.uoa.magdik.cloudsim;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.network.datacenter.NetworkPacket;
import org.cloudbus.cloudsim.network.datacenter.Switch;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


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

    public boolean isVmstatechange() {
        return vmstatechange;
    }

    public void setVmstatechange(boolean vmstatechange) {
        this.vmstatechange = vmstatechange;
    }

    public boolean vmstatechange = true;
    public boolean tobeoff = false;
    public boolean tobeon = false;
    public int waitmigrate = 0;
    public Synchronizer synchronizer;

    public double getStartsynchtime() {
        return startsynchtime;
    }

    public void setStartsynchtime(double startsynchtime) {
        this.startsynchtime = startsynchtime;
    }

    double startsynchtime = 0 ;
    /**
     * Contains the hypercube neighbors of this host.
     */
    private Map<Integer, HyperPowerHost> neighbors;
    public Map<HyperPowerHost, ArrayList<Integer>> nodesh;
    private HashMap<HyperPowerHost, ArrayList<Integer>> nodenew;



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
    Map<HyperPowerHost, ArrayList<Integer>> heartnodes;

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
        nodenew = new HashMap<HyperPowerHost, ArrayList<Integer>>();
        heartnodes = new HashMap<>();
        setName("HyperHost" + (getId() - 2));
    }


    @Override
    public double updateVmsProcessing(double currentTime) {
        //double smallerTime = super.updateVmsProcessing(currentTime);
        double smallerTime = Double.MAX_VALUE;

        for (Vm vm : getVmList()) {
            double time = vm.updateVmProcessing(currentTime, getVmScheduler().getAllocatedMipsForVm(vm));
            if (time > 0.0 && time < smallerTime) {
                smallerTime = time;
            }
        }
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

            /*if (!Log.isDisabled()) {
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
            }*/

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
           // vm.setHost(this);
            p += pvm.getPower();
        }
        //if (tobeoff || tobeon) {
          //  p += 20;
        //}
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
		/*if (fromUtilization == 0) {
			return 0;
		}
		double fromPower = getPower(fromUtilization);
		double toPower = getPower(toUtilization);
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
            double currentpower = getPower();// E.getPower(getVmList());
            if (currentpower == 0) {
                return PowerState.OFF;
            } else if (currentpower <= Pidle) {
                return PowerState.IDLE;
            } else if (currentpower >= Pidle && currentpower < Pmin) {
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
            } else if (temppower >= Pidle && temppower < Pmin) {
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
        HyperVmAllocationPolicy hp = (HyperVmAllocationPolicy) getDatacenter().getVmAllocationPolicy();
        if(hp.getOnHosts().contains(this))
        {
            //System.out.println("TURNING HOST:" + (getId() - 2) + "OFF");
            hp.getOnHosts().remove(this);
            HyperPowerDatacenter hpd  = (HyperPowerDatacenter) getDatacenter();
            hpd.incrementhostoffCount();
        }
    }

    public void switchOn() {
        this.setState(PowerState.IDLE);
        tobeon = false;
        HyperVmAllocationPolicy hp = (HyperVmAllocationPolicy) getDatacenter().getVmAllocationPolicy();
        if(!hp.getOnHosts().contains(this))
        {
            hp.getOnHosts().add(this);
            HyperPowerDatacenter hpd = (HyperPowerDatacenter) getDatacenter();
            hpd.incrementhostonCount();
        }
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
            case HyperCloudSimTags.REMOVEVM:
                Vm vm = (Vm) ev.getData();
                vmDestroy(vm);
                //getDatacenter().getVmList().remove(vm);
                //sendNow(getDatacenter().getId(), CloudSimTags.VM_DESTROY, vm);
                break;
            case HyperCloudSimTags.HEARTBEAT:
                processhearbeat(ev);
                break;
            case HyperCloudSimTags.FORWARD_MSG:
        }
    }

    @Override
    public void shutdownEntity() {
        Log.printLine(getName() + " is shutting down...");
    }

    public void processhearbeat(SimEvent ev) {
       // System.out.println("host" + (this.getId() - 2) + " RECEIVING HB TIME:" + CloudSim.clock());
        Map<? extends HyperPowerHost, ? extends ArrayList<Integer>> nnodes = (Map<? extends HyperPowerHost, ? extends ArrayList<Integer>>) ev.getData();
        heartnodes.putAll((Map<? extends HyperPowerHost, ? extends ArrayList<Integer>>) ev.getData());
    }

    public void buildpowermap()
    {
        setStartsynchtime(CloudSim.clock());
        nodenew.clear();
        HashMap<HyperPowerHost, ArrayList<Integer>> nodedifference = new HashMap<HyperPowerHost, ArrayList<Integer>>();
        if(heartnodes!= null) {
            for (Map.Entry<HyperPowerHost, ArrayList<Integer>> entry : heartnodes.entrySet()) {
                if (!this.nodesh.containsKey(entry.getKey()) && entry.getKey().getId() != getId()) {
                    nodedifference.put(entry.getKey(), entry.getValue());
                    nodesh.put(entry.getKey(), entry.getValue());
                    nodesh.get(entry.getKey()).add(entry.getKey().getId() - 2);
                    cyclepowers.put(entry.getKey().getId() - 2, entry.getKey().getPower());
                }
                if (entry.getKey().getVmList().size() > 18) {
                }
                cyclepowers.put(entry.getKey().getId() - 2, entry.getKey().getPower());
            }
        }
        nodenew.putAll(nodedifference);
        for(HyperPowerHost h : nodesh.keySet())
        {
            cyclepowers.put(h.getId() - 2, h.getPower());
        }
        heartnodes.clear();
    }

    public void sendheartbeats() {
        if(nodenew.size() == 0) {
            nodenew.putAll(nodesh);
        }
        SimEvent ev = new SimEvent();
        ev.setSource(getId());
        for (HyperPowerHost h : neighbors.values()) {
            HashMap<HyperPowerHost, ArrayList<Integer>> nodesend = new HashMap<HyperPowerHost, ArrayList<Integer>>();
            nodesend.putAll(nodenew);
            sendNow(h.getId(), HyperCloudSimTags.HEARTBEAT, nodesend);
        }
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


    public int partialVmMigration() {
       CopyOnWriteArrayList vms = (CopyOnWriteArrayList) getVmList();
        int idx = 0;
        int vmcount = vms.size();
        boolean con = true;
        PowerState s;
            ArrayList<Integer> vmids = new ArrayList<>();
            while (con) {
                for(Vm rvm : getVmList()) {
                    HyperPowerVm vm = (HyperPowerVm) rvm;
                    if (vmids.contains(vm.getId())) {
                        continue;
                    } else
                    {
                        vmids.add(vm.getId());
                    }
                    if (vm.isInMigration()) continue;
                    for (Map.Entry<HyperPowerHost, ArrayList<Integer>> entry : descendsortedneighbors) {
                        double currentTime = CloudSim.clock();
                        if(currentTime - getStartsynchtime() > 45)
                        {
                            return 2;
                        }
                        s = getPowerStatebyPower(getTempPower());
                        double npower = cyclepowers.get(entry.getKey().getId() - 2);
                        PowerState ps = getPowerStatebyPower(npower);
                        if (s != PowerState.OVERU) {
                            return 1;
                        }
                        if (vm.getPower() >= entry.getKey().Pmax - npower) {
                            continue;
                        }
                        if(ps == PowerState.OVERU) continue;
                        if (entry.getKey().isSuitableForVm(vm)) {
                            Map<String, Object> migrate = new HashMap<String, Object>();
                            migrate.put("vm", vm);
                            migrate.put("host", entry.getKey());
                            entry.getKey().waitmigrate++;
                            vm.setInMigration(true);

                            if (ps == PowerState.OFF) {
                                entry.getKey().tobeon = true;
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

                            //System.out.println("--partial-- Time " + currentTime+ " VN " + vm.getId() + " from Host" + (getId() - 2) + " with " +
                            //        "" + getVmList().size() + " VMs to Host" + (entry.getKey().getId() - 2) + " with " + entry.getKey().getVmList().size() + " Vms and PPOWER:" + npower);
                            break;
                        }
                    }
                }
                break;
            }
            setVmstatechange(false);
            return 1;
        }

    public int fullVmMigration()
    {
        CopyOnWriteArrayList vms = (CopyOnWriteArrayList) getVmList();
        int idx = 0;
        int vmcount = vms.size();
        ArrayList<Integer> vmids = new ArrayList<>();
        PowerState s;
        while (true) {
            for(Vm rvm : getVmList()) {
                HyperPowerVm vm = (HyperPowerVm) rvm;
                if (vmids.contains(vm.getId())) {
                    continue;
                } else
                {
                    vmids.add(vm.getId());
                }
                s = getPowerStatebyPower(getPower());
                if (vm.isInMigration())
                {
                    continue;
                }
                for (Map.Entry<HyperPowerHost, ArrayList<Integer>> entry : descendsortedneighbors) {
                    double currentTime = CloudSim.clock();
                    if(currentTime - getStartsynchtime() > 45)
                    {
                        return 2;
                    }
                    double npower = cyclepowers.get(entry.getKey().getId() - 2);
                    PowerState ps = getPowerStatebyPower(npower);//entry.getKey().getPowerState();
                    if (ps == PowerState.OVERU || ps == PowerState.OFF || ps == PowerState.IDLE)// entry.getKey().getVmList().size() == 0) {
                    {
                        continue;
                    }
                    /*else if(ps == PowerState.UNDERU && npower < getPower() )//entry.getKey().getVmList().size() < getVmList().size())
                    {
                        //continue;
                    }*/
                    if (getVmList().size() == 0 || s == PowerState.OVERU) {
                        break;
                    }
                    if (vm.getPower() >= entry.getKey().Pmax - npower){//entry.getKey().getTempPower()) {
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
                        //System.out.println("Time" + currentTime + " VM " + vm.getId() + "from Host" + (getId() - 2) + " with " +
                        //                "" + getVmList().size() + " VMs to Host" + (entry.getKey().getId() - 2) + " with " + entry.getKey().getVmList().size() + " Vms and PPOWER:" + npower);
                                send(
                                        getDatacenter().getId(),
                                        10,// + (vm.getRam() / ((double) entry.getKey().getBw() / (2 * 8000))),
                                        HyperCloudSimTags.VM_MIGRATE,
                                        migrate);
                        break;
                    }
                }
            }
            break;
        }
        return 1;
    }

    /**
     * Allocates PEs and memory to a new VM in the Host.
     *
     * @param vm Vm being started
     * @return $true if the VM could be started in the host; $false otherwise
     * @pre $none
     * @post $none
     */
    public boolean vmCreate(Vm vm) {
        if (getStorage() < vm.getSize()) {
            Log.printLine("[VmScheduler.vmCreate] Allocation of VM #" + vm.getId() + " to Host #" + getId()
                    + " failed by storage");
            return false;
        }

        if (!getRamProvisioner().allocateRamForVm(vm, vm.getCurrentRequestedRam())) {
            Log.printLine("[VmScheduler.vmCreate] Allocation of VM #" + vm.getId() + " to Host #" + getId()
                    + " failed by RAM");
            return false;
        }

        if (!getBwProvisioner().allocateBwForVm(vm, vm.getCurrentRequestedBw())) {
            Log.printLine("[VmScheduler.vmCreate] Allocation of VM #" + vm.getId() + " to Host #" + getId()
                    + " failed by BW");
            getRamProvisioner().deallocateRamForVm(vm);
            return false;
        }

        if (!getVmScheduler().allocatePesForVm(vm, vm.getCurrentRequestedMips())) {
            Log.printLine("[VmScheduler.vmCreate] Allocation of VM #" + vm.getId() + " to Host #" + getId()
                    + " failed by MIPS");
            getRamProvisioner().deallocateRamForVm(vm);
            getBwProvisioner().deallocateBwForVm(vm);
            return false;
        }

        HyperPowerHost oldhost = (HyperPowerHost) vm.getHost();
        if(oldhost != null)
        {
            HyperVmAllocationPolicy hp = (HyperVmAllocationPolicy) getDatacenter().getVmAllocationPolicy();
            hp.getVmTable().remove(vm.getUid());
            oldhost.vmDestroy(vm);
            oldhost.removeMigratingInVm(vm);
            if(oldhost.getVmList().size() == 0) oldhost.switchOff();
        }

        setStorage(getStorage() - vm.getSize());
        getVmList().add(vm);
        vm.setHost(this);
       // System.out.println(CloudSim.clock() + " VM" + vm.getId() + " CREATED IN HOST: " + (this.getId() - 2));
        return true;
    }
}
