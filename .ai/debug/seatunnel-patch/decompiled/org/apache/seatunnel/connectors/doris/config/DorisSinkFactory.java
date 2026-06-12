/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.auto.service.AutoService
 */
package org.apache.seatunnel.connectors.doris.config;

import com.google.auto.service.AutoService;
import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.table.factory.Factory;
import org.apache.seatunnel.api.table.factory.TableSinkFactory;
import org.apache.seatunnel.connectors.doris.config.DorisConfig;

@AutoService(value={Factory.class})
public class DorisSinkFactory
implements TableSinkFactory {
    public static final String IDENTIFIER = "Doris";

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public OptionRule optionRule() {
        return OptionRule.builder().required(DorisConfig.FENODES, DorisConfig.USERNAME, DorisConfig.PASSWORD, DorisConfig.SINK_LABEL_PREFIX, DorisConfig.DORIS_SINK_CONFIG_PREFIX).optional(DorisConfig.SINK_ENABLE_2PC, DorisConfig.SINK_ENABLE_DELETE).build();
    }
}

