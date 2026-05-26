public interface Sender {
    void start();
    void join() throws InterruptedException;
}
