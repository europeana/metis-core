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
package eu.europeana.metis.core.mongo;

import com.mongodb.MongoClient;
import eu.europeana.metis.core.common.AltLabel;
import eu.europeana.metis.core.common.PrefLabel;
import eu.europeana.metis.core.dataset.Dataset;
import eu.europeana.metis.core.dataset.DatasetIdSequence;
import eu.europeana.metis.core.workflow.ScheduledWorkflow;
import eu.europeana.metis.core.workflow.Workflow;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.plugins.EnrichmentPlugin;
import eu.europeana.metis.core.workflow.plugins.HTTPHarvestPlugin;
import eu.europeana.metis.core.workflow.plugins.OaipmhHarvestPlugin;
import eu.europeana.metis.core.workflow.plugins.ValidationPlugin;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

/**
 * Class to initializes the mongo collections and the {@link Datastore} connection.
 */
public class MorphiaDatastoreProvider {

  private Datastore datastore;

  /**
   * Constructor to initialize the mongo collections and the {@link Datastore} connection.
   * @param mongoClient {@link MongoClient}
   * @param databaseName the database name
   */
  public MorphiaDatastoreProvider(MongoClient mongoClient, String databaseName) {
    Morphia morphia = new Morphia();
    morphia.map(Dataset.class);
    morphia.map(DatasetIdSequence.class);
    morphia.map(PrefLabel.class);
    morphia.map(AltLabel.class);
    morphia.map(Workflow.class);
    morphia.map(WorkflowExecution.class);
    morphia.map(ScheduledWorkflow.class);
    morphia.map(OaipmhHarvestPlugin.class);
    morphia.map(HTTPHarvestPlugin.class);
    morphia.map(EnrichmentPlugin.class);
    morphia.map(ValidationPlugin.class);
    datastore = morphia.createDatastore(mongoClient, databaseName);
    datastore.ensureIndexes();

    DatasetIdSequence datasetIdSequence = datastore.find(DatasetIdSequence.class).get();
    if (datasetIdSequence == null) {
      datastore.save(new DatasetIdSequence(0));
    }
  }

  /**
   * @return the {@link Datastore} connection to Mongo
   */
  public Datastore getDatastore() {
    return datastore;
  }
}
