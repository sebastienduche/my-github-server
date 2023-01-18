package com.sebastienduche;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;

/**
 * Title : My-GitHub-Server
 * Description : Votre description
 * Copyright : Copyright (c) 2022
 * Société : Seb Informatique
 *
 * @author Sébastien Duché
 */
class MyLauncherLoading extends JDialog {

    private static final long serialVersionUID = -2314335792718752038L;
    private final JLabel label;
    private final JProgressBar jProgressBar = new JProgressBar();

    /**
     * Initialise a small window with a line of text and the title "Loading..."
     *
     * @param txt String
     */
    MyLauncherLoading(String txt) {
        super(new JFrame(), "", false);
        setSize(270, 75);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setTitle("Loading...");
        setLocation((screenSize.width - 270) / 2, (screenSize.height - 75) / 2);
        label = new JLabel(txt, SwingConstants.CENTER);
        jProgressBar.setMinimum(0);
        jProgressBar.setMaximum(100);
        GridBagLayout gridBagLayout = new GridBagLayout();
        getContentPane().setLayout(gridBagLayout);
        getContentPane().add(label, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 20));
        getContentPane().add(jProgressBar, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 0, 1, 0), 270, 4));
        setResizable(false);
    }

    /**
     * Allow to change text and title when the dialog is visible
     *
     * @param text  String
     * @param title String: can be null
     */
    public void setText(String text, String title) {
        label.setText(text);
        if (title != null) {
            setTitle(title);
        }
        repaint();
    }

    /**
     * Set the progress bar value between 0 and 100
     *
     * @param i int
     */
    public void setValue(int i) {
        jProgressBar.setValue(i);
        repaint();
    }

}
