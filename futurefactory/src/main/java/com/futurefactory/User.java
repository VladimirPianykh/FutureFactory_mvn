package com.futurefactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.futurefactory.Data.Editable;
import com.futurefactory.WorkFrame.WorkTabButton;

public class User implements Serializable{
	public static class Authorization implements Serializable{
		public LocalDateTime inTime,outTime;
		public int tries;
		public String login;
		public InetAddress ip;
		public Authorization(User u){
			inTime=LocalDateTime.now();
			tries=u.tries;
			login=u.login;
			try{
				ip=InetAddress.getLocalHost();
			}catch(UnknownHostException ex){}
		}
		public void writeLogout(){outTime=LocalDateTime.now();}
	}
	public static interface Role{}
	public static enum DefaultRole implements Role{
		EMPTY,
		ADMIN
	}
	public static interface Feature{
		public void paint(Graphics2D g2,BufferedImage image,int h);
		public void fillTab(JPanel content,JPanel tab,Font font);
	}
	public static enum DefaultFeature implements Feature{
		HISTORY{
			public void paint(Graphics2D g2,BufferedImage image,int h){
				Area p=new Area(new Arc2D.Double(h/10,h/10,h*4/5,h*4/5,-90,270,Arc2D.PIE));
				p.subtract(new Area(new Ellipse2D.Double(h/5,h/5,h*3/5,h*3/5)));
				p.add(new Area(new Polygon(new int[]{h/10-h/30,h/5+h/30,h*3/20},new int[]{h/2,h/2,h/2+h/10},3)));
				g2.fillPolygon(new int[]{h/2-h/100,h/2+h/100,h/2+h/30,h*3/4,h*3/4,h/2-h/30},new int[]{h/4,h/4,h/2-h/30,h*2/3-h/50,h*2/3,h/2+h/30},6);
				g2.fill(p);
			}
			public void fillTab(JPanel content,JPanel tab,Font font){
				JPanel p=WorkTabButton.createTable(User.getActiveUser().history.size()+1,4,tab);
				JLabel[]data0={new JLabel("login"),new JLabel("login time"),new JLabel("logout time"),new JLabel("IP")};
				for(JLabel l:data0){
					l.setFont(font);
					l.setForeground(Color.BLACK);
					p.add(l);
				}
				User.getActiveUser().history.descendingIterator().forEachRemaining(a->{
					JLabel[]data={new JLabel(a.login),new JLabel(a.inTime.toString()),new JLabel(a.outTime==null?"---":a.outTime.toString()),new JLabel(a.ip.toString())};
					for(JLabel l:data){
						l.setFont(font);
						l.setToolTipText(l.getText());
						p.add(l);
					}
				});
			}
		},
		ROLE_SETTING{
			public void paint(Graphics2D g2,BufferedImage image,int h){
				g2.setStroke(new BasicStroke(h/15,2,0));
				g2.drawOval(h/3,h/3-h/6,h/3,h/3);
				g2.drawPolyline(new int[]{h/4,h/2-h/10,h/2+h/10,h*3/4},new int[]{h,h/2+h/15,h/2+h/15,h},4);
			}
			public void fillTab(JPanel content,JPanel tab,Font font){
				JPanel p=WorkTabButton.createTable(User.getUserCount(),5,tab);
				Role[]roles=registeredRoles.toArray(new Role[0]);
				User.forEachUser(u->{
					JLabel name=new JLabel(u.login);
					name.setFont(font);
					JComboBox<Role>b=new JComboBox<Role>(roles);
					b.setSelectedItem(u.role);
					name.addMouseListener(new MouseAdapter(){
						public void mouseClicked(MouseEvent e){
							if(SwingUtilities.isRightMouseButton(e)){
								User.deleteUser(u.login);
								name.setText("<deleted>");
								name.setEnabled(false);
								b.removeAllItems();
								User.save();
							}
						}
					});
					b.setAction(new AbstractAction(){
						public void actionPerformed(ActionEvent e){
							u.role=(Role)b.getSelectedItem();
							User.save();
						}
					});
					p.add(name);
					p.add(b);
				});
			}
		},
		MODEL_EDITING{
			public void paint(Graphics2D g2,BufferedImage image,int h){
				//g2.drawPolygon(new int[]{h/4,h/2-h/10,h/2+h/10,h*3/4},new int[]{h,h/2+h/15,h/2+h/15,h},4);
				g2.setStroke(new BasicStroke(h/40));
				g2.drawPolyline(new int[]{h/4,h/4+h/20,h*3/4+h/20,h*3/4+h/20+h/10,h*3/4+h/5,h*3/4+h/5,h*3/4+h/5-h/40,h*3/4+h/5-h/40,h*3/4+h/20+h/10,h*3/4+h/40},new int[]{h*3/4,h*3/4,h/4,h/4,h/4-h/20,h/4-h/20-h/10,h/4-h/20-h/10,h/4-h/10,h/4-h/20,h/4-h/20},10);
				for(int x=0;x<h;x++)for(int y=0;y<h-x;y++)
				image.setRGB(x,y,image.getRGB(h-y-1,h-x-1));
			}
			public void fillTab(JPanel content,JPanel tab,Font font){
				Data d=Data.getInstance();
				JPanel p=WorkTabButton.createTable(d.editables.size()+(User.getActiveUser().hasPermission(DefaultPermission.CREATE)?1:0),1,tab);
				int s=content.getHeight()/7;
				BufferedImage editImage=new BufferedImage(content.getHeight()/7,content.getHeight()/7,6),
				addImage=new BufferedImage(content.getHeight()/7,content.getHeight()/7,6);
				Graphics2D g2=addImage.createGraphics();
				g2.setPaint(new RadialGradientPaint(s/2,s/2,s/2,new float[]{0,1},new Color[]{new Color(94,86,82),new Color(66,60,57)}));
				g2.fillRect(0,0,s,s);
				g2.setStroke(new BasicStroke(s/40,2,2));
				g2.setPaint(new RadialGradientPaint(s/2,s/2,s/2,new float[]{0,1},new Color[]{new Color(110,128,40),new Color(81,92,36)}));
				g2.drawRect(s/3,s/10,s/3,s*4/5);
				g2.drawRect(s/10,s/3,s*4/5,s/3);
				Color c1=new Color(42,46,30),c2=new Color(32,36,21);
				for(Editable r:Data.getInstance().editables){
					HButton b=new HButton(){
						public void paintComponent(Graphics g){
							Graphics2D g2=(Graphics2D)g;
							g2.setColor(Color.GRAY);
							g2.fillRect(0,0,getWidth(),getHeight());
							g2.setFont(font);
							FontMetrics fm=g2.getFontMetrics();
							g2.setPaint(new GradientPaint(0,0,c1,0,getHeight(),c2));
							g2.drawString(r.name,getWidth()/100,(getHeight()+fm.getAscent()+fm.getLeading()-fm.getDescent())/2);
							g2.drawImage(editImage,getWidth()-editImage.getWidth(),0,this);
							g2.setColor(new Color(0,0,0,scale*10));
							g2.fillRect(0,0,getWidth(),getHeight());
							if(true){//FILLIN
								g2.setPaint(new LinearGradientPaint(getWidth()-getHeight()/2,getHeight()/4,getWidth(),getHeight()*3/4,new float[]{0,0.5f},new Color[]{new Color(89+scale*5,87,63),new Color(122+scale*5,119,80)},CycleMethod.REFLECT));
								g2.fillOval(getWidth()-getHeight()*9/8,getHeight()/8+1,getHeight()*3/4-1,getHeight()*3/4-1);
								g2.setPaint(new LinearGradientPaint(getWidth()-getHeight()/2,getHeight()/4,getWidth(),getHeight()*3/4,new float[]{0,0.5f},new Color[]{new Color(135+scale*5,52,14),new Color(194+scale*5,47,17)},CycleMethod.REFLECT));
								g2.fillOval(getWidth()-getHeight(),getHeight()/4,getHeight()/2,getHeight()/2);
								g2.setPaint(new LinearGradientPaint(getWidth()-getHeight()/2,getHeight()/4,getWidth(),getHeight()*3/4,new float[]{0,0.5f},new Color[]{new Color(54,51,36),new Color(77,74,56)},CycleMethod.REFLECT));
								g2.drawOval(getWidth()-getHeight(),getHeight()/4,getHeight()/2,getHeight()/2);
								g2.drawOval(getWidth()-getHeight()*9/8,getHeight()/8+1,getHeight()*3/4-1,getHeight()*3/4-1);
							}
						}
					};
					b.setAction(new AbstractAction(){
						public void actionPerformed(ActionEvent e){
							if(ProgramStarter.editor==null)throw new RuntimeException("Editor ha not been set.");
							ProgramStarter.editor.constructEditor(r);
						}
					});
					p.add(b);
				};
				if(User.getActiveUser().hasPermission(DefaultPermission.CREATE)){
					HButton add=new HButton(){
					public void paintComponent(Graphics g){
						g.setColor(Color.GRAY);
						g.fillRect(0,0,getWidth(),getHeight());
						g.drawImage(addImage,(getWidth()-addImage.getWidth())/2,0,this);
						g.setColor(new Color(0,0,0,scale*10));
						g.fillRect(0,0,getWidth(),getHeight());
					}};
					add.setAction(new AbstractAction(){
						public void actionPerformed(ActionEvent e){
							if(ProgramStarter.editor==null)throw new RuntimeException("Editor ha not been set.");
							ProgramStarter.editor.constructEditor(null);
							SwingUtilities.getWindowAncestor(content).dispose();
							ProgramStarter.frame=new WorkFrame(User.getActiveUser());
						}
					});
					p.add(add);
				}
			}
		},
		/*REPORT{
			public void paint(Graphics2D g2,BufferedImage image,int h){
				g2.setStroke(new BasicStroke(h/15,2,0));
				g2.drawPolygon(new int[]{h/3,h*2/3-h/20,h*2/3,h*2/3,h/3},new int[]{h/5,h/5,h/5+h/20,h*4/5,h*4/5},5);
				for(int i=0;i<4;i++)g2.drawLine(h*4/9,h*(3+i)/10,h*5/9,h*(3+i)/10);
			}
			public void fillTab(JPanel content,JPanel tab,Font font){
				HButton b=new HButton(){
					public void paint(Graphics g){
						Graphics2D g2=(Graphics2D)g;
						g2.setPaint(new LinearGradientPaint(getWidth()/2,0,getWidth(),getHeight(),new float[]{0.4f,0.41f,0.5f,0.51f},new Color[]{Color.GRAY,Color.LIGHT_GRAY,Color.LIGHT_GRAY,Color.GRAY}));
						g2.fillRect(0,0,getWidth(),getHeight());
						g2.setStroke(new BasicStroke(scale));
						g2.setColor(Color.DARK_GRAY);
						g2.drawRect(0,0,getWidth(),getHeight());
					}
				};
				b.setAction(new AbstractAction(){
				public void actionPerformed(ActionEvent e){try{
					File file=new File("C:/Users/user/Downloads/report.xls");
					if(file.exists())file.delete();
					FileOutputStream fOS=new FileOutputStream("C:/Users/user/Downloads/report.xls");
					HSSFWorkbook w=new HSSFWorkbook();
					Sheet sheet=w.createSheet("price-list");
					Collection<Editable>models=Data.getInstance().editables;
					Row r0=sheet.createRow(0);
					r0.createCell(0).setCellValue("№");
					r0.createCell(1).setCellValue("Товар");
					r0.createCell(2).setCellValue("Цена");
					int i=1;
					for(Editable model:models){
						if(model.status==Status.PRODUCED){
							Row r=sheet.createRow(i);
							r.createCell(0).setCellValue(i);
							r.createCell(1).setCellValue(model.name);
							r.createCell(2).setCellValue(model.price+" RUB");
							i++;
						}
					}
					w.write(fOS);
					System.exit(0);
				}catch(IOException ex){ex.printStackTrace();}}});
				b.setBounds(content.getWidth()/4,content.getHeight()/2-content.getHeight()/6,content.getWidth()/2,content.getHeight()/3);
				tab.add(b);
			}
		}*/
	}
	public static interface Permission{}
	public static enum DefaultPermission implements Permission{
		CREATE,
	}
	private static HashMap<String,User>userMap;
	public static ArrayList<Role>registeredRoles=new ArrayList<Role>();
	public static ArrayList<Feature>registeredFeatures=new ArrayList<Feature>();
	public static HashMap<Role,Permission[]>permissions=new HashMap<Role,Permission[]>();
	static{
		for(Role r:DefaultRole.values())registeredRoles.add(r);
		for(Feature f:DefaultFeature.values())registeredFeatures.add(f);
		permissions.put(DefaultRole.EMPTY,new Permission[]{});
		permissions.put(DefaultRole.ADMIN,registeredRoles.toArray(new Permission[0]));
	}
	private static User user;
	public String login,password;
	public Role role;
	public int passTries,tries;
	public LocalDateTime lockTime=null;
	public ArrayDeque<Authorization>history=new ArrayDeque<Authorization>();
	@SuppressWarnings("unchecked")
	private static void load(){
		try{
			FileInputStream fIS=new FileInputStream(Root.folder+"Users.ser");
			ObjectInputStream oIS=new ObjectInputStream(fIS);
			userMap=(HashMap<String,User>)oIS.readObject();
			oIS.close();fIS.close();
		}catch(IOException ex){
			userMap=new HashMap<String,User>();
		}catch(ClassNotFoundException ex){throw new RuntimeException("FATAL ERROR: Users corrupted");}
		for(User u:userMap.values())if(u.lockTime!=null){
			if(u.lockTime.until(LocalDateTime.now(),ChronoUnit.MINUTES)<5)System.exit(1);
			else u.lockTime=null;
		}
	}
	public static User register(String login,String pass){
		if(userMap==null)load();
		user=new User(login,pass,userMap.isEmpty()?DefaultRole.ADMIN:DefaultRole.EMPTY);
		userMap.put(login,user);
		save();
		return user;
	}
	public static User getActiveUser(){
		if(user==null)throw new RuntimeException("No user registered!");
		return user;
	}
	public static int getUserCount(){
		if(userMap==null)load();
		return userMap.size();
	}
	public static User getUser(String s){
		if(userMap==null)load();
		return userMap.get(s);
	}
	public static boolean hasUser(String s){
		if(userMap==null)load();
		return userMap.containsKey(s);
	}
	public static void deleteUser(String login){userMap.remove(login);}
	public static void forEachUser(Consumer<User>c){
		if(userMap==null)load();
		for(User u:userMap.values())c.accept(u);
	}
	public static void save(){
		try{
			FileOutputStream fOS=new FileOutputStream(Root.folder+"Users.ser");
			ObjectOutputStream oOS=new ObjectOutputStream(fOS);
			oOS.writeObject(User.userMap);
			oOS.close();fOS.close();
		}catch(IOException ex){ex.printStackTrace();}
	}
	private User(String login,String password,Role role){
		this.login=login;this.password=password;this.role=role;
		userMap.put(login,this);
	}
	public boolean hasPermission(Permission p){
		for(Permission m:permissions.get(role))if(m==p)return true;
		return false;
	}
	public boolean login(String password){
		if(password.equals(this.password)){
			lockTime=null;
			user=this;
			history.addFirst(new Authorization(this));
			passTries=0;tries=0;
			ProgramStarter.frame=new WorkFrame(this);
			new Message("Logged in succesfully!");
			Runtime.getRuntime().addShutdownHook(new Thread(){
				public void run(){logout();}
			});
			return true;
		}
		else if(passTries>1){
			passTries=2;
			lockTime=LocalDateTime.now();
			save();
			System.exit(1);
			return false;
		}else{
			passTries++;tries++;
			save();
			new Message("Incorrect password.");
			return false;
		}
	}
	public void logout(){
		history.getFirst().writeLogout();
		save();
	}
}