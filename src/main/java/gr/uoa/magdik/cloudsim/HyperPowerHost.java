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

    public HashMap<Integer, Double> cyclepowers;
    private PowerModel p;   //The power model
    public boolean vmstatechange = true;
    public int waitmigrate = 0;
    public Synchronizer synchronizer;
    double startsynchtime = 0 ;
    private Map<Integer, HyperPowerHost> neighbors; //Contains the hypercube neighbors of this host.
    public Map<HyperPowerHost, ArrayList<Integer>> nodesh;
    private HashMap<HyperPowerHost, ArrayList<Integer>> nodenew;
    private double utilizationMips;     //The utilization mips.
    public double Pidle = 160;
    public double Pmin = 180;
    public double Pmax = 250;
    public List<NetworkPacket> packetTosendLocal;
    public List<NetworkPacket> packetTosendGlobal;
    public List<NetworkPacket> packetrecieved;
    public double memory;
    public Switch sw; // Edge switch in general
    public double bandwidth;// latency
    public int vmsaftercycle = 0;
    SortedSet<Map.Entry<HyperPowerHost, ArrayList<Integer>>> descendsortedneighbors;
    Map<HyperPowerHost, ArrayList<Integer>> heartnodes;
    PowerState s;
    public enum PowerState {
        OFF, IDLE, UNDERU, OK, OVERU
    }
    @Override
    public int compareTo(Object o) {
        PowerHost p = (PowerHost) o;
        return Double.compare(this.getPower(), p.getPower());
    }

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
        s = PowerState.IDLE;
        nodesh = new HashMap<HyperPowerHost, ArrayList<Integer>>();
        cyclepowers = new HashMap<>();
        nodenew = new HashMap<HyperPowerHost, ArrayList<Integer>>();
        heartnodes = new HashMap<>();
        setName("HyperHost" + (getId() - 2));
        descendsortedneighbors = new TreeSet<>(
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
    }

    public double getStartsynchtime() {
        return startsynchtime;
    }

    public void setStartsynchtime(double startsynchtime) {
        this.startsynchtime = startsynchtime;
    }

    public boolean isVmstatechange() {
        return vmstatechange;
    }

    public void setVmstatechange(boolean vmstatechange) {
        this.vmstatechange = vmstatechange;
    }

    public Synchronizer getSynchronizer() {
        return synchronizer;
    }

    public void setSynchronizer(Synchronizer synchronizer) {
        this.synchronizer = synchronizer;
        if(synchronizer != null)
            this.synchronizer.host = this;
    }

    /*@Override
    public double updateVmsProcessing(double currentTime) {
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
    }*/


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


    @Override
    public double getPower() {
        if (s == PowerState.OFF) {
            return 0;
        }
        double p = Pidle;
        for (Vm vm : getVmList()) {
            HyperPowerVm pvm = (HyperPowerVm) vm;
            p += pvm.getPower();
        }
        return p;
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
     * Sets the power model.
     *
     * @param powerModel the new power model
     */
    protected void setPowerModel(PowerModel powerModel) {
        this.p = powerModel;
    }


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
        return Pidle + this.vmsaftercycle * 5;
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
        HyperVmAllocationPolicy hp = (HyperVmAllocationPolicy) getDatacenter().getVmAllocationPolicy();
        if(hp.getOnHosts().contains(this))
        {
            hp.getOnHosts().remove(this);
            HyperPowerDatacenter hpd  = (HyperPowerDatacenter) getDatacenter();
            hpd.incrementhostoffCount();
        }
    }

    public void switchOn() {
        this.setState(PowerState.IDLE);
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
        switch (ev.getTag()) {
            case HyperCloudSimTags.REMOVEVM:
                Vm vm = (Vm) ev.getData();
                vmDestroy(vm);
                break;
            case HyperCloudSimTags.HEARTBEAT:
                processHearbeat(ev);
                break;
            case HyperCloudSimTags.FORWARD_MSG:
        }
    }

    @Override
    public void shutdownEntity() {
        Log.printLine(getName() + " is shutting down...");
    }

    public void processHearbeat(SimEvent ev) {
        Map<? extends HyperPowerHost, ? extends ArrayList<Integer>> nnodes = (Map<? extends HyperPowerHost, ? extends ArrayList<Integer>>) ev.getData();
        heartnodes.putAll((Map<? extends HyperPowerHost, ? extends ArrayList<Integer>>) ev.getData());
    }

    public void buildPowermap()
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

    public void sortCachebyPower()
    {

        /*descendsortedneighbors = new TreeSet<>(
                new Comparator<Map.Entry<HyperPowerHost, ArrayList<Integer>>>() {
                    @Override
                    public int compare(Map.Entry<HyperPowerHost, ArrayList<Integer>> e1,
                                       Map.Entry<HyperPowerHost, ArrayList<Integer>> e2) {
                        int ord = Double.compare(e1.getKey().getPower(), e2.getKey().getPower());
                        if (ord == 1)
                            return -1;
                        return 1;
                    }
                });*/
        descendsortedneighbors.clear();
        descendsortedneighbors.addAll(nodesh.entrySet());
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
        PowerState s;
            ArrayList<Integer> vmids = new ArrayList<>();
            while (true) {
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
                            Map<String, Object> migrate = new HashMap<>();
                            migrate.put("vm", vm);
                            migrate.put("host", entry.getKey());
                            entry.getKey().waitmigrate++;
                            vm.setInMigration(true);

                            if (ps == PowerState.OFF) {
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
        ArrayList<Integer> vm_ids = new ArrayList<>();
        PowerState s;
        do {
            for (Vm rvm : getVmList()) {
                HyperPowerVm vm = (HyperPowerVm) rvm;
                if (vm_ids.contains(vm.getId())) {
                    continue;
                } else {
                    vm_ids.add(vm.getId());
                }
                s = getPowerStatebyPower(getPower());
                if (vm.isInMigration()) {
                    continue;
                }
                for (Map.Entry<HyperPowerHost, ArrayList<Integer>> entry : descendsortedneighbors) {
                    double currentTime = CloudSim.clock();
                    if (currentTime - getStartsynchtime() > 45) {
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
                    if (vm.getPower() >= entry.getKey().Pmax - npower) {//entry.getKey().getTempPower()) {
                        continue;
                    }

                    //test if condition are met and migrate
                    if (entry.getKey().isSuitableForVm(vm)) {
                        HyperPowerHost mh = entry.getKey();
                        Map<String, Object> migrate = new HashMap<>();
                        migrate.put("vm", vm);
                        migrate.put("host", mh);
                        mh.waitmigrate++;
                        mh.vmsaftercycle += 1;
                        vmsaftercycle -= 1;
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
        } while (true);
        return 1;
    }


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
            if(oldhost.getVmList().size() == 0)
            {
                hp.offHosts.add(oldhost);
                oldhost.switchOff();
            }
        }

        setStorage(getStorage() - vm.getSize());
        getVmList().add(vm);
        vm.setHost(this);
       // System.out.println(CloudSim.clock() + " VM" + vm.getId() + " CREATED IN HOST: " + (this.getId() - 2));
        return true;
    }
}
