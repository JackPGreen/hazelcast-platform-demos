/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hazelcast.platform.demos.banking.transactionmonitor;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.platform.demos.utils.UtilsConstants;
import com.hazelcast.platform.demos.utils.UtilsProperties;
import com.hazelcast.platform.demos.utils.UtilsSlack;
import com.hazelcast.platform.demos.utils.UtilsSlackSQLJob;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hazelcast.platform.demos.utils.CheckConnectIdempotentCallable;

/**
 * <p>Initialise the Jet cluster to ensure the necessary extra parts
 * exist and necessary jobs are running. These are idempotent operations,
 * the will only do anything for the first node in the Jet cluster.
 * </p>
 */
public class ApplicationInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInitializer.class);

    /**
     * <p>Configure Hazelcast logging via Slf4j. Implementation
     * in "{@code pom.xml}" is Logback.
     * </p>
     * <p>Set this before Hazelcast starts rather than in
     * "{@code hazelcast.yml}", otherwise some log messages
     * are produced before "{@code hazelcast.yml}" is read
     * dictating the right logging framework to use.
     * </p>
     */
    static {
        System.setProperty("hazelcast.logging.type", "slf4j");
    }

    @SuppressFBWarnings(value = "DM_EXIT", justification = "VM shutdown is fail-fast for bad arguments")
    public static void build(String[] args) throws Exception {
        String bootstrapServers = null;
        String pulsarAddress = null;
        String postgresAddress = null;

        if (args.length == 3) {
            bootstrapServers = args[0];
            pulsarAddress = args[1];
            postgresAddress = args[2];
        } else {
            bootstrapServers = System.getProperty("my.bootstrap.servers", "");
            pulsarAddress = System.getProperty(MyConstants.PULSAR_CONFIG_KEY, "");
            postgresAddress = System.getProperty(MyConstants.POSTGRES_CONFIG_KEY, "");
            if (bootstrapServers.isBlank() || pulsarAddress.isBlank() || postgresAddress.isBlank()) {
                LOGGER.error("Usage: 2 arg expected: bootstrapServers pulsarAddress postgresAddress");
                LOGGER.error("eg: 127.0.0.1:9092,127.0.0.1:9093,127.0.0.1:9094 127.0.0.1:6650 127.0.0.1:5432");
                System.exit(1);
            }
        }
        LOGGER.info("'bootstrapServers'=='{}'", bootstrapServers);
        LOGGER.info("'pulsarAddress'=='{}'", pulsarAddress);
        LOGGER.info("'postgresAddress'=='{}'", postgresAddress);

        // Exit if properties not as expected
        Properties properties = null;
        try {
            properties = UtilsProperties.loadClasspathProperties(MyConstants.APPLICATION_PROPERTIES_FILE);
            Properties properties2 = UtilsSlack.loadSlackAccessProperties();
            properties.putAll(properties2);
        } catch (Exception e) {
            LOGGER.error("launchNeededJobs:" + UtilsSlackSQLJob.class.getSimpleName(), e);
            LOGGER.error("launchNeededJobs:" + UtilsSlackSQLJob.class.getSimpleName()
                    + " - No jobs submitted for Slack");
            return;
        }

        Config config = ApplicationConfig.buildConfig(properties);

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        // Since this code creates a Hazelcast node, it cannot be Hazelcast Cloud
        boolean useHzCloud = false;

        TransactionMonitorFlavor transactionMonitorFlavor = MyUtils.getTransactionMonitorFlavor(properties);
        LOGGER.info("TransactionMonitorFlavor=='{}'", transactionMonitorFlavor);
        boolean localhost = System.getProperty("my.docker.enabled", "").equalsIgnoreCase("false");
        boolean kubernetes = System.getProperty("my.kubernetes.enabled", "").equalsIgnoreCase("true");

        // Custom classes on classpath check, only likely to fail if Hz Cloud and not uploaded
        CheckConnectIdempotentCallable.silentCheckCustomClasses(hazelcastInstance);

        // First node runs initialization
        int size = hazelcastInstance.getCluster().getMembers().size();

        String initializerProperty = "my.initialize";
        if (System.getProperty(initializerProperty, "").equalsIgnoreCase(Boolean.TRUE.toString())) {
            ApplicationInitializer.initialise(hazelcastInstance, bootstrapServers, pulsarAddress,
                    postgresAddress, properties, config.getClusterName(), localhost, kubernetes);
        } else {
            if (size == 1) {
                LOGGER.info("Mini initialize, only WAN maps as '{}'=='{}', assume client will do the rest",
                        initializerProperty, System.getProperty(initializerProperty));
                ApplicationInitializer.miniInitialize(hazelcastInstance, transactionMonitorFlavor, useHzCloud, localhost);
            } else {
                LOGGER.info("Skip initialize, assume done by first node, current cluster size is {}", size);
            }
        }
    }

    /**
     * <p>Minimal version of initialize, only maps and mappings needed by WAN.
     * (Instead of WAN replicating "{@code __sql.catalog}" which would do too many.
     * </p>
     */
    public static void miniInitialize(HazelcastInstance hazelcastInstance,
            TransactionMonitorFlavor transactionMonitorFlavor, boolean useHzCloud, boolean localhost) throws Exception {
        TransactionMonitorIdempotentInitializationAdmin
        .createMinimal(hazelcastInstance, transactionMonitorFlavor, useHzCloud, localhost);
    }

    /**
     * <p>Ensure the necessary {@link com.hazelcast.core.DistributedObject} exist to
     * hold processing results. Launch the Jet jobs for this example.
     * </p>
     */
    public static void initialise(HazelcastInstance hazelcastInstance, String bootstrapServers,
            String pulsarAddress, String postgresAddress, Properties properties, String clusterName,
            boolean localhost, boolean kubernetes)
                    throws Exception {

        String pulsarOrKafka = properties.getProperty(MyConstants.PULSAR_OR_KAFKA_KEY);
        boolean usePulsar = MyUtils.usePulsar(pulsarOrKafka);
        LOGGER.debug("usePulsar='{}'", usePulsar);
        String kubernetesOrHzCloud = properties.getProperty(MyConstants.USE_HZ_CLOUD);
        boolean useHzCloud = MyUtils.useHzCloud(kubernetesOrHzCloud);
        LOGGER.debug("useHzCloud='{}'", useHzCloud);
        if (useHzCloud) {
            String message = String.format("useHzCloud=%b but running Hazelcast node! (property '%s'=='%s')",
                    useHzCloud, MyConstants.USE_HZ_CLOUD, kubernetesOrHzCloud);
            throw new RuntimeException(message);
        }
        TransactionMonitorFlavor transactionMonitorFlavor = MyUtils.getTransactionMonitorFlavor(properties);
        LOGGER.info("TransactionMonitorFlavor=='{}'", transactionMonitorFlavor);

        // Address from environment/command line, others from application.properties file.
        properties.put(MyConstants.POSTGRES_ADDRESS, postgresAddress);
        String ourProjectProvenance = properties.getProperty(MyConstants.PROJECT_PROVENANCE);
        String projectName = properties.getOrDefault(UtilsConstants.SLACK_PROJECT_NAME,
                ApplicationInitializer.class.getSimpleName()).toString();

        TransactionMonitorIdempotentInitialization.createNeededObjects(hazelcastInstance,
                properties, ourProjectProvenance, transactionMonitorFlavor, localhost, useHzCloud);
        addListeners(hazelcastInstance, bootstrapServers, pulsarAddress, usePulsar, projectName, clusterName,
                transactionMonitorFlavor, kubernetes);
        TransactionMonitorIdempotentInitialization.loadNeededData(hazelcastInstance, bootstrapServers, pulsarAddress, usePulsar,
                useHzCloud, transactionMonitorFlavor);
        TransactionMonitorIdempotentInitialization.defineQueryableObjects(hazelcastInstance,
                bootstrapServers, properties, transactionMonitorFlavor,
                localhost, kubernetes, useHzCloud);

        TransactionMonitorIdempotentInitialization.launchNeededJobs(hazelcastInstance, bootstrapServers,
                pulsarAddress, properties, clusterName, transactionMonitorFlavor, kubernetes);
    }


    /**
     * <p>Logging listeners, and job control.
     * </p>
     *
     * @param hazelcastInstance
     */
    static void addListeners(HazelcastInstance hazelcastInstance, String bootstrapServers,
            String pulsarAddress, boolean usePulsar, String projectName, String clusterName,
            TransactionMonitorFlavor transactionMonitorFlavor, boolean kubernetes) {
        MyMembershipListener myMembershipListener = new MyMembershipListener(hazelcastInstance);
        hazelcastInstance.getCluster().addMembershipListener(myMembershipListener);

        JobControlListener jobControlListener =
                new JobControlListener(bootstrapServers, pulsarAddress, usePulsar,
                        projectName, clusterName, transactionMonitorFlavor, kubernetes);
        hazelcastInstance.getMap(MyConstants.IMAP_NAME_JOB_CONTROL)
            .addLocalEntryListener(jobControlListener);
    }

}
