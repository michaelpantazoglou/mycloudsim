/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package gr.uoa.magdik.cloudslim;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.util.MathUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * The class of a VM that stores its CPU utilization history. The history is used by VM allocation
 * and selection policies.
 * 
 * If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:
 * 
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 2.0
 */
public class HyperPowerVm extends PowerVm {


	PowerModel pm;

	public double getDelay() {
		return delay;
	}

	public void setDelay(double delay) {
		this.delay = delay;
	}

	double delay;
	double removedelay;


	public double getRemovedelay() {
		return removedelay;
	}

	public void setRemovedelay(double removedelay) {
		this.removedelay = removedelay;
	}

	/**
	 * Instantiates a new power vm.
	 *
	 * @param id the id
	 * @param userId the user id
	 * @param mips the mips
	 * @param pesNumber the pes number
	 * @param ram the ram
	 * @param bw the bw
	 * @param size the size
	 * @param priority the priority
	 * @param vmm the vmm
	 * @param cloudletScheduler the cloudlet scheduler
	 * @param schedulingInterval the scheduling interval
	 */
	public HyperPowerVm(
            final int id,
            final int userId,
            final double mips,
            final int pesNumber,
            final int ram,
            final long bw,
            final long size,
            final int priority,
            final String vmm,
            final CloudletScheduler cloudletScheduler,
            final double schedulingInterval) {

		super(id, userId, mips, pesNumber, ram, bw, size, priority, vmm, cloudletScheduler, schedulingInterval);
		pm = new PowerModelLinear(0.00, 0.0);
		delay = 0;
		removedelay = 0;
	}



	/**
	 * Gets the power. For this moment only consumed by all PEs.
	 *
	 * @return the power
	 */
	public double getPower() {
		return 5.0;
        //return getPower(getUtilizationOfCpu());
	}

	/**
	 * Gets the power. For this moment only consumed by all PEs.
	 *
	 * @param utilization the utilization
	 * @return the power
	 */
	protected double getPower(double utilization) {
		double power = 0;
		try {
			PowerHost ph = (PowerHost) getHost();
            if(ph == null)
            {
                //System.exit(0);
            }
			power = ph.getPowerModel().getPower(utilization);
//			System.out.println("VMpower = " + power);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return power;
	}


}
