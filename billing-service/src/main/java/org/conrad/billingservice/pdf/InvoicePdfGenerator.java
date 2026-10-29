package org.conrad.billingservice.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.conrad.billingservice.model.Invoice;
import org.conrad.billingservice.model.InvoiceCategory;
import org.conrad.billingservice.model.InvoiceLine;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Builds a printable PDF version of an invoice. Only formatting lives here — no database or HTTP calls. */
@Component
public class InvoicePdfGenerator {

    private static final Locale NORWEGIAN = Locale.forLanguageTag("nb-NO");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
    private static final Font HEADING = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 11);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

    public byte[] generate(Invoice invoice, String residentName, String apartmentNumber) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph("FAKTURA", TITLE));
        document.add(new Paragraph(" "));

        // Who the invoice is for, and the key details
        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.addCell(infoCell("Til:", residentName));
        info.addCell(infoCell("Fakturanummer:", String.valueOf(invoice.getId())));
        info.addCell(infoCell("Leilighet:", apartmentNumber));
        info.addCell(infoCell("Periode:", invoice.getPeriod()));
        info.addCell(infoCell("Status:", statusText(invoice)));
        info.addCell(infoCell("Forfallsdato:", invoice.getDueDate().format(DATE)));
        document.add(info);
        document.add(new Paragraph(" "));

        // One row per invoice line, then the total
        PdfPTable lines = new PdfPTable(new float[]{5, 3, 2});
        lines.setWidthPercentage(100);
        lines.addCell(headerCell("Beskrivelse", Element.ALIGN_LEFT));
        lines.addCell(headerCell("Kategori", Element.ALIGN_LEFT));
        lines.addCell(headerCell("Beløp", Element.ALIGN_RIGHT));

        for (InvoiceLine line : invoice.getLines()) {
            lines.addCell(bodyCell(line.getDescription(), Element.ALIGN_LEFT));
            lines.addCell(bodyCell(categoryText(line.getCategory()), Element.ALIGN_LEFT));
            lines.addCell(bodyCell(money(line.getAmount()), Element.ALIGN_RIGHT));
        }

        PdfPCell totalLabel = new PdfPCell(new Phrase("Totalt å betale", HEADING));
        totalLabel.setColspan(2);
        totalLabel.setBorder(Rectangle.TOP);
        totalLabel.setPadding(6);
        lines.addCell(totalLabel);
        PdfPCell totalAmount = new PdfPCell(new Phrase(money(invoice.getTotalAmount()), HEADING));
        totalAmount.setBorder(Rectangle.TOP);
        totalAmount.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalAmount.setPadding(6);
        lines.addCell(totalAmount);

        document.add(lines);
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Beløpet betales innen forfallsdato. Ved spørsmål, kontakt styret.", SMALL));

        document.close();
        return out.toByteArray();
    }

    private PdfPCell infoCell(String label, String value) {
        Phrase phrase = new Phrase();
        phrase.add(new Phrase(label + " ", HEADING));
        phrase.add(new Phrase(value, NORMAL));
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(4);
        return cell;
    }

    private PdfPCell headerCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADING));
        cell.setBackgroundColor(new Color(235, 235, 235));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell bodyCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(Color.LIGHT_GRAY);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6);
        return cell;
    }

    private String money(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(NORWEGIAN).format(amount);
    }

    private String statusText(Invoice invoice) {
        return switch (invoice.getEffectiveStatus()) {
            case PAID -> "Betalt";
            case PENDING -> "Ikke betalt";
            case OVERDUE -> "Forfalt";
        };
    }

    private String categoryText(InvoiceCategory category) {
        return switch (category) {
            case SHARED_COST -> "Felleskostnader";
            case WATER -> "Vann";
            case ELECTRICITY -> "Strøm";
            case PARKING -> "Parkering";
            case OTHER -> "Annet";
        };
    }
}