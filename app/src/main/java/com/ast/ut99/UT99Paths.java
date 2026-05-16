package com.ast.ut99;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

final class UT99Paths {
    static final String DATA_DIR_NAME = "UT99";
    static final String EXTRA_DATA_ROOT = "com.ast.ut99.EXTRA_DATA_ROOT";

    private UT99Paths() {
    }

    static File appFilesRoot(Context context) {
        File external = context.getExternalFilesDir(null);
        if (external != null) {
            return external;
        }
        // Extremely defensive fallback for devices where external app storage is temporarily unavailable.
        return new File(context.getFilesDir(), "external-files");
    }

    static File preferredRoot(Context context) {
        return new File(appFilesRoot(context), DATA_DIR_NAME);
    }

    static File legacyRoot(Context context) {
        return appFilesRoot(context);
    }

    static File homeDir(Context context) {
        return new File(context.getFilesDir(), "home");
    }

    static File installRoot(Context context) {
        return preferredRoot(context);
    }

    static File resolveDataRoot(Context context) {
        File preferred = preferredRoot(context);
        if (hasUsableGameData(preferred)) {
            return preferred;
        }

        // Older test builds and some manual copies placed System/Maps/... directly below files/.
        // Keep supporting that layout so a misplaced data copy does not hard-crash at startup.
        File legacy = legacyRoot(context);
        if (hasUsableGameData(legacy)) {
            return legacy;
        }

        ensureSkeleton(preferred);
        return preferred;
    }

    static boolean hasUsableGameData(File root) {
        if (root == null || !root.isDirectory()) {
            return false;
        }

        File system = new File(root, "System");
        if (!system.isDirectory()) {
            return false;
        }

        String[] requiredDirs = {"Maps", "Textures", "Sounds", "Music"};
        for (String name : requiredDirs) {
            if (!new File(root, name).isDirectory()) {
                return false;
            }
        }

        // Be permissive: different UT99 installs/mod packs may not have all ini files yet.
        String[] coreMarkers = {
                "Core.u",
                "Engine.u",
                "Botpack.u",
                "UnrealTournament.ini",
                "Default.ini"
        };
        for (String marker : coreMarkers) {
            if (new File(system, marker).isFile()) {
                return true;
            }
        }

        return false;
    }

    static void ensureSkeleton(File root) {
        if (root == null) {
            return;
        }
        if (!root.exists()) {
            root.mkdirs();
        }
        String[] dirs = {"System", "Maps", "Textures", "Sounds", "Music", "Cache", "Save", "Logs"};
        for (String name : dirs) {
            File dir = new File(root, name);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }

    static void ensureAndroidIni(File root) throws IOException {
        if (root == null) {
            throw new IOException("Data root is null");
        }
        ensureSkeleton(root);
        File system = new File(root, "System");
        if (!system.isDirectory() && !system.mkdirs()) {
            throw new IOException("Cannot create System folder: " + system.getAbsolutePath());
        }

        writeUtf8(new File(system, "AndroidUT99.ini"), buildAndroidIni());
        writeUtf8(new File(system, "AndroidUser.ini"), buildAndroidUserIni());
    }

    private static void writeUtf8(File file, String text) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create folder: " + parent.getAbsolutePath());
        }
        FileOutputStream out = new FileOutputStream(file, false);
        try {
            out.write(text.getBytes(Charset.forName("UTF-8")));
        } finally {
            out.close();
        }
    }

    private static String buildAndroidIni() {
        return "[URL]\n" +
                "Protocol=unreal\n" +
                "ProtocolDescription=Unreal Protocol\n" +
                "Name=Player\n" +
                "Map=CityIntro.unr\n" +
                "LocalMap=CityIntro.unr\n" +
                "Host=\n" +
                "Portal=\n" +
                "MapExt=unr\n" +
                "SaveExt=usa\n" +
                "Port=7777\n" +
                "Class=Botpack.TMale1\n" +
                "\n" +
                "[FirstRun]\n" +
                "FirstRun=436\n" +
                "\n" +
                "[Engine.Engine]\n" +
                "GameRenderDevice=NOpenGLESDrv.NOpenGLESRenderDevice\n" +
                "WindowedRenderDevice=NOpenGLESDrv.NOpenGLESRenderDevice\n" +
                "RenderDevice=NOpenGLESDrv.NOpenGLESRenderDevice\n" +
                "AudioDevice=Audio.GenericAudioSubsystem\n" +
                "NetworkDevice=IpDrv.TcpNetDriver\n" +
                "Console=UTMenu.UTConsole\n" +
                "Language=int\n" +
                "GameEngine=Engine.GameEngine\n" +
                "EditorEngine=Editor.EditorEngine\n" +
                "DefaultGame=Botpack.DeathMatchPlus\n" +
                "DefaultServerGame=Botpack.DeathMatchPlus\n" +
                "ViewportManager=NSDLDrv.NSDLClient\n" +
                "Render=Render.Render\n" +
                "Input=Engine.Input\n" +
                "Canvas=Engine.Canvas\n" +
                "CdPath=../\n" +
                "\n" +
                "[Core.System]\n" +
                "PurgeCacheDays=30\n" +
                "SavePath=../Save\n" +
                "CachePath=../Cache\n" +
                "CacheExt=.uxx\n" +
                "Paths=../System/*.u\n" +
                "Paths=../Maps/*.unr\n" +
                "Paths=../Textures/*.utx\n" +
                "Paths=../Sounds/*.uax\n" +
                "Paths=../Music/*.umx\n" +
                "Suppress=DevLoad\n" +
                "Suppress=DevSave\n" +
                "Suppress=DevNetTraffic\n" +
                "\n" +
                "[Engine.GameEngine]\n" +
                "CacheSizeMegs=4\n" +
                "UseSound=True\n" +
                "ServerActors=IpDrv.UdpBeacon\n" +
                "ServerPackages=Core\n" +
                "ServerPackages=Engine\n" +
                "ServerPackages=Fire\n" +
                "ServerPackages=Botpack\n" +
                "\n" +
                "[NSDLDrv.NSDLClient]\n" +
                "DefaultDisplay=0\n" +
                "StartupFullscreen=True\n" +
                "UseJoystick=True\n" +
                "DeadZoneXYZ=0.100000\n" +
                "DeadZoneRUV=0.100000\n" +
                "ScaleXYZ=100.000000\n" +
                "ScaleRUV=100.000000\n" +
                "InvertY=False\n" +
                "InvertV=False\n" +
                "\n" +
                "[NOpenGLESDrv.NOpenGLESRenderDevice]\n" +
                "NoFiltering=False\n" +
                "Overbright=True\n" +
                "DetailTextures=False\n" +
                "UseVAO=False\n" +
                "UseBGRA=False\n" +
                "\n" +
                "[Audio.GenericAudioSubsystem]\n" +
                "UseFilter=True\n" +
                "UseSurround=False\n" +
                "UseStereo=True\n" +
                "UseCDMusic=False\n" +
                "UseDigitalMusic=True\n" +
                "UseSpatial=False\n" +
                "UseReverb=False\n" +
                "Use3dHardware=False\n" +
                "LowSoundQuality=False\n" +
                "ReverseStereo=False\n" +
                "Latency=40\n" +
                "OutputRate=22050Hz\n" +
                "Channels=16\n" +
                "MusicVolume=192\n" +
                "SoundVolume=224\n" +
                "AmbientFactor=0.700000\n" +
                "\n" +
                "[IpDrv.TcpNetDriver]\n" +
                "AllowDownloads=False\n" +
                "ConnectionTimeout=15.0\n" +
                "InitialConnectTimeout=30.0\n" +
                "AckTimeout=1.0\n" +
                "KeepAliveTime=0.2\n" +
                "MaxClientRate=20000\n" +
                "SimLatency=0\n" +
                "RelevantTimeout=5.0\n" +
                "SpawnPrioritySeconds=1.0\n" +
                "ServerTravelPause=4.0\n" +
                "NetServerMaxTickRate=20\n" +
                "LanServerMaxTickRate=35\n";
    }

    private static String buildAndroidUserIni() {
        return "[DefaultPlayer]\n" +
                "Name=Player\n" +
                "Class=Botpack.TMale1\n" +
                "team=0\n" +
                "skin=CommandoSkins.cmdo\n" +
                "Face=CommandoSkins.Blake\n" +
                "\n" +
                "[Engine.Input]\n" +
"\n" +
"; UT99_ANDROID_GAMEPAD_BINDS_V39\n" +
"W=MoveForward\n" +
"S=MoveBackward\n" +
"A=StrafeLeft\n" +
"D=StrafeRight\n" +
"Space=Jump\n" +
"LeftMouse=Fire\n" +
"RightMouse=AltFire\n" +
"MouseX=Axis aMouseX Speed=1.0\n" +
"MouseY=Axis aMouseY Speed=1.0\n" +
                "Aliases[0]=(Command=\"Button bFire | Fire\",Alias=Fire)\n" +
                "Aliases[1]=(Command=\"Button bAltFire | AltFire\",Alias=AltFire)\n" +
                "Aliases[2]=(Command=\"Axis aBaseY Speed=+300.0\",Alias=MoveForward)\n" +
                "Aliases[3]=(Command=\"Axis aBaseY Speed=-300.0\",Alias=MoveBackward)\n" +
                "Aliases[4]=(Command=\"Axis aBaseX Speed=-150.0\",Alias=TurnLeft)\n" +
                "Aliases[5]=(Command=\"Axis aBaseX Speed=+150.0\",Alias=TurnRight)\n" +
                "Aliases[6]=(Command=\"Axis aStrafe Speed=-300.0\",Alias=StrafeLeft)\n" +
                "Aliases[7]=(Command=\"Axis aStrafe Speed=+300.0\",Alias=StrafeRight)\n" +
                "Aliases[8]=(Command=\"Jump | Axis aUp Speed=+300.0\",Alias=Jump)\n" +
                "Aliases[9]=(Command=\"Button bDuck | Axis aUp Speed=-300.0\",Alias=Duck)\n" +
                "Escape=ShowMenu\n" +
                "Space=Jump\n" +
                "Enter=Fire\n" +
                "LeftMouse=Fire\n" +
                "RightMouse=AltFire\n" +
                "Up=MoveForward\n" +
                "Down=MoveBackward\n" +
                "Left=TurnLeft\n" +
                "Right=TurnRight\n";
    }

    static String dataMessage(Context context) {
        File preferred = preferredRoot(context);
        File legacy = legacyRoot(context);
        return "Unreal Tournament data missing\n\n" +
                "Install or copy the game folders to:\n" +
                preferred.getAbsolutePath() + "\n\n" +
                "Also accepted for older test builds:\n" +
                legacy.getAbsolutePath() + "\n\n" +
                "Required:\n" +
                "System, Maps, Textures, Sounds, Music\n\n" +
                "You can now select either the Unreal Tournament folder or a ZIP containing these folders.";
    }
}
