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



    public boolean allocateHostForVm(Vm vm, Host host, List<Host> visitedHosts, List<Host> offHosts)
    {
        //System.out.println("Allocating huper vm id =  " + vm.getId());
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
            //check if host is ok and add vm
            if(hwReqMet(hs,vm)) {
                //host.getVmList().add(vm);
                //System.out.println("Host meets requirments");
                //if(hs.vmCreate(vm)) {
                if (host.vmCreate(vm))
                {
                    getVmTable().put(vm.getUid(), host);
                    return true;
                }
                /*else
                {
                    return false;
                }*/
            }
        }
        //System.out.println("Hossssssssssssss");
        visitedHosts.add(hs);
        Iterator it = hs.getNeighbors().entrySet().iterator();
        while (it.hasNext()) {

            Map.Entry pairs = (Map.Entry) it.next();
            HyperPowerHost neighbor = (HyperPowerHost) pairs.getValue();
            //System.out.println("Hossssssssssssss222 " + neighbor.getId());
            if (neighbor.getState() != HyperPowerHost.State.OVERU && neighbor.getState() != HyperPowerHost.State.OFF && !visitedHosts.contains(neighbor)) {
                return allocateHostForVm(vm, neighbor, visitedHosts, offHosts);
            }
            if (neighbor.getState() == HyperPowerHost.State.OFF && !offHosts.contains(neighbor)) {
                tobeoffHosts.add(neighbor);
            }
        }
        //System.out.println("Hossssssssssssss33");
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
        //System.out.println("Hossssssssssssss444");

        if(!offHosts.isEmpty())
        {
            //System.out.println("Hossssssssssssss555 " + offHosts.size());
            HyperPowerHost h = (HyperPowerHost) offHosts.get(0);
            PowerDatacenter pd = (PowerDatacenter) h.getDatacenter();

            offHosts.remove(h);
            pd.wakeuphost(h, (PowerVm) vm);
            //h.switchOn();
            //tobeonHosts.add(h);
            //h.tobeon = true;
            //switch on h
            //System.exit(-1);
            //return allocateHostForVm(vm, h, visitedHosts, offHosts);
        }

        return false;
    }


    public void turnonhost_cratevm()
    {

    }

    @Override
    public boolean allocateHostForVm(Vm vm) {


        int index = (int)(Math.random()*this.getHostList().size());
        //System.out.println("Allocating random vm with index" + index);
        return allocateHostForVm(vm, getHostList().get(index));
    }

    @Override
    public boolean allocateHostForVm(Vm vm, Host host) {

        return allocateHostForVm(vm, host, visitedHosts, tobeoffHosts);
    }

    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList)
    {

        return synchronizeHosts();
        //return null;
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
    protected List<HyperPowerHostUtilizationHistory> getOverUtilizedHosts() {
        return null;
    }

    public List<Map<String, Object>> synchronizeHosts()
    {
        int partial = 0;
        int fl = 0;
        int flvm =  0;
        monitorDatacenter();
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
            //System.out.println("SYNCHRONIZE STATE = " +host.getState() );
            if(host.getState() == State.OVERU)
            {
                //System.out.println("PARTIALM"  );
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
                //System.out.println("FULLM"  );
                //migrate all
                if(mf!=null) {
                    migrationMap.addAll(mf);
                }
            }
            //FSystem.exit(-1);
        }
        System.out.println(visitedHosts.size());
        System.out.println(tobeoffHosts.size());
        System.out.println("partia " + partial);
        System.out.println("fl "+  fl);
        System.out.println("fl "+  fl);
        System.out.println("flvm "+  flvm);

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
        System.out.println("nvm " + nvm);
        nh = underHosts.size() + idleHosts.size() + okHosts.size() + overHosts.size() + tobeoffHosts.size();
        System.out.println("underHosts " + underHosts.size());
        System.out.println("idleHosts " + idleHosts.size());
        System.out.println("okHosts " + okHosts.size());
        System.out.println("overHosts " + overHosts.size());
        System.out.println("tobeoffHosts " + tobeoffHosts.size());
        System.out.println("offHosts " + currentoffHosts.size());
    }


    public List<Map<String, Object>> partialVmMigration(HyperPowerHost h)
    {
        //System.out.println("PAPARTM1");
        List<Map<String, Object>> migrationMap = new ArrayList<>();
        SortedSet<Map.Entry<Integer, HyperPowerHost>> sortedneighbors = new TreeSet<Map.Entry<Integer, HyperPowerHost>>(
                new Comparator<Map.Entry<Integer, HyperPowerHost>>() {
                    @Override
                    public int compare(Map.Entry<Integer, HyperPowerHost> e1,
                                       Map.Entry<Integer, HyperPowerHost> e2) {
                        return Double.compare(e1.getValue().getPower(), e2.getValue().getPower());
                    }
                });
        sortedneighbors.addAll(h.getNeighbors().entrySet());
        //System.out.println("PAPARTM2");
        ArrayList vms = (ArrayList) h.getVmList();
        for(Map.Entry<Integer, HyperPowerHost> entry : sortedneighbors) {
            if(entry.getValue().getState() == State.OVERU)
            {
                continue;
            }
            int idx = 0;
            PowerVm vm;
            int vmcount = vms.size();
            while(true)
            {
                if(idx == vms.size())
                {
                    break;
                }
                vm = (PowerVm) vms.get(idx++);
                //System.out.println("PAPARTM3");
                if(vmcount == 0 ||  h.getState() != State.OVERU)
                {
                    //System.out.println("to ret null");
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
                    //System.out.println("FIND MIGRATING old = " + vm.getHost().getId() + " new = " + entry.getValue().getId());

                    //return migrationMap;
                    //System.exit(-1);
                }
                //test if condition are met and migrate
            }
        }
        //System.out.println("NOT FIND MIGRATING");
        return migrationMap;
    }

    public List<Map<String, Object>> fullVmMigration(HyperPowerHost h)
    {
        //System.out.println("FULLFULLM");
        List<Map<String, Object>> migrationMap = new ArrayList<>();
        SortedSet<Map.Entry<Integer, HyperPowerHost>> sortedneighbors = new TreeSet<Map.Entry<Integer, HyperPowerHost>>(
                new Comparator<Map.Entry<Integer, HyperPowerHost>>() {
                    @Override
                    public int compare(Map.Entry<Integer, HyperPowerHost> e1,
                                       Map.Entry<Integer, HyperPowerHost> e2) {
                        return Double.compare(e1.getValue().getPower(), e2.getValue().getPower());
                    }
                });
        sortedneighbors.addAll(h.getNeighbors().entrySet());
        ArrayList vms = (ArrayList) h.getVmList();
        for(Map.Entry<Integer, HyperPowerHost> entry : sortedneighbors) {
            //System.out.println("ENTRY STATE" + entry.getValue().getState());
            if (entry.getValue().getState() == HyperPowerHost.State.OVERU || entry.getValue().getState() == State.OFF  || tobeoffHosts.contains(entry.getValue())) {
                continue;
            }
            int idx = 0;
            int vmcount = vms.size();
            PowerVm vm;
            while (true) {
                if(idx == vms.size())
                {
                    break;
                }
                vm = (PowerVm) vms.get(idx++);
                if (h.getVmList().size() == 0 || entry.getValue().getState() == State.OVERU)//if(h.getVmList().size() == 0 ||  h.getState() != HyperPowerHost.State.OVERU)
                {
                    //System.out.println("TO R NULL" + entry.getValue().getVmList().size());
                    //return null;
                    break;
                }


                if (vm.getPower() >= entry.getValue().Pmax - entry.getValue().getPower()) {
                    //System.out.println("FULLFULLM VPOW " + vm.getPower() + " MAX " + entry.getValue().Pmax +" HOSTPOW " + entry.getValue().getPower());
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
                    //System.out.println("FULLL MIGRATING old = " + vm.getHost().getId() + " new = " + entry.getValue().getId());

                    //return migrationMap;
                    //break;
                }
            }
            if (vmcount == 0 && h.waitmigrate == 0) {

                //h.setState(State.OFF);
                tobeoffHosts.add(h);
                h.tobeoff = true;
                break;
            }
        }
        //System.out.println("MIGR = "+migrationMap.size());

        return migrationMap;
    }
}
