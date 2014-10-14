package gr.uoa.di.madgik.cloudsim;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.power.PowerHost;

import java.util.List;

/**
 * Created by tchalas on 14/10/2014.
 */
public class HyperDatacenter extends Datacenter{
    public HyperDatacenter(
            String name,
            DatacenterCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval) throws Exception
            {
                super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
            }


    public <T extends Host> List<T> getHostList() {
        return (List<T>) getCharacteristics().getHostList();
    }

    protected void controlHosts()
    {
        //hosts communicate here
        for (HypercubeHost host : this. <HypercubeHost> getHostList())
        {

        }

    }

}
