/*
 * Copyright 2007-2013 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 *
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */
package eu.europeana.metis.framework.rest;

import eu.europeana.metis.framework.dao.ZohoClient;
import eu.europeana.metis.framework.dao.ZohoMockClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * In-memory implementation of Zoho rest
 * Created by ymamakis on 6/6/16.
 */
@Component
@Profile("development")
public class InMemoryZohoRestConfig implements RestConfig{
    @Override
    public ZohoClient getZohoClient() {
        ZohoMockClient client = new ZohoMockClient();
        client.populate();
        return client;
    }
}
