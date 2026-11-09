/*
 * MohistMC
 * Copyright (C) 2018-2024.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.mohistmc.action;

import com.mohistmc.MohistMCStart;
import com.mohistmc.feature.DefaultLibraries;
import com.mohistmc.libraries.Libraries;
import com.mohistmc.tools.FileUtils;
import com.mohistmc.tools.SHA256;
import com.mohistmc.util.DataParser;
import com.mohistmc.util.JarLoader;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public abstract class Action {

    private static final PrintStream origin = System.out;
    public final String mohistVer;
    public final String forgeVer;
    public final String mcpVer;
    public final String mcVer;
    public final String forgeStart;
    public final File universalJar;
    public final File serverJar;
    public final File lzma;
    public final File installInfo;
    public final String otherStart;
    public final File extra;
    public final File slim;
    public final File srg;
    public final String mcpStart;
    public final File mcpZip;
    public final File mcpTxt;
    public final File minecraft_server;
    public final String libPath = "libraries";

    public List<URL> installerTourls = new ArrayList<>();


    protected Action() {
        init();
        this.mohistVer = DataParser.versionMap.get("mohist");
        this.forgeVer = DataParser.versionMap.get("forge");
        this.mcpVer = DataParser.versionMap.get("mcp");
        this.mcVer = DataParser.versionMap.get("minecraft");

        this.forgeStart = "net/minecraftforge/forge/" + mcVer + "-" + forgeVer + "/forge-" + mcVer + "-" + forgeVer;
        this.universalJar = new File(libPath, forgeStart + "-universal.jar");
        this.serverJar = new File(libPath, forgeStart + "-server.jar");

        this.lzma = new File(libPath, "com/mohistmc/installation/data/server.lzma");
        this.installInfo = new File(libPath, "com/mohistmc/installation/installInfo");

        this.otherStart = "net/minecraft/server/" + mcVer + "-" + mcpVer + "/server-" + mcVer + "-" + mcpVer;

        this.extra = new File(libPath, otherStart + "-extra.jar");
        this.slim = new File(libPath, otherStart + "-slim.jar");
        this.srg = new File(libPath, otherStart + "-srg.jar");

        this.mcpStart = "de/oceanlabs/mcp/mcp_config/" + mcVer + "-" + mcpVer + "/mcp_config-" + mcVer + "-" + mcpVer;
        this.mcpZip = new File(libPath, mcpStart + ".zip");
        this.mcpTxt = new File(libPath, mcpStart + "-mappings.txt");

        this.minecraft_server = new File(libPath, "net/minecraft/server/" + mcVer + "/server-" + mcVer + ".jar");
    }

    protected void run(String mainClass, String[] args) throws Exception {
        TimeZone original = TimeZone.getDefault();
        List<URL> classPath = stringToUrl(installerTourls);
        System.out.println("[Mohist] Loading " + classPath);
        URLClassLoader loader = URLClassLoader.newInstance(classPath.toArray(new URL[0]));
        try {
            Class.forName(mainClass, true, loader).getDeclaredMethod("main", String[].class).invoke(null, new Object[]{args});
        } finally {
            // Forge installer tools force GMT for reproducible archives.
            TimeZone.setDefault(original);
        }
        loader.clearAssertionStatus();
        loader.close();
    }

    protected List<URL> stringToUrl(List<URL> strs) throws Exception {
        List<URL> temp = new ArrayList<>();
        for (URL t : strs) {
            File file = new File(t.toURI());
            JarLoader.loadJar(file.toPath());
            temp.add(file.toURI().toURL());
        }
        return temp;
    }

    /*
    THIS IS TO NOT SPAM CONSOLE WHEN IT WILL PRINT A LOT OF THINGS
     */
    protected void mute() throws Exception {
        File out = new File(libPath, "com/mohistmc/installation/installationLogs.txt");
        if (!out.exists()) {
            out.getParentFile().mkdirs();
            out.createNewFile();
        }
        System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(out))));
    }

    protected void unmute() {
        System.setOut(origin);
    }

    protected void copyFileFromJar(File file, String pathInJar) {
        InputStream is = MohistMCStart.class.getClassLoader().getResourceAsStream(pathInJar);
        if (!file.exists() || !SHA256.is(file, SHA256.as(is)) || file.length() <= 1) {
            // Clear old version
            File parentfile = file.getParentFile();
            if (file.getPath().contains("minecraftforge")) {
                int lastSlashIndex = parentfile.getPath().replaceAll("\\\\", "/").lastIndexOf("/");
                String result = parentfile.getPath().substring(0, lastSlashIndex + 1);
                File old = new File(result);
                if (old.exists()) {
                    FileUtils.deleteFolders(old);
                }
            }
            file.getParentFile().mkdirs();
            if (is != null) {
                try {
                    file.createNewFile();
                    Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {
                }
            } else {
                System.out.println("[Mohist] The file " + file.getName() + " doesn't exists in the Mohist jar !");
                System.exit(0);
            }
        }
    }

    public boolean needsInstall() throws IOException {
        if (installInfo.exists()) {
            String jarmd = SHA256.as(MohistMCStart.jarTool.getFile());
            List<String> lines = Files.readAllLines(installInfo.toPath());
            return lines.size() < 2 || !jarmd.equals(lines.get(1));
        }
        return true;
    }

    private void init() {
        try {
            BufferedReader b = new BufferedReader(new InputStreamReader(DefaultLibraries.class.getClassLoader().getResourceAsStream("installer.txt")));
            for (String line = b.readLine(); line != null; line = b.readLine()) {
                Libraries libraries = Libraries.from(line);
                File file = new File("libraries", libraries.getPath());
                URL url = file.toURI().toURL();
                installerTourls.add(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
