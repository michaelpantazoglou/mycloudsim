package gr.uoa.magdik.cloudslim;

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


    @Override
    public void run() {
        int hs = host.getVmList().size();
        System.out.println("Starting threaf of host" + (host.getId() - 2));
        while(synching) {

                if (mode == 0) {
                    //HyperVmAllocationPolicy.partialVmMigration(host);

                    host.partialVmMigration();
                } else {
                    host.fullVmMigration();
                }
                //hs = host.getVmList().size();
            }

    }
}
