package com.bazaarvoice.emodb.examples.skeleton.resources;

import com.bazaarvoice.emodb.blob.api.BlobStore;
import com.bazaarvoice.emodb.esquire.api.Entity;
import com.bazaarvoice.emodb.esquire.api.Esquire;
import com.bazaarvoice.emodb.examples.skeleton.WellKnowns;
import com.bazaarvoice.emodb.sor.api.AuditBuilder;
import com.bazaarvoice.emodb.sor.api.DataStore;
import com.bazaarvoice.emodb.sor.api.ReadConsistency;
import com.bazaarvoice.emodb.sor.api.TableOptionsBuilder;
import com.bazaarvoice.emodb.sor.api.WriteConsistency;
import com.bazaarvoice.emodb.sor.delta.Deltas;
import com.bazaarvoice.emodb.sor.delta.MapDeltaBuilder;
import com.bazaarvoice.emodb.sor.uuid.TimeUUIDs;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.inject.Inject;
import com.yammer.dropwizard.jersey.params.IntParam;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.Maps;

import javax.annotation.Nullable;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.textQuery;

@Path ("/listing")
public class ListingResource {

    private static final String TABLE = "listing";

    @Inject
    private DataStore sorClient;
   
    @Inject private Esquire esClient;

    private boolean tableExists;

    @PUT
    @Path("sor/{listingid}")
    @Consumes (MediaType.APPLICATION_JSON)
    public Response createListing(@PathParam ("listingid") String listingID, Map<String, Object> attributes) {
        createTableIfNonExistant();
        sorClient.update(
                TABLE,
                listingID,
                TimeUUIDs.newUUID(),
                Deltas.mapBuilder().putAll(attributes).put("type", "listing").build(),
                new AuditBuilder().setLocalHost().setProgram(WellKnowns.APP_NAME).setComment("Creating listing").build(),
                WriteConsistency.STRONG);
        return Response.ok().build();
    }

    @GET
    @Path("sor/ui/{listingid}")
    @Produces (MediaType.TEXT_HTML)
    public ListingView getListingUI(@PathParam("listingid") String listingID) {
        createTableIfNonExistant();
        Map<String, Object> listingAsMap = sorClient.get(TABLE, listingID, ReadConsistency.WEAK);
        return new ListingView(listingAsMap);
    }

    @GET
    @Path("sor/{listingid}")
    @Produces (MediaType.APPLICATION_JSON)
    public Response getListing(@PathParam("listingid") String listingID) {
        createTableIfNonExistant();
        return Response.ok(
                sorClient.get(TABLE, listingID, ReadConsistency.WEAK),
                MediaType.APPLICATION_JSON)
                .build();
    }

    @GET
    @Path("es/all")
    @Produces (MediaType.APPLICATION_JSON)
    public Collection<Map<String, Object>> listAllListings(@QueryParam ("limit") @DefaultValue ("100") IntParam limit) {
        createTableIfNonExistant();
        List<Entity> entities = esClient.queryTable(TABLE).type("listing").limit(limit.get()).execute();
        return Collections2.transform(entities, new Function<Entity, Map<String, Object>>() {
            @Override
            public Map<String, Object> apply(@Nullable Entity input) {
                return input.asMap();
            }
        });
    }

    @GET
    @Path("es/search")
    @Produces (MediaType.APPLICATION_JSON)
    public Collection<Map<String, Object>> listAllListings(@QueryParam ("text") String searchText) {
        createTableIfNonExistant();

        Client client = esClient.backDoor(); // or instantiate an elasticsearch client directly
        SearchRequestBuilder search
                = client.prepareSearch("&"+TABLE)
                .setQuery(textQuery("description", searchText));
        SearchResponse response = search.execute().actionGet();


        List<Entity> entities = esClient.queryTable(TABLE).type("listing").with("description", searchText).execute();
        return Collections2.transform(entities, new Function<Entity, Map<String, Object>>() {
            @Override
            public Map<String, Object> apply(@Nullable Entity input) {
                return input.asMap();
            }
        });
    }

    @DELETE
    @Path("sor/{listingid}")
    public Response deleteListing(@PathParam("listingid") String listingID) {
        createTableIfNonExistant();
        sorClient.update(
                TABLE,
                listingID,
                TimeUUIDs.newUUID(),
                Deltas.delete(),
                new AuditBuilder().setLocalHost().setProgram(WellKnowns.APP_NAME).setComment("Deleting listing").build(),
                WriteConsistency.STRONG);
        return Response.ok().build();
    }

    @POST
    @Path("sor/{listingid}")
    @Consumes (MediaType.APPLICATION_JSON)
    public Response updateListing(@PathParam("listingid") String listingID, Map<String, Object> newListingAttributes) {
        createTableIfNonExistant();

        MapDeltaBuilder deltaBuilder = Deltas.mapBuilder();

        Map<String, Object> oldListingAttributes = sorClient.get(TABLE, listingID, ReadConsistency.WEAK);
        for(Map.Entry<String, Object> oldEntry : oldListingAttributes.entrySet()) {
            if (newListingAttributes.containsKey(oldEntry.getKey())) {
                if (newListingAttributes.get(oldEntry.getKey()).equals(oldEntry.getValue())) {
                    newListingAttributes.remove(oldEntry.getKey());  // same key and value in old and new map, no sense in updating it
                }
            } else {
                deltaBuilder.remove(oldEntry.getKey());
            }
        }
        deltaBuilder.putAll(newListingAttributes);
        deltaBuilder.putIfAbsent("type", "listing");

        sorClient.update(
                TABLE,
                listingID,
                TimeUUIDs.newUUID(),
                deltaBuilder.build(),
                new AuditBuilder().setLocalHost().setProgram(WellKnowns.APP_NAME).setComment("Updating listing").build(),
                WriteConsistency.STRONG);
        return Response.ok().build();
    }

    private void createTableIfNonExistant() {
        if (!tableExists && !sorClient.getTableExists(TABLE)) {
            sorClient.createTable(
                    TABLE,
                    new TableOptionsBuilder().setPlacement("emo:ugc").build(),
                    Maps.<String, Object>newHashMap(),
                    new AuditBuilder().setLocalHost().setProgram(WellKnowns.APP_NAME).setComment("Creating table").build());

            tableExists = true;
        }
    }
}
