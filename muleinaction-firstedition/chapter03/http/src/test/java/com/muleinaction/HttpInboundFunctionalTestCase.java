package com.muleinaction;

import org.mule.api.service.Service;
import org.mule.api.MuleMessage;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.tck.FunctionalTestCase;
import org.mule.module.client.MuleClient;
import org.mule.context.notification.EndpointMessageNotification;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author John D'Emic (john.demic@gmail.com)
 */
public class HttpInboundFunctionalTestCase extends FunctionalTestCase {

    private static String DEST_DIRECTORY = "./data/reports";

    private CountDownLatch latch = new CountDownLatch(1);

    @Override
    protected String getConfigResources() {
        return "conf/http-inbound-config.xml";
    }

    protected void doSetUp() throws Exception {
        super.doSetUp();
        FileUtils.cleanDirectory(new File(DEST_DIRECTORY));
          muleContext.registerListener(new EndpointMessageNotificationListener() {
            public void onNotification(final ServerNotification notification) {
                if ("httpInboundService".equals(notification.getResourceIdentifier())) {
                    final EndpointMessageNotification messageNotification = (EndpointMessageNotification) notification;
                    if (messageNotification.getEndpoint().getName().equals("endpoint.file.data.reports")) {
                        latch.countDown();
                    }
                }
            }
        });
    }

    public void testCorrectlyInitialized() throws Exception {
        final Service service = muleContext.getRegistry().lookupService(
                "httpInboundService");
        assertNotNull(service);
        assertEquals("httpInboundModel", service.getModel().getName());
    }

    public void testMessageSentAndReceived() throws Exception {
        MuleClient muleClient = new MuleClient(muleContext);
        MuleMessage response = muleClient.send("http://localhost:9765/backup-reports",
                REPORT_XML, null);
        assertTrue("Message did not reach directory on time", latch.await(15, TimeUnit.SECONDS));        
        assertEquals(1, FileUtils.listFiles(new File(DEST_DIRECTORY), new WildcardFileFilter("*.xml"), null).size());
        File file = (File) FileUtils.listFiles(new File(DEST_DIRECTORY), new WildcardFileFilter("*.xml"), null).toArray()[0];
        assertEquals(REPORT_XML, FileUtils.readFileToString(file));
    }

    private static String REPORT_XML = "<backup><host>esb01</host></backup>";

}
