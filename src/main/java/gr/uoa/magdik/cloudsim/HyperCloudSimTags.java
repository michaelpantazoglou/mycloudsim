/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package gr.uoa.magdik.cloudsim;

import org.cloudbus.cloudsim.core.CloudSimTags;


public class HyperCloudSimTags extends CloudSimTags {

    public static final int HOST_ON = BASE + 49;

    public static final int HOST_OFF = BASE + 50;

    public static final int HOST_ON_MIGRATE = BASE + 51;

    public static final int HOST_ON_ALLOCATE = BASE + 52;

    public static final int HEARTBEAT = BASE + 53;

    public static final int FORWARD_MSG = BASE + 54;

    public static final int REMOVEVM = BASE + 55;





    /** Private Constructor */
	private HyperCloudSimTags() {
        super();
        throw new UnsupportedOperationException("CloudSim Tags cannot be instantiated");
	}

}
