package gr.uoa.magdik.cloudsim;

/**
 * Created by tchalas on 10/13/15.
 */
public class Synchronizer extends Thread {

    public class Mutex {
        public void acquire() throws InterruptedException { }
        public void release() { }
    }

    //mode defining partial or full migration
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

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean started = false;


    @Override
    public void run() {
        started = true;
        while(synching) {
            try {
                if (mode == 0) {
                    if(host.partialVmMigration() == 2)  synching = false;
                } else {
                    if(host.fullVmMigration() == 2)  synching = false;
                }
                synchronized (this) {
                    //wait for a signal that the new vm was created in the host
                    wait();
                }
            } catch (InterruptedException e) {
                synching = false;
            }
        }
    }
}
