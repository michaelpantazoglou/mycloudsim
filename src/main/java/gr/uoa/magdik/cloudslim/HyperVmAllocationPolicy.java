package gr.uoa.magdik.cloudslim;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.power.*;
import static gr.uoa.magdik.cloudslim.HyperPowerHost.*;

import java.util.*;

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

    }

    //LISTS FOR VISITED AND SWITCHED OFF HOSTS
    public List<Host> visitedHosts;
    public List<Host> tobeoffHosts;
    public List<Host> tobeonHosts;
    public List<Host> offHosts;
    public HashMap <Integer, Integer> inithostsvm;


    public boolean placeVminHost(Vm vm, Host host)
    {
        vmcount++;

        if(vm.getHost() != null)
        {
            System.exit(-1);
        }
        HyperPowerHost hs = (HyperPowerHost) host;
        if(hs.getState() == State.OFF)
        {
            System.exit(-2);
        }

        //if(hs.getState() != State.OVERU)
        //{
            if(hwReqMet(hs,vm)) {

                if (host.vmCreate(vm))
                {
                    getVmTable().put(vm.getUid(), host);
                    if(vmcount == 49)
                    {
                        System.out.println("l");
                        monitorDatacenter();
                    }
                    return true;
                }
            }
       // }


        return false;
    }


    public boolean allocateHostForVm(Vm vm, Host host, List<Host> visitedHosts, List<Host> offHosts)
    {
        if(vm.getHost() != null)
        {
            System.exit(-1);
        }
        HyperPowerHost hs = (HyperPowerHost) host;
        if(hs.getState() == State.OFF)
        {
            System.exit(-2);
        }

        if(hs.getState() != State.OVERU)
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
            if (neighbor.getState() != HyperPowerHost.State.OVERU && neighbor.getState() != HyperPowerHost.State.OFF && !visitedHosts.contains(neighbor)) {
                return allocateHostForVm(vm, neighbor, visitedHosts, offHosts);
            }
            if (neighbor.getState() == HyperPowerHost.State.OFF && !offHosts.contains(neighbor)) {
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

    @Override
    public boolean allocateHostForVm(Vm vm) {
        if(inithostsvm != null)
        {
            Iterator it = inithostsvm.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                HyperPowerHost hp = (HyperPowerHost) getHostList().get((Integer) pair.getKey());
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

        return allocateHostForVm(vm, host, visitedHosts, tobeoffHosts);
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

    public boolean hwReqMet(Host host ,Vm vm )
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
     * @return the over utilized hosts
     */
    protected List<PowerHostUtilizationHistory> getOverUtilizedHosts() {
        return null;
    }

    public List<Map<String, Object>> synchronizeHosts()
    {


        //PROBLEM WITH MIGRATION DALEY

        int partial = 0;
        int fl = 0;
        int flvm =  0;
        //monitorDatacenter();
        List<Map<String, Object>> migrationMap = new ArrayList<>();
        System.out.println("SYNCHRONIZE " + getHostList().size());
        int nvm = 0;

        for (HyperPowerHost host : this. <HyperPowerHost> getHostList())
        {
            nvm += host.getVmList().size();
            if(host.getState() == State.OFF || host.getVmList().size() == 0)
            {
                continue;
            }
            if(host.getState() == State.OVERU)
            {
                List<Map<String, Object>> mp = partialVmMigration(host);
                partial++;

                //partial migration. migrate until below max
                if(mp!=null) {
                    migrationMap.addAll(mp);
                }
            }
            else if(host.getState() == State.UNDERU)
            {
                List<Map<String, Object>> mf = fullVmMigration(host);
                fl++;
                flvm += host.getVmList().size();
                //migrate all
                if(mf!=null) {
                    migrationMap.addAll(mf);
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
            if(host.getPower() == 0)
            {
                currentoffHosts.add(host);
                continue;
            }
            nvm += host.getVmList().size();
            HyperPowerHost.State s = host.getState();
            if(s == State.UNDERU)
            {
                underHosts.add(host);
            }
            else if(s == State.IDLE)
            {
                idleHosts.add(host);
            }
            else if(s == State.OK)
            {
                okHosts.add(host);
            }
            else if(s == State.OVERU)
            {
                overHosts.add(host);
            }
        }
    }


    public List<Map<String, Object>> partialVmMigration(HyperPowerHost h)
    {
        //o algorithnos einai lathos den prepei na kanei olous tous geitones, mporei se kapoion na sthlei dyo fores5
        //System.out.println("PAPARTM1");
        List<Map<String, Object>> migrationMap = new ArrayList<>();
        SortedSet<Map.Entry<Integer, HyperPowerHost>> descendsortedneighbors = new TreeSet<Map.Entry<Integer, HyperPowerHost>>(
                new Comparator<Map.Entry<Integer, HyperPowerHost>>() {
                    @Override
                    public int compare(Map.Entry<Integer, HyperPowerHost> e1,
                                       Map.Entry<Integer, HyperPowerHost> e2) {
                        int ord = Double.compare(e1.getValue().getPower(), e2.getValue().getPower());
                        if(ord == 1)
                            return -1;
                        return 1;
                    }
                });
        descendsortedneighbors.addAll((Collection<? extends Map.Entry<Integer, HyperPowerHost>>) h.getNeighbors().entrySet());





        ArrayList vms = (ArrayList) h.getVmList();
        int idx = 0;
        HyperPowerVm vm;
        int vmcount = vms.size();
        boolean con = true;
        State s;
        while(con) {
            s = h.getStatebyPower(h.getTempPower(vmcount));

            if (idx == vms.size()) {
                con = false;
                break;
            }
            vm = (HyperPowerVm) vms.get(idx++);
            for (Map.Entry<Integer, HyperPowerHost> entry : descendsortedneighbors) {

                if (vmcount == 0 || s != State.OVERU) {
                    return migrationMap;
                }

                if (vm.getPower() >= entry.getValue().Pmax - entry.getValue().getPower()) {
                    continue;
                }
                if(hwReqMet(entry.getValue(), vm))
                {
                    if(entry.getValue().getState() == State.OFF)
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
                    break;
                }
            }
        }




        /*for(Map.Entry<Integer, HyperPowerHost> entry : sortedneighbors) {
            if(entry.getValue().getState() == State.OVERU)
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
                if(vmcount == 0 ||  h.getState() != State.OVERU)
                {
                    return null;
                }

                if(vm.getPower() >= entry.getValue().Pmax - entry.getValue().getPower())
                {
                    continue;
                }
                if(hwReqMet(entry.getValue(), vm))
                {
                    if(entry.getValue().getState() == State.OFF)
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
        return migrationMap;
    }

    public List<Map<String, Object>> fullVmMigration(HyperPowerHost h)
    {
        List<Map<String, Object>> migrationMap = new ArrayList<>();
        SortedSet<Map.Entry<Integer, HyperPowerHost>> sortedneighbors = new TreeSet<Map.Entry<Integer, HyperPowerHost>>(
                new Comparator<Map.Entry<Integer, HyperPowerHost>>() {
                    @Override
                    public int compare(Map.Entry<Integer, HyperPowerHost> e1,
                                       Map.Entry<Integer, HyperPowerHost> e2) {
                        int ord = Double.compare(e1.getValue().getPower(), e2.getValue().getPower());
                        if(ord == 1)
                            return -1;
                        return 1;
                    }
                });
        sortedneighbors.addAll((Collection<? extends Map.Entry<Integer, HyperPowerHost>>) h.getNeighbors().entrySet());
        ArrayList vms = (ArrayList) h.getVmList();

        int idx = 0;
        int vmcount = vms.size();
        HyperPowerVm vm;
        State s;
        while (true) {
            s = h.getStatebyPower(h.getTempPower(vmcount));
            if (idx == vms.size()) {
                break;
            }
            vm = (HyperPowerVm) vms.get(idx++);
            for(Map.Entry<Integer, HyperPowerHost> entry : sortedneighbors) {
                if (entry.getValue().getState() == HyperPowerHost.State.OVERU || entry.getValue().getState() == State.OFF || tobeoffHosts.contains(entry.getValue())) {
                    continue;
                }
                if (h.getVmList().size() == 0 || s == State.OVERU)
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
            if (entry.getValue().getState() == HyperPowerHost.State.OVERU || entry.getValue().getState() == State.OFF  || tobeoffHosts.contains(entry.getValue())) {
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
                if (h.getVmList().size() == 0 || entry.getValue().getState() == State.OVERU)
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
