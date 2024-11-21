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

package org.gradle.internal.logging.serializer;

import org.gradle.internal.logging.events.UserInputResumeEvent;
import org.gradle.internal.serialize.Decoder;
import org.gradle.internal.serialize.Encoder;
import org.gradle.internal.serialize.Serializer;
import org.gradle.internal.time.Timestamp;

public class UserInputResumeEventSerializer implements Serializer<UserInputResumeEvent> {
    private final Serializer<Timestamp> timestampSerializer;

    public UserInputResumeEventSerializer(Serializer<Timestamp> timestampSerializer) {
        this.timestampSerializer = timestampSerializer;
    }

    @Override
    public void write(Encoder encoder, UserInputResumeEvent event) throws Exception {
        timestampSerializer.write(encoder, event.getTime());
    }

    @Override
    public UserInputResumeEvent read(Decoder decoder) throws Exception {
        return new UserInputResumeEvent(timestampSerializer.read(decoder));
    }
}
