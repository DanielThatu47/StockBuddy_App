// src/main/java/com/stockbuddy/email/EmailHtmlTemplates.java
package com.stockbuddy.email;

import com.stockbuddy.model.DemoTradingAccount;
import com.stockbuddy.model.Holding;
import com.stockbuddy.model.Prediction;
import com.stockbuddy.model.PredictionPoint;
import com.stockbuddy.model.Sentiment;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Responsive, table-based HTML emails styled for a dark theme (matches typical StockBuddy UI).
 * Logo URL should be an absolute HTTPS URL in production (e.g. CDN); optional for local dev.
 */
public final class EmailHtmlTemplates {

	private EmailHtmlTemplates() {
	}

	public static String escape(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;");
	}

	private static String nfMoney(double v) {
		NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
		return nf.format(v);
	}

	private static String logoBlock(String logoUrl) {
		if (logoUrl != null && !logoUrl.isBlank()) {
			String src = escape(logoUrl.trim());
			return "<img src=\"" + src + "\" alt=\"StockBuddy\" height=\"40\" style=\"display:block;border:0;outline:none;text-decoration:none;max-width:200px;height:auto;\" />";
		}
		return "<span style=\"font-size:22px;font-weight:700;letter-spacing:0.06em;color:#e6edf3;\">StockBuddy</span>";
	}

	private static String layout(String logoUrl, String appLink, String preheader, String inner) {
		String link = (appLink != null && !appLink.isBlank()) ? escape(appLink.trim()) : "#";
		String pre = escape(preheader);
		return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\" /><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\" />"
				+ "<meta name=\"color-scheme\" content=\"dark\" /><meta name=\"supported-color-schemes\" content=\"dark\" />"
				+ "<title>StockBuddy</title></head><body style=\"margin:0;padding:0;background:#0d1117;\">"
				+ "<span style=\"display:none;font-size:1px;color:#0d1117;line-height:1px;max-height:0;max-width:0;opacity:0;overflow:hidden;\">"
				+ pre + "</span>"
				+ "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background:#0d1117;padding:24px 12px;\">"
				+ "<tr><td align=\"center\">"
				+ "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:560px;background:#161b22;border:1px solid #30363d;border-radius:12px;overflow:hidden;\">"
				+ "<tr><td style=\"padding:24px 28px 8px 28px;border-bottom:1px solid #30363d;\">" + logoBlock(logoUrl) + "</td></tr>"
				+ "<tr><td style=\"padding:28px 28px 32px 28px;color:#e6edf3;font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;font-size:15px;line-height:1.55;\">"
				+ inner
				+ "<p style=\"margin:28px 0 0 0;font-size:13px;color:#8b949e;\">You are receiving this because of activity on your StockBuddy account."
				+ " If this was not you, secure your account and contact support.</p>"
				+ "<p style=\"margin:12px 0 0 0;font-size:13px;\"><a href=\"" + link + "\" style=\"color:#58a6ff;text-decoration:none;\">Open StockBuddy</a></p>"
				+ "</td></tr></table></td></tr></table></body></html>";
	}

	private static String pill(String text, String bg, String fg) {
		return "<span style=\"display:inline-block;padding:6px 12px;border-radius:999px;font-size:12px;font-weight:600;background:"
				+ bg + ";color:" + fg + ";\">" + escape(text) + "</span>";
	}

	private static String otpDigits(String otp) {
		StringBuilder sb = new StringBuilder();
		String o = escape(otp);
		for (int i = 0; i < o.length(); i++) {
			char c = o.charAt(i);
			sb.append("<span style=\"display:inline-block;min-width:36px;text-align:center;margin:0 4px;padding:12px 0;border-radius:8px;"
					+ "background:#21262d;border:1px solid #30363d;font-size:22px;font-weight:700;letter-spacing:0.08em;color:#58a6ff;\">")
					.append(c).append("</span>");
		}
		return sb.toString();
	}

	public static String otpEmailHtml(String logoUrl, String appLink, String purpose, String otp) {
		String headline;
		String sub;
		String expiry;
		String pillLabel;
		String pillBg;
		String pillFg;
		if ("EMAIL_VERIFY".equals(purpose)) {
			headline = "Verify your email";
			sub = "Enter this code in the StockBuddy app to confirm your email address.";
			expiry = "This code expires in <strong>5 minutes</strong>. If you did not request this, you can ignore this email.";
			pillLabel = "Email verification";
			pillBg = "rgba(88,166,255,0.18)";
			pillFg = "#58a6ff";
		} else if ("PASSWORD_RESET".equals(purpose)) {
			headline = "Reset your password";
			sub = "Use this one-time code in the StockBuddy app to set a new password.";
			expiry = "This code expires in <strong>15 minutes</strong>. If you did not request a reset, ignore this message.";
			pillLabel = "Password reset";
			pillBg = "rgba(240,180,60,0.2)";
			pillFg = "#f0b848";
		} else if ("2FA_ENABLE".equals(purpose) || "LOGIN_2FA".equals(purpose)) {
			headline = "Two-factor security code";
			sub = "Complete sign-in or two-factor setup in the StockBuddy app with the code below.";
			expiry = "This code expires in <strong>5 minutes</strong>. Never share this code with anyone.";
			pillLabel = "Two-factor authentication";
			pillBg = "rgba(63,185,80,0.2)";
			pillFg = "#3fb950";
		} else {
			headline = "Your security code";
			sub = "Use this code to continue in the StockBuddy app.";
			expiry = "This code expires shortly. Do not share it with anyone.";
			pillLabel = "Security";
			pillBg = "#21262d";
			pillFg = "#8b949e";
		}

		String codeMono = escape(otp);
		String inner = "<p style=\"margin:0 0 12px 0;\">"
				+ "<span style=\"display:inline-block;padding:6px 14px;border-radius:999px;font-size:12px;font-weight:700;"
				+ "letter-spacing:0.04em;background:" + pillBg + ";color:" + pillFg + ";\">" + escape(pillLabel) + "</span></p>"
				+ "<h1 style=\"margin:0 0 12px 0;font-size:24px;font-weight:700;color:#e6edf3;letter-spacing:-0.02em;\">"
				+ escape(headline) + "</h1>"
				+ "<p style=\"margin:0 0 22px 0;color:#8b949e;font-size:15px;line-height:1.55;\">" + escape(sub) + "</p>"
				+ "<div style=\"margin:0 0 20px 0;padding:22px 18px;border-radius:14px;border:1px solid #30363d;"
				+ "background:linear-gradient(180deg,#1c2128 0%,#161b22 100%);text-align:center;\">"
				+ "<p style=\"margin:0 0 14px 0;font-size:11px;text-transform:uppercase;letter-spacing:0.14em;color:#8b949e;\">Your code</p>"
				+ "<div style=\"margin:0 0 16px 0;\">" + otpDigits(otp) + "</div>"
				+ "<p style=\"margin:0;padding:12px 14px;border-radius:8px;background:#0d1117;border:1px dashed #30363d;"
				+ "font-family:ui-monospace,SFMono-Regular,Menlo,Monaco,Consolas,monospace;font-size:20px;font-weight:700;"
				+ "letter-spacing:0.35em;color:#58a6ff;\">" + codeMono + "</p></div>"
				+ "<p style=\"margin:0 0 18px 0;color:#8b949e;font-size:14px;line-height:1.5;\">" + expiry + "</p>"
				+ "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"border-radius:10px;"
				+ "background:#21262d;border:1px solid #30363d;\"><tr><td style=\"padding:14px 16px;\">"
				+ "<p style=\"margin:0;font-size:13px;color:#8b949e;line-height:1.45;\"><strong style=\"color:#e6edf3;\">Security tip:</strong> "
				+ "StockBuddy will never ask for this code by phone or email. Only enter it in the official app.</p>"
				+ "</td></tr></table>";
		return layout(logoUrl, appLink, headline + " — StockBuddy", inner);
	}

	public static String otpEmailPlain(String purpose, String otp) {
		if ("EMAIL_VERIFY".equals(purpose)) {
			return "Verify your email on StockBuddy.\n\nCode: " + otp + "\n\nExpires in 5 minutes.";
		}
		if ("PASSWORD_RESET".equals(purpose)) {
			return "Reset your StockBuddy password.\n\nCode: " + otp + "\n\nExpires in 15 minutes.";
		}
		return "Your StockBuddy security code:\n\n" + otp + "\n\nExpires in 5 minutes.";
	}

	public static String predictionEmailHtml(String logoUrl, String appLink, Prediction p) {
		String sym = escape(p.getSymbol());
		StringBuilder rows = new StringBuilder();
		List<PredictionPoint> pts = p.getPredictions();
		if (pts != null) {
			int max = Math.min(pts.size(), 14);
			for (int i = 0; i < max; i++) {
				PredictionPoint pt = pts.get(i);
				rows.append("<tr><td style=\"padding:10px 12px;border-bottom:1px solid #30363d;color:#8b949e;\">")
						.append(escape(pt.getDate()))
						.append("</td><td style=\"padding:10px 12px;border-bottom:1px solid #30363d;text-align:right;font-weight:600;color:#e6edf3;\">")
						.append(nfMoney(pt.getPrice()))
						.append("</td></tr>");
			}
			if (pts.size() > max) {
				rows.append("<tr><td colspan=\"2\" style=\"padding:10px 12px;color:#8b949e;font-size:13px;\">… and ")
						.append(pts.size() - max).append(" more points in the app.</td></tr>");
			}
		}

		String sentimentBlock = "";
		Sentiment s = p.getSentiment();
		if (s != null && (s.getSummary() != null && !s.getSummary().isBlank())) {
			sentimentBlock = "<div style=\"margin-top:20px;padding:16px;border-radius:10px;background:#21262d;border:1px solid #30363d;\">"
					+ "<p style=\"margin:0 0 8px 0;font-size:12px;text-transform:uppercase;letter-spacing:0.08em;color:#8b949e;\">Market sentiment</p>"
					+ "<p style=\"margin:0;color:#e6edf3;font-size:14px;\">" + escape(s.getSummary()) + "</p></div>";
		}

		String inner = "<p style=\"margin:0 0 8px 0;\">" + pill("Prediction ready", "#1f3a5f", "#58a6ff") + "</p>"
				+ "<h1 style=\"margin:12px 0 16px 0;font-size:22px;font-weight:700;color:#e6edf3;\">"
				+ "Forecast for <span style=\"color:#58a6ff;\">" + sym + "</span></h1>"
				+ "<p style=\"margin:0 0 16px 0;color:#8b949e;\">Your "
				+ p.getDaysAhead() + "-day model run finished. Here is a sample of projected prices.</p>"
				+ "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"border:1px solid #30363d;border-radius:10px;overflow:hidden;\">"
				+ "<tr style=\"background:#21262d;\"><th align=\"left\" style=\"padding:10px 12px;font-size:12px;color:#8b949e;\">Date</th>"
				+ "<th align=\"right\" style=\"padding:10px 12px;font-size:12px;color:#8b949e;\">Price</th></tr>"
				+ rows + "</table>"
				+ sentimentBlock;
		return layout(logoUrl, appLink, "Prediction ready for " + p.getSymbol(), inner);
	}

	public static String predictionEmailPlain(Prediction p) {
		StringBuilder b = new StringBuilder();
		b.append("StockBuddy — prediction ready for ").append(p.getSymbol()).append("\n\n");
		if (p.getSentiment() != null && p.getSentiment().getSummary() != null) {
			b.append("Sentiment: ").append(p.getSentiment().getSummary()).append("\n\n");
		}
		b.append("Open the app for the full chart and details.\n");
		return b.toString();
	}

	public static String tradeEmailHtml(String logoUrl, String appLink, String type, String symbol, String companyName,
			int quantity, double price, double totalAmount, DemoTradingAccount after) {
		String badge = "BUY".equalsIgnoreCase(type)
				? pill("Buy", "rgba(63,185,80,0.2)", "#3fb950")
				: pill("Sell", "rgba(248,81,73,0.2)", "#f85149");
		String sym = escape(symbol);
		String co = escape(companyName);
		String inner = "<p style=\"margin:0 0 8px 0;\">" + badge + "</p>"
				+ "<h1 style=\"margin:12px 0 8px 0;font-size:22px;font-weight:700;color:#e6edf3;\">"
				+ "Demo trade executed</h1>"
				+ "<p style=\"margin:0 0 20px 0;color:#8b949e;\">" + co + " <span style=\"color:#e6edf3;\">(" + sym + ")</span></p>"
				+ "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"border:1px solid #30363d;border-radius:10px;\">"
				+ row("Shares", String.valueOf(quantity))
				+ row("Price", nfMoney(price))
				+ row("Total", nfMoney(Math.abs(totalAmount)))
				+ "</table>"
				+ portfolioSnapshotHtml(after);
		return layout(logoUrl, appLink, "Demo " + type + " — " + symbol, inner);
	}

	public static String tradeEmailPlain(String type, String symbol, int quantity, double price, double totalAmount, DemoTradingAccount after) {
		return "StockBuddy demo trade (" + type + ")\n\n"
				+ symbol + " × " + quantity + " @ " + nfMoney(price) + "\n"
				+ "Total: " + nfMoney(Math.abs(totalAmount)) + "\n\n"
				+ portfolioSnapshotPlain(after);
	}

	private static String row(String k, String v) {
		return "<tr><td style=\"padding:12px 16px;border-bottom:1px solid #30363d;color:#8b949e;width:42%;\">"
				+ escape(k) + "</td><td style=\"padding:12px 16px;border-bottom:1px solid #30363d;text-align:right;font-weight:600;color:#e6edf3;\">"
				+ v + "</td></tr>";
	}

	private static String portfolioSnapshotHtml(DemoTradingAccount a) {
		if (a == null) {
			return "";
		}
		int positions = a.getHoldings() != null ? a.getHoldings().size() : 0;
		String inner = "<div style=\"margin-top:22px;padding:16px;border-radius:10px;background:#21262d;border:1px solid #30363d;\">"
				+ "<p style=\"margin:0 0 10px 0;font-size:12px;text-transform:uppercase;letter-spacing:0.08em;color:#8b949e;\">Portfolio snapshot</p>"
				+ "<p style=\"margin:4px 0;color:#e6edf3;\">Cash balance: <strong>" + nfMoney(a.getBalance()) + "</strong></p>"
				+ "<p style=\"margin:4px 0;color:#e6edf3;\">Total equity: <strong>" + nfMoney(a.getEquity()) + "</strong></p>"
				+ "<p style=\"margin:4px 0;color:#8b949e;font-size:13px;\">Open positions: " + positions + "</p></div>";
		return inner;
	}

	private static String portfolioSnapshotPlain(DemoTradingAccount a) {
		if (a == null) {
			return "";
		}
		int positions = a.getHoldings() != null ? a.getHoldings().size() : 0;
		return "Portfolio snapshot\nCash: " + nfMoney(a.getBalance()) + "\nEquity: " + nfMoney(a.getEquity())
				+ "\nPositions: " + positions + "\n";
	}

	/** Standalone portfolio / valuation alert (e.g. after market data refresh). */
	public static String portfolioEmailHtml(String logoUrl, String appLink, DemoTradingAccount a, String headline) {
		String h = (headline != null && !headline.isBlank()) ? escape(headline) : "Your demo portfolio was updated";
		StringBuilder posRows = new StringBuilder();
		if (a.getHoldings() != null) {
			for (Holding hld : a.getHoldings()) {
				if (hld == null) {
					continue;
				}
				posRows.append("<tr><td style=\"padding:10px 12px;border-bottom:1px solid #30363d;color:#e6edf3;\">")
						.append(escape(hld.getSymbol()))
						.append("</td><td style=\"padding:10px 12px;border-bottom:1px solid #30363d;text-align:right;color:#8b949e;\">")
						.append(hld.getQuantity()).append(" sh</td><td style=\"padding:10px 12px;border-bottom:1px solid #30363d;text-align:right;color:#e6edf3;\">")
						.append(nfMoney(hld.getCurrentValue()))
						.append("</td></tr>");
			}
		}
		String table = "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"margin-top:16px;border:1px solid #30363d;border-radius:10px;overflow:hidden;\">"
				+ "<tr style=\"background:#21262d;\"><th align=\"left\" style=\"padding:10px 12px;font-size:12px;color:#8b949e;\">Symbol</th>"
				+ "<th align=\"right\" style=\"padding:10px 12px;font-size:12px;color:#8b949e;\">Qty</th>"
				+ "<th align=\"right\" style=\"padding:10px 12px;font-size:12px;color:#8b949e;\">Value</th></tr>"
				+ posRows + "</table>";

		String inner = "<h1 style=\"margin:0 0 12px 0;font-size:22px;font-weight:700;color:#e6edf3;\">" + h + "</h1>"
				+ portfolioSnapshotHtml(a)
				+ (a.getHoldings() != null && !a.getHoldings().isEmpty() ? table : "");
		return layout(logoUrl, appLink, "Portfolio update — StockBuddy", inner);
	}

	public static String portfolioEmailPlain(DemoTradingAccount a, String headline) {
		String h = (headline != null && !headline.isBlank()) ? headline : "Portfolio update";
		StringBuilder b = new StringBuilder();
		b.append(h).append("\n\n").append(portfolioSnapshotPlain(a));
		if (a.getHoldings() != null) {
			for (Holding hld : a.getHoldings()) {
				if (hld != null) {
					b.append(hld.getSymbol()).append(" ").append(hld.getQuantity()).append(" sh — ")
							.append(nfMoney(hld.getCurrentValue())).append("\n");
				}
			}
		}
		return b.toString();
	}
}
