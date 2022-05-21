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
// Utility game player.
package UtilityGame;

import java.applet.Applet;
import java.applet.AudioClip;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.net.*;
import java.net.URL;

import java.rmi.*;
import java.rmi.server.*;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;


public class GamePlayer extends JApplet implements GameUpdate, ActionListener, Runnable {
   // Dimensions.
   static final int GUI_WIDTH  = GameItems.ITEM_PANEL_WIDTH;
   static final int GUI_HEIGHT = (GameItems.ITEM_PANEL_HEIGHT * (int)((double)GameItems.NUM_ITEMS * 1.5)) + 300;

   // Master host IP address.
   String host;

   // Sounds.
   final static String ALERT_SOUND_FILE = "notify.au";
   final static String ERROR_SOUND_FILE = "beep.au";
   AudioClip           alertSound;
   AudioClip           errorSound;

   // Controls.
   TextField id;
   JPanel    gui;
   GameItems items;
   TextField gameNum;
   boolean   gameActive;
   TextField roundNum;
   boolean   roundActive;
   JLabel    statusMessage;
   TextField utility;
   JCheckBox submit;
   boolean[] enabledItems;
   int               choice;
   JButton           choiceReleaseAccept;
   double            choiceReleaseFee;
   boolean           allowChoiceRelease;
   double            noise;
   JButton           connect;
   TextField         masterName;
   GameAction        master;
   JCheckBox         mute;
   TextArea          text;
   JScrollPane       historyScroll;
   HistoryTableModel historyTableModel;
   JTable            historyTable;
   private Thread    blinkerThread;

   // Applet information.
   public String getAppletInfo()
   {
      return("Utility Game Player Copyright (c) 2005 Tom Portegys, All Rights Reserved (portegys@gmail.com) - Version 1.0");
   }


   // Initialize.
   public void init()
   {
      JPanel p;
      JLabel label;

      setSize(GUI_WIDTH, GUI_HEIGHT);

      gui = (JPanel)getContentPane();
      gui.setLayout(new FlowLayout());
      p = new JPanel();
      p.setLayout(new FlowLayout());
      gui.add(p);
      p.add(new JLabel("ID:"));
      id = new TextField();
      id.setEditable(false);
      p.add(id);
      p.add(new JLabel("Game #:"));
      gameNum = new TextField();
      gameNum.setEditable(false);
      p.add(gameNum);
      gameActive = false;
      p.add(new JLabel("Round #:"));
      roundNum = new TextField();
      roundNum.setEditable(false);
      p.add(roundNum);
      statusMessage = new JLabel("Please connect to a game master...");
      p.add(statusMessage);
      items = new GameItems(false,
                            new Dimension(GameItems.ITEM_PANEL_WIDTH,
                                          GameItems.ITEM_PANEL_HEIGHT * GameItems.NUM_ITEMS));
      gui.add(items);
      p = new JPanel();
      p.setLayout(new FlowLayout());
      gui.add(p);
      label = new JLabel("Accumulated utility:");
      label.setToolTipText("Utility accumulated for this game");
      p.add(label);
      utility = new TextField("0.0");
      utility.setEditable(false);
      p.add(utility);
      submit = new JCheckBox("Submit choice");
      submit.setToolTipText("Submit choice to game master");
      submit.addActionListener(this);
      enabledItems = new boolean[GameItems.NUM_ITEMS];

      for (int i = 0; i < GameItems.NUM_ITEMS; i++)
      {
         enabledItems[i] = true;
      }

      p.add(submit);
      choice = -1;
      label  = new JLabel("Choice release fee:");
      label.setToolTipText(
         "Pay this amount of utility to make another choice");
      p.add(label);
      choiceReleaseAccept = new JButton("NA");
      choiceReleaseAccept.setToolTipText("Not available at this time");
      choiceReleaseAccept.addActionListener(this);
      p.add(choiceReleaseAccept);
      choiceReleaseFee   = 0.0;
      allowChoiceRelease = false;
      noise = 0.0;
      p     = new JPanel();
      p.setLayout(new FlowLayout());
      gui.add(p);
      connect = new JButton("Connect:");
      connect.setToolTipText("Connect to game master");
      connect.addActionListener(this);
      master = null;
      p.add(connect);

      if (host == null)
      {
         host = getParameter("MasterHost");
      }
      if (host == null)
      {
         host = "localhost";
      }

      masterName = new TextField(host);
      p.add(masterName);
      mute = new JCheckBox("Mute");
      mute.setToolTipText("Turn off sounds");
      p.add(mute);
      historyTableModel = new HistoryTableModel();
      historyTable      = new JTable(historyTableModel);
      historyTable.setToolTipText("Choice history");
      historyScroll = new JScrollPane(historyTable);
      historyScroll.setPreferredSize(new Dimension(
                                        (int)((double)GameItems.ITEM_PANEL_WIDTH * .9), 100));
      gui.add(historyScroll);
      p = new JPanel();
      p.setLayout(new FlowLayout());
      gui.add(p);
      text = new TextArea(3, 70);
      text.setEditable(false);
      p.add(text);

      // Get sounds.
      URL url = GamePlayer.class .getResource(ALERT_SOUND_FILE);

      if (url != null)
      {
         alertSound = Applet.newAudioClip(url);
      }
      else
      {
         showStatus("Cannot find sound file " + ALERT_SOUND_FILE);
      }

      url = GamePlayer.class .getResource(ERROR_SOUND_FILE);

      if (url != null)
      {
         errorSound = Applet.newAudioClip(url);
      }
      else
      {
         showStatus("Cannot find sound file " + ERROR_SOUND_FILE);
      }

      blinkerThread = null;
   }


   // Start applet.
   public void start()
   {
      // Export this object for RMI.
      try {
         UnicastRemoteObject.exportObject(this);
      }
      catch (Exception e) {
         showStatus("Cannot export GamePlayer object for RMI");
      }

      // Start blinker thread.
      blinkerThread = new Thread(this);
      blinkerThread.start();

      // Start items display.
      items.startDisplay();
   }


   // Stop applet.
   public void stop()
   {
      // Disconnect.
      if (master != null)
      {
         try {
            master.unregister(this);
         }
         catch (Exception e) {
         }

         master = null;
      }

      // Unexport this object for RMI.
      try {
         UnicastRemoteObject.unexportObject(this, true);
      }
      catch (Exception e) {
         showStatus("Cannot unexport GamePlayer object for RMI");
      }

      // Stop blinker thread.
      if (blinkerThread != null)
      {
         blinkerThread.interrupt();

         try {
            blinkerThread.join();
         }
         catch (InterruptedException e) {
         }

         blinkerThread = null;
      }

      // Reset.
      connect.setText("Connect:");
      statusMessage.setText("Please connect to a game master...");
      gameActive  = false;
      roundActive = false;
      gameNum.setText("");
      roundNum.setText("");
      items.resetItems();
      items.stopDisplay();

      for (int i = 0; i < GameItems.NUM_ITEMS; i++)
      {
         enabledItems[i] = true;
         items.items[i].setItemName(null);
      }

      choice = -1;
      submit.setSelected(false);
      choiceReleaseAccept.setText("NA");
      choiceReleaseFee   = 0.0;
      allowChoiceRelease = false;
      noise = 0.0;
      utility.setText("0.0");
      clearHistory();
      text.setText("");
   }


   // Repaint GUI.
   public void paint(Graphics g)
   {
      gui.repaint();
   }


   // Action listener.
   public void actionPerformed(ActionEvent evt)
   {
      int i;

      // Connect/disconnect?
      if (evt.getSource() == connect)
      {
         if (master == null)
         {
            String host = masterName.getText().trim();

            if (host.equals(""))
            {
               return;
            }

            i = -1;

            try {
               String service = "//" + host + ":" + GameMaster.PORT +
                                "/UtilityGame.GameMaster";
               master = (GameAction)Naming.lookup(service);

               if ((i = master.register((GameUpdate) this)) < 0)
               {
                  text.append(
                     "Cannot connect to game in progress; try again later\n");
                  playError();
                  master = null;
               }
            }
            catch (Exception e) {
               text.append("Cannot connect: " + e.toString() + "\n");
            }

            if (master == null)
            {
               return;
            }

            id.setText("" + i);
            connect.setText("Disconnect:");
            statusMessage.setText("Please wait for next game to begin...");
         }
         else
         {
            try {
               master.unregister(this);
            }
            catch (Exception e) {
               text.append("Disconnect failed: " + e.toString() + "\n");
            }

            reset();
         }

         return;
      }

      // Submit?
      if (evt.getSource() == submit)
      {
         if ((master != null) && gameActive && roundActive && submit.isSelected())
         {
            if ((choice = items.getSelectedItem()) != -1)
            {
               for (i = 0; i < GameItems.NUM_ITEMS; i++)
               {
                  items.items[i].enableItem(false);
               }

               return;
            }
         }

         if ((choice == -1) && submit.isSelected())
         {
            playError();
            submit.setSelected(false);

            return;
         }

         if ((choice != -1) && !submit.isSelected())
         {
            playError();
            submit.setSelected(true);

            return;
         }

         return;
      }

      // Accept choice release?
      if (evt.getSource() == choiceReleaseAccept)
      {
         if ((master != null) && gameActive && roundActive &&
             (choice != -1) && submit.isSelected() &&
             allowChoiceRelease)
         {
            double u = Double.parseDouble(utility.getText().trim());

            if (u < choiceReleaseFee)
            {
               playError();
               text.append("Insufficient utility!\n");

               return;
            }

            u -= choiceReleaseFee;
            utility.setText("" + u);
            choice = -1;
            submit.setSelected(false);

            for (i = 0; i < GameItems.NUM_ITEMS; i++)
            {
               items.items[i].enableItem(enabledItems[i]);
            }
         }
         else
         {
            playError();
         }

         return;
      }
   }


   // Blinker thread loop.
   public void run()
   {
      int     i;
      boolean onoff = true;

      if (Thread.currentThread() != blinkerThread)
      {
         return;
      }

      blinkerThread.setPriority(Thread.MIN_PRIORITY);

      while (!blinkerThread.isInterrupted())
      {
         if (roundActive)
         {
            if (onoff)
            {
               submit.setForeground(Color.black);
            }
            else
            {
               submit.setForeground(Color.red);
            }
         }
         else
         {
            submit.setForeground(Color.black);
         }

         onoff = !onoff;

         try {
            Thread.sleep(1000);
         }
         catch (InterruptedException e) {
            break;
         }
      }
   }


   // GameUpdate implementation:
   // Start game.
   public void startGame(int gameNum)
   {
      gameActive  = true;
      roundActive = false;
      this.gameNum.setText("" + gameNum);
      roundNum.setText("0");
      utility.setText("0.0");
      clearHistory();
      items.resetItems();
      items.scrambleItems();
      statusMessage.setText("Game active: waiting for round to start...");
      playAlert();
      text.append("==========\n");
      text.append("Start game " + this.gameNum.getText().trim() + " " +
                  new Date().toString() + "\n");
   }


   // Stop game; return utility.
   public double stopGame(String[] Avals, String[] Xvals, String[] Svals,
                          String[] Yvals, String[] Dvals)
   {
      gameActive = roundActive = false;
      items.updateConstants(Avals, Xvals, Yvals);
      items.updateValues(Svals, Dvals, noise);

      double u = Double.parseDouble(utility.getText().trim());

      updateHistory("End");
      items.resetItems();

      for (int i = 0; i < GameItems.NUM_ITEMS; i++)
      {
         enabledItems[i] = true;
         items.items[i].setItemName(null);
      }

      choice = -1;
      submit.setSelected(false);
      choiceReleaseAccept.setText("NA");
      choiceReleaseFee   = 0.0;
      allowChoiceRelease = false;
      noise = 0.0;
      statusMessage.setText(
         "Game over: please wait for next game to begin...");
      playAlert();
      text.append("----------\n");
      text.append("Stop game " + gameNum.getText().trim() + " " +
                  new Date().toString() + "\n");
      text.append("Accumulated utility=" + u + "\n");
      text.append("==========\n");

      return(u);
   }


   // Start round.
   public void startRound(int roundNum, boolean[] enabledItems,
                          String[] Avals, String[] Xvals, String[] Yvals,
                          boolean allowChoiceRelease, double choiceReleaseFee)
   {
      int i;
      int j;

      roundActive = true;

      int oldRound = Integer.parseInt(this.roundNum.getText().trim());
      this.roundNum.setText("" + roundNum);

      for (i = 0; i < GameItems.NUM_ITEMS; i++)
      {
         this.enabledItems[i] = enabledItems[i];

         if (enabledItems[i])
         {
            items.items[i].setItemName(null);
         }
         else
         {
            items.items[i].setItemName("NA");
         }
         items.items[i].enableItem(enabledItems[i]);
         items.items[i].setItemVisible(enabledItems[i]);
      }

      items.showVisible();
      items.updateConstants(Avals, Xvals, Yvals);

      if (choice != -1)
      {
         double u = Double.parseDouble(utility.getText().trim());

         for (i = 0, j = roundNum - oldRound - 1; i < j; i++)
         {
            u += items.items[choice].getUtility();
         }

         utility.setText("" + u);
      }

      if ((choice != -1) && !enabledItems[choice])
      {
         choice = -1;
      }
      submit.setSelected(false);

      this.choiceReleaseFee   = choiceReleaseFee;
      this.allowChoiceRelease = allowChoiceRelease;

      if (!allowChoiceRelease)
      {
         choiceReleaseAccept.setText("NA");
         choiceReleaseAccept.setToolTipText("Not available at this time");
      }
      else
      {
         choiceReleaseAccept.setText("" + choiceReleaseFee);
         choiceReleaseAccept.setToolTipText(
            "Pay this amount of utility to make another choice");
      }

      statusMessage.setText("Round active: choose and submit an item...");
      playAlert();
      text.append("----------\n");
      text.append("Start round=" + roundNum + "\n");
      text.append("Choice=");

      if (choice == -1)
      {
         text.append("None\n");
      }
      else
      {
         text.append("" + choice + "\n");
      }

      text.append("Accumulated utility=" + utility.getText().trim() + "\n");

      if (items.items[0].Ytext.getText().trim().equals("NA"))
      {
         text.append("Choice\t\tA\tX\tS\tU\n");

         for (i = 0; i < GameItems.NUM_ITEMS; i++)
         {
            if (enabledItems[i])
            {
               text.append("" + i + "\t");
               text.append("\t" + items.items[i].Atext.getText().trim() +
                           "\t" + items.items[i].Xtext.getText().trim() + "\t" +
                           items.items[i].Stext.getText().trim() + "\t" +
                           items.items[i].Utext.getText().trim() + "\n");
            }
         }
      }
      else
      {
         text.append("Choice\t\tA\tX\tS\tY\tD\tU\n");

         for (i = 0; i < GameItems.NUM_ITEMS; i++)
         {
            if (enabledItems[i])
            {
               text.append("" + i + "\t");
               text.append("\t" + items.items[i].Atext.getText().trim() +
                           "\t" + items.items[i].Xtext.getText().trim() + "\t" +
                           items.items[i].Stext.getText().trim() + "\t" +
                           items.items[i].Ytext.getText().trim() + "\t" +
                           items.items[i].Dtext.getText().trim() + "\t" +
                           items.items[i].Utext.getText().trim() + "\n");
            }
         }
      }

      for (i = 0, j = roundNum - oldRound - 1; i < j; i++)
      {
         updateHistory("" + (roundNum - (j - (i + 1))));
      }
   }


   // Stop round; return player item choice.
   public int stopRound()
   {
      roundActive = false;
      statusMessage.setText("Round over: waiting for next round to start...");
      playAlert();

      return(choice);
   }


   // Sum round; let players see others' choices.
   public void sumRound(String[] Svals, String[] Dvals, double noise)
   {
      int i;
      int j;

      this.noise = noise;
      items.updateValues(Svals, Dvals, noise);

      if (choice != -1)
      {
         double u = Double.parseDouble(utility.getText().trim());

         u += items.items[choice].getUtility();
         utility.setText("" + u);
      }

      int round = Integer.parseInt(roundNum.getText().trim());
      text.append("----------\n");
      text.append("Stop round=" + round + "\n");
      text.append("Choice=");

      if (choice == -1)
      {
         text.append("None\n");
      }
      else
      {
         text.append("" + choice + "\n");
      }

      text.append("Accumulated utility=" + utility.getText().trim() + "\n");

      if (items.items[0].Ytext.getText().trim().equals("NA"))
      {
         text.append("Choice\t\tA\tX\tS\tU\n");

         for (i = 0; i < GameItems.NUM_ITEMS; i++)
         {
            if (enabledItems[i])
            {
               text.append("" + i + "\t");
               text.append("\t" + items.items[i].Atext.getText().trim() +
                           "\t" + items.items[i].Xtext.getText().trim() + "\t" +
                           items.items[i].Stext.getText().trim() + "\t" +
                           items.items[i].Utext.getText().trim() + "\n");
            }
         }
      }
      else
      {
         text.append("Choice\t\tA\tX\tS\tY\tD\tU\n");

         for (i = 0; i < GameItems.NUM_ITEMS; i++)
         {
            if (enabledItems[i])
            {
               text.append("" + i + "\t");
               text.append("\t" + items.items[i].Atext.getText().trim() +
                           "\t" + items.items[i].Xtext.getText().trim() + "\t" +
                           items.items[i].Stext.getText().trim() + "\t" +
                           items.items[i].Ytext.getText().trim() + "\t" +
                           items.items[i].Dtext.getText().trim() + "\t" +
                           items.items[i].Utext.getText().trim() + "\n");
            }
         }
      }

      updateHistory("" + round);
   }


   // Get player id.
   public int getID()
   {
      return(Integer.parseInt(id.getText().trim()));
   }


   // Game notifcation.
   public void gameNotification(String note)
   {
      text.append(note + "\n");
   }


   // Drop master connection.
   public void dropConnection()
   {
      text.append("Game master connection dropped!\n");
      reset();
   }


   // Reset.
   void reset()
   {
      items.resetItems();
      gameActive  = false;
      roundActive = false;
      choice      = -1;
      master      = null;
      connect.setText("Connect:");
      statusMessage.setForeground(Color.black);
      statusMessage.setText("Please connect to a game master...");
   }


   // Play alert sound.
   void playAlert()
   {
      if ((alertSound != null) && !mute.isSelected())
      {
         alertSound.play();
      }
   }


   // Play error sound.
   void playError()
   {
      if ((errorSound != null) && !mute.isSelected())
      {
         errorSound.play();
      }
   }


   // Clear history.
   void clearHistory()
   {
      Object[][] rowData        = new Object[0][GameItems.NUM_ITEMS + 1];
      historyTableModel.rowData = rowData;
      historyTableModel.fireTableDataChanged();
   }


   // Update history.
   void updateHistory(String round)
   {
      int i;
      int j;

      Object[][] rowData = new Object[historyTableModel.getRowCount() + 1][GameItems.NUM_ITEMS +
                                                                           1];

      for (i = 0; i < historyTableModel.getRowCount(); i++)
      {
         for (j = 0; j <= GameItems.NUM_ITEMS; j++)
         {
            rowData[i][j] = historyTableModel.rowData[i][j];
         }
      }

      rowData[i][0] = round;

      for (j = 0; j < GameItems.NUM_ITEMS; j++)
      {
         rowData[i][j + 1] = items.items[j].Stext.getText().trim();
      }

      historyTableModel.rowData = rowData;
      historyTableModel.fireTableDataChanged();
   }


   // History table model.
   class HistoryTableModel extends AbstractTableModel {
      // Column names.
      private String[] columnNames;

      // Row data.
      Object[][] rowData = new Object[0][GameItems.NUM_ITEMS + 1];

      // Constructor.
      HistoryTableModel()
      {
         columnNames    = new String[GameItems.NUM_ITEMS + 1];
         columnNames[0] = "Round";

         for (int i = 0; i < GameItems.NUM_ITEMS; i++)
         {
            columnNames[i + 1] = "Choice " + i;
         }
      }


      public int getColumnCount()
      {
         return(columnNames.length);
      }


      public int getRowCount()
      {
         return(rowData.length);
      }


      public String getColumnName(int col)
      {
         return(columnNames[col]);
      }


      public Object getValueAt(int row, int col)
      {
         return(rowData[row][col]);
      }
   }

   // Main.
   @SuppressWarnings("deprecation")
   public static void main(String[] args)
   {
      // Create player.
      GamePlayer player = new GamePlayer();

      // Set master host IP address.
      if ((args.length == 0) || (args.length == 1))
      {
         player.host = "localhost";
      }
      else if (args.length == 2)
      {
         if (!args[0].equals("MasterHost"))
         {
            System.err.println("Usage: java UtilityGame.GamePlayer [-MasterHost <master IP address>]");
            System.exit(1);
         }
         player.host = args[1];
      }
      else if (args.length == 3)
      {
         if (!args[1].equals("MasterHost"))
         {
            System.err.println("Usage: java UtilityGame.GamePlayer [-MasterHost <master IP address>]");
            System.exit(1);
         }
         player.host = args[2];
      }
      else
      {
         System.err.println("Usage: java UtilityGame.GamePlayer [-MasterHost <master IP address>]");
         System.exit(1);
      }

      // Create frame.
      JFrame frame = new JFrame();
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setTitle("Utility Game Player");
      frame.setBounds(0, 0, 550, 649);
      frame.setLayout(new GridLayout(1, 1));
      frame.add(player);
      frame.setVisible(true);

      // Run applet.
      player.init();
      player.start();
      frame.resize(550, 650);
   }
}
