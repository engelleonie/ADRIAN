package tech.jorn.adrian.experiment;


public class AgentController {
    public static class MyRunnable implements Runnable {
        @Override
        public void run() {
            int a = 0;
            for(int i = 10; i > 0; i--) {
                a += i;
            }
            System.out.println("a = " + a);
            String threadName = Thread.currentThread().getName();

            System.out.println(threadName + " finished");
        }


    }
    public static void main(String[] args) {
        Thread thread1 = new Thread (new MyRunnable());
        thread1.start();
        Thread thread2 = new Thread (new MyRunnable());
        thread2.start();
    }
}
