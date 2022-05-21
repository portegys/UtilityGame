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
// Utility game master.
package UtilityGame;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.net.*;
import java.net.URL;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.*;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;


public class GameMaster extends UnicastRemoteObject implements GameAction,
    ActionListener, ItemListener, ChangeListener, Runnable {
    // Dimensions.
    static final int GUI_WIDTH = GameItems.ITEM_PANEL_WIDTH;
    static final int GUI_HEIGHT = (GameItems.ITEM_PANEL_HEIGHT * (int) ((double) GameItems.NUM_ITEMS * 1.5)) +
        150;

    // RMI port.
    static final int PORT = 5001;

    // Controls.
    JFrame frame;
    GameItems items;
    TextField playerCount;
    Vector players;
    TextField gameNum;
    JCheckBox startGame;
    boolean gameStarted;
    TextField roundNum;
    JCheckBox startRound;
    boolean roundStarted;
    TextField roundTimer;
    TextField utilitySum;
    TextField choiceReleaseFee;
    JCheckBox allowChoiceRelease;
    JSlider noise;
    TextField noiseLevel;
    TextField logFileName;
    JCheckBox startLog;
    PrintWriter logFile;
    JButton textSend;
    TextField text;
    JButton quit;
    Object playersMutex;
    Object accumMutex;
    double utility;
    int[] choices;
    private Thread timerThread;

    // Constructor.
    public GameMaster() throws RemoteException {
        frame = new JFrame("Utility Game Master");
        frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    System.err.println("Please use quit button!");
                    System.exit(1);
                }
            });
        frame.setSize(GUI_WIDTH, GUI_HEIGHT);

        JPanel panel = (JPanel) frame.getContentPane();
        panel.setLayout(new FlowLayout());
        items = new GameItems(true,
                new Dimension(GameItems.ITEM_PANEL_WIDTH,
                    GameItems.ITEM_PANEL_HEIGHT * GameItems.NUM_ITEMS));
        panel.add(items);

        JPanel p = new JPanel();
        p.setLayout(new FlowLayout());
        panel.add(p);
        p.add(new JLabel("Players:"));
        playerCount = new TextField("0");
        playerCount.setEditable(false);
        p.add(playerCount);
        players = new Vector();
        p.add(new JLabel("Game #:"));
        gameNum = new TextField("0");
        p.add(gameNum);
        startGame = new JCheckBox("Start");
        startGame.addItemListener(this);
        p.add(startGame);
        gameStarted = false;
        p.add(new JLabel("Round #:"));
        roundNum = new TextField("0");
        p.add(roundNum);
        startRound = new JCheckBox("Start");
        startRound.addItemListener(this);
        p.add(startRound);
        roundStarted = false;
        p.add(new JLabel("Round timer:"));
        roundTimer = new TextField("0");
        roundTimer.setEditable(false);
        p.add(roundTimer);
        p = new JPanel();
        p.setLayout(new FlowLayout());
        panel.add(p);
        p.add(new JLabel("Choice release fee:"));
        choiceReleaseFee = new TextField("0.0");
        p.add(choiceReleaseFee);
        allowChoiceRelease = new JCheckBox("Enable");
        allowChoiceRelease.addItemListener(this);
        p.add(allowChoiceRelease);
        p.add(new JLabel("Log file:"));
        logFileName = new TextField("", 10);
        p.add(logFileName);
        startLog = new JCheckBox("Start");
        startLog.addItemListener(this);
        p.add(startLog);
        logFile = null;
        p = new JPanel();
        p.setLayout(new FlowLayout());
        panel.add(p);
        p.add(new JLabel("Noise: 0%", Label.RIGHT));
        noise = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        noise.addChangeListener(this);
        p.add(noise);
        p.add(new JLabel("100%", Label.LEFT));
        p.add(new JLabel("Level:"));
        noiseLevel = new TextField("0%");
        noiseLevel.setEditable(false);
        p.add(noiseLevel);
        p = new JPanel();
        p.setLayout(new FlowLayout());
        panel.add(p);
        p.add(new JLabel("Final game utility:"));
        utilitySum = new TextField("0.0");
        utilitySum.setEditable(false);
        p.add(utilitySum);
        textSend = new JButton("Notify players:");
        textSend.addActionListener(this);
        p.add(textSend);
        text = new TextField("", 20);
        p.add(text);
        quit = new JButton("Quit");
        quit.addActionListener(this);
        p.add(quit);
        frame.setVisible(true);
        playersMutex = new Object();
        accumMutex = new Object();
        utility = 0.0;
        choices = new int[GameItems.NUM_ITEMS];

        for (int i = 0; i < GameItems.NUM_ITEMS; i++)
            choices[i] = 0;

        // Set up RMI registry.
        //System.setSecurityManager(new RMISecurityManager());
        try {
            LocateRegistry.createRegistry(PORT);
            Naming.rebind("//:" + PORT + "/UtilityGame.GameMaster", this);
        } catch (Exception e) {
            System.err.println("RMI registration failed: " + e.toString());
        }

        // Start round timer thread.
        timerThread = new Thread(this);
        timerThread.start();
    }

    // Button listener.
    public void actionPerformed(ActionEvent evt) {
        GameUpdate player;

        // Quit?
        if (evt.getSource() == quit) {
            synchronized (playersMutex) {
                for (int i = 0; i < players.size(); i++) {
                    player = (GameUpdate) players.get(i);

                    try {
                        player.dropConnection();
                    } catch (Exception e) {
                    }
                }
            }

            System.exit(0);
        }

        // Send notification?
        if (evt.getSource() == textSend) {
            String s = text.getText().trim();

            if (s.equals("")) {
                return;
            }

            synchronized (playersMutex) {
                for (int i = 0; i < players.size(); i++) {
                    player = (GameUpdate) players.get(i);

                    try {
                        player.gameNotification(s);
                    } catch (Exception e) {
                        players.removeElement(player);
                        i--;
                    }
                }

                playerCount.setText("" + players.size());
            }

            return;
        }
    }

    // Start listener.
    public void itemStateChanged(ItemEvent evt) {
        int i;
        int j;
        int n;
        double d;
        double d2;
        boolean[] enabledItems;
        String[] avals;
        String[] xvals;
        String[] svals;
        String[] yvals;
        String[] dvals;
        GameUpdate player;
        GameStarter[] gameStarters;
        GameStopper[] gameStoppers;
        RoundStarter[] roundStarters;
        RoundStopper[] roundStoppers;

        if (evt.getSource() == startGame) {
            if (!gameStarted) {
                if (!startGame.isSelected()) {
                    return;
                }

                n = Integer.parseInt(gameNum.getText().trim());

                synchronized (playersMutex) {
                    gameStarters = new GameStarter[players.size()];

                    for (i = 0; i < players.size(); i++) {
                        player = (GameUpdate) players.get(i);
                        gameStarters[i] = new GameStarter(player, n);
                        gameStarters[i].start();
                    }
                }

                for (i = 0; i < gameStarters.length; i++) {
                    try {
                        gameStarters[i].join();
                    } catch (Exception e) {
                    }
                }

                if (logFile != null) {
                    logFile.println("Start game " + gameNum.getText().trim() +
                        " " + new Date().toString());
                    logFile.println("Players=" + players.size());
                    logFile.flush();
                }

                playerCount.setText("" + players.size());
                startGame.setText("Stop");
                utilitySum.setText("0.0");
                gameStarted = true;
            } else {
                if (startGame.isSelected()) {
                    return;
                }

                if (startRound.isSelected()) {
                    startGame.setSelected(true);

                    return;
                }

                avals = new String[GameItems.NUM_ITEMS];
                xvals = new String[GameItems.NUM_ITEMS];
                svals = new String[GameItems.NUM_ITEMS];
                yvals = new String[GameItems.NUM_ITEMS];
                dvals = new String[GameItems.NUM_ITEMS];

                for (i = 0; i < GameItems.NUM_ITEMS; i++) {
                    avals[i] = items.items[i].Atext.getText();
                    xvals[i] = items.items[i].Xtext.getText();
                    svals[i] = items.items[i].Stext.getText();
                    yvals[i] = items.items[i].Ytext.getText();
                    dvals[i] = items.items[i].Dtext.getText();
                }

                utility = 0.0;

                synchronized (playersMutex) {
                    gameStoppers = new GameStopper[players.size()];

                    for (i = 0; i < players.size(); i++) {
                        player = (GameUpdate) players.get(i);
                        gameStoppers[i] = new GameStopper(player, avals, xvals,
                                svals, yvals, dvals);
                        gameStoppers[i].start();
                    }
                }

                for (i = 0; i < gameStoppers.length; i++) {
                    try {
                        gameStoppers[i].join();
                    } catch (Exception e) {
                    }
                }

                if (logFile != null) {
                    logFile.println("Stop game " + gameNum.getText().trim() +
                        " " + new Date().toString());
                    logFile.println("Players=" + players.size());
                    logFile.println("Accumulated utility=" + utility);
                    logFile.flush();
                }

                playerCount.setText("" + players.size());
                utilitySum.setText("" + utility);
                startGame.setText("Start");
                n = Integer.parseInt(gameNum.getText().trim()) + 1;
                gameNum.setText("" + n);
                roundNum.setText("0");
                gameStarted = false;
            }

            return;
        }

        if (evt.getSource() == startRound) {
            if (!roundStarted) {
                if (!startRound.isSelected()) {
                    return;
                }

                if (!gameStarted) {
                    startRound.setSelected(false);

                    return;
                }

                n = Integer.parseInt(roundNum.getText().trim());
                enabledItems = new boolean[GameItems.NUM_ITEMS];

                for (i = 0; i < GameItems.NUM_ITEMS; i++) {
                    enabledItems[i] = items.items[i].isSelected();
                }

                avals = new String[GameItems.NUM_ITEMS];
                xvals = new String[GameItems.NUM_ITEMS];
                svals = new String[GameItems.NUM_ITEMS];
                yvals = new String[GameItems.NUM_ITEMS];
                dvals = new String[GameItems.NUM_ITEMS];

                for (i = 0; i < GameItems.NUM_ITEMS; i++) {
                    avals[i] = items.items[i].Atext.getText();
                    xvals[i] = items.items[i].Xtext.getText();
                    svals[i] = items.items[i].Stext.getText();
                    yvals[i] = items.items[i].Ytext.getText();
                    dvals[i] = items.items[i].Dtext.getText();
                }

                if (!allowChoiceRelease.isSelected()) {
                    d = -1.0;
                } else {
                    d = Double.parseDouble(choiceReleaseFee.getText().trim());
                }

                d2 = (double) noise.getValue() / 100.0;

                synchronized (playersMutex) {
                    roundStarters = new RoundStarter[players.size()];

                    for (i = 0; i < players.size(); i++) {
                        player = (GameUpdate) players.get(i);
                        roundStarters[i] = new RoundStarter(player, n,
                                enabledItems, avals, xvals, svals, yvals,
                                dvals, d, d2);
                        roundStarters[i].start();
                    }
                }

                for (i = 0; i < roundStarters.length; i++) {
                    try {
                        roundStarters[i].join();
                    } catch (Exception e) {
                    }
                }

                playerCount.setText("" + players.size());
                startRound.setText("Stop");
                roundStarted = true;
            } else {
                if (startRound.isSelected()) {
                    return;
                }

                avals = new String[GameItems.NUM_ITEMS];
                xvals = new String[GameItems.NUM_ITEMS];
                svals = new String[GameItems.NUM_ITEMS];
                yvals = new String[GameItems.NUM_ITEMS];
                dvals = new String[GameItems.NUM_ITEMS];

                for (i = 0; i < GameItems.NUM_ITEMS; i++) {
                    avals[i] = items.items[i].Atext.getText();
                    xvals[i] = items.items[i].Xtext.getText();
                    svals[i] = items.items[i].Stext.getText();
                    yvals[i] = items.items[i].Ytext.getText();
                    dvals[i] = items.items[i].Dtext.getText();
                }

                for (i = 0; i < GameItems.NUM_ITEMS; i++) {
                    choices[i] = 0;
                }

                synchronized (playersMutex) {
                    roundStoppers = new RoundStopper[players.size()];

                    for (i = 0; i < players.size(); i++) {
                        player = (GameUpdate) players.get(i);
                        roundStoppers[i] = new RoundStopper(player);
                        roundStoppers[i].start();
                    }
                }

                for (i = 0; i < roundStoppers.length; i++) {
                    try {
                        roundStoppers[i].join();
                    } catch (Exception e) {
                    }
                }

                for (i = 0; i < GameItems.NUM_ITEMS; i++) {
                    svals[i] = "" + choices[i];

                    for (j = n = 0; j < GameItems.NUM_ITEMS; j++) {
                        if (j != i) {
                            n += choices[j];
                        }
                    }

                    dvals[i] = "" + n;
                }

                items.updateValues(avals, xvals, svals, yvals, dvals, 0.0);

                if (logFile != null) {
                    logFile.println("Round=" + roundNum.getText().trim());

                    if (allowChoiceRelease.isSelected()) {
                        logFile.println("Choice release fee=" +
                            choiceReleaseFee.getText().trim());
                    } else {
                        logFile.println("Choice release fee=NA");
                    }

                    logFile.println("Noise=" +
                        ((double) noise.getValue() / 100.0) + "%");
                    logFile.println("Choice\tAvailable\tA\tX\tS\tY\tD\tU");

                    for (i = 0; i < GameItems.NUM_ITEMS; i++) {
                        logFile.print("" + i + "\t");

                        if (items.items[i].isSelected()) {
                            logFile.print("Y\t");
                        } else {
                            logFile.print("N\t");
                        }

                        logFile.println("\t" + avals[i] + "\t" + xvals[i] +
                            "\t" + svals[i] + "\t" + yvals[i] + "\t" +
                            dvals[i] + "\t" + items.items[i].u);
                    }

                    logFile.flush();
                }

                playerCount.setText("" + players.size());
                startRound.setText("Start");
                n = Integer.parseInt(roundNum.getText().trim()) + 1;
                roundNum.setText("" + n);
                roundTimer.setText("0");
                roundStarted = false;
            }

            return;
        }

        if (evt.getSource() == allowChoiceRelease) {
            if (allowChoiceRelease.isSelected()) {
                allowChoiceRelease.setText("Disable");
            } else {
                allowChoiceRelease.setText("Enable");
            }

            return;
        }

        if (evt.getSource() == startLog) {
            if (startLog.isSelected()) {
                String fileName = logFileName.getText().trim();

                if (fileName.equals("")) {
                    startLog.setSelected(false);

                    return;
                }

                if (logFile != null) {
                    logFile.close();
                    logFile = null;
                }

                try {
                    logFile = new PrintWriter(new FileOutputStream(fileName));
                    logFile.println("Log created: " + new Date().toString());
                    logFile.flush();
                } catch (IOException e) {
                    logFile = null;
                    startLog.setSelected(false);

                    return;
                }

                startLog.setText("Stop");
            } else {
                if (logFile != null) {
                    logFile.println("Log closed: " + new Date().toString());
                    logFile.close();
                    logFile = null;
                }

                startLog.setText("Start");
            }

            return;
        }
    }

    // Noise slider listener.
    public void stateChanged(ChangeEvent evt) {
        noiseLevel.setText("" + noise.getValue() + "%");
    }

    // Round timer thread loop.
    public void run() {
        int i;

        if (Thread.currentThread() != timerThread) {
            return;
        }

        timerThread.setPriority(Thread.MIN_PRIORITY);

        while (!timerThread.isInterrupted()) {
            if (roundStarted) {
                i = Integer.parseInt(roundTimer.getText().trim()) + 1;
                roundTimer.setText("" + i);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    // GameAction implementation:
    // Register player.
    public synchronized boolean register(GameUpdate player) {
        if (startGame.isSelected()) {
            return false;
        }

        if (!(players.contains(player))) {
            players.addElement(player);
        }

        playerCount.setText("" + players.size());

        return true;
    }

    // Unregister player.
    public synchronized void unregister(GameUpdate player) {
        players.removeElement(player);
        playerCount.setText("" + players.size());
    }

    public static void main(String[] args) {
        try {
            new GameMaster();
        } catch (Exception e) {
            System.err.println("Cannot create GameMaster: " + e.toString());
            System.exit(1);
        }
    }

    // Game starter.
    class GameStarter extends Thread {
        GameUpdate player;
        int n;

        GameStarter(GameUpdate player, int n) {
            this.player = player;
            this.n = n;
        }

        public void run() {
            if (Thread.currentThread() != this) {
                return;
            }

            try {
                player.startGame(n);
            } catch (Exception e) {
                synchronized (playersMutex) {
                    players.removeElement(player);
                }
            }
        }
    }

    // Game stopper.
    class GameStopper extends Thread {
        GameUpdate player;
        String[] avals;
        String[] xvals;
        String[] svals;
        String[] yvals;
        String[] dvals;

        GameStopper(GameUpdate player, String[] avals, String[] xvals,
            String[] svals, String[] yvals, String[] dvals) {
            this.player = player;
            this.avals = avals;
            this.xvals = xvals;
            this.svals = svals;
            this.yvals = yvals;
            this.dvals = dvals;
        }

        public void run() {
            if (Thread.currentThread() != this) {
                return;
            }

            try {
                synchronized (accumMutex) {
                    utility += player.stopGame(avals, xvals, svals, yvals, dvals);
                }
            } catch (Exception e) {
                synchronized (playersMutex) {
                    players.removeElement(player);
                }
            }
        }
    }

    // Round starter.
    class RoundStarter extends Thread {
        GameUpdate player;
        int n;
        boolean[] enabledItems;
        String[] avals;
        String[] xvals;
        String[] svals;
        String[] yvals;
        String[] dvals;
        double fee;
        double noiseLevel;

        RoundStarter(GameUpdate player, int n, boolean[] enabledItems,
            String[] avals, String[] xvals, String[] svals, String[] yvals,
            String[] dvals, double fee, double noiseLevel) {
            this.player = player;
            this.n = n;
            this.enabledItems = enabledItems;
            this.avals = avals;
            this.xvals = xvals;
            this.svals = svals;
            this.yvals = yvals;
            this.dvals = dvals;
            this.fee = fee;
            this.noiseLevel = noiseLevel;
        }

        public void run() {
            if (Thread.currentThread() != this) {
                return;
            }

            try {
                player.startRound(n, enabledItems, avals, xvals, svals, yvals,
                    dvals, fee, noiseLevel);
            } catch (Exception e) {
                synchronized (playersMutex) {
                    players.removeElement(player);
                }
            }
        }
    }

    // Round stopper.
    class RoundStopper extends Thread {
        GameUpdate player;

        RoundStopper(GameUpdate player) {
            this.player = player;
        }

        public void run() {
            int i;

            if (Thread.currentThread() != this) {
                return;
            }

            try {
                i = player.stopRound();

                if (i != -1) {
                    synchronized (accumMutex) {
                        choices[i]++;
                    }
                }
            } catch (Exception e) {
                synchronized (playersMutex) {
                    players.removeElement(player);
                }
            }
        }
    }
}
