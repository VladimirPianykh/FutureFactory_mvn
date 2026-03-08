package com.bpa4j.defaults.features;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import com.bpa4j.core.Data;
import com.bpa4j.core.User.Feature;
import com.bpa4j.navigation.ImplementedInfo;

/**
 * A Board-like feature displaying a list of commands (clickable links).
 * @author AI-generated
 */
public final class CmdList implements Feature {
	
	static {
		if(!Data.getInstance().ftrInstances.containsKey(CmdList.class.getName())) {
			Data.getInstance().ftrInstances.put(CmdList.class.getName(), new HashMap<>());
		}
	}
	
	private final String name;
	private final ArrayList<Command> commands = new ArrayList<>();
	
	private CmdList(String name) {
		this.name = name;
	}
	
	@SuppressWarnings("unchecked")
	public static CmdList getList(String name) {
		if(((HashMap<String, CmdList>) Data.getInstance().ftrInstances.get(CmdList.class.getName())).containsKey(name)) {
			return ((HashMap<String, CmdList>) Data.getInstance().ftrInstances.get(CmdList.class.getName())).get(name);
		} else {
			throw new IllegalArgumentException("CmdList \"" + name + "\" does not exist.");
		}
	}
	
	@SuppressWarnings("unchecked")
	public static CmdList registerList(String name) {
		if(((HashMap<String, CmdList>) Data.getInstance().ftrInstances.get(CmdList.class.getName())).containsKey(name)) {
			return ((HashMap<String, CmdList>) Data.getInstance().ftrInstances.get(CmdList.class.getName())).get(name);
		}
		CmdList b = new CmdList(name);
		((HashMap<String, CmdList>) Data.getInstance().ftrInstances.get(CmdList.class.getName())).put(name, b);
		return b;
	}
	
	public CmdList addCommand(String cmdName, Runnable action) {
		commands.add(new Command(cmdName, action));
		return this;
	}
	
	@Override
	public void fillTab(JPanel content, JPanel tab, Font font) {
		tab.setLayout(new BorderLayout());
		tab.setBorder(BorderFactory.createEmptyBorder(tab.getHeight() / 300, tab.getWidth() / 300, tab.getHeight() / 300, tab.getWidth() / 300));
		
		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		listPanel.setBackground(Color.DARK_GRAY);
		
		Font linkFont = new Font(Font.DIALOG, Font.ITALIC, Math.max(16, tab.getHeight() / 30));
		
		listPanel.add(Box.createVerticalStrut(10));
		for (Command cmd : commands) {
			JButton btn = new JButton(cmd.name);
			btn.setFont(linkFont);
			btn.setForeground(Color.WHITE);
			btn.setHorizontalAlignment(SwingConstants.LEFT);
			btn.setAlignmentX(Component.LEFT_ALIGNMENT);
			
			// Make button look like a link
			btn.setOpaque(false);
			btn.setContentAreaFilled(false);
			btn.setBorderPainted(false);
			btn.setFocusPainted(false);
			btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
			
			btn.addActionListener(e -> cmd.action.run());
			
			btn.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					btn.setText("<html><u>" + cmd.name + "</u></html>");
				}

				@Override
				public void mouseExited(MouseEvent e) {
					btn.setText(cmd.name);
				}
			});
			
			listPanel.add(btn);
			listPanel.add(Box.createVerticalStrut(10));
		}
		
		JScrollPane scrollPane = new JScrollPane(listPanel);
		scrollPane.setBorder(BorderFactory.createTitledBorder(null, "Команды", 0, 0, new Font(Font.DIALOG, Font.PLAIN, Math.max(12, tab.getHeight() / 50)), Color.WHITE));
		scrollPane.getViewport().setBackground(Color.DARK_GRAY);
		
		tab.add(scrollPane, BorderLayout.CENTER);
	}
	
	@Override
	public void paint(Graphics2D g2, BufferedImage image, int s) {
		g2.setStroke(new BasicStroke(Math.max(1, s / 40)));
		g2.drawLine(s / 6, s / 4, s * 5 / 6, s / 4);
		g2.drawLine(s / 6, s / 2, s * 2 / 3, s / 2);
		g2.drawLine(s / 6, s * 3 / 4, s * 5 / 6, s * 3 / 4);
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public List<ImplementedInfo> getImplementedInfo() {
		return List.of();
	}
	
	private static class Command {
		public final String name;
		public final Runnable action;
		public Command(String name, Runnable action) {
			this.name = name;
			this.action = action;
		}
	}
}
