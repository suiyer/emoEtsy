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

public class FeedbackDownloader {
    static HttpClient localHttpClient = new DefaultHttpClient();
    static HttpClient etsyHttpClient = new DefaultHttpClient();

    public static void main(String args[]) {

        URI newUri = null;
        String path = "/listing/es/all";
        try {
            newUri = new URI("http", null, "localhost", 10090, path, null, null);
            HttpGet httpget = new HttpGet(newUri);
            HttpResponse response = localHttpClient.execute(httpget);

            HttpEntity resEntity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == 200 && resEntity != null) {


                ObjectMapper mapper = new ObjectMapper();

                InputStream instream = resEntity.getContent();

                ArrayNode data = mapper.readValue(instream, ArrayNode.class);
                Iterator<JsonNode> listings = data.getElements();
                for (; listings.hasNext(); ) {
                    JsonNode listing = listings.next();
                    String userID = listing.get("user_id").asText();


                    URI uri = new URI("http", null, "openapi.etsy.com", 80, "/v2/users/" + userID + "/feedback/as-seller", "api_key=lb1p6bet8gloxrbusmbifwno", null);
                    httpget = new HttpGet(uri);
                    response = etsyHttpClient.execute(httpget);

                    resEntity = response.getEntity();
                    if (response.getStatusLine().getStatusCode() == 200 && resEntity != null) {


                        ObjectMapper feedbackMapper = new ObjectMapper();

                        InputStream feedbackstream = resEntity.getContent();

                        ObjectNode feedbackdata = feedbackMapper.readValue(feedbackstream, ObjectNode.class);
                        ArrayNode feedback = (ArrayNode) feedbackdata.get("results");
                        Iterator<JsonNode> feedbackItems = feedback.getElements();
                        for (; feedbackItems.hasNext(); ) {
                            storeFeedback(feedbackItems.next());
                        }

                    }
                }
            }
        } catch (IOException ioe) {

            throw new RuntimeException("The catalog is not available at this time.", ioe);
        } catch (URISyntaxException urise) {

            throw new RuntimeException("The catalog is not available at this time.", urise);
        }

    }

    public static void storeFeedback(JsonNode feedback) {
        String path = "/feedback/sor/" + feedback.get("feedback_id");
        System.out.println(path);

        URI newUri = null;
        try {
            newUri = new URI("http", null, "localhost", 10090, path, null, null);
            HttpPost post = new HttpPost(newUri);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(feedback.toString(), "application/json", "UTF-8"));
            HttpResponse response = localHttpClient.execute(post);

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
}
