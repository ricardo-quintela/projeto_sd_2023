class Callme3 {
    void call(String msg) {
        System.out.print("[" + msg);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
        System.out.println("]");
    }
}

class Caller3 implements Runnable {
    String msg;
    Callme3 target;
    Thread t;

    public Caller3(Callme3 targ, String s) {
        target = targ;
        msg = s;
        t = new Thread(this);
        t.start();
    }

    public void run() {
        synchronized (target) { // synchronized block
            target.call(msg);
        }
    }
}

class Synch3 {
    public static void main(String args[]) {
        Callme3 target1 = new Callme3();
        Callme3 target2 = new Callme3();
        Callme3 target3 = new Callme3();
        Caller3 ob1 = new Caller3(target1, "Hello");
        Caller3 ob2 = new Caller3(target2, "Synchronized");
        Caller3 ob3 = new Caller3(target3, "World");
        try {
            ob1.t.join();
            ob2.t.join();
            ob3.t.join();
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
    }
}