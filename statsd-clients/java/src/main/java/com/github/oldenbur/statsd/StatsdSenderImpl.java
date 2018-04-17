package com.github.oldenbur.statsd;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

public class StatsdSenderImpl implements StatsdSender {

    static final Logger logger = LoggerFactory.getLogger(StatsdSenderImpl.class);

    private static StatsdSenderImpl instance;

    private StatsDClient statsd;

    private String host;
    private String clusterId;
    private String serviceName;

    private StatsdSenderImpl(String statsdHost, int statsdPort, String host, String clusterId, String serviceName) {
        statsd = new NonBlockingStatsDClient(null, statsdHost, statsdPort);
        this.host = host;
        this.clusterId = clusterId;
        this.serviceName = serviceName;
    }

    /**
     * Factory method for creating/accessing the StatsdSender instance.
     * @param statsdHost host/IP of the statsd endpoint
     * @param statsdPort port of the statsd endpoint
     * @param clusterId clusterId tag value
     * @param serviceName serviceName tag value
     */
    public static StatsdSender getStatsdSender(String statsdHost, int statsdPort, String clusterId, String serviceName) {

        String host;
        try {
            host = java.net.InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.warn("caught Exception getting hostname", e);
            host = "default";
        }

        if (instance == null) {
            instance = new StatsdSenderImpl(statsdHost, statsdPort, host, clusterId, serviceName);
        }

        return instance;
    }

    private String addTags(String field) {
        String taggedField = String.format("%s,host=%s,cluster=%s,service=%s", field, host, clusterId, serviceName);
        return taggedField;
    }

    @Override
    public void count(String field, int value) {
        statsd.count(addTags(field), value);
    }

    @Override
    public void gauge(String field, int valueMs) {
        statsd.recordGaugeValue(addTags(field), valueMs);
    }

    @Override
    public void timing(String field, int value) {
        statsd.recordExecutionTime(addTags(field), value);
    }
}
