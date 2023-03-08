package searchEngine;

/**
 * Um {@code ThreadSlave} é uma thread que deve ser criada
 * por um {@code ThreadHandeler} para realizar tarefas em paralelo
 * 
 * O método {@code run} deve ser re-implementado para definir o comportamento
 * As threads devem ser escalonadas e sincronizadas devidamente
 */
public class ThreadSlave implements Runnable {
    
    protected int id;
    protected String parentName;
    protected boolean state;
    protected Thread thread;

    /**
     * Construtor da classe {@code ThreadSlave}
     * 
     * @param id o id da thread
     * @param parentName o nome do objeto que criou esta thread
     */
    public ThreadSlave(int id, String parentName) {
        this.id = id;
        this.parentName = parentName;
        this.state = false;

        // criar e inicializar a thread
        this.thread = new Thread(this);
        System.out.println(this + " created!");
        this.thread.start();
    }

    /**
     * Construtor por omissão da classe {@code ThreadSlave}
     */
    public ThreadSlave() {}

    @Override
    public void run() {}

        /**
     * Imprime no stdout o estado da thread
     */
    public synchronized void printState() {
        if (getState()) {
            System.out.println(this + " is active!");
            return;
        }
        System.out.println(this + " is stopped!");
    }

    public int getId() {
        return id;
    }

    public Thread getThread() {
        return thread;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public boolean getState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "#" + this.id + "@" + this.parentName;
    }
}
