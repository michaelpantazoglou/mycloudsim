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
import org.jfree.data.xy.XYSeries;

import java.io.PrintWriter;
import java.util.*;

import static gr.uoa.magdik.cloudsim.HyperHelper.generateRandomInteger;


public class HyperPowerDatacenter extends Datacenter {

	/** The power. */
	private double power;
    private double powerhours = 0;
    int vc = 0;

    public double getSampletime() {
        return sampletime;
    }

    public void setSampletime(double sampletime) {
        this.sampletime = sampletime;
    }

    public double sampletime = 1800.00;

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

    public int getHours() {
        return hours;
    }

    int hours = 0;
    XYSeries onhoststime;
    XYSeries vmstime;
    XYSeries energytime;
    XYSeries switchoffs;
    XYSeries migrations;
    XYSeries switchons;

    public XYSeries getSwitchoffs() {
        return switchoffs;
    }

    public XYSeries getSwitchons() {
        return switchons;
    }

    public XYSeries getMigrations() {
        return migrations;
    }

    public XYSeries getVmstime() {
        return vmstime;
    }

    public void setVmstime(XYSeries vmstime) {
        this.vmstime = vmstime;
    }

    public XYSeries getEnergytime() {
        return energytime;
    }

    public void setEnergytime(XYSeries energytime) {
        this.energytime = energytime;
    }

    public XYSeries getOnhoststime() {
        return onhoststime;
    }

    public void setOnhoststime(XYSeries onhoststime) {
        this.onhoststime = onhoststime;
    }


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
        onhoststime = new XYSeries("Active Hosts");
        vmstime = new XYSeries("VMs");
        energytime = new XYSeries("Power Consuption");
        switchoffs = new XYSeries("Switch offs");
        switchons = new XYSeries("Switch Ons");
        migrations = new XYSeries("VM migrations");
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


    public void setHours(int hours) {
        this.hours = hours;
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

        int newvms = 0;
        if(CloudSim.clock() > 65) newvms = getVmList().size() - oldvmcount;
        vc += newvms;


        HyperVmAllocationPolicy hp = (HyperVmAllocationPolicy) getVmAllocationPolicy();

        for (Host h : getHostList())//this. <HyperPowerHost> getHostList())
        {
            HyperPowerHost host = (HyperPowerHost) h;

            if(host.getSynchronizer() != null) {
                host.getSynchronizer().setSynching(false);
                try {
                    if(host.getSynchronizer().isAlive()) {;
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

            if(getVmList().size() > 0  && mode!=3){ // && newvms > 0) {
                Random random = new Random(Double.doubleToLongBits(Math.random()));
                ArrayList<Vm> removeVms = new ArrayList<>();
                if(mode == 1) {
                    rvms = rate * 60 - newvms;
                    if(rvms > getVmList().size()) rvms = getVmList().size();
                    if(newvms <= 0) rvms = 0;
                }
                else if(mode == 2)
                {
                    rvms = rate * 59;
                    if(rvms > getVmList().size()) rvms = getVmList().size();
                }
                else if(mode == 4)
                {
                    rvms = generateRandomInteger(0, 550, random);
                    if(rvms > getVmList().size()) rvms = getVmList().size();
                }
                int max = getVmList().size();
                for (int idx = 0; idx < rvms; idx++) {

                    int vmr = generateRandomInteger(0, max, random);

                    HyperPowerVm vm = (HyperPowerVm) getVmList().get(vmr);
                    int r = 0;
                    while(removeVms.contains(vm))
                    {
                        //random = new Random(max * 6675675);
                        System.out.println("ff");
                        if(r == 5)
                        {
                            for(Vm rvm : getVmList())
                            {
                                if(!removeVms.contains(rvm))
                                {
                                    vm = (HyperPowerVm) rvm;
                                    break;
                                }
                            }
                            break;
                        }
                        vmr = generateRandomInteger(0, max, random);
                        vm = (HyperPowerVm) getVmList().get(vmr);
                        r++;
                    }
                    HyperPowerHost oldhost  = (HyperPowerHost) vm.getHost();
                    if(oldhost == null)
                    {
                        nohost +=1;
                    }
                    removeVms.add(vm);
                    getVmAllocationPolicy().deallocateHostForVm(vm);
                    max-=1;
                }
                if(rvms > 0 && removeVms.size() != rvms) throw new IllegalArgumentException("UU " + rvms + "-" + removeVms.size());
                getVmList().removeAll(removeVms);
                vc -= removeVms.size();
            }

            oldvmcount = getVmList().size();
            System.out.println(vc);

            long power = 0;
            power += (migrationCount - oldmigrationcount) * 20;
            power += (hostonoffCount - oldhostonoffcount) * 100;
            power += getVmList().size() * 5;
            power += hp.getOnHosts().size() * 160;
            double powerperiod = power * 0.0166667/1000;

            powerhours += powerperiod;

            if(CloudSim.clock() < 70 || CloudSim.clock() - sampletime > hours * sampletime)
            {
                double kwpower = 0;
                int temphours = hours;
                if(CloudSim.clock() > 70) {
                    temphours++;
                    kwpower = powerhours;// * 0.5 / 1000;
                    powerhours = 0;
                }
                powertimelog.println(CloudSim.clock() + " " + kwpower);
                energytime.add(temphours, kwpower);
                vmstime.add(temphours, getVmList().size());
                getSwitchons().add(temphours, hostoncount);
                getSwitchoffs().add(temphours, hostoffcount);
                onhoststime.add(temphours, hp.getOnHosts().size());
                migrations.add(temphours, migrationCount - oldmigrationcount);
                oldhostonoffcount = hostonoffCount;
                oldmigrationcount = migrationCount;
                setHostoffcount(0);
                setHostoncount(0);
            }

            vmstimelog.println(CloudSim.clock() + " " + getVmList().size());

            if (!isDisableMigrations()) {
                Log.write("Period: " + round++);
                Log.write("Time: " + CloudSim.clock() + " -- Datacenter VMs " + getVmList().size() + "Power - End of Period : " + this.getPower() + " --\n");
                power = 0;

				List<Map<String, Object>> migrationMap = getVmAllocationPolicy().optimizeAllocation(
						getVmList());
                Log.write("Time: " + CloudSim.clock() + "-- Datacenter Hosts " + getHostList().size() + " and Power - Start of next Period : "
                        + this.getHostPower() + "--");
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

    private void removeVms(int newvms)
    {
        //removing vms
        int nohost = 0;
        int rvms = 0;

        if(getVmList().size() > 0  && mode!=3){ // && newvms > 0) {
            Random random = new Random(Double.doubleToLongBits(Math.random()));
            ArrayList<Vm> removeVms = new ArrayList<>();
            if(mode == 1) {
                rvms = rate * 60 - newvms;
                if(rvms > getVmList().size()) rvms = getVmList().size();
                if(newvms <= 0) rvms = 0;
            }
            else if(mode == 2)
            {
                rvms = rate * 59;
                if(rvms > getVmList().size()) rvms = getVmList().size();
            }
            else if(mode == 4)
            {
                rvms = generateRandomInteger(0, 550, random);
                if(rvms > getVmList().size()) rvms = getVmList().size();
            }
            int max = getVmList().size();
            for (int idx = 0; idx < rvms; idx++) {

                int vmr = generateRandomInteger(0, max, random);

                HyperPowerVm vm = (HyperPowerVm) getVmList().get(vmr);
                int r = 0;
                while(removeVms.contains(vm))
                {
                    //random = new Random(max * 6675675);
                    System.out.println("ff");
                    if(r == 5)
                    {
                        for(Vm rvm : getVmList())
                        {
                            if(!removeVms.contains(rvm))
                            {
                                vm = (HyperPowerVm) rvm;
                                break;
                            }
                        }
                        break;
                    }
                    vmr = generateRandomInteger(0, max, random);
                    vm = (HyperPowerVm) getVmList().get(vmr);
                    r++;
                }
                HyperPowerHost oldhost  = (HyperPowerHost) vm.getHost();
                if(oldhost == null)
                {
                    nohost +=1;
                }
                removeVms.add(vm);
                getVmAllocationPolicy().deallocateHostForVm(vm);
                max-=1;
            }
            if(rvms > 0 && removeVms.size() != rvms) throw new IllegalArgumentException("UU " + rvms + "-" + removeVms.size());
            getVmList().removeAll(removeVms);
            vc -= removeVms.size();
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

    protected void processVmMigrate(SimEvent ev, boolean ack) {
        Object tmp = ev.getData();
        if (!(tmp instanceof Map<?, ?>)) {
            throw new ClassCastException("The data object must be Map<String, Object>");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> migrate = (HashMap<String, Object>) tmp;

        Vm vm = (Vm) migrate.get("vm");
        HyperPowerHost host = (HyperPowerHost) migrate.get("host");
        Host oldhost = vm.getHost();
        HyperVmAllocationPolicy h = (HyperVmAllocationPolicy) getVmAllocationPolicy();
        if(host.getPowerState() == HyperPowerHost.PowerState.OFF)
        {
            Log.printLine("[Datacenter.processVmMigrate] VM migration to the destination host failed");
            vm.setInMigration(false);
            return;
        }

        boolean result = h.placeVminHost(vm,host);
        if (!result) {
            Log.printLine("[Datacenter.processVmMigrate] VM allocation to the destination host failed");
        }
        else
        {
            incrementMigrationCount();
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

}
