package org.oasis_open.contextserver.itests;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;

import java.io.File;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

/**
 * Base class for integration tests.
 * 
 * @author kevan
 */
public abstract class BaseTest {
    
    protected static final String HTTP_PORT = "8181";
    
    protected static final String URL = "http://localhost:" + HTTP_PORT;

    @Configuration
    public Option[] config() {
        MavenArtifactUrlReference karafUrl = maven()
                .groupId("org.apache.karaf")
                .artifactId("apache-karaf")
                .version("3.0.2")
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
        MavenUrlReference karafCxfRepo = maven()
                .groupId("org.apache.cxf.karaf")
                .artifactId("apache-cxf")
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
                debugConfiguration("5005", false),
                karafDistributionConfiguration()
                        .frameworkUrl(karafUrl)
                        .unpackDirectory(new File("target/exam"))
                        .useDeployFolder(false),
//                keepRuntimeFolder(),
                configureConsole().ignoreLocalConsole().ignoreRemoteShell(),
                logLevel(LogLevel.INFO),
//                editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", HTTP_PORT),
//                systemProperty("org.osgi.service.http.port").value(HTTP_PORT),
                features(karafPaxWebRepo, "war"),
                features(karafCxfRepo, "cxf"),
                features(karafStandardRepo, "openwebbeans"),
                features(karafStandardRepo, "pax-cdi-web-openwebbeans"),
                features(contextServerRepo, "context-server-kar"),
                // we need to wrap the HttpComponents libraries ourselves since the OSGi bundles provided by the project are incorrect
                wrappedBundle(mavenBundle("org.apache.httpcomponents",
                        "httpcore").versionAsInProject()),
                wrappedBundle(mavenBundle("org.apache.httpcomponents",
                        "httpmime").versionAsInProject()),
                wrappedBundle(mavenBundle("org.apache.httpcomponents",
                        "httpclient").versionAsInProject())
        };
    }
}
