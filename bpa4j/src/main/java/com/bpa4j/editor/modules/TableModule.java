package com.bpa4j.editor.modules;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.bpa4j.defaults.table.FieldCellRenderer;
import com.bpa4j.defaults.table.FieldCellValue;
import com.bpa4j.defaults.table.FormCellEditor;
import com.bpa4j.editor.EditorEntry;
import com.bpa4j.ui.HButton;
import com.bpa4j.core.Data.Editable;

import java.awt.Graphics;

/**
 * A module that displays a table of a particular field of the given type.
 */
public class TableModule implements EditorModule{
	private Field f;
	private Class<?>type;
	private ArrayList<Consumer<JTable>>tableDecorators=new ArrayList<>();
	/**
	 * Constructs a new TableModule for the given field.
	 * @param f - field to make table for
	 * @param type - type of the {@code f} field
	 */
	public TableModule(Field f,Class<?>type){this.f=f;this.type=type;}
	/**
	 * <p>Adds a table decorator to this module.</p>
	 * <p>
	 * Table decorators will process the table additionaly upon creation.
	 * </p>
	 * @param decorator - table decorator to be used
	 */
	public TableModule addTableDecorator(Consumer<JTable>decorator){
		if(tableDecorators==null)tableDecorators=new ArrayList<>();
		tableDecorators.add(decorator);
		return this;
	}
	public JPanel createTab(JDialog editor,Editable editable,boolean isNew,Runnable deleter){
		try{
			if(f.get(editable)==null)throw new IllegalStateException("The field designated for TableModule must be non-null.");
			@SuppressWarnings("unchecked")
			Collection<Object>a=(Collection<Object>)f.get(editable);
			JPanel tab=new JPanel(new BorderLayout());
			tab.setSize(editor.getSize());
			JTable t=new JTable();
			t.setPreferredSize(new Dimension(tab.getWidth()*2/3,tab.getHeight()*2/3));
			t.setDefaultEditor(Object.class,new FormCellEditor());
			t.setDefaultRenderer(Object.class,new FieldCellRenderer());
			List<Field>fields=Stream.of(type.getFields()).filter(f->f.isAnnotationPresent(EditorEntry.class)).toList();
			DefaultTableModel m=new DefaultTableModel(fields.stream().map(f->f.getAnnotation(EditorEntry.class).translation()).toArray(),0);
			for(Object o:a){
				FieldCellValue[]v=new FieldCellValue[fields.size()];
				for(int i=0;i<fields.size();++i)v[i]=new FieldCellValue(fields.get(i),o);
				m.addRow(v);
			}
			t.setModel(m);
			for(Consumer<JTable>c:tableDecorators)c.accept(t);
			JScrollPane s=new JScrollPane(t,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			tab.add(s,BorderLayout.CENTER);
			HButton b=new HButton(){
				public void paint(Graphics g){
					int c=50-scale*4;
					if(getModel().isPressed())c-=25;
					g.setColor(new Color(c,c,c));
					g.fillRect(0,0,getWidth(),getHeight());
					FontMetrics fm=g.getFontMetrics();
					g.setColor(Color.WHITE);
					g.drawString("Добавить",(getWidth()-fm.stringWidth("Добавить"))/2,(getHeight()+fm.getAscent()+fm.getLeading()-fm.getDescent())/2);
				};
			};
			b.addActionListener(e->{
				try{
					Object o=type.getDeclaredConstructor().newInstance();
					a.add(o);
					FieldCellValue[]v=new FieldCellValue[fields.size()];
					for(int i=0;i<fields.size();++i)v[i]=new FieldCellValue(fields.get(i),o);
					m.addRow(v);
					tab.revalidate();
				}catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
			});
			b.setPreferredSize(new Dimension(tab.getWidth(),tab.getHeight()/10));
			tab.add(b,BorderLayout.SOUTH);
			return tab;
		}catch(IllegalAccessException ex){throw new RuntimeException(ex);}
	}
}
