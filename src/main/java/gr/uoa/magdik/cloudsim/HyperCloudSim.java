/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package gr.uoa.magdik.cloudsim;

import org.cloudbus.cloudsim.core.*;
import org.cloudbus.cloudsim.power.PowerDatacenter;

import java.util.*;

/**
 * This class extends the CloudSimCore to enable network simulation in CloudSim. Also, it disables
 * all the network models from CloudSim, to provide a simpler simulation of networking. In the
 * network model used by CloudSim, a topology file written in BRITE format is used to describe the
 * network. Later, nodes in such file are mapped to CloudSim entities. Delay calculated from the
 * BRITE model are added to the messages send through CloudSim. Messages using the old model are
 * converted to the apropriate methods with the correct parameters.
 * 
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class HyperCloudSim extends CloudSim{

	public static boolean runClockTick() {
		SimEntity ent;
		boolean queue_empty;

		int entities_size = entities.size();

		for (int i = 0; i < entities_size; i++) {
			ent = entities.get(i);
			if (ent.getState() == SimEntity.RUNNABLE) {
				ent.run();
			}
		}

		// If there are more future events then deal with them
		if (future.size() > 0) {
			List<SimEvent> toRemove = new ArrayList<SimEvent>();
			Iterator<SimEvent> fit = future.iterator();
			queue_empty = false;
			SimEvent first = fit.next();
			processEvent(first);
			future.remove(first);

			fit = future.iterator();

			// Check if next events are at same time...
			boolean trymore = fit.hasNext();
			while (trymore) {
				SimEvent next = fit.next();
				if (next.eventTime() == first.eventTime()) {
					processEvent(next);
					toRemove.add(next);
					trymore = fit.hasNext();
				} else {
					trymore = false;
				}
			}
			future.removeAll(toRemove);

		}
		return false;
	}


}
