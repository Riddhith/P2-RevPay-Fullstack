package com.revature.revpay.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import com.revature.revpay.model.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportUtil {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static byte[] exportToCsv(List<Transaction> transactions) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(out))) {
            writer.writeNext(
                    new String[] { "TXN ID", "Type", "Amount", "Sender", "Receiver", "Status", "Note", "Date" });
            for (Transaction t : transactions) {
                writer.writeNext(new String[] {
                        String.valueOf(t.getTxnId()),
                        t.getTxnType(),
                        t.getAmount().toPlainString(),
                        t.getSenderName() != null ? t.getSenderName() : "-",
                        t.getReceiverName() != null ? t.getReceiverName() : "-",
                        t.getStatus(),
                        t.getNote() != null ? t.getNote() : "",
                        t.getTxnTimestamp() != null ? t.getTxnTimestamp().format(FMT) : ""
                });
            }
        }
        return out.toByteArray();
    }

    public static byte[] exportToPdf(List<Transaction> transactions) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
        Font cellFont = new Font(Font.FontFamily.HELVETICA, 9);

        Paragraph title = new Paragraph("RevPay - Transaction History", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(15f);
        document.add(title);

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 6, 10, 10, 14, 14, 10, 16, 14 });

        String[] headers = { "TXN ID", "Type", "Amount", "Sender", "Receiver", "Status", "Note", "Date" };
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(new BaseColor(59, 130, 246));
            cell.setPadding(6);
            table.addCell(cell);
        }

        for (Transaction t : transactions) {
            table.addCell(new Phrase(String.valueOf(t.getTxnId()), cellFont));
            table.addCell(new Phrase(t.getTxnType(), cellFont));
            table.addCell(new Phrase("₹" + t.getAmount().toPlainString(), cellFont));
            table.addCell(new Phrase(t.getSenderName() != null ? t.getSenderName() : "-", cellFont));
            table.addCell(new Phrase(t.getReceiverName() != null ? t.getReceiverName() : "-", cellFont));
            table.addCell(new Phrase(t.getStatus(), cellFont));
            table.addCell(new Phrase(t.getNote() != null ? t.getNote() : "", cellFont));
            table.addCell(new Phrase(t.getTxnTimestamp() != null ? t.getTxnTimestamp().format(FMT) : "", cellFont));
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }
}
