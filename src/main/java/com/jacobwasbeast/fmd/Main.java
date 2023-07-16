package com.jacobwasbeast.fmd;

import com.jcraft.jsch.*;
import com.mojang.logging.LogUtils;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Main.MODID)
public class Main {
    public static boolean isRunonce = false;
    public static String ip = "";
    public static String port = "";
    public static String user = "";
    public static String pass = "";
    // Define mod id in a common place for everything to reference
    public static final String MODID = "fmd";
    public Main() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            if (!Main.isRunonce) {
                try {
                    System.out.println("Downloading mods");
                    if (Main.sync(ip, port, user, pass)) {
                        if (!Main.todownload.isEmpty())
                            Main.showmessage("Downloaded " + Main.todownload.size() + " mods, please restart your game");
                    } else {
                        Main.showmessage("Failed to download mods error code: 0");
                    }
                    Main.isRunonce = true;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    public static ArrayList<String> list = new ArrayList<>();

    public static ArrayList<String> todownload = new ArrayList<>();



    public static void showmessage(String message) throws Throwable {
        // crash the game with a message
        throw new Throwable("THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR THIS IS NOT A ERROR " + message);
    }
    public static boolean sync(String ftpserver, String port, String username, String password) throws Throwable {
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            JSch jsch = new JSch();
            JSch.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            JSch.setConfig("CheckCiphers", "aes256-ctr,aes192-ctr,aes128-ctr");

            Session session = jsch.getSession(username, ftpserver, Integer.parseInt(port));
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            System.out.println("Connected to " + ftpserver + ":" + port + " as " + username);
            ChannelSftp sftpChannel = (ChannelSftp) channel;


            Path path = Path.of(System.getProperty("user.dir"));
            Path mods = path.resolve("mods");
            File localFolder = new File(mods.toString());

            setTodownload(sftpChannel, localFolder, todownload);

            sftpChannel.exit();
            session.disconnect();

            return true;
        } catch (JSchException | SftpException | IOException e) {
            // handle exceptions
            showmessage("Failed to sync with server " + e.toString());
            return false;
        }
    }

    public static void setTodownload(ChannelSftp sftpChannel, File folder, ArrayList<String> names) throws SftpException, IOException {
        Vector<ChannelSftp.LsEntry> list = sftpChannel.ls("/");
        for (ChannelSftp.LsEntry entry : list) {
            String fileName = entry.getFilename();
            if (!fileName.equals(".") && !fileName.equals("..")) {
                if (!names.contains(fileName)) {
                    String localPath = folder.getPath();
                    String remotePath = "/";
                    String remoteFilePath = remotePath + fileName;
                    String localFilePath = localPath + File.separator + fileName;

                    if (entry.getAttrs().isDir()) {
                        (new File(localFilePath)).mkdirs();
                        setTodownload(sftpChannel, new File(localFilePath), names);  // Recursively download directory
                    } else {
                        if (!new File(localFilePath).exists()) {
                            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(localFilePath));
                            sftpChannel.get(remoteFilePath, outputStream);
                            outputStream.close();
                            todownload.add(fileName);
                        }
                    }
                }
            }
        }
    }

    public static byte[] readFileToBytes(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] bytes = new byte[(int)file.length()];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            fis.read(bytes);
        } finally {
            if (fis != null)
                fis.close();
        }
        return bytes;
    }
}
