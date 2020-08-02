package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.AnsiHun.string2byteArrayU;
import static com.teamadmino.admino_backend.server.database.ServerMain.endPoints;
import static com.teamadmino.admino_backend.server.database.ServerMain.log;
import static com.teamadmino.admino_backend.server.database.ServerMain.tables;

import com.teamadmino.admino_backend.server.database.DatabaseTable;
import com.teamadmino.admino_backend.server.database.MemoryRecord;
import com.teamadmino.admino_backend.server.database.Record;
import com.teamadmino.admino_backend.server.http.RequestContext;
import com.teamadmino.admino_backend.server.ui.ElementUtils;
import com.teamadmino.admino_backend.server.utils.StringVector;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

public class GenericFilterUtils {

    public static void genericAddFilterAction(RequestContext rc, String... options) throws Exception {
        JSONArray desc = new JSONArray().put("Description");
        for (String item : options) {
            desc.put(item);
        }
        JSONArray data = new JSONArray().put(desc);
        JSONObject args = new JSONObject().put("data", data);
        rc.loadToPopupTest("_admino:generic/selectSimple", args);
    }

    public static void genericDelFilterAction(RequestContext rc) throws Exception {
        DatabaseTable filterTable = tables.get(rc.getScreenVars().getString("filterTableName"));
        if (filterTable.recordCount == 0) {
            rc.snackErrorMessage("A filter tábla üres");
            return;
        }
        new Record(filterTable).delete((int) rc.getTableRowPos("filters"));
        rc.tableForceRefresh("filters");
    }

    public static void genericAddFilterActionOnReturn(RequestContext rc) throws Exception {
        String filterName = rc.getReturnValue().getString("col0");
        JSONObject args = new JSONObject();
        args.put("genericFilterName", filterName);
        switch (filterName) {
            case "Tulajdonos": {
                JSONArray kod = new JSONArray().put("id");
                JSONArray name = new JSONArray().put("name");
                DatabaseTable tulajdonos = tables.get("TULAJD");
                for (int i = 1; i <= tulajdonos.recordCount; i++) {
                    MemoryRecord memoryRecord = tulajdonos.getMemoryRecord(i, 1);
                    kod.put(memoryRecord.getASFormattedString("kod"));
                    name.put(memoryRecord.getASFormattedString("name"));
                }
                JSONArray data = new JSONArray();
                data.put(kod);
                data.put(name);
                args.put("data", data);
                rc.loadToPopupTest2("_admino:generic/selectSimple", args, "getGenericFilterItem");
                break;
            }
            case "Szállító": {
                JSONArray kod = new JSONArray().put("id");
                JSONArray name = new JSONArray().put("name");
                DatabaseTable szallito = tables.get("SZALLITO");
                for (int i = 1; i <= szallito.recordCount; i++) {
                    MemoryRecord memoryRecord = szallito.getMemoryRecord(i, 1);
                    kod.put(memoryRecord.getASFormattedString("kod"));
                    name.put(memoryRecord.getASFormattedString("name"));
                }
                JSONArray data = new JSONArray();
                data.put(kod);
                data.put(name);
                args.put("data", data);
                rc.loadToPopupTest2("_admino:generic/selectSimple", args, "getGenericFilterItem");
                break;
            }
            case "Feldolgozó gép": {
                JSONArray kod = new JSONArray().put("id");
                JSONArray name = new JSONArray().put("name");
                DatabaseTable feldgep = tables.get("FELDGEP");
                for (int i = 1; i <= feldgep.recordCount; i++) {
                    MemoryRecord memoryRecord = feldgep.getMemoryRecord(i);
                    kod.put(String.format("%02d", i));
                    name.put(memoryRecord.getASFormattedString("name"));
                }
                JSONArray data = new JSONArray();
                data.put(kod);
                data.put(name);
                args.put("data", data);
                rc.loadToPopupTest2("_admino:generic/selectSimple", args, "getGenericFilterItem");
                break;
            }
            case "Feldolgozás típus": {
                JSONArray kod = new JSONArray().put("id");
                JSONArray name = new JSONArray().put("name");
                DatabaseTable feldn = tables.get("FELDN");
                for (int i = 1; i <= feldn.recordCount; i++) {
                    MemoryRecord memoryRecord = feldn.getMemoryRecord(i);
                    kod.put(String.valueOf(i - 1));
                    name.put(memoryRecord.getASFormattedString("name"));
                }
                JSONArray data = new JSONArray();
                data.put(kod);
                data.put(name);
                args.put("data", data);
                rc.loadToPopupTest2("_admino:generic/selectSimple", args, "getGenericFilterItem");
                break;
            }
            case "Tételszám kezdete":
            case "Tételszám vége":
            case "Tételszám tartalmazza":
                rc.loadToPopupTest2("fbz_prod:common/getText", args, "getGenericFilterItem");
                break;
            case "Kiadott cikkszám volt benne":
            case "Bevételezett cikkszám volt benne":
            case "Cikkszám":
                args.put("title", "Cikkszám");
                args.put("min", 0);
                args.put("max", 9999);
                args.put("value", 0);
                rc.loadToPopupTest2("fbz_prod:common/getNumber", args, "getGenericFilterItem");
                break;
            case "Feldolgozás száma":
                args.put("title", filterName);
                args.put("min", 0);
                args.put("max", 99999);
                args.put("value", 0);
                rc.loadToPopupTest2("fbz_prod:common/getNumber", args, "getGenericFilterItem");
                break;
            case "Feldolgozás éve":
            case "Bálaszám év":
                args.put("title", filterName + " (utolsó 2 számjegy)");
                args.put("min", 0);
                args.put("max", 99);
                args.put("value", 20);
                rc.loadToPopupTest2("fbz_prod:common/getNumber", args, "getGenericFilterItem");
                break;
            case "Van-e kiadott bála rögzítve?":
            case "Van-e bevételezett bála rögzítve?":
                args.put("title", filterName);
                rc.loadToPopupTest2("fbz_prod:common/getIgenNem", args, "getGenericFilterItem");
                break;
            case "Időszak":
                rc.loadToPopupTest2("fbz_prod:common/getDateInterval", args, "getGenericFilterItem");
                break;
            default:
                rc.snackWarningMessage("Ez a filter opció még fejlesztés alatt van");
                break;
        }
        rc.closeSender();
    }

    public static void getAddFilterItem(RequestContext rc) throws Exception {
        rc.closeSender();
        String filterName = rc.getReturnArgs().getString("genericFilterName");
        DatabaseTable filterTable = tables.get(rc.getScreenVars().getString("filterTableName"));
        switch (filterName) {
            case "Szállító":
            case "Tulajdonos":
            case "Feldolgozó gép":
            case "Feldolgozás típus":
                new Record(filterTable)
                    .setField("field", filterName)
                    .setField("operator", "==")
                    .setField("value", rc.getReturnValue().getString("col0"))
                    .insert();
                break;
            case "Tételszám kezdete":
            case "Tételszám vége":
            case "Tételszám tartalmazza":
                new Record(filterTable)
                    .setField("field", filterName)
                    .setField("operator", "==")
                    .setField("value", rc.getReturnValue().getString("text").toUpperCase())
                    .insert();
                break;
            case "Van-e kiadott bála rögzítve?":
            case "Van-e bevételezett bála rögzítve?":
                new Record(filterTable)
                    .setField("field", filterName)
                    .setField("operator", "==")
                    .setField("value", rc.getReturnValue().getString("text"))
                    .insert();
                break;
            case "Feldolgozás száma":
            case "Kiadott cikkszám volt benne":
            case "Bevételezett cikkszám volt benne":
            case "Feldolgozás éve":
            case "Bálaszám év":
            case "Cikkszám":
                new Record(filterTable)
                    .setField("field", filterName)
                    .setField("operator", "==")
                    .setField("value", rc.getReturnValue().getString("number"))
                    .insert();
                break;
            case "Időszak":
                new Record(filterTable)
                    .setField("field", filterName)
                    .setField("operator", "==")
                    .setField("value", rc.getReturnValue().getString("date1")
                                       + " ~ " + rc.getReturnValue().getString("date2"))
                    .insert();
                break;
            default:
                rc.snackWarningMessage("Ez a filter opció még fejlesztés alatt van");
                break;
        }
        rc
            .set("filters", "value", new JSONObject()
                .put("keys", ElementUtils.tablePositionLast)
                .put("cursorpos", 0))
            .tableForceRefresh("filters")
            .setFocus("filters");
    }

    public static void filterEQ(byte value, byte[] data, boolean[] flags) {
        int records = data.length - 1;
        for (int i = 1; i <= records; i++) {
            if (flags[i] && data[i] != value) {
                flags[i] = false;
            }
        }
    }

    public static void filterModuloEQ(byte value, int modulo, int[] data, boolean[] flags) {
        int records = data.length - 1;
        for (int i = 1; i <= records; i++) {
            if (flags[i] && data[i] % modulo != value) {
                flags[i] = false;
            }
        }
    }

    public static void filterEQ(char value, char[] data, boolean[] flags) {
        int records = data.length - 1;
        for (int i = 1; i <= records; i++) {
            if (flags[i] && data[i] != value) {
                flags[i] = false;
            }
        }
    }

    public static void filterDivEQ(int value, int divider, int[] data, boolean[] flags) {
        int records = data.length - 1;
        for (int i = 1; i <= records; i++) {
            if (flags[i] && data[i] / divider != value) {
                flags[i] = false;
            }
        }
    }

    public static void filterBetween(int min, int max, int[] data, boolean[] flags) {
        int records = data.length - 1;
        for (int i = 1; i <= records; i++) {
            if (flags[i] && data[i] < min || max < data[i]) {
                flags[i] = false;
            }
        }
    }

    public static void filterStartsWithCaseInsensitive(String pattern, StringVector data, boolean[] flags)
        throws Exception {

        if (pattern.length() == 0) {
            return;
        }
        int patternLength = data.recordSize;
        if (pattern.length() > patternLength) {
            Arrays.fill(flags, false);
            return;
        }
        byte[] patternBytes = string2byteArrayU(pattern);
        if (patternBytes.length < patternLength) {
            patternLength = patternBytes.length;
        }

        int records = data.recordCount;
        for (int i = 1; i <= records; i++) {
            if (flags[i]) {
                int base = i * data.recordSize;
                for (int j = 0; j < patternLength; j++) {
                    if (data.data[base + j] != patternBytes[j]) {
                        flags[i] = false;
                        break;
                    }
                }
            }
        }

    }

    public static void filterEndsWithCaseInsensitive(String pattern, StringVector data, boolean[] flags)
        throws Exception {

        if (pattern.length() == 0) {
            return;
        }
        int patternLength = data.recordSize;
        if (pattern.length() > patternLength) {
            Arrays.fill(flags, false);
            return;
        }
        byte[] patternBytes = string2byteArrayU(pattern);
        if (patternBytes.length < patternLength) {
            patternLength = patternBytes.length;
        }

        int records = data.recordCount;
        for (int i = 1; i <= records; i++) {
            if (flags[i]) {
                int base = i * data.recordSize;
                int end;
                for (end = data.recordSize - 1; end > 0; end--) {
                    if (data.data[base + end] != 32) {
                        break;
                    }
                }
                if (end < patternLength - 1) {
                    flags[i] = false;
                } else {
                    end -= patternLength - 1;
                    for (int j = 0; j < patternLength; j++) {
                        if (data.data[base + end + j] != patternBytes[j]) {
                            flags[i] = false;
                            break;
                        }
                    }
                }
            }
        }

    }

    public static void filterContainsCaseInsensitive(String pattern, StringVector data, boolean[] flags)
        throws Exception {

        if (pattern.length() == 0) {
            return;
        }
        int patternLength = data.recordSize;
        if (pattern.length() > patternLength) {
            Arrays.fill(flags, false);
            return;
        }
        byte[] patternBytes = string2byteArrayU(pattern);
        if (patternBytes.length < patternLength) {
            patternLength = patternBytes.length;
        }

        int start;
        int end = data.recordSize - patternLength;
        int records = data.recordCount;
        nextRecord:
        for (int i = 1; i <= records; i++) {
            if (flags[i]) {
                int base = i * data.recordSize;
                nextChar:
                for (start = 0; start <= end; start++) {
                    for (int j = 0; j < patternLength; j++) {
                        if (data.data[base + j] != patternBytes[j]) {
                            base++;
                            continue nextChar;
                        }
                    }
                    continue nextRecord; // match found
                }
                flags[i] = false;
            }
        }

    }
}
