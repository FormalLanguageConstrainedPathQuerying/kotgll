/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.jpackage.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import static jdk.jpackage.internal.MacAppImageBuilder.ICON_ICNS;
import static jdk.jpackage.internal.MacAppImageBuilder.MAC_CF_BUNDLE_IDENTIFIER;
import static jdk.jpackage.internal.OverridableResource.createResource;

import static jdk.jpackage.internal.StandardBundlerParam.APP_NAME;
import static jdk.jpackage.internal.StandardBundlerParam.CONFIG_ROOT;
import static jdk.jpackage.internal.StandardBundlerParam.LICENSE_FILE;
import static jdk.jpackage.internal.StandardBundlerParam.TEMP_ROOT;
import static jdk.jpackage.internal.StandardBundlerParam.VERBOSE;
import static jdk.jpackage.internal.StandardBundlerParam.DMG_CONTENT;

public class MacDmgBundler extends MacBaseInstallerBundler {

    private static final ResourceBundle I18N = ResourceBundle.getBundle(
            "jdk.jpackage.internal.resources.MacResources");

    static final String DEFAULT_BACKGROUND_IMAGE = "background_dmg.tiff";
    static final String BACKGROUND_IMAGE_FOLDER =".background";
    static final String BACKGROUND_IMAGE = "background.tiff";
    static final String DEFAULT_DMG_SETUP_SCRIPT = "DMGsetup.scpt";
    static final String TEMPLATE_BUNDLE_ICON = "JavaApp.icns";

    static final String DEFAULT_LICENSE_PLIST="lic_template.plist";

    public static final BundlerParamInfo<String> INSTALLER_SUFFIX =
            new StandardBundlerParam<> (
            "mac.dmg.installerName.suffix",
            String.class,
            params -> "",
            (s, p) -> s);

    public Path bundle(Map<String, ? super Object> params,
            Path outdir) throws PackagerException {
        Log.verbose(MessageFormat.format(I18N.getString("message.building-dmg"),
                APP_NAME.fetchFrom(params)));

        IOUtils.writableOutputDir(outdir);

        try {
            Path appLocation = prepareAppBundle(params);

            if (appLocation != null && prepareConfigFiles(appLocation,params)) {
                Path configScript = getConfig_Script(params);
                if (IOUtils.exists(configScript)) {
                    IOUtils.run("bash", configScript);
                }

                return buildDMG(params, appLocation, outdir);
            }
            return null;
        } catch (IOException | PackagerException ex) {
            Log.verbose(ex);
            throw new PackagerException(ex);
        }
    }

    private static final String hdiutil = "/usr/bin/hdiutil";

    private void prepareDMGSetupScript(Path appLocation,
            Map<String, ? super Object> params) throws IOException {
        Path dmgSetup = getConfig_VolumeScript(params);
        Log.verbose(MessageFormat.format(
                I18N.getString("message.preparing-dmg-setup"),
                dmgSetup.toAbsolutePath().toString()));

        Path imageDir = IMAGES_ROOT.fetchFrom(params);
        if (!Files.exists(imageDir)) {
             Files.createDirectories(imageDir);
        }
        Path rootPath = Path.of(imageDir.toString()).toRealPath();
        Path volumePath = rootPath.resolve(APP_NAME.fetchFrom(params));
        String volumeUrl = volumePath.toUri().toString() + File.separator;

        Path bgFile = Path.of(rootPath.toString(), APP_NAME.fetchFrom(params),
                              BACKGROUND_IMAGE_FOLDER, BACKGROUND_IMAGE);

        Map<String, String> data = new HashMap<>();
        data.put("DEPLOY_VOLUME_URL", volumeUrl);
        data.put("DEPLOY_BG_FILE", bgFile.toString());
        data.put("DEPLOY_VOLUME_PATH", volumePath.toString());
        data.put("DEPLOY_APPLICATION_NAME", APP_NAME.fetchFrom(params));
        String targetItem = (StandardBundlerParam.isRuntimeInstaller(params)) ?
              APP_NAME.fetchFrom(params) : appLocation.getFileName().toString();
        data.put("DEPLOY_TARGET", targetItem);
        data.put("DEPLOY_INSTALL_LOCATION", getInstallDir(params, true));
        data.put("DEPLOY_INSTALL_LOCATION_DISPLAY_NAME",
                getInstallDirDisplayName(params));

        createResource(DEFAULT_DMG_SETUP_SCRIPT, params)
                .setCategory(I18N.getString("resource.dmg-setup-script"))
                .setSubstitutionData(data)
                .saveToFile(dmgSetup);
    }

    private Path getConfig_VolumeScript(Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params).resolve(
                APP_NAME.fetchFrom(params) + "-dmg-setup.scpt");
    }

    private Path getConfig_VolumeBackground(
            Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params).resolve(
                APP_NAME.fetchFrom(params) + "-background.tiff");
    }

    private Path getConfig_VolumeIcon(Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params).resolve(
                APP_NAME.fetchFrom(params) + "-volume.icns");
    }

    private Path getConfig_LicenseFile(Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params).resolve(
                APP_NAME.fetchFrom(params) + "-license.plist");
    }

    private void prepareLicense(Map<String, ? super Object> params) {
        try {
            String licFileStr = LICENSE_FILE.fetchFrom(params);
            if (licFileStr == null) {
                return;
            }

            Path licFile = Path.of(licFileStr);
            byte[] licenseContentOriginal =
                    Files.readAllBytes(licFile);
            String licenseInBase64 =
                    Base64.getEncoder().encodeToString(licenseContentOriginal);

            Map<String, String> data = new HashMap<>();
            data.put("APPLICATION_LICENSE_TEXT", licenseInBase64);

            createResource(DEFAULT_LICENSE_PLIST, params)
                    .setCategory(I18N.getString("resource.license-setup"))
                    .setSubstitutionData(data)
                    .saveToFile(getConfig_LicenseFile(params));

        } catch (IOException ex) {
            Log.verbose(ex);
        }
    }

    private boolean prepareConfigFiles(Path appLocation,
            Map<String, ? super Object> params) throws IOException {

        createResource(DEFAULT_BACKGROUND_IMAGE, params)
                    .setCategory(I18N.getString("resource.dmg-background"))
                    .saveToFile(getConfig_VolumeBackground(params));

        createResource(TEMPLATE_BUNDLE_ICON, params)
                .setCategory(I18N.getString("resource.volume-icon"))
                .setExternal(ICON_ICNS.fetchFrom(params))
                .saveToFile(getConfig_VolumeIcon(params));

        createResource(null, params)
                .setCategory(I18N.getString("resource.post-install-script"))
                .saveToFile(getConfig_Script(params));

        prepareLicense(params);

        prepareDMGSetupScript(appLocation, params);

        return true;
    }

    private Path getConfig_Script(Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params).resolve(
                APP_NAME.fetchFrom(params) + "-post-image.sh");
    }

    private String findSetFileUtility() {
        String typicalPaths[] = {"/Developer/Tools/SetFile",
                "/usr/bin/SetFile", "/Developer/usr/bin/SetFile"};

        String setFilePath = null;
        for (String path : typicalPaths) {
            Path f = Path.of(path);
            if (Files.exists(f) && Files.isExecutable(f)) {
                setFilePath = path;
                break;
            }
        }

        if (setFilePath != null) {
            try {
                ProcessBuilder pb = new ProcessBuilder(setFilePath, "-h");
                Process p = pb.start();
                int code = p.waitFor();
                if (code == 0) {
                    return setFilePath;
                }
            } catch (Exception ignored) {}

            return null;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("/usr/bin/xcrun", "-find", "SetFile");
            Process p = pb.start();
            InputStreamReader isr = new InputStreamReader(p.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String lineRead = br.readLine();
            if (lineRead != null) {
                Path f = Path.of(lineRead);
                if (Files.exists(f) && Files.isExecutable(f)) {
                    return f.toAbsolutePath().toString();
                }
            }
        } catch (IOException ignored) {}

        return null;
    }

    private Path buildDMG( Map<String, ? super Object> params,
            Path appLocation, Path outdir) throws IOException {
        boolean copyAppImage = false;
        Path imagesRoot = IMAGES_ROOT.fetchFrom(params);
        if (!Files.exists(imagesRoot)) {
            Files.createDirectories(imagesRoot);
        }

        Path protoDMG = imagesRoot.resolve(APP_NAME.fetchFrom(params) +"-tmp.dmg");
        Path finalDMG = outdir.resolve(MAC_INSTALLER_NAME.fetchFrom(params)
                + INSTALLER_SUFFIX.fetchFrom(params) + ".dmg");

        Path srcFolder = appLocation.getParent();
        if (StandardBundlerParam.isRuntimeInstaller(params)) {
            Path newRoot = Files.createTempDirectory(TEMP_ROOT.fetchFrom(params),
                    "root-");

            Path home = appLocation.resolve("Contents/Home");
            Path source = (Files.exists(home)) ? home : appLocation;

            Path root = newRoot.resolve(
                    MAC_CF_BUNDLE_IDENTIFIER.fetchFrom(params));
            Path dest = root.resolve("Contents/Home");

            IOUtils.copyRecursive(source, dest);

            srcFolder = newRoot;
        }

        Log.verbose(MessageFormat.format(I18N.getString(
                "message.creating-dmg-file"), finalDMG.toAbsolutePath()));

        Files.deleteIfExists(protoDMG);
        try {
            Files.deleteIfExists(finalDMG);
        } catch (IOException ex) {
            throw new IOException(MessageFormat.format(I18N.getString(
                    "message.dmg-cannot-be-overwritten"),
                    finalDMG.toAbsolutePath()));
        }

        Files.createDirectories(protoDMG.getParent());
        Files.createDirectories(finalDMG.getParent());

        String hdiUtilVerbosityFlag = VERBOSE.fetchFrom(params) ?
                "-verbose" : "-quiet";
        List <String> dmgContent = DMG_CONTENT.fetchFrom(params);
        for (String content : dmgContent) {
            Path path = Path.of(content);
            IOUtils.copyRecursive(path, srcFolder.resolve(path.getFileName()));
        }
        ProcessBuilder pb = new ProcessBuilder(
                hdiutil,
                "create",
                hdiUtilVerbosityFlag,
                "-srcfolder", srcFolder.toAbsolutePath().toString(),
                "-volname", APP_NAME.fetchFrom(params),
                "-ov", protoDMG.toAbsolutePath().toString(),
                "-fs", "HFS+",
                "-format", "UDRW");
        try {
            IOUtils.exec(pb, false, null, true, Executor.INFINITE_TIMEOUT);
        } catch (IOException ex) {
            Log.verbose(ex); 

            copyAppImage = true;

            long size = new PathGroup(Map.of(new Object(), srcFolder)).sizeInBytes();
            size += 50 * 1024 * 1024; 
            pb = new ProcessBuilder(
                hdiutil,
                "create",
                hdiUtilVerbosityFlag,
                "-size", String.valueOf(size),
                "-volname", APP_NAME.fetchFrom(params),
                "-ov", protoDMG.toAbsolutePath().toString(),
                "-fs", "HFS+");
            new RetryExecutor()
                .setMaxAttemptsCount(10)
                .setAttemptTimeoutMillis(3000)
                .setWriteOutputToFile(true)
                .execute(pb);
        }

        pb = new ProcessBuilder(
                hdiutil,
                "attach",
                protoDMG.toAbsolutePath().toString(),
                hdiUtilVerbosityFlag,
                "-mountroot", imagesRoot.toAbsolutePath().toString());
        IOUtils.exec(pb, false, null, true, Executor.INFINITE_TIMEOUT);

        Path mountedRoot = imagesRoot.resolve(APP_NAME.fetchFrom(params));

        if (copyAppImage) {
            if (srcFolder.toString().toLowerCase().endsWith(".app")) {
                Path destPath = mountedRoot
                        .resolve(srcFolder.getFileName());
                Files.createDirectory(destPath);
                IOUtils.copyRecursive(srcFolder, destPath);
            } else {
                IOUtils.copyRecursive(srcFolder, mountedRoot);
            }
        }

        try {
            Path bgdir = mountedRoot.resolve(BACKGROUND_IMAGE_FOLDER);
            Files.createDirectories(bgdir);
            IOUtils.copyFile(getConfig_VolumeBackground(params),
                    bgdir.resolve(BACKGROUND_IMAGE));

            try {
                pb = new ProcessBuilder("/usr/bin/osascript",
                        getConfig_VolumeScript(params).toAbsolutePath().toString());
                IOUtils.exec(pb, 180); 
            } catch (IOException ex) {
                Log.verbose(ex);
            }

            Path volumeIconFile = mountedRoot.resolve(".VolumeIcon.icns");
            IOUtils.copyFile(getConfig_VolumeIcon(params),
                    volumeIconFile);

            String setFileUtility = findSetFileUtility();
            if (setFileUtility != null) {
                try {
                    volumeIconFile.toFile().setWritable(true);
                    pb = new ProcessBuilder(
                            setFileUtility,
                            "-c", "icnC",
                            volumeIconFile.toAbsolutePath().toString());
                    IOUtils.exec(pb);
                    volumeIconFile.toFile().setReadOnly();

                    pb = new ProcessBuilder(
                            setFileUtility,
                            "-a", "C",
                            mountedRoot.toAbsolutePath().toString());
                    IOUtils.exec(pb);
                } catch (IOException ex) {
                    Log.error(ex.getMessage());
                    Log.verbose("Cannot enable custom icon using SetFile utility");
                }
            } else {
                Log.verbose(I18N.getString("message.setfile.dmg"));
            }

        } finally {
            pb = new ProcessBuilder(
                    hdiutil,
                    "detach",
                    hdiUtilVerbosityFlag,
                    mountedRoot.toAbsolutePath().toString());
            RetryExecutor retryExecutor = new RetryExecutor();
            retryExecutor.setExecutorInitializer(exec -> {
                if (!Files.exists(mountedRoot)) {
                    retryExecutor.abort();
                }
            });
            try {
                retryExecutor.setMaxAttemptsCount(10).setAttemptTimeoutMillis(6000)
                        .execute(pb);
            } catch (IOException ex) {
                if (!retryExecutor.isAborted()) {
                    if (Files.exists(mountedRoot)) {
                        pb = new ProcessBuilder(
                                hdiutil,
                                "detach",
                                "-force",
                                hdiUtilVerbosityFlag,
                                mountedRoot.toAbsolutePath().toString());
                        IOUtils.exec(pb, false, null, true, Executor.INFINITE_TIMEOUT);
                    }
                }
            }
        }

        pb = new ProcessBuilder(
                hdiutil,
                "convert",
                protoDMG.toAbsolutePath().toString(),
                hdiUtilVerbosityFlag,
                "-format", "UDZO",
                "-o", finalDMG.toAbsolutePath().toString());
        try {
            new RetryExecutor()
                .setMaxAttemptsCount(10)
                .setAttemptTimeoutMillis(3000)
                .execute(pb);
        } catch (Exception ex) {
            Path protoDMG2 = imagesRoot
                    .resolve(APP_NAME.fetchFrom(params) + "-tmp2.dmg");
            Files.copy(protoDMG, protoDMG2);
            try {
                pb = new ProcessBuilder(
                        hdiutil,
                        "convert",
                        protoDMG2.toAbsolutePath().toString(),
                        hdiUtilVerbosityFlag,
                        "-format", "UDZO",
                        "-o", finalDMG.toAbsolutePath().toString());
                IOUtils.exec(pb, false, null, true, Executor.INFINITE_TIMEOUT);
            } finally {
                Files.deleteIfExists(protoDMG2);
            }
        }

        if (Files.exists(getConfig_LicenseFile(params))) {
            pb = new ProcessBuilder(
                    hdiutil,
                    "udifrez",
                    finalDMG.toAbsolutePath().toString(),
                    "-xml",
                    getConfig_LicenseFile(params).toAbsolutePath().toString()
            );
            new RetryExecutor()
                .setMaxAttemptsCount(10)
                .setAttemptTimeoutMillis(3000)
                .execute(pb);
        }

        Files.deleteIfExists(protoDMG);

        Log.verbose(MessageFormat.format(I18N.getString(
                "message.output-to-location"),
                APP_NAME.fetchFrom(params), finalDMG.toAbsolutePath().toString()));

        return finalDMG;
    }



    @Override
    public String getName() {
        return I18N.getString("dmg.bundler.name");
    }

    @Override
    public String getID() {
        return "dmg";
    }

    @Override
    public boolean validate(Map<String, ? super Object> params)
            throws ConfigException {
        try {
            Objects.requireNonNull(params);

            validateAppImageAndBundeler(params);

            return true;
        } catch (RuntimeException re) {
            if (re.getCause() instanceof ConfigException) {
                throw (ConfigException) re.getCause();
            } else {
                throw new ConfigException(re);
            }
        }
    }

    @Override
    public Path execute(Map<String, ? super Object> params,
            Path outputParentDir) throws PackagerException {
        return bundle(params, outputParentDir);
    }

    @Override
    public boolean supported(boolean runtimeInstaller) {
        return isSupported();
    }

    public static final String[] required =
            {"/usr/bin/hdiutil", "/usr/bin/osascript"};
    public static boolean isSupported() {
        try {
            for (String s : required) {
                Path f = Path.of(s);
                if (!Files.exists(f) || !Files.isExecutable(f)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isDefault() {
        return true;
    }
}
