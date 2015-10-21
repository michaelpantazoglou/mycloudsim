package gr.uoa.magdik.cloudslim;



import gr.uoa.magdik.cloudslim.HyperRunner;

import java.io.IOException;

/**
 * Created by tchalas on 3/18/15.
 */
public class Main {
    /**
     * The main method.
     *
     * @param args the arguments
     * @throws java.io.IOException Signals that an I/O exception has occurred.
     */
    public static void main(String[] args) throws IOException {
        boolean enableOutput = true;
        boolean outputToFile = false;
        String inputFolder = "";
        String outputFolder = "";
        String workload = "random";//"random"; // Random workload
        String vmAllocationPolicy = "greenrandom";
        String vmSelectionPolicy = "rs"; // Maximum Correlation (MC) VM selection policy
        String parameter = "1.5"; // the safety parameter of the IQR policy

        new HyperRunner(
                enableOutput,
                outputToFile,
                inputFolder,
                outputFolder,
                workload,
                vmAllocationPolicy,
                vmSelectionPolicy,
                parameter);
    }
}
