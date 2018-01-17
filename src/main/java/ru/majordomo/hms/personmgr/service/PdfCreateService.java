//package ru.majordomo.hms.personmgr.service.Document;
//
//import com.google.common.base.Splitter;
//import com.itextpdf.text.*;
//import com.itextpdf.text.pdf.*;
//import com.itextpdf.text.pdf.draw.LineSeparator;
//import org.apache.commons.lang.WordUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.ApplicationContext;
//import org.springframework.core.io.Resource;
//import org.springframework.stereotype.Service;
//import ru.majordomo.hms.personmgr.service.AccountHelper;
//
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.Arrays;
//import java.util.List;
//
//import static com.itextpdf.text.Element.*;
//import static ru.majordomo.hms.personmgr.common.Utils.getMonthName;
//
//@Service
//public class PdfCreateService {
//    private Font FONT_14;
//    private Font FONT_8;
//    private Font FONT_8_BOLD;
//    private Font FONT_12_BOLD;
//
//    private final static Logger logger = LoggerFactory.getLogger(PdfCreateService.class);
//
//    private final AccountHelper accountHelper;
//    private final ApplicationContext applicationContext;
//
//    @Autowired
//    public PdfCreateService(
//            AccountHelper accountHelper,
//            ApplicationContext applicationContext
//    ) {
//        this.accountHelper = accountHelper;
//        this.applicationContext = applicationContext;
//
//        try {
//            String FONT_PATH = "fonts/DejaVuSans.ttf";
//            BaseFont bf = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//            String FONT_PATH_BOLD = "fonts/DejaVuSans-Bold.ttf";
//            BaseFont bfB = BaseFont.createFont(FONT_PATH_BOLD, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
//            FONT_14 = new Font(bf, 14);
//            FONT_8 = new Font(bf, 8);
//            FONT_8_BOLD = new Font(bfB, 8);
//            FONT_12_BOLD = new Font(bfB, 12);
//        } catch (DocumentException | IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public Document createAct(String file, MonthlyBill monthlyBill, boolean withStamp) {
//
//        Document document = null;
//
//        try {
//            document = new Document();
//            PdfWriter.getInstance(document, new FileOutputStream(file));
//            document.open();
//
//            document.setMargins(10, 10, 15, 15);
//
//            addMetaDataAct(document, monthlyBill);
//
//            addActTitle(document, monthlyBill);
//
//            createActTable(document, monthlyBill);
//
//            addActFooter(document, withStamp);
//            document.close();
//        } catch (DocumentException | IOException e) {
//            logger.error(e.getMessage());
//            e.printStackTrace();
//        }
//        return document;
//    }
//
//    private String createDocumentName(MonthlyBill monthlyBill) {
//        String day = String.valueOf(monthlyBill.getBillDate().getDayOfMonth());
//        String monthName = getMonthName(monthlyBill.getBillDate().getMonthValue());
//        String year = String.valueOf(monthlyBill.getBillDate().getYear());
//        String documentNumber = documentNumberService.createDocumentNumber(monthlyBill);
//
//        return "Акт № " + documentNumber + " от " + day + " " + monthName + " " + year + " г.";
//    }
//
//
//    private void addMetaDataAct(Document document, MonthlyBill monthlyBill) {
//        document.addTitle(this.createDocumentName(monthlyBill));
//        document.addSubject(this.createDocumentName(monthlyBill));
//        document.addAuthor("Majordomo.ru");
//        document.addCreator("Majordomo.ru");
//    }
//
//    private void addActTitle(Document document, MonthlyBill monthlyBill)
//            throws DocumentException {
//
//        Paragraph preface = new Paragraph();
//        preface.add(new Paragraph(this.createDocumentName(monthlyBill), FONT_14));
//        creteSeparatorLine(preface);
//        creteEmptyLine(preface, 1);
//        preface.add(new Paragraph("Исполнитель: " + monthlyBill.getPaymentCompany().getName(), FONT_8));
//        preface.add(new Paragraph("Адрес исполнителя: " + monthlyBill.getPaymentCompany().getLegalAddress(), FONT_8));
//        preface.add(new Paragraph("ИНН/КПП исполнителя: " +
//                monthlyBill.getPaymentCompany().getInn() + " / " + monthlyBill.getPaymentCompany().getKpp(), FONT_8));
//        preface.add(new Paragraph("Заказчик: " + monthlyBill.getClientName(), FONT_8));
//        preface.add(new Paragraph("Адрес заказчика: " + monthlyBill.getLegalAddress(), FONT_8));
//        preface.add(new Paragraph("ИНН/КПП заказчика: " +
//                monthlyBill.getInn() + " / " + monthlyBill.getKpp(), FONT_8));
//
//        document.add(preface);
//    }
//
//    private void addActFooter(Document document, boolean withStamp) throws DocumentException, IOException {
//        PdfPTable table;
//
//        if (withStamp) {
//            table = new PdfPTable(1);
//            table.setWidthPercentage(100);
//
//            Resource resource = applicationContext.getResource("classpath:images/act_footer_di.png");
//
//            Image img = Image.getInstance(resource.getURL());
//
//            img.scalePercent(70);
//
//            PdfPCell c1 = new PdfPCell(img);
//            c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//
//            c1.disableBorderSide(Rectangle.LEFT);
//            c1.disableBorderSide(Rectangle.RIGHT);
//            c1.disableBorderSide(Rectangle.BOTTOM);
//            c1.disableBorderSide(Rectangle.TOP);
//
//            table.addCell(c1);
//        } else {
//            float[] signatureColumnWidths = {3, 5, 1, 3, 5};
//            table = new PdfPTable(signatureColumnWidths);
//            table.getDefaultCell().setPadding(4);
//            table.setWidthPercentage(100);
//            table.getDefaultCell().setHorizontalAlignment(ALIGN_CENTER);
//            table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
//
//            PdfPCell c1 = new PdfPCell(new Phrase("Исполнитель", FONT_8_BOLD));
//            c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//            c1.setPadding(4);
//
//            c1.disableBorderSide(Rectangle.LEFT);
//            c1.disableBorderSide(Rectangle.RIGHT);
//            c1.disableBorderSide(Rectangle.BOTTOM);
//            c1.disableBorderSide(Rectangle.TOP);
//
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase("", FONT_8_BOLD));
//            c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//            c1.setPadding(4);
//
//            c1.disableBorderSide(Rectangle.LEFT);
//            c1.disableBorderSide(Rectangle.RIGHT);
//            c1.disableBorderSide(Rectangle.TOP);
//
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase(" ", FONT_8_BOLD));
//            c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//            c1.setPadding(4);
//
//            c1.disableBorderSide(Rectangle.LEFT);
//            c1.disableBorderSide(Rectangle.RIGHT);
//            c1.disableBorderSide(Rectangle.BOTTOM);
//            c1.disableBorderSide(Rectangle.TOP);
//
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase("Заказчик", FONT_8_BOLD));
//            c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//            c1.setPadding(4);
//
//            c1.disableBorderSide(Rectangle.LEFT);
//            c1.disableBorderSide(Rectangle.RIGHT);
//            c1.disableBorderSide(Rectangle.BOTTOM);
//            c1.disableBorderSide(Rectangle.TOP);
//
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase("", FONT_8_BOLD));
//            c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//            c1.setPadding(4);
//
//            c1.disableBorderSide(Rectangle.LEFT);
//            c1.disableBorderSide(Rectangle.RIGHT);
//            c1.disableBorderSide(Rectangle.TOP);
//
//            table.addCell(c1);
//        }
//
//        document.add(table);
//
//        if (!withStamp) {
//            Paragraph paragraph = new Paragraph();
//            creteEmptyLine(paragraph, 1);
//            paragraph.add(new Paragraph("За исполнителя:", FONT_8));
//            paragraph.add(new Paragraph("офис-менеджер  ______________________________  Полянская Я. Н.", FONT_8));
//            paragraph.add(new Paragraph("на основании доверенности № 6 от 10 февраля 2014г", FONT_8));
//
//            document.add(paragraph);
//        }
//    }
//
//    private void creteEmptyLine(Paragraph paragraph, int number) {
//        for (int i = 0; i < number; i++) {
//            paragraph.add(new Paragraph(" "));
//        }
//    }
//
//    private void creteSeparatorLine(Paragraph paragraph) {
//        LineSeparator ls = new LineSeparator();
//        paragraph.add(new Chunk(ls));
//    }
//
//    private void createActTable(Document document, MonthlyBill monthlyBill) throws DocumentException {
//        Paragraph paragraph = new Paragraph();
//        creteEmptyLine(paragraph, 1);
//        document.add(paragraph);
//
//        float[] columnWidths = {10, 80, 20, 20, 25, 25};
//
//        PdfPTable table = new PdfPTable(columnWidths);
//        table.getDefaultCell().setPadding(4);
//        table.setWidthPercentage(100);
//        table.getDefaultCell().setHorizontalAlignment(ALIGN_CENTER);
//        table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
//
//        PdfPCell c1 = new PdfPCell(new Phrase("№", FONT_8));
//        c1.setHorizontalAlignment(ALIGN_CENTER);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Наименование работ, услуг", FONT_8));
//        c1.setHorizontalAlignment(ALIGN_CENTER);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Кол-во", FONT_8));
//        c1.setHorizontalAlignment(ALIGN_CENTER);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Ед.", FONT_8));
//        c1.setHorizontalAlignment(ALIGN_CENTER);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Цена", FONT_8));
//        c1.setHorizontalAlignment(ALIGN_CENTER);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Сумма", FONT_8));
//        c1.setHorizontalAlignment(ALIGN_CENTER);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        table.setHeaderRows(1);
//
//        int rowIndex = 0;
//        BigDecimal totalCost = BigDecimal.ZERO;
//
//        for (MonthlyBillServiceItem monthlyBillServiceItem : monthlyBill.getMonthlyBillServiceItems()) {
//            c1 = new PdfPCell(new Phrase(String.valueOf(rowIndex + 1), FONT_8));
//            c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
//            c1.setPadding(4);
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase(monthlyBillServiceItem.getServiceName(), FONT_8));
//            c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//            c1.setPadding(4);
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase("1", FONT_8));
//            c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
//            c1.setPadding(4);
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase("шт.", FONT_8));
//            c1.setHorizontalAlignment(ALIGN_CENTER);
//            c1.setPadding(4);
//            table.addCell(c1);
//
//            BigDecimal cost = monthlyBillServiceItem.getCost().setScale(2, BigDecimal.ROUND_HALF_UP);
//
//            String costString = formatBigDecimal(cost);
//
//            c1 = new PdfPCell(new Phrase(costString, FONT_8));
//            c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
//            c1.setPadding(4);
//            table.addCell(c1);
//            table.addCell(c1);
//
//            totalCost = totalCost.add(cost);
//
//            rowIndex = rowIndex + 1;
//        }
//
//        totalCost = totalCost.setScale(2, BigDecimal.ROUND_HALF_UP);
//
//        String totalCostString = formatBigDecimal(totalCost);
//
//        c1 = new PdfPCell(new Phrase(" ", FONT_8));
//        c1.setPadding(4);
//        c1.disableBorderSide(Rectangle.LEFT);
//        c1.disableBorderSide(Rectangle.RIGHT);
//        c1.disableBorderSide(Rectangle.BOTTOM);
//
//        table.addCell(c1);
//        table.addCell(c1);
//        table.addCell(c1);
//        table.addCell(c1);
//        table.addCell(c1);
//        table.addCell(c1);
//
//        c1.disableBorderSide(Rectangle.TOP);
//        table.addCell(c1);
//        table.addCell(c1);
//        table.addCell(c1);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Итого:", FONT_8_BOLD));
//        c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
//        c1.setPadding(4);
//        c1.setBorderColor(BaseColor.WHITE);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase(totalCostString, FONT_8_BOLD));
//        c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
//        c1.setPadding(4);
//        c1.setBorderColor(BaseColor.WHITE);
//        table.addCell(c1);
//
//        c1 = new PdfPCell();
//        c1.setBorderColor(BaseColor.WHITE);
//
//        table.addCell(c1);
//        table.addCell(c1);
//        table.addCell(c1);
//
//
//        if (monthlyBill.getPaymentCompany().isVat()) {
//            c1 = new PdfPCell(new Phrase("В том числе НДС:", FONT_8_BOLD));
//            c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
//            c1.setColspan(2);
//            c1.setPadding(4);
//            c1.setBorderColor(BaseColor.WHITE);
//            table.addCell(c1);
//
//            String totalCostNdsString = formatBigDecimal(
//                    totalCost.
//                            multiply(BigDecimal.valueOf(18)).
//                            divide(BigDecimal.valueOf(118), 2, BigDecimal.ROUND_HALF_UP)
//            );
//
//            c1 = new PdfPCell(new Phrase(totalCostNdsString, FONT_8_BOLD));
//            c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
//            c1.setPadding(4);
//            c1.setBorderColor(BaseColor.WHITE);
//            table.addCell(c1);
//        } else {
//            c1 = new PdfPCell(new Phrase("Без налога (НДС)", FONT_8_BOLD));
//            c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
//            c1.setColspan(2);
//            c1.setPadding(4);
//            c1.setBorderColor(BaseColor.WHITE);
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase("-", FONT_8_BOLD));
//            c1.setHorizontalAlignment(Element.ALIGN_RIGHT);
//            c1.setPadding(4);
//            c1.setBorderColor(BaseColor.WHITE);
//            table.addCell(c1);
//        }
//
//        document.add(table);
//
//        MoneyToString moneyToString = new MoneyToString(totalCost);
//
//        String totalCostSpelled = moneyToString.num2str();
//        totalCostSpelled = totalCostSpelled.substring(0, 1).toUpperCase() + totalCostSpelled.substring(1);
//
//        paragraph = new Paragraph();
//        creteEmptyLine(paragraph, 1);
//        paragraph.add(new Paragraph("Всего оказано услуг " + rowIndex + ", на сумму " + totalCostString + " руб.", FONT_8));
//        paragraph.add(new Paragraph(totalCostSpelled, FONT_8_BOLD));
//        creteEmptyLine(paragraph, 1);
//        paragraph.add(new Paragraph("Вышеперечисленные услуги выполнены полностью и в срок. " +
//                "Заказчик претензий по объему, качеству и срокам оказания услуг не имеет.", FONT_8));
//        creteSeparatorLine(paragraph);
//        creteEmptyLine(paragraph, 1);
//
//        document.add(paragraph);
//    }
//
//    public Document createActSverki(
//            String file,
//            PaymentAccount paymentAccount,
//            PaymentCompany paymentCompany,
//            List<ActSverkiPreparedData> actSverkiPreparedDatas,
//            LocalDate startDate,
//            LocalDate endDate,
//            boolean withStamp
//    ) {
//
//        Document document = null;
//
//        try {
//            document = new Document(PageSize.A4.rotate());
//            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
//            writer.setBoxSize("art", new Rectangle(36, 36, 788, 559));
//            writer.setMargins(10, 10, 10, 10);
//
//            FooterEventHelper event = new FooterEventHelper();
//            writer.setPageEvent(event);
//
//            document.open();
//
//            document.setMargins(10, 10, 10, 10);
//
//            addMetaDataActSverki(document, startDate, endDate);
//
//            AccountOwner accountOwner = accountHelper.getOwner(paymentAccount.getAccountId());
//
//            addActSverkiTitle(document, paymentAccount, accountOwner, paymentCompany, startDate, endDate);
//
//            createActSverkiTable(document, paymentAccount, accountOwner, paymentCompany, actSverkiPreparedDatas, endDate);
//
//            addActSverkiFooter(document, paymentAccount, accountOwner, paymentCompany, withStamp);
//
//            document.close();
//
//        } catch (DocumentException | IOException e) {
//            logger.error(e.getMessage());
//            e.printStackTrace();
//        }
//        return document;
//    }
//
//    private void addMetaDataActSverki(Document document, LocalDate startDate, LocalDate endDate) {
//        document.addTitle(this.createDocumentName(startDate, endDate));
//        document.addSubject(this.createDocumentName(startDate, endDate));
//        document.addAuthor("Majordomo.ru");
//        document.addCreator("Majordomo.ru");
//    }
//
//    private void addActSverkiTitle(
//            Document document,
//            PaymentAccount paymentAccount,
//            AccountOwner accountOwner,
//            PaymentCompany paymentCompany,
//            LocalDate startDate,
//            LocalDate endDate
//    )
//            throws DocumentException {
//        Paragraph paragraph;
//        Paragraph preface = new Paragraph();
//
//        paragraph = new Paragraph("Акт сверки", FONT_12_BOLD);
//        paragraph.setAlignment(ALIGN_CENTER);
//        preface.add(paragraph);
//
//        paragraph = new Paragraph("взаимных расчетов за период: " + createDatePeriodText(startDate, endDate), FONT_8);
//        paragraph.setAlignment(ALIGN_CENTER);
//        preface.add(paragraph);
//
//        paragraph = new Paragraph("между " + paymentCompany.getName(), FONT_8);
//        paragraph.setAlignment(ALIGN_CENTER);
//        preface.add(paragraph);
//
//        paragraph = new Paragraph("и " + accountOwner.getName(), FONT_8);
//        paragraph.setAlignment(ALIGN_CENTER);
//        preface.add(paragraph);
//
//        creteEmptyLine(preface, 1);
//        preface.add(new Paragraph("Мы, нижеподписавшиеся, Главный бухгалтер " +
//                paymentCompany.getName() + " " + paymentCompany.getAccountant() + ", с одной стороны, и " +
//                accountOwner.getName() + ", с другой стороны, составили настоящий акт сверки в том, " +
//                "что состояние взаимных расчетов по данным учета следующее:", FONT_8)
//        );
//
//        document.add(preface);
//    }
//
//    private void createActSverkiTable(
//            Document document,
//            PaymentAccount paymentAccount,
//            AccountOwner accountOwner,
//            PaymentCompany paymentCompany,
//            List<ActSverkiPreparedData> actSverkiPreparedDatas,
//            LocalDate endDate
//    ) throws DocumentException {
//        Paragraph paragraph = new Paragraph();
//        creteEmptyLine(paragraph, 1);
//        document.add(paragraph);
//
//        float[] columnWidths = {60, 215, 60, 60, 60, 215, 60, 60};
//
//        PdfPTable table = new PdfPTable(8);
//        table.getDefaultCell().setPadding(4);
//        table.setTotalWidth(columnWidths);
//        table.setLockedWidth(true);
//        table.setWidthPercentage(100);
//        table.setSkipLastFooter(true);
//        table.getDefaultCell().setHorizontalAlignment(ALIGN_CENTER);
//        table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
//
//        PdfPCell c1 = new PdfPCell(new Phrase("По данным " + paymentCompany.getName() + ", руб.", FONT_8));
//        c1.setColspan(4);
//        c1.setHorizontalAlignment(ALIGN_LEFT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("По данным " + accountOwner.getName() + ", руб.", FONT_8));
//        c1.setColspan(4);
//        c1.setHorizontalAlignment(ALIGN_LEFT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Дата", FONT_8));
//        c1.setHorizontalAlignment(ALIGN_CENTER);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Документ", FONT_8));
//        c1.setHorizontalAlignment(ALIGN_CENTER);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Дебет", FONT_8));
//        c1.setHorizontalAlignment(ALIGN_CENTER);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Кредит", FONT_8));
//        c1.setHorizontalAlignment(ALIGN_CENTER);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Дата", FONT_8));
//        c1.setHorizontalAlignment(ALIGN_CENTER);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Документ", FONT_8));
//        c1.setHorizontalAlignment(ALIGN_CENTER);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Дебет", FONT_8));
//        c1.setHorizontalAlignment(ALIGN_CENTER);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Кредит", FONT_8));
//        c1.setHorizontalAlignment(ALIGN_CENTER);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        table.setHeaderRows(2);
//
//        String startOurDebet = "";
//        String startOurKredit = "";
//        String startClientDebet = "";
//        String startClientKredit = "";
//
//        BigDecimal startBalance = actSverkiPreparedDatas.get(0).getStartBalance().setScale(2, BigDecimal.ROUND_HALF_UP);
//
//        if (startBalance.compareTo(BigDecimal.ZERO) > 0) {
//            startOurKredit = startClientDebet = formatBigDecimal(startBalance);
//        } else if(startBalance.compareTo(BigDecimal.ZERO) < 0){
//            startOurDebet = startClientKredit = formatBigDecimal(startBalance.abs());
//        }
//
//        c1 = new PdfPCell(new Phrase("Сальдо начальное", FONT_8_BOLD));
//        c1.setColspan(2);
//        c1.setHorizontalAlignment(ALIGN_LEFT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase(startOurDebet, FONT_8_BOLD));
//        c1.setHorizontalAlignment(ALIGN_RIGHT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase(startOurKredit, FONT_8_BOLD));
//        c1.setHorizontalAlignment(ALIGN_RIGHT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Сальдо начальное", FONT_8_BOLD));
//        c1.setColspan(2);
//        c1.setHorizontalAlignment(ALIGN_LEFT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase(startClientDebet, FONT_8_BOLD));
//        c1.setHorizontalAlignment(ALIGN_RIGHT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase(startClientKredit, FONT_8_BOLD));
//        c1.setHorizontalAlignment(ALIGN_RIGHT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        BigDecimal totalPaymentsAmount = BigDecimal.ZERO;
//        BigDecimal totalBillsAmount = BigDecimal.ZERO;
//
//        for (ActSverkiPreparedData actSverkiPreparedData : actSverkiPreparedDatas) {
//            BigDecimal amount = actSverkiPreparedData.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP);
//
//            String ourDocumentText = "";
//            String clientDocumentText = "";
//            String ourDebet = "";
//            String ourCredit = "";
//            String clientDebet = "";
//            String clientCredit = "";
//            String date = actSverkiPreparedData.getDate().format(DateTimeFormatter.ofPattern("dd.MM.uuuu"));
//            switch (actSverkiPreparedData.getType()) {
//                case PAYMENT:
//                    ourDocumentText = clientDocumentText = "Оплата (" + actSverkiPreparedData.getDocumentNumber() + " от " + date + ")";
//                    ourCredit = clientDebet = formatBigDecimal(amount);
//                    totalPaymentsAmount = totalPaymentsAmount.add(amount);
//
//                    break;
//                case MONTHLY_BILL:
//                    ourDocumentText = "Продажа (" + actSverkiPreparedData.getDocumentNumber() + " от " + date + ")";
//                    clientDocumentText = "Приход (" + actSverkiPreparedData.getDocumentNumber() + " от " + date + ")";
//                    ourDebet = clientCredit = formatBigDecimal(amount);
//                    totalBillsAmount = totalBillsAmount.add(amount);
//
//                    break;
//                case MONEY_RETURN:
//                    ourDocumentText = clientDocumentText = "Возврат (" + actSverkiPreparedData.getDocumentNumber() + " от " + date + ")";
//                    ourDebet = clientCredit = formatBigDecimal(amount);
//                    totalBillsAmount = totalBillsAmount.add(amount);
//
//                    break;
//                case MONEY_TRANSFER:
//                    ourDocumentText = clientDocumentText = "Перевод ден.средств (" + actSverkiPreparedData.getDocumentNumber() + " от " + date + ")";
//                    ourDebet = clientCredit = formatBigDecimal(amount);
//                    totalBillsAmount = totalBillsAmount.add(amount);
//
//                    break;
//            }
//            c1 = new PdfPCell(new Phrase(date, FONT_8));
//            c1.setHorizontalAlignment(ALIGN_CENTER);
//            c1.setPadding(4);
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase(ourDocumentText, FONT_8));
//            c1.setHorizontalAlignment(ALIGN_LEFT);
//            c1.setPadding(4);
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase(ourDebet, FONT_8));
//            c1.setHorizontalAlignment(ALIGN_RIGHT);
//            c1.setPadding(4);
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase(ourCredit, FONT_8));
//            c1.setHorizontalAlignment(ALIGN_RIGHT);
//            c1.setPadding(4);
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase(date, FONT_8));
//            c1.setHorizontalAlignment(ALIGN_CENTER);
//            c1.setPadding(4);
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase(clientDocumentText, FONT_8));
//            c1.setHorizontalAlignment(ALIGN_LEFT);
//            c1.setPadding(4);
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase(clientDebet, FONT_8));
//            c1.setHorizontalAlignment(ALIGN_RIGHT);
//            c1.setPadding(4);
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase(clientCredit, FONT_8));
//            c1.setHorizontalAlignment(ALIGN_RIGHT);
//            c1.setPadding(4);
//            table.addCell(c1);
//        }
//
//        String ourTotalDebet = "";
//        String ourTotalKredit = "";
//        String clientTotalDebet = "";
//        String clientTotalKredit = "";
//
//        if (totalBillsAmount.compareTo(BigDecimal.ZERO) != 0) {
//            ourTotalDebet = clientTotalKredit = formatBigDecimal(totalBillsAmount);
//        }
//
//        if (totalPaymentsAmount.compareTo(BigDecimal.ZERO) != 0) {
//            ourTotalKredit = clientTotalDebet = formatBigDecimal(totalPaymentsAmount);
//        }
//
//        c1 = new PdfPCell(new Phrase("Обороты за период", FONT_8_BOLD));
//        c1.setColspan(2);
//        c1.setHorizontalAlignment(ALIGN_LEFT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase(ourTotalDebet, FONT_8_BOLD));
//        c1.setHorizontalAlignment(ALIGN_RIGHT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase(ourTotalKredit, FONT_8_BOLD));
//        c1.setHorizontalAlignment(ALIGN_RIGHT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Обороты за период", FONT_8_BOLD));
//        c1.setColspan(2);
//        c1.setHorizontalAlignment(ALIGN_LEFT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase(clientTotalDebet, FONT_8_BOLD));
//        c1.setHorizontalAlignment(ALIGN_RIGHT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase(clientTotalKredit, FONT_8_BOLD));
//        c1.setHorizontalAlignment(ALIGN_RIGHT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        String endOurDebet = "";
//        String endOurKredit = "";
//        String endClientDebet = "";
//        String endClientKredit = "";
//
//        BigDecimal endBalance = actSverkiPreparedDatas.get(0).getEndBalance().setScale(2, BigDecimal.ROUND_HALF_UP);
//
//        if (endBalance.compareTo(BigDecimal.ZERO) > 0) {
//            endOurKredit = endClientDebet = formatBigDecimal(endBalance);
//        } else if(endBalance.compareTo(BigDecimal.ZERO) < 0){
//            endOurDebet = endClientKredit = formatBigDecimal(endBalance.abs());
//        }
//
//        c1 = new PdfPCell(new Phrase("Сальдо конечное", FONT_8_BOLD));
//        c1.setColspan(2);
//        c1.setHorizontalAlignment(ALIGN_LEFT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase(endOurDebet, FONT_8_BOLD));
//        c1.setHorizontalAlignment(ALIGN_RIGHT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase(endOurKredit, FONT_8_BOLD));
//        c1.setHorizontalAlignment(ALIGN_RIGHT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("Сальдо конечное", FONT_8_BOLD));
//        c1.setColspan(2);
//        c1.setHorizontalAlignment(ALIGN_LEFT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase(endClientDebet, FONT_8_BOLD));
//        c1.setHorizontalAlignment(ALIGN_RIGHT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase(endClientKredit, FONT_8_BOLD));
//        c1.setHorizontalAlignment(ALIGN_RIGHT);
//        c1.setPadding(4);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase(" ", FONT_8));
//        c1.setPadding(4);
//        c1.disableBorderSide(Rectangle.LEFT);
//        c1.disableBorderSide(Rectangle.RIGHT);
//        c1.disableBorderSide(Rectangle.BOTTOM);
//
//        table.addCell(c1);
//        table.addCell(c1);
//        table.addCell(c1);
//        table.addCell(c1);
//        table.addCell(c1);
//        table.addCell(c1);
//        table.addCell(c1);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("По данным " + paymentCompany.getName(), FONT_8));
//        c1.disableBorderSide(Rectangle.TOP);
//        c1.setColspan(4);
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setPadding(4);
//        c1.setBorderColor(BaseColor.WHITE);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase("По данным " + accountOwner.getName(), FONT_8));
//        c1.disableBorderSide(Rectangle.TOP);
//        c1.setColspan(4);
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setPadding(4);
//        c1.setBorderColor(BaseColor.WHITE);
//        table.addCell(c1);
//
//        String itogText = "на " + createDateText(endDate) + " задолженность отсутствует.";
//
//        if (!endOurDebet.equals("") && endOurKredit.equals("")) {
//            MoneyToString moneyToString = new MoneyToString(endBalance.abs());
//
//            String totalCostSpelled = moneyToString.num2str();
//            totalCostSpelled = totalCostSpelled.substring(0, 1).toUpperCase() + totalCostSpelled.substring(1);
//
//            itogText = "на " + createDateText(endDate) + " задолженность в пользу\n" +
//                    paymentCompany.getName() + "\n" + endOurDebet + " руб.\n(" + totalCostSpelled + ")";
//        } else if (endOurDebet.equals("") && !endOurKredit.equals("")) {
//            MoneyToString moneyToString = new MoneyToString(endBalance.abs());
//
//            String totalCostSpelled = moneyToString.num2str();
//            totalCostSpelled = totalCostSpelled.substring(0, 1).toUpperCase() + totalCostSpelled.substring(1);
//
//            itogText = "на " + createDateText(endDate) + " задолженность в пользу\n" +
//                    accountOwner.getName() + "\n" + endOurKredit + " руб.\n(" + totalCostSpelled + ")";
//        }
//
//        c1 = new PdfPCell(new Phrase(itogText, FONT_8_BOLD));
//        c1.disableBorderSide(Rectangle.TOP);
//        c1.setColspan(4);
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setPadding(4);
//        c1.setBorderColor(BaseColor.WHITE);
//        table.addCell(c1);
//
//        c1 = new PdfPCell(new Phrase(itogText, FONT_8_BOLD));
//        c1.disableBorderSide(Rectangle.TOP);
//        c1.setColspan(4);
//        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//        c1.setPadding(4);
//        c1.setBorderColor(BaseColor.WHITE);
//        table.addCell(c1);
//
//        document.add(table);
//    }
//
//
//    private void addActSverkiFooter(
//            Document document,
//            PaymentAccount paymentAccount,
//            AccountOwner accountOwner,
//            PaymentCompany paymentCompany,
//            boolean withStamp
//    ) throws DocumentException, IOException {
//        PdfPTable table;
//
//        float[] columnWidths = {45, 215, 60, 60, 45, 215, 60, 60};
//
//        table = new PdfPTable(8);
//        table.getDefaultCell().setPadding(4);
//        table.setTotalWidth(columnWidths);
//        table.setLockedWidth(true);
//        table.setWidthPercentage(100);
//        table.setSkipLastFooter(true);
//        table.getDefaultCell().setHorizontalAlignment(ALIGN_CENTER);
//        table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
//
//        if (withStamp) {
//            Resource resource = applicationContext.getResource("classpath:images/act_sverki_hosting_prokofieva.gif");
//
//            Image img = Image.getInstance(resource.getURL());
//
//            img.scalePercent(25);
//
//            PdfPCell c1 = new PdfPCell(img);
//            c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//            c1.setColspan(4);
//
//            c1.disableBorderSide(Rectangle.LEFT);
//            c1.disableBorderSide(Rectangle.RIGHT);
//            c1.disableBorderSide(Rectangle.BOTTOM);
//            c1.disableBorderSide(Rectangle.TOP);
//
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase("От " + accountOwner.getName() +
//                    "\n\n______________\n\n_____________________________________(________________)\n\nМ.П.", FONT_8)
//            );
//            c1.disableBorderSide(Rectangle.TOP);
//            c1.setColspan(4);
//            c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//            c1.setPadding(4);
//            c1.setBorderColor(BaseColor.WHITE);
//            table.addCell(c1);
//        } else {
//            PdfPCell c1 = new PdfPCell(new Phrase("От " + paymentCompany.getName() +
//                    "\n\nГлавный бухгалтер\n\n_____________________________________(" +
//                    paymentCompany.getAccountant() + ")\n\nМ.П.", FONT_8)
//            );
//            c1.disableBorderSide(Rectangle.TOP);
//            c1.setColspan(4);
//            c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//            c1.setPadding(4);
//            c1.setBorderColor(BaseColor.WHITE);
//            table.addCell(c1);
//
//            c1 = new PdfPCell(new Phrase("От " + accountOwner.getName() +
//                    "\n\n______________\n\n_____________________________________(________________)\n\nМ.П.", FONT_8)
//            );
//            c1.disableBorderSide(Rectangle.TOP);
//            c1.setColspan(4);
//            c1.setHorizontalAlignment(Element.ALIGN_LEFT);
//            c1.setPadding(4);
//            c1.setBorderColor(BaseColor.WHITE);
//            table.addCell(c1);
//        }
//
//        document.add(table);
//    }
//
//    public Document createInvoice(
//            String file,
//            PaymentAccount paymentAccount,
//            PaymentCompany paymentCompany,
//            Invoice invoice,
//            boolean withStamp
//    ) {
//
//        Document document = null;
//
//        try {
//            document = new Document(PageSize.A4);
//            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
//            document.open();
//
//            document.setMargins(10, 10, 15, 15);
//
//            PdfContentByte canvas = writer.getDirectContentUnder();
//
//            Image image;
//            if (withStamp) {
//                Resource resource = applicationContext.getResource("classpath:images/invoice_hosting.client.png");
//
//                image = Image.getInstance(resource.getURL());
//            } else {
//                Resource resource = applicationContext.getResource("classpath:images/invoice_hosting.clean.png");
//
//                image = Image.getInstance(resource.getURL());
//            }
//
//            image.scalePercent(58);
//            image.setAbsolutePosition(0, 0);
//            canvas.addImage(image);
//
//            addMetaDataInvoice(document, invoice);
//            addInvoiceData(canvas, paymentAccount, invoice);
//
//            document.close();
//
//        } catch (DocumentException | IOException e) {
//            logger.error(e.getMessage());
//            e.printStackTrace();
//        }
//        return document;
//    }
//
//    private void addInvoiceData(
//            PdfContentByte canvas,
//            PaymentAccount paymentAccount,
//            Invoice invoice
//    )
//            throws DocumentException {
//        ColumnText columnText = new ColumnText(canvas);
//
//        Phrase myText = new Phrase(invoice.getInvoiceNumber(), FONT_12_BOLD);
//        columnText.setSimpleColumn(myText, 255, 555, 580, 440, 95, Element.ALIGN_LEFT);
//
//        columnText.go();
//
//        myText = new Phrase(invoice.getCreated().format(DateTimeFormatter.ofPattern("dd.MM.uuuu")), FONT_12_BOLD);
//        columnText.setSimpleColumn(myText, 345, 555, 580, 440, 95, Element.ALIGN_LEFT);
//
//        columnText.go();
//
//        String clientName = invoice.getClientName();
//
//        List<String> clientNameSplit = Arrays.asList(clientName);
//        if (clientName.length() > 62) {
//            clientNameSplit = Splitter.on("\n").splitToList(WordUtils.wrap(clientName, 62, "\n", true));
//        }
//
//        myText = new Phrase(clientNameSplit.get(0), FONT_12_BOLD);
//        columnText.setSimpleColumn(myText, 85, 515, 580, 345, 95, Element.ALIGN_LEFT);
//
//        columnText.go();
//
//        myText = new Phrase(clientNameSplit.size() > 1 ? clientNameSplit.get(1) : "", FONT_12_BOLD);
//        columnText.setSimpleColumn(myText, 25, 430, 580, 345, 35, Element.ALIGN_LEFT);
//
//        columnText.go();
//
//        myText = new Phrase(clientNameSplit.size() > 2 ? clientNameSplit.get(2) : "", FONT_12_BOLD);
//        columnText.setSimpleColumn(myText, 25, 410, 580, 345, 35, Element.ALIGN_LEFT);
//
//        columnText.go();
//
//        myText = new Phrase("Пополнение счета аккаунта " + paymentAccount.getName(), FONT_12_BOLD);
//        columnText.setSimpleColumn(myText, 25, 340, 580, 285, 35, Element.ALIGN_LEFT);
//
//        columnText.go();
//
//        myText = new Phrase(formatBigDecimal(invoice.getAmount()), FONT_12_BOLD);
//        columnText.setSimpleColumn(myText, 455, 340, 580, 285, 35, Element.ALIGN_LEFT);
//
//        columnText.go();
//
//        myText = new Phrase("-", FONT_12_BOLD);
//        columnText.setSimpleColumn(myText, 455, 295, 580, 235, 35, Element.ALIGN_LEFT);
//
//        columnText.go();
//
//        myText = new Phrase(formatBigDecimal(invoice.getAmount()), FONT_12_BOLD);
//        columnText.setSimpleColumn(myText, 455, 272, 580, 207, 35, Element.ALIGN_LEFT);
//
//        columnText.go();
//
//        MoneyToString moneyToString = new MoneyToString(invoice.getAmount());
//
//        String amountSpelled = moneyToString.num2str();
//        amountSpelled = amountSpelled.substring(0, 1).toUpperCase() + amountSpelled.substring(1);
//
//        List<String> amountSpelledSplit = Arrays.asList(amountSpelled);
//        if (amountSpelled.length() > 49) {
//            amountSpelledSplit = Splitter.on("\n").splitToList(WordUtils.wrap(amountSpelled, 49, "\n", true));
//        }
//
//        myText = new Phrase(amountSpelledSplit.get(0), FONT_12_BOLD);
//        columnText.setSimpleColumn(myText, 195, 232, 580, 170, 35, Element.ALIGN_LEFT);
//
//        columnText.go();
//
//        myText = new Phrase(amountSpelledSplit.size() > 1 ? amountSpelledSplit.get(1) : "", FONT_12_BOLD);
//        columnText.setSimpleColumn(myText, 25, 215, 580, 120, 35, Element.ALIGN_LEFT);
//
//        columnText.go();
//    }
//
//    private void addMetaDataInvoice(Document document, Invoice invoice) {
//        document.addTitle(this.createInvoiceDocumentName(invoice));
//        document.addSubject(this.createInvoiceDocumentName(invoice));
//        document.addAuthor("Majordomo.ru");
//        document.addCreator("Majordomo.ru");
//    }
//
//    private String createInvoiceDocumentName(Invoice invoice) {
//        String day = String.valueOf(invoice.getCreated().getDayOfMonth());
//        String monthName = getMonthName(invoice.getCreated().getMonthValue());
//        String year = String.valueOf(invoice.getCreated().getYear());
//        String documentNumber = invoice.getInvoiceNumber();
//
//        return "Счет № " + documentNumber + " от " + day + " " + monthName + " " + year + " г.";
//    }
//
//    private String createDocumentName(LocalDate startDate, LocalDate endDate) {
//        return "Акт сверки за период: " + createDatePeriodText(startDate, endDate);
//    }
//
//    private String createDatePeriodText(LocalDate startDate, LocalDate endDate) {
//        return createDateText(startDate) + " - " + createDateText(endDate);
//    }
//
//    private String createDateText(LocalDate date) {
//        String day = String.valueOf(date.getDayOfMonth());
//        String monthName = getMonthName(date.getMonthValue());
//        String year = String.valueOf(date.getYear());
//
//        return day + " " + monthName + " " + year + " г.";
//    }
//
//    private class FooterEventHelper extends PdfPageEventHelper {
//
//        public void onEndPage (PdfWriter writer, Document document) {
//            Rectangle rect = writer.getBoxSize("art");
//
//            ColumnText.showTextAligned(writer.getDirectContent(),
//                    Element.ALIGN_RIGHT, new Phrase(String.format("Страница %d", writer.getPageNumber()), FONT_8),
//                    rect.getRight() - 20, rect.getBottom() - 18, 0);
//        }
//    }
//}
