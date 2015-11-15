package gr.uoa.magdik.cloudslim;

import gr.uoa.magdik.cloudslim.*;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.power.*;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.util.MathUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

/**
 * The Class HyperHelper.
 * 
 * If you are using any algorithms, policies or workload included in the power package, please cite
 * the following paper:
 * 
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 * 
 * @author Anton Beloglazov
 */
public class HyperHelper  {


	private static int hostId = 0;

    private static HyperPowerDatacenter cd;


    public static List<Vm> placeVmsinHosts(HashMap<Integer,Integer> mapvms, int brokerId)
    {
        int VM_MIPS = HyperConstants.VM_MIPS[0];
        long VM_SIZE = HyperConstants.VM_SIZE; // image size (MB)
        int VM_RAM = HyperConstants.VM_RAM[0]; // vm memory (MB)
        long VM_BW = HyperConstants.VM_BW;
        int VM_PES = HyperConstants.VM_PES[0]; // number of cpus
        int i = 0;
        List<Vm> vms = new ArrayList<Vm>();
        Iterator it = mapvms.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            HyperPowerHost hp = (HyperPowerHost) cd.getHostbyId((Integer) pair.getKey());
            for(int j = 0; j< (int) pair.getValue(); j++)
            {
                HyperPowerVm vm = new HyperPowerVm(
                        i,
                        brokerId,
                        VM_MIPS,
                        VM_PES,
                        VM_RAM,
                        VM_BW,
                        VM_SIZE,
                        1,
                        "Xen",
                        new CloudletSchedulerDynamicWorkload(HyperConstants.VM_MIPS[0], HyperConstants.VM_PES[0]),
                        HyperConstants.SCHEDULING_INTERVAL);
                HyperVmAllocationPolicy hva= (HyperVmAllocationPolicy) cd.getVmAllocationPolicy();
                if(hva.placeVminHost(vm, hp) == false)
                {
                    return null;
                }
                vms.add(vm);
            }

            i++;
        }
        return vms;
    }

	public static void removeRandomVms(HyperDatacenterBroker broker, int vmsNumber, double delay)
	{
		List<Vm> removevms = new ArrayList<Vm>();
		int size = broker.getLateVmList().size();
		if(size == 0) return;
		Random random = new Random();
		for (int idx = 0; idx < vmsNumber; ++idx){
			int vmr = generateRandomInteger(0, size - 1, random);
			HyperPowerVm vm = (HyperPowerVm) broker.getLateVmList().get(vmr);
			vm.setRemovedelay(delay);
			removevms.add(vm);
		}
		broker.submitRemoveVmList(removevms);
	}

	public static List<Vm> createVmsDelay(DatacenterBroker broker, int vmsNumber, double delay) {
		int VM_MIPS = HyperConstants.VM_MIPS[0];
		long VM_SIZE = HyperConstants.VM_SIZE; // image size (MB)
		int VM_RAM = HyperConstants.VM_RAM[0]; // vm memory (MB)
		long VM_BW = HyperConstants.VM_BW;
		int VM_PES = HyperConstants.VM_PES[0]; // number of cpus
		//String vmm = "Xen"; // VMM name
		List<Vm> vms = new ArrayList<Vm>();
		HyperDatacenterBroker b = (HyperDatacenterBroker) broker;
		for (int i = 0; i < vmsNumber; i++) {
			int vmType = i / (int) Math.ceil((double) vmsNumber / HyperConstants.VM_TYPES);
			HyperPowerVm vm = new HyperPowerVm(
					broker.getVmList().size() + b.getLateVmList().size() + i,
					broker.getId(),
					VM_MIPS,
					VM_PES,
					VM_RAM,
					VM_BW,
					VM_SIZE,
					1,
					"Xen",
					new CloudletSchedulerDynamicWorkload(HyperConstants.VM_MIPS[vmType], HyperConstants.VM_PES[vmType]),
					HyperConstants.SCHEDULING_INTERVAL);
			vm.setDelay(delay);
			vms.add(vm);
		}
		b.submitDelayVmList(vms);
		return vms;
	}

	/**
	 * Creates the vm list.
	 * 
	 * @param brokerId the broker id
	 * @param vmsNumber the vms number
	 * 
	 * @return the list< vm>
	 */
	public static List<Vm> createVmList(DatacenterBroker broker, int vmsNumber) {
        // VM description
        int VM_MIPS = HyperConstants.VM_MIPS[0];
        long VM_SIZE = HyperConstants.VM_SIZE; // image size (MB)
        int VM_RAM = HyperConstants.VM_RAM[0]; // vm memory (MB)
        long VM_BW = HyperConstants.VM_BW;
        int VM_PES = HyperConstants.VM_PES[0]; // number of cpus
        //String vmm = "Xen"; // VMM name
		List<Vm> vms = new ArrayList<Vm>();
		for (int i = 0; i < vmsNumber; i++) {
			int vmType = i / (int) Math.ceil((double) vmsNumber / HyperConstants.VM_TYPES);
			//int vmType = 3;
			vms.add(new HyperPowerVm(
					broker.getVmList().size() + i,
					broker.getId(),
					VM_MIPS,
					VM_PES,
					VM_RAM,
					VM_BW,
					VM_SIZE,
					1,
					"Xen",
					new CloudletSchedulerDynamicWorkload(HyperConstants.VM_MIPS[vmType], HyperConstants.VM_PES[vmType]),
					HyperConstants.SCHEDULING_INTERVAL));
		}
		broker.submitVmList(vms);
		return vms;
	}





		public static List<HyperPowerHost> createHostList(int dimension) {
			List<HyperPowerHost> l = new ArrayList<>();

			if (dimension == 0) {

				int hostType = 1;//hostId % HyperConstants.HOST_TYPES;
                List<Pe> peList1 = new ArrayList<Pe>();
                for (int j = 0; j < 2; j++) {
                    peList1.add(new Pe(j, new PeProvisionerSimple(12000)));
                }

                int MIPS = HyperConstants.HOST_MIPS[0];

                int HOST_RAM = HyperConstants.HOST_RAM[0];
                int HOST_STORAGE = HyperConstants.HOST_STORAGE;
                int HOST_BANDWIDTH = HyperConstants.HOST_BW;
//hostId++,


                HyperPowerHost h1 = new HyperPowerHost(
						-1,
                        new RamProvisionerSimple(HOST_RAM),
                        new BwProvisionerSimple(HOST_BANDWIDTH),
                        HOST_STORAGE,
                        peList1,
                        new VmSchedulerTimeSharedOverSubscription(peList1),
                        HyperConstants.HOST_POWER[hostType]);
				l.add(h1);

				hostType = hostId % HyperConstants.HOST_TYPES;

                List<Pe> peList2 = new ArrayList<Pe>();
                for (int j = 0; j < 2; j++) {
                    peList2.add(new Pe(j, new PeProvisionerSimple(12000)));
                }

                HyperPowerHost h2 = new HyperPowerHost(
						-1,
                        new RamProvisionerSimple(HOST_RAM),
                        new BwProvisionerSimple(HOST_BANDWIDTH),
                        HOST_STORAGE,
                        peList2,
                        new VmSchedulerTimeSharedOverSubscription(peList2),
                        HyperConstants.HOST_POWER[hostType]);
				l.add(h2);
				h1.setNeighbor(dimension, h2);
				h2.setNeighbor(dimension, h1);
				h1.startEntity();
				h2.startEntity();

				return l;
			}

			List<HyperPowerHost> l1 = createHostList(dimension - 1);
			List<HyperPowerHost> l2 = createHostList(dimension - 1);

			for (int i = 0; i<Math.pow(2, dimension); i++) {
				l1.get(i).setNeighbor(dimension, l2.get(i));
				l2.get(i).setNeighbor(dimension, l1.get(i));
			}


			//l = new ArrayList<HyperPowerHost>(l1);
            l.addAll(l1);
			l.addAll(l2);
			return l;
	}

	/**
	 * Creates the broker.
	 * 
	 * @return the datacenter broker
	 */
	public static DatacenterBroker createBroker() {
		HyperDatacenterBroker broker = null;
		try {
			broker = new HyperDatacenterBroker("Broker");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return broker;
	}

	/**
	 * Creates the datacenter.
	 * 
	 * @param name the name
	 * @param datacenterClass the datacenter class
	 * @param hostList the host list
	 * @param vmAllocationPolicy the vm allocation policy
	 *
	 *
	 * @return the power datacenter
	 * 
	 * @throws Exception the exception
	 */
	public static Datacenter createDatacenter(
			String name,
			Class<? extends Datacenter> datacenterClass,
			List<HyperPowerHost> hostList,
			VmAllocationPolicy vmAllocationPolicy) throws Exception {
		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this resource
		double costPerBw = 0.0; // the cost of using bw in this resource

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch,
				os,
				vmm,
				hostList,
				time_zone,
				cost,
				costPerMem,
				costPerStorage,
				costPerBw);

		HyperPowerDatacenter datacenter = null;
		try {
			datacenter = (HyperPowerDatacenter) datacenterClass.getConstructor(
					String.class,
					DatacenterCharacteristics.class,
					VmAllocationPolicy.class,
					List.class,
					Double.TYPE).newInstance(
					name,
					characteristics,
					vmAllocationPolicy,
					new LinkedList<Storage>(),
					HyperConstants.SCHEDULING_INTERVAL);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
        cd = datacenter;
		return datacenter;
	}




	public static int Factorial(int n)
	{
		if(n == 0 || n == 1)
		{
			return 1;
		}
		return n * Factorial(n-1);
	}

	/**
	 * Gets the times before host shutdown.
	 * 
	 * @param hosts the hosts
	 * @return the times before host shutdown
	 */
	public static List<Double> getTimesBeforeHostShutdown(List<Host> hosts) {
		List<Double> timeBeforeShutdown = new LinkedList<Double>();
		for (Host host : hosts) {
			boolean previousIsActive = true;
			double lastTimeSwitchedOn = 0;
			for (HostStateHistoryEntry entry : ((HostDynamicWorkload) host).getStateHistory()) {
				if (previousIsActive == true && entry.isActive() == false) {
					timeBeforeShutdown.add(entry.getTime() - lastTimeSwitchedOn);
				}
				if (previousIsActive == false && entry.isActive() == true) {
					lastTimeSwitchedOn = entry.getTime();
				}
				previousIsActive = entry.isActive();
			}
		}
		return timeBeforeShutdown;
	}

	/**
	 * Gets the times before vm migration.
	 * 
	 * @param vms the vms
	 * @return the times before vm migration
	 */
	public static List<Double> getTimesBeforeVmMigration(List<Vm> vms) {
		List<Double> timeBeforeVmMigration = new LinkedList<Double>();
		for (Vm vm : vms) {
			boolean previousIsInMigration = false;
			double lastTimeMigrationFinished = 0;
			for (VmStateHistoryEntry entry : vm.getStateHistory()) {
				if (previousIsInMigration == true && entry.isInMigration() == false) {
					timeBeforeVmMigration.add(entry.getTime() - lastTimeMigrationFinished);
				}
				if (previousIsInMigration == false && entry.isInMigration() == true) {
					lastTimeMigrationFinished = entry.getTime();
				}
				previousIsInMigration = entry.isInMigration();
			}
		}
		return timeBeforeVmMigration;
	}

	/**
	 * Prints the results.
	 *  @param datacenter the datacenter
	 * @param lastClock the last clock
     * @param experimentName the experiment name
     * @param outputInCsv the output in csv
     * @param outputFolder the output folder
     */
	public static void printResults(
			HyperPowerDatacenter datacenter,
			List<Vm> vms,
			double lastClock,
			String experimentName,
			boolean outputInCsv,
			String outputFolder) {
		Log.enable();
		List<Host> hosts = datacenter.getHostList();

		int numberOfHosts = hosts.size();
		int numberOfVms = vms.size();

		double totalSimulationTime = lastClock;
		double energy = datacenter.getPower() / (3600 * 1000);
		int numberOfMigrations = datacenter.getMigrationCount();

		Map<String, Double> slaMetrics = getSlaMetrics(vms);

		double slaOverall = slaMetrics.get("overall");
		double slaAverage = slaMetrics.get("average");
		double slaDegradationDueToMigration = slaMetrics.get("underallocated_migration");
		// double slaTimePerVmWithMigration = slaMetrics.get("sla_time_per_vm_with_migration");
		// double slaTimePerVmWithoutMigration =
		// slaMetrics.get("sla_time_per_vm_without_migration");
		// double slaTimePerHost = getSlaTimePerHost(hosts);
		double slaTimePerActiveHost = getSlaTimePerActiveHost(hosts);

		double sla = slaTimePerActiveHost * slaDegradationDueToMigration;

		List<Double> timeBeforeHostShutdown = getTimesBeforeHostShutdown(hosts);

		int numberOfHostShutdowns = timeBeforeHostShutdown.size();

		double meanTimeBeforeHostShutdown = Double.NaN;
		double stDevTimeBeforeHostShutdown = Double.NaN;
		if (!timeBeforeHostShutdown.isEmpty()) {
			meanTimeBeforeHostShutdown = MathUtil.mean(timeBeforeHostShutdown);
			stDevTimeBeforeHostShutdown = MathUtil.stDev(timeBeforeHostShutdown);
		}

		List<Double> timeBeforeVmMigration = getTimesBeforeVmMigration(vms);
		double meanTimeBeforeVmMigration = Double.NaN;
		double stDevTimeBeforeVmMigration = Double.NaN;
		if (!timeBeforeVmMigration.isEmpty()) {
			meanTimeBeforeVmMigration = MathUtil.mean(timeBeforeVmMigration);
			stDevTimeBeforeVmMigration = MathUtil.stDev(timeBeforeVmMigration);
		}

		if (outputInCsv) {
			File folder = new File(outputFolder);
			if (!folder.exists()) {
				folder.mkdir();
			}
			File folder1 = new File(outputFolder + "/stats");
			if (!folder1.exists()) {
				folder1.mkdir();
			}
			File folder2 = new File(outputFolder + "/time_before_host_shutdown");
			if (!folder2.exists()) {
				folder2.mkdir();
			}
			File folder3 = new File(outputFolder + "/time_before_vm_migration");
			if (!folder3.exists()) {
				folder3.mkdir();
			}
			File folder4 = new File(outputFolder + "/metrics");
			if (!folder4.exists()) {
				folder4.mkdir();
			}

			StringBuilder data = new StringBuilder();
			String delimeter = ",";

			data.append(experimentName + delimeter);
			data.append(parseExperimentName(experimentName));
			data.append(String.format("%d", numberOfHosts) + delimeter);
			data.append(String.format("%d", numberOfVms) + delimeter);
			data.append(String.format("%.2f", totalSimulationTime) + delimeter);
			data.append(String.format("%.5f", energy) + delimeter);
			data.append(String.format("%d", numberOfMigrations) + delimeter);
			data.append(String.format("%.10f", sla) + delimeter);
			data.append(String.format("%.10f", slaTimePerActiveHost) + delimeter);
			data.append(String.format("%.10f", slaDegradationDueToMigration) + delimeter);
			data.append(String.format("%.10f", slaOverall) + delimeter);
			data.append(String.format("%.10f", slaAverage) + delimeter);
			// data.append(String.format("%.5f", slaTimePerVmWithMigration) + delimeter);
			// data.append(String.format("%.5f", slaTimePerVmWithoutMigration) + delimeter);
			// data.append(String.format("%.5f", slaTimePerHost) + delimeter);
			data.append(String.format("%d", numberOfHostShutdowns) + delimeter);
			data.append(String.format("%.2f", meanTimeBeforeHostShutdown) + delimeter);
			data.append(String.format("%.2f", stDevTimeBeforeHostShutdown) + delimeter);
			data.append(String.format("%.2f", meanTimeBeforeVmMigration) + delimeter);
			data.append(String.format("%.2f", stDevTimeBeforeVmMigration) + delimeter);

			if (datacenter.getVmAllocationPolicy() instanceof PowerVmAllocationPolicyMigrationAbstract) {
				PowerVmAllocationPolicyMigrationAbstract vmAllocationPolicy = (PowerVmAllocationPolicyMigrationAbstract) datacenter
						.getVmAllocationPolicy();

				double executionTimeVmSelectionMean = MathUtil.mean(vmAllocationPolicy
                        .getExecutionTimeHistoryVmSelection());
				double executionTimeVmSelectionStDev = MathUtil.stDev(vmAllocationPolicy
                        .getExecutionTimeHistoryVmSelection());
				double executionTimeHostSelectionMean = MathUtil.mean(vmAllocationPolicy
                        .getExecutionTimeHistoryHostSelection());
				double executionTimeHostSelectionStDev = MathUtil.stDev(vmAllocationPolicy
                        .getExecutionTimeHistoryHostSelection());
				double executionTimeVmReallocationMean = MathUtil.mean(vmAllocationPolicy
                        .getExecutionTimeHistoryVmReallocation());
				double executionTimeVmReallocationStDev = MathUtil.stDev(vmAllocationPolicy
                        .getExecutionTimeHistoryVmReallocation());
				double executionTimeTotalMean = MathUtil.mean(vmAllocationPolicy
                        .getExecutionTimeHistoryTotal());
				double executionTimeTotalStDev = MathUtil.stDev(vmAllocationPolicy
                        .getExecutionTimeHistoryTotal());

				data.append(String.format("%.5f", executionTimeVmSelectionMean) + delimeter);
				data.append(String.format("%.5f", executionTimeVmSelectionStDev) + delimeter);
				data.append(String.format("%.5f", executionTimeHostSelectionMean) + delimeter);
				data.append(String.format("%.5f", executionTimeHostSelectionStDev) + delimeter);
				data.append(String.format("%.5f", executionTimeVmReallocationMean) + delimeter);
				data.append(String.format("%.5f", executionTimeVmReallocationStDev) + delimeter);
				data.append(String.format("%.5f", executionTimeTotalMean) + delimeter);
				data.append(String.format("%.5f", executionTimeTotalStDev) + delimeter);

				writeMetricHistory(hosts, vmAllocationPolicy, outputFolder + "/metrics/" + experimentName
						+ "_metric");
			}

			data.append("\n");

			writeDataRow(data.toString(), outputFolder + "/stats/" + experimentName + "_stats.csv");
			writeDataColumn(timeBeforeHostShutdown, outputFolder + "/time_before_host_shutdown/"
					+ experimentName + "_time_before_host_shutdown.csv");
			writeDataColumn(timeBeforeVmMigration, outputFolder + "/time_before_vm_migration/"
					+ experimentName + "_time_before_vm_migration.csv");

		} else {
			Log.setDisabled(false);
			Log.printLine();
			Log.printLine(String.format("Experiment name: " + experimentName));
			Log.printLine(String.format("Number of hosts: " + numberOfHosts));
			Log.printLine(String.format("Number of VMs: " + numberOfVms));
			Log.printLine(String.format("Total simulation time: %.2f sec", totalSimulationTime));
			Log.printLine(String.format("Energy consumption: %.2f kWh", energy));
			Log.printLine(String.format("Number of VM migrations: %d", numberOfMigrations));
			Log.printLine(String.format("SLA: %.5f%%", sla * 100));
			Log.printLine(String.format(
                    "SLA perf degradation due to migration: %.2f%%",
                    slaDegradationDueToMigration * 100));
			Log.printLine(String.format("SLA time per active host: %.2f%%", slaTimePerActiveHost * 100));
			Log.printLine(String.format("Overall SLA violation: %.2f%%", slaOverall * 100));
			Log.printLine(String.format("Average SLA violation: %.2f%%", slaAverage * 100));
			// Log.printLine(String.format("SLA time per VM with migration: %.2f%%",
			// slaTimePerVmWithMigration * 100));
			// Log.printLine(String.format("SLA time per VM without migration: %.2f%%",
			// slaTimePerVmWithoutMigration * 100));
			// Log.printLine(String.format("SLA time per host: %.2f%%", slaTimePerHost * 100));
			Log.printLine(String.format("Number of host shutdowns: %d", numberOfHostShutdowns));
			Log.printLine(String.format(
                    "Mean time before a host shutdown: %.2f sec",
                    meanTimeBeforeHostShutdown));
			Log.printLine(String.format(
                    "StDev time before a host shutdown: %.2f sec",
                    stDevTimeBeforeHostShutdown));
			Log.printLine(String.format(
                    "Mean time before a VM migration: %.2f sec",
                    meanTimeBeforeVmMigration));
			Log.printLine(String.format(
                    "StDev time before a VM migration: %.2f sec",
                    stDevTimeBeforeVmMigration));

			if (datacenter.getVmAllocationPolicy() instanceof PowerVmAllocationPolicyMigrationAbstract) {
				PowerVmAllocationPolicyMigrationAbstract vmAllocationPolicy = (PowerVmAllocationPolicyMigrationAbstract) datacenter
						.getVmAllocationPolicy();

				double executionTimeVmSelectionMean = MathUtil.mean(vmAllocationPolicy
                        .getExecutionTimeHistoryVmSelection());
				double executionTimeVmSelectionStDev = MathUtil.stDev(vmAllocationPolicy
                        .getExecutionTimeHistoryVmSelection());
				double executionTimeHostSelectionMean = MathUtil.mean(vmAllocationPolicy
                        .getExecutionTimeHistoryHostSelection());
				double executionTimeHostSelectionStDev = MathUtil.stDev(vmAllocationPolicy
                        .getExecutionTimeHistoryHostSelection());
				double executionTimeVmReallocationMean = MathUtil.mean(vmAllocationPolicy
                        .getExecutionTimeHistoryVmReallocation());
				double executionTimeVmReallocationStDev = MathUtil.stDev(vmAllocationPolicy
                        .getExecutionTimeHistoryVmReallocation());
				double executionTimeTotalMean = MathUtil.mean(vmAllocationPolicy
                        .getExecutionTimeHistoryTotal());
				double executionTimeTotalStDev = MathUtil.stDev(vmAllocationPolicy
                        .getExecutionTimeHistoryTotal());

				Log.printLine(String.format(
                        "Execution time - VM selection mean: %.5f sec",
                        executionTimeVmSelectionMean));
				Log.printLine(String.format(
                        "Execution time - VM selection stDev: %.5f sec",
                        executionTimeVmSelectionStDev));
				Log.printLine(String.format(
                        "Execution time - host selection mean: %.5f sec",
                        executionTimeHostSelectionMean));
				Log.printLine(String.format(
                        "Execution time - host selection stDev: %.5f sec",
                        executionTimeHostSelectionStDev));
				Log.printLine(String.format(
                        "Execution time - VM reallocation mean: %.5f sec",
                        executionTimeVmReallocationMean));
				Log.printLine(String.format(
                        "Execution time - VM reallocation stDev: %.5f sec",
                        executionTimeVmReallocationStDev));
				Log.printLine(String.format("Execution time - total mean: %.5f sec", executionTimeTotalMean));
				Log.printLine(String
                        .format("Execution time - total stDev: %.5f sec", executionTimeTotalStDev));
			}
			Log.printLine();
		}

		Log.setDisabled(true);
	}

	/**
	 * Parses the experiment name.
	 * 
	 * @param name the name
	 * @return the string
	 */
	public static String parseExperimentName(String name) {
		Scanner scanner = new Scanner(name);
		StringBuilder csvName = new StringBuilder();
		scanner.useDelimiter("_");
		for (int i = 0; i < 4; i++) {
			if (scanner.hasNext()) {
				csvName.append(scanner.next() + ",");
			} else {
				csvName.append(",");
			}
		}
		scanner.close();
		return csvName.toString();
	}

	/**
	 * Gets the sla time per active host.
	 * 
	 * @param hosts the hosts
	 * @return the sla time per active host
	 */
	protected static double getSlaTimePerActiveHost(List<Host> hosts) {
		double slaViolationTimePerHost = 0;
		double totalTime = 0;

		for (Host _host : hosts) {
			HostDynamicWorkload host = (HostDynamicWorkload) _host;
			double previousTime = -1;
			double previousAllocated = 0;
			double previousRequested = 0;
			boolean previousIsActive = true;

			for (HostStateHistoryEntry entry : host.getStateHistory()) {
				if (previousTime != -1 && previousIsActive) {
					double timeDiff = entry.getTime() - previousTime;
					totalTime += timeDiff;
					if (previousAllocated < previousRequested) {
						slaViolationTimePerHost += timeDiff;
					}
				}

				previousAllocated = entry.getAllocatedMips();
				previousRequested = entry.getRequestedMips();
				previousTime = entry.getTime();
				previousIsActive = entry.isActive();
			}
		}

		return slaViolationTimePerHost / totalTime;
	}

	/**
	 * Gets the sla time per host.
	 * 
	 * @param hosts the hosts
	 * @return the sla time per host
	 */
	protected static double getSlaTimePerHost(List<Host> hosts) {
		double slaViolationTimePerHost = 0;
		double totalTime = 0;

		for (Host _host : hosts) {
			HostDynamicWorkload host = (HostDynamicWorkload) _host;
			double previousTime = -1;
			double previousAllocated = 0;
			double previousRequested = 0;

			for (HostStateHistoryEntry entry : host.getStateHistory()) {
				if (previousTime != -1) {
					double timeDiff = entry.getTime() - previousTime;
					totalTime += timeDiff;
					if (previousAllocated < previousRequested) {
						slaViolationTimePerHost += timeDiff;
					}
				}

				previousAllocated = entry.getAllocatedMips();
				previousRequested = entry.getRequestedMips();
				previousTime = entry.getTime();
			}
		}

		return slaViolationTimePerHost / totalTime;
	}

	/**
	 * Gets the sla metrics.
	 * 
	 * @param vms the vms
	 * @return the sla metrics
	 */
	protected static Map<String, Double> getSlaMetrics(List<Vm> vms) {
		Map<String, Double> metrics = new HashMap<String, Double>();
		List<Double> slaViolation = new LinkedList<Double>();
		double totalAllocated = 0;
		double totalRequested = 0;
		double totalUnderAllocatedDueToMigration = 0;

		for (Vm vm : vms) {
			double vmTotalAllocated = 0;
			double vmTotalRequested = 0;
			double vmUnderAllocatedDueToMigration = 0;
			double previousTime = -1;
			double previousAllocated = 0;
			double previousRequested = 0;
			boolean previousIsInMigration = false;

			for (VmStateHistoryEntry entry : vm.getStateHistory()) {
				if (previousTime != -1) {
					double timeDiff = entry.getTime() - previousTime;
					vmTotalAllocated += previousAllocated * timeDiff;
					vmTotalRequested += previousRequested * timeDiff;

					if (previousAllocated < previousRequested) {
						slaViolation.add((previousRequested - previousAllocated) / previousRequested);
						if (previousIsInMigration) {
							vmUnderAllocatedDueToMigration += (previousRequested - previousAllocated)
									* timeDiff;
						}
					}
				}

				previousAllocated = entry.getAllocatedMips();
				previousRequested = entry.getRequestedMips();
				previousTime = entry.getTime();
				previousIsInMigration = entry.isInMigration();
			}

			totalAllocated += vmTotalAllocated;
			totalRequested += vmTotalRequested;
			totalUnderAllocatedDueToMigration += vmUnderAllocatedDueToMigration;
		}

		metrics.put("overall", (totalRequested - totalAllocated) / totalRequested);
		if (slaViolation.isEmpty()) {
			metrics.put("average", 0.);
		} else {
			metrics.put("average", MathUtil.mean(slaViolation));
		}
		metrics.put("underallocated_migration", totalUnderAllocatedDueToMigration / totalRequested);
		// metrics.put("sla_time_per_vm_with_migration", slaViolationTimePerVmWithMigration /
		// totalTime);
		// metrics.put("sla_time_per_vm_without_migration", slaViolationTimePerVmWithoutMigration /
		// totalTime);

		return metrics;
	}

	/**
	 * Write data column.
	 * 
	 * @param data the data
	 * @param outputPath the output path
	 */
	public static void writeDataColumn(List<? extends Number> data, String outputPath) {
		File file = new File(outputPath);
		try {
			file.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(0);
		}
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			for (Number value : data) {
				writer.write(value.toString() + "\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	 * Write data row.
	 * 
	 * @param data the data
	 * @param outputPath the output path
	 */
	public static void writeDataRow(String data, String outputPath) {
		File file = new File(outputPath);
		try {
			file.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(0);
		}
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(data);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	 * Write metric history.
	 * 
	 * @param hosts the hosts
	 * @param vmAllocationPolicy the vm allocation policy
	 * @param outputPath the output path
	 */
	public static void writeMetricHistory(
			List<? extends Host> hosts,
			PowerVmAllocationPolicyMigrationAbstract vmAllocationPolicy,
			String outputPath) {
		// for (Host host : hosts) {
		for (int j = 0; j < 10; j++) {
			Host host = hosts.get(j);

			if (!vmAllocationPolicy.getTimeHistory().containsKey(host.getId())) {
				continue;
			}
			File file = new File(outputPath + "_" + host.getId() + ".csv");
			try {
				file.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
				System.exit(0);
			}
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(file));
				List<Double> timeData = vmAllocationPolicy.getTimeHistory().get(host.getId());
				List<Double> utilizationData = vmAllocationPolicy.getUtilizationHistory().get(host.getId());
				List<Double> metricData = vmAllocationPolicy.getMetricHistory().get(host.getId());

				for (int i = 0; i < timeData.size(); i++) {
					writer.write(String.format(
							"%.2f,%.2f,%.2f\n",
							timeData.get(i),
							utilizationData.get(i),
							metricData.get(i)));
				}
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

	/**
	 * Prints the Cloudlet objects.
	 * 
	 * @param list list of Cloudlets
	 */
	public static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "\t";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent + "Resource ID" + indent + "VM ID" + indent
                + "Time" + indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId());

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				Log.printLine(indent + "SUCCESS" + indent + indent + cloudlet.getResourceId() + indent
                        + cloudlet.getVmId() + indent + dft.format(cloudlet.getActualCPUTime()) + indent
                        + dft.format(cloudlet.getExecStartTime()) + indent + indent
                        + dft.format(cloudlet.getFinishTime()));
			}
		}
	}

	/**
	 * Prints the metric history.
	 * 
	 * @param hosts the hosts
	 * @param vmAllocationPolicy the vm allocation policy
	 */
	public static void printMetricHistory(
			List<? extends Host> hosts,
			PowerVmAllocationPolicyMigrationAbstract vmAllocationPolicy) {
		for (int i = 0; i < 10; i++) {
			Host host = hosts.get(i);

			Log.printLine("Host #" + host.getId());
			Log.printLine("Time:");
			if (!vmAllocationPolicy.getTimeHistory().containsKey(host.getId())) {
				continue;
			}
			for (Double time : vmAllocationPolicy.getTimeHistory().get(host.getId())) {
				Log.format("%.2f, ", time);
			}
			Log.printLine();

			for (Double utilization : vmAllocationPolicy.getUtilizationHistory().get(host.getId())) {
				Log.format("%.2f, ", utilization);
			}
			Log.printLine();

			for (Double metric : vmAllocationPolicy.getMetricHistory().get(host.getId())) {
				Log.format("%.2f, ", metric);
			}
			Log.printLine();
		}
	}

	public static int generateRandomInteger(int aStart, int aEnd, Random aRandom){
		if (aStart > aEnd) {
			throw new IllegalArgumentException("Start cannot exceed End.");
		}
		//get the range, casting to long to avoid overflow problems
		long range = (long)aEnd - (long)aStart + 1;
		// compute a fraction of the range, 0 <= frac < range
		long fraction = (long)(range * aRandom.nextDouble());
		int randomNumber =  (int)(fraction + aStart);
		return randomNumber;
	}
}
