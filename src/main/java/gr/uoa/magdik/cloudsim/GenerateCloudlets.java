/*
 *
 */
package gr.uoa.magdik.cloudsim;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelNull;
import org.cloudbus.cloudsim.UtilizationModelStochastic;

import java.util.ArrayList;
import java.util.List;

public class GenerateCloudlets {

	/**
	 * Creates the cloudlet list.
	 * 
	 * @param brokerId the broker id
	 * @param cloudletsNumber the cloudlets number
	 * 
	 * @return the list<cloudlet>
	 */
	public static List<Cloudlet> createCloudletList(int brokerId, int cloudletsNumber) {
		List<Cloudlet> list = new ArrayList<Cloudlet>();

		long fileSize = 300;
		long outputSize = 300;
		long seed = HyperConstants.CLOUDLET_UTILIZATION_SEED;
		UtilizationModel utilizationModelNull = new UtilizationModelNull();

		for (int i = 0; i < cloudletsNumber; i++) {
			Cloudlet cloudlet = null;
			if (seed == -1) {
				cloudlet = new Cloudlet(
						i,
						HyperConstants.CLOUDLET_LENGTH,
						HyperConstants.CLOUDLET_PES,
						fileSize,
						outputSize,
						new UtilizationModelStochastic(),
						utilizationModelNull,
						utilizationModelNull);
			} else {
				cloudlet = new Cloudlet(
						i,
						HyperConstants.CLOUDLET_LENGTH,
						HyperConstants.CLOUDLET_PES,
						fileSize,
						outputSize,
						new UtilizationModelStochastic(seed * i),
						utilizationModelNull,
						utilizationModelNull);
			}
			cloudlet.setUserId(brokerId);
			cloudlet.setVmId(i);
			list.add(cloudlet);
		}
		return list;
	}
}
