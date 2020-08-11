/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.lookup;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.apache.nifi.serialization.record.MapRecord;
import org.apache.nifi.controller.ControllerService;
import org.apache.nifi.json.JsonRecordSetWriter;
import org.apache.nifi.json.JsonTreeReader;
import org.apache.nifi.schema.access.SchemaAccessUtils;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.distributed.cache.client.Deserializer;
import org.apache.nifi.distributed.cache.client.DistributedMapCacheClient;
import org.apache.nifi.distributed.cache.client.Serializer;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Test;

import static org.apache.nifi.schema.inference.SchemaInferenceUtil.INFER_SCHEMA;

public class TestDistributedMapCacheRecordLookupService {

    final static Optional<String> EMPTY_STRING = Optional.empty();

    @Test
    public void testDistributedMapCacheLookupService() throws LookupFailureException,InitializationException {
        final TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
        final DistributedMapCacheRecordLookupService service = new DistributedMapCacheRecordLookupService();
        final DistributedMapCacheClient client = new DistributedMapCacheClientImpl();
        ControllerService writer = new JsonRecordSetWriter();
        ControllerService reader = new JsonTreeReader();
        runner.addControllerService("reader", reader);
        runner.setProperty(reader, SchemaAccessUtils.SCHEMA_ACCESS_STRATEGY,INFER_SCHEMA);
        runner.addControllerService("writer", writer);
        runner.addControllerService("client", client);
        runner.addControllerService("lookup-service", service);
        runner.setProperty(service, DistributedMapCacheRecordLookupService.PROP_DISTRIBUTED_CACHE_SERVICE, "client");
        runner.setProperty(service, DistributedMapCacheRecordLookupService.RECORD_READER, "reader");

        runner.enableControllerService(reader);
        runner.enableControllerService(client);
        runner.enableControllerService(service);

        runner.assertValid(service);

        assertThat(service, instanceOf(LookupService.class));

        final Optional<?> get = service.lookup(Collections.singletonMap("key", "myKey"));
        assertEquals("myValue1", ((MapRecord) get.get()).getAsString("myKey1"));

        final Optional<?> absent = service.lookup(Collections.singletonMap("key", "absentKey"));
        assertEquals(EMPTY_STRING, absent);
    }

    static final class DistributedMapCacheClientImpl extends AbstractControllerService implements DistributedMapCacheClient {

        private Map<String, Object> map = new HashMap<String, Object>();

        @OnEnabled
        public void onEnabled(final ConfigurationContext context) throws IOException {
        	String byteOut = "{\"myKey1\":\"myValue1\"}";
            map.put("myKey", byteOut.getBytes());
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void onPropertyModified(final PropertyDescriptor descriptor, final String oldValue, final String newValue) {
        }

        @Override
        protected java.util.List<PropertyDescriptor> getSupportedPropertyDescriptors() {
            return new ArrayList<>();
        }

        @Override
        public <K, V> boolean putIfAbsent(final K key, final V value, final Serializer<K> keySerializer, final Serializer<V> valueSerializer) throws IOException {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public <K, V> V getAndPutIfAbsent(final K key, final V value, final Serializer<K> keySerializer, final Serializer<V> valueSerializer,
                final Deserializer<V> valueDeserializer) throws IOException {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public <K> boolean containsKey(final K key, final Serializer<K> keySerializer) throws IOException {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K, V> V get(final K key, final Serializer<K> keySerializer, final Deserializer<V> valueDeserializer) throws IOException {
            return (V) map.get(key);
        }

        @Override
        public <K> boolean remove(final K key, final Serializer<K> serializer) throws IOException {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public long removeByPattern(String regex) throws IOException {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public <K, V> void put(final K key, final V value, final Serializer<K> keySerializer, final Serializer<V> valueSerializer) throws IOException {
            throw new UnsupportedOperationException("not implemented");
        }
    }

}
