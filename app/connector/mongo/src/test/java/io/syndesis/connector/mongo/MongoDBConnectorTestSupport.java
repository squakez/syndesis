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

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.impl.RouteService;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.syndesis.common.model.action.ConnectorAction;
import io.syndesis.common.model.connection.Connector;
import io.syndesis.common.model.integration.Step;
import io.syndesis.common.model.integration.StepKind;
import io.syndesis.connector.support.test.ConnectorTestSupport;

public abstract class MongoDBConnectorTestSupport extends ConnectorTestSupport {

	protected static final String CONNECTION_BEAN_NAME = "myDb";

	private static MongodExecutable mongodExecutable;

	protected final static String HOST = "localhost";
	protected final static int PORT = 27017;
	protected final static String DATABASE = "test";
	protected final static String COLLECTION = "test";

	// Client connections
	protected static MongoClient mongoClient;
	protected static MongoDatabase database;
	protected static MongoCollection<Document> collection;

	@AfterClass
	public static void tearDownMongo() {
		mongodExecutable.stop();
		mongoClient.close();
	}

	@BeforeClass
	public static void startUpMongo() throws Exception {
		IMongodConfig mongodConfig = new MongodConfigBuilder().version(Version.Main.PRODUCTION)
				.net(new Net(HOST, PORT, Network.localhostIsIPv6())).build();

		MongodStarter starter = MongodStarter.getDefaultInstance();
		mongodExecutable = starter.prepare(mongodConfig);
		mongodExecutable.start();
		initClient();
	}

	private static void initClient() {
		mongoClient = new MongoClient(HOST);
		database = mongoClient.getDatabase(DATABASE);
		collection = database.getCollection(COLLECTION);
	}

	// **************************
	// Helpers
	// **************************

	protected Step newMongoDBEndpointStep(String actionId, Consumer<Step.Builder> consumer, MongoClient mongoClient) {
		final Connector connector = getResourceManager().mandatoryLoadConnector("mongodb");
		final ConnectorAction action = getResourceManager().mandatoryLookupAction(connector, actionId);

		final Step.Builder builder = new Step.Builder().stepKind(StepKind.endpoint).action(action)
				.connection(new io.syndesis.common.model.connection.Connection.Builder().connector(connector).build());

		consumer.accept(builder);

		return builder.build();
	}

	protected List<Step> fromDirectToMongo(String directStart, MongoClient mongoClient, String connector, String db,
			String collection, String operation) {
		return Arrays.asList(
				newSimpleEndpointStep("direct", builder -> builder.putConfiguredProperty("name", directStart)),
				newMongoDBEndpointStep(connector, builder -> {
					builder.putConfiguredProperty("database", db);
					builder.putConfiguredProperty("collection", collection);
					builder.putConfiguredProperty("operation", operation);
				}, mongoClient));
	}

	protected List<Step> fromMongoToMock(String mock, MongoClient mongoClient, String connector, String db,
			String collection, String tailTrackIncreasingField) {
		return Arrays.asList(newMongoDBEndpointStep(connector, builder -> {
			builder.putConfiguredProperty("database", db);
			builder.putConfiguredProperty("collection", collection);
			builder.putConfiguredProperty("tailTrackIncreasingField", tailTrackIncreasingField);
		}, mongoClient), newSimpleEndpointStep("mock", builder -> builder.putConfiguredProperty("name", mock)));
	}
}
