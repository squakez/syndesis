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

import com.mongodb.client.model.Filters;
import io.syndesis.common.model.integration.Step;
import org.bson.Document;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.JUnitTestsShouldIncludeAssert"})
public class MongoDBConnectorSaveTest extends MongoDBConnectorTestSupport {

    // **************************
    // Set up
    // **************************

    @Override
    protected List<Step> createSteps() {
        return fromDirectToMongo("start", "io.syndesis.connector:connector-mongodb-producer", DATABASE, COLLECTION,
            "save");
    }

    // **************************
    // Tests
    // **************************

    @Test
    public void mongoSaveNewTest() {
        // When
        // Given
        String saveArguments = "{\"_id\":11,\"test\":\"new\"}]";
        template().sendBody("direct:start", saveArguments);
        // Then
        List<Document> docsFound = collection.find(Filters.eq("_id", 11)).into(new ArrayList<Document>());
        assertEquals(1, docsFound.size());
        assertEquals("new", docsFound.get(0).getString("test"));
    }

    @Test
    public void mongoSaveExistingTest() {
        // When
        String saveArguments = "{\"_id\":32,\"test\":\"new\"}]";
        template().sendBody("direct:start", saveArguments);
        // Given
        String updateArguments = "{\"_id\":32,\"test\":\"save\",\"newField\":true}]";
        template().sendBody("direct:start", updateArguments);
        // Then
        List<Document> docsFound = collection.find(Filters.eq("_id", 32)).into(new ArrayList<Document>());
        assertEquals(1, docsFound.size());
        assertEquals("save", docsFound.get(0).getString("test"));
        assertEquals(true, docsFound.get(0).get("newField"));
    }

}
