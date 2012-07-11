package com.bazaarvoice.emodb.examples.skeleton;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

/** This class pulls data from the etsy api and puts it into SOR **/

public class EtsyDataIngestor {

    private static ObjectMapper jsonMapper = new ObjectMapper();
    private static HttpClient httpClient = new DefaultHttpClient();

    public static void main(String[] args) {
        //get listings from etsy
        Iterator<JsonNode> listings = getEtsyListings();

        //put listings into SOR
        for ( ; listings.hasNext(); ) {
            storeListing(listings.next());
        }


    }

    public static void storeListing(JsonNode listing) {
        String path = "/listing/sor/" + listing.get("listing_id");
        System.out.println(path);

        URI newUri = null;
        try {
            newUri = new URI("http", null, "localhost", 10090, path, null, null);
            HttpPost post = new HttpPost(newUri);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(listing.toString(), "application/json", "UTF-8"));
            HttpResponse response = httpClient.execute(post);

            EntityUtils.consume(response.getEntity());
        } catch (URISyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClientProtocolException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }

    public static Iterator<JsonNode> getEtsyListings() {
        try {
            URI uri = new URI("http://openapi.etsy.com/v2/listings/active?api_key=lb1p6bet8gloxrbusmbifwno");
            HttpGet httpget = new HttpGet(uri);
            HttpResponse response = httpClient.execute(httpget);

            HttpEntity resEntity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == 200 && resEntity != null) {
                InputStream instream = resEntity.getContent();

                ObjectNode data = jsonMapper.readValue(instream, ObjectNode.class);
                ArrayNode listings = (ArrayNode) data.get("results");
                return listings.getElements();
            }
        } catch (IOException ioe) {

            throw new RuntimeException("The catalog is not available at this time.", ioe);
        } catch (URISyntaxException urise) {

            throw new RuntimeException("The catalog is not available at this time.", urise);
        }

        return null;
    }
}
