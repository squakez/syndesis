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
package io.syndesis.connector.mongo;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.syndesis.common.model.integration.Step;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.Test;

public class MongoDBConnectorFindAllTest extends MongoDBConnectorProducerTestSupport {

    private final static String COLLECTION = "findAllCollection";

    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    @Override
    protected List<Step> createSteps() {
        return fromDirectToMongo("start", "io.syndesis.connector:connector-mongodb-find", DATABASE, COLLECTION);
    }

    @Test
    public void mongoFindAllTest() {
        // When
        String uniqueId = UUID.randomUUID().toString();
        Document doc = new Document();
        doc.append("_id", 1);
        doc.append("unique", uniqueId);
        collection.insertOne(doc);
        String uniqueId2 = UUID.randomUUID().toString();
        Document doc2 = new Document();
        doc2.append("_id", 2);
        doc2.append("unique", uniqueId2);
        collection.insertOne(doc2);
        // Given
        @SuppressWarnings("unchecked")
        List<String> resultsAsString = template.requestBody("direct:start", null, List.class);
        List<Document> results = resultsAsString.stream().map(Document::parse).collect(Collectors.toList());
        // Then
        Assertions.assertThat(results).hasSize(2);
        Assertions.assertThat(results).contains(doc, doc2);
    }

    @Test
    public void mongoFindAllFilterTest() {
        // When
        String uniqueId = UUID.randomUUID().toString();
        Document doc = new Document();
        doc.append("color", "green");
        doc.append("unique", uniqueId);
        collection.insertOne(doc);
        String uniqueId2 = UUID.randomUUID().toString();
        Document doc2 = new Document();
        doc2.append("color", "red");
        doc2.append("unique", uniqueId2);
        collection.insertOne(doc2);
        // Given
        @SuppressWarnings("unchecked")
        List<String> resultsAsString = template.requestBody("direct:start", "{\"color\": \"red\"}", List.class);
        List<Document> results = resultsAsString.stream().map(Document::parse).collect(Collectors.toList());
        // Then
        Assertions.assertThat(results).hasSize(1);
        Assertions.assertThat(results).contains(doc2);
    }
}
