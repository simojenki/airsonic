package org.airsonic.test.cucumber.steps.api;

import cucumber.api.java8.En;
import org.airsonic.test.cucumber.server.AirsonicServer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class StreamStepDef implements En {

    private CloseableHttpResponse response;
    private CloseableHttpClient client;
    private boolean closed = false;
    private byte[] body;
    private Header[] headers;

    public StreamStepDef(AirsonicServer server) {
        this.client = HttpClientBuilder.create().build();
        Given("Media file (.*) is added", (String mediaFile) -> {
            // TODO fix this
            server.uploadToDefaultMusicFolder(
                    Paths.get(this.getClass().getResource("/blobs/stream/piano").toURI()),
                    "");
        });

        When("A stream request is sent", () -> {
            RequestBuilder builder = RequestBuilder.create("GET").setUri(server.getBaseUri() + "/rest/stream");
            builder.addParameter("id", "2");
            builder.addHeader("Range", "bytes=0-");
            builder.addHeader("Accept", "audio/webm,audio/ogg,audio/wav,audio/*;");
            server.addRestParameters(builder);
            response = client.execute(builder.build());
        });

        Then("The response bytes are equal", () -> {
            ensureResponseRead();

            FileUtils.writeByteArrayToFile(new File("/tmp/bytearray"), body);

            byte[] expected = IOUtils.toByteArray(this.getClass().getResource("/blobs/stream/piano/piano.mp3").toURI());
//
//            HexDump.dump(expected, 0, System.out, 0);

            Assert.assertArrayEquals(expected, body);
        });
        Then("^Dump the headers$", () -> {
            ensureResponseRead();

            System.out.println("Headers:");
            for (Header header : headers) {
                System.out.print(header.getName());
                System.out.print(": ");
                for (HeaderElement element : header.getElements()) {
                    System.out.print(element);
                    System.out.print(", ");
                }
                System.out.println();
            }
        });


    }

    void ensureResponseRead() throws IOException {
        if(closed) {
            return;
        } else {
            this.body = EntityUtils.toByteArray(response.getEntity());
            this.headers = response.getAllHeaders();
            closed = true;
            response.close();
        }
    }

}
