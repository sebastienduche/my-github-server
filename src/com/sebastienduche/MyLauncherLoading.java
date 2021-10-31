package com.sebastienduche;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

/**
 * <p>Titre : Cave à vin</p>
 * <p>Description : Votre description</p>
 * <p>Copyright : Copyright (c) 2003</p>
 * <p>Société : Seb Informatique</p>
 * @author Sébastien Duché
 * @version 0.3
 * @since 31/10/21
 */
class MyLauncherLoading extends JDialog {

	private static final long serialVersionUID = -2314335792718752038L;
	private JLabel label;
	private final JProgressBar jProgressBar = new JProgressBar();
	private final GridBagLayout gridBagLayout = new GridBagLayout();

	/**
	 * Loading: Constructeur avec texte de la fenêtre.
	 *
	 * @param txt String
	 */
	MyLauncherLoading(String txt) {
		super(new JFrame(), "", false);
		jbInit(txt);
	}

	/**
	 * jbInit: Fonction d'initialisation avec texte de la fenêtre.
	 *
	 * @param txt String
	 */
	private void jbInit(String txt) {

		setSize(270, 75);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setTitle("Loading...");
		setLocation( (screenSize.width - 270) / 2, (screenSize.height - 75) / 2);
		label = new JLabel(txt, SwingConstants.CENTER);
		jProgressBar.setMinimum(0);
		jProgressBar.setMaximum(100);
		getContentPane().setLayout(gridBagLayout);
		getContentPane().add(label, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 20));
		getContentPane().add(jProgressBar, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(1, 0, 1, 0), 270, 4));
		setResizable(false);
	}

	/**
	 * setText: Fonction pour remplacer le texte de la fenêtre.
	 *
	 * @param txt String
	 * @param titre String
	 */
	public void setText(String txt, String titre) {
		label.setText(txt);
		if (titre != null){
			setTitle(titre);
		}
		repaint();
	}

	/**
	 * setValue: Fonction pour affecter une valeur à la barre de progression
	 * (0-100).
	 *
	 * @param i int
	 */
	public void setValue(int i) {
		jProgressBar.setValue(i);
		repaint();
	}

}
