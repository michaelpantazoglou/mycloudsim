package gr.uoa.magdik.cloudsim;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
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

        if(args.length == 5)
        {
            hours = Integer.parseInt(args[0]);
            hypercubesize = Integer.parseInt(args[1]);
            initvms = Integer.parseInt(args[2]);
            rate = Integer.parseInt(args[4]);
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
            hours = 1;
            hypercubesize = 3;
            initvms = 20;
            rate = 0;
            mode = 1;
            //throw new IllegalArgumentException("Plese check the arguments provided");
        }
        System.out.println(hours);
        System.out.println(hypercubesize);
        System.out.println(initvms);
        System.out.println(rate);
        System.out.println(mode);
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
        //System.out.println("initvms" + initvms);
        //System.exit(-1);

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

            if(mode < 2) {
                for (int j = 61; j < 29790; j++) //17200
                {
                    if (j % 4 == 0 || j % 5 == 0) {
                        delayvms = 0;
                    }
                    if(mode == 1)
                    {
                        if (Math.random() > 0.5) {
                            vmList.addAll(createVmsDelay(broker, rate, 1.0 * j));
                            delayvms += 4;
                        }
                    }
                    else
                    {
                        vmList.addAll(createVmsDelay(broker, 4, 1.0 * j));
                        delayvms += 4;
                    }
                }
            }


            HyperVmAllocationPolicy hv = (HyperVmAllocationPolicy) datacenter.getVmAllocationPolicy();
            hv.setDatacenter(datacenter);
            //hv.inithostsvm = hostvms;
            //hv.initoffhosts();
            hv.getOnHosts().addAll(hostList);
            datacenter.setDisableMigrations(false);
           // Calendar cal = Calendar.getInstance();
           // cal.add(Calendar.DATE, 1);
            int f = new File("logs-plots").list().length;
            System.out.println(f);
           // System.exit(-1);
           // SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
            //String date = format1.format(cal.getTime());
            Date date = new Date();

            new File("logs-plots/" + f + "-"  + date).mkdir();
            Log.writer = new PrintWriter("logs-plots/" + f + "-" + date + "/results ", "UTF-8");
            datacenter.vmstimelog = new PrintWriter("logs-plots/" + f + "-" + date + "/vmstime.dat", "UTF-8");
            datacenter.onhoststimelog = new PrintWriter("logs-plots/" + f + "-" + date + "/onhoststime.dat", "UTF-8");
            datacenter.powertimelog = new PrintWriter("logs-plots/" + f + "-" + date + "/powertime.dat", "UTF-8");
            gnuscript = new PrintWriter("logs-plots/" + f + "-" + date + "/gnuscript", "UTF-8");
            gnuscript.println("set term png");
            gnuscript.println("set output \"onhoststime.png\"");
            gnuscript.println("plot \"onhoststime.dat\" using 1:2 w linesp");
            gnuscript.println("set output \"vmstime.png\"");
            gnuscript.println("plot \"vmstime.dat\" using 1:2 w linesp");
            gnuscript.println("set output \"powertime.png\"");
            gnuscript.println("plot \"powertime.dat\" using 1:2 w linesp");

            gnuscript.close();
            //Process p = Runtime.getRuntime().exec("gnuplot logs-plots/" + f + "-" + date + "/gnuscript");
            //p.waitFor();
            CloudSim.terminateSimulation(hours * 3600);

            IncomingRequests incomingRequests = new IncomingRequests();
            incomingRequests.setDatacenterBroker(broker);
            incomingRequests.setHyperPowerDatacenter(datacenter);
            incomingRequests.start();

            double lastClock = HyperCloudSim.startSimulation();
            datacenter.vmstimelog.close();
            datacenter.powertimelog.close();
            datacenter.onhoststimelog.close();
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            Log.printLine("Received " + newList.size() + " cloudlets");

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }
    }
}
