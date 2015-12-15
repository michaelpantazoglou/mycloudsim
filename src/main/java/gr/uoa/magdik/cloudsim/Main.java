package gr.uoa.magdik.cloudsim;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.jfree.data.xy.XYSeries;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static gr.uoa.magdik.cloudsim.GenerateCloudlets.createCloudletList;
import static gr.uoa.magdik.cloudsim.HyperHelper.*;

/**
 * Created by tchalas on 4/25/15.
 */
public class Main {
    public static void main(String[] args) throws IOException
    {
        //HashMap<Integer, Integer> hostvms = new HashMap();
        int hours = 0;
        int hypercubesize = 0;
        int initvms = 0;
        int rate = 0;
        int mode = 0;
        String expname = "stable";

        if(args.length == 5)
        {
            hours = Integer.parseInt(args[0]);
            hypercubesize = Integer.parseInt(args[1]);
            initvms = Integer.parseInt(args[2]);
            rate = Integer.parseInt(args[4]);
            expname = args[5];
            mode = 1;
            if(args[3].equals("i"))
            {
                mode = 0;
            }
            else if(args[3].equals("d"))
            {
                mode = 2;
            }
            else if(args[3].equals("n"))
            {
                mode = 3;
            }
        }
        else
        {
            hours = 9;
            hypercubesize = 10;
            initvms = 200;
            rate = 1;
            mode = 1;
            //throw new IllegalArgumentException("Plese check the arguments provided");
        }
        System.out.println(hours);
        System.out.println(hypercubesize);
        System.out.println(initvms);
        System.out.println(mode);
        System.out.println(rate);

        //System.exit(-1);

        PrintWriter gnuscript;
        DatacenterBroker broker;
        List<Cloudlet> cloudletList;
        List<Vm> vmList;
        List<HyperPowerHost> hostList;
        HyperPowerDatacenter datacenter;

        File plan = new File("plan");
        boolean initread = false;
        boolean incomingread = false;


        try {
            HyperCloudSim.init(1, Calendar.getInstance(), false);
            broker = createBroker();
            int brokerId = broker.getId();
            vmList = new ArrayList<Vm>();
            cloudletList = createCloudletList(brokerId, HyperConstants.NUMBER_OF_CLOUDLETS);
            int vmsnumber = HyperConstants.NUMBER_OF_VMS;


            //double log2base = Math.log(Math.exp(hypercubesize))/Math.log(2);
            hostList = createHostList((int) hypercubesize - 1);
            broker.submitCloudletList(cloudletList);
            datacenter = (HyperPowerDatacenter) createDatacenter(
                    "Datacenter",
                    HyperPowerDatacenter.class,
                    hostList,
                    new HyperVmAllocationPolicy(hostList));

            datacenter.setMode(mode);
            datacenter.setRate(rate);
            vmList.addAll(createVmList(broker, initvms));
            int delayvms = 0;
            int checkrandom0 = 0;
            int checkrandom1 = 0;
            if(mode < 2) {
                for (int j = 61; j < hours * 3600; j++) //17200
                {
                    if (j % 4 == 0 || j % 5 == 0) {
                        delayvms = 0;
                    }
                    if(mode == 1)
                    {
                        if (Math.random() < 0.5) {
                            vmList.addAll(createVmsDelay(broker, rate, 1.0 * j));
                            checkrandom0++;
                        }
                        else
                        {
                            checkrandom1++;
                        }
                    }
                    else
                    {
                        vmList.addAll(createVmsDelay(broker, 4, 1.0 * j));
                    }
                }
            }

            HyperVmAllocationPolicy hv = (HyperVmAllocationPolicy) datacenter.getVmAllocationPolicy();
            hv.setDatacenter(datacenter);
            hv.getOnHosts().addAll(hostList);
            datacenter.setDisableMigrations(false);
            int f = new File("logs-plots").list().length;
            Date date = new Date();

            new File("logs-plots/" + f + "-"  + expname + "-" + date).mkdir();
            Log.writer = new PrintWriter("logs-plots/" + f + "-" + expname + "-" + date + "/results ", "UTF-8");
            datacenter.vmstimelog = new PrintWriter("logs-plots/" + f + "-" + expname + "-" + date + "/vmstime.dat", "UTF-8");
            datacenter.onhoststimelog = new PrintWriter("logs-plots/" + f + "-" + expname + "-" + date + "/onhoststime.dat", "UTF-8");
            datacenter.powertimelog = new PrintWriter("logs-plots/" + f + "-" + expname + "-" + date + "/powertime.dat", "UTF-8");
            CloudSim.terminateSimulation(hours * 3600);

            IncomingRequests incomingRequests = new IncomingRequests();
            incomingRequests.setDatacenterBroker(broker);
            incomingRequests.setHyperPowerDatacenter(datacenter);
            incomingRequests.start();

            ArrayList<XYSeries> plotseries = new ArrayList<>();
            plotseries.add(datacenter.getOnhoststime());
            plotseries.add(datacenter.getVmstime());
            Log.setDisabled(true);
            double lastClock = HyperCloudSim.startSimulation();
            datacenter.vmstimelog.close();
            datacenter.powertimelog.close();
            datacenter.onhoststimelog.close();
            createPlots(datacenter, hv, date, f, expname);
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            Log.printLine("Received " + newList.size() + " cloudlets");
            System.out.println("Simulation reached termination time");
            System.out.println(checkrandom0);
            System.out.println(checkrandom1);

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }
    }

    private static void createPlots(HyperPowerDatacenter datacenter, HyperVmAllocationPolicy hv, Date date, int experimentcount, String expname)
    {
        ArrayList<XYSeries> plotseries = new ArrayList<>();;
        plotseries.add(datacenter.getVmstime());
        plotseries.add(datacenter.getOnhoststime());
        createLinePlots(plotseries, "logs-plots/" + experimentcount + "-" + expname + "-" + date + "/", "hosts+vms-time");
        plotseries.remove(datacenter.getOnhoststime());
        plotseries.add(datacenter.getEnergytime());
        createLinePlots(plotseries, "logs-plots/" + experimentcount + "-" + expname + "-" + date + "/", "energy+vms-time");
        plotseries.clear();
        plotseries.add(hv.getStateIDLE());
        plotseries.add(hv.getStateOVER());
        plotseries.add(hv.getStateOK());
        plotseries.add(hv.getStateOFF());
        plotseries.add(hv.getStateUNDER());
        createBarPlots(plotseries, "logs-plots/" + experimentcount + "-" + expname + "-" + date + "/", "hosts state");
        plotseries.clear();
        plotseries.add(datacenter.getSwitchoffs());
        createLinePlots(plotseries, "logs-plots/" + experimentcount + "-" + expname + "-" + date + "/", "switchoffs");
        plotseries.clear();
        plotseries.add(datacenter.getSwitchons());
        createLinePlots(plotseries, "logs-plots/" + experimentcount + "-" + expname + "-" + date + "/", "switchons");

    }
}




//commented code for ececution plan by file
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

//hv.inithostsvm = hostvms;
//hv.initoffhosts();
//System.out.println("initvms" + initvms);
//System.exit(-1);