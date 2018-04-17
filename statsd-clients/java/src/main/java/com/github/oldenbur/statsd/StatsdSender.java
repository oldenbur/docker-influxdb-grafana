package com.github.oldenbur.statsd;

public interface StatsdSender {
    void count(String field, int value);

    void timing(String field, int valueMs);

    void gauge(String field, int value);
}
