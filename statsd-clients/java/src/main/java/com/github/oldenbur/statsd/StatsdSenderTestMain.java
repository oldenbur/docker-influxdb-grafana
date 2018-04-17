package com.github.oldenbur.statsd;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

public class StatsdSenderTestMain {

    static final Logger logger = LoggerFactory.getLogger(StatsdSenderTestMain.class);

    public static final String OPTION_HOST = "host";
    public static final String OPTION_PORT = "port";
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 8125;

    public static final String CLUSTER_ID = "tig-cluster";
    public static final String SERVICE_NAME = "java-statsd";

    public static final String COUNT_FIELD = "jcount";
    public static final String GAUGE_FIELD = "jgauge";
    public static final String TIMING_FIELD = "jtiming";

    class CmdOptions {
        String host;
        int port;
    }

    public static final void main(String[] args) {

        logger.info("StatsdSenderTestMain starting");

        StatsdSenderTestMain sender = new StatsdSenderTestMain();
        CmdOptions statsdOptions;
        try {
            statsdOptions = sender.parseCli(args);
        } catch (ParseException|NumberFormatException e) {
            e.printStackTrace();
            return;
        }

        final StatsdSender statsd =
                StatsdSenderImpl.getStatsdSender(statsdOptions.host, statsdOptions.port, CLUSTER_ID, SERVICE_NAME);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                logger.info("sending to statsd");
                statsd.count(COUNT_FIELD, 10);
                statsd.gauge(GAUGE_FIELD, 100);
                statsd.timing(TIMING_FIELD, 25);
            }
        };

        Timer timer = new Timer(false);
        timer.scheduleAtFixedRate(task, 0, 5000);
//        Runtime.getRuntime().addShutdownHook(new Thread(){
//            @Override
//            public void run() { logger.info("shutdown hook thread run()"); }
//        });
        logger.info("StatsdSenderTestMain complete");
    }

    private CmdOptions parseCli(String[] args) throws ParseException {

        Options options = new Options()
                .addOption("h", OPTION_HOST, true, "statsd host")
                .addOption("p", OPTION_PORT, true, "statsd port");

        CommandLine cmd;
        cmd = new DefaultParser().parse(options, args);

        CmdOptions statsdOptions = new CmdOptions();

        statsdOptions.host = cmd.getOptionValue(OPTION_HOST);
        if (statsdOptions.host == null) {
            statsdOptions.host = DEFAULT_HOST;
        }

        String portStr = cmd.getOptionValue(OPTION_PORT);
        if (portStr != null) {
            statsdOptions.port = Integer.parseInt(portStr);
        } else {
            statsdOptions.port = DEFAULT_PORT;
        }

        return statsdOptions;
    }
}