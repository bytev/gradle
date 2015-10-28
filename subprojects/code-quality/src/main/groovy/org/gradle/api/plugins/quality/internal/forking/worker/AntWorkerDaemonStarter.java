/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.plugins.quality.internal.forking.worker;

import java.io.File;
import org.gradle.StartParameter;
import org.gradle.api.internal.tasks.compile.daemon.DaemonForkOptions;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.internal.Factory;
import org.gradle.process.internal.JavaExecHandleBuilder;
import org.gradle.process.internal.WorkerProcess;
import org.gradle.process.internal.WorkerProcessBuilder;
import org.gradle.util.Clock;


public class AntWorkerDaemonStarter {

    private final static Logger LOG = Logging.getLogger(AntWorkerDaemonStarter.class);
    private final Factory<WorkerProcessBuilder> workerFactory;
    private final StartParameter startParameter;

    public AntWorkerDaemonStarter(Factory<WorkerProcessBuilder> workerFactory, StartParameter startParameter){
        this.workerFactory = workerFactory;
        this.startParameter = startParameter;
    }

    public AntWorkerDaemonClient startDaemon(File workingDir, DaemonForkOptions forkOptions) {
        LOG.debug("Starting Gradle compiler daemon with fork options {}.", forkOptions);
        Clock clock = new Clock();
        WorkerProcessBuilder builder = workerFactory.create();
        builder.setLogLevel(startParameter.getLogLevel()); // NOTE: might make sense to respect per-compile-task log level
        builder.applicationClasspath(forkOptions.getClasspath());
        builder.sharedPackages(forkOptions.getSharedPackages());
        builder.setLoadApplicationInSystemClassLoader(true);
        JavaExecHandleBuilder javaCommand = builder.getJavaCommand();
        javaCommand.setMinHeapSize(forkOptions.getMinHeapSize());
        javaCommand.setMaxHeapSize(forkOptions.getMaxHeapSize());
        javaCommand.setJvmArgs(forkOptions.getJvmArgs());
        javaCommand.setWorkingDir(workingDir);
        WorkerProcess process = builder.worker(new AntWorkerDaemonServer()).setBaseName("Gradle Ant Worker Daemon").build();
        process.start();

        AntWorkerDaemonServerProtocol server = process.getConnection().addOutgoing(AntWorkerDaemonServerProtocol.class);
        AntWorkerDaemonClient client = new AntWorkerDaemonClient(forkOptions, process, server);
        process.getConnection().addIncoming(AntWorkerDaemonClientProtocol.class, client);
        process.getConnection().connect();

        LOG.info("Started Gradle ant worker daemon ({}) with fork options {}.", clock.getTime(), forkOptions);

        return client;
    }
}
