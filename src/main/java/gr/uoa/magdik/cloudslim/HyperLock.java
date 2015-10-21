package gr.uoa.magdik.cloudslim;

/**
 * Created by tchalas on 10/16/15.
 */
public class HyperLock{

    private boolean isLocked = false;

    public synchronized void lock()
            throws InterruptedException{
        while(isLocked){
            wait();
        }
        isLocked = true;
    }

    public synchronized void unlock(){
        isLocked = false;
        notify();
    }
}
