package gr.uoa.madgik.cloudsim;

import gr.uoa.di.madgik.cloudsim.HypercubeHost;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by michael on 11/9/14.
 */
public class Main {

	/** Hypercube overlay dimension */
	private static final Integer HYPERCUBE_DIMENSION = 6;

	/** Million instructions per second */
	private static final Integer MIPS = 1000;

	/** RAM of each host */
	private static final Integer HOST_RAM = 4096;

	/** Storage capacity of each host */
	private static final Integer HOST_STORAGE = 1000000;

	/** Bandwidth of each host */
	private static final Integer HOST_BANDWIDTH = 10000;


	private static int hostId = 0;

	/**
	 * This method returns a list of Hypercube hosts organized in a complete N-dimensional hypercube.
	 *
	 * @param dimension
	 * @return
	 */
	private static List<HypercubeHost> hypercube(int dimension) {
		List<HypercubeHost> l = new ArrayList<>();
		if (dimension == 0) {

			// List of CPUs (or Processing Elements)
			List<Pe> peList = new ArrayList<>();
			peList.add(new Pe(0, new PeProvisionerSimple(MIPS)));

			HypercubeHost h1 = new HypercubeHost(
					hostId++,
					new RamProvisionerSimple(HOST_RAM),
					new BwProvisionerSimple(HOST_BANDWIDTH),
					HOST_STORAGE,
					peList,
					new VmSchedulerTimeShared(peList));
			l.add(h1);

			HypercubeHost h2 = new HypercubeHost(
					hostId++,
					new RamProvisionerSimple(HOST_RAM),
					new BwProvisionerSimple(HOST_BANDWIDTH),
					HOST_STORAGE,
					peList,
					new VmSchedulerTimeShared(peList));
			l.add(h2);

			h1.setNeighbor(dimension, h2.getId());
			h2.setNeighbor(dimension, h1.getId());

			return l;
		}

		List<HypercubeHost> l1 = hypercube(dimension - 1);
		List<HypercubeHost> l2 = hypercube(dimension - 1);

		for (int i = 0; i<Math.pow(2, dimension); i++) {
			l1.get(i).setNeighbor(dimension, l2.get(i).getId());
			l2.get(i).setNeighbor(dimension, l1.get(i).getId());
		}

		l = new ArrayList<>(l1);
		l.addAll(l2);

		return l;
	}

	/**
	 * Creates the Data center.
	 *
	 * @return the created Data center
	 */
	private static Datacenter createDatacenter() {

		// List of hosts
		List<HypercubeHost> hostList = hypercube(HYPERCUBE_DIMENSION - 1);
		assert (hostList.size() == Math.pow(2, HYPERCUBE_DIMENSION));
		for (HypercubeHost h : hostList) {
			for (int i=0; i<HYPERCUBE_DIMENSION; i++) {
				System.out.println("Neighbor of h" + h.getId() + " in dimension " + i + ": h" + h.getNeighbor(i));
			}
		}

		String arch = "x86";      // system architecture
		String os = "Linux";          // operating system
		String vmm = "Xen";
		double time_zone = 10.0;         // time zone this resource located
		double cost = 3.0;              // the cost of using processing in this resource
		double costPerMem = 0.05;		// the cost of using memory in this resource
		double costPerStorage = 0.001;	// the cost of using storage in this resource
		double costPerBw = 0.0;			// the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>();	//we are not adding SAN devices by now

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter("Datacenter", characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	public static void main(String... args) throws Exception {

		// Initialize the CloudSim package
		CloudSim.init(1, Calendar.getInstance(), false);

		// Create the Data center
		Datacenter datacenter = createDatacenter();
	}
}
