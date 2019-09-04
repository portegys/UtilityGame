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
// Master updates to player.
package UtilityGame;

import java.rmi.*;


public interface GameUpdate extends java.rmi.Remote {
    // Start game.
    void startGame(int gameNum) throws RemoteException;

    // Stop game; return utility.
    double stopGame(String[] Avals, String[] Xvals, String[] Svals,
        String[] Yvals, String[] Dvals) throws RemoteException;

    // Start round.
    void startRound(int roundNum, boolean[] enabledItems, String[] Avals,
        String[] Xvals, String[] Yvals, boolean allowChoiceRelease,
        double choiceReleaseFee) throws RemoteException;

    // Stop round; return player item choice.
    int stopRound() throws RemoteException;

    // Sum round; let players see others' choices.
    void sumRound(String[] Svals, String[] Dvals, double noise)
        throws RemoteException;

    // Get player id.
    int getID() throws RemoteException;

    // Supply game notification.
    void gameNotification(String note) throws RemoteException;

    // Drop master connection.
    void dropConnection() throws RemoteException;
}
