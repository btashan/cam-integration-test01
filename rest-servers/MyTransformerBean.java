package com.ipaas.test;

public class MyTransformerBean {

    /**
     * Camel bu metodu otomatik olarak algılar ve gelen mesaj gövdesini 
     * 'body' parametresine enjekte eder.
     */
    public String processPayload(String body) {
        if (body == null || body.trim().isEmpty()) {
            body = "Boş Veri";
        }
        // Gelen veriyi işleyip XML formatında süsleyerek geri döner
        return "<processed-data>\n" +
               "  <status>SUCCESS</status>\n" +
               "  <original-payload>" + body + "</original-payload>\n" +
               "  <timestamp>" + java.time.Instant.now() + "</timestamp>\n" +
               "</processed-data>";
    }
}
