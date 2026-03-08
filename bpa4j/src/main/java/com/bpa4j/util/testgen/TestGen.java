package com.bpa4j.util.testgen;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.bpa4j.core.Data;
import com.bpa4j.core.Data.Editable;
import com.bpa4j.core.Data.EditableGroup;
import com.bpa4j.defaults.editables.AbstractCustomer;
import com.bpa4j.editor.EditorEntry;
import com.bpa4j.editor.Input;
import com.bpa4j.editor.NameProvider;
import com.bpa4j.editor.Verifier;

/**
 * Utility for test data generation with a fluent API.
 * @author fluent-API - AI-generated
 */
public class TestGen<T extends Editable> implements Supplier<T>{
	// private final String[] names={
	// 	"Алиса", "Боря", "Дима", "Оля", "Егор", "Таня", "Витя", "Костя", "Юля", "Стас", "Галя",
	// 	"Марк", "Настя", "Федя", "Ира", "Мила", "Сева", "Лера", "Паша", "Лёва", "Ника",
	// 	"Тим", "Рита", "Жора", "Аня", "Вика", "Гоша", "Рома", "Сава", "Зоя", "Яна",
	// 	"Макс", "Лиза", "Рената", "Саша", "Катя", "Варя", "Гена", "Нина", "Арс", "Алла",
	// 	"Илья", "Вова", "Соня", "Лёша", "Женя", "Петя", "Олег", "Юра", "Маша",
	// 	"Лена", "Даша", "Толя", "Надя", "Лика", "Тима", "Кира", "Глеб", "Миша",
	// 	"Люба", "Вера", "Аркаша", "Мария", "Семен", "Света", "Ваня", "Ярик", "Филя"
	// };
	public enum VerificationPolicy{
		IGNORE,
		WARN,
		DELETE,
		THROW
	}
	private final Class<T> type;
	private int amount;
	private EditableGroup<T> target;
	private int currentIndex;
	private Map<String, Object> fieldOverrides=new HashMap<>();
	private Supplier<String> nameSupplier;
	private String namePattern;
	private NameProvider nameProvider;
	private Map<Class<?>, Supplier<?>> defaultSources=new HashMap<>();
	private Verifier verifier;
	private Set<String> skipFields=new HashSet<>();
	private T template;
	private VerificationPolicy verificationPolicy=VerificationPolicy.WARN;
	private TestGen(Class<T> type){
		this.type=type;
		this.currentIndex=0;
	}
	public static <T extends Editable> TestGen<T> gen(Class<T> type){
		return new TestGen<>(type);
	}
	public TestGen<T> withField(String name, Object valueOrSupplier){
		fieldOverrides.put(name,valueOrSupplier);
		return this;
	}
	public TestGen<T> withFields(Map<String, Object> fieldsMap){
		fieldOverrides.putAll(fieldsMap);
		return this;
	}
	public TestGen<T> setDefaultSource(Class<?> fieldType, Supplier<?> source){
		defaultSources.put(fieldType,source);
		return this;
	}
	public TestGen<T> skipField(String... fieldNames){
		skipFields.addAll(Arrays.asList(fieldNames));
		return this;
	}
	public TestGen<T> setTemplate(T template){
		this.template=template;
		return this;
	}
	public TestGen<T> withName(String name){
		return withName(()->name);
	}
	public TestGen<T> withName(Supplier<String>name){
		nameSupplier=name;
		return this;
	}
	public TestGen<T> withName(List<String>names){
		int[]i={0};
		return withName(()->names.get(i[0]++));
	}
	public TestGen<T> withName(String...names){
		return withName(List.of(names));
	}
	public TestGen<T> withNamePattern(String pattern){
		namePattern=pattern;
		return this;
	}
	public TestGen<T> withNameProvider(NameProvider p){
		nameProvider=p;
		return this;
	}
	public TestGen<T> withVerifier(Verifier v){
		verifier=v;
		return this;
	}
	public TestGen<T> setVerificationFailurePolicy(VerificationPolicy policy){
		verificationPolicy=policy;
		return this;
	}
	public TestGen<T> to(EditableGroup<T> target, int amount){
		this.target=target;
		this.amount=amount;
		this.currentIndex=0;
		for(int i=0;i<amount;++i){
			T obj=get();
			if(obj!=null){
				target.add(obj);
			}
		}
		return this;
	}
	public List<T> toList(int n){
		List<T> list=new ArrayList<>();
		for(int i=0;i<n;++i){
			T obj=get();
			if(obj!=null){
				list.add(obj);
			}
		}
		return list;
	}
	@SuppressWarnings("unchecked")
	public T[] toArray(int n){
		T[] array=(T[])java.lang.reflect.Array.newInstance(type,n);
		int index=0;
		for(int i=0;i<n;++i){
			T obj=get();
			if(obj!=null){
				array[index++]=obj;
			}
		}
		return array;
	}
	public TestGen<T> clone(){
		TestGen<T> copy=new TestGen<>(type);
		copy.amount=amount;
		copy.target=target;
		copy.currentIndex=currentIndex;
		copy.fieldOverrides=new HashMap<>(fieldOverrides);
		copy.nameSupplier=nameSupplier;
		copy.namePattern=namePattern;
		copy.nameProvider=nameProvider;
		copy.defaultSources=new HashMap<>(defaultSources);
		copy.verifier=verifier;
		copy.skipFields=new HashSet<>(skipFields);
		copy.template=template;
		copy.verificationPolicy=verificationPolicy;
		return copy;
	}
	@Override
	@SuppressWarnings("unchecked")
	public T get(){
		try{
			T e=type.getDeclaredConstructor().newInstance();
			if(template!=null){
				Field[] fields=type.getDeclaredFields();
				for(Field f:fields){
					if(!skipFields.contains(f.getName())&&fieldOverrides.containsKey(f.getName())){
						f.setAccessible(true);
						Object templateValue=f.get(template);
						if(templateValue!=null){
							try{f.set(e,templateValue);}catch(Exception ex){}
						}
					}
				}
			}
			Input a=type.getAnnotation(Input.class);
			NameProvider p=nameProvider;
			if(p==null&&a!=null&&a.nameProvider()!=NameProvider.class)p=a.nameProvider().getDeclaredConstructor().newInstance();
			Verifier v=verifier;
			if(v==null&&a!=null&&a.verifier()!=Verifier.class)v=a.verifier().getDeclaredConstructor().newInstance();
			for(Field f:type.getFields()){
				if(skipFields.contains(f.getName()))continue;
				if(Editable.class.isAssignableFrom(f.getType())&&f.isAnnotationPresent(EditorEntry.class)){
					try{
						EditableGroup<?> fieldGroup=Data.getInstance().getGroup((Class<? extends Editable>)f.getType());
						f.set(e,fieldGroup.get(currentIndex%fieldGroup.size()));
					}catch(IllegalArgumentException ex){}
				}
				if(fieldOverrides.containsKey(f.getName())){
					Object override=fieldOverrides.get(f.getName());
					if(override instanceof Supplier){
						f.set(e,((Supplier<?>)override).get());
					}else{
						f.set(e,override);
					}
				}else{
					Class<?> fieldType=f.getType();
					if(defaultSources.containsKey(fieldType)){
						f.set(e,defaultSources.get(fieldType).get());
					}else{
						Supplier<?> defaultSource=DefaultSourceRegistry.getDefaultSource(fieldType);
						if(defaultSource!=null){
							f.set(e,defaultSource.get());
						}
					}
				}
			}
			if(p!=null)e.name=p.provideName(e);
			else if(nameSupplier!=null)e.name=((Supplier<String>)nameSupplier).get();
			else if(namePattern!=null)e.name=String.format(namePattern,currentIndex);
			else if(e instanceof AbstractCustomer)e.name=DefaultSourceRegistry.names[currentIndex%DefaultSourceRegistry.names.length];
			else e.name+=" #"+(int)(Math.random()*1000000);
			if(v!=null){
				String verdict=v.verify(e,e,true);
				if(!verdict.isEmpty()){
					switch(verificationPolicy){
						case THROW:
							throw new IllegalStateException(e.name+" of type "+type.getName()+" is invalid. Verifier verdict: "+verdict);
						case WARN:
							System.err.println("WARNING: "+e.name+" of type "+type.getName()+" is invalid. Verifier verdict: "+verdict);
							break;
						case DELETE:
							System.err.println("WARNING: Deleted "+e.name+" of type "+type.getName()+" because it is invalid. Verifier verdict: "+verdict);
							return null;
						case IGNORE:
							break;
					}
				}
			}
			currentIndex++;
			return e;
		}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
	}
	/**
	 * Generates objects for the given groups randomly.
	 * <p>Groups are processed in the order of passing.</p>
	 * <p>
	 * Generated objects meet the following conditions:
	 * <ul>
	 *   <li>
	 *     For each {@code Editable} field at least one of the following is true:
	 *     <ul>
	 *       <li>Each {@code Editable} from the same group is taken.</li>
	 *       <li>None of the same group is taken twice.</li>
	 *     </ul>
	 *   </li>
	 * </ul>
	 * @deprecated Use the fluent API: {@code TestGen.gen(Class).to(group, amount)}
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	public static void generate(int amount,EditableGroup<?>...groups){
		String[] staticNames={
			"Алиса", "Боря", "Дима", "Оля", "Егор", "Таня", "Витя", "Костя", "Юля", "Стас", "Галя",
			"Марк", "Настя", "Федя", "Ира", "Мила", "Сева", "Лера", "Паша", "Лёва", "Ника",
			"Тим", "Рита", "Жора", "Аня", "Вика", "Гоша", "Рома", "Сава", "Зоя", "Яна",
			"Макс", "Лиза", "Рената", "Саша", "Катя", "Варя", "Гена", "Нина", "Арс", "Алла",
			"Илья", "Вова", "Соня", "Лёша", "Женя", "Петя", "Олег", "Юра", "Маша",
			"Лена", "Даша", "Толя", "Надя", "Лика", "Тима", "Кира", "Глеб", "Миша",
			"Люба", "Вера", "Аркаша", "Мария", "Семен", "Света", "Ваня", "Ярик", "Филя"
		};
		for(EditableGroup<?>g:groups)try{
			Input a=g.type.getAnnotation(Input.class);
			NameProvider p=null;
			if(a!=null&&a.nameProvider()!=NameProvider.class)p=a.nameProvider().getDeclaredConstructor().newInstance();
			Verifier v=null;
			if(a!=null&&a.verifier()!=Verifier.class)v=a.verifier().getDeclaredConstructor().newInstance();
			for(int i=0;i<amount;++i){
				Editable e=g.type.getDeclaredConstructor().newInstance();
				for(Field f:g.type.getFields())if(Editable.class.isAssignableFrom(f.getType())&&f.isAnnotationPresent(EditorEntry.class)){
					try{
						EditableGroup<?>fieldGroup=Data.getInstance().getGroup((Class<? extends Editable>)f.getType());
						f.set(e,fieldGroup.get(i%fieldGroup.size()));
					}catch(IllegalArgumentException ex){}
				}
				if(p!=null)e.name=p.provideName(e);
				else if(e instanceof AbstractCustomer)e.name=staticNames[i%staticNames.length];
				else e.name+=" #"+(int)(Math.random()*1000000);
				if(v!=null){
					String verdict=v.verify(e,e,true);
					if(!verdict.isEmpty())System.err.println("WARNING: "+e.name+" of type "+g.type.getName()+" is invalid. Verifier verdict: "+verdict);
				}
				g.add(e);
			}
		}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
	}
}