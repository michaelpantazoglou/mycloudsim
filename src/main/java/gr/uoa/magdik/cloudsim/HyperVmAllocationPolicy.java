package gr.uoa.magdik.cloudsim;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.*;
import org.jfree.data.xy.XYSeries;
import static gr.uoa.magdik.cloudsim.HyperPowerHost.*;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by tchalas on 1/22/15.
 */
public class HyperVmAllocationPolicy extends PowerVmAllocationPolicyAbstract {
    int vmcounter = 0;
    public HyperVmAllocationPolicy(List<? extends Host> list) {
        super(list);
        tobeoffHosts = new ArrayList<>();
        tobeonHosts = new ArrayList<>();
        offHosts = new ArrayList<>();
        onHosts = new CopyOnWriteArrayList<>();
        stateOVER = new XYSeries("Overutilized Hosts");
        stateUNDER = new XYSeries("Underutilized Hosts");
        stateIDLE = new XYSeries("Idle Hosts");
        stateOK = new XYSeries("OK Hosts");
        stateOFF = new XYSeries("OFF Hosts");
    }

    HyperPowerDatacenter datacenter;
    PrintWriter writer;
    public List<Host> tobeoffHosts;
    public List<Host> tobeonHosts;
    private XYSeries stateOVER;
    private XYSeries stateUNDER;
    private XYSeries stateIDLE;
    private XYSeries stateOK;
    private XYSeries stateOFF;
    public CopyOnWriteArrayList<Host> getOnHosts() {
        return onHosts;
    }
    public CopyOnWriteArrayList<Host> onHosts;
    public static List<Host> offHosts;
    public HashMap <Integer, Integer> inithostsvm;
    public HyperPowerDatacenter getDatacenter() {
        return datacenter;
    }
    public void setDatacenter(HyperPowerDatacenter datacenter) {
        this.datacenter = datacenter;
    }

    public XYSeries getStateOVER() {
        return stateOVER;
    }
    public XYSeries getStateOFF() {
        return stateOFF;
    }
    public XYSeries getStateIDLE() {
        return stateIDLE;
    }
    public XYSeries getStateOK() {
        return stateOK;
    }
    public XYSeries getStateUNDER() {return stateUNDER;}


    public boolean placeVminHost(Vm vm, Host host)
    {
        vmcounter++;
        HyperPowerHost hs = (HyperPowerHost) host;
        if(hs.getPowerState() == PowerState.OFF)
            return false;
        if(hwReqMet(hs,vm)) {
            if (host.vmCreate(vm))
            {
                getVmTable().put(vm.getUid(), host);
                if(hs.getSynchronizer() != null) {
                    synchronized (hs.getSynchronizer()) {
                        hs.getSynchronizer().notify();
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean newallocateHostForVm(HyperPowerVm vm, HyperPowerHost host)
    {
        ArrayList<HyperPowerHost> okcache = new ArrayList<>();
        ArrayList<HyperPowerHost> undercache = new ArrayList<>();
        ArrayList<HyperPowerHost> idlecache = new ArrayList<>();
        ArrayList<HyperPowerHost> offcache = new ArrayList<>();
        ArrayList<HyperPowerHost> overcache = new ArrayList<>();
        ArrayList<HyperPowerHost> L;
        boolean overnodes = true;
        SortedSet<Map.Entry<HyperPowerHost, ArrayList<Integer>>> sortcache = host.sortcachebyproximity();
        for(Map.Entry<HyperPowerHost, ArrayList<Integer>> entry : sortcache)
        {
            HyperPowerHost h = entry.getKey();
            if(h.getPowerState() != PowerState.OVERU)
            {    overnodes = false;}
            else
            {
                overcache.add(h);
            }
            if(h.getPowerState() == PowerState.OK)
                okcache.add(h);
            else if(h.getPowerState() == PowerState.UNDERU)
                undercache.add(h);
            else if(h.getPowerState() == PowerState.IDLE)
                idlecache.add(h);
            else if(h.getPowerState() == PowerState.OFF)
                offcache.add(h);
        }
        if(overnodes) {
            return false;
        }
        if(okcache.size() > 0) {
            for (HyperPowerHost h : okcache)
            {
                if(vm.getPower() < h.Pmax - h.getPower())
                {
                    if(hwReqMet(h,vm)) {

                        if (h.vmCreate(vm))
                        {
                            //System.out.println(CloudSim.clock() + " VM" + vm.getId() + " CREATED IN HOST: " + (h.getId() - 2));
                            getVmTable().put(vm.getUid(), h);
                            if(h.getSynchronizer() != null) {
                                synchronized (h.getSynchronizer()) {
                                    h.getSynchronizer().notify();
                                }
                            }
                            return true;
                        }
                    }
                }
            }
            if(okcache.size() == host.nodesh.size())
            {
                HyperPowerHost h = okcache.get(0);
                if(hwReqMet(h, vm)) {
                    if (h.vmCreate(vm))
                    {
                        //h.setVmstatechange(true);
                        if(h.getSynchronizer() != null) {
                            synchronized (h.getSynchronizer()) {
                                h.getSynchronizer().notify();
                            }
                        }
                        getVmTable().put(vm.getUid(), h);
                        return true;
                    }
                }
            }
        }
        L = undercache;
        if(L.size() == 0)
        {
            L = idlecache;
            if(idlecache.size() == 0)
            {
                L = offcache;
                if(L.size() == 0) {
                    //System.out.println(CloudSim.clock() + " VM" + vm.getId() + " FAILED lsize ");
                    return false;
                }
            }
        }
        HyperPowerHost h = L.get(0);
        if(hwReqMet(h,vm)) {
            if (h.vmCreate(vm))
            {
                if(h.getPowerState() == PowerState.OFF)
                {
                    h.switchOn();
                    offHosts.remove(h);
                }
                //System.out.println(CloudSim.clock() + " VM" + vm.getId() + " CREATED IN HOST: " + (h.getId() - 2));
                getVmTable().put(vm.getUid(), h);
                if(h.getSynchronizer() != null) {
                    synchronized (h.getSynchronizer()) {
                        h.getSynchronizer().notify();
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allocateHostForVm(Vm vm) {
        HyperPowerVm hvm = (HyperPowerVm) vm;
        //initalize vm in hosts if we have plan
        if(inithostsvm != null && hvm.getDelay() == 0)
        {
            Iterator it = inithostsvm.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                HyperPowerHost hp = (HyperPowerHost) getHostList().get((Integer) pair.getKey());
                if((int) pair.getValue() == -1)
                {
                    offHosts.add(hp);
                    hp.switchOff();
                }
                if(hp.getVmList().size() < (int) pair.getValue())
                {
                    return (placeVminHost(vm, hp));
                }
            }
        }
        //otherwise choose a host as vm initiator
        int index = (int) (Math.random() * this.getOnHosts().size());
        return allocateHostForVm(vm, getOnHosts().get(index));
    }

    @Override
    public boolean allocateHostForVm(Vm vm, Host host) {
        HyperPowerHost h = (HyperPowerHost) host;
        HyperPowerVm v = (HyperPowerVm) vm;
        return newallocateHostForVm(v, h);
    }

    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList)
    {
        return synchronizeHosts();
    }

    @Override
    public void deallocateHostForVm(Vm vm) {
        HyperPowerHost host = (HyperPowerHost) getVmTable().remove(vm.getUid());
        if (host != null) {
            host.vmDestroy(vm);
            if(host.getVmList().size() == 0) host.switchOff();
        }
    }

    @Override
    public Host getHost(Vm vm) {
        return vm.getHost();
    }

    @Override
    public Host getHost(int vmId, int userId) {
        return getVmTable().get(Vm.getUid(userId, vmId));
    }

    //check is a host meets the requirments for a vm
    public static boolean hwReqMet(Host host, Vm vm)
    {
        return host.isSuitableForVm(vm);
    }

    public List<Map<String, Object>> synchronizeHosts()
    {
        int overhosts = 0; int underhosts = 0; int idlehosts = 0; int okhosts = 0;
        List<Map<String, Object>> migrationMap = new ArrayList<>();
        int nvm = 0;
        double time = CloudSim.clock();
     //  monitorDatacenter();
      //  getDatacenter().onhoststimelog.println(CloudSim.clock() + "\t" + getOnHosts().size());
        for (Host h : getOnHosts())
        {
            HyperPowerHost host = (HyperPowerHost) h;
            if(!offHosts.contains(host))
            {
                Log.write(" --- HOST " + (host.getId()-2) + " : " + host.getVmList().size() + " VMs and Power " + host.getPower() + " ---");
            }

                if (host.getPowerState() == PowerState.OK)
                    okhosts++;
                else if (host.getPowerState() == PowerState.IDLE)
                    idlehosts++;
                else if (host.getPowerState() == PowerState.OVERU)
                    overhosts++;
                else if (host.getPowerState() == PowerState.UNDERU)
                    underhosts++;

            host.buildPowermap();
            nvm += host.getVmList().size();
            host.sortNeighbors();
            if(host.getPowerState() == PowerState.OFF)
            {
                continue;
            }
            if(host.getVmList().size() == 0)
            {
                host.switchOff();
                offHosts.add(host);
                continue;
            }
            if(host.getPowerState() == PowerState.OVERU)
            {
                if(host.getSynchronizer() == null)
                {
                    host.setSynchronizer(new Synchronizer());
                    host.getSynchronizer().setHost(host);
                }
                host.getSynchronizer().setMode(0);
                host.getSynchronizer().setSynching(true);
                if(host.getSynchronizer().getState() == Thread.State.NEW) {
                    host.getSynchronizer().start();
                    while (!host.getSynchronizer().started)
                    {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            else if(host.getPowerState() == PowerState.UNDERU)
            {
                if(host.getSynchronizer() == null)
                {
                    host.setSynchronizer(new Synchronizer());
                    host.getSynchronizer().setHost(host);
                }
                host.getSynchronizer().setMode(1);
                host.getSynchronizer().setSynching(true);
                host.getSynchronizer().setStarted(false);
                if(host.getSynchronizer().getState() == Thread.State.NEW) {
                    host.getSynchronizer().start();
                    while (!host.getSynchronizer().started)
                    {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        if(time < 70 || time - 1800 > datacenter.getHours() * 1800) {
            //int hours = datacenter.getHours();
            stateOVER.add(time, overhosts);
            stateIDLE.add(time, idlehosts);
            stateOK.add(time, okhosts);
            stateUNDER.add(time, underhosts);
            stateOFF.add(time, offHosts.size());
            if(time > 70) {
                datacenter.setHours(datacenter.getHours() + 1);
            }
        }
        return migrationMap;
    }

    /*public void monitorDatacenter()
    {
        List<Host> okHosts;
        List<Host> underHosts;
        List<Host> idleHosts;
        List<Host> overHosts;
        List<Host> currentoffHosts;
        okHosts = new ArrayList<>();
        underHosts = new ArrayList<>();
        idleHosts = new ArrayList<>();
        overHosts = new ArrayList<>();
        currentoffHosts = new ArrayList<>();
        ArrayList<Host> tempon = new ArrayList<>();
        for (Host h : getOnHosts())
        {
            if(!tempon.contains(h))
            {
                tempon.add(h);
            }
        }
        getOnHosts().clear();
        getOnHosts().addAll(tempon);
        for (Host h : getOnHosts())//this. <HyperPowerHost> getHostList())
        {
            HyperPowerHost host = (HyperPowerHost) h;
            host.buildPowermap();
            if(!offHosts.contains(host))
            {
                Log.write(" --- HOST " + (host.getId()-2) + " : " + host.getVmList().size() + " VMs and Power " + host.getTempPower() + " ---");
            }
            if(host.getPower() == 0)
            {
                currentoffHosts.add(host);
                continue;
            }
            HyperPowerHost.PowerState s = host.getPowerState();
            if(s == PowerState.UNDERU)
            {
                underHosts.add(host);
            }
            else if(s == PowerState.IDLE)
            {
                idleHosts.add(host);
            }
            else if(s == PowerState.OK)
            {
                okHosts.add(host);
            }
            else if(s == PowerState.OVERU)
            {
                overHosts.add(host);
            }
        }
    }*/
}