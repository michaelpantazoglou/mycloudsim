package gr.uoa.magdik.cloudslim;

import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tchalas on 10/26/15.
 */
public class HyperDatacenterBroker extends PowerDatacenterBroker{


    public <T extends Vm> List<T> getLateVmList() {
        return (List<T>) lateVmList;
    }

    public void setLateVmList(List<? extends Vm> lateVmList) {
        this.lateVmList = lateVmList;
    }

    /** The vms created list. */
    protected List<? extends Vm> lateVmList;

    public <T extends Vm> List<T>getRemoveVmList() {
        return (List<T>) removeVmList;
    }

    public void setRemoveVmList(List<? extends Vm> removeVmList) {
        this.removeVmList = removeVmList;
    }

    /** The vms tobe removed list. */
    protected List<? extends Vm> removeVmList;

    public HyperDatacenterBroker(String name) throws Exception {
        super(name);
        lateVmList = new ArrayList<>();
        removeVmList = new ArrayList<>();
    }


    /**
     * Process the return of a request for the characteristics of a PowerDatacenter.
     *
     * @param ev a SimEvent object
     * @pre ev != $null
     * @post $none
     */
    protected void processResourceCharacteristics(SimEvent ev) {
        DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
        getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);

        if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
            setDatacenterRequestedIdsList(new ArrayList<Integer>());
            createVmsInDatacenter(getDatacenterIdsList().get(0));
            removeVmsFromDatacenter(getDatacenterIdsList().get(0));
        }
    }


    /**
     * Create the virtual machines in a datacenter.
     *
     * @param datacenterId Id of the chosen PowerDatacenter
     * @pre $none
     * @post $none
     */
    //CHANGING TO PUBLIC
    public void createVmsInDatacenter(int datacenterId) {
        // send as much vms as possible for this datacenter before trying the next one
        int requestedVms = 0;
        String datacenterName = CloudSim.getEntityName(datacenterId);
        double delay;
        for (Vm vm : getVmList()) {
            //HyperPowerVm hvm = (HyperPowerVm) vm;
            if (!getVmsToDatacentersMap().containsKey(vm.getId())) {
                Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId()
                        + " in " + datacenterName);
                sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);
                //delay = hvm.getDelay();
                //send(datacenterId, delay, CloudSimTags.VM_CREATE_ACK, vm);
                requestedVms++;
            }
        }
        for(Vm vm : getLateVmList())
        {
            HyperPowerVm hvm = (HyperPowerVm) vm;
            if (!getVmsToDatacentersMap().containsKey(vm.getId())) {
                Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId()
                        + " in " + datacenterName);
                //sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);
                delay = hvm.getDelay();
                send(datacenterId, delay, CloudSimTags.VM_CREATE_ACK, vm);
                requestedVms++;
            }
        }
        getDatacenterRequestedIdsList().add(datacenterId);
        setVmsRequested(requestedVms);
        setVmsAcks(0);
    }

    private void removeVmsFromDatacenter(Integer datacenterId)
    {
        String datacenterName = CloudSim.getEntityName(datacenterId);
        for(Vm vm : getRemoveVmList())
        {
            HyperPowerVm hvm = (HyperPowerVm) vm;
                Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to REMOVE VM #" + vm.getId()
                        + " in " + datacenterName);
                //sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);
                double delay = hvm.getRemovedelay();
                send(datacenterId, delay, HyperCloudSimTags.REMOVEVM, vm);
        }
    }

    protected void processVmCreate(SimEvent ev) {
        int[] data = (int[]) ev.getData();
        int datacenterId = data[0];
        int vmId = data[1];
        int result = data[2];

        if (result == CloudSimTags.TRUE) {
            getVmsToDatacentersMap().put(vmId, datacenterId);
            if(VmList.getById(getVmList(), vmId) != null)
            {
                getVmsCreatedList().add(VmList.getById(getVmList(), vmId));
            }
            else
            {
                getVmsCreatedList().add(VmList.getById(getLateVmList(), vmId));
            }
            Log.printLine(CloudSim.clock() + ": " + getName() + ": VM #" + vmId
                    + " has been created in Datacenter #" + datacenterId + ", Host #"
                    + VmList.getById(getVmsCreatedList(), vmId).getHost().getId());
        } else {
            Log.printLine(CloudSim.clock() + ": " + getName() + ": Creation of VM #" + vmId
                    + " failed in Datacenter #" + datacenterId);
        }

        incrementVmsAcks();

        // all the requested VMs have been created
        if (getVmsCreatedList().size() == getVmList().size() - getVmsDestroyed()) {
            submitCloudlets();
        } else {
            // all the acks received, but some VMs were not created
            if (getVmsRequested() == getVmsAcks()) {
                // find id of the next datacenter that has not been tried
                for (int nextDatacenterId : getDatacenterIdsList()) {
                    if (!getDatacenterRequestedIdsList().contains(nextDatacenterId)) {
                        createVmsInDatacenter(nextDatacenterId);
                        return;
                    }
                }

                // all datacenters already queried
                if (getVmsCreatedList().size() > 0) { // if some vm were created
                    submitCloudlets();
                } else { // no vms created. abort
                    Log.printLine(CloudSim.clock() + ": " + getName()
                            + ": none of the required VMs could be created. Aborting");
                    finishExecution();
                }
            }
        }
    }

    public void submitDelayVmList(List<? extends Vm> list) {
        getLateVmList().addAll(list);
    }
    public void submitRemoveVmList(List<? extends Vm> list) {
        getRemoveVmList().addAll(list);
    }
}
