package me.kitakeyos.urimapper;

import lombok.extern.slf4j.Slf4j;
import me.kitakeyos.urimapper.ui.URIMapperGUI;
import org.microemu.app.Main;

import javax.microedition.io.Connection;
import java.io.IOException;

@Slf4j
public class ConnectorImpl extends org.microemu.microedition.io.ConnectorImpl {

    @Override
    public Connection open(String name, int mode, boolean timeouts) throws IOException {
        URIMapperGUI mapperGUI = Main.uriMapperGUI;
        name = mapperGUI.getMappedURI(name);
        mapperGUI.addCapturedURI(name);
        return super.open(name, mode, timeouts);
    }
}
