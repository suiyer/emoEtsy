package com.bazaarvoice.emodb.examples.skeleton;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class FeedbackDownloader {
    static HttpClient httpClient = new DefaultHttpClient();
    public static void main(String args[]) {
        try {
                    URI uri = new URI("http://openapi.etsy.com/v2/users/ArtsySandra/feedback/as-seller?api_key=lb1p6bet8gloxrbusmbifwno");
                    HttpGet httpget = new HttpGet(uri);
                    HttpResponse response = httpClient.execute(httpget);

                    HttpEntity resEntity = response.getEntity();
                    if (response.getStatusLine().getStatusCode() == 200 && resEntity != null) {


                       ObjectMapper mapper = new ObjectMapper();

               InputStream instream = resEntity.getContent();

               String data = mapper.readValue(instream, String.class);


                    }
                } catch (IOException ioe) {

                    throw new RuntimeException("The catalog is not available at this time.", ioe);
                } catch (URISyntaxException urise) {

                    throw new RuntimeException("The catalog is not available at this time.", urise);
                }

    }
}
