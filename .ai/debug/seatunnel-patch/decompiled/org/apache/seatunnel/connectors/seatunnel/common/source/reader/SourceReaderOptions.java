/*
 * Decompiled with CFR 0.152.
 */
package org.apache.seatunnel.connectors.seatunnel.common.source.reader;

import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;
import org.apache.seatunnel.shade.com.typesafe.config.Config;

public class SourceReaderOptions {
    public static final Option<Long> SOURCE_READER_CLOSE_TIMEOUT = Options.key("source.reader.close.timeout").longType().defaultValue(60000L).withDescription("The timeout when closing the source reader");
    public static final Option<Integer> ELEMENT_QUEUE_CAPACITY = Options.key("source.reader.element.queue.capacity").intType().defaultValue(2).withDescription("The capacity of the element queue in the source reader.");
    public final long sourceReaderCloseTimeout;
    public final int elementQueueCapacity;

    public SourceReaderOptions(Config config) {
        this(ReadonlyConfig.fromConfig(config));
    }

    public SourceReaderOptions(ReadonlyConfig config) {
        this.sourceReaderCloseTimeout = config.get(SOURCE_READER_CLOSE_TIMEOUT);
        this.elementQueueCapacity = config.get(ELEMENT_QUEUE_CAPACITY);
    }

    public long getSourceReaderCloseTimeout() {
        return this.sourceReaderCloseTimeout;
    }

    public int getElementQueueCapacity() {
        return this.elementQueueCapacity;
    }
}

