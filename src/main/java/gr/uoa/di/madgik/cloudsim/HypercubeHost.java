package gr.uoa.di.madgik.cloudsim;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.lists.HostList;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by michael on 11/9/14.
 */
public class HypercubeHost extends Host {

	/**
	 * Contains the hypercube neighbors of this host.
	 */
	private Map<Integer, Integer> neighbors;

	/**
	 * Instantiates a new host.
	 *
	 * @param id             the id
	 * @param ramProvisioner the ram provisioner
	 * @param bwProvisioner  the bw provisioner
	 * @param storage        the storage
	 * @param peList         the pe list
	 * @param vmScheduler    the vm scheduler
	 */
	public HypercubeHost(int id, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage, List<? extends Pe> peList, VmScheduler vmScheduler) {
		super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler);
		neighbors = new HashMap<Integer, Integer>();
	}

	/**
	 * Sets the neighbor at the specified dimension.
	 *
	 * @param dimension
	 * @param hostId
	 */
	public void setNeighbor(Integer dimension, Integer hostId) {
		neighbors.put(dimension, hostId);
	}

	/**
	 * Gets the neighbor at the specified dimension.
	 *
	 * @param dimension
	 * @return
	 */
	public Integer getNeighbor(Integer dimension) {
		return neighbors.get(dimension);
	}

	/**
	 * Gets all neighbors.
	 *
	 * @return
	 */
	public Map<Integer, Integer> getNeighbors() {
		return neighbors;
	}
}
