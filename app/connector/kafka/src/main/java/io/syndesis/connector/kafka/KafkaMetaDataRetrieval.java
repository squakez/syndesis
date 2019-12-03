/*
 * Copyright (C) 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.syndesis.connector.kafka;

import io.syndesis.connector.support.verifier.api.ComponentMetadataRetrieval;
import io.syndesis.connector.support.verifier.api.PropertyPair;
import io.syndesis.connector.support.verifier.api.SyndesisMetaConnectionProperties;
import io.syndesis.connector.support.verifier.api.SyndesisMetadata;
import org.apache.camel.CamelContext;
import org.apache.camel.component.extension.MetaDataExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KafkaMetaDataRetrieval extends ComponentMetadataRetrieval {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMetaDataRetrieval.class);

    /**
     * TODO: use local extension, remove when switching to camel 2.22.x
     */
    @Override
    protected MetaDataExtension resolveMetaDataExtension(CamelContext context, Class<? extends MetaDataExtension> metaDataExtensionClass, String componentId, String actionId) {
        return new KafkaMetaDataExtension(context);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected SyndesisMetadata adapt(CamelContext context, String componentId, String actionId, Map<String, Object> properties, MetaDataExtension.MetaData metadata) {
        try {
            Set<String> topicsNames = (Set<String>) metadata.getPayload();

            List<PropertyPair> topicsResult = new ArrayList<>();
            topicsNames.stream().forEach(
                t -> topicsResult.add(new PropertyPair(t, t))
            );

            return SyndesisMetadata.of(
                Collections.singletonMap("topic", topicsResult)
            );
        } catch ( Exception e) {
            return SyndesisMetadata.EMPTY;
        }
    }

    @Override
    public SyndesisMetaConnectionProperties fetchConnectionDynamicProperties(Map<String, Object> properties) {
        LOGGER.info("Calling fetchConnectionDynamicProperties({})", properties);

        // To retrieve dynamically any connection information
        Map<String, List<PropertyPair>> dynamicProperties = new HashMap<>();
        List<PropertyPair> brokers = new ArrayList<>();
        brokers.add(new PropertyPair("cluster1","172.0.0.1:9092,172.0.0.2:9092,172.0.0.3:9092"));
        brokers.add(new PropertyPair("cluster2","172.10.10.11:9092,172.10.10.12:9092,172.10.10.13:9092"));
        // ...
        dynamicProperties.put("brokers", brokers);

        return new SyndesisMetaConnectionProperties(dynamicProperties);
    }
}
