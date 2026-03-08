package com.bpa4j;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import com.bpa4j.core.Data.Editable;
import com.bpa4j.core.Data.EditableGroup;
import com.bpa4j.core.ProgramStarter;
import com.bpa4j.core.Registrator;
import com.bpa4j.core.Root;
import com.bpa4j.core.User;
import com.bpa4j.core.User.Feature;
import com.bpa4j.core.User.Permission;
import com.bpa4j.core.User.Role;
import com.bpa4j.defaults.editables.AbstractCustomer;
import com.bpa4j.defaults.editables.Processable;
import com.bpa4j.defaults.features.Board;
import com.bpa4j.defaults.features.Calendar;
import com.bpa4j.defaults.features.DatedList;
import com.bpa4j.defaults.features.DefaultFeature;
import com.bpa4j.defaults.features.ItemList;
import com.bpa4j.defaults.features.Report;
import com.bpa4j.defaults.ftr_attributes.data_renderers.AnswerDataRenderer;
import com.bpa4j.defaults.ftr_attributes.data_renderers.ChartDataRenderer;
import com.bpa4j.defaults.ftr_attributes.data_renderers.ChartDataRenderer.ChartMode;
import com.bpa4j.defaults.ftr_attributes.data_renderers.TableDataRenderer;
import com.bpa4j.defaults.ftr_attributes.daters.EventDater;
import com.bpa4j.defaults.ftr_attributes.element_suppliers.ChartDataConverter;
import com.bpa4j.defaults.ftr_attributes.element_suppliers.GroupElementSupplier;
import com.bpa4j.defaults.input.ConditionalWEditor;
import com.bpa4j.defaults.input.FlagWEditor;
import com.bpa4j.defaults.input.FunctionEditor;
import com.bpa4j.editor.EditorEntry;
import com.bpa4j.editor.Input;
import com.bpa4j.editor.ModularEditor;
import com.bpa4j.editor.modules.CustomerModule;
import com.bpa4j.editor.modules.ExcludeModule;
import com.bpa4j.editor.modules.FormModule;
import com.bpa4j.editor.modules.LimitToModule;
import com.bpa4j.editor.modules.LogWatchModule;
import com.bpa4j.editor.modules.StageApprovalModule;
import com.bpa4j.navigation.ImplementedInfo;
import com.bpa4j.navigation.Navigator;
import com.bpa4j.ui.PathIcon;
import com.bpa4j.util.testgen.TestGen;

public final class FullTester{
	public static class MyEditable5 extends Editable{
		public MyEditable5(){
			super("Новый объект");
		}
	}
	public static class MyEditable4 extends Editable{
		static int index=2024;
		@EditorEntry(translation="Группа")
		public String group=new String[]{"С баллом <100","С баллом <200"}[(int)(Math.random()*2)];
		@SuppressWarnings("PMD.UnusedAssignment")
		@EditorEntry(translation="Категория")
		public String value1=String.valueOf(index--);
		@EditorEntry(translation="Значение")
		public int value2=(int)(Math.random()*100);
		public MyEditable4(){super("Новый объект");}
	}
	public static class MyEditable3 extends Editable{
		@EditorEntry(translation="Группа")
		public String group=new String[]{"С баллом <100","С баллом <200"}[(int)(Math.random()*2)];
		@EditorEntry(translation="Величина 1")
		public int value1=(int)(Math.random()*100);
		@EditorEntry(translation="Величина 2")
		public int value2=(int)(Math.random()*100);
		public MyEditable3(){super("Новый объект");}
	}
	public static class MyCustomer extends AbstractCustomer{}
	public static class MyEditable2 extends Editable{
		public static enum Status{
			S1("Состояние 1"),
			S2("Состояние 2"),
			S3("Состояние 3"),
			S4("Состояние 4");
			private final String translation;
			private Status(String translation){this.translation=translation;}
			public String toString(){return translation;}
		}
		@EditorEntry(translation="Перечисление")
		public Status a=Status.S1;
		@EditorEntry(translation="Группа",editorBaseSource=FlagWEditor.class)
		public EditableGroup<MyEditable3>group=new EditableGroup<MyEditable3>(
			new PathIcon("ui/left.png",Root.SCREEN_SIZE.height/11,Root.SCREEN_SIZE.height/11),
			new PathIcon("ui/right.png",Root.SCREEN_SIZE.height/11,Root.SCREEN_SIZE.height/11),
			MyEditable3.class
		);
		public MyEditable2(){
			super("Новый объект");
		}
	}
	@Input(completer=MyProcessable.Completer.class,nameProvider=MyProcessable.NameProvider.class)
	public static class MyProcessable extends Processable{
		public static class Completer implements com.bpa4j.editor.Completer{
			public boolean isCompletable(Editable original,Editable editable,int fieldsEdited){
				return fieldsEdited>=2;
			}
			public void completeObject(Editable original,Editable editable,int fieldsEdited){
				MyProcessable e=(MyProcessable)editable;
				e.doubleField=e.intField*2;
			}
		}
		public static class NameProvider implements com.bpa4j.editor.NameProvider{
			public String provideName(Editable editable){
				return editable.name+"1";
			}
		}
		@EditorEntry(translation="Строка",editorBaseSource = ConditionalWEditor.class)
		public String strField;
		static{
			try{
				ConditionalWEditor.configure(MyProcessable.class.getField("strField"),null,e->Math.random()>0.4);
			}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
		}
		@EditorEntry(translation="Число")
		public int intField;
		@EditorEntry(translation="Дробное число",properties = {"readonly"})
		public double doubleField;
		@EditorEntry(translation="Дата",properties = {"initonly"})
		public LocalDate dateField=LocalDate.now();
		@EditorEntry(translation="Редактируемый объект")
		public MyEditable2 editableField;
		@EditorEntry(translation="Supplier",editorBaseSource=FunctionEditor.class)
		public static Supplier<String>s=()->{
			return Stream.generate(()->(char)('a'+Math.random()*25)).limit(20).map(c->String.valueOf(c)).reduce((c1,c2)->c1+c2).get();
		};
		public MyProcessable(){
			super(
				"Новый объект",
				new Stage("Отрицание",AppPermission.MANAGE_PROCESSABLE,e->{
					Wrapper<Boolean>w=new Wrapper<Boolean>(true);
					ProgramStarter.editor.constructEditor(new MyEditable2(),true,()->w.var=false);
					return w.var;
				}),
				new Stage("Гнев",AppPermission.MANAGE_PROCESSABLE,AppPermission.MANAGE_PROCESSABLE,0),
				new Stage("Торг",AppPermission.MANAGE_PROCESSABLE,AppPermission.MANAGE_PROCESSABLE,0),
				new Stage("Депрессия",AppPermission.MANAGE_PROCESSABLE,AppPermission.MANAGE_PROCESSABLE,0),
				new Stage("Принятие",AppPermission.MANAGE_PROCESSABLE,AppPermission.MANAGE_PROCESSABLE,0)
			);
		}
		public List<ImplementedInfo>getImplementedInfo(){
			ImplementedInfo info=new ImplementedInfo("Пункт ТЗ здесь")
				.appendComment("Всё, вы нашли MyProcessable.")
				.appendText("Порадуйтесь.");
			return List.of(info);
		}
	}
	public static class MyEvent implements Calendar.Event{
		@EditorEntry(translation="Название")
		public String property1="НТО";
		@EditorEntry(translation="Этап")
		public String property2=new String[]{"1 отборочный","2 отборочный","Финал"}[(int)(Math.random()*3)];
		public String toString(){return "Олимпиада";}
	}
	public static class MyDater implements Dater<MyProcessable>{
		public JComponent apply(MyProcessable t,LocalDate u){
			return new JSpinner();
		}
	}
	public enum AppRole implements Role{
		MYROLE(
			()->AppPermission.values(),
			()->new Feature[]{
				DefaultFeature.MODEL_EDITING,
				Board.registerBoard("board",MyProcessable.class),
				Calendar.registerCalendar("calendar",MyEvent.class),
				Report.registerReport("report"),
				ItemList.registerList("list",MyProcessable.class),
				DatedList.registerList("dlist",MyProcessable.class)
			}
		);
		private AppRole(Supplier<Permission[]>p,Supplier<Feature[]>f){SwingUtilities.invokeLater(()->Registrator.register(this,f.get(),p.get()));}
	}
	public enum AppPermission implements Permission{
		READ_MYEDITABLE5,
		CREATE_MYEDITABLE5,
		READ_MYPROCESSABLE,
		CREATE_MYPROCESSABLE,
		READ_MYTHIRDEDITABLE,
		CREATE_MYTHIRDEDITABLE,
		READ_MYFOURTHEDITABLE,
		CREATE_MYFOURTHEDITABLE,
		READ_MYCUSTOMER,
		CREATE_MYCUSTOMER,
		MANAGE_PROCESSABLE;
		private AppPermission(){Registrator.register(this);}
	}
	private FullTester(){}
	public static void main(String[]args)throws ReflectiveOperationException,URISyntaxException{
		new com.bpa4j.util.codegen.ProjectGraph(new java.io.File("C:/Users/user/Desktop/IT/Java/1C/NTO training/team/T1/NTO_TRAINING/src/main/java")).show();
		Navigator.init();
		ProgramStarter.welcomeMessage="";
		ProgramStarter.authRequired=false;
		ProgramStarter.editor=new ModularEditor(
			new LimitToModule(new StageApprovalModule(),MyProcessable.class),
			new LimitToModule(new LogWatchModule(),MyProcessable.class),
			new LimitToModule(new CustomerModule(),MyCustomer.class),
			new ExcludeModule(new FormModule(),MyProcessable.class,MyCustomer.class)
		);
		if(ProgramStarter.firstLaunch){
			User.register("Пользователь 1","",AppRole.MYROLE);
			EditableGroup<MyProcessable>myProcessables=new EditableGroup<>(
				new PathIcon("ui/left.png",Root.SCREEN_SIZE.height/11,Root.SCREEN_SIZE.height/11),
				new PathIcon("ui/right.png",Root.SCREEN_SIZE.height/11,Root.SCREEN_SIZE.height/11),
				MyProcessable.class
			);
			EditableGroup<MyEditable3>myThirdEditables=new EditableGroup<>(
				new PathIcon("ui/left.png",Root.SCREEN_SIZE.height/11,Root.SCREEN_SIZE.height/11),
				new PathIcon("ui/right.png",Root.SCREEN_SIZE.height/11,Root.SCREEN_SIZE.height/11),
				MyEditable3.class
			);
			EditableGroup<MyEditable4>myFourthEditables=new EditableGroup<>(
				new PathIcon("ui/left.png",Root.SCREEN_SIZE.height/11,Root.SCREEN_SIZE.height/11),
				new PathIcon("ui/right.png",Root.SCREEN_SIZE.height/11,Root.SCREEN_SIZE.height/11),
				MyEditable4.class
			);
			EditableGroup<MyCustomer>customers=new EditableGroup<>(
				new PathIcon("ui/left.png",Root.SCREEN_SIZE.height/11,Root.SCREEN_SIZE.height/11),
				new PathIcon("ui/right.png",Root.SCREEN_SIZE.height/11,Root.SCREEN_SIZE.height/11),
				MyCustomer.class
			);
			EditableGroup<MyEditable5>myEditables5=new EditableGroup<>(
				new PathIcon("ui/left.png",Root.SCREEN_SIZE.height/11,Root.SCREEN_SIZE.height/11),
				new PathIcon("ui/right.png",Root.SCREEN_SIZE.height/11,Root.SCREEN_SIZE.height/11),
				MyEditable5.class
			);
			Registrator.register(myProcessables,myThirdEditables.hide(),myFourthEditables.hide(),customers,myEditables5);
			ProgramStarter.runProgram();
			TestGen.generate(4,myProcessables);
			TestGen.generate(50,myThirdEditables,myFourthEditables);
		}else ProgramStarter.runProgram();
		GroupElementSupplier<MyProcessable>groupES=new GroupElementSupplier<>(MyProcessable.class);
		Board.<MyProcessable>getBoard("board").setSorter(new Board.Sorter<MyProcessable>(){
			private final JComboBox<Boolean>c=new JComboBox<>();
			public JComponent getConfigurator(Runnable saver,ArrayList<MyProcessable>objects){
				c.removeAllItems();
				c.addItem(true);
				c.addItem(false);
				c.addItemListener(e->saver.run());
				return c;
			}
			public int compare(MyProcessable o1,MyProcessable o2){
				try{
					return(c.getSelectedItem()==null||(boolean)c.getSelectedItem())?o1.name.length()-o2.name.length():o1.strField.length()-o2.strField.length();
				}catch(NullPointerException ex){return 0;}
			}
		}).setElementSupplier(groupES).setAllowCreation(true);
		Calendar.<MyEvent>getCalendar("calendar")
			.setEventFiller(m->{
				for(int i=0;i<10;++i){
					LocalDate t=LocalDate.now().plusDays((int)(Math.random()*15));
					if(m.containsKey(t))m.get(t).add(new MyEvent());
					else m.put(t,new ArrayList<>(Stream.of(new MyEvent()).toList()));
				}
			})
			.setDater(new EventDater<>(EventDater::listProperties));
		JSpinner s=new JSpinner(new SpinnerNumberModel(50,10,100,1));
		Report.getReport("report")
			.addConfigurator(saver->{
				s.addChangeListener(e->saver.run());
				return s;
			}).addDataRenderer(new ChartDataRenderer(
				ChartMode.LINEAR_COMPARE,
				new ChartDataConverter<>(()->new ArrayList<>(new GroupElementSupplier<>(MyEditable3.class).get().stream().filter(e->e.value1<(int)s.getValue()).toList())),
				"Координаты смертей после резов",
				"x",
				"y"
			)).addDataRenderer(new ChartDataRenderer(
				ChartMode.BAR,
				new ChartDataConverter<>(new GroupElementSupplier<>(MyEditable4.class)),
				"Смерти после резов",
				"Год",
				"Количество, млн. чел"
			)).addDataRenderer(new AnswerDataRenderer(
				()->"ЧЕКЛИСТ НТО.",
				()->"Проблемы: есть",
				()->"Паника: есть",
				()->"Кровь: "+((int)s.getValue()>50?"есть "+((int)s.getValue()-50)+" литров":"нет (но это временно)")
			)).addDataRenderer(new TableDataRenderer<>(groupES,"Данные о жертвах НТО",true));
		ItemList.<MyProcessable>getList("list")
			.setElementSupplier(new GroupElementSupplier<>(MyProcessable.class))
			.addCollectiveAction(a->{
				for(MyProcessable p:a)p.toString(); //Does nothing
			});
		DatedList.<MyProcessable>getList("dlist").setDateProvider(()->new MyDater());
		FlagWEditor.configure(MyEditable2.class.getField("group"),null,null,()->new EditableGroup<MyEditable3>(
			new PathIcon("ui/product.png",Root.SCREEN_SIZE.height/11,Root.SCREEN_SIZE.height/11),
			new PathIcon("ui/product_add.png",Root.SCREEN_SIZE.height/11,Root.SCREEN_SIZE.height/11),
			MyEditable3.class
		));
	}
}
