/*
 * Copyright 2024 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.result;

import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.internal.serialize.Decoder;
import org.gradle.internal.serialize.Encoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A serializer for {@link AttributeContainer} that deduplicates the values and delegates to
 * another serializer for the actual serialization.
 * <p>
 * This serializer should not be reused to serialize multiple graphs.
 */
public class DeduplicatingAttributeContainerSerializer implements AttributeContainerSerializer {
    // We use an identity hashmap for performance, because we know that our attributes
    // are generated by a factory which guarantees same instances
    private final Map<AttributeContainer, Integer> writeIndex = new IdentityHashMap<>();
    private final List<ImmutableAttributes> readIndex = new ArrayList<>();
    private final AttributeContainerSerializer delegate;

    public DeduplicatingAttributeContainerSerializer(AttributeContainerSerializer delegate) {
        this.delegate = delegate;
    }

    public void reset() {
        writeIndex.clear();
        readIndex.clear();
    }

    @Override
    public ImmutableAttributes read(Decoder decoder) throws IOException {
        boolean empty = decoder.readBoolean();
        if (empty) {
            return ImmutableAttributes.EMPTY;
        }
        int idx = decoder.readSmallInt();
        ImmutableAttributes attributes;
        if (idx == readIndex.size()) {
            // new entry
            attributes = delegate.read(decoder);
            readIndex.add(attributes);
        } else {
            attributes = readIndex.get(idx);
        }
        return attributes;
    }

    @Override
    public void write(Encoder encoder, AttributeContainer container) throws IOException {
        if (container.isEmpty()) {
            encoder.writeBoolean(true);
            return;
        } else {
            encoder.writeBoolean(false);
        }
        Integer idx = writeIndex.get(container);
        if (idx == null) {
            // new value
            int index = writeIndex.size();
            encoder.writeSmallInt(index);
            writeIndex.put(container, index);
            delegate.write(encoder, container);
        } else {
            // known value, only write index
            encoder.writeSmallInt(idx);
        }
    }
}
