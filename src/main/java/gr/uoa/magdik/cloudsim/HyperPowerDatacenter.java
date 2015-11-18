/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package gr.uoa.magdik.cloudsim;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.PredicateType;
import org.cloudbus.cloudsim.power.PowerVm;

import java.io.PrintWriter;
import java.util.*;

import static gr.uoa.magdik.cloudsim.HyperHelper.generateRandomInteger;


public class HyperPowerDatacenter extends Datacenter {

	/** The power. */
	private double power;

	/** The disable migrations. */
	private boolean disableMigrations;

	/** The cloudlet submited. */
	private double cloudletSubmitted;

	/** The migration count. */
	private int migrationCount;

    public int getHostonoffCount() {
        return hostonoffCount;
    }
    public void setHostonoffCount(int hostonoffCount) {
        this.hostonoffCount = hostonoffCount;
    }

    /** The switching on/off host count. */
    private int hostonoffCount;

    int round = 0;

    public PrintWriter powertimelog;
    public PrintWriter onhoststimelog;
    public PrintWriter vmstimelog;

    int oldmigrationcount = 0;
    int oldhostonoffcount = 0;
    int oldvmcount = 0;

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    int mode;

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    int rate;

    public int getHostoncount() {
        return hostoncount;
    }

    public void setHostoncount(int hostoncount) {
        this.hostoncount = hostoncount;
    }

    public int getHostoffcount() {
        return hostoffcount;
    }

    public void setHostoffcount(int hostoffcount) {
        this.hostoffcount = hostoffcount;
    }

    int hostoncount = 0;
    int hostoffcount = 0;
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
                processCloudletSubmit(ev, false);
                break;

            // New Cloudlet arrives, but the sender asks for an ack
            case HyperCloudSimTags.CLOUDLET_SUBMIT_ACK:
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
            case HyperCloudSimTags.REMOVEVM:
                removeVm(ev);
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
        hp.tobeoffHosts.remove(h);
        //h.shutdownEntity();
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
        hp.tobeonHosts.remove(h);
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
        System.out.println("TIME :" + CloudSim.clock() + " - End of Period");
        long power = 0;
        power += (migrationCount - oldmigrationcount) * 20;
        power += (hostonoffCount - oldhostonoffcount) * 100;
        power += getVmList().size() * 5;
        int newvms = getVmList().size() - oldvmcount;
        oldvmcount = getVmList().size();
        oldhostonoffcount = hostonoffCount;
        oldmigrationcount = migrationCount;

        vmstimelog.println(CloudSim.clock() + " " + getVmList().size());
        powertimelog.println(CloudSim.clock() + " " + power);
        //log
        HyperVmAllocationPolicy hp = (HyperVmAllocationPolicy) getVmAllocationPolicy();
        for (Host h :  getHostList())//this. <HyperPowerHost> getHostList())
        {
            HyperPowerHost host = (HyperPowerHost) h;

            if(host.getSynchronizer() != null) {
                host.getSynchronizer().setSynching(false);
                try {
                    if(host.getSynchronizer().isAlive()) {
                        //System.out.println("STATE THREAD OF HOST " + host.getSynchronizer().getState() + " SYNCING " + host.getSynchronizer().isSynching());
                        /*if(host.getSynchronizer().getState() == Thread.State.WAITING) {
                            synchronized (host.getSynchronizer()) {
                                System.out.println("NOTIF THREAD OF HOST" + (host.getId() - 2));
                                host.getSynchronizer().notify();
                            }
                        }*/
                       // System.out.println("JOINING THREAD OF HOST" + (host.getId() - 2));
                        if(host.getSynchronizer().getState() == Thread.State.RUNNABLE) {
                            //System.out.println("STATE THREAD OF HOST " + host.getSynchronizer().getState());
                        }
                 //       System.out.println(CloudSim.clock());
                        host.getSynchronizer().interrupt();
                        host.getSynchronizer().join();
                        host.setSynchronizer(null);
                    }
                    else
                    {
                        host.getSynchronizer().join();
                        host.setSynchronizer(null);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            host.sendheartbeats();

            host.vmsaftercycle = host.getVmList().size();

        }


        if (getCloudletSubmitted() == -1 || getCloudletSubmitted() == CloudSim.clock()) {
			CloudSim.cancelAll(getId(), new PredicateType(HyperCloudSimTags.VM_DATACENTER_EVENT));
			schedule(getId(), getSchedulingInterval(), HyperCloudSimTags.VM_DATACENTER_EVENT);
			return;
		}
		double currentTime = CloudSim.clock();
		// if some time passed since last processing
		if (currentTime > getLastProcessTime()) {
			double minTime = updateCloudetProcessingWithoutSchedulingFutureEventsForce();


            //removing vms
            int nohost = 0;
            int rvms = 0;
            if(getVmList().size() > 100){ // && newvms > 0) {
                ArrayList<Vm> removeVms = new ArrayList<>();
                if(mode == 1) {
                    rvms = 240 - newvms;
                    if(rvms > getVmList().size()) rvms = newvms;
                }
                else if(mode == 2)
                {
                    rvms = rate * 59;
                    if(rvms > getVmList().size()) rvms = 0;
                }
                int max = getVmList().size() - 1;
                for (int idx = 0; idx < rvms; idx++) {
                    Random random = new Random();
                    int vmr = generateRandomInteger(0, max, random);

                    HyperPowerVm vm = (HyperPowerVm) getVmList().get(vmr);
                    HyperPowerHost oldhost  = (HyperPowerHost) vm.getHost();
                    if(oldhost == null)
                    {
                        nohost +=1;
                        System.out.println(CloudSim.clock() + " DELETED: " + vm.getId());

                       // continue;
                    }
                    else
                    {
                        System.out.println(CloudSim.clock() + " DELETED: " + vm.getId() + " host " + (oldhost.getId() - 2));

                    }
                  //  hp.getVmTable().remove(vm.getUid());
                    //if(oldhost != null)
                    //{
                        //if(oldhost.getVmList().size() == 1)  oldhost.switchOff();
                        //oldhost.vmDestroy(vm);

                    removeVms.add(vm);
                    getVmAllocationPolicy().deallocateHostForVm(vm);

                       // getVmList().remove(vm);
                       // sendNow(oldhost.getId(), HyperCloudSimTags.REMOVEVM, vm);
                        max-=1;

                   // }
                    /*oldhost.removeMigratingInVm(vm);
                      getVmAllocationPolicy().deallocateHostForVm(vm);
                    getVmList().remove(vm);
                    if(oldhost.getVmList().size() == 0)  oldhost.switchOff();*/

                }

                getVmList().removeAll(removeVms);
            }





            if (!isDisableMigrations()) {
                Log.write("Period: " + round++);
                Log.write("Time: " + CloudSim.clock() + " -- Datacenter VMs " + getVmList().size() + "Power - End of Period : " + this.getPower() + " --\n");
                power = 0;

				List<Map<String, Object>> migrationMap = getVmAllocationPolicy().optimizeAllocation(
						getVmList());
                Log.write("");
                Log.write("Time: " + CloudSim.clock() + "-- Datacenter Hosts " + getHostList().size() + " and Power - Start of next Period : "
                        + this.getHostPower() + "--");
                //Log.write("");
/*
				if (migrationMap != null) {
					for (Map<String, Object> migrate : migrationMap) {
						power += 20;
                        Vm vm = (Vm) migrate.get("vm");
						HyperPowerHost targetHost = (HyperPowerHost) migrate.get("host");
						HyperPowerHost oldHost = (HyperPowerHost) vm.getHost();

                        System.out.println("oldhost Host" + (oldHost.getId() - 2) + " newhost Host" + (targetHost.getId() - 2));

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

    /*                    if(targetHost.tobeon)
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
                    this.power += 100;
                    send(
                            getId(),
                            10,
                            HyperCloudSimTags.HOST_OFF,
                            ph);
                }
  */              /*for(Host ph : hp.tobeonHosts)
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
            } else {
                send(getId(), getSchedulingInterval(), HyperCloudSimTags.VM_DATACENTER_EVENT);
            }

			setLastProcessTime(currentTime);
		}
        hp.tobeoffHosts.clear();
        hp.tobeonHosts.clear();

	}

    private double getHostPower() {
        power = 0;
        /*for(Host host : getHostList())
        {
            HyperPowerHost h = (HyperPowerHost) host;
            power += h.getPower();
        }*/
        return power;
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

		//Log.printLine("\n\n--------------------------------------------------------------\n\n");
		//Log.formatLine("New resource usage for the time frame starting at %.2f:", currentTime);

		//for (HyperPowerHost host : this.<HyperPowerHost> getHostList()) {
        HyperVmAllocationPolicy hp = (HyperVmAllocationPolicy) getVmAllocationPolicy();
        for (Host h : hp.getOnHosts()) {
            HyperPowerHost host = (HyperPowerHost) h;
			//Log.printLine();

			double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
			if (time < minTime) {
				minTime = time;
			}

			/*Log.formatLine(
                    "%.2f: [Host #%d] utilization is %.2f%%",
                    currentTime,
                    host.getId(),
                    host.getUtilizationOfCpu() * 100);*/
		}

		if (timeDiff > 0) {
			/*Log.formatLine(
                    "\nEnergy consumption for the last time frame from %.2f to %.2f:",
                    getLastProcessTime(),
                    currentTime);*/

			/*for (HyperPowerHost host : this.<HyperPowerHost> getHostList()) {
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
*/
		}
		//setPower(getPower() + timeFrameDatacenterEnergy);
		checkCloudletCompletion();

		/** Remove completed VMs **/
		/*for (HyperPowerHost host : this.<HyperPowerHost> getHostList()) {
			for (Vm vm : host.getCompletedVms()) {
				getVmAllocationPolicy().deallocateHostForVm(vm);
				getVmList().remove(vm);
				Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
			}
		}*/

		//Log.printLine();

		setLastProcessTime(currentTime);
		return minTime;
	}

	/*
	 * (non-Javadoc)
	 * @see org.cloudbus.cloudsim.Datacenter#processVmMigrate(org.cloudbus.cloudsim.core.SimEvent,
	 * boolean)
	 */


    protected void processVmMigrate(SimEvent ev, boolean ack) {
        Object tmp = ev.getData();
        if (!(tmp instanceof Map<?, ?>)) {
            throw new ClassCastException("The data object must be Map<String, Object>");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> migrate = (HashMap<String, Object>) tmp;

        Vm vm = (Vm) migrate.get("vm");
        System.out.println("CC " + CloudSim.clock() + " MIGRATING VM" + vm.getId());
        Host host = (Host) migrate.get("host");
        Host oldhost = vm.getHost();
        HyperVmAllocationPolicy h = (HyperVmAllocationPolicy) getVmAllocationPolicy();
        boolean evaluate = h.evaluatemigration(vm, host);
        if(!evaluate)
        {
            Log.printLine("[Datacenter.processVmMigrate] VM migration to the destination host failed");
            vm.setInMigration(false);
            return;
        }
        if(oldhost == null)
        {
            //vm.setInMigration(false);
            //return;
        }
        /*getVmAllocationPolicy().deallocateHostForVm(vm);
        if(oldhost == null) {
            System.out.println("CC " + CloudSim.clock() + " VM:" + vm.getId());
        }
        oldhost.removeMigratingInVm(vm);*/

        boolean result = h.placeVminHost(vm,host);
                //getVmAllocationPolicy().allocateHostForVm(vm, host);
        if (!result) {
            Log.printLine("[Datacenter.processVmMigrate] VM allocation to the destination host failed");
            //System.exit(0);
        }
        else
        {
            incrementMigrationCount();
            /*if(oldhost.getVmList().size() == 0)
            {
                /*send(
                        getId(),
                        0,
                        HyperCloudSimTags.HOST_OFF,
                        oldhost);*/
             /*   HyperPowerHost oldh = (HyperPowerHost) oldhost;
                h.offHosts.add(oldh);
                oldh.switchOff();
            }*/
            vm.setInMigration(false);
        }

        HyperPowerHost hp = (HyperPowerHost) host;
        //hp.waitmigrate--;

        if (ack) {
            int[] data = new int[3];
            data[0] = getId();
            data[1] = vm.getId();

            if (result) {
                data[2] = CloudSimTags.TRUE;
            } else {
                data[2] = CloudSimTags.FALSE;
            }
            sendNow(ev.getSource(), CloudSimTags.VM_CREATE_ACK, data);
        }

        /*Log.formatLine(
                "%.2f: Migration of VM #%d to Host #%d is completed",
                CloudSim.clock(),
                vm.getId(),
                host.getId() - 2);
        */vm.setInMigration(false);
       // System.out.println("CC " + CloudSim.clock() + " MIG OK VM:" + vm.getId());

    }


	/*@Override
	protected void processVmMigrate(SimEvent ev, boolean ack) {
		updateCloudetProcessingWithoutSchedulingFutureEvents();
		super.processVmMigrate(ev, ack);
		SimEvent event = CloudSim.findFirstDeferred(getId(), new PredicateType(HyperCloudSimTags.VM_MIGRATE));
		if (event == null || event.eventTime() > CloudSim.clock()) {
			updateCloudetProcessingWithoutSchedulingFutureEventsForce();
		}
	}*/

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

    /**
     * Increment migration count.
     */
    protected void incrementhostonoffCount() {
        setHostonoffCount(getHostonoffCount() + 1);
    }

    public void incrementhostoffCount() {
        incrementhostonoffCount();
        setHostoffcount(getHostoffcount() + 1);
    }

    public void incrementhostonCount() {
        incrementhostonoffCount();
        setHostoncount(getHostoncount() + 1);
    }


    public Host getHostbyId(int id)
    {
        for (Host hp : this.getHostList())
        {
            if (hp.getId() == id)
            {
                return hp;
            }
        }
        return null;
    }

    public void removeVm(SimEvent ev)
    {
        processVmDestroy(ev, true);
    }

    protected void checkCloudletCompletion() {
        HyperVmAllocationPolicy hp = (HyperVmAllocationPolicy) getVmAllocationPolicy();
        for (Host host : hp.getOnHosts()) {
            for (Vm vm : host.getVmList()) {
                while (vm.getCloudletScheduler().isFinishedCloudlets()) {
                    Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
                    if (cl != null) {
                        sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
                    }
                }
            }
        }
    }

}
