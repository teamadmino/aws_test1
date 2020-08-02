package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.ServerMain.workdir;
import static com.teamadmino.admino_backend.server.database.Utilities.readFileToStringHun;

import com.teamadmino.admino_backend.server.http.RequestContext;

public class DatabaseVersion {

    public static void init_onload(RequestContext rc) throws Exception {
        rc.setValue(
            "monotext",
            "<br>" +
            readFileToStringHun(workdir + "/fbzdemo/data/log")
                .replaceAll("\r", "")
                .replaceAll("\n", "<br>")
                .replaceAll(" ", "&nbsp")
        );
    }


}
