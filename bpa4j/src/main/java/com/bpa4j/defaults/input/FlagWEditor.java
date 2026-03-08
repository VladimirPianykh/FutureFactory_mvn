package com.bpa4j.defaults.input;

import java.awt.GridLayout;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Supplier;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.bpa4j.Wrapper;
import com.bpa4j.core.EditableDemo;
import com.bpa4j.editor.EditorEntryBase;
import com.bpa4j.editor.modules.FormModule;

/**
 * An editor wrapper that displays a flag (checkbox), indicating whether this field actually exists.
 */
public class FlagWEditor implements EditorEntryBase{
	private static final HashMap<Field,Class<? extends EditorEntryBase>>types=new HashMap<>();
	private static final HashMap<Field,Object>defaults=new HashMap<>();
	private static final HashMap<Field,Supplier<?>>initials=new HashMap<>();
	/**
	 * Configures the editor for the specified field.
	 * @param f - field to configure for
	 * @param editor - an editor to wrap
	 * @param defaultValue - a value that used if flag is not set
	 * @param initValue - a value that is set when a flag has just been selected
	 */
	public static void configure(Field f,Class<? extends EditorEntryBase>editor,Object defaultValue,Supplier<?>initValue){
		types.put(f,editor);
		defaults.put(f,defaultValue);
		initials.put(f,initValue);
	}
	public JComponent createEditorBase(Object o,Field f,Wrapper<Supplier<?>>saver,Wrapper<EditableDemo>demo){
		try{
			EditorEntryBase editor=types.get(f)==null?null:types.get(f).getDeclaredConstructor().newInstance();
			Object d=defaults.get(f),init=initials.get(f).get();
			JPanel p=new JPanel(new GridLayout());
			Wrapper<Supplier<?>>subSaver=new Wrapper<Supplier<?>>(null);
			JCheckBox b=new JCheckBox();
			boolean disabled=Objects.equals(f.get(o),d);
			if(disabled)f.set(o,init);
			JComponent c=editor==null?FormModule.wrapEditorComponent(FormModule.createEditorBase(o,f,subSaver),null):editor.createEditorBase(o,f,subSaver,demo);
			if(disabled)f.set(o,d);
			b.addActionListener(e->{
				if(b.isSelected())p.add(c);
				else p.remove(c);
				p.revalidate();
			});
			p.add(b);
			if(!disabled){
				b.setSelected(true);
				p.add(c);
			}
			saver.var=()->b.isSelected()?subSaver.var.get():d;
			return p;
		}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
	}
}
