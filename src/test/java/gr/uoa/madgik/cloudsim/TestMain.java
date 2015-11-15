package gr.uoa.madgik.cloudsim;

import gr.uoa.magdik.cloudslim.*;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import static gr.uoa.magdik.cloudslim.HyperHelper.*;
import static gr.uoa.magdik.cloudslim.GenerateCloudlets.*;

/**
 * Created by tchalas on 4/25/15.
 */
public class TestMain {
    public static void main(String[] args) throws IOException
    {
        HashMap<Integer, Integer> hostvms = new HashMap();
/*        hostvms.put(0,2);
        hostvms.put(1,2);
        hostvms.put(2,3);
        hostvms.put(3,-1);
        hostvms.put(4,-1);
        hostvms.put(5,-1);
        hostvms.put(6,-1);
        hostvms.put(7,-1);
*/
        DatacenterBroker broker;
        List<Cloudlet> cloudletList;
        List<Vm> vmList;
        List<HyperPowerHost> hostList;
        HyperPowerDatacenter datacenter;

        File plan = new File("plan");
        boolean initread = false;
        boolean incomingread = false;
        int initvms = 4;
        /*try (BufferedReader br = new BufferedReader(new FileReader(plan))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                if(line.equals("")) initread = false;
                if(initread)
                {
                    String linearr[] = line.split("\t");
                    int hostid = Integer.parseInt(linearr[0]);
                    int numberofvms = Integer.parseInt(linearr[1]);
                    if(numberofvms != -1) initvms += numberofvms;
                    System.out.println("HOST" + hostid + " VMS " + numberofvms);
                    hostvms.put(hostid - 1,numberofvms);
                }
                else if(incomingread)
                {

                }
                if(line.equals("HOSTID\tVMS"))  initread = true;
                if(line.equals("TIME\tVMS"))
                {
                    initread = false;
                    incomingread = true;
                }
            }
        }*/
        //System.out.println("initvms" + initvms);
        //System.exit(-1);

        try {
            CloudSim.init(1, Calendar.getInstance(), false);
            broker = createBroker();
            int brokerId = broker.getId();
            vmList = new ArrayList<Vm>();
            cloudletList = createCloudletList(brokerId, HyperConstants.NUMBER_OF_CLOUDLETS);
            int vmsnumber = HyperConstants.NUMBER_OF_VMS;

            //vmList.addAll(createVmsDelay(broker,7,120.0));
            //vmList.addAll(createVmsDelay(broker,7,120.0));
            double log2base = Math.log(64)/Math.log(2);
            //broker.submitVmList(vmList);
            hostList = createHostList((int) log2base - 1);
            broker.submitCloudletList(cloudletList);

            datacenter = (HyperPowerDatacenter) createDatacenter(
                    "Datacenter",
                    HyperPowerDatacenter.class,
                    hostList,
                    new HyperVmAllocationPolicy(hostList));

            vmList.addAll(createVmList(broker, initvms));
            int delayvms = 0;
            for(int j = 2; j < 670; j++) //17200
            {
                if(j % 4 == 0 || j % 5 == 0)
                {
                    //removeRandomVms((HyperDatacenterBroker) broker, 14 - delayvms, j * 10.0 + 0.3);
                    delayvms = 0;
                    //continue;
                }
                if(Math.random() > 0.5)
                {
                    vmList.addAll(createVmsDelay(broker,4, 1.0*j));
                    delayvms += 4;
                }
            }
            HyperVmAllocationPolicy hv = (HyperVmAllocationPolicy) datacenter.getVmAllocationPolicy();
            hv.setDatacenter(datacenter);
            //hv.inithostsvm = hostvms;
            //hv.initoffhosts();
            hv.getOnHosts().addAll(hostList);
            datacenter.setDisableMigrations(false);

            Log.writer = new PrintWriter("results", "UTF-8");
            datacenter.vmstimelog = new PrintWriter("vmstime.dat", "UTF-8");
            datacenter.onhoststimelog = new PrintWriter("onhoststime.dat", "UTF-8");
            datacenter.powertimelog = new PrintWriter("powertime.dat", "UTF-8");


            IncomingRequests incomingRequests = new IncomingRequests();
            incomingRequests.setDatacenterBroker(broker);
            incomingRequests.setHyperPowerDatacenter(datacenter);
            incomingRequests.start();

            //CloudSim.terminateSimulation(Constants.SIMULATION_LIMIT);
            double lastClock = CloudSim.startSimulation();
            //CloudSim.pauseSimulation();
            //vmList.addAll(HyperHelper.placeVmsinHosts(hostvms, brokerId));
            //System.out.println(res);
            //CloudSim.resumeSimulation();
            //assertNotEquals(vmList, null);
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            Log.printLine("Received " + newList.size() + " cloudlets");
            datacenter.vmstimelog.close();
            datacenter.powertimelog.close();
            datacenter.onhoststimelog.close();
            //CloudSim.stopSimulation();
/*
            HyperHelper.printResults(
                    datacenter,
                    vmList,
                    lastClock,
                    "",
                    Constants.OUTPUT_CSV,
                    "");
*/
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }
    }
}
