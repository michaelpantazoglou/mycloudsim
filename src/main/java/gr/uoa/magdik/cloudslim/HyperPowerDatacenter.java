/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package gr.uoa.magdik.cloudslim;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.PredicateType;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PowerDatacenter is a class that enables simulation of power-aware data centers.
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
public class HyperPowerDatacenter extends Datacenter {

	/** The power. */
	private double power;

	/** The disable migrations. */
	private boolean disableMigrations;

	/** The cloudlet submited. */
	private double cloudletSubmitted;

	/** The migration count. */
	private int migrationCount;

	/**
	 * Instantiates a new datacenter.
	 *
	 * @param name the name
	 * @param characteristics the res config
	 * @param schedulingInterval the scheduling interval
	 * @param vmAllocationPolicy the vm provisioner
	 * @param storageList the storage list
	 * @throws Exception the exception
	 */
	public HyperPowerDatacenter(
            String name,
            DatacenterCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);

		setPower(0.0);
		setDisableMigrations(false);
		setCloudletSubmitted(-1);
		setMigrationCount(0);
	}


    @Override
    public void processEvent(SimEvent ev) {
        int srcId = -1;

        switch (ev.getTag()) {
            // Resource characteristics inquiry
            case HyperCloudSimTags.RESOURCE_CHARACTERISTICS:
                srcId = ((Integer) ev.getData()).intValue();
                sendNow(srcId, ev.getTag(), getCharacteristics());
                break;

            // Resource dynamic info inquiryrypoulia

            case HyperCloudSimTags.RESOURCE_DYNAMICS:
                srcId = ((Integer) ev.getData()).intValue();
                sendNow(srcId, ev.getTag(), 0);
                break;

            case HyperCloudSimTags.RESOURCE_NUM_PE:
                srcId = ((Integer) ev.getData()).intValue();
                int numPE = getCharacteristics().getNumberOfPes();
                sendNow(srcId, ev.getTag(), numPE);
                break;

            case HyperCloudSimTags.RESOURCE_NUM_FREE_PE:
                srcId = ((Integer) ev.getData()).intValue();
                int freePesNumber = getCharacteristics().getNumberOfFreePes();
                sendNow(srcId, ev.getTag(), freePesNumber);
                break;

            // New Cloudlet arrives
            case HyperCloudSimTags.CLOUDLET_SUBMIT:
                //System.out.println("PROCESSEVENTCLOUDLET1");
                processCloudletSubmit(ev, false);
                break;

            // New Cloudlet arrives, but the sender asks for an ack
            case HyperCloudSimTags.CLOUDLET_SUBMIT_ACK:
                //System.out.println("PROCESSEVENTCLOUDLET2");
                processCloudletSubmit(ev, true);
                break;

            // Cancels a previously submitted Cloudlet
            case HyperCloudSimTags.CLOUDLET_CANCEL:
                processCloudlet(ev, HyperCloudSimTags.CLOUDLET_CANCEL);
                break;

            // Pauses a previously submitted Cloudlet
            case HyperCloudSimTags.CLOUDLET_PAUSE:
                processCloudlet(ev, HyperCloudSimTags.CLOUDLET_PAUSE);
                break;

            // Pauses a previously submitted Cloudlet, but the sender
            // asks for an acknowledgement
            case HyperCloudSimTags.CLOUDLET_PAUSE_ACK:
                processCloudlet(ev, HyperCloudSimTags.CLOUDLET_PAUSE_ACK);
                break;

            // Resumes a previously submitted Cloudlet
            case HyperCloudSimTags.CLOUDLET_RESUME:
                processCloudlet(ev, HyperCloudSimTags.CLOUDLET_RESUME);
                break;

            // Resumes a previously submitted Cloudlet, but the sender
            // asks for an acknowledgement
            case HyperCloudSimTags.CLOUDLET_RESUME_ACK:
                processCloudlet(ev, HyperCloudSimTags.CLOUDLET_RESUME_ACK);
                break;

            // Moves a previously submitted Cloudlet to a different resource
            case HyperCloudSimTags.CLOUDLET_MOVE:
                processCloudletMove((int[]) ev.getData(), HyperCloudSimTags.CLOUDLET_MOVE);
                break;

            // Moves a previously submitted Cloudlet to a different resource
            case HyperCloudSimTags.CLOUDLET_MOVE_ACK:
                processCloudletMove((int[]) ev.getData(), HyperCloudSimTags.CLOUDLET_MOVE_ACK);
                break;

            // Checks the status of a Cloudlet
            case HyperCloudSimTags.CLOUDLET_STATUS:
                processCloudletStatus(ev);
                break;

            // Ping packet
            case HyperCloudSimTags.INFOPKT_SUBMIT:
                processPingRequest(ev);
                break;

            case HyperCloudSimTags.VM_CREATE:
                processVmCreate(ev, false);
                break;

            case HyperCloudSimTags.VM_CREATE_ACK:
                processVmCreate(ev, true);
                break;

            case HyperCloudSimTags.VM_DESTROY:
                processVmDestroy(ev, false);
                break;

            case HyperCloudSimTags.VM_DESTROY_ACK:
                processVmDestroy(ev, true);
                break;

            case HyperCloudSimTags.VM_MIGRATE:
                processVmMigrate(ev, false);
                break;

            case HyperCloudSimTags.VM_MIGRATE_ACK:
                processVmMigrate(ev, true);
                break;

            case HyperCloudSimTags.VM_DATA_ADD:
                processDataAdd(ev, false);
                break;

            case HyperCloudSimTags.VM_DATA_ADD_ACK:
                processDataAdd(ev, true);
                break;

            case HyperCloudSimTags.VM_DATA_DEL:
                processDataDelete(ev, false);
                break;

            case HyperCloudSimTags.VM_DATA_DEL_ACK:
                processDataDelete(ev, true);
                break;

            case HyperCloudSimTags.VM_DATACENTER_EVENT:
                updateCloudletProcessing();
                checkCloudletCompletion();
                break;
            case HyperCloudSimTags.HOST_OFF:
                turnoffhost(ev);
                break;
            case HyperCloudSimTags.HOST_ON:
                turnonhost(ev);
                break;
            case HyperCloudSimTags.HOST_ON_MIGRATE:
                turnonhostandmigrate(ev);
                break;
            case HyperCloudSimTags.HOST_ON_ALLOCATE:
                turnonhostandallocate(ev);
                break;

            // other unknown tags are processed by this method
            default:
                processOtherEvent(ev);
                break;
        }
    }

    protected void turnoffhost(SimEvent ev)
    {
        HyperPowerHost h = (HyperPowerHost) ev.getData();
        HyperVmAllocationPolicy hp = (HyperVmAllocationPolicy) getVmAllocationPolicy();
        hp.offHosts.add(h);
        h.switchOff();
    }

    protected void turnonhost(SimEvent ev)
    {
        HyperPowerHost h = (HyperPowerHost) ev.getData();
        h.switchOn();
    }

    protected void turnonhostandmigrate(SimEvent ev)
    {
        Map<String, Object> map = (Map<String, Object>) ev.getData();
        HyperPowerHost h = (HyperPowerHost) map.get("host");
        HyperVmAllocationPolicy hp = (HyperVmAllocationPolicy) getVmAllocationPolicy();
        hp.offHosts.remove(h);
        h.switchOn();
        processVmMigrate( ev, false);
    }

    protected void turnonhostandallocate(SimEvent ev)
    {
        Map<String, Object> map = (Map<String, Object>) ev.getData();
        HyperPowerHost h = (HyperPowerHost) map.get("host");
        PowerVm pv = (PowerVm) map.get("vm");
        HyperVmAllocationPolicy hp = (HyperVmAllocationPolicy) getVmAllocationPolicy();
        hp.offHosts.remove(h);
        h.switchOn();
        getVmAllocationPolicy().allocateHostForVm(pv, h);
    }


    /**
	 * Updates processing of each cloudlet running in this PowerDatacenter. It is necessary because
	 * Hosts and VirtualMachines are simple objects, not entities. So, they don't receive events and
	 * updating cloudlets inside them must be called from the outside.
	 * 
	 * @pre $none
	 * @post $none
	 */
	@Override
	protected void updateCloudletProcessing() {
		if (getCloudletSubmitted() == -1 || getCloudletSubmitted() == CloudSim.clock()) {
			CloudSim.cancelAll(getId(), new PredicateType(HyperCloudSimTags.VM_DATACENTER_EVENT));
			schedule(getId(), getSchedulingInterval(), HyperCloudSimTags.VM_DATACENTER_EVENT);
			return;
		}
		double currentTime = CloudSim.clock();

		// if some time passed since last processing
		if (currentTime > getLastProcessTime()) {
			System.out.print(currentTime + " ");

			double minTime = updateCloudetProcessingWithoutSchedulingFutureEventsForce();

			if (!isDisableMigrations()) {

				List<Map<String, Object>> migrationMap = getVmAllocationPolicy().optimizeAllocation(
						getVmList());

                HyperVmAllocationPolicy hp = (HyperVmAllocationPolicy) getVmAllocationPolicy();
				if (migrationMap != null) {
					for (Map<String, Object> migrate : migrationMap) {
						Vm vm = (Vm) migrate.get("vm");
						HyperPowerHost targetHost = (HyperPowerHost) migrate.get("host");
						HyperPowerHost oldHost = (HyperPowerHost) vm.getHost();

						if (oldHost == null) {
							Log.formatLine(
                                    "%.2f: Migration of VM #%d to Host #%d is started",
                                    currentTime,
                                    vm.getId(),
                                    targetHost.getId());
						} else {
							Log.formatLine(
                                    "%.2f: Migration of VM #%d from Host #%d to Host #%d is started",
                                    currentTime,
                                    vm.getId(),
                                    oldHost.getId(),
                                    targetHost.getId());
						}

						targetHost.addMigratingInVm(vm);
						incrementMigrationCount();

						/** VM migration delay = RAM / bandwidth **/
						// we use BW / 2 to model BW available for migration purposes, the other
						// half of BW is for VM communication
						// around 16 seconds for 1024 MB using 1 Gbit/s network

                        if(targetHost.tobeon)
                        {
                            send(
                                    getId(),
                                    10 + (vm.getRam() / ((double) targetHost.getBw() / (2 * 8000))),
                                    HyperCloudSimTags.HOST_ON_MIGRATE,
                                    migrate);
                        }
                        else {
                            send(
                                    getId(),
                                    vm.getRam() / ((double) targetHost.getBw() / (2 * 8000)),
                                    HyperCloudSimTags.VM_MIGRATE,
                                    migrate);
                        }
					}
				}

                for(Host ph : hp.tobeoffHosts)
                {
                    send(
                            getId(),
                            10,
                            HyperCloudSimTags.HOST_OFF,
                            ph);
                }
                /*for(Host ph : hp.tobeonHosts)
                {
                    send(
                            getId(),
                            10,
                            HyperCloudSimTags.HOST_ON_ALLOCATE,
                            ph);
                }*/
			}

			// schedules an event to the next time
			if (minTime != Double.MAX_VALUE) {
				CloudSim.cancelAll(getId(), new PredicateType(HyperCloudSimTags.VM_DATACENTER_EVENT));
				send(getId(), getSchedulingInterval(), HyperCloudSimTags.VM_DATACENTER_EVENT);
			}

			setLastProcessTime(currentTime);
		}


	}

    public void wakeuphost(HyperPowerHost ph, HyperPowerVm pvm)
    {
        Map<String, Object> allocate = new HashMap<String, Object>();
        allocate.put("vm", pvm);
        allocate.put("host", ph);
        send(
                getId(),
                10,
                HyperCloudSimTags.HOST_ON_ALLOCATE,
                allocate);
    }

	/**
	 * Update cloudet processing without scheduling future events.
	 * 
	 * @return the double
	 */
	protected double updateCloudetProcessingWithoutSchedulingFutureEvents() {
		if (CloudSim.clock() > getLastProcessTime()) {
			return updateCloudetProcessingWithoutSchedulingFutureEventsForce();
		}
		return 0;
	}

	/**
	 * Update cloudet processing without scheduling future events.
	 * 
	 * @return the double
	 */
	protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce() {
		double currentTime = CloudSim.clock();
		double minTime = Double.MAX_VALUE;
		double timeDiff = currentTime - getLastProcessTime();
		double timeFrameDatacenterEnergy = 0.0;

		Log.printLine("\n\n--------------------------------------------------------------\n\n");
		Log.formatLine("New resource usage for the time frame starting at %.2f:", currentTime);

		for (HyperPowerHost host : this.<HyperPowerHost> getHostList()) {
			Log.printLine();

			double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
			if (time < minTime) {
				minTime = time;
			}

			Log.formatLine(
                    "%.2f: [Host #%d] utilization is %.2f%%",
                    currentTime,
                    host.getId(),
                    host.getUtilizationOfCpu() * 100);
		}

		if (timeDiff > 0) {
			Log.formatLine(
                    "\nEnergy consumption for the last time frame from %.2f to %.2f:",
                    getLastProcessTime(),
                    currentTime);

			for (HyperPowerHost host : this.<HyperPowerHost> getHostList()) {
				double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
				double utilizationOfCpu = host.getUtilizationOfCpu();
				double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
						previousUtilizationOfCpu,
						utilizationOfCpu,
						timeDiff);
				//System.out.println("HOSTENERGY = " + timeFrameHostEnergy);
				//System.out.println("HOSTUTIL = " + utilizationOfCpu);
				//System.out.println("PREHOSTUTIL = " + previousUtilizationOfCpu);
				timeFrameDatacenterEnergy += timeFrameHostEnergy;

				Log.printLine();
				Log.formatLine(
                        "%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%",
                        currentTime,
                        host.getId(),
                        getLastProcessTime(),
                        previousUtilizationOfCpu * 100,
                        utilizationOfCpu * 100);
				Log.formatLine(
                        "%.2f: [Host #%d] energy is %.2f W*sec",
                        currentTime,
                        host.getId(),
                        timeFrameHostEnergy);
			}

			Log.formatLine(
                    "\n%.2f: Data center's energy is %.2f W*sec\n",
                    currentTime,
                    timeFrameDatacenterEnergy);
		}

		setPower(getPower() + timeFrameDatacenterEnergy);

		checkCloudletCompletion();

		/** Remove completed VMs **/
		System.out.println("Removing completed vms");
		for (HyperPowerHost host : this.<HyperPowerHost> getHostList()) {
			for (Vm vm : host.getCompletedVms()) {
				getVmAllocationPolicy().deallocateHostForVm(vm);
				getVmList().remove(vm);
				Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
			}
		}

		Log.printLine();

		setLastProcessTime(currentTime);
		return minTime;
	}

	/*
	 * (non-Javadoc)
	 * @see org.cloudbus.cloudsim.Datacenter#processVmMigrate(org.cloudbus.cloudsim.core.SimEvent,
	 * boolean)
	 */
	@Override
	protected void processVmMigrate(SimEvent ev, boolean ack) {
		updateCloudetProcessingWithoutSchedulingFutureEvents();
		super.processVmMigrate(ev, ack);
		SimEvent event = CloudSim.findFirstDeferred(getId(), new PredicateType(HyperCloudSimTags.VM_MIGRATE));
		if (event == null || event.eventTime() > CloudSim.clock()) {
			updateCloudetProcessingWithoutSchedulingFutureEventsForce();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.Datacenter#processCloudletSubmit(cloudsim.core.SimEvent, boolean)
	 */
	@Override
	protected void processCloudletSubmit(SimEvent ev, boolean ack) {
		super.processCloudletSubmit(ev, ack);
		setCloudletSubmitted(CloudSim.clock());
	}

	/**
	 * Gets the power.
	 * 
	 * @return the power
	 */
	public double getPower() {
		return power;
	}

	/**
	 * Sets the power.
	 * 
	 * @param power the new power
	 */
	protected void setPower(double power) {
		this.power = power;
	}

	/**
	 * Checks if PowerDatacenter is in migration.
	 * 
	 * @return true, if PowerDatacenter is in migration
	 */
	protected boolean isInMigration() {
		boolean result = false;
		for (Vm vm : getVmList()) {
			if (vm.isInMigration()) {
				result = true;
				break;
			}
		}
		return result;
	}

	/**
	 * Checks if is disable migrations.
	 * 
	 * @return true, if is disable migrations
	 */
	public boolean isDisableMigrations() {
		return disableMigrations;
	}

	/**
	 * Sets the disable migrations.
	 * 
	 * @param disableMigrations the new disable migrations
	 */
	public void setDisableMigrations(boolean disableMigrations) {
		this.disableMigrations = disableMigrations;
	}

	/**
	 * Checks if is cloudlet submited.
	 * 
	 * @return true, if is cloudlet submited
	 */
	protected double getCloudletSubmitted() {
		return cloudletSubmitted;
	}

	/**
	 * Sets the cloudlet submited.
	 * 
	 * @param cloudletSubmitted the new cloudlet submited
	 */
	protected void setCloudletSubmitted(double cloudletSubmitted) {
		this.cloudletSubmitted = cloudletSubmitted;
	}

	/**
	 * Gets the migration count.
	 * 
	 * @return the migration count
	 */
	public int getMigrationCount() {
		return migrationCount;
	}

	/**
	 * Sets the migration count.
	 * 
	 * @param migrationCount the new migration count
	 */
	protected void setMigrationCount(int migrationCount) {
		this.migrationCount = migrationCount;
	}

	/**
	 * Increment migration count.
	 */
	protected void incrementMigrationCount() {
		setMigrationCount(getMigrationCount() + 1);
	}



}
