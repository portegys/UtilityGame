/*
 * This software is provided under the terms of the GNU General
 * Public License as published by the Free Software Foundation.
 *
 * Copyright (c) 2005 Tom Portegys, All Rights Reserved.
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.
 */
// Game items.
package UtilityGame;

import java.awt.*;
import java.awt.event.*;

import java.net.*;

import java.util.*;

import javax.swing.*;


public class GameItems extends JPanel implements Runnable {
    // Number of items.
    // Note: update constructor when changing this.
    static final int NUM_ITEMS = 6;

    // Dimensions.
    static final int ITEM_PANEL_WIDTH = 550;
    static final int ITEM_PANEL_HEIGHT = 40;

    // Display frequency.
    static final int DISPLAY_FREQUENCY = 100;

    // Items.
    GameItem[] items;
    CheckboxGroup checkgroup;
    boolean master;
    private Thread displayThread;

    // Constructor.
    public GameItems(boolean master, Dimension size) {
        this.master = master;
        setSize(size.width, size.height);
        setLayout(new GridLayout(NUM_ITEMS, 1));
        items = new GameItem[NUM_ITEMS];
        checkgroup = new CheckboxGroup();

        for (int i = 0; i < NUM_ITEMS; i++) {
            items[i] = new GameItem(new Dimension(size.width,
                        size.height / NUM_ITEMS));
            add(items[i]);
        }

        URL imgURL = GameItems.class.getResource("square.gif");

        if (imgURL != null) {
            items[0].setImage(new ImageIcon(imgURL).getImage());
        } else {
            System.err.println("Cannot find image file square.gif");
        }

        imgURL = GameItems.class.getResource("circle.gif");

        if (imgURL != null) {
            items[1].setImage(new ImageIcon(imgURL).getImage());
        } else {
            System.err.println("Cannot find image file circle.gif");
        }

        imgURL = GameItems.class.getResource("triangle.gif");

        if (imgURL != null) {
            items[2].setImage(new ImageIcon(imgURL).getImage());
        } else {
            System.err.println("Cannot find image file triangle.gif");
        }

        imgURL = GameItems.class.getResource("star.gif");

        if (imgURL != null) {
            items[3].setImage(new ImageIcon(imgURL).getImage());
        } else {
            System.err.println("Cannot find image file star.gif");
        }

        imgURL = GameItems.class.getResource("cross.gif");

        if (imgURL != null) {
            items[4].setImage(new ImageIcon(imgURL).getImage());
        } else {
            System.err.println("Cannot find image file cross.gif");
        }

        imgURL = GameItems.class.getResource("octagon.gif");

        if (imgURL != null) {
            items[5].setImage(new ImageIcon(imgURL).getImage());
        } else {
            System.err.println("Cannot find image file octagon.gif");
        }

        // Start display thread.
        startDisplay();
    }

    // Get selected item (-1 if none).
    int getSelectedItem() {
        for (int i = 0; i < NUM_ITEMS; i++) {
            if (items[i].isSelected()) {
                return i;
            }
        }

        return -1;
    }

    // Reset.
    void resetItems() {
        for (int i = 0; i < NUM_ITEMS; i++) {
            items[i].reset();
        }
    }

    // Scramble item descriptions.
    void scrambleItems() {
        int i;
        int j;
        Image image;
        String name;

        Random random = new Random(new Date().getTime());

        for (i = 0; i < NUM_ITEMS; i++) {
            j = random.nextInt(NUM_ITEMS);
            image = items[i].image;
            name = items[i].name;
            items[i].image = items[j].image;
            items[i].name = items[j].name;
            items[j].image = image;
            items[j].name = name;
            items[i].erase = true;
        }
    }

    // Update item constants.
    void updateConstants(String[] Avals, String[] Xvals, String[] Yvals) {
        for (int i = 0; i < NUM_ITEMS; i++) {
            items[i].updateConstants(Avals[i], Xvals[i], Yvals[i]);
        }
    }

    // Update item values.
    void updateValues(String[] Svals, String[] Dvals, double noise) {
        int i;
        int j;
        int k;

        int[] s = new int[NUM_ITEMS];
        int[] d = new int[NUM_ITEMS];
        int[] sdec = new int[NUM_ITEMS];
        int[] ddec = new int[NUM_ITEMS];

        for (i = 0; i < NUM_ITEMS; i++) {
            s[i] = Integer.parseInt(Svals[i].trim());
            sdec[i] = 0;
            d[i] = Integer.parseInt(Dvals[i].trim());
            ddec[i] = 0;
        }

        if (noise > 0.0) {
            Random r = new Random(new Date().getTime());

            for (i = 0; i < NUM_ITEMS; i++) {
                for (j = 0; j < s[i]; j++) {
                    if (r.nextDouble() <= noise) {
                        sdec[i]++;

                        for (k = 0; k < NUM_ITEMS; k++) {
                            if (k != i) {
                                ddec[k]++;
                            }
                        }
                    }
                }
            }
        }

        for (i = 0; i < NUM_ITEMS; i++) {
            items[i].updateValues(Svals[i], Dvals[i], sdec[i], ddec[i]);
        }
    }

    // Show visible items.
    void showVisible() {
        for (int i = 0; i < NUM_ITEMS; i++) {
            remove(items[i]);
        }

        for (int i = 0; i < NUM_ITEMS; i++) {
            if (items[i].visible) {
                add(items[i]);
            }
        }
    }

    // Display thread loop.
    public void run() {
        if (Thread.currentThread() != displayThread) {
            return;
        }

        displayThread.setPriority(Thread.MIN_PRIORITY);

        while (!displayThread.isInterrupted()) {
            for (int i = 0; i < NUM_ITEMS; i++) {
                if (master) {
                    items[i].enableItem(items[i].isSelected());
                }

                items[i].refresh();
            }

            try {
                Thread.sleep(DISPLAY_FREQUENCY);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    // Start display thread.
    void startDisplay() {
        if (displayThread == null) {
            displayThread = new Thread(this);
            displayThread.start();
        }
    }

    // Stop display thread.
    void stopDisplay() {
        if (displayThread != null) {
            displayThread.interrupt();

            try {
                displayThread.join();
            } catch (InterruptedException e) {
            }

            displayThread = null;
        }
    }

    // Main.
    public static void main(String[] args) {
        GameItems gameItems;

        JFrame frame = new JFrame("Game Items");
        frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
        frame.setSize(ITEM_PANEL_WIDTH,
            ITEM_PANEL_HEIGHT * (int) ((double) NUM_ITEMS * 1.5));
        frame.getContentPane().setLayout(new FlowLayout());

        if ((args.length == 1) && args[0].equals("master")) {
            gameItems = new GameItems(true,
                    new Dimension(ITEM_PANEL_WIDTH + 1,
                        ITEM_PANEL_HEIGHT * NUM_ITEMS));
        } else {
            gameItems = new GameItems(false,
                    new Dimension(ITEM_PANEL_WIDTH + 1,
                        ITEM_PANEL_HEIGHT * NUM_ITEMS));
        }

        frame.getContentPane().add(gameItems);
        frame.setVisible(true);
    }

    // Item.
    class GameItem extends JPanel {
        Canvas display;
        Dimension displaySize;
        Image image;
        String name;
        Checkbox check;
        TextField Atext;
        TextField Xtext;
        TextField Stext;
        TextField Ytext;
        TextField Dtext;
        TextField Utext;
        JLabel labelY0;
        JLabel labelY1;
        JLabel labelD0;
        JLabel labelD1;
        JLabel labelU0;
        JLabel labelU1;
        double a;
        double x;
        double y;
        double u;
        int s;
        int d;
        Font font;
        FontMetrics fontMetrics;
        boolean enabled;
        boolean erase;
        boolean visible;

        // Constructor.
        GameItem(Dimension size) {
            setSize(size.width, size.height);
            display = new Canvas();
            displaySize = new Dimension(size.height, size.height);
            display.setSize(displaySize);
            image = null;
            name = null;
            Atext = new TextField("0.0");
            Xtext = new TextField("0.0");
            Stext = new TextField("0");
            Stext.setEditable(false);
            Ytext = new TextField("NA");
            Dtext = new TextField("0");
            Dtext.setEditable(false);
            Utext = new TextField("0.0");
            Utext.setEditable(false);

            if (master) {
                check = new Checkbox("", true);
            } else {
                check = new Checkbox("", checkgroup, false);
                Atext.setEditable(false);
                Xtext.setEditable(false);
                Ytext.setEditable(false);
            }

            a = x = y = u = 0.0;
            s = d = 0;
            setLayout(new FlowLayout());
            add(display);
            add(check);

            JLabel label = new JLabel("A:");
            label.setToolTipText("Utility constant");
            add(label);
            add(Atext);
            add(new JLabel("+"));
            label = new JLabel("(X:");
            label.setToolTipText("Same choosers weight");
            add(label);
            add(Xtext);
            add(new JLabel("*"));
            label = new JLabel("S:");
            label.setToolTipText("Number of players choosing this item");
            add(label);
            add(Stext);
            labelY0 = new JLabel(") +");

            if (master) {
                add(labelY0);
            }

            labelY1 = new JLabel("(Y:");
            labelY1.setToolTipText("Different choosers weight");

            if (master) {
                add(labelY1);
            }

            if (master) {
                add(Ytext);
            }

            labelD0 = new JLabel("*");

            if (master) {
                add(labelD0);
            }

            labelD1 = new JLabel("D:");
            labelD1.setToolTipText("Number of players not choosing this item");

            if (master) {
                add(labelD1);
            }

            if (master) {
                add(Dtext);
            }

            labelU0 = new JLabel(") =");
            add(labelU0);
            labelU1 = new JLabel("U:");
            labelU1.setToolTipText("Utility of this item");
            add(labelU1);
            add(Utext);
            font = new Font("Helvetica", Font.BOLD, 12);
            enabled = true;
            erase = false;
            visible = true;
        }

        // Set the item image.
        void setImage(Image image) {
            this.image = image;
        }

        // Set the item name.
        void setItemName(String name) {
            this.name = name;
        }

        // Set the item visibility.
        void setItemVisible(boolean visible) {
            this.visible = visible;
        }

        // Enable/disable item for selection.
        void enableItem(boolean enabled) {
            if (enabled != this.enabled) {
                erase = true;
            }

            this.enabled = enabled;

            if (!master) {
                check.setEnabled(enabled);
            }
        }

        // Item selected?
        boolean isSelected() {
            if (master || enabled) {
                return check.getState();
            } else {
                return false;
            }
        }

        // Reset.
        void reset() {
            enableItem(true);
            check.setState(false);
            Atext.setText("0.0");
            Xtext.setText("0.0");
            Stext.setText("0");
            Ytext.setText("NA");
            Dtext.setText("0");
            Utext.setText("0.0");
            a = x = y = u = 0.0;
            s = d = 0;
        }

        // Update constants.
        void updateConstants(String Aval, String Xval, String Yval) {
            a = Double.parseDouble(Aval.trim());
            Atext.setText(Aval);
            x = Double.parseDouble(Xval.trim());
            Xtext.setText(Xval);

            if (Yval.equals("NA")) {
                y = 0.0;
            } else {
                y = Double.parseDouble(Yval.trim());
            }

            Ytext.setText(Yval);

            if (master) {
                return;
            }

            synchronized (this) {
                erase = true;
                remove(labelY0);
                remove(labelY1);
                remove(Ytext);
                remove(labelD0);
                remove(labelD1);
                remove(Dtext);
                remove(labelU0);
                remove(labelU1);
                remove(Utext);

                if (!Yval.equals("NA")) {
                    add(labelY0);
                    add(labelY1);
                    add(Ytext);
                    add(labelD0);
                    add(labelD1);
                    add(Dtext);
                }

                add(labelU0);
                add(labelU1);
                add(Utext);
            }
        }

        // Update values; compute utility.
        void updateValues(String Sval, String Dval, int sdec, int ddec) {
            s = Integer.parseInt(Sval.trim());
            Stext.setText("" + (s - sdec));
            d = Integer.parseInt(Dval.trim());
            Dtext.setText("" + (d - ddec));
            u = a + (x * (double) (s - sdec)) + (y * (double) (d - ddec));
            Utext.setText("" + u);
            u = a + (x * (double) s) + (y * (double) d);

            if (master) {
                return;
            }

            synchronized (this) {
                erase = true;
                remove(labelY0);
                remove(labelY1);
                remove(Ytext);
                remove(labelD0);
                remove(labelD1);
                remove(Dtext);
                remove(labelU0);
                remove(labelU1);
                remove(Utext);

                if (!Ytext.getText().equals("NA")) {
                    add(labelY0);
                    add(labelY1);
                    add(Ytext);
                    add(labelD0);
                    add(labelD1);
                    add(Dtext);
                }

                add(labelU0);
                add(labelU1);
                add(Utext);
            }
        }

        // Get utility.
        double getUtility() {
            return Double.parseDouble(Utext.getText().trim());
        }

        // Refresh display.
        synchronized void refresh() {
            Graphics g;
            int x;
            int y;
            int d;
            int w;

            g = display.getGraphics();

            if (g == null) {
                return;
            }

            if (erase) {
                erase = false;
                g.setColor(display.getBackground());
                g.fillRect(0, 0, displaySize.width, displaySize.height);
            }

            if (name != null) {
                if (fontMetrics == null) {
                    g.setFont(font);
                    fontMetrics = g.getFontMetrics();
                }

                w = fontMetrics.stringWidth(name);
                g.setColor(Color.black);
                g.drawString(name, ((displaySize.width - w) / 2) + 1,
                    displaySize.height / 2);
            } else if (image != null) {
                if (displaySize.width > displaySize.height) {
                    d = (int) ((double) displaySize.height * .9);
                } else {
                    d = (int) ((double) displaySize.width * .9);
                }

                x = (int) ((double) (displaySize.width - d) / 2.0);
                y = (int) ((double) (displaySize.height - d) / 2.0);
                g.drawImage(image, x, y, d, d, this);
            }

            g.setColor(Color.black);
            g.drawLine(0, 0, displaySize.width, 0);
            g.drawLine(0, displaySize.height - 1, displaySize.width,
                displaySize.height - 1);
            g.drawLine(0, 0, 0, displaySize.height);
            g.drawLine(displaySize.width - 1, 0, displaySize.width - 1,
                displaySize.height);
        }
    }
}
