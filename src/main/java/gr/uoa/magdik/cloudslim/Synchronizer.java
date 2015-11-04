package gr.uoa.magdik.cloudslim;

import org.cloudbus.cloudsim.core.CloudSim;

/**
 * Created by tchalas on 10/13/15.
 */
public class Synchronizer extends Thread {

    public class Mutex {
        public void acquire() throws InterruptedException { }
        public void release() { }
    }

    int mode = 0;

    public HyperPowerHost getHost() {
        return host;
    }

    public void setHost(HyperPowerHost host) {
        this.host = host;
    }

    HyperPowerHost host;

    public void setSynching(boolean synching) {
        this.synching = synching;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public boolean isSynching() {
        return synching;
    }

    public int getMode() {
        return mode;
    }

    boolean synching = false;
    HyperLock lock = new HyperLock();

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean started = false;


    @Override
    public void run() {
        int hs = host.getVmList().size();
        System.out.println("Time:" + CloudSim.clock() + " Starting threaf of host" + (host.getId() - 2));
        started = true;
        while(synching) {
            //if(host.isVmstatechange()) {
            try {
                if (mode == 0) {
                    //HyperVmAllocationPolicy.partialVmMigration(host);
                    host.partialVmMigration();
                } else {
                    host.fullVmMigration();
                }
                //hs = host.getVmList().size();
                //}
                synchronized (this) {
                        wait();
                }
            } catch (InterruptedException e) {
                synching = false;
                e.printStackTrace();
            }
        }
    }
}
