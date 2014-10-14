package gr.uoa.di.madgik.cloudsim;

/**
 * Created by tchalas on 10/10/2014.
 */

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.lists.HostList;
import org.cloudbus.cloudsim.lists.PeList;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;
import org.cloudbus.cloudsim.network.datacenter.*;

import java.util.*;

public class PowerNetworkHost extends NetworkHost {


    /** The utilization mips. */
    private double utilizationMips;

    /** The previous utilization mips. */
    private double previousUtilizationMips;

    /** The state history. */
    private final List<HostStateHistoryEntry> stateHistory = new LinkedList<HostStateHistoryEntry>();

    private PowerModel powerModel;

    public PowerNetworkHost(
            int id,
            RamProvisioner ramProvisioner,
            BwProvisioner bwProvisioner,
            long storage,
            List<? extends Pe> peList,
            VmScheduler vmScheduler,
            PowerModel powerModel)
    {
        super( id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler);
        setPowerModel(powerModel);
    }


    /*
     * (non-Javadoc)
     * @see cloudsim.Host#updateVmsProcessing(double)
     */
    @Override
    public double updateVmsProcessing(double currentTime) {

        /*
        double smallerTime = Double.MAX_VALUE;
		// insert in each vm packet recieved
		recvpackets();
		for (Vm vm : super.getVmList()) {
			double time = ((NetworkVm) vm).updateVmProcessing(currentTime, getVmScheduler()
					.getAllocatedMipsForVm(vm));
			if (time > 0.0 && time < smallerTime) {
				smallerTime = time;
			}
		}
		// send the packets to other hosts/VMs
		sendpackets();

		return smallerTime;
         */


        double smallerTime = super.updateVmsProcessing(currentTime);
        setPreviousUtilizationMips(getUtilizationMips());
        setUtilizationMips(0);
        double hostTotalRequestedMips = 0;

        for (Vm vm : getVmList()) {
            getVmScheduler().deallocatePesForVm(vm);
        }

        for (Vm vm : getVmList()) {
            getVmScheduler().allocatePesForVm(vm, vm.getCurrentRequestedMips());
        }

        for (Vm vm : getVmList()) {
            double totalRequestedMips = vm.getCurrentRequestedTotalMips();
            double totalAllocatedMips = getVmScheduler().getTotalAllocatedMipsForVm(vm);

            if (!Log.isDisabled()) {
                Log.formatLine(
                        "%.2f: [Host #" + getId() + "] Total allocated MIPS for VM #" + vm.getId()
                                + " (Host #" + vm.getHost().getId()
                                + ") is %.2f, was requested %.2f out of total %.2f (%.2f%%)",
                        CloudSim.clock(),
                        totalAllocatedMips,
                        totalRequestedMips,
                        vm.getMips(),
                        totalRequestedMips / vm.getMips() * 100);

                List<Pe> pes = getVmScheduler().getPesAllocatedForVM(vm);
                StringBuilder pesString = new StringBuilder();
                for (Pe pe : pes) {
                    pesString.append(String.format(" PE #" + pe.getId() + ": %.2f.", pe.getPeProvisioner()
                            .getTotalAllocatedMipsForVm(vm)));
                }
                Log.formatLine(
                        "%.2f: [Host #" + getId() + "] MIPS for VM #" + vm.getId() + " by PEs ("
                                + getNumberOfPes() + " * " + getVmScheduler().getPeCapacity() + ")."
                                + pesString,
                        CloudSim.clock());
            }

            if (getVmsMigratingIn().contains(vm)) {
                Log.formatLine("%.2f: [Host #" + getId() + "] VM #" + vm.getId()
                        + " is being migrated to Host #" + getId(), CloudSim.clock());
            } else {
                if (totalAllocatedMips + 0.1 < totalRequestedMips) {
                    Log.formatLine("%.2f: [Host #" + getId() + "] Under allocated MIPS for VM #" + vm.getId()
                            + ": %.2f", CloudSim.clock(), totalRequestedMips - totalAllocatedMips);
                }

                vm.addStateHistoryEntry(
                        currentTime,
                        totalAllocatedMips,
                        totalRequestedMips,
                        (vm.isInMigration() && !getVmsMigratingIn().contains(vm)));

                if (vm.isInMigration()) {
                    Log.formatLine(
                            "%.2f: [Host #" + getId() + "] VM #" + vm.getId() + " is in migration",
                            CloudSim.clock());
                    totalAllocatedMips /= 0.9; // performance degradation due to migration - 10%
                }
            }

            setUtilizationMips(getUtilizationMips() + totalAllocatedMips);
            hostTotalRequestedMips += totalRequestedMips;
        }

        addStateHistoryEntry(
                currentTime,
                getUtilizationMips(),
                hostTotalRequestedMips,
                (getUtilizationMips() > 0));

        return smallerTime;
    }

    /**
     * Gets the completed vms.
     *
     * @return the completed vms
     */
    public List<Vm> getCompletedVms() {
        List<Vm> vmsToRemove = new ArrayList<Vm>();
        for (Vm vm : getVmList()) {
            if (vm.isInMigration()) {
                continue;
            }
            if (vm.getCurrentRequestedTotalMips() == 0) {
                vmsToRemove.add(vm);
            }
        }
        return vmsToRemove;
    }

    /**
     * Gets the max utilization among by all PEs.
     *
     * @return the utilization
     */
    public double getMaxUtilization() {
        return PeList.getMaxUtilization(getPeList());
    }

    /**
     * Gets the max utilization among by all PEs allocated to the VM.
     *
     * @param vm the vm
     * @return the utilization
     */
    public double getMaxUtilizationAmongVmsPes(Vm vm) {
        return PeList.getMaxUtilizationAmongVmsPes(getPeList(), vm);
    }

    /**
     * Gets the utilization of memory.
     *
     * @return the utilization of memory
     */
    public double getUtilizationOfRam() {
        return getRamProvisioner().getUsedRam();
    }

    /**
     * Gets the utilization of bw.
     *
     * @return the utilization of bw
     */
    public double getUtilizationOfBw() {
        return getBwProvisioner().getUsedBw();
    }

    /**
     * Get current utilization of CPU in percentage.
     *
     * @return current utilization of CPU in percents
     */
    public double getUtilizationOfCpu() {
        double utilization = getUtilizationMips() / getTotalMips();
        if (utilization > 1 && utilization < 1.01) {
            utilization = 1;
        }
        return utilization;
    }

    /**
     * Gets the previous utilization of CPU in percentage.
     *
     * @return the previous utilization of cpu
     */
    public double getPreviousUtilizationOfCpu() {
        double utilization = getPreviousUtilizationMips() / getTotalMips();
        if (utilization > 1 && utilization < 1.01) {
            utilization = 1;
        }
        return utilization;
    }

    /**
     * Get current utilization of CPU in MIPS.
     *
     * @return current utilization of CPU in MIPS
     */
    public double getUtilizationOfCpuMips() {
        return getUtilizationMips();
    }

    /**
     * Gets the utilization mips.
     *
     * @return the utilization mips
     */
    public double getUtilizationMips() {
        return utilizationMips;
    }

    /**
     * Sets the utilization mips.
     *
     * @param utilizationMips the new utilization mips
     */
    protected void setUtilizationMips(double utilizationMips) {
        this.utilizationMips = utilizationMips;
    }

    /**
     * Gets the previous utilization mips.
     *
     * @return the previous utilization mips
     */
    public double getPreviousUtilizationMips() {
        return previousUtilizationMips;
    }

    /**
     * Sets the previous utilization mips.
     *
     * @param previousUtilizationMips the new previous utilization mips
     */
    protected void setPreviousUtilizationMips(double previousUtilizationMips) {
        this.previousUtilizationMips = previousUtilizationMips;
    }

    /**
     * Gets the state history.
     *
     * @return the state history
     */
    public List<HostStateHistoryEntry> getStateHistory() {
        return stateHistory;
    }

    /**
     * Adds the state history entry.
     *
     * @param time the time
     * @param allocatedMips the allocated mips
     * @param requestedMips the requested mips
     * @param isActive the is active
     */
    public
    void
    addStateHistoryEntry(double time, double allocatedMips, double requestedMips, boolean isActive) {

        HostStateHistoryEntry newState = new HostStateHistoryEntry(
                time,
                allocatedMips,
                requestedMips,
                isActive);
        if (!getStateHistory().isEmpty()) {
            HostStateHistoryEntry previousState = getStateHistory().get(getStateHistory().size() - 1);
            if (previousState.getTime() == time) {
                getStateHistory().set(getStateHistory().size() - 1, newState);
                return;
            }
        }
        getStateHistory().add(newState);
    }


    /**
     * Gets the power. For this moment only consumed by all PEs.
     *
     * @return the power
     */
    public double getPower() {
        return getPower(getUtilizationOfCpu());
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
            power = getPowerModel().getPower(utilization);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return power;
    }

    /**
     * Gets the max power that can be consumed by the host.
     *
     * @return the max power
     */
    public double getMaxPower() {
        double power = 0;
        try {
            power = getPowerModel().getPower(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return power;
    }

    /**
     * Gets the energy consumption using linear interpolation of the utilization change.
     *
     * @param fromUtilization the from utilization
     * @param toUtilization the to utilization
     * @param time the time
     * @return the energy
     */
    public double getEnergyLinearInterpolation(double fromUtilization, double toUtilization, double time) {
        if (fromUtilization == 0) {
            return 0;
        }
        double fromPower = getPower(fromUtilization);
        double toPower = getPower(toUtilization);
        return (fromPower + (toPower - fromPower) / 2) * time;
    }

    /**
     * Sets the power model.
     *
     * @param powerModel the new power model
     */
    protected void setPowerModel(PowerModel powerModel) {
        this.powerModel = powerModel;
    }

    /**
     * Gets the power model.
     *
     * @return the power model
     */
    public PowerModel getPowerModel() {
        return powerModel;
    }

}


