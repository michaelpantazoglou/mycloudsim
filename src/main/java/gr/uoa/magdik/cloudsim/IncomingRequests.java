package gr.uoa.magdik.cloudsim;

import org.cloudbus.cloudsim.CloudletSchedulerDynamicWorkload;
import org.cloudbus.cloudsim.DatacenterBroker;

import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by tchalas on 10/21/15.
 */
public class IncomingRequests implements Runnable{

    Scanner inputReader = new Scanner(System.in);

    public DatacenterBroker getDatacenterBroker() {
        return datacenterBroker;
    }

    public void setDatacenterBroker(DatacenterBroker datacenterBroker) {
        this.datacenterBroker = datacenterBroker;
    }

    DatacenterBroker datacenterBroker;

    public HyperPowerDatacenter getHyperPowerDatacenter() {
        return hyperPowerDatacenter;
    }

    public void setHyperPowerDatacenter(HyperPowerDatacenter hyperPowerDatacenter) {
        this.hyperPowerDatacenter = hyperPowerDatacenter;
    }

    HyperPowerDatacenter hyperPowerDatacenter;

    //create a thread object and check if it's not already created
    static Thread thread;

    //This method gets called from the main
    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    @Override
    public void run() {
        readTextFromConsole();
    }

    public void readTextFromConsole() {
        System.out.println("Enter something:");
        String myinput = inputReader.nextLine();
        System.out.println("You Entered: " + myinput);
        if(myinput.substring(0,2).equals("vm")) {
            String vmsnumber = myinput.substring(2);
            int newvms = Integer.parseInt(vmsnumber);
            ArrayList<HyperPowerVm> newvmslist = new ArrayList<>();
            for (int i = 0; i < newvms; i++)
            {
                int VM_MIPS = HyperConstants.VM_MIPS[0];
                long VM_SIZE = HyperConstants.VM_SIZE;
                int VM_RAM = HyperConstants.VM_RAM[0];
                long VM_BW = HyperConstants.VM_BW;
                int VM_PES = HyperConstants.VM_PES[0];
                HyperPowerVm vm = new HyperPowerVm(
                    datacenterBroker.getVmList().size() + i,
                    datacenterBroker.getId(),
                    VM_MIPS,
                    VM_PES,
                    VM_RAM,
                    VM_BW,
                    VM_SIZE,
                    1,
                    "Xen",
                    new CloudletSchedulerDynamicWorkload(HyperConstants.VM_MIPS[1], HyperConstants.VM_PES[1]),
                    HyperConstants.SCHEDULING_INTERVAL);
                    newvmslist.add(vm);
            }
            datacenterBroker.submitVmList(newvmslist);
            datacenterBroker.createVmsInDatacenter(datacenterBroker.getDatacenterIdsList().get(0));
            HyperVmAllocationPolicy hv = (HyperVmAllocationPolicy) hyperPowerDatacenter.getVmAllocationPolicy();
            hv.inithostsvm = null;
        }
        else if(myinput.substring(0,2).equals("cl"))
        {

        }
        readTextFromConsole();
    }
}
