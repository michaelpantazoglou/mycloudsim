/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package gr.uoa.magdik.cloudslim;

import org.cloudbus.cloudsim.core.CloudSimTags;

/**
 * Contains various static command tags that indicate a type of action that needs to be undertaken
 * by CloudSim entities when they receive or send events. <b>NOTE:</b> To avoid conflicts with other
 * tags, CloudSim reserves negative numbers, 0 - 299, and 9600.
 * 
 * @author Manzur Murshed
 * @author Rajkumar Buyya
 * @author Anthony Sulistio
 * @since CloudSim Toolkit 1.0
 */
public class HyperCloudSimTags extends CloudSimTags {



    public static final int HOST_ON = BASE + 49;

    public static final int HOST_OFF = BASE + 50;

    public static final int HOST_ON_MIGRATE = BASE + 51;

    public static final int HOST_ON_ALLOCATE = BASE + 52;

	/** Private Constructor */
	private HyperCloudSimTags() {
        super();
        throw new UnsupportedOperationException("CloudSim Tags cannot be instantiated");
	}

}
