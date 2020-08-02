package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.IndexEngine.findAbsolutePosition;
import static com.teamadmino.admino_backend.server.database.IndexEngine.findAbsolutePositionFirst;
import static com.teamadmino.admino_backend.server.database.IndexEngine.findIndexedInterval;
import static com.teamadmino.admino_backend.server.database.ReportingSnapshot.getsnapshot;
import static com.teamadmino.admino_backend.server.database.ServerMain.tables;
import static com.teamadmino.admino_backend.server.http.RequestContext.getTempTableName;
import static com.teamadmino.admino_backend.server.processing.JobProcessor.addJobLog;
import static com.teamadmino.admino_backend.server.utils.ExcelUtils.getTimeStringFileName;
import static com.teamadmino.application.fbzprod.utils.Utils.fmt52;
import static com.teamadmino.application.fbzprod.utils.Utils.fmt62;
import static com.teamadmino.application.fbzprod.utils.Utils.fmtDate;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.teamadmino.admino_backend.server.database.DatabaseSchemaDefinition;
import com.teamadmino.admino_backend.server.database.DatabaseTable;
import com.teamadmino.admino_backend.server.database.MemoryRecord;
import com.teamadmino.admino_backend.server.database.Record;
import com.teamadmino.admino_backend.server.database.ReportingSnapshot;
import com.teamadmino.admino_backend.server.processing.JobConfig;
import com.teamadmino.admino_backend.server.utils.Interval;
import com.teamadmino.admino_backend.server.utils.PdfDocument;
import com.teamadmino.admino_backend.server.utils.StringVector;

import java.util.Arrays;

public class JobFeldolgozasElszamolas {

    public static void feldolgozasElszamolasSingleV0(int jobId, JobConfig jobConfig) throws Exception {

        //feld:kod,fbev,fkia,date,tula,szal,1;gongy:kg1;kbiz:kod,date,1; bbiz:kod,date,1; bkia:brds,nyer,aear,bcik,bzsz,bkod,bbru,bgon,tetl,btul,bszl,4; vizsger:wtet,wtyp,wsum,1; tulajd:name,mn,kod,1; szallito:name,mn,kod,1; cikk:name,ckod,1; bbej:brds,bcik,bzsz,bkod,bbru,bgon,tetl,btul,bszl,nyer,4
        ReportingSnapshot snapshot = getsnapshot(Arrays.asList(
            "BBEJ#4",
            "BBEJ.bbru",
            "BBEJ.bcik",
            "BBEJ.bgon",
            "BBEJ.bkod",
            "BBEJ.brds",
            "BBEJ.bszl",
            "BBEJ.btul",
            "BBEJ.bzsz",
            "BBEJ.nyer",
            "BBEJ.tetl",
            "BBEJ?",
            "BBIZ#1",
            "BBIZ.date",
            "BBIZ.kod",
            "BBIZ?",
            "BKIA#4",
            "BKIA.aear",
            "BKIA.bbru",
            "BKIA.bcik",
            "BKIA.bgon",
            "BKIA.bkod",
            "BKIA.brds",
            "BKIA.bszl",
            "BKIA.btul",
            "BKIA.bzsz",
            "BKIA.nyer",
            "BKIA.tetl",
            "BKIA?",
            "CIKK#1",
            "CIKK.ckod",
            "CIKK.name",
            "CIKK?",
            "FELD#1",
            "FELD.date",
            "FELD.fbev",
            "FELD.fkia",
            "FELD.kod",
            "FELD.szal",
            "FELD.tula",
            "FELD?",
            "GONGY.kg1",
            "GONGY?",
            "KBIZ#1",
            "KBIZ.date",
            "KBIZ.kod",
            "KBIZ?",
            "SZALLITO#1",
            "SZALLITO.kod",
            "SZALLITO.mn",
            "SZALLITO.name",
            "SZALLITO?",
            "TULAJD#1",
            "TULAJD.kod",
            "TULAJD.mn",
            "TULAJD.name",
            "TULAJD?",
            "VIZSGER#1",
            "VIZSGER.wsum",
            "VIZSGER.wtet",
            "VIZSGER.wtyp",
            "VIZSGER?"
        ));
        long now = System.currentTimeMillis();
        addJobLog(jobId, "Data loaded", (byte) 0);

        PdfDocument pdf = new PdfDocument(PageSize.A4.rotate());
        pdf.document.setMargins(18, 18, 18, 18);

        String tempTableName = getTempTableName("resultTableName");
        DatabaseSchemaDefinition databaseSchema;
        DatabaseTable tempTable;

        databaseSchema = new DatabaseSchemaDefinition(tempTableName, "temp", "schema");
        databaseSchema
            .addField("tu", "tu", "int8", "number", 3)
            .addField("sz", "sz", "int8", "number", 3)
            .addField("ck", "ck", "int16", "number", 4)
            .addField("ts", "ts", "string", "string", 11)
            .addField("nt", "nt", "int32", "fixed1", 10)
            .addField("bl", "bl", "int32", "number", 10)
            .addField("ph", "ph", "int16", "fixed2", 10)
            .addField("ny", "ny", "int64", "fixed2", 10)
            .addField("rd", "rd", "int64", "fixed3", 10)
            .addIndex("tu,sz,ck,ts");
        tempTable = new DatabaseTable("_test", databaseSchema);

        if (jobConfig.config.containsKey("id")) {
            elszamolas((int) jobConfig.config.get("id"), snapshot, pdf, tempTable);
        } else {
            DatabaseTable list = tables.get((String) jobConfig.config.get("list"));
            for (int f = 1; f <= list.recordCount; f++) {
                elszamolas(list.getMemoryRecord(f, 1).getInt("fe"), snapshot, pdf, tempTable);
                if (System.currentTimeMillis() - now > 1000) {
                    addJobLog(jobId, "Processing: " + f + " of " + list.recordCount,
                              (byte) ((f * 100 - 1) / list.recordCount));
                }
            }
        }
        addJobLog(jobId, "Calculation done, writing output", (byte) 99);

        pdf.document.close();
        jobConfig.config.put("localFileName", pdf.localFileName);
        jobConfig.config.put("fileName", pdf.fileName);
        addJobLog(jobId, "Done: " + pdf.fileName, (byte) 100);
    }

    private static void elszamolas(int id, ReportingSnapshot snapshot, PdfDocument pdf,
                                   DatabaseTable tempTable) throws Exception {

        int[] iBBEJ4 = snapshot.indexes.get("BBEJ#4");
        char[] BBEJ_bbru = (char[]) snapshot.data.get("BBEJ.bbru");
        char[] BBEJ_bcik = (char[]) snapshot.data.get("BBEJ.bcik");
        byte[] BBEJ_bgon = (byte[]) snapshot.data.get("BBEJ.bgon");
        int[] BBEJ_bkod = (int[]) snapshot.data.get("BBEJ.bkod");
        char[] BBEJ_brds = (char[]) snapshot.data.get("BBEJ.brds");
        byte[] BBEJ_bszl = (byte[]) snapshot.data.get("BBEJ.bszl");
        byte[] BBEJ_btul = (byte[]) snapshot.data.get("BBEJ.btul");
        int[] BBEJ_bzsz = (int[]) snapshot.data.get("BBEJ.bzsz");
        int[] BBEJ_nyer = (int[]) snapshot.data.get("BBEJ.nyer");
        StringVector BBEJ_tetl = (StringVector) snapshot.data.get("BBEJ.tetl");
        int aBBEJ = snapshot.sizes.get("BBEJ?");
        int[] iBBIZ1 = snapshot.indexes.get("BBIZ#1");
        int[] BBIZ_date = (int[]) snapshot.data.get("BBIZ.date");
        int[] BBIZ_kod = (int[]) snapshot.data.get("BBIZ.kod");
        int aBBIZ = snapshot.sizes.get("BBIZ?");
        int[] iBKIA4 = snapshot.indexes.get("BKIA#4");
        int[] BKIA_aear = (int[]) snapshot.data.get("BKIA.aear");
        char[] BKIA_bbru = (char[]) snapshot.data.get("BKIA.bbru");
        char[] BKIA_bcik = (char[]) snapshot.data.get("BKIA.bcik");
        byte[] BKIA_bgon = (byte[]) snapshot.data.get("BKIA.bgon");
        int[] BKIA_bkod = (int[]) snapshot.data.get("BKIA.bkod");
        char[] BKIA_brds = (char[]) snapshot.data.get("BKIA.brds");
        byte[] BKIA_bszl = (byte[]) snapshot.data.get("BKIA.bszl");
        byte[] BKIA_btul = (byte[]) snapshot.data.get("BKIA.btul");
        int[] BKIA_bzsz = (int[]) snapshot.data.get("BKIA.bzsz");
        int[] BKIA_nyer = (int[]) snapshot.data.get("BKIA.nyer");
        StringVector BKIA_tetl = (StringVector) snapshot.data.get("BKIA.tetl");
        int aBKIA = snapshot.sizes.get("BKIA?");
        int[] iCIKK1 = snapshot.indexes.get("CIKK#1");
        char[] CIKK_ckod = (char[]) snapshot.data.get("CIKK.ckod");
        StringVector CIKK_name = (StringVector) snapshot.data.get("CIKK.name");
        int aCIKK = snapshot.sizes.get("CIKK?");
        int[] iFELD1 = snapshot.indexes.get("FELD#1");
        int[] FELD_date = (int[]) snapshot.data.get("FELD.date");
        int[] FELD_fbev = (int[]) snapshot.data.get("FELD.fbev");
        int[] FELD_fkia = (int[]) snapshot.data.get("FELD.fkia");
        int[] FELD_kod = (int[]) snapshot.data.get("FELD.kod");
        byte[] FELD_szal = (byte[]) snapshot.data.get("FELD.szal");
        byte[] FELD_tula = (byte[]) snapshot.data.get("FELD.tula");
        int aFELD = snapshot.sizes.get("FELD?");
        int[] GONGY_kg1 = (int[]) snapshot.data.get("GONGY.kg1");
        int aGONGY = snapshot.sizes.get("GONGY?");
        int[] iKBIZ1 = snapshot.indexes.get("KBIZ#1");
        int[] KBIZ_date = (int[]) snapshot.data.get("KBIZ.date");
        int[] KBIZ_kod = (int[]) snapshot.data.get("KBIZ.kod");
        int aKBIZ = snapshot.sizes.get("KBIZ?");
        int[] iSZALLITO1 = snapshot.indexes.get("SZALLITO#1");
        byte[] SZALLITO_kod = (byte[]) snapshot.data.get("SZALLITO.kod");
        StringVector SZALLITO_mn = (StringVector) snapshot.data.get("SZALLITO.mn");
        StringVector SZALLITO_name = (StringVector) snapshot.data.get("SZALLITO.name");
        int aSZALLITO = snapshot.sizes.get("SZALLITO?");
        int[] iTULAJD1 = snapshot.indexes.get("TULAJD#1");
        byte[] TULAJD_kod = (byte[]) snapshot.data.get("TULAJD.kod");
        StringVector TULAJD_mn = (StringVector) snapshot.data.get("TULAJD.mn");
        StringVector TULAJD_name = (StringVector) snapshot.data.get("TULAJD.name");
        int aTULAJD = snapshot.sizes.get("TULAJD?");
        int[] iVIZSGER1 = snapshot.indexes.get("VIZSGER#1");
        char[] VIZSGER_wsum = (char[]) snapshot.data.get("VIZSGER.wsum");
        StringVector VIZSGER_wtet = (StringVector) snapshot.data.get("VIZSGER.wtet");
        byte[] VIZSGER_wtyp = (byte[]) snapshot.data.get("VIZSGER.wtyp");
        int aVIZSGER = snapshot.sizes.get("VIZSGER?");

        int fap = findAbsolutePositionFirst(id, FELD_kod, iFELD1);
        if (fap < 0) {
            return;
        }
        if (pdf.document.getPageNumber() == 0) {
            pdf.document.newPage();
        }

        Record record = new Record(tempTable);

        int sntk = 0;
        int sblk = 0;
        int sntb = 0;
        int sblb = 0;
        long snyk = 0;
        long snyb = 0;
        long srdk = 0;
        long srdb = 0;

        pdf.document.add(new Paragraph(
            "Feldolgozás elszámolas: "
            + fmt52(id) + " ~ "
            + fmtDate(FELD_date[fap])
            , pdf.titleFont));

        pdf.document.add(new Paragraph(
            "\n"
            + String.format("Tulajdonos:  %02d  ", FELD_tula[fap] & 0xFF)
            + TULAJD_mn.getAsString(findAbsolutePositionFirst(FELD_tula[fap], TULAJD_kod, iTULAJD1)) + "  "
            + TULAJD_name.getAsString(findAbsolutePositionFirst(FELD_tula[fap], TULAJD_kod, iTULAJD1)) + "\n"
            + String.format("Szállító:   %03d  ", FELD_szal[fap] & 0xFF)
            + SZALLITO_mn.getAsString(findAbsolutePositionFirst(FELD_szal[fap], SZALLITO_kod, iSZALLITO1)) + "  "
            + SZALLITO_name.getAsString(findAbsolutePositionFirst(FELD_szal[fap], SZALLITO_kod, iSZALLITO1))
            , pdf.numFont));

        pdf.document.add(new Paragraph("\n", pdf.numFont));
        tempTable.recordCount = 0;
        int kap = findAbsolutePositionFirst(FELD_fkia[fap], KBIZ_kod, iKBIZ1);
        do {
            if (kap < 0) {
                pdf.document.add(new Paragraph("Nem található kiadási bizonylat"));
                break;
            }
            pdf.document.add(new Paragraph(
                "Kiadási bizonylat: "
                + fmt62(KBIZ_kod[kap]) + " ~ "
                + fmtDate(KBIZ_date[kap])
            ));
            pdf.document.add(new Paragraph("\n", pdf.numFont));
            Interval kbi = findIndexedInterval(FELD_fkia[fap], FELD_fkia[fap], BKIA_bzsz, iBKIA4);
            if (kbi.end == -1) {
                pdf.document.add(new Paragraph("Nincs bála rögzítve"));
                break;
            }

            pdf.document.add(new Paragraph(
                "" +
                "Tu Sz Cikk Megnevezés                     Tételszám  Bála   Nettó   Súly%          Pehely Nyers értek     RDS%\n"
                +
                "--------------------------------------------------------------------------------------------------------------\n"
                +
//              "12 12 1234 123456789012345678901234567890 12345678901 ### #####.# ####.##         ####.## #########.#  ###.##%\n"
                "",
                pdf.numFont));

            for (int ip = kbi.start; ip <= kbi.end; ip++) {
                int ap = iBKIA4[ip];

                char ph;
                int nt = BKIA_bbru[ap] - GONGY_kg1[BKIA_bgon[ap]];
                long ny = BKIA_aear[ap] == -1 ? BKIA_nyer[ap] : BKIA_aear[ap] * (long) nt;
                long rd = BKIA_brds[ap] * (long) nt;

                Interval vi = findIndexedInterval(BKIA_tetl.select(ap), BKIA_tetl.select(ap), VIZSGER_wtet, iVIZSGER1);
                int av = findAbsolutePositionFirst((byte) 11, VIZSGER_wtyp, iVIZSGER1, vi);
                ph = av > 0 ? VIZSGER_wsum[av] : 0;

                int pos = record
                    .setField("tu", BKIA_btul[ap])
                    .setField("sz", BKIA_bszl[ap])
                    .setField("ck", BKIA_bcik[ap])
                    .setField("ts", BKIA_tetl.select(ap))
                    .setField("nt", nt)
                    .setField("bl", 1)
                    .setField("ph", ph)
                    .setField("ny", ny)
                    .setField("rd", rd)
                    .insertOrFind();
                if (pos < 0) {
                    MemoryRecord memoryRecord = tempTable.getMemoryRecord(-pos);
                    memoryRecord.setField("bl", memoryRecord.getInt("bl") + 1);
                    memoryRecord.setField("nt", memoryRecord.getInt("nt") + nt);
                    memoryRecord.setField("ny", memoryRecord.getLong("ny") + ny);
                    memoryRecord.setField("rd", memoryRecord.getLong("rd") + rd);
                }
                sblk++;
                sntk += nt;
                snyk += ny;
                srdk += rd;
            }

            for (int i = 1; i <= tempTable.recordCount; i++) {
                MemoryRecord memoryRecord = tempTable.getMemoryRecord(tempTable.indexes.get(1)[i]);
                double ph = (double) memoryRecord.getChar("ph") / 100;
                pdf.document.add(new Paragraph(
                    ""
                    + TULAJD_mn
                        .getAsString(findAbsolutePosition(memoryRecord.getByte("tu"), TULAJD_kod, iTULAJD1, true))
                    + " "
                    + SZALLITO_mn
                        .getAsString(findAbsolutePosition(memoryRecord.getByte("sz"), SZALLITO_kod, iSZALLITO1, true))
                    + " "
                    + String.format("%04d", (int) memoryRecord.getChar("ck"))
                    + " "
                    + CIKK_name.getAsString(findAbsolutePositionFirst(memoryRecord.getChar("ck"), CIKK_ckod, iCIKK1))
                    + " "
                    + memoryRecord.getAsString("ts")
                    + " "
                    + String.format("%3d", memoryRecord.getInt("bl"))
                    + " "
                    + String.format("%7.1f", (double) memoryRecord.getInt("nt") / 10)
                    + " "
                    + String.format("%7.2f", 100 * (double) memoryRecord.getInt("nt") / sntk)
                    + " "
                    + "        "
                    + ((ph == 0.0) ? "       " : String.format("%7.2f", ph))
                    + " "
                    + String.format("%11.2f", (double) memoryRecord.getLong("ny") / 100)
                    + "  "
                    + String.format("%6.2f%%",
                                    (double) memoryRecord.getLong("rd") / (double) memoryRecord.getInt("nt") / 100)
                    , pdf.numFont));
            }
            pdf.document.add(new Paragraph(
                "" +
                "--------------------------------------------------------------------------------------------------------------\n"
                + String.format("%2d", tempTable.recordCount)
                + " tétel   Összesen =                                 "
                + String.format("%3d", sblk)
                + " "
                + String.format("%7.1f", (double) sntk / 10)
                + "  100.00"
                + " "
                + "        "
                + "        "
                + String.format("%11.2f", (double) snyk / 100)
                + "  "
                + String.format("%6.2f%%",
                                srdk / (double) sntk / 100)
                , pdf.numTotalFont));

        } while (false);

        pdf.document.add(new Paragraph("\n", pdf.numFont));
        tempTable.recordCount = 0;
        int bap = findAbsolutePositionFirst(FELD_fbev[fap], BBIZ_kod, iBBIZ1);
        do {
            if (bap < 0) {
                pdf.document.add(new Paragraph("Nem található bevételi bizonylat"));
                break;
            }
            pdf.document.add(new Paragraph(
                "Bevételi bizonylat: "
                + fmt62(BBIZ_kod[bap]) + " ~ "
                + fmtDate(BBIZ_date[bap])
            ));
            pdf.document.add(new Paragraph("\n", pdf.numFont));
            Interval kbi = findIndexedInterval(FELD_fbev[fap], FELD_fbev[fap], BBEJ_bzsz, iBBEJ4);
            if (kbi.end == -1) {
                pdf.document.add(new Paragraph("Nincs bála rögzítve"));
                break;
            }

            pdf.document.add(new Paragraph(
                "" +
                "Tu Sz Cikk Megnevezés                     Tételszám  Bála   Nettó   Kiad%    Bev%  Pehely Nyers értek     RDS%\n"
                +
                "--------------------------------------------------------------------------------------------------------------\n"
                +
                "",
                pdf.numFont));

            for (int ip = kbi.start; ip <= kbi.end; ip++) {
                int ap = iBBEJ4[ip];

                char ph;
                int nt = BBEJ_bbru[ap] - GONGY_kg1[BBEJ_bgon[ap]];
                long ny = BBEJ_nyer[ap];
                long rd = BBEJ_brds[ap] * (long) nt;

                Interval vi = findIndexedInterval(BBEJ_tetl.select(ap), BBEJ_tetl.select(ap), VIZSGER_wtet, iVIZSGER1);
                int av = findAbsolutePositionFirst((byte) 11, VIZSGER_wtyp, iVIZSGER1, vi);
                ph = av > 0 ? VIZSGER_wsum[av] : 0;

                int pos = record
                    .setField("tu", BBEJ_btul[ap])
                    .setField("sz", BBEJ_bszl[ap])
                    .setField("ck", BBEJ_bcik[ap])
                    .setField("ts", BBEJ_tetl.select(ap))
                    .setField("nt", nt)
                    .setField("bl", 1)
                    .setField("ph", ph)
                    .setField("ny", ny)
                    .setField("rd", rd)
                    .insertOrFind();
                if (pos < 0) {
                    MemoryRecord memoryRecord = tempTable.getMemoryRecord(-pos);
                    memoryRecord.setField("bl", memoryRecord.getInt("bl") + 1);
                    memoryRecord.setField("nt", memoryRecord.getInt("nt") + nt);
                    memoryRecord.setField("ny", memoryRecord.getLong("ny") + ny);
                    memoryRecord.setField("rd", memoryRecord.getLong("rd") + rd);
                }
                sblb++;
                sntb += nt;
                snyb += ny;
                srdb += rd;
            }

            for (int i = 1; i <= tempTable.recordCount; i++) {
                MemoryRecord memoryRecord = tempTable.getMemoryRecord(tempTable.indexes.get(1)[i]);
                double ph = (double) memoryRecord.getChar("ph") / 100;
                pdf.document.add(new Paragraph(
                    ""
                    + TULAJD_mn
                        .getAsString(findAbsolutePosition(memoryRecord.getByte("tu"), TULAJD_kod, iTULAJD1, true))
                    + " "
                    + SZALLITO_mn
                        .getAsString(findAbsolutePosition(memoryRecord.getByte("sz"), SZALLITO_kod, iSZALLITO1, true))
                    + " "
                    + String.format("%04d", (int) memoryRecord.getChar("ck"))
                    + " "
                    + CIKK_name.getAsString(findAbsolutePositionFirst(memoryRecord.getChar("ck"), CIKK_ckod, iCIKK1))
                    + " "
                    + memoryRecord.getAsString("ts")
                    + " "
                    + String.format("%3d", memoryRecord.getInt("bl"))
                    + " "
                    + String.format("%7.1f", (double) memoryRecord.getInt("nt") / 10)
                    + " "
                    + (sntk > 0 ? String.format("%7.2f", 100 * (double) memoryRecord.getInt("nt") / sntk) : "       ")
                    + " "
                    + String.format("%7.2f", 100 * (double) memoryRecord.getInt("nt") / sntb)
                    + " "
                    + ((ph == 0.0) ? "       " : String.format("%7.2f", ph))
                    + " "
                    + String.format("%11.2f", (double) memoryRecord.getLong("ny") / 100)
                    + "  "
                    + String.format("%6.2f%%",
                                    (double) memoryRecord.getLong("rd") / (double) memoryRecord.getInt("nt") / 100)
                    , pdf.numFont));
            }
            pdf.document.add(new Paragraph(
                "" +
                "--------------------------------------------------------------------------------------------------------------\n"
                + String.format("%2d", tempTable.recordCount)
                + " tétel   Összesen =                                 "
                + String.format("%3d", sblb)
                + " "
                + String.format("%7.1f", (double) sntb / 10)
                + " "
                + (sntk > 0 ? String.format("%7.2f", 100 * (double) sntb / sntk) : "       ")
                + "  100.00"
                + "        "
                + " "
                + String.format("%11.2f", (double) snyb / 100)
                + "  "
                + String.format("%6.2f%%",
                                srdb / (double) sntb / 100)
                , pdf.numTotalFont));

        } while (false);

        if (sblb > 0 && sblk > 0) {
            pdf.document.add(new Paragraph("\n", pdf.numFont));
            pdf.document.add(new Paragraph(
                ""
                + "           Eltérés  =                                "
                + String.format("%4d", sblb - sblk)
                + " "
                + String.format("%7.1f", (double) (sntb - sntk) / 10)
                + " "
                + (sntk > 0 ? String.format("%7.2f", 100 * (double) (sntb - sntk) / sntk) : "      -")
                , pdf.numTotalFont));
        }

    }

}
