package com.bpa4j.defaults.input;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import com.bpa4j.Wrapper;
import com.bpa4j.core.Data.Editable;
import com.bpa4j.core.EditableDemo;
import com.bpa4j.editor.EditorEntryBase;
import com.bpa4j.ui.LazyPanel;
import com.bpa4j.util.SprintUI;

/**
 * An editor that suggest the user selection from an {@code ArrayList}, provided by the element supplier.
 * If the field is a {@code Collection}, the choice is multiple, otherwise it is singular.
 */
public class SelectFromEditor implements EditorEntryBase{
	private static HashMap<Field,Function<Editable,ArrayList<?>>>elementSuppliers=new HashMap<>();
	/**
	 * Configures the editor for the specified field.
	 * @param f - field to configure for
	 * @param elementSupplier - a Function (accepting the editable object) to get elements from
	 */
	public static void configure(Field f,Function<Editable,ArrayList<?>>elementSupplier){elementSuppliers.put(f,elementSupplier);}
	@SuppressWarnings({ "unchecked", "null" })
	public JComponent createEditorBase(Object o, Field f, Wrapper<Supplier<?>> saver, Wrapper<EditableDemo> demo) {
		Wrapper<Supplier<?>> w = new Wrapper<>(()->{
			try { return f.get(o); } catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
		});
		saver.var = ()-> w.var.get();

		boolean isCollection = Collection.class.isAssignableFrom(f.getType());
		Object currentValue;
		try { currentValue = f.get(o); } catch (IllegalAccessException ex) { throw new IllegalStateException(ex); }

		ArrayList<Object> c;
		if (isCollection) {
			c = new ArrayList<>();
			c.addAll((Collection<Object>) currentValue);
		} else {
			c = null;
		}

		return new LazyPanel(panel -> {
			ArrayList<?> elements = elementSuppliers.get(f).apply(demo.var.get());
			if (isCollection) {
				JMenu m = SprintUI.createMenu();
				for (Object element : elements) {
					JCheckBoxMenuItem item = new JCheckBoxMenuItem(String.valueOf(element));
					item.setSelected(c.contains(element));
					item.addActionListener(e -> {
						if (item.isSelected()) c.add(element);
						else c.remove(element);
					});
					m.add(item);
				}
				w.var = () -> {
					try {
						Collection<Object> l = (Collection<Object>) f.getType().getDeclaredConstructor().newInstance();
						l.addAll(c);
						return l;
					} catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
				};
				panel.add(SprintUI.wrap(m));
			} else {
				JComboBox<Object> combo = new JComboBox<>();
				for (Object item : elements.toArray()) combo.addItem(item);
				combo.setSelectedItem(currentValue);
				w.var = () -> combo.getSelectedItem() == null && f.getType().isPrimitive() ? currentValue : combo.getSelectedItem();
				panel.add(combo);
			}
		});
	}
}
