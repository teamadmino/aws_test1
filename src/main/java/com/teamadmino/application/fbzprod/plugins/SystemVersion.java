package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.ServerMain.*;
import static com.teamadmino.admino_backend.server.database.Utilities.readFileToStringHun;

import com.teamadmino.admino_backend.server.database.ServerMain;
import com.teamadmino.admino_backend.server.http.RequestContext;

public class SystemVersion {

    public static void init_onload(RequestContext rc) throws Exception {
        rc.setValue(
            "monotext",
            "<br>" +
            readFileToStringHun(workdir + "/resources/version")
                .replaceAll("\r", "")
                .replaceAll("\n", "<br>")
                .replaceAll(" ", "&nbsp")
        );
    }


}
