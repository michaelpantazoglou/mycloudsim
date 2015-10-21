package gr.uoa.magdik.cloudslim;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
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
    public static List<Host> offHosts;
    public HashMap <Integer, Integer> inithostsvm;
    HyperPowerDatacenter datacenter;
    int round =  0;

    public boolean placeVminHost(Vm vm, Host host)
    {
        vmcount++;
        System.out.println("pLACING to HOST" + (host.getId() - 2));
        if(vm.getHost() != null)
        {
            throw new IllegalArgumentException("VM ALready assigned");
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
        vm.setInMigration(false);
        if(hwReqMet(hs,vm)) {

            if (host.vmCreate(vm))
            {
                getVmTable().put(vm.getUid(), host);
                System.out.println("VM" + vm.getId() + " placed in Host" + (host.getId() - 2));
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

        ArrayList<HyperPowerHost> L;
        boolean overnodes = true;
        SortedSet<Map.Entry<HyperPowerHost, ArrayList<Integer>>> sortcache = host.sortcachebyproximity();
        for(Map.Entry<HyperPowerHost, ArrayList<Integer>> entry : sortcache)
        {
            HyperPowerHost h = entry.getKey();
            if(h.getPowerState() != PowerState.OVERU)
                overnodes = false;
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
                if(vm.getPower() >= h.Pmax - h.getPower())
                {
                    if(hwReqMet(h,vm)) {

                        if (h.vmCreate(vm))
                        {
                            getVmTable().put(vm.getUid(), h);
                            return true;
                        }
                    }
                }
            }
            if(okcache.size() == host.nodesh.size())
            {
                HyperPowerHost h = okcache.get(0);
                if(hwReqMet(h,vm)) {
                    if (h.vmCreate(vm))
                    {
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
                if(L.size() > 0)
                    return false;
            }
        }
        HyperPowerHost h = L.get(0);
        if(hwReqMet(h,vm)) {
            if (h.vmCreate(vm))
            {
                getVmTable().put(vm.getUid(), h);
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
        if(inithostsvm != null)
        {
            Iterator it = inithostsvm.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                HyperPowerHost hp = (HyperPowerHost) getHostList().get((Integer) pair.getKey());
                if((int) pair.getValue() == -1)
                {
                    hp.switchOff();
                }

                if(hp.getVmList().size() < (int) pair.getValue())
                {
                    return (placeVminHost(vm, hp));
                }
            }
        }
        else {
            int index = (int) (Math.random() * this.getHostList().size());
            return allocateHostForVm(vm, getHostList().get(index));
        }
        return false;
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
        //System.out.println("DEAllocating " + vm.getId());
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
        //System.out.println("Getting host");
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
        // set thread priority to 1
        //t.setPriority(1);
        //PROBLEM WITH MIGRATION DALEY
        int partial = 0;
        int fl = 0;
        int flvm =  0;
        List<Map<String, Object>> migrationMap = new ArrayList<>();
        System.out.println("SYNCHRONIZE " + getHostList().size());
        int nvm = 0;

        monitorDatacenter();
        for (HyperPowerHost host : this. <HyperPowerHost> getHostList())
        {
            nvm += host.getVmList().size();
            //host.sendheartbeats();
            host.sortNeighbors();
            if(host.getPowerState() == PowerState.OFF || host.getVmList().size() == 0)
            {
                continue;
            }
            if(host.getPowerState() == PowerState.OVERU)
            {
                //List<Map<String, Object>> mp = partialVmMigration(host);
                if(host.getSynchronizer() == null)
                {
                    host.setSynchronizer(new Synchronizer());
                    host.getSynchronizer().setHost(host);
                }
                host.getSynchronizer().setMode(0);
                host.getSynchronizer().setSynching(true);
                if(host.getSynchronizer().getState() == Thread.State.NEW) {
                    System.out.println("starting threee");
                    host.getSynchronizer().start();
                }
                partial++;

                //partial migration. migrate until below max
                //if(mp!=null) {
                 //   migrationMap.addAll(mp);
                //}
            }
            else if(host.getPowerState() == PowerState.UNDERU)
            {
                /*List<Map<String, Object>> mf = fullVmMigration(host);
                fl++;
                flvm += host.getVmList().size();
                //migrate all
                if(mf!=null) {
                    migrationMap.addAll(mf);
                }*/
                if(host.getSynchronizer() == null)
                {
                    host.setSynchronizer(new Synchronizer());
                    host.getSynchronizer().setHost(host);
                }
                host.getSynchronizer().setMode(1);
                host.getSynchronizer().setSynching(true);
                if(host.getSynchronizer().getState() == Thread.State.NEW) {
                    System.out.println("starting threee2");
                    host.getSynchronizer().start();
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
        for (HyperPowerHost host : this. <HyperPowerHost> getHostList())
        {
            if(!offHosts.contains(host))
            {
                //Log.write(" --- HOST " + (host.getId()-2) + " : " + host.vmsaftercycle + " VMs and Power " + host.getTempPower() + " ---");
                Log.write(" --- HOST " + (host.getId()-2) + " : " + host.getVmList().size() + " VMs and Power " + host.getTempPower() + " ---");
            }
            else
            {
                Log.write(" --- HOST " + (host.getId()-2) + " is off ");

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

/*
    public static List<Map<String, Object>> partialVmMigration(HyperPowerHost h)
    {
        //o algorithnos einai lathos den prepei na kanei olous tous geitones, mporei se kapoion na sthlei dyo fores5
        //System.out.println("PAPARTM1");
        List<Map<String, Object>> migrationMap = new ArrayList<>();
        SortedSet<Map.Entry<HyperPowerHost, ArrayList<Integer>>> descendsortedneighbors = new TreeSet<Map.Entry<HyperPowerHost, ArrayList<Integer>>>(
                new Comparator<Map.Entry<HyperPowerHost, ArrayList<Integer>>>() {
                    @Override
                    public int compare(Map.Entry<HyperPowerHost, ArrayList<Integer>> e1,
                                       Map.Entry<HyperPowerHost, ArrayList<Integer>> e2) {
                        int ord = Double.compare(e1.getKey().getPower(), e2.getKey().getPower());
                        if(ord == 1)
                            return -1;
                        return 1;
                    }
                });
        descendsortedneighbors.addAll((Collection<? extends Map.Entry<HyperPowerHost, ArrayList<Integer>>>) h.nodesh.entrySet());

        ArrayList vms = (ArrayList) h.getVmList();
        int idx = 0;
        HyperPowerVm vm;
        int vmcount = vms.size();
        boolean con = true;
        PowerState s;
        while(con) {
            s = h.getPowerStatebyPower(h.getTempPower());

            if (idx == vms.size()) {
                con = false;
                break;
            }
            vm = (HyperPowerVm) vms.get(idx++);
            for (Map.Entry<HyperPowerHost, ArrayList<Integer>> entry : descendsortedneighbors) {

                if (vmcount == 0 || s != PowerState.OVERU) {
                    return migrationMap;
                }

                if (vm.getPower() >= entry.getKey().Pmax - entry.getKey().getTempPower()) {
                    continue;
                }
                if(hwReqMet(entry.getKey(), vm))
                {
                    if(entry.getKey().getPowerState() == PowerState.OFF)
                    {
                        entry.getKey().tobeon = true;  // switchOn();
                        offHosts.remove(entry.getKey());
                    }
                    Map<String, Object> migrate = new HashMap<String, Object>();
                    migrate.put("vm", vm);
                    migrate.put("host", entry.getKey());
                    entry.getKey().waitmigrate++;
                    migrationMap.add(migrate);
                    vmcount--;

                    entry.getKey().vmsaftercycle += 1;
                    h.vmsaftercycle -= 1;
                    //System.out.println("from Host" + (h.getId() - 2) + " with " +
                      //      "" + h.vmsaftercycle + " VMs to Host" + (entry.getKey().getId() - 2) + " with " + entry.getKey().vmsaftercycle + " Vms");
                    System.out.println("--partial-- from Host" + (h.getId() - 2) + " with " +
                            "" + h.getVmList().size() + " VMs to Host" + (entry.getKey().getId() - 2) + " with " + entry.getKey().getVmList().size() + " Vms");
                    break;
                }
            }
        }

*/


        /*for(Map.Entry<Integer, HyperPowerHost> entry : sortedneighbors) {
            if(entry.getValue().getPowerState() == PowerState.OVERU)
            {
                continue;
            }
            int idx = 0;
            HyperPowerVm vm;
            int vmcount = vms.size();
            while(true)
            {
                if(idx == vms.size())
                {
                    break;
                }
                vm = (HyperPowerVm) vms.get(idx++);
                if(vmcount == 0 ||  h.getPowerState() != PowerState.OVERU)
                {
                    return null;
                }

                if(vm.getPower() >= entry.getValue().Pmax - entry.getValue().getPower())
                {
                    continue;
                }
                if(hwReqMet(entry.getValue(), vm))
                {
                    if(entry.getValue().getPowerState() == PowerState.OFF)
                    {
                        entry.getValue().tobeon = true;  // switchOn();
                        offHosts.remove(entry.getValue());
                    }
                    Map<String, Object> migrate = new HashMap<String, Object>();
                    migrate.put("vm", vm);
                    migrate.put("host", entry.getValue());
                    entry.getValue().waitmigrate++;
                    migrationMap.add(migrate);
                    vmcount--;
                }
            }
        }*/
     //   return migrationMap;
    //}

    public List<Map<String, Object>> fullVmMigration(HyperPowerHost h)
    {
        List<Map<String, Object>> migrationMap = new ArrayList<>();
        SortedSet<Map.Entry<HyperPowerHost, ArrayList<Integer>>> sortedneighbors = new TreeSet<Map.Entry<HyperPowerHost, ArrayList<Integer>>>(
                new Comparator<Map.Entry<HyperPowerHost, ArrayList<Integer>>>() {
                    @Override
                    public int compare(Map.Entry<HyperPowerHost, ArrayList<Integer>> e1,
                                       Map.Entry<HyperPowerHost, ArrayList<Integer>> e2) {
                        int ord = Double.compare(e1.getKey().getPower(), e2.getKey().getPower());
                        if(ord == 1)
                            return -1;
                        return 1;
                    }
                });
        sortedneighbors.addAll((Collection<? extends Map.Entry<HyperPowerHost, ArrayList<Integer>>>) h.nodesh.entrySet());
        CopyOnWriteArrayList vms = (CopyOnWriteArrayList) h.getVmList();

        int idx = 0;
        int vmcount = vms.size();
        HyperPowerVm vm;
        PowerState s;
        while (true) {
            s = h.getPowerStatebyPower(h.getTempPower());
            if (idx == vms.size()) {
                break;
            }
            vm = (HyperPowerVm) vms.get(idx++);
            for(Map.Entry<HyperPowerHost, ArrayList<Integer>> entry : sortedneighbors) {
                if (entry.getKey().getPowerState() == HyperPowerHost.PowerState.OVERU || entry.getKey().getPowerState() == PowerState.OFF || tobeoffHosts.contains(entry.getValue()) || entry.getKey().getVmList().size() == 0) {
                    continue;
                }
                if (h.getVmList().size() == 0 || s == PowerState.OVERU)
                {
                    break;
                }

                if (vm.getPower() >= entry.getKey().Pmax - entry.getKey().getTempPower()) {
                    continue;
                }
                //test if condition are met and migrate
                if (hwReqMet(entry.getKey(), vm)) {
                    HyperPowerHost mh = entry.getKey();
                    Map<String, Object> migrate = new HashMap<String, Object>();
                    migrate.put("vm", vm);
                    migrate.put("host", mh);
                    mh.waitmigrate++;
                    mh.vmsaftercycle += 1;
                    h.vmsaftercycle -= 1;
                    migrationMap.add(migrate);
                    vmcount--;
                    System.out.println("from Host" + (h.getId() - 2) + " with " +
                            "" + h.getVmList().size() + " VMs to Host" + (entry.getKey().getId() - 2) + " with " + entry.getKey().getVmList().size() + " Vms");

                    break;
                }


            }
            if (vmcount == 0 && h.waitmigrate == 0) {

                tobeoffHosts.add(h);
                h.tobeoff = true;
                break;
            }
        }

        /*
        for(Map.Entry<Integer, HyperPowerHost> entry : sortedneighbors) {
            if (entry.getValue().getPowerState() == HyperPowerHost.PowerState.OVERU || entry.getValue().getPowerState() == PowerState.OFF  || tobeoffHosts.contains(entry.getValue())) {
                continue;
            }
            int idx = 0;
            int vmcount = vms.size();
            HyperPowerVm vm;
            while (true) {
                if(idx == vms.size())
                {
                    break;
                }
                vm = (HyperPowerVm) vms.get(idx++);
                if (h.getVmList().size() == 0 || entry.getValue().getPowerState() == PowerState.OVERU)
                {
                    break;
                }

                if (vm.getPower() >= entry.getValue().Pmax - entry.getValue().getPower()) {
                    continue;
                }
                //test if condition are met and migrate
                if (hwReqMet(entry.getValue(), vm)) {
                    HyperPowerHost mh = entry.getValue();
                    Map<String, Object> migrate = new HashMap<String, Object>();
                    migrate.put("vm", vm);
                    migrate.put("host", mh);
                    mh.waitmigrate++;
                    migrationMap.add(migrate);
                    vmcount--;
                }
            }
            if (vmcount == 0 && h.waitmigrate == 0) {

                tobeoffHosts.add(h);
                h.tobeoff = true;
                break;
            }
        }*/
        return migrationMap;
    }



}
