/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.internal.operations.recorder;

import org.gradle.internal.concurrent.Stoppable;
import org.gradle.internal.progress.BuildOperationDescriptor;
import org.gradle.internal.progress.BuildOperationListener;
import org.gradle.internal.progress.BuildOperationListenerManager;
import org.gradle.internal.progress.OperationFinishEvent;
import org.gradle.internal.progress.OperationStartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BuildOperationRecorder implements Stoppable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildOperationRecorder.class);

    private final BuildOperationListenerManager listenerManager;
    private final BuildOperationListener listener;

    private List<RecordedBuildOperation> recordedEvents = new ArrayList<RecordedBuildOperation>();
    private boolean stopped = false;

    public BuildOperationRecorder(BuildOperationListenerManager listenerManager) {
        this.listenerManager = listenerManager;
        this.listener = new BuildOperationListener() {
            @Override
            public void started(BuildOperationDescriptor buildOperation, OperationStartEvent startEvent) {
                recordedEvents.add(new RecordedBuildOperation(buildOperation, startEvent, RecordedBuildOperation.OperationEventType.START));
            }

            @Override
            public void finished(BuildOperationDescriptor buildOperation, OperationFinishEvent finishEvent) {
                recordedEvents.add(new RecordedBuildOperation(buildOperation, finishEvent, RecordedBuildOperation.OperationEventType.FINISHED));
            }
        };
        this.listenerManager.addListener(listener);
    }

    public List<RecordedBuildOperation> retrieveEventsAndStop() {
        List<RecordedBuildOperation> returnEvents = recordedEvents;
        recordedEvents = new ArrayList<RecordedBuildOperation>();
        stop();
        return returnEvents;
    }

    public void discardEventsAndStop() {
        if(!stopped){
            int discardedEventCount = recordedEvents.size();
            recordedEvents.clear();
            LOGGER.debug(Integer.toString(discardedEventCount) + " build operation events discarded.");
            stop();
        }
    }

    @Override
    public void stop() {
        stopped = true;
        listenerManager.removeListener(listener);
    }

    public static class RecordedBuildOperation {
        public final BuildOperationDescriptor buildOperation;
        public final Object event;
        public final OperationEventType eventType;

        public RecordedBuildOperation(BuildOperationDescriptor buildOperation, Object event, OperationEventType eventType) {
            this.buildOperation = buildOperation;
            this.event = event;
            this.eventType = eventType;
        }

        public enum OperationEventType {
            START,
            FINISHED
        }
    }
}
