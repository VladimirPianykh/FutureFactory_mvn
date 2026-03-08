package com.bpa4j.editor.modules;

import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JFormattedTextField.AbstractFormatterFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.DefaultFormatter;

import com.bpa4j.Wrapper;
import com.bpa4j.core.Data;
import com.bpa4j.core.Data.Editable;
import com.bpa4j.core.Data.EditableGroup;
import com.bpa4j.core.EditableDemo;
import com.bpa4j.core.ProgramStarter;
import com.bpa4j.defaults.input.EmptySaver;
import com.bpa4j.editor.Completer;
import com.bpa4j.editor.EditorEntry;
import com.bpa4j.editor.EditorEntryBase;
import com.bpa4j.editor.InfoProvider;
import com.bpa4j.editor.Input;
import com.bpa4j.editor.NameProvider;
import com.bpa4j.editor.Verifier;
import com.bpa4j.ui.HButton;
import com.bpa4j.ui.Message;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DateTimePicker;
import com.github.lgooddatepicker.components.TimePicker;

public class FormModule implements EditorModule{
	public JPanel createTab(JDialog editor,Editable editable,boolean isNew,Runnable deleter){
		JPanel tab=new JPanel(null);
		tab.setBackground(Color.BLACK);
		JButton ok=new JButton(){
			public void paint(Graphics g){
				g.setClip(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),getHeight(),getHeight()));
				g.setColor(getModel().isPressed()?Color.DARK_GRAY:getBackground());
				g.fillRect(0,0,getWidth(),getHeight());
				g.setColor(Color.BLACK);
				FontMetrics fm=g.getFontMetrics();
				g.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()+fm.getLeading()-fm.getDescent())/2);
				if(getModel().isRollover()){
					g.setColor(new Color(255,255,255,200));
					((Graphics2D)g).setStroke(new BasicStroke(getHeight()/10));
					g.drawRoundRect(0,0,getWidth(),getHeight(),getHeight(),getHeight());
				}
			}
		};
		ok.setBounds(editor.getWidth()*2/5,editor.getHeight()*9/10,editor.getWidth()/5,editor.getHeight()/20);
		ok.setOpaque(false);
		ok.setFont(new Font(Font.DIALOG,Font.PLAIN,ok.getHeight()));
		JTextField nameField=new JTextField(editable.name);
		nameField.setBounds(editor.getWidth()/5,editor.getHeight()/100,editor.getWidth()*3/5,editor.getHeight()/10);
		nameField.setFont(new Font(Font.DIALOG,Font.PLAIN,nameField.getHeight()*2/3));
		nameField.setBackground(Color.DARK_GRAY);
		nameField.setForeground(Color.LIGHT_GRAY);
		Wrapper<Verifier>verifier=new Wrapper<>(null);
		Wrapper<NameProvider>nameProvider=new Wrapper<>(null);
		Wrapper<InfoProvider>infoProvider=new Wrapper<>(null);
		Wrapper<Completer>completer=new Wrapper<>(null);
		Input v=((Input)editable.getClass().getAnnotation(Input.class));
		if(v!=null)try{
			if(v.verifier()!=Verifier.class)verifier.var=v.verifier().getDeclaredConstructor().newInstance();
			if(v.nameProvider()!=NameProvider.class)nameProvider.var=v.nameProvider().getDeclaredConstructor().newInstance();
			if(v.infoProvider()!=InfoProvider.class)infoProvider.var=v.infoProvider().getDeclaredConstructor().newInstance();
			if(v.completer()!=Completer.class)completer.var=v.completer().getDeclaredConstructor().newInstance();
		}catch(ReflectiveOperationException ex){throw new IllegalStateException("Verifiers, name providers and info providers must have default constructors.",ex);}
		tab.add(nameField);
		tab.add(ok);
		ArrayList<Field>editableFields=new ArrayList<>();
		for(Field f:editable.getClass().getFields()){
			EditorEntry a=(EditorEntry)f.getAnnotation(EditorEntry.class);
			if(a==null)continue;
			for(String p:a.properties())if(p.equals("hide"))continue;
			editableFields.add(f);
		}
		CardLayout layout=new CardLayout();
		JPanel form=new JPanel(layout);
		Font font=new Font(Font.DIALOG,Font.PLAIN,editor.getHeight()*3/40);
		ArrayList<Supplier<?>>savers=new ArrayList<>();
		Wrapper<Supplier<?>>currentSaver=new Wrapper<Supplier<?>>(null);
		int k=0;
		Wrapper<EditableDemo>demo=new Wrapper<>(null);
		for(Field f:editableFields)try{
			EditorEntry a=(EditorEntry)f.getAnnotation(EditorEntry.class);
			JPanel entry=new JPanel(new GridLayout(1,2));
			JTextArea name=new JTextArea(a.translation());
			name.setEditable(false);
			name.setLineWrap(true);
			name.setWrapStyleWord(true);
			name.setFont(font);
			name.setBackground(Color.DARK_GRAY);
			name.setForeground(Color.WHITE);
			name.setBorder(BorderFactory.createTitledBorder(null,"Параметр",0,0,font.deriveFont(font.getSize2D()/2),Color.LIGHT_GRAY));
			entry.add(name);
			JComponent c;
			if(a.editorBaseSource()==EditorEntryBase.class)c=wrapEditorComponent(createEditorBase(editable,f,currentSaver),font);
			else c=a.editorBaseSource().getDeclaredConstructor().newInstance().createEditorBase(editable,f,currentSaver,demo);
			if(currentSaver.var==null)throw new IllegalStateException("Saver for "+f.getName()+" is null.");
			savers.add(currentSaver.var);
			if(c!=null){
				c.setBorder(BorderFactory.createTitledBorder(null,"Значение",0,0,font.deriveFont(font.getSize2D()/2),Color.LIGHT_GRAY));
				boolean flag=false;
				for(String s:a.properties())if(s.equals("readonly")||(!isNew&&s.equals("initonly"))){flag=true;break;}
				if(flag){
					c.setFocusable(false);
					JList<String>l=new JList<>();
					DefaultListModel<String>m=new DefaultListModel<>();
					m.add(0,"");
					l.setCellRenderer((list,value,index,isSelected,cellHasFocus)->{
						c.setPreferredSize(l.getSize());
						return c;
					});
					l.setModel(m);
					entry.add(l);
				}else entry.add(c);
			}
			if(c==null)form.add(String.valueOf(k),new JLabel("---"));
			else form.add(String.valueOf(k),entry);
			++k;
		}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
		demo.var=new EditableDemo(editable.getClass(),savers,()->nameField.getText());
		DefaultListModel<String>results=new DefaultListModel<>();
		JList<String>l=new JList<>(results);
		form.add(String.valueOf(k),l);
		ok.setText(savers.isEmpty()?"Готово":"Далее");
		ok.setBackground(savers.isEmpty()?Color.GREEN:Color.GRAY);
		JProgressBar p=new JProgressBar(0,savers.size());
		p.setBounds(editor.getWidth()/8,editor.getHeight()/6,editor.getWidth()*3/4,editor.getHeight()/20);
		p.setBackground(Color.WHITE);
		p.setForeground(Color.GREEN);
		tab.add(p);
		Wrapper<Integer>w=new Wrapper<Integer>(0);
		HButton cancel=null;
		if(deleter!=null){
			HButton delete=new HButton(10,5){
				public void paint(Graphics g){
					g.setColor(new Color(255-scale*8,20,20));
					g.fillRoundRect(0,0,getWidth(),getHeight(),getHeight()*2/3,getHeight()*2/3);
					((Graphics2D)g).setStroke(new BasicStroke(getHeight()/20));
					g.setColor(Color.DARK_GRAY);
					g.drawRoundRect(0,0,getWidth(),getHeight(),getHeight()*2/3,getHeight()*2/3);
					g.setColor(Color.BLACK);
					g.drawLine(getWidth()/3,getHeight()/3,getWidth()*2/3,getHeight()*2/3);
					g.drawLine(getWidth()*2/3,getHeight()/3,getWidth()/3,getHeight()*2/3);
					if(getModel().isPressed()){
						g.setColor(new Color(0,0,0,100));
						g.fillRoundRect(0,0,getWidth(),getHeight(),getHeight()*2/3,getHeight()*2/3);
					}
				}
			};
			delete.addActionListener(e->{deleter.run();editor.dispose();});
			delete.setBounds(editor.getWidth()*3/10,editor.getHeight()*9/10,editor.getHeight()/20,editor.getHeight()/20);
			delete.setToolTipText("Удалить");
			delete.setOpaque(false);
			tab.add(delete);
		}
		HButton complete=null;
		if(completer.var!=null){
			complete=new HButton(){
				public void paint(Graphics g){
					g.setColor(new Color(20,40+scale*10,20));
					g.fillRoundRect(0,0,getWidth(),getHeight(),getHeight()*2/3,getHeight()*2/3);
					((Graphics2D)g).setStroke(new BasicStroke(getHeight()/20));
					g.setColor(Color.DARK_GRAY);
					g.drawRoundRect(0,0,getWidth(),getHeight(),getHeight()*2/3,getHeight()*2/3);
					g.setColor(Color.BLACK);
					g.drawLine(getWidth()/3,getHeight()/3,getWidth()*2/3,getHeight()/2);
					g.drawLine(getWidth()*2/3,getHeight()/2,getWidth()/3,getHeight()*2/3);
					if(getModel().isPressed()){
						g.setColor(new Color(0,0,0,100));
						g.fillRoundRect(0,0,getWidth(),getHeight(),getHeight()*2/3,getHeight()*2/3);
					}
				}
			};
			complete.addActionListener(e->{
				try{
					editable.name=nameField.getText();
					for(int i=0;i<editableFields.size();++i)if(!(savers.get(i)instanceof EmptySaver))editableFields.get(i).set(editable,savers.get(i).get());
					completer.var.completeObject(editable,demo.var.get(),w.var);
					editor.dispose();
				}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
			});
			complete.setBounds(ok.getX()+ok.getWidth()*3/2,ok.getY(),ok.getHeight(),ok.getHeight());
			complete.setToolTipText("Заполнить автоматически");
			tab.add(complete);
			complete.setVisible(completer.var.isCompletable(editable,demo.var.get(),0));
		}
		final HButton fComplete=complete;
		if(!isNew){
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
			c.addActionListener(e->{
				if(w.var==0)editor.dispose();
				else{
					p.setValue(--w.var);
					layout.show(form,String.valueOf(w.var));
					if(w.var==0)c.setText("Отмена");
					ok.setText("Далее");
					ok.setBackground(Color.GRAY);
					if(fComplete!=null)fComplete.setVisible(completer.var.isCompletable(editable,demo.var.get(),0));
				}
			});
			c.setBounds(editor.getWidth()*2/5,editor.getHeight()*12/15,editor.getWidth()/5,editor.getHeight()/20);
			c.setText("Отмена");
			c.setOpaque(false);
			c.setFont(ok.getFont());
			tab.add(c);
			cancel=c;
		}
		final HButton fCancel=cancel;
		Wrapper<JComponent>info=new Wrapper<>(null);
		if(infoProvider.var!=null){
			info.var=infoProvider.var.provideInfo(editable,demo.var);
			info.var.setBounds(editor.getWidth()*5/6,editor.getHeight()/4,editor.getWidth()/6,editor.getHeight()/2);
			tab.add(info.var);
		}
		ok.addActionListener(e->{
			try{
				if(info.var!=null)info.var.dispatchEvent(new ComponentEvent(tab,ComponentEvent.COMPONENT_SHOWN));
				if(savers.isEmpty()){
					editor.dispose();
					editable.name=nameField.getText();
					return;
				}
				if(w.var==savers.size()){
					String verifierMsg=verifier.var==null?"":verifier.var.verify(editable,demo.var.get(),isNew);
					if(verifierMsg.isEmpty()){
						for(int i=0;i<editableFields.size();++i)if(!(savers.get(i)instanceof EmptySaver))editableFields.get(i).set(editable,savers.get(i).get());
						editor.dispose();
						editable.name=nameField.getText();
					}else{
						ok.setBackground(Color.RED);
						Timer t=new Timer(1000,evt->ok.setBackground(Color.GRAY));
						t.setRepeats(false);
						t.start();
						layout.show(form,String.valueOf(w.var=0));
						p.setValue(0);
						new Message(verifierMsg,Color.RED);
						if(fComplete!=null)fComplete.setVisible(completer.var.isCompletable(editable,demo.var.get(),0));
					}
				}else{
					p.setValue(++w.var);
					layout.show(form,String.valueOf(w.var));
					if(info.var!=null){
						if(info.var instanceof JTable)((AbstractTableModel)((JTable)info.var).getModel()).fireTableDataChanged();
						else if(info.var instanceof JScrollPane){
							Component c=((JScrollPane)info.var).getViewport().getView();
							if(c instanceof JTable)((AbstractTableModel)((JTable)c).getModel()).fireTableDataChanged();
						}
						info.var.repaint();
						info.var.revalidate();
					}
					if(w.var==savers.size()){
						ok.setText("Готово");
						ok.setBackground(Color.GREEN);
						if(nameProvider.var!=null)nameField.setText(nameProvider.var.provideName(demo.var.get()));
						results.removeAllElements();
						for(int i=0;i<editableFields.size();++i)if(!(savers.get(i)instanceof EmptySaver))results.addElement(editableFields.get(i).getAnnotation(EditorEntry.class).translation()+": "+savers.get(i).get());
					}
					if(fCancel!=null)fCancel.setText("Назад");
					if(fComplete!=null)fComplete.setVisible(completer.var.isCompletable(editable,demo.var.get(),w.var));
				}
			}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
		});
		form.setBounds(infoProvider.var==null?editor.getWidth()/8:editor.getWidth()/20,editor.getHeight()/4,editor.getWidth()*3/4,editor.getHeight()/2);
		tab.add(form);
		nameField.requestFocusInWindow();
		nameField.setSelectionStart(0);
		nameField.setSelectionEnd(nameField.getText().length());
		return tab;
	}
	public static JComponent wrapEditorComponent(JComponent a,Font font){
		a.setFont(font);
		a.setBackground(Color.DARK_GRAY);
		a.setForeground(Color.WHITE);
		a.setOpaque(true);
		return a;
	}
	@SuppressWarnings("unchecked")
	public static JComponent createEditorBase(Object o,Field f,Wrapper<Supplier<?>>saver){
		try{
			if(f.getType()==String.class){
				JTextArea a=new JTextArea((String)f.get(o));
				a.setLineWrap(true);
				a.setWrapStyleWord(true);
				saver.var=()->a.getText();
				return a;
			}else if(f.getType()==int.class){
				JSpinner a=new JSpinner(new SpinnerNumberModel(f.getInt(o),0,10000000,1));
				saver.var=()->a.getValue();
				return a;
			}else if(f.getType()==double.class){
				JTextArea a=new JTextArea(f.get(o).toString());
				a.setLineWrap(true);
				saver.var=()->{
					try{return Double.parseDouble(a.getText());}
					catch(Exception ex){
						a.setText("0");
						new Message("Неправильный формат числа, выбрано значение по умолчанию",Color.RED);
						return 0d;
					}
				};
				return a;
			}else if(f.getType()==float.class){
				JTextArea a=new JTextArea(f.get(o).toString());
				a.setLineWrap(true);
				saver.var=()->{
					try{return Float.parseFloat(a.getText());}
					catch(Exception exc){
						a.setText("0");
						new Message("Неправильный формат числа, выбрано значение по умолчанию",Color.RED);
						return 0f;
					}
				};
				return a;
			}else if(f.getType()==boolean.class){
				JCheckBox a=new JCheckBox();
				a.setSelected(f.getBoolean(o));
				saver.var=()->a.isSelected();
				return a;
			}else if(f.getType()==Integer.class){
				JSpinner a=new JSpinner(new SpinnerNumberModel(f.get(o)==null?-1:f.getInt(o),-1,10000000,1));
				JSpinner.NumberEditor t=new JSpinner.NumberEditor(a);
				//UNTESTED
				t.getTextField().setFormatterFactory(new AbstractFormatterFactory(){
					public AbstractFormatter getFormatter(JFormattedTextField tf){
						return new DefaultFormatter(){
							public Object stringToValue(String s)throws ParseException{
								if(s==null||s.isBlank())return -1;
								else return super.stringToValue(s);
							}
							public String valueToString(Object s)throws ParseException{
								if(((int)s)==-1)return "";
								else return super.valueToString(s);
							}
						};
					}
				});
				if(f.get(o)==null)a.setEditor(t);
				saver.var=()->a.getValue();
				return a;
			}else if(f.getType()==Double.class){
				JTextArea a=new JTextArea(f.get(o)==null?"":f.get(o).toString());
				a.setLineWrap(true);
				saver.var=()->{
					try{return a.getText().isBlank()?null:Double.parseDouble(a.getText());}
					catch(Exception ex){
						a.setText("0");
						new Message("Неправильный формат числа, выбрано значение по умолчанию",Color.RED);
						return 0d;
					}
				};
				return a;
			}else if(f.getType()==Float.class){
				JTextArea a=new JTextArea(f.get(o)==null?"":f.get(o).toString());
				a.setLineWrap(true);
				saver.var=()->{
					try{return a.getText().isBlank()?null:Float.parseFloat(a.getText());}
					catch(Exception exc){
						a.setText("0");
						new Message("Неправильный формат числа, выбрано значение по умолчанию",Color.RED);
						return 0f;
					}
				};
				return a;
			}else if(f.getType()==LocalTime.class){
				TimePicker a=new TimePicker();
				try{a.setTime((LocalTime)f.get(o));}catch(NullPointerException ex){throw new NullPointerException("LocalTime fields must be non-null");}
				saver.var=()->a.getTime();
				return a;
			}else if(f.getType()==LocalDate.class){
				DatePicker a=new DatePicker();
				try{a.setDate((LocalDate)f.get(o));}catch(NullPointerException ex){throw new NullPointerException("LocalDate fields must be non-null");}
				saver.var=()->a.getDate();
				return a;
			}else if(f.getType()==LocalDateTime.class){
				DateTimePicker d=new DateTimePicker();
				try{d.setDateTimeStrict((LocalDateTime)f.get(o));}catch(NullPointerException ex){throw new NullPointerException("LocalDateTime fields must be non-null");}
				saver.var=()->{
					try{
						return d.getDateTimeStrict()==null?f.get(o):d.getDateTimeStrict();
					}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
				};
				return d;
			}else if(Editable.class.isAssignableFrom(f.getType())){
				try{
					JComboBox<Editable>a=new JComboBox<>();
					for(Editable e:Data.getInstance().getGroup((Class<? extends Editable>)f.getType()))a.addItem(e);
					a.setSelectedItem(f.get(o));
					saver.var=()->a.getSelectedItem();
					return a;
				}catch(IllegalArgumentException exception){
					Wrapper<Editable>value=new Wrapper<>((Editable)f.get(o));
					JButton a=new JButton(value.var==null?"":value.var.name);
					ActionListener edit=e->{
						ProgramStarter.editor.constructEditor((Editable)value.var,false);
					};
					a.addActionListener(value.var==null?new ActionListener(){
						public void actionPerformed(ActionEvent e){
							try{
								f.set(o,f.getType().getDeclaredConstructor().newInstance());
								value.var=(Editable)f.get(o);
								ProgramStarter.editor.constructEditor((Editable)value.var,true,()->{
									try{f.set(o,null);}catch(IllegalAccessException ex){throw new IllegalStateException(ex);}
								});
								a.removeActionListener(this);
								a.addActionListener(edit);
							}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
						}
					}:edit);
					saver.var=()->value.var;
					return a;
				}
			}else if(EditableGroup.class.isAssignableFrom(f.getType())){
				JPanel a=new JPanel(new GridLayout(0,1));
				EditableGroup<?>group=(EditableGroup<?>)f.get(o);
				for(Editable editable:group){
					JButton b=group.createElementButton(editable,null);
					b.addActionListener(e->{
						ProgramStarter.editor.constructEditor(editable,false,()->{
							group.remove(editable);
							a.remove(b);
							a.revalidate();
						});
					});
					a.add(b);
				}
				JButton create=group.createAddButton(null);
				create.addActionListener(e->{
					try{
						Editable nEditable=group.type.getDeclaredConstructor().newInstance();
						group.add(nEditable);
						JButton b=group.createElementButton(nEditable,null);
						Runnable deleter=()->{
							group.remove(nEditable);
							a.remove(b);
							a.revalidate();
						};
						b.addActionListener(evt->ProgramStarter.editor.constructEditor(nEditable,false,deleter));
						a.add(b,a.getComponentCount()-1);
						ProgramStarter.editor.constructEditor(nEditable,true,deleter);
						a.revalidate();
					}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
				});
				a.add(create);
				saver.var=()->group;
				return a;
			}else if(f.getType().isEnum()){
				JComboBox<Object>a=new JComboBox<>();
				Object en=f.get(o);
				if(en==null)throw new NullPointerException("Enum value of field \""+f.getName()+"\" is null.");
				for(Object obj:(Object[])en.getClass().getMethod("values").invoke(o))a.addItem(obj);
				a.setSelectedItem(f.get(o));
				saver.var=()->a.getSelectedItem();
				return a;
			}
		}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
		throw new UnsupportedOperationException("Component for "+f.getType()+" is not defined.");
	}
}