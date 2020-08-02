package com.teamadmino.application.fbzprod.plugins;

import static com.itextpdf.text.Element.ALIGN_CENTER;
import static com.itextpdf.text.Element.ALIGN_LEFT;
import static com.itextpdf.text.Element.ALIGN_MIDDLE;
import static com.itextpdf.text.Element.ALIGN_RIGHT;
import static com.teamadmino.admino_backend.server.database.ReportingSnapshot.getsnapshot;
import static com.teamadmino.admino_backend.server.database.ServerMain.getNextTempId;
import static com.teamadmino.admino_backend.server.database.ServerMain.jobConfigs;
import static com.teamadmino.admino_backend.server.database.ServerMain.log;
import static com.teamadmino.admino_backend.server.database.ServerMain.tables;
import static com.teamadmino.admino_backend.server.database.ServerMain.workdir;
import static com.teamadmino.admino_backend.server.processing.JobProcessor.createJob;
import static com.teamadmino.admino_backend.server.processing.JobProcessor.getJodStatus;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.teamadmino.admino_backend.server.database.DatabaseSchemaDefinition;
import com.teamadmino.admino_backend.server.database.DatabaseTable;
import com.teamadmino.admino_backend.server.database.MemoryRecord;
import com.teamadmino.admino_backend.server.database.Record;
import com.teamadmino.admino_backend.server.database.ReportingSnapshot;
import com.teamadmino.admino_backend.server.database.TableViewDefinition;
import com.teamadmino.admino_backend.server.http.RequestContext;
import com.teamadmino.admino_backend.server.processing.JobConfig;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ReportsVisszavetUploads {

    public static void init_onload(RequestContext rc) throws Exception {
        String tempTableName = RequestContext.getTempTableName("list");
        DatabaseSchemaDefinition databaseSchema = new DatabaseSchemaDefinition(
            tempTableName,
            "Visszavételezések vonalkód feltöltésekkel",
            "schema"
        );
        databaseSchema
            .addField("vuup", "Feltöltés", "int16", "zeropadded", 5)
            .addField("dttm", "Időpont", "int32", "time", 19);
        DatabaseTable tempTable = new DatabaseTable("_test", databaseSchema);
        Record record = tempTable.newRecordObject();

        ReportingSnapshot snapshot = getsnapshot(Arrays.asList(
            "VDUPL?",
            "VDAT?",
            "VDAT.vaje",
            "VDAT.vabl",
            "VDAT.vaup",
            "VDAT.vatm"
        ));

        Integer aVdupl = snapshot.sizes.get("VDUPL?");
        Integer aVdat = snapshot.sizes.get("VDAT?");

        char[] vaup = (char[]) snapshot.data.get("VDAT.vaup");
        byte[] vaje = (byte[]) snapshot.data.get("VDAT.vaje");
        int[] vatm = (int[]) snapshot.data.get("VDAT.vatm");

        int v = 0;
        boolean[] vu = new boolean[aVdupl + 1];
        int[] vt = new int[aVdupl + 1];
        for (int i = 1; i <= aVdat; i++) {
            char u = vaup[i];
            vt[u]++;
            if (vaje[i] == 'V') {
                v++;
                if (!vu[u]) {
                    vu[u] = true;
                    record.setField("vuup", u).setField("dttm", vatm[i]).insert();
                }
            }
        }

        TableViewDefinition tableViewDefinition = new TableViewDefinition(tempTableName);
        tableViewDefinition.addTableField("vuup");
        tableViewDefinition.addTableField("dttm");
        rc.setupTableView("list", tableViewDefinition).setFocus("list");
        rc.getScreenVars().put("list", tempTableName);
    }

    public static void bla_action(RequestContext rc) throws Exception {

        try {

            int up;
            try {
                JSONObject status = rc.getValueJson("list");
                up = Integer.parseInt(status.getJSONObject("keys").optString("$1"));
            } catch (Exception e) {
                up = 0;
                rc.snackErrorMessage("Error cheking id");
                return;
            }

            ReportingSnapshot snapshot = getsnapshot(Arrays.asList(
                "VDUPL?",
                "VDAT?",
                "VDAT.vaje",
                "VDAT.vabl",
                "VDAT.vaup",
                "GONGY.kg1"
            ));

            Integer aVdupl = snapshot.sizes.get("VDUPL?");
            Integer aVdat = snapshot.sizes.get("VDAT?");

            char[] vaup = (char[]) snapshot.data.get("VDAT.vaup");
            byte[] vaje = (byte[]) snapshot.data.get("VDAT.vaje");
            int[] vabl = (int[]) snapshot.data.get("VDAT.vabl");
            int[] kg1 = (int[]) snapshot.data.get("GONGY.kg1");

            Set<Integer> bla = new TreeSet<>();

            String IMG = "workdir/fbzdemo/resources/fbz_logo_small.png";
            Font titleFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
            Font headerFont = new Font(Font.FontFamily.HELVETICA,9);
            Font numFont = new Font(Font.FontFamily.COURIER,9);
            Font textFont = new Font(Font.FontFamily.HELVETICA,9);

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSS");
            Calendar cal = Calendar.getInstance();
            String time = dateFormat.format(cal.getTime());

            String fileName = "generated-" + time + String.format("_%03d", getNextTempId() % 1000) + ".pdf";
            String localFileName = workdir + "/temp/" + fileName;

            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(localFileName));
            document.open();

            PdfPTable titleTable = new PdfPTable(2);
            titleTable.setWidths(new int[]{10, 90});
            titleTable.setTotalWidth(100);
            titleTable.setWidthPercentage(100);
            Image img = Image.getInstance(IMG);
            PdfPCell cellLogo = new PdfPCell(img, true);
            cellLogo.setBorderWidth(0);
            titleTable.addCell(cellLogo);
            Paragraph title = new Paragraph("Visszavételezés vonalkód feltöltéssel: " + up, titleFont);
            PdfPCell cellTitle = new PdfPCell(title);
            cellTitle.setHorizontalAlignment(ALIGN_CENTER);
            cellTitle.setVerticalAlignment(ALIGN_MIDDLE);
            cellTitle.setBorderWidth(0);
            titleTable.addCell(cellTitle);
            document.add(titleTable);
            document.add(new Paragraph(" "));

            PdfPCell cell;
            Paragraph paragraph;

            PdfPTable dataTable = new PdfPTable(5);
            dataTable.setWidths(new int[]{10, 11, 4, 30, 6});
            dataTable.setTotalWidth(10 + 11 + 4 + 30 + 6);
            dataTable.setWidthPercentage(100);

            paragraph = new Paragraph("Bálaszám",headerFont);
            cell = new PdfPCell(paragraph);
            cell.setHorizontalAlignment(ALIGN_LEFT);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            dataTable.addCell(cell);

            paragraph = new Paragraph("Tételszám",headerFont);
            cell = new PdfPCell(paragraph);
            cell.setHorizontalAlignment(ALIGN_LEFT);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            dataTable.addCell(cell);

//            paragraph = new Paragraph("Kiadás");
//            cell = new PdfPCell(paragraph);
//            cell.setHorizontalAlignment(ALIGN_LEFT);
//            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
//            dataTable.addCell(cell);

            paragraph = new Paragraph("Cikk",headerFont);
            cell = new PdfPCell(paragraph);
            cell.setHorizontalAlignment(ALIGN_LEFT);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            dataTable.addCell(cell);

            paragraph = new Paragraph("Cikk megnevezése",headerFont);
            cell = new PdfPCell(paragraph);
            cell.setHorizontalAlignment(ALIGN_LEFT);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            dataTable.addCell(cell);

            paragraph = new Paragraph("Nettó Kg",headerFont);
            cell = new PdfPCell(paragraph);
            cell.setHorizontalAlignment(ALIGN_RIGHT);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            dataTable.addCell(cell);

            //            PdfPTable dataTable = new PdfPTable(4);
//            dataTable.setWidths(new int[]{4, 30, 10, 10});
//            dataTable.setTotalWidth(54);
//            dataTable.setWidthPercentage(100);

//            paragraph = new Paragraph("Cikk");
//            cell = new PdfPCell(paragraph);
//            cell.setHorizontalAlignment(ALIGN_LEFT);
//            //cell.setBorderColor(BaseColor.WHITE);
//            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
//
//            dataTable.addCell(cell);
//            paragraph = new Paragraph("Cikk megnevezése");
//            cell = new PdfPCell(paragraph);
//            cell.setHorizontalAlignment(ALIGN_LEFT);
//            //cell.setBorderColor(BaseColor.WHITE);
//            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
//            dataTable.addCell(cell);
//
//            paragraph = new Paragraph("Bála db");
//            cell = new PdfPCell(paragraph);
//            cell.setHorizontalAlignment(ALIGN_RIGHT);
//            //cell.setBorderColor(BaseColor.BLACK);
//            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
//            dataTable.addCell(cell);
//
//            paragraph = new Paragraph("Nettó Kg");
//            cell = new PdfPCell(paragraph);
//            cell.setHorizontalAlignment(ALIGN_RIGHT);
//            //cell.setBorderColor(BaseColor.WHITE);
//            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
//            dataTable.addCell(cell);

            DatabaseTable cikkTable = tables.get("CIKK");
            Record cikkRecord = new Record(cikkTable);
            MemoryRecord cikkMr;

            DatabaseTable bbejTable = tables.get("BBEJ");
            Record bbejRecord = new Record(bbejTable);
            MemoryRecord bbejMr;

            DatabaseTable bkiaTable = tables.get("BKIA");
            Record bkiaRecord = new Record(bkiaTable);
            MemoryRecord bkiaMr;

            for (int i = 1; i <= aVdat; i++) {
                char u = vaup[i];
                if (u != up) {
                    continue;
                }
                if (vaje[i] == 'V') {
                    int bl = vabl[i];
                    if (!bla.contains(bl)) {
                        bla.add(bl);

                        bbejRecord.setField("bkod", bl);
                        int posBl = bbejRecord.findLast(1, Arrays.asList("bkod"));
                        if (posBl < 1) {
                            continue;
                        }
                        bbejMr = bbejTable.getMemoryRecord(posBl, 1);

//                        bkiaRecord.setField("bkod", bl);
//                        int posBk = bkiaRecord.findLast(1, Arrays.asList("bkod"));
//                        if (posBk < 1) {
//                            continue;
//                        }
//                        bkiaMr = bkiaTable.getMemoryRecord(posBk, 1);

                        cikkRecord.setField("ckod", (char) Integer.parseInt(bbejMr.getAsString("bcik")));
                        int posCk = cikkRecord.findLast(1, Arrays.asList("ckod"));
                        if (posCk < 1) {
                            continue;
                        }
                        cikkMr = cikkTable.getMemoryRecord(posCk, 1);

                        paragraph = new Paragraph(String.format("%7d/%02d", bl / 100, bl % 100),textFont);
                        cell = new PdfPCell(paragraph);
                        cell.setHorizontalAlignment(ALIGN_LEFT);
                        cell.setBorderColor(BaseColor.LIGHT_GRAY);
                        dataTable.addCell(cell);

                        paragraph = new Paragraph(bbejMr.getAsString("tetl"),textFont);
                        cell = new PdfPCell(paragraph);
                        cell.setHorizontalAlignment(ALIGN_LEFT);
                        cell.setBorderColor(BaseColor.LIGHT_GRAY);
                        dataTable.addCell(cell);

//                        int kb = Integer.parseInt(bkiaMr.getAsString("bzsz"));
//                        paragraph = new Paragraph(String.format("%06d/%02d", kb / 100, kb % 100));
//                        cell = new PdfPCell(paragraph);
//                        cell.setHorizontalAlignment(ALIGN_LEFT);
//                        cell.setBorderColor(BaseColor.LIGHT_GRAY);
//                        dataTable.addCell(cell);

                        paragraph = new Paragraph(String.format("%04d", Integer.parseInt(bbejMr.getAsString("bcik"))),textFont);
                        cell = new PdfPCell(paragraph);
                        cell.setHorizontalAlignment(ALIGN_LEFT);
                        cell.setBorderColor(BaseColor.LIGHT_GRAY);
                        dataTable.addCell(cell);

                        paragraph = new Paragraph(cikkMr.getAsString("name").trim(),textFont);
                        cell = new PdfPCell(paragraph);
                        cell.setHorizontalAlignment(ALIGN_LEFT);
                        cell.setBorderColor(BaseColor.LIGHT_GRAY);
                        dataTable.addCell(cell);

                        int nt = Integer.parseInt(bbejMr.getAsString("bbru")) -
                                 kg1[Integer.parseInt(bbejMr.getAsString("bgon"))];

                        paragraph = new Paragraph(String.format("%6.1f", nt / 10.0), numFont);
                        cell = new PdfPCell(paragraph);
                        cell.setHorizontalAlignment(ALIGN_RIGHT);
                        cell.setBorderColor(BaseColor.LIGHT_GRAY);
                        dataTable.addCell(cell);

                    }
                }
            }

//                    paragraph = new Paragraph(jo.getString("cikk"));
//                    cell = new PdfPCell(paragraph);
//                    cell.setHorizontalAlignment(ALIGN_LEFT);
//                    cell.setBorderColor(BaseColor.LIGHT_GRAY);
//                    dataTable.addCell(cell);
//
//                    paragraph = new Paragraph(jo.getString("name"));
//                    cell = new PdfPCell(paragraph);
//                    cell.setHorizontalAlignment(ALIGN_LEFT);
//                    cell.setBorderColor(BaseColor.LIGHT_GRAY);
//                    dataTable.addCell(cell);
//
//                    paragraph = new Paragraph(jo.getString("cnt"), numFont);
//                    cell = new PdfPCell(paragraph);
//                    cell.setHorizontalAlignment(ALIGN_RIGHT);
//                    cell.setBorderColor(BaseColor.LIGHT_GRAY);
//                    dataTable.addCell(cell);
//
//                    paragraph = new Paragraph(jo.getString("net"), numFont);
//                    cell = new PdfPCell(paragraph);
//                    cell.setHorizontalAlignment(ALIGN_RIGHT);
//                    cell.setBorderColor(BaseColor.LIGHT_GRAY);
//                    dataTable.addCell(cell);

            document.add(dataTable);
            document.close();

            JSONObject action = new JSONObject()
                .put("type", "download")
                .put("downloadId", localFileName)
                .put("fileName", "report.pdf")
                .put("fileType", "application/pdf")
                .put("fileAction", "open");
            rc
                .set("timer", "frequency", 1)
                .set("timer", "value", 1)
                .set("timer", "action", action);
        } catch (Exception e) {
            e.printStackTrace();
            rc.snackErrorMessage("Error creating result");
        }
    }

    public static void tet_action(RequestContext rc) throws Exception {

        try {

            int up;
            try {
                JSONObject status = rc.getValueJson("list");
                up = Integer.parseInt(status.getJSONObject("keys").optString("$1"));
            } catch (Exception e) {
                up = 0;
                rc.snackErrorMessage("Error cheking id");
                return;
            }

            log.info("rep");
            ReportingSnapshot snapshot = getsnapshot(Arrays.asList(
                "VDUPL?",
                "VDAT?",
                "VDAT.vaje",
                "VDAT.vabl",
                "VDAT.vaup",
                "GONGY.kg1"
            ));

            Integer aVdupl = snapshot.sizes.get("VDUPL?");
            Integer aVdat = snapshot.sizes.get("VDAT?");

            char[] vaup = (char[]) snapshot.data.get("VDAT.vaup");
            byte[] vaje = (byte[]) snapshot.data.get("VDAT.vaje");
            int[] vabl = (int[]) snapshot.data.get("VDAT.vabl");
            int[] kg1 = (int[]) snapshot.data.get("GONGY.kg1");

            Set<Integer> bla = new TreeSet<>();
            Map<String, Integer> grp = new TreeMap<>();
            int[] sbl = new int[5000];
            int[] snt = new int[5000];

            DatabaseTable cikkTable = tables.get("CIKK");
            Record cikkRecord = new Record(cikkTable);
            MemoryRecord cikkMr;

            DatabaseTable bbejTable = tables.get("BBEJ");
            Record bbejRecord = new Record(bbejTable);
            MemoryRecord bbejMr;

            DatabaseTable bkiaTable = tables.get("BKIA");
            Record bkiaRecord = new Record(bkiaTable);
            MemoryRecord bkiaMr;

            int bl;

            log.info("calc");
            for (int i = 1; i <= aVdat; i++) {
                char u = vaup[i];
                if (u != up) {
                    continue;
                }
                if (vaje[i] == 'V') {
                    bl = vabl[i];
                    if (!bla.contains(bl)) {

                        long now = System.currentTimeMillis();
                        bla.add(bl);

                        bbejRecord.setField("bkod", bl);
                        int posBl = bbejRecord.findLast(1, Arrays.asList("bkod"));
                        if (posBl < 1) {
                            continue;
                        }
                        bbejMr = bbejTable.getMemoryRecord(posBl, 1);

                        String k = bbejMr.getAsString("tetl") + "\t"
                                   + String.format("%04d", Integer.parseInt(bbejMr.getAsString("bcik")));
                        int idx = grp.computeIfAbsent(k, v -> grp.size());

                        snt[idx] += Integer.parseInt(bbejMr.getAsString("bbru")) -
                                    kg1[Integer.parseInt(bbejMr.getAsString("bgon"))];
                        sbl[idx]++;

                    }
                }
            }
            log.info("pdf");

            String IMG = "workdir/fbzdemo/resources/fbz_logo_small.png";
            Font catFont = new Font(Font.FontFamily.TIMES_ROMAN, 18,
                                    Font.BOLD);

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            Calendar cal = Calendar.getInstance();
            String time = dateFormat.format(cal.getTime());

            String fileName = "generated-" + time + String.format("_%03d", getNextTempId() % 1000) + ".pdf";
            String localFileName = workdir + "/temp/" + fileName;

            Document document = new Document();
            //Document document = new Document(PageSize.LETTER.rotate());

            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(localFileName));
            document.open();

            PdfPTable titleTable = new PdfPTable(2);
            titleTable.setWidths(new int[]{10, 90});
            titleTable.setTotalWidth(100);
            titleTable.setWidthPercentage(100);
            Image img = Image.getInstance(IMG);
            PdfPCell cellLogo = new PdfPCell(img, true);
            cellLogo.setBorderWidth(0);
            titleTable.addCell(cellLogo);
            Paragraph title = new Paragraph("Visszavételezés vonalkód feltöltéssel: " + up, catFont);
            PdfPCell cellTitle = new PdfPCell(title);
            cellTitle.setHorizontalAlignment(ALIGN_CENTER);
            cellTitle.setVerticalAlignment(ALIGN_MIDDLE);
            cellTitle.setBorderWidth(0);
            titleTable.addCell(cellTitle);
            document.add(titleTable);
            document.add(new Paragraph(" "));

            PdfPCell cell;
            Paragraph paragraph;

            PdfPTable dataTable = new PdfPTable(5);
            dataTable.setWidths(new int[]{11, 4, 30, 6, 6});
            dataTable.setTotalWidth(11 + 4 + 30 + 6 + 6);
            dataTable.setWidthPercentage(100);

            paragraph = new Paragraph("Tételszám");
            cell = new PdfPCell(paragraph);
            cell.setHorizontalAlignment(ALIGN_LEFT);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            dataTable.addCell(cell);

            paragraph = new Paragraph("Cikk");
            cell = new PdfPCell(paragraph);
            cell.setHorizontalAlignment(ALIGN_LEFT);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            dataTable.addCell(cell);

            paragraph = new Paragraph("Cikk megnevezése");
            cell = new PdfPCell(paragraph);
            cell.setHorizontalAlignment(ALIGN_LEFT);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            dataTable.addCell(cell);

            paragraph = new Paragraph("Bála db");
            cell = new PdfPCell(paragraph);
            cell.setHorizontalAlignment(ALIGN_RIGHT);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            dataTable.addCell(cell);

            paragraph = new Paragraph("Nettó Kg");
            cell = new PdfPCell(paragraph);
            cell.setHorizontalAlignment(ALIGN_RIGHT);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            dataTable.addCell(cell);

            Font numFont = new Font(Font.FontFamily.COURIER);

            log.info("print");
            for (String k : grp.keySet()) {
                int i = grp.get(k);
                String[] f = k.split("\t");

                cikkRecord.setField("ckod", (char) Integer.parseInt(f[1]));
                int posCk = cikkRecord.findLast(1, Arrays.asList("ckod"));
                if (posCk < 1) {
                    continue;
                }
                cikkMr = cikkTable.getMemoryRecord(posCk, 1);

                paragraph = new Paragraph(f[0]);
                cell = new PdfPCell(paragraph);
                cell.setHorizontalAlignment(ALIGN_LEFT);
                cell.setBorderColor(BaseColor.LIGHT_GRAY);
                dataTable.addCell(cell);

                paragraph = new Paragraph(f[1]);
                cell = new PdfPCell(paragraph);
                cell.setHorizontalAlignment(ALIGN_LEFT);
                cell.setBorderColor(BaseColor.LIGHT_GRAY);
                dataTable.addCell(cell);

                paragraph = new Paragraph(cikkMr.getAsString("name").trim());
                cell = new PdfPCell(paragraph);
                cell.setHorizontalAlignment(ALIGN_LEFT);
                cell.setBorderColor(BaseColor.LIGHT_GRAY);
                dataTable.addCell(cell);

                paragraph = new Paragraph(String.valueOf(sbl[i]), numFont);
                cell = new PdfPCell(paragraph);
                cell.setHorizontalAlignment(ALIGN_RIGHT);
                cell.setBorderColor(BaseColor.LIGHT_GRAY);
                dataTable.addCell(cell);

                paragraph = new Paragraph(String.format("%6.1f", snt[i] / 10.0), numFont);
                cell = new PdfPCell(paragraph);
                cell.setHorizontalAlignment(ALIGN_RIGHT);
                cell.setBorderColor(BaseColor.LIGHT_GRAY);
                dataTable.addCell(cell);

            }
            document.add(dataTable);
            document.close();

            JSONObject action = new JSONObject()
                .put("type", "download")
                .put("downloadId", localFileName)
                .put("fileName", "report.pdf")
                .put("fileType", "application/pdf")
                .put("fileAction", "open");
            JSONArray actionArray = new JSONArray().put(action);
            JSONObject response = new JSONObject().put("startAction", actionArray);

            rc.setResponseFromJson(response);

        } catch (Exception e) {
            e.printStackTrace();
            rc.snackErrorMessage("Error creating result");
        }

    }

    public static void tetx_action(RequestContext rc) throws Exception {

        try {

            int up;
            try {
                JSONObject status = rc.getValueJson("list");
                up = Integer.parseInt(status.getJSONObject("keys").optString("$1"));
            } catch (Exception e) {
                up = 0;
                rc.snackErrorMessage("Error cheking id");
                return;
            }

            log.info("rep");
            ReportingSnapshot snapshot = getsnapshot(Arrays.asList(
                "VDUPL?",
                "VDAT?",
                "VDAT.vaje",
                "VDAT.vabl",
                "VDAT.vaup",
                "GONGY.kg1"
            ));

            Integer aVdupl = snapshot.sizes.get("VDUPL?");
            Integer aVdat = snapshot.sizes.get("VDAT?");

            char[] vaup = (char[]) snapshot.data.get("VDAT.vaup");
            byte[] vaje = (byte[]) snapshot.data.get("VDAT.vaje");
            int[] vabl = (int[]) snapshot.data.get("VDAT.vabl");
            int[] kg1 = (int[]) snapshot.data.get("GONGY.kg1");

            Set<Integer> bla = new TreeSet<>();
            Map<String, Integer> grp = new TreeMap<>();
            int[] sbl = new int[5000];
            int[] snt = new int[5000];

            DatabaseTable cikkTable = tables.get("CIKK");
            Record cikkRecord = new Record(cikkTable);
            MemoryRecord cikkMr;

            DatabaseTable bbejTable = tables.get("BBEJ");
            Record bbejRecord = new Record(bbejTable);
            MemoryRecord bbejMr;

            DatabaseTable bkiaTable = tables.get("BKIA");
            Record bkiaRecord = new Record(bkiaTable);
            MemoryRecord bkiaMr;

            int bl;

            log.info("calc");
            for (int i = 1; i <= aVdat; i++) {
                char u = vaup[i];
                if (u != up) {
                    continue;
                }
                if (vaje[i] == 'V') {
                    bl = vabl[i];
                    if (!bla.contains(bl)) {

                        long now = System.currentTimeMillis();
                        bla.add(bl);

                        bbejRecord.setField("bkod", bl);
                        int posBl = bbejRecord.findLast(1, Arrays.asList("bkod"));
                        if (posBl < 1) {
                            continue;
                        }
                        bbejMr = bbejTable.getMemoryRecord(posBl, 1);

                        String k = bbejMr.getAsString("tetl") + "\t"
                                   + String.format("%04d", Integer.parseInt(bbejMr.getAsString("bcik")));
                        int idx = grp.computeIfAbsent(k, v -> grp.size());

                        snt[idx] += Integer.parseInt(bbejMr.getAsString("bbru")) -
                                    kg1[Integer.parseInt(bbejMr.getAsString("bgon"))];
                        sbl[idx]++;

                    }
                }
            }
            log.info("xls");

            Workbook workbook = new XSSFWorkbook();
            CreationHelper createHelper = workbook.getCreationHelper();
            Sheet sheet = workbook.createSheet("data");

            // Create a Font for styling header cells
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            //headerFont.setFontHeightInPoints((short) 14);
            headerFont.setColor(IndexedColors.BLUE.getIndex());

            // Create a CellStyle with the font
            CellStyle headerCellStyleL = workbook.createCellStyle();
            headerCellStyleL.setFont(headerFont);
            CellStyle headerCellStyleR = workbook.createCellStyle();
            headerCellStyleR.setFont(headerFont);
            headerCellStyleR.setAlignment(HorizontalAlignment.RIGHT);

            Row headerRow = sheet.createRow(0);

            Cell cell0 = headerRow.createCell(0);
            cell0.setCellValue("Tételszám");
            cell0.setCellStyle(headerCellStyleL);

            Cell cell1 = headerRow.createCell(1);
            cell1.setCellValue("Cikkszám");
            cell1.setCellStyle(headerCellStyleL);

            Cell cell2 = headerRow.createCell(2);
            cell2.setCellValue("Cikk megnevezése");
            cell2.setCellStyle(headerCellStyleL);

            Cell cell3 = headerRow.createCell(3);
            cell3.setCellValue("Bála db");
            cell3.setCellStyle(headerCellStyleR);

            Cell cell4 = headerRow.createCell(4);
            cell4.setCellValue("Nettó kg");
            cell4.setCellStyle(headerCellStyleR);
            int rowNum = 1;

            CellStyle fixed1style = workbook.createCellStyle();
            DataFormat df = workbook.createDataFormat();
            fixed1style.setDataFormat(df.getFormat("0.0"));
            fixed1style.setAlignment(HorizontalAlignment.RIGHT);

            log.info("print");
            for (String k : grp.keySet()) {
                int i = grp.get(k);
                String[] f = k.split("\t");
                cikkRecord.setField("ckod", (char) Integer.parseInt(f[1]));
                int posCk = cikkRecord.findLast(1, Arrays.asList("ckod"));
                if (posCk < 1) {
                    continue;
                }
                cikkMr = cikkTable.getMemoryRecord(posCk, 1);

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(f[0]);

                row.createCell(1).setCellValue(f[1]);
                row.getCell(1).setCellType(Cell.CELL_TYPE_STRING);

                row.createCell(2).setCellValue(cikkMr.getAsString("name").trim());
                row.createCell(3).setCellValue(sbl[i]);
                row.createCell(4).setCellValue((double) snt[i] / 10);
                //row.createCell(4).setCellValue(11.22);
                row.getCell(4).setCellStyle(fixed1style);

            }

            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            Calendar cal = Calendar.getInstance();
            String time = dateFormat.format(cal.getTime());
            String fileName = "generated-" + time + String.format("_%03d", getNextTempId() % 1000) + ".xlsx";
            String localFileName = workdir + "/temp/" + fileName;

            FileOutputStream fileOut = new FileOutputStream(localFileName);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();

//            Document document = new Document();
//            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(localFileName));
//            document.open();
//            Paragraph title = new Paragraph("Visszavételezés vonalkód feltöltéssel: " + up, catFont);

            JSONObject action = new JSONObject()
                .put("type", "download")
                .put("downloadId", localFileName)
                .put("fileName", fileName)
                .put("fileType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .put("fileAction", "download");
            JSONArray actionArray = new JSONArray().put(action);
            JSONObject response = new JSONObject().put("startAction", actionArray);

            rc.setResponseFromJson(response);

        } catch (Exception e) {
            e.printStackTrace();
            rc.snackErrorMessage("Error creating result");
        }

    }

    public static void export_action(RequestContext rc) throws Exception {

        String tableName = rc.getScreenVars().getString("list");
        int myJob = createJob(new JobConfig("Excel Export: " + rc.getValueString("screenTitle"))
                                  .putParam("handler", "tableExportXLS")
                                  .putParam("table", tableName)
                                  .putParam("sessionInfo", rc.getSessionInfo())
        );
        rc.getScreenVars().put("jobid", myJob);
        rc
            .startBlockingPoll()
            .set("groupid", "isLoading", true);
    }

    public static void export_poll(RequestContext rc) throws Exception {
        int jobId = rc.getScreenVars().getInt("jobid");
        String status = getJodStatus(jobId);
        if (status.startsWith("*")) {
            rc
                .setValue(rc.event + "@poll", 0)
                .set("groupid", "isLoading", false);
            if (status.equals("*Success")) {
                rc.addDownloadStartAction((String) jobConfigs.get(jobId).config.get("localFileName"));
            } else {
                rc.snackErrorMessage("Error exporting data");
            }
        }
    }

}
