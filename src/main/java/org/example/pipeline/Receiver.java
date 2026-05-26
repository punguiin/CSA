package org.example.pipeline;

public interface Receiver {
    void start();
    void stop();
    void join() throws InterruptedException;
}
