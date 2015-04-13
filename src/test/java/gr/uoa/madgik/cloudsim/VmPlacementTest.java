package gr.uoa.madgik.cloudsim;

import gr.uoa.magdik.cloudslim.HyperPowerDatacenter;
import gr.uoa.magdik.cloudslim.HyperPowerHost;
import gr.uoa.magdik.cloudslim.HyperVmAllocationPolicy;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Created by tchalas on 4/13/15.
 */



public class VmPlacementTest {
    @Test
    public void testplace()
    {
        HashMap<Integer, Integer> hostvms = new HashMap();
        hostvms.put(1,20);
        hostvms.put(2,8);
        hostvms.put(3,16);
        hostvms.put(4,0);
        hostvms.put(5,2);
        hostvms.put(6,3);
        hostvms.put(7,0);
        hostvms.put(8,0);
        DatacenterBroker broker;
        List<Cloudlet> cloudletList;
        List<Vm> vmList;
        List<HyperPowerHost> hostList;
        try {
            CloudSim.init(1, Calendar.getInstance(), false);

            broker = HyperHelper.createBroker();
            int brokerId = broker.getId();

            cloudletList = GenerateCloudlets.createCloudletList(brokerId, HyperConstants.NUMBER_OF_VMS);
            //vmList = HyperHelper.createVmList(brokerId, cloudletList.size());
            double log2base = Math.log(HyperConstants.NUMBER_OF_HOSTS)/Math.log(2);
            hostList = HyperHelper.createHostList((int)log2base);
            HyperPowerDatacenter datacenter = (HyperPowerDatacenter) HyperHelper.createDatacenter(
                    "Datacenter",
                    HyperPowerDatacenter.class,
                    hostList,
                    new HyperVmAllocationPolicy(hostList));
            boolean res = HyperHelper.placeVmsinHosts(hostvms, brokerId);
            System.out.println(res);
            assertEquals(res, true);

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }
    }
}


