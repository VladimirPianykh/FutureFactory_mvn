package com.bpa4j.defaults.ftr_attributes.data_renderers;

import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import com.bpa4j.defaults.table.EmptyCellEditor;
import com.bpa4j.editor.EditorEntry;
import com.bpa4j.ui.HButton;
import com.bpa4j.ui.Message;
import com.bpa4j.util.excel.ExcelUtils;

/**
 * <p>Returns a table, rendering all editable fields of the component given.</p>
 * <p>You can create a separate info class or use an existing editable to fill the table.</p>
 */
public class TableDataRenderer<T>implements Supplier<JComponent>{
	private Supplier<ArrayList<T>>elementSupplier;
	private String title;
	private boolean allowExport;
	private Consumer<JTable>tableDecorator;
	public TableDataRenderer(Supplier<ArrayList<T>>elementSupplier){this.elementSupplier=elementSupplier;}
	public TableDataRenderer(Supplier<ArrayList<T>>elementSupplier,String title){this(elementSupplier);this.title=title;}
	public TableDataRenderer(Supplier<ArrayList<T>>elementSupplier,String title,boolean allowExport){this(elementSupplier,title);this.allowExport=allowExport;}
	public TableDataRenderer<T>addTableDecorator(Consumer<JTable>tableDecorator){
		this.tableDecorator=tableDecorator;
		return this;
	}
	@SuppressWarnings("PMD.UseArraysAsList")
	public JComponent get(){
		ArrayList<T>a=elementSupplier.get();
		if(a.isEmpty())return new JTable();
		Field[]allFields=a.get(0).getClass().getFields();
		ArrayList<Field>fields=new ArrayList<>();
		for(Field f:allFields)if(f.isAnnotationPresent(EditorEntry.class))fields.add(f);
		DefaultTableModel m=new DefaultTableModel(fields.stream().map(f->f.getAnnotation(EditorEntry.class).translation()).toArray(),0);
		JTable table=new JTable(m);
		table.setDefaultEditor(Object.class,new EmptyCellEditor());
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		if(tableDecorator!=null)tableDecorator.accept(table);
		for(T t:a){
			Object[]o=new Object[fields.size()];
			for(int i=0;i<o.length;++i)try{
				o[i]=fields.get(i).get(t);
			}catch(IllegalAccessException ex){throw new IllegalStateException(ex);}
			m.addRow(o);
		}
		JScrollPane s=new JScrollPane(table,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		if(title!=null)s.setBorder(BorderFactory.createTitledBorder(title));
		if(allowExport){
			GridBagLayout l=new GridBagLayout();
			l.rowWeights=l.columnWeights=new double[]{0.2,0.2,0.2,0.2,0.2};
			JPanel p=new JPanel(l);
			GridBagConstraints c1=new GridBagConstraints(),c2=new GridBagConstraints();
			HButton export=new HButton(){
				public void paint(Graphics g){
					Graphics2D g2=(Graphics2D)g;
					g2.setColor(new Color(12,54,3));
					g2.fillRect(0,0,getWidth(),getHeight());
					g2.setStroke(new BasicStroke(getHeight()/30));
					g2.setColor(Color.BLACK);
					g2.drawRect(0,0,getWidth(),getHeight());
					g2.setColor(new Color(0,0,0,(getModel().isPressed()?50:10)+scale*4));
					g2.fillRect(0,0,getWidth(),getHeight());
					FontMetrics fm=g2.getFontMetrics();
					g2.setColor(Color.WHITE);
					g2.drawString("Экспорт",(getWidth()-fm.stringWidth("Экспорт"))/2,(getHeight()+fm.getAscent()+fm.getLeading()-fm.getDescent())/2);
				}
			};
			export.addActionListener(e->{
				String home = System.getProperty("user.home");
				String outputPath = home + "\\Downloads\\" + title + ".xlsx";
				try {
					ExcelUtils.saveInstances(new File(outputPath), a);
				} catch (IllegalAccessException ex) {
					new Message("Не удалось экспортировать таблицу", Color.RED);
				}
				new Message("Файл экспортирован. Проверьте папку Загрузки", Color.GREEN);
			});
			c1.gridx=c1.gridy=4;
			c1.gridwidth=c1.gridheight=1;
			c1.weightx=c1.weighty=0.2;
			c1.fill=GridBagConstraints.BOTH;
			p.add(export,c1);
			c2.gridx=c2.gridy=0;
			c2.gridwidth=c2.gridheight=GridBagConstraints.REMAINDER;
			c2.weightx=c2.weighty=1;
			c2.fill=GridBagConstraints.BOTH;
			p.add(s,c2);
			return p;
		}else return s;
	}
}
