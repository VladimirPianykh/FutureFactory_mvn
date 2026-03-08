package com.bpa4j.editor.modules;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JDialog;
import javax.swing.JPanel;

import com.bpa4j.core.User;
import com.bpa4j.core.Data.Editable;
import com.bpa4j.defaults.editables.Processable;
import com.bpa4j.ui.HButton;

public class StageMapModule implements EditorModule{
	public JPanel createTab(JDialog editor,Editable editable,boolean isNew,Runnable deleter){
		Processable p=(Processable)editable;
		JPanel tab=new JPanel(new FlowLayout(FlowLayout.CENTER,0,editor.getWidth()/30)){
			@Override
			protected void paintComponent(Graphics g){
				super.paintComponent(g);
				g.setColor(Color.WHITE);
				g.drawLine(0,getHeight()/2,getWidth(),getHeight()/2);
			}
		};
		int h=Math.min(editor.getWidth()/3,editor.getWidth()*9/(10*p.stages.length));
		Dimension size=new Dimension(h,h);
		for(int i=0;i<p.stages.length;++i){
			final int ii=i;
			boolean canPress=User.getActiveUser().hasPermission(p.getStage().approver)&&!p.isLastStage();
			HButton b=new HButton(10,5){
				public void paint(Graphics g){
					g.setClip(new Ellipse2D.Double(0,0,getWidth(),getHeight()));
					g.setColor(ii==p.getStageIndex()?(canPress?Color.CYAN:new Color(0,100,100)):ii<p.getStageIndex()?(canPress?Color.GREEN:new Color(0,100,0)):Color.LIGHT_GRAY);
					g.fillOval(0,0,getWidth(),getHeight());
					((Graphics2D)g).setStroke(new BasicStroke(getHeight()/30));
					g.setColor(Color.DARK_GRAY);
					g.drawOval(0,0,getWidth(),getHeight());
					if(canPress&&ii==p.getStageIndex()){
						int a=scale*8;
						if(getModel().isPressed())a+=50;
						g.setColor(new Color(0,0,0,a));
						g.fillOval(0,0,getWidth(),getHeight());
					}
					FontMetrics fm=g.getFontMetrics();
					g.setColor(Color.BLACK);
					g.drawString(p.stages[ii].name,(getWidth()-fm.stringWidth(p.stages[ii].name))/2,(getHeight()+fm.getAscent()+fm.getLeading()-fm.getDescent())/2);
				}
			};
			if(canPress&&i==p.getStageIndex()){
				b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				b.addActionListener(e->{
					if(p.approve(""))editor.dispose();
				});
			}
			b.setOpaque(false);
			b.setPreferredSize(size);
			tab.add(b);
		}
		HButton c=new HButton(15,5){
			public void paint(Graphics g){
				g.setClip(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),getHeight(),getHeight()));
				g.setColor(new Color(71+scale*5,16+scale*2,1+scale));
				g.fillRect(0,0,getWidth(),getHeight());
				g.setColor(Color.WHITE);
				FontMetrics fm=g.getFontMetrics();
				g.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()+fm.getLeading()-fm.getDescent())/2);
			}
		};
		c.addActionListener(e->editor.dispose());
		c.setSize(editor.getWidth()/5,editor.getHeight()/20);
		c.setText("Отмена");
		c.setOpaque(false);
		c.setFont(new Font(Font.DIALOG,Font.PLAIN,c.getHeight()*2/3));
		tab.add(c);
		return tab;
	}
}