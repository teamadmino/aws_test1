package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.ServerMain.backendService;
import static com.teamadmino.admino_backend.server.database.ServerMain.getNextTempId;
import static com.teamadmino.admino_backend.server.database.ServerMain.log;
import static com.teamadmino.admino_backend.server.database.ServerMain.tables;
import static com.teamadmino.admino_backend.server.database.ServerMain.workdir;

import com.teamadmino.admino_backend.server.database.DatabaseTable;
import com.teamadmino.admino_backend.server.database.FieldDefinition;
import com.teamadmino.admino_backend.server.database.MemoryRecord;
import com.teamadmino.admino_backend.server.database.TableViewDefinition;
import com.teamadmino.admino_backend.server.http.RequestContext;
import com.teamadmino.admino_backend.server.ui.ElementUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.RandomAccessFile;

public class ReportsCommonTableviewElszamolas {

    public static void init_onload(RequestContext rc) throws Exception {
        JSONObject args = rc.getArgs();
        setupTable(rc, "listK", "resultTableNameK");
        setupTable(rc, "listB", "resultTableNameB");
        JSONObject dataK = new JSONObject();
        JSONObject dataB = new JSONObject();
        generateData(rc, dataK, "resultTableNameK");
        generateData(rc, dataB, "resultTableNameB");

        JSONArray data = new JSONArray();
        if (dataK.keySet().size() > 0) {
            dataK.put("label", "Kiadás");
            data.put(dataK);
        }
        if (dataB.keySet().size() > 0) {
            dataB.put("label", "Bevételezés");
            data.put(dataB);
        }
        rc.getScreenVars().put("chartData", data);

        String url = backendService + "/request/" + rc.screenId + "$" + "chartiframe" + "@download"
                     + "?sid=" + rc.posted.get("sid") + "&id=" + getNextTempId();
        rc.setValue("chartiframe", url);
        rc.iFrameUpdate("chartiframe");

    }

    private static void setupTable(RequestContext rc, String tableElementId, String resultTableName) throws Exception {
        JSONObject args = rc.getArgs();
        String tempTableName = args.getString(resultTableName);
        TableViewDefinition tableViewDefinition = new TableViewDefinition(tempTableName);
        DatabaseTable tempTable = tables.get(tempTableName);
        rc.getScreenVars().put(resultTableName, tempTableName);

        for (FieldDefinition field : tempTable.fieldList) {
            tableViewDefinition.addTableField(field.name);
        }

        rc.setupTableView(tableElementId, tableViewDefinition);

        JSONArray columns = rc.getUiElement(tableElementId).definition.getJSONArray("columns");
        JSONArray headers = new JSONArray();
        JSONArray orders = new JSONArray();
        JSONArray types = new JSONArray();
        for (int i = 0; i < columns.length(); i++) {
            headers.put(columns.getJSONObject(i).getString("description"));
        }

        if (tempTable.indexCount > 0) {
            for (int i = 0; i < tempTable.fieldList.size(); i++) {
                String order;
                String type;
                switch (tempTable.fieldList.get(i).textFormat) {
                    case TEXT_FORMAT_NUMBER:
                    case TEXT_FORMAT_FIXED1:
                    case TEXT_FORMAT_FIXED2:
                    case TEXT_FORMAT_FIXED3:
                    case TEXT_FORMAT_FIXED4:
                    case TEXT_FORMAT_SPACEPADDED:
                    case TEXT_FORMAT_INTEGER:
                    case TEXT_FORMAT_DEC1:
                    case TEXT_FORMAT_DEC2:
                    case TEXT_FORMAT_DEC3:
                    case TEXT_FORMAT_DEC4:
                    case TEXT_FORMAT_DEC5:
                    case TEXT_FORMAT_DEC6:
                    case TEXT_FORMAT_DEC7:
                    case TEXT_FORMAT_DEC8:
                    case TEXT_FORMAT_DEC9:
                    case TEXT_FORMAT_NUMBER_0P:
                    case TEXT_FORMAT_NUMBER_X:
                    case TEXT_FORMAT_FIXED1_X:
                    case TEXT_FORMAT_FIXED1_0P:
                    case TEXT_FORMAT_FIXED2_X:
                        type = "num";
                        order = "desc";
                        break;
                    case TEXT_FORMAT_TIME:
                    case TEXT_FORMAT_DATE:
                    case TEXT_FORMAT_COMPOSITE1:
                    case TEXT_FORMAT_COMPOSITE2:
                    case TEXT_FORMAT_COMPOSITE2Z:
                    case TEXT_FORMAT_ZEROPADDED:
                    case TEXT_FORMAT_COMPOSITE2Z_0P:
                    case TEXT_FORMAT_COMPOSITE2Z_X:
                    case TEXT_FORMAT_ZEROPADDED_X:
                    case TEXT_FORMAT_ZEROPADDED_0P:
                    case TEXT_FORMAT_HEX:
                        type = "num";
                        order = "asc";
                        break;
                    //case TEXT_FORMAT_STRING:
                    //case TEXT_FORMAT_CHAR:
                    //case TEXT_FORMAT_PARENTS:
                    //case TEXT_FORMAT_NOP:
                    //case TEXT_FORMAT_CODE1:
                    //case TEXT_FORMAT_UNKNOWN:
                    default:
                        type = "text";
                        order = "asc";
                        break;
                }
                orders.put(order);
                types.put(type);
            }
            rc.getScreenVars().put(tableElementId + "columns", columns);
            rc.getScreenVars().put(tableElementId + "headers", headers);
            rc.getScreenVars().put(tableElementId + "orders", orders);
            rc.getScreenVars().put(tableElementId + "types", types);
            rc.getScreenVars().put(tableElementId + "sortBy", tempTable.fieldList.size() - 1);
            rc.getScreenVars().put(tableElementId + "sortOrder", orders.getString(tempTable.fieldList.size() - 1));
            rc.getScreenVars().put(tableElementId + "indexed", true);
            setupHeaders(rc, tableElementId);

            int newIndex =
                rc.getScreenVars().getString(tableElementId + "sortOrder").equals("asc") ?
                tempTable.fieldList.size() : -tempTable.fieldList.size();
            JSONObject val = new JSONObject();
            val.put("index", String.valueOf(newIndex));
            rc.putScreenValue(rc.getResponseScreen(), rc.getUiElement(tableElementId).path, "value", val);

//            int lastGroup;
//            for (lastGroup = tempTable.fieldList.size() - 1; lastGroup > 0; lastGroup--) {
//                if (orders.get(lastGroup).equals("asc")) {
//                    break;
//                }
//            }
//            String lastMetric = "";
//            JSONArray metrics = new JSONArray();
//            for (int i = lastGroup + 1; i < tempTable.fieldList.size(); i++) {
//                JSONObject metric = new JSONObject();
//                metric.put("label", tempTable.fieldList.get(i).description);
//                lastMetric = tempTable.fieldList.get(i).name;
//                metric.put("value", lastMetric);
//                metrics.put(metric);
//            }
//            rc.setValue("chartMetric", lastMetric);
//            rc.set("chartMetric", "options", metrics);
//            rc.set("chartArea", "hidden", true);
//        } else {
//            rc.set("chartOptions", "hidden", true);
//            rc.set("chartArea", "hidden", true);
        }
    }

    private static void setupHeaders(RequestContext rc, String tableElementId) {
        JSONArray columns = rc.getScreenVars().getJSONArray(tableElementId + "columns");
        JSONArray types = rc.getScreenVars().getJSONArray(tableElementId + "types");
        JSONArray headers = rc.getScreenVars().getJSONArray(tableElementId + "headers");
        int index = rc.getScreenVars().getInt(tableElementId + "sortBy");
        String sortOrder = rc.getScreenVars().getString(tableElementId + "sortOrder");
        for (int i = 0; i < columns.length(); i++) {
            if (i != index) {
                String order = "<span class='material-icons' style='color:#666666; font-size:20px'>swap_vert</span>";
                if (columns.getJSONObject(i).optString("headerStyle").contains("right")) {
                    columns.getJSONObject(i).put("description",
                                                 order + " "
                                                 + "<span style='color:#00DDDD;'>"
                                                 + headers.getString(i)
                                                 + "</span>");
                } else {
                    columns.getJSONObject(i).put("description",
                                                 "<span style='color:#00DDDD;'>"
                                                 + headers.getString(i)
                                                 + "</span> " + order);
                }
                columns.getJSONObject(i)
                    .put("headerContainerStyle", new JSONObject("{\"background\": \"#003333\"}"));
            } else {
                String order;
                if (sortOrder.equals("asc")) {
                    if (types.get(i).equals("num")) {
                        order =
                            "<span class='admino-icon-sort-numeric-asc' style='color:#00FF00; font-size:20px'></span>";
                    } else {
                        order =
                            "<span class='admino-icon-sort-alpha-asc' style='color:#00FF00; font-size:20px'></span>";
                    }
                } else {
                    if (types.get(i).equals("num")) {
                        order =
                            "<span class='admino-icon-sort-numeric-desc' style='color:#FF0000; font-size:20px'></span>";
                    } else {
                        order =
                            "<span class='admino-icon-sort-alpha-desc' style='color:#FF0000; font-size:20px'></span>";
                    }
                }
//                if (sortOrder.equals("asc")) {
//                    if (types.get(i).equals("num")) {
//                        order = "<span class='admino-icon-sort-amount-asc' style='color:#00FF00;'></span>";
//                    } else {
//                        order = "<span class='admino-icon-sort-amount-asc' style='color:#00FF00;'></span>";
//                    }
//                } else {
//                    if (types.get(i).equals("num")) {
//                        order = "<span class='admino-icon-sort-amount-desc' style='color:#FF0000;'></span>";
//                    } else {
//                        order = "<span class='admino-icon-sort-amount-desc' style='color:#FF0000;'></span>";
//                    }
//                }

                if (columns.getJSONObject(i).optString("headerStyle").contains("right")) {
                    columns.getJSONObject(i).put("description",
                                                 order + "<span style='color:#FFFFAA;'>&nbsp"
                                                 + headers.getString(i)
                                                 + "</span>"
                    );
                } else {
                    columns.getJSONObject(i).put("description",
                                                 "<span style='color:#FFFFAA;'>"
                                                 + headers.getString(i) + "&nbsp</span>" + order);
                }
                columns.getJSONObject(i)
                    .put("headerContainerStyle", new JSONObject("{\"background\": \"#004444\"}"));
            }
        }
    }

    public static void listK_headerCellClick(RequestContext rc) throws Exception {
        list_headerCellClick(rc, "listK");
    }

    public static void listB_headerCellClick(RequestContext rc) throws Exception {
        list_headerCellClick(rc, "listB");
    }

    public static void list_headerCellClick(RequestContext rc, String tableElementId) throws Exception {
        if (!rc.getScreenVars().optBoolean(tableElementId + "indexed", false)) {
            return;
        }
        String modifiers = rc.posted.getJSONArray("activeModifierKeys").toString();

        JSONArray columns = rc.getScreenVars().getJSONArray(tableElementId + "columns");
        JSONArray headers = rc.getScreenVars().getJSONArray(tableElementId + "headers");
        JSONArray orders = rc.getScreenVars().getJSONArray(tableElementId + "orders");
        int index = rc.getScreenVars().getInt(tableElementId + "sortBy");
        String sortOrder = rc.getScreenVars().getString(tableElementId + "sortOrder");
        int newIndex = rc.getValueJson(tableElementId).getInt("selectedHeaderColumnIndex");
        if (newIndex == index) {
            sortOrder = sortOrder.equals("asc") ? "desc" : "asc";
            orders.put(newIndex, sortOrder);
        } else {
            sortOrder = orders.getString(newIndex);
        }
        rc.getScreenVars().put(tableElementId + "sortBy", newIndex);
        rc.getScreenVars().put(tableElementId + "sortOrder", sortOrder);
        setupHeaders(rc, tableElementId);
        rc.set(tableElementId, "columns", columns);

        newIndex = sortOrder.equals("asc") ? newIndex + 1 : -1 - newIndex;
        JSONObject val = new JSONObject();
        val.put("index", String.valueOf(newIndex));

        if (modifiers.contains("Alt")) {
            val.put("cursorpos", rc.getValueJson(tableElementId).getInt("count") / 2);
        } else {
            val.put("keys", ElementUtils.tablePositionFirst).put("cursorpos", 0);
        }

        rc.putScreenValue(rc.getResponseScreen(), rc.getUiElement(tableElementId).path, "value", val);
        rc.tableForceRefresh(tableElementId);
    }

    public static void chartiframe_download(RequestContext rc) throws Exception {

        StringBuilder out = new StringBuilder();
        out.append("" +
                   "<!-- Generated by the backend -->\n"
                   + "<script>\n"
                   + "function getBackendVariables() {\n"
                   + "  var vars = " + rc.getScreenVars().getJSONArray("chartData").toString() + ";\n"
                   + "  return vars;\n"
                   + "}\n"
                   + "</script>\n"
                   + "<!-- End of generated code -->\n");

        String staticSource = workdir + "/_admino/resources/am4/demos/pie-of-a-pie.html";

        byte[] generatedContent = out.toString().getBytes();
        int staticContentLength = (int) new File(staticSource).length();

        byte[] response = new byte[generatedContent.length + staticContentLength];
        System.arraycopy(generatedContent, 0, response, 0, generatedContent.length);

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(staticSource, "r")) {
            randomAccessFile.read(response, generatedContent.length, staticContentLength);
            rc.setCustomResponse(response, "text/html; charset=utf-8", 200);
        }

    }

    private static void generateData(RequestContext rc, JSONObject data, String resultTableName) throws Exception {
        DatabaseTable tempTable = tables.get(rc.getScreenVars().getString(resultTableName));
        if (tempTable.recordCount == 0) {
            return;
        }
        MemoryRecord memoryRecord;
        int limit = tempTable.recordCount > 10 ? 9 : (int) tempTable.recordCount;
        int[] index = tempTable.indexes.get(tempTable.indexes.size() - 1);
        JSONArray array = new JSONArray();
        data.put("subData", array);
        double sum = 0;
        for (int i = 1; i <= limit; i++) {
            int absPos = index[(int) (tempTable.recordCount - i + 1)];
            memoryRecord = tempTable.getMemoryRecord(absPos);
            JSONObject item = new JSONObject();
            item.put("label",
                     memoryRecord.getASFormattedString("id").trim() + " " +
                     memoryRecord.getAsString("nm").trim()
            );
            double value = Double.parseDouble(memoryRecord.getASFormattedString("nt").trim());
            sum += value;
            item.put("value", value);
            array.put(item);
        }
        double other = 0;
        for (int i = limit + 1; i <= tempTable.recordCount; i++) {
            int absPos = index[(int) (tempTable.recordCount - i + 1)];
            memoryRecord = tempTable.getMemoryRecord(absPos);
            other += Double.parseDouble(memoryRecord.getASFormattedString("nt").trim());
        }
        if (other > 0) {
            JSONObject item = new JSONObject();
            item.put("label", "**** Egyebek");
            item.put("value", other);
            array.put(item);
        }
        data.put("value", sum + other);
    }

}
