package vn.com.lcx.common.mail;

import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import vn.com.lcx.common.constant.CommonConstant;
import vn.com.lcx.common.utils.ExceptionUtils;
import vn.com.lcx.common.utils.LogUtils;

import javax.activation.DataHandler;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MailHelper {

    private MailHelper() {
    }

    public static Map<String, String> sendHTMLEmail(final MailProperties mailProperties) {
        if (
                mailProperties == null ||
                        StringUtils.isBlank(mailProperties.getHost()) ||
                        StringUtils.isBlank(mailProperties.getPort()) ||
                        StringUtils.isBlank(mailProperties.getUsername()) ||
                        StringUtils.isBlank(mailProperties.getPassword()) ||
                        mailProperties.getMailSendingMethod() == null ||
                        CollectionUtils.isEmpty(mailProperties.getEmailInfos())
        ) {
            throw new MailPropertiesEmptyError("Mail properties empty" + (mailProperties != null ? mailProperties.toString() : ""));
        }
        val resultMap = new HashMap<String, String>();
        val properties = System.getProperties();
        switch (mailProperties.getMailSendingMethod()) {
            case LIVE:
                properties.setProperty("mail.smtp.host", mailProperties.getHost());
                properties.setProperty("mail.smtp.auth", "true");
                properties.setProperty("mail.smtp.starttls.enable", "true");
                properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                properties.setProperty("mail.smtp.port", mailProperties.getPort());
                properties.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");

                // properties.setProperty("mail.smtp.host", mailProperties.getHost());
                // properties.setProperty("mail.smtp.auth", "true");
                // properties.setProperty("mail.smtp.starttls.enable", "true");
                // properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                // properties.setProperty("mail.smtp.port", mailProperties.getPort());
                // properties.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");
                break;
            case UAT:
            case LIVE_NO_TRUST:
                properties.setProperty("mail.smtp.host", mailProperties.getHost());
                properties.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");
                properties.setProperty("mail.smtp.auth", "true");
                properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                properties.setProperty("mail.smtp.port", mailProperties.getPort());

                // properties.setProperty("mail.smtp.auth", "true");
                // properties.setProperty("mail.smtp.starttls.enable", "true");
                // properties.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");
                // properties.setProperty("mail.smtp.host", mailProperties.getHost());
                // properties.setProperty("mail.smtp.port", mailProperties.getPort());
                // properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                // properties.setProperty("mail.smtp.ssl.trust", "*"); // Trust any SSL certificate
                disableSslVerification();
                break;
        }
        val session = Session.getInstance(
                properties,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(mailProperties.getUsername(), mailProperties.getPassword());
                    }
                }
        );
        try {
            Transport transport = session.getTransport("smtp");
            transport.connect();
            val mailInfos = mailProperties.getEmailInfos();
            for (val mailInfo : mailInfos) {
                try {
                    val message = new MimeMessage(session);
                    message.setFrom(mailProperties.getUsername());

                    val toAddresses = new InternetAddress[mailInfo.getToUsers().size()];
                    List<String> toUsers = mailInfo.getToUsers();
                    for (int i = 0; i < toUsers.size(); i++) {
                        String toUser = toUsers.get(i);
                        toAddresses[i] = new InternetAddress(toUser);
                    }

                    message.setRecipients(Message.RecipientType.TO, toAddresses);
                    List<String> ccUsers = new ArrayList<>();
                    if (CollectionUtils.isNotEmpty(mailInfo.getCcUsers())) {
                        val ccAddresses = new InternetAddress[mailInfo.getCcUsers().size()];
                        ccUsers.addAll(mailInfo.getCcUsers());
                        for (int i = 0; i < ccUsers.size(); i++) {
                            String ccUser = ccUsers.get(i);
                            ccAddresses[i] = new InternetAddress(ccUser);
                        }
                        message.setRecipients(Message.RecipientType.CC, ccAddresses);
                    }
                    List<String> bccUsers = new ArrayList<>();
                    if (CollectionUtils.isNotEmpty(mailInfo.getBccUser())) {
                        val bccAddresses = new InternetAddress[mailInfo.getBccUser().size()];
                        bccUsers.addAll(mailInfo.getBccUser());
                        for (int i = 0; i < bccUsers.size(); i++) {
                            String bccUser = bccUsers.get(i);
                            bccAddresses[i] = new InternetAddress(bccUser);
                        }
                        message.setRecipients(Message.RecipientType.CC, bccAddresses);
                    }

                    message.setSentDate(new Date());

                    message.setSubject(mailInfo.getSubject(), CommonConstant.UTF_8_STANDARD_CHARSET);

                    val multipart = new MimeMultipart("related");
                    val mimeBodyPart = new MimeBodyPart();
                    mimeBodyPart.setDataHandler(
                            new DataHandler(
                                    new ByteArrayDataSource(
                                            mailInfo.getBody(),
                                            "text/html; charset=utf-8"
                                    )
                            )
                    );

                    multipart.addBodyPart(mimeBodyPart);
                    message.setContent(multipart);
                    message.saveChanges();
                    LogUtils.writeLog(
                            LogUtils.Level.INFO,
                            String.format(
                                    "\nStart to send email with information:" +
                                            "\n    - from email: %s" +
                                            "\n    - to email: %s" +
                                            "\n    - cc email: %s" +
                                            "\n    - bcc email: %s" +
                                            "\n    - subject: %s" +
                                            "\n    - content: %s",
                                    mailProperties.getUsername(),
                                    String.join(", ", toUsers),
                                    String.join(", ", ccUsers),
                                    String.join(", ", bccUsers),
                                    mailInfo.getSubject(),
                                    mailInfo.getBody()
                            )
                    );
                    transport.sendMessage(message, message.getAllRecipients());
                    resultMap.put(mailInfo.getId(), "SUCCESS");
                } catch (Throwable e) {
                    val stackTrace = ExceptionUtils.getStackTrace(e);
                    resultMap.put(
                            mailInfo.getId(),
                            StringUtils.isBlank(stackTrace) ?
                                    "Error" :
                                    stackTrace.length() > 3500 ?
                                            stackTrace.substring(0, 3500) : stackTrace
                    );
                }
            }
            transport.close();
        } catch (Throwable e) {
            // throw new MailSendingError(e);
            LogUtils.writeLog(e.getMessage(), e);
        }
        return resultMap;
    }

    private static void disableSslVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Set a default hostname verifier to trust any host
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            LogUtils.writeLog(e.getMessage(), e);
        }
    }

}
