package gr.uoa.magdik.cloudslim;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.*;
import static gr.uoa.magdik.cloudslim.HyperPowerHost.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by tchalas on 1/22/15.
 */
public class HyperVmAllocationPolicy extends PowerVmAllocationPolicyAbstract {
    int vmcount = 0;
    public HyperVmAllocationPolicy(List<? extends Host> list) {
        super(list);
        visitedHosts = new ArrayList<>();
        tobeoffHosts = new ArrayList<>();
        tobeonHosts = new ArrayList<>();
        offHosts = new ArrayList<>();
        onHosts = new CopyOnWriteArrayList<>();
        TemponHosts = new ArrayList<>();
        vmsplaced = false;
        /*try {
            writer = new PrintWriter("results", "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }*/
    }

    PrintWriter writer;
    //LISTS FOR VISITED AND SWITCHED OFF HOSTS
    public List<Host> visitedHosts;
    public List<Host> tobeoffHosts;
    public List<Host> tobeonHosts;

    public CopyOnWriteArrayList<Host> getOnHosts() {
        return onHosts;
    }

    public void setOnHosts(CopyOnWriteArrayList<Host> onHosts) {
        this.onHosts = onHosts;
    }

    public CopyOnWriteArrayList<Host> onHosts;
    public ArrayList<Host> TemponHosts;
    public static List<Host> offHosts;
    public HashMap <Integer, Integer> inithostsvm;

    public HyperPowerDatacenter getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(HyperPowerDatacenter datacenter) {
        this.datacenter = datacenter;
    }

    boolean vmsplaced;
    HyperPowerDatacenter datacenter;
    int round =  0;

    public boolean placeVminHost(Vm vm, Host host)
    {
        vmcount++;
        //System.out.println("pLACING " + vm.getId() + " to HOST" + (host.getId() - 2));
        if(vm.getHost() != null)
        {
          //  throw new IllegalArgumentException("VM " + vm.getId() + " ALready assigned");
        }
        HyperPowerHost hs = (HyperPowerHost) host;
        if(hs.getPowerState() == PowerState.OFF)
        {
            return false;
            //throw new IllegalArgumentException("HOST OFF");
            //System.exit(-2);
        }

        //if(hs.getPowerState() != PowerState.OVERU)
        //{

        if(hwReqMet(hs,vm)) {

            if (host.vmCreate(vm))
            {
                getVmTable().put(vm.getUid(), host);
                if(hs.getSynchronizer() != null) {
                    synchronized (hs.getSynchronizer()) {
                        hs.getSynchronizer().notify();
                    }
                }
               // System.out.println("VM" + vm.getId() + " placed in Host" + (host.getId() - 2));
                return true;
            }
        }
        // }
        return false;
    }

    public boolean evaluatemigration(Vm vm, Host host)
    {
        HyperPowerHost hs = (HyperPowerHost) host;
        if(hs.getPowerState() == PowerState.OFF)
        {
            return false;
            //throw new IllegalArgumentException("HOST OFF");
            //System.exit(-2);
        }
        return true;
    }

    public boolean newallocateHostForVm(HyperPowerVm vm, HyperPowerHost host)
    {
        //ArrayList<HyperPowerHost> hostcache = (ArrayList<HyperPowerHost>) host.nodesh.keySet();
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
        if(overnodes)
            return false;
        if(okcache.size() > 0) {
            for (HyperPowerHost h : okcache)
            {
                if(vm.getPower() < h.Pmax - h.getPower())
                {
                    if(hwReqMet(h,vm)) {

                        if (h.vmCreate(vm))
                        {
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
                if(L.size() == 0)
                    return false;
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


    public boolean allocateHostForVm(Vm vm, Host host, List<Host> visitedHosts, List<Host> offHosts)
    {
        if(vm.getHost() != null)
        {
            System.exit(-1);
        }
        HyperPowerHost hs = (HyperPowerHost) host;
        if(hs.getPowerState() == PowerState.OFF)
        {
            System.exit(-2);
        }

        if(hs.getPowerState() != PowerState.OVERU)
        {
            if(hwReqMet(hs,vm)) {

                if (host.vmCreate(vm))
                {
                    getVmTable().put(vm.getUid(), host);
                    return true;
                }
            }
        }
        visitedHosts.add(hs);
        Iterator it = hs.getNeighbors().entrySet().iterator();
        while (it.hasNext()) {

            Map.Entry pairs = (Map.Entry) it.next();
            HyperPowerHost neighbor = (HyperPowerHost) pairs.getValue();
            if (neighbor.getPowerState() != HyperPowerHost.PowerState.OVERU && neighbor.getPowerState() != HyperPowerHost.PowerState.OFF && !visitedHosts.contains(neighbor)) {
                return allocateHostForVm(vm, neighbor, visitedHosts, offHosts);
            }
            if (neighbor.getPowerState() == HyperPowerHost.PowerState.OFF && !offHosts.contains(neighbor)) {
                tobeoffHosts.add(neighbor);
            }
        }
        it = hs.getNeighbors().entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry pairs = (Map.Entry) it.next();
            HyperPowerHost neighbor = (HyperPowerHost) pairs.getValue();
            if(!offHosts.contains(neighbor) && !visitedHosts.contains(neighbor))
            {
                return allocateHostForVm(vm, neighbor, visitedHosts, offHosts);
            }
        }

        if(!offHosts.isEmpty())
        {
            HyperPowerHost h = (HyperPowerHost) offHosts.get(0);
            HyperPowerDatacenter pd = (HyperPowerDatacenter) h.getDatacenter();

            offHosts.remove(h);
            pd.wakeuphost(h, (HyperPowerVm) vm);
        }
        return false;
    }


    public void turnonhost_cratevm()
    {

    }

    public void initoffhosts()
    {
        if(inithostsvm != null) {
            Iterator it = inithostsvm.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                HyperPowerHost hp = (HyperPowerHost) getHostList().get((Integer) pair.getKey());
                if ((int) pair.getValue() == -1) {
                    hp.switchOff();
                    offHosts.add(hp);
                }
            }
        }
    }

    @Override
    public boolean allocateHostForVm(Vm vm) {
        HyperPowerVm hvm = (HyperPowerVm) vm;
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

            //int index = (int) (Math.random() * this.getHostList().size());
            //return allocateHostForVm(vm, getHostList().get(index));
        int index = (int) (Math.random() * this.getOnHosts().size());
        return allocateHostForVm(vm, getOnHosts().get(index));
    }

    @Override
    public boolean allocateHostForVm(Vm vm, Host host) {

        //return allocateHostForVm(vm, host, visitedHosts, tobeoffHosts);
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
        Host host = getVmTable().remove(vm.getUid());
        if (host != null) {
            host.vmDestroy(vm);
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

    public static boolean hwReqMet(Host host, Vm vm)
    {
        if(host.isSuitableForVm(vm))
        {
            return true;
        }
        return false;
    }

    /**
     * Gets the over utilized hosts.
     *
     * @r+eturn the over utilized hosts
     */
    protected List<PowerHostUtilizationHistory> getOverUtilizedHosts() {
        return null;
    }

    public List<Map<String, Object>> synchronizeHosts()
    {
        Thread t = Thread.currentThread();
        t.setName("Admin Thread");
        int partial = 0;
        int fl = 0;
        int flvm =  0;
        List<Map<String, Object>> migrationMap = new ArrayList<>();
        int nvm = 0;
     //  monitorDatacenter();
        getDatacenter().onhoststimelog.println(CloudSim.clock() + "\t" + getOnHosts().size());
        for (Host h : getOnHosts())
        {
            if(h.getVmList().size() > 70)
            {
              //  System.out.println("ddd " + CloudSim.clock());
            }
            HyperPowerHost host = (HyperPowerHost) h;
            if(!offHosts.contains(host))
            {
                //Log.write(" --- HOST " + (host.getId()-2) + " : " + host.vmsaftercycle + " VMs and Power " + host.getTempPower() + " ---");
                Log.write(" --- HOST " + (host.getId()-2) + " : " + host.getVmList().size() + " VMs and Power " + host.getPower() + " ---");
            }
            host.buildpowermap();
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
                partial++;
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
        return migrationMap;
    }

    public void monitorDatacenter()
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
        int nvm = 0;
        int nh = 0;
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

            host.buildpowermap();

            if(!offHosts.contains(host))
            {
                //Log.write(" --- HOST " + (host.getId()-2) + " : " + host.vmsaftercycle + " VMs and Power " + host.getTempPower() + " ---");
                Log.write(" --- HOST " + (host.getId()-2) + " : " + host.getVmList().size() + " VMs and Power " + host.getTempPower() + " ---");
            }
            else
            {
                //Log.write(" --- HOST " + (host.getId()-2) + " is off ");

            }
            if(host.getPower() == 0)
            {
                currentoffHosts.add(host);
                continue;
            }
            nvm += host.getVmList().size();
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
    }
}