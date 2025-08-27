package com.example.email;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

public class SendGridService {
    private final String apiKey;
    private final String fromEmail;

    public SendGridService(String apiKey, String fromEmail) {
        this.apiKey = Objects.requireNonNull(apiKey, "SendGrid API key must not be null");
        this.fromEmail = Objects.requireNonNull(fromEmail, "From email must not be null");
    }

    public void sendEmail(Collection<String> recipients, String subject, String htmlBody) throws IOException {
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("Recipients must not be empty");
        }
        Email from = new Email(fromEmail);
        Content content = new Content("text/html", htmlBody);
        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setSubject(subject);
        mail.addContent(content);

        Personalization personalization = new Personalization();
        for (String toEmail : recipients) {
            personalization.addTo(new Email(toEmail));
        }
        mail.addPersonalization(personalization);

        SendGrid sg = new SendGrid(apiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            int status = response.getStatusCode();
            if (status >= 400) {
                throw new IOException("SendGrid error: status=" + status + ", body=" + response.getBody());
            }
        } catch (IOException e) {
            throw e;
        }
    }
}


