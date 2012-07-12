package com.bazaarvoice.emodb.examples.skeleton;

import com.bazaarvoice.emodb.blob.api.BlobStore;
import com.bazaarvoice.emodb.blob.client.BlobStoreClientFactory;
import com.bazaarvoice.emodb.blob.client.BlobStoreFixedHostDiscoverySource;
import com.bazaarvoice.emodb.examples.skeleton.config.SkeletonConfiguration;
import com.bazaarvoice.soa.pool.ServicePoolBuilder;
import com.bazaarvoice.soa.retry.RetryNTimes;
import com.sun.xml.internal.rngom.util.Uri;
import com.yammer.dropwizard.config.Configuration;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ListingImageIngestor {
    static HttpClient localHttpClient = new DefaultHttpClient();
    static HttpClient etsyHttpClient = new DefaultHttpClient();

    public static void main(String args[]) {

        URI newUri = null;


                    Iterator<JsonNode> listings = getListings();
            for (; listings.hasNext();) {
                    JsonNode listing = listings.next();
                if (listing.get("listing_id") != null) {
                String listingID = listing.get("listing_id").asText();
                    Iterator<JsonNode> imagesIterator = getImages(listingID);
                for (; imagesIterator.hasNext();) {
                    JsonNode image = imagesIterator.next();
                    String imageID = image.get("listing_image_id").asText();
                    if (image.get("url_fullxfull") != null) {
                        String uri = image.get("url_fullxfull").asText();
                    storeImage(imageID, uri);
                    }
                }
                }
                    }


}


static Iterator<JsonNode> getListings() {
    URI newUri = null;
        String path = "/listing/es/all";
    Iterator<JsonNode> listings = null;
        try {
            newUri = new URI("http", null, "localhost", 10090, path, null, null);
            HttpGet httpget = new HttpGet(newUri);
            HttpResponse response = localHttpClient.execute(httpget);

            HttpEntity resEntity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == 200 && resEntity != null) {


                ObjectMapper mapper = new ObjectMapper();

                InputStream instream = resEntity.getContent();

                ArrayNode data = mapper.readValue(instream, ArrayNode.class);
                listings = data.getElements();
            }
        } catch(Exception e) {

        }
    return listings;
}

    static Iterator<JsonNode> getImages(String listingID) {
        Iterator<JsonNode> imageItems = null;
        try {
            URI uri = new URI("http", null, "openapi.etsy.com", 80, "/v2/listings/" + listingID+"/images", "api_key=lb1p6bet8gloxrbusmbifwno", null);
                    HttpGet httpget = new HttpGet(uri);
                    HttpResponse response = etsyHttpClient.execute(httpget);

                    HttpEntity resEntity = response.getEntity();
                    if (response.getStatusLine().getStatusCode() == 200 && resEntity != null) {


                        ObjectMapper feedbackMapper = new ObjectMapper();

                        InputStream feedbackstream = resEntity.getContent();

                        ObjectNode feedbackdata = feedbackMapper.readValue(feedbackstream, ObjectNode.class);
                        ArrayNode feedback = (ArrayNode) feedbackdata.get("results");
                        imageItems = feedback.getElements();
        }
        } catch(Exception e) {

        }
        return imageItems;
    }

    static void storeImage(String imageID, String uri) {
    try {
        URI newUri = new URI(uri);
         HttpGet httpget = new HttpGet(newUri);
            HttpResponse response = localHttpClient.execute(httpget);

            HttpEntity resEntity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == 200 && resEntity != null) {
                InputStream inputStream = resEntity.getContent();
                SkeletonConfiguration configuration = new SkeletonConfiguration();
                    BlobStoreFixedHostDiscoverySource blobStoreEndPointOverrides = configuration.getBlobStoreEndPointOverrides();
            BlobStore blobStore = ServicePoolBuilder.create(BlobStore.class)
        .withHostDiscoverySource(blobStoreEndPointOverrides)
        .withServiceFactory(new BlobStoreClientFactory())
        .buildProxy(new RetryNTimes(5, 200, TimeUnit.MILLISECONDS));

                Map<String, String> attrMap = new HashMap<String, String>();
                attrMap.put("image", "image");
                blobStore.put(imageID, inputStream, attrMap, null);
                
            }
        
    } catch (URISyntaxException e) {

    } catch (IOException e) {

    }
    }
}
