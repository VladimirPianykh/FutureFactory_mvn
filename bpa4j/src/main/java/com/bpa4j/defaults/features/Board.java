package com.bpa4j.defaults.features;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.bpa4j.core.Data;
import com.bpa4j.core.Data.Editable;
import com.bpa4j.core.ProgramStarter;
import com.bpa4j.core.User.Feature;
import com.bpa4j.defaults.table.FieldCellValue;
import com.bpa4j.defaults.table.FormCellEditor;
import com.bpa4j.editor.EditorEntry;
import com.bpa4j.navigation.HelpView.FeatureInstruction;
import com.bpa4j.navigation.ImplementedInfo;
import com.bpa4j.ui.AutoLayout;
import com.bpa4j.ui.HButton;

/**
 * A board with objects of the given type.
 * Similar classes are called Board-like.
 * <p>
 * Generally an editable table,
 * which can be supplemented with configuration components: filterrs, etc.
 */
@SuppressWarnings("unchecked")
public final class Board<T extends Serializable>implements Feature{
	public static interface Filter<T>extends Predicate<T>{
		public default JComponent getConfigurator(Runnable saver,ArrayList<T>objects){return null;}
	}
	public static interface Sorter<T>extends Comparator<T>{
		public default JComponent getConfigurator(Runnable saver,ArrayList<T>objects){return null;}
	}
	private static class Slicer<T>implements Filter<T>{
		private JComboBox<String>c;
		private final Function<T,String>f;
		public Slicer(Function<T,String>function){f=function;}
		public JComponent getConfigurator(Runnable saver,ArrayList<T>objects){
			HashSet<String>set=new HashSet<>();
			for(T t:objects)set.add(f.apply(t));
			c=new JComboBox<>();
			for(String str:set)c.addItem(str);
			c.addItemListener(e->saver.run());
			return c;
		}
		public boolean test(T t){return f.apply(t).equals(c.getSelectedItem());}
	}
	static{
		if(!Data.getInstance().ftrInstances.containsKey(Board.class.getName()))Data.getInstance().ftrInstances.put(Board.class.getName(),new HashMap<>());
	}
	private final Class<T>type;
	private final String name;
	private ArrayList<T>objects=new ArrayList<>(){
		private void writeObject(ObjectOutputStream out)throws IOException,ClassNotFoundException{
			if(elementSupplier!=null)clear();
			out.defaultWriteObject();
		}
	};
	private transient ArrayList<Consumer<JTable>>tableDecorators;
	private transient Sorter<T>sorter;
	private transient Filter<T>filter;
	private transient Supplier<ArrayList<T>>elementSupplier;
	private transient boolean allowCreation;
	private Board(String name,Class<T>type){this.name=name;this.type=type;}
	public static<T extends Serializable>Board<T>getBoard(String name){
		if(((HashMap<String,Board<?>>)Data.getInstance().ftrInstances.get(Board.class.getName())).containsKey(name))return(Board<T>)((HashMap<String,Board<?>>)Data.getInstance().ftrInstances.get(Board.class.getName())).get(name);
		else throw new IllegalArgumentException("Board \""+name+"\" does not exist.");
	}
	public static<T extends Serializable>Board<T>registerBoard(String name,Class<T>type){
		if(((HashMap<String,Board<?>>)Data.getInstance().ftrInstances.get(Board.class.getName())).containsKey(name)&&((HashMap<String,Board<?>>)Data.getInstance().ftrInstances.get(Board.class.getName())).get(name).type.equals(type))return(Board<T>)((HashMap<String,Board<?>>)Data.getInstance().ftrInstances.get(Board.class.getName())).get(name);
		Board<T>b=new Board<T>(name,type);
		((HashMap<String,Board<?>>)Data.getInstance().ftrInstances.get(Board.class.getName())).put(name,b);
		return b;
	}
	@SuppressWarnings("PMD.ReplaceVectorWithList")
	private void fillTable(DefaultTableModel m){
		if(sorter!=null)objects.sort(sorter);
		if(filter==null)for(T t:objects){
			Vector<Object>values=new Vector<>();
			for(Field f:type.getFields())if(f.isAnnotationPresent(EditorEntry.class))values.add(new FieldCellValue(f,t));
			m.addRow(values);
		}else for(T t:objects){
			if(filter.test(t)){
				Vector<Object>values=new Vector<>();
				for(Field f:type.getFields())if(f.isAnnotationPresent(EditorEntry.class))values.add(new FieldCellValue(f,t));
				m.addRow(values);
			}
		}
	}
	/**
	 * <p>Adds a table decorator to this board.</p>
	 * <p>
	 * Table decorators will process the table additionaly upon creation.
	 * @param decorator - table decorator to be used
	 */
	public Board<T>addTableDecorator(Consumer<JTable>decorator){
		if(tableDecorators==null)tableDecorators=new ArrayList<>();
		tableDecorators.add(decorator);
		return this;
	}
	/**
	 * <p>Sets a filter to this board.</p>
	 * <p>Filter is an objects that provides a {@link JComponent} for configuration
	 * and a {@code test(T)} method to check if an element should be displayed.</p>
	 * @param filter - filter to be used
	 */
	public Board<T>setFilter(Filter<T>filter){
		this.filter=filter;
		return this;
	}
	/**
	 * <p>Sets a slicer to this board.</p>
	 * <p>
	 * Slicer names a group for each object given.
	 * Tasks with the same name will be displayed simultaneously.
	 * Slicer will replace the current filter.
	 * @param slicer - function to be used as a slicer
	 */
	public Board<T>setSlicer(Function<T,String>slicer){
		filter=new Slicer<T>(slicer);
		return this;
	}
	/**
	 * Sets a sorter to this board.
	 * @param sorter - comparator to be used for sorting
	 */
	public Board<T>setSorter(Sorter<T>sorter){
		this.sorter=sorter;
		return this;
	}
	/**
	 * Sets a supplier to get elements from (instead of saving and loading them).
	 * @param supplier - supplier to be used
	 */
	public Board<T>setElementSupplier(Supplier<ArrayList<T>>supplier){
		elementSupplier=supplier;
		return this;
	}
	/**
	 * Permits creation of new elements if {@code allow} is true.
	 */
	public Board<T>setAllowCreation(boolean allow){allowCreation=allow;return this;}
	public ArrayList<T>getObjects(){return objects;}
	public T createObject(){
		try{
			T object=type.getDeclaredConstructor().newInstance();
			objects.add(object);
			return object;
		}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
	}
	public void fillTab(JPanel content,JPanel tab,Font font){
		if(elementSupplier!=null)objects=elementSupplier.get();
		tab.setLayout(new BorderLayout());
		tab.setBorder(BorderFactory.createEmptyBorder(tab.getHeight()/300,tab.getWidth()/300,tab.getHeight()/300,tab.getWidth()/300));
		JPanel config=null;
		JComponent filterConfig=null,sorterConfig=null;
		JTable t=new JTable();
		t.setBackground(Color.DARK_GRAY);
		t.setForeground(Color.WHITE);
		t.setRowHeight(tab.getHeight()/10);
		t.getTableHeader().setFont(new Font(Font.DIALOG,Font.ITALIC,tab.getHeight()/30));
		t.setFillsViewportHeight(true);
		t.setDefaultEditor(Object.class,new FormCellEditor());
		JScrollPane sPane=new JScrollPane(t);
		sPane.setBorder(BorderFactory.createTitledBorder(null,"Задания",0,0,new Font(Font.DIALOG,Font.PLAIN,tab.getHeight()/50),Color.WHITE));
		ArrayList<String>s=new ArrayList<>();
		for(Field f:type.getFields())if(f.isAnnotationPresent(EditorEntry.class))s.add(f.getAnnotation(EditorEntry.class).translation());
		DefaultTableModel m=new DefaultTableModel(s.toArray(new String[0]),0);
		if(filter!=null)filterConfig=filter.getConfigurator(()->{m.setRowCount(0);fillTable(m);},objects);
		if(sorter!=null)sorterConfig=sorter.getConfigurator(()->{m.setRowCount(0);fillTable(m);},objects);
		if(allowCreation||filterConfig!=null||sorterConfig!=null){
			config=new JPanel(new AutoLayout());
			config.setPreferredSize(new Dimension(tab.getWidth(),tab.getHeight()/9));
			if(filterConfig!=null)config.add(filterConfig);
			if(sorterConfig!=null)config.add(sorterConfig);
			if(allowCreation){
				HButton create=new HButton(){
					public void paint(Graphics g){
						g.setColor(Color.DARK_GRAY);
						g.fillRect(0,0,getWidth(),getHeight());
						FontMetrics fm=g.getFontMetrics();
						g.setColor(Color.WHITE);
						g.drawString("Добавить",(getWidth()-fm.stringWidth("Добавить"))/2,(getHeight()+fm.getAscent()+fm.getLeading()-fm.getDescent())/2);
					}
				};
				create.addActionListener(e->{
					try{
						T o=type.getDeclaredConstructor().newInstance();
						objects.add(o);
						if(o instanceof Editable)ProgramStarter.editor.constructEditor((Editable)o,true,()->objects.remove(o));
						m.setRowCount(0);
						fillTable(m);
						tab.revalidate();
					}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
				});
				config.add(create);
			}
		}
		fillTable(m);
		sPane.setPreferredSize(new Dimension(tab.getWidth(),config==null?tab.getHeight():tab.getHeight()*8/9));
		t.setModel(m);
		if(tableDecorators!=null)for(Consumer<JTable>c:tableDecorators)c.accept(t);
		tab.add(sPane,BorderLayout.SOUTH);
		if(config!=null)tab.add(config,BorderLayout.NORTH);
	}
	public void paint(Graphics2D g2,BufferedImage image,int s){
		g2.setStroke(new BasicStroke(s/10));
		g2.drawRect(s/10,s/5,s*4/5,s*3/5);
		g2.setStroke(new BasicStroke(s/20));
		g2.drawPolygon(new int[]{s/2,s/4,s/2,s/3,s/2,s/2,s/2,s*2/3,s/2,s*3/4,s/2,s*2/3,s/2,s/2,s/2,s/3,s/2},new int[]{s/2,s/2,s/2,s/3,s/2,s/4,s/2,s/3,s/2,s/2,s/2,s*2/3,s/2,s*3/4,s/2,s*2/3,s/2},16);
        g2.fillOval(s/3,s/3,s/3,s/3);
	}
	public String toString(){return name;}
	/**
	 * AI-generated.
	 */
	public List<ImplementedInfo>getImplementedInfo(){
		// Получаем информацию от типа объектов, а не от конкретных экземпляров
		try{
			T prototype=type.getDeclaredConstructor().newInstance();
			if(prototype instanceof Editable){
				return ((Editable)prototype).getImplementedInfo().stream()
					.<ImplementedInfo>map(info -> info.appendInstruction(new FeatureInstruction(this)))
					.collect(java.util.stream.Collectors.toList());
			}
		}catch(ReflectiveOperationException ex){
			throw new IllegalStateException(ex);
		}
		return List.of();
	}
}