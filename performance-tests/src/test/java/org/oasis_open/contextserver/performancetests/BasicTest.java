package org.oasis_open.contextserver.performancetests;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.carrotsearch.junitbenchmarks.WriterConsumer;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.oasis_open.contextserver.api.Metadata;
import org.oasis_open.contextserver.api.services.SegmentService;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BasicTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(BasicTest.class);

    /**
     * Enables the benchmark rule.
     */
    @Rule
    public TestRule getBenchmarkRun() {
        try {
            File benchmarks = new File("../../benchmarks");
            benchmarks.mkdirs();
            return new BenchmarkRule(new WriterConsumer(new FileWriter(new File(benchmarks,"benchmark.txt"),true)));
        } catch (IOException e) {
            LOGGER.error("Cannot get benchamrks",e);
        }
        return null;
    }

    @Inject
    protected SegmentService segmentService;

    @Configuration
    public Option[] config() {
        MavenArtifactUrlReference karafUrl = maven()
                .groupId("org.apache.karaf")
                .artifactId("apache-karaf")
                .version("3.0.1")
                .type("tar.gz");

        MavenUrlReference karafStandardRepo = maven()
                .groupId("org.apache.karaf.features")
                .artifactId("standard")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
        MavenUrlReference karafPaxWebRepo = maven()
                .groupId("org.ops4j.pax.web")
                .artifactId("pax-web-features")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
        MavenUrlReference karafSpringRepo = maven()
                .groupId("org.apache.karaf.features")
                .artifactId("spring")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
        MavenUrlReference karafCxfRepo = maven()
                .groupId("org.apache.cxf.karaf")
                .artifactId("apache-cxf")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
        MavenUrlReference karafEnterpriseRepo = maven()
                .groupId("org.apache.karaf.features")
                .artifactId("enterprise")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
        MavenUrlReference contextServerRepo = maven()
                .groupId("org.oasis-open.contextserver")
                .artifactId("context-server-kar")
                .classifier("features")
                .type("xml")
                .versionAsInProject();

        return new Option[]{
                KarafDistributionOption.debugConfiguration("5005", false),
                karafDistributionConfiguration()
                        .frameworkUrl(karafUrl)
                        .unpackDirectory(new File("target/exam"))
                        .useDeployFolder(false),
                keepRuntimeFolder(),
                KarafDistributionOption.features(karafPaxWebRepo, "war"),
                KarafDistributionOption.features(karafCxfRepo, "cxf"),
                KarafDistributionOption.features(karafStandardRepo, "openwebbeans"),
                KarafDistributionOption.features(karafStandardRepo, "pax-cdi-web-openwebbeans"),
                KarafDistributionOption.features(contextServerRepo, "context-server-kar"),
                // we need to wrap the HttpComponents libraries ourselves since the OSGi bundles provided by the project are incorrect
                wrappedBundle(mavenBundle("org.apache.httpcomponents",
                        "httpcore").versionAsInProject()),
                wrappedBundle(mavenBundle("org.apache.httpcomponents",
                        "httpmime").versionAsInProject()),
                wrappedBundle(mavenBundle("org.apache.httpcomponents",
                        "httpclient").versionAsInProject()),
                wrappedBundle(mavenBundle("com.carrotsearch",
                        "junit-benchmarks", "0.7.2")),
//                wrappedBundle(mavenBundle("com.h2database",
//                        "h2", "1.4.181"))
        };
    }

    @BenchmarkOptions(benchmarkRounds = 100, warmupRounds = 0, concurrency = 10)
    @Test
    public void testContext() throws IOException {

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://localhost:8181/context.js");
        CloseableHttpResponse response = httpclient.execute(httpGet);
        // The underlying HTTP connection is still held by the response object
        // to allow the response content to be streamed directly from the network socket.
        // In order to ensure correct deallocation of system resources
        // the user MUST call CloseableHttpResponse#close() from a finally clause.
        // Please note that if response content is not fully consumed the underlying
        // connection cannot be safely re-used and will be shut down and discarded
        // by the connection manager.
        String responseContent = null;
        try {
            HttpEntity entity = response.getEntity();
            // do something useful with the response body
            // and ensure it is fully consumed
            responseContent = EntityUtils.toString(entity);
        } finally {
            response.close();
        }
    }

    @BenchmarkOptions(benchmarkRounds = 100, warmupRounds = 0, concurrency = 10)
    @Test
    public void testSegments() {
        Assert.assertNotNull("Segment service should be available", segmentService);
        Set<Metadata> segmentMetadatas = segmentService.getSegmentMetadatas();
        Assert.assertNotEquals("Segment metadata list should not be empty", 0, segmentMetadatas.size());
        LOGGER.info("Retrieved " + segmentMetadatas.size() + " segment metadata entries");
    }



}
