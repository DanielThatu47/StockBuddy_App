// src/main/java/com/stockbuddy/service/EmailService.java
package com.stockbuddy.service;

import com.stockbuddy.email.EmailHtmlTemplates;
import com.stockbuddy.model.DemoTradingAccount;
import com.stockbuddy.model.Prediction;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Sends transactional email over SMTP (Jakarta Mail / Angus).
 * HTML uses a dark theme; set {@code app.email.logo-url} to an absolute HTTPS URL in production.
 */
@Service
public class EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailService.class);

	@Value("${spring.mail.host:}")
	private String host;

	@Value("${spring.mail.port:587}")
	private int port;

	@Value("${spring.mail.username:}")
	private String username;

	@Value("${spring.mail.password:}")
	private String password;

	@Value("${app.mail.from:StockBuddy <noreply@localhost>}")
	private String fromAddress;

	/** Absolute URL to your logo (CDN or static host). Leave empty to use text mark only. */
	@Value("${app.email.logo-url:}")
	private String logoUrl;

	/** Link shown in the email footer (e.g. your marketing site or deep link). */
	@Value("${app.email.app-link:}")
	private String appLink;

	public boolean isMailAvailable() {
		return host != null && !host.isBlank();
	}

	private Session buildSession() {
		Properties props = new Properties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.host", host.trim());
		props.put("mail.smtp.port", String.valueOf(port));
		boolean auth = username != null && !username.isBlank();
		props.put("mail.smtp.auth", Boolean.toString(auth));
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.ssl.protocols", "TLSv1.2");

		if (auth) {
			final String user = username.trim();
			final String pass = password != null ? password.replaceAll("\\s+", "") : "";
			return Session.getInstance(props, new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(user, pass);
				}
			});
		}
		return Session.getInstance(props);
	}

	private void sendMultipart(String to, String subject, String plainText, String html) throws Exception {
		Session session = buildSession();
		MimeMessage msg = new MimeMessage(session);
		msg.setFrom(InternetAddress.parse(fromAddress.trim(), false)[0]);
		msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to.trim(), false));
		msg.setSubject(subject, StandardCharsets.UTF_8.name());

		// Plain first, HTML last — multipart/alternative: last part is the preferred rich body (RFC 2046).
		MimeBodyPart textPart = new MimeBodyPart();
		textPart.setText(plainText == null ? "" : plainText, StandardCharsets.UTF_8.name());

		MimeBodyPart htmlPart = new MimeBodyPart();
		// Use subtype "html" so Content-Type is text/html; charset=UTF-8 (better client support than raw setContent).
		htmlPart.setText(html == null ? "" : html, StandardCharsets.UTF_8.name(), "html");

		MimeMultipart alt = new MimeMultipart("alternative");
		alt.addBodyPart(textPart);
		alt.addBodyPart(htmlPart);
		msg.setContent(alt);
		msg.saveChanges();
		Transport.send(msg);
	}

	public void sendOtpEmail(String to, String purpose, String otp) throws Exception {
		if (!isMailAvailable() || to == null || to.isBlank()) {
			return;
		}
		String subject = subjectFor(purpose);
		String plain = EmailHtmlTemplates.otpEmailPlain(purpose, otp);
		String html = EmailHtmlTemplates.otpEmailHtml(logoUrl, appLink, purpose, otp);
		sendMultipart(to, subject, plain, html);
	}

	public void sendPredictionCompletedEmail(String to, Prediction prediction) {
		if (!isMailAvailable() || to == null || to.isBlank() || prediction == null) {
			return;
		}
		try {
			String subject = "StockBuddy — prediction ready for " + prediction.getSymbol();
			String plain = EmailHtmlTemplates.predictionEmailPlain(prediction);
			String html = EmailHtmlTemplates.predictionEmailHtml(logoUrl, appLink, prediction);
			sendMultipart(to, subject, plain, html);
		} catch (Exception e) {
			log.warn("Prediction completion email failed for {}: {}", to, e.getMessage());
		}
	}

	public void sendDemoTradeEmail(String to, String type, String symbol, String companyName,
			int quantity, double price, double totalAmount, DemoTradingAccount accountAfter) {
		if (!isMailAvailable() || to == null || to.isBlank()) {
			return;
		}
		try {
			String subject = "StockBuddy — demo " + type + " · " + symbol;
			String plain = EmailHtmlTemplates.tradeEmailPlain(type, symbol, quantity, price, totalAmount, accountAfter);
			String html = EmailHtmlTemplates.tradeEmailHtml(logoUrl, appLink, type, symbol, companyName,
					quantity, price, totalAmount, accountAfter);
			sendMultipart(to, subject, plain, html);
		} catch (Exception e) {
			log.warn("Demo trade email failed for {}: {}", to, e.getMessage());
		}
	}

	public void sendPortfolioUpdateEmail(String to, DemoTradingAccount account, String headline) {
		if (!isMailAvailable() || to == null || to.isBlank() || account == null) {
			return;
		}
		try {
			String subject = "StockBuddy — portfolio update";
			String plain = EmailHtmlTemplates.portfolioEmailPlain(account, headline);
			String html = EmailHtmlTemplates.portfolioEmailHtml(logoUrl, appLink, account, headline);
			sendMultipart(to, subject, plain, html);
		} catch (Exception e) {
			log.warn("Portfolio email failed for {}: {}", to, e.getMessage());
		}
	}

	private static String subjectFor(String purpose) {
		if ("EMAIL_VERIFY".equals(purpose)) {
			return "StockBuddy — verify your email";
		}
		if ("PASSWORD_RESET".equals(purpose)) {
			return "StockBuddy — reset your password";
		}
		if ("2FA_ENABLE".equals(purpose) || "LOGIN_2FA".equals(purpose)) {
			return "StockBuddy — two-factor code";
		}
		return "StockBuddy — your security code";
	}
}
