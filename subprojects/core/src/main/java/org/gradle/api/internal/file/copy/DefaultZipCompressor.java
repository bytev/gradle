/*
 * Copyright 2012 the original author or authors.
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
package org.gradle.api.internal.file.copy;

import org.apache.tools.zip.Zip64Mode;
import org.apache.tools.zip.ZipOutputStream;
import org.gradle.api.UncheckedIOException;
import org.gradle.internal.IoActions;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DefaultZipCompressor implements ZipCompressor {
    private final int entryCompressionMethod;
    private final Zip64Mode zip64Mode;

    public DefaultZipCompressor(boolean allowZip64Mode, int entryCompressionMethod) {
        this.entryCompressionMethod = entryCompressionMethod;
        zip64Mode = allowZip64Mode ? Zip64Mode.AsNeeded : Zip64Mode.Never;
    }

    @Override
    public ZipOutputStream createArchiveOutputStream(File destination) throws IOException {
        ZipOutputStream outStream = new ZipOutputStream(destination);
        try {
            outStream.setUseZip64(zip64Mode);
            outStream.setMethod(entryCompressionMethod);
            return outStream;
        } catch (Exception e) {
            IoActions.closeQuietly(outStream);
            String message = String.format("Unable to create ZIP output stream for file %s.", destination);
            throw new UncheckedIOException(message, e);
        }
    }

    @Override
    public FileSystem createArchiveFileSystem(Path destination, String encoding) throws IOException {
        Map<String, Object> env = new HashMap<>();
        env.put("create", true);
        if (encoding != null) {
            env.put("encoding", encoding);
        }
        return FileSystems.newFileSystem(URI.create("jar:" + destination.toUri()), env, null);
    }
}
