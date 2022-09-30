/*
 * Copyright (c) 2021 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.du;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.nio.file.Paths;
import java.util.logging.Level;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import discUtils.core.DiscFileSystem;
import discUtils.core.FileSystemInfo;
import discUtils.core.FileSystemManager;
import discUtils.core.FileSystemParameters;
import discUtils.core.LogicalVolumeInfo;
import discUtils.core.PhysicalVolumeInfo;
import discUtils.core.VirtualDisk;
import discUtils.core.VolumeManager;
import dotnet4j.io.FileAccess;


/**
 * GuiMain.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2021/12/11 umjammer initial version <br>
 */
@PropsEntity(url = "file://${user.dir}/local.properties")
public class GuiMain {

    static {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod", "sun\\.util\\.logging\\..+#.+");
    }

    @Property
    String discImage;

    public static void main(String[] args) throws Exception {
        GuiMain app = new GuiMain();
        PropsEntity.Util.bind(app);
        app.test1();
    }

    void test1() throws Exception {
        String forceType = null;
        VirtualDisk disk = VirtualDisk.openDisk(discImage, forceType, FileAccess.Read, null, null);
Debug.println(Level.FINE, "disk: " + disk);
        VolumeManager manager = new VolumeManager();
Debug.println(Level.FINE, "manager: " + manager);
        manager.addDisk(disk);
        int pvc = 0;
        for (PhysicalVolumeInfo pvi : manager.getPhysicalVolumes()) {
Debug.println(Level.FINE, "pvi[" + pvc++ + "]: " + pvi);
        }
        int lvc = 0;
        for (LogicalVolumeInfo lvi : manager.getLogicalVolumes()) {
Debug.println(Level.FINE, "lvi[" + lvc++ + "]: " + lvi);
            int fsc = 0;
            for (FileSystemInfo fsi : FileSystemManager.detectFileSystems(lvi)) {
Debug.println(Level.FINE, "fsi[" + fsc++ + "]: " + fsi);
                DiscFileSystem fs = fsi.open(lvi, new FileSystemParameters());
Debug.println(Level.FINE, "fs: " + fs);
            }
        }

        JPanel basePanel = new JPanel();
        basePanel.setLayout(new GridLayout(4, 2));

        JLabel label = new JLabel(Paths.get(discImage).getFileName().toString());
        basePanel.add(label);

        JPanel diskPanel = new JPanel(new FlowLayout());
        diskPanel.setBackground(Color.blue);
        for (PhysicalVolumeInfo pvi : manager.getPhysicalVolumes()) {
            JLabel pvPanel = new JLabel(pvi.toString());
            pvPanel.setBorder(new LineBorder(Color.cyan, 10));
            pvPanel.setForeground(Color.white);
            diskPanel.add(pvPanel);
        }
        basePanel.add(diskPanel);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(basePanel);

        JFrame frame = new JFrame();
        frame.setTitle("Virtual Disk Utility");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(scrollPane);
        frame.pack();
        frame.setVisible(true);
    }
}

/* */
