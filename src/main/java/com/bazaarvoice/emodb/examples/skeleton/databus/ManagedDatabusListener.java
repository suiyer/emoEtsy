package com.bazaarvoice.emodb.examples.skeleton.databus;

import com.bazaarvoice.emodb.databus.api.Databus;
import com.bazaarvoice.emodb.databus.api.Event;
import com.bazaarvoice.emodb.databus.api.EventKey;
import com.bazaarvoice.emodb.examples.skeleton.config.DatabusListeningConfiguration;
import com.bazaarvoice.emodb.sor.condition.Conditions;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.inject.Inject;
import com.yammer.dropwizard.lifecycle.Managed;
import com.yammer.dropwizard.logging.Log;
import org.apache.commons.collections.CollectionUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ManagedDatabusListener implements Managed {

    private final Log log = Log.forClass(getClass());

    @Inject private Databus databusClient;
    @Inject @DatabusResource private ExecutorService databusPollingExecutorService;
    @Inject @DatabusResource private ScheduledExecutorService databusSubscriptionExecutorService;
    @Inject private DatabusListeningConfiguration databusListeningConfiguration;

    @Override
    public void start() throws Exception {

        //Perform the initial subscription to listen to EVERYTHING
        databusClient.subscribe(databusListeningConfiguration.getSubscriptionName(), Conditions.alwaysTrue(), 86400, 86400);

        // Start a number of threads equal to "core" threads that will poll the databus independently looking for work.
        for (int threadCtr = 0; threadCtr < databusListeningConfiguration.getNumberPollingThreads(); ++threadCtr) {
            databusPollingExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    while(true) {
                        try {
                            //log.info("[{}] Polling databus", Thread.currentThread().getName());
                            final List<Event> events = databusClient.poll(
                                    databusListeningConfiguration.getSubscriptionName(),
                                    databusListeningConfiguration.getEventTimeToLiveSeconds(),
                                    databusListeningConfiguration.getMaxNumEventsPerDatabusPoll());

                            if (CollectionUtils.isNotEmpty(events)) {
                                // Do something useful with the event
                                for (Event event : events) {
                                    log.info("[{}] Received event: key={}, contents={}", Thread.currentThread().getName(), event.getEventKey(), event.getContent());
                                }

                                // Now that we've done "something" with the events, we need to ack them to prevent them from being repeatedly redelivered.
                                databusClient.acknowledge(databusListeningConfiguration.getSubscriptionName(), Collections2.transform(events, new Function<Event, EventKey>() {
                                    @Override
                                    public EventKey apply(@Nullable Event input) {
                                        return input.getEventKey();
                                    }
                                }));
                            }
                        } catch (Throwable t) {
                            // These polling threads should be as resilient as possible.
                            log.error(t, "[{}] An uncaught exception occurred while polling for events from the Databus.  Stacktrace follows: ", Thread.currentThread().getName());
                        }

                        // Slow things down just a little bit to avoid breaking a sweat
                        try {
                            if (!Thread.interrupted()) {
                                Thread.sleep(databusListeningConfiguration.getPollDelayMillis());
                            }
                        } catch (InterruptedException e) { /* no-op */ }

                        // Allow the thread to die if the interrupted flag is set (will get set by the Thread Pool when it wants to shut down).
                        if (Thread.interrupted()) {
                            break;
                        }
                    }
                    log.info("DatabusPollerThread[name={}] exiting",  Thread.currentThread().getName());
                }
            });
            try {
                Thread.sleep(1000L); // Delay starting the threads a little bit so that they don't all slam the Databus at the same time.
            } catch (InterruptedException e) {/* no-op */}
        }


        // Create a scheduled and repeating action that will subscribe to the databus as long as this service remains alive.
        databusSubscriptionExecutorService.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Renew subscription to the databus
                            log.info("Renewing subscription to the databus");
                            databusClient.subscribe(
                                    databusListeningConfiguration.getSubscriptionName(),
                                    Conditions.alwaysTrue(),
                                    databusListeningConfiguration.getSubscriptionTimeToLiveSeconds(),
                                    databusListeningConfiguration.getSubscriptionEventTimeToLiveSeconds());
                        } catch (Throwable t) {
                            // Any exception or abnormal termination of this thread within the single-threaded ScheduledExecutorService will cause subsequent scheduled executions to be skipped.
                            log.error(t, "Error while renewing subscription to the databus");
                        }
                    }
                },
                databusListeningConfiguration.getInitialSubscriptionDelaySeconds(),
                databusListeningConfiguration.getSubscriptionIntervalSeconds(),
                TimeUnit.SECONDS);
    }

    @Override
    public void stop() throws Exception {
    }
}
