package gr.uoa.madgik.cloudsim;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.examples.power.Helper;
import org.cloudbus.cloudsim.examples.power.RunnerAbstract;
import org.cloudbus.cloudsim.examples.power.random.RandomHelper;
import org.cloudbus.cloudsim.examples.power.random.RandomRunner;

import java.util.Calendar;
import java.util.HashMap;

/**
 * Class extended from RundomRunner
 */
public class HyperRunner extends HyperRunnerAbstract {

	/**
	 * @param enableOutput
	 * @param outputToFile
	 * @param inputFolder
	 * @param outputFolder
	 * @param workload
	 * @param vmAllocationPolicy
	 * @param vmSelectionPolicy
	 * @param parameter
	 */
	public HyperRunner(
            boolean enableOutput,
            boolean outputToFile,
            String inputFolder,
            String outputFolder,
            String workload,
            String vmAllocationPolicy,
            String vmSelectionPolicy,
            String parameter) {
		super(
				enableOutput,
				outputToFile,
				inputFolder,
				outputFolder,
				workload,
				vmAllocationPolicy,
				vmSelectionPolicy,
				parameter);
	}


    /*
 * (non-Javadoc)
 *
 * @see org.cloudbus.cloudsim.examples.power.RunnerAbstract#init(java.lang.String)
 */
    @Override
    protected void init(String inputFolder) {
        try {
            CloudSim.init(1, Calendar.getInstance(), false);

            broker = HyperHelper.createBroker();
            int brokerId = broker.getId();
            cloudletList = GenerateCloudlets.createCloudletList(brokerId, HyperConstants.NUMBER_OF_CLOUDLETS);
            vmList = HyperHelper.createVmList(brokerId, cloudletList.size());
            double log2base = Math.log(HyperConstants.NUMBER_OF_HOSTS)/Math.log(2);
            hostList = HyperHelper.createHostList((int)log2base);
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            System.exit(0);
        }
    }





}
