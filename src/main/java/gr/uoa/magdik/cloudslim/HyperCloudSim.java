/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package gr.uoa.magdik.cloudslim;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.*;
import org.cloudbus.cloudsim.core.predicates.Predicate;
import org.cloudbus.cloudsim.core.predicates.PredicateAny;
import org.cloudbus.cloudsim.core.predicates.PredicateNone;
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



	/**
	 * Internal method used to run one tick of the simulation. This method should <b>not</b> be
	 * called in simulations.
	 * 
	 * @return true, if successful otherwise
	 */
	public static boolean runClockTick() {
		SimEntity ent;
		boolean queue_empty;
		
		int entities_size = entities.size();
        //System.out.println("CLOCK " + clock);


		for (int i = 0; i < entities_size; i++) {
			ent = entities.get(i);

			//checking if entity is a hyperdatacenter. If yes we need to check the hosts
            if(ent.getClass().equals(PowerDatacenter.class))
            {
                PowerDatacenter pd = (PowerDatacenter) ent;
                HyperVmAllocationPolicy hp = (HyperVmAllocationPolicy) pd.getVmAllocationPolicy();
                hp.monitorDatacenter();
            }

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
            System.out.println(first.getTag());
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

		} else {
			//queue_empty = true;
			//running = false;
			//printMessage("Simulation: No more future events");
		}
		return false;
		//return queue_empty;
	}


}
