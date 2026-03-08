package com.bpa4j.util.codegen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;

import com.bpa4j.core.Root;
import com.bpa4j.ui.Message;
import com.bpa4j.util.codegen.ProjectGraph.EditableNode;
import com.bpa4j.util.codegen.ProjectGraph.ProjectNode;
import com.bpa4j.util.codegen.ProjectGraph.EditableNode.Property;
import com.bpa4j.util.codegen.ProjectGraph.EditableNode.Property.PropertyType;

public class TaskAnalyzer{
	public static interface AnalysisResult{
		public boolean checkCompletion(ProjectGraph project);
		public boolean generate(String prompt,ProjectGraph project);
	}
	public static class NewObjectResult implements AnalysisResult{
		public ArrayList<Property>properties;
		public String objectName;
		public NewObjectResult(String objectName,Property...properties){
			this.objectName=objectName;
			this.properties=new ArrayList<>(Arrays.asList(properties));
		}
		public boolean generate(String prompt,ProjectGraph project){
			if(prompt.isBlank())return false;
			if(objectName.isEmpty()){
				String[]p=prompt.split("\s,?*");
				if(p.length<2)return false;
				project.createEditableNode(p[0],p[1],properties.toArray(new Property[0])).location.toPath();
			}else project.createEditableNode(prompt,objectName,properties.toArray(new Property[0])).location.toPath();
			return true;
		}
		public boolean checkCompletion(ProjectGraph project){
			for(ProjectNode n:project.nodes)if(n instanceof EditableNode&&objectName.equalsIgnoreCase(((EditableNode)n).objectName))return true;
			return false;
		}
		public String toString(){
			return(objectName.isEmpty()?"Новый объект [имя, отображаемое имя]":"Новый объект \""+objectName+"\" [имя]")+
				(properties.isEmpty()?" без свойств":" со свойствами:\n"+properties.stream().map(p->p.toString()).collect(Collectors.joining("\n")));
		}
	}
	public static class AddPropertyResult implements AnalysisResult{
		public Property property;
		public String objectName;
		public AddPropertyResult(Property property,String objectName){
			this.property=property;
			this.objectName=objectName;
		}
		private EditableNode findEditable(ProjectGraph project)throws IOException{
			for(ProjectNode n:project.nodes)if(n instanceof EditableNode){
				EditableNode e=(EditableNode)n;
				if(objectName.equalsIgnoreCase(e.objectName))return e;
			}
			throw new FileNotFoundException();
		}
		public boolean generate(String prompt,ProjectGraph project){
			if(prompt.isBlank())return false;
			try{
				File f=findEditable(project).location;
				StringBuilder s=new StringBuilder(Files.readString(f.toPath()));
				s.insert(s.indexOf("extends Editable{")+"extends Editable{".length(),"\n\t\t"+property.getCode(prompt));
				Files.writeString(f.toPath(),s.toString(),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
				return true;
			}catch(IOException ex){
				new Message("Cannot find file with editable \""+objectName+" \"",Color.RED);
				return false;
			}
			//TODO: add property into the file
		}
		public boolean checkCompletion(ProjectGraph project){
			try{
				String s=Files.readString(findEditable(project).location.toPath());
				return s.contains(property.name)&&(property.type==null||s.contains(property.type.toString()));
			}catch(IOException ex){return false;}
		}
		public String toString(){
			return "Добавить свойство \""+property.name+"\" объекту \""+objectName+"\" [название переменной]";
		}
	}
	public static class UndefinedResult implements AnalysisResult{
		private final String text;
		public UndefinedResult(String text){this.text=text.indent(-100);}
		public String toString(){return text;}
		public boolean checkCompletion(ProjectGraph project){return false;}
		public boolean generate(String prompt,ProjectGraph project){
			throw new UnsupportedOperationException("TaskAnalyzer cannot handle such tasks.");
		}
	}
	private String text;
	private final ProjectGraph project;
	private final ArrayList<AnalysisResult>results=new ArrayList<>();
	public TaskAnalyzer(ProjectGraph project,String text){this.project=project;this.text=preprocess(text);}
	public TaskAnalyzer(ProjectGraph project,File f){
		this.project=project;
		try{
			if(f.getName().endsWith(".txt"))text=Files.readString(f.toPath());
			else if(f.getName().endsWith(".pdf"))text=new PDFTextStripper().getText(Loader.loadPDF(f));
			else throw new UnsupportedOperationException();
		}catch(IOException ex){throw new UncheckedIOException(ex);}
		text=preprocess(text);
	}
	private String preprocess(String text){
		return text
			.replace('\u00AB','"')
			.replace('\u00BB','"')
			.replace('\u2014','-')
			.replaceAll("-.?\r?\n","");
	}
	private Property extractProperty(String s,boolean inQuotes){
		PropertyType type=null;
		String lc=s.toLowerCase();
		if(lc.contains("имя"))return null;
		if(lc.contains("время"))type=PropertyType.DATETIME;
		else if(lc.contains("дата"))type=PropertyType.DATE;
		else for(String t:new String[]{"целое","количество","кол-во","цена"})if(lc.contains(t))type=PropertyType.INT;
		if(type==null)for(String t:new String[]{"дробн","веществен","числ"})if(lc.contains(t))type=PropertyType.DOUBLE;
		if(type==null)for(String t:new String[]{"текст","строк","строч","назван","описани"})if(lc.contains(t))type=PropertyType.STRING;
		if(type==null)for(String t:new String[]{"булев","логиче","флаг"," ли "})if(lc.contains(t))type=PropertyType.BOOL;
		return inQuotes?new Property(Pattern.compile("\"(.*?)\"").matcher(lc).results().map(e->e.group(1)).findAny().orElse(""),type)
			:new Property(Pattern.compile("(.*)\\(.*\\)").matcher(lc).results().map(e->e.group(1)).findAny().orElse(""),type);
	}
	private AnalysisResult extractUpgradeAction(String s,String objectName){
		Matcher m=Pattern.compile("добавить.*\"(.*?)\"",Pattern.DOTALL+Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE).matcher(s);
		if(m.find()){
			Property p=extractProperty(s,true);
			return p.name.isEmpty()?new UndefinedResult(s):new AddPropertyResult(p,objectName);
		}else{
			//TODO: extract other upgrade actions
			return new UndefinedResult(s);
		}
	}
	private void processParagraph(String s){
		s=s+"::sub";
		ArrayList<Property>a=new ArrayList<>();
		List<MatchResult>l=Pattern.compile("(?:([a-zA-Z])\\. |::sub)").matcher(s).results().toList();
		Matcher m=Pattern.compile("(?:доработать|дополнить|улучшить).*?\"(.*?)\"",Pattern.DOTALL+Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE).matcher(s);
		if(m.find()){
			Matcher name=Pattern.compile("\"(.*?)\"").matcher(s);
			name.find();
			//TODO: add object completion
			for(int i=0;i<l.size()-1;++i)results.add(extractUpgradeAction(s.substring(l.get(i).end(),l.get(i+1).start()),m.group(1)));
		}else if((m=Pattern.compile("(?:объект|справочник) [^\\.]*?(?:\"(.*)\"|о (.*?)[\\.,\\?!:\\(\\)])",Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE).matcher(s)).find()){
			for(int i=0;i<l.size()-1;++i){
				Property p=extractProperty(s.substring(l.get(i).end(),l.get(i+1).start()),false);
				if(p!=null)a.add(p);
			}
			results.add(new NewObjectResult(m.group(m.group(1)==null?2:1),a.toArray(new Property[0])));
		}else results.add(new UndefinedResult(s));
	}
	@SuppressWarnings("PMD.SystemPrintln")
	public void analyze(){
		String lcText=text.toLowerCase();
		if(!lcText.contains("::paragraph")){
			int t=lcText.indexOf("тестов");
			if(t==-1)text=text+"::paragraph";
			else{
				StringBuilder b=new StringBuilder(text);
				b.insert(t,"::paragraph");
				text=b.toString();
			}
		}
		lcText=text.toLowerCase();
		List<MatchResult>l=Pattern.compile("(?:(\\d+)\\. |::paragraph)").matcher(text).region(lcText.indexOf("техническое задание"),text.length()).results().toList();
		for(int i=0;i<l.size()-1;++i){
			processParagraph(text.substring(l.get(i).end(),l.get(i+1).start()));
			if(l.get(i+1).group(1)==null)break;
		}
		results.sort((r1,r2)->r1 instanceof UndefinedResult?1:-1);
	}
	public void show(){
		JFrame frame=new JFrame("Task analysis");
		frame.setUndecorated(true);
		frame.setSize(Root.SCREEN_SIZE);
		frame.setLayout(new GridLayout());
		JPanel list=new JPanel();
		for(AnalysisResult r:results){
			JPanel p=new JPanel(new BorderLayout());
			p.setBorder(BorderFactory.createLineBorder(r instanceof UndefinedResult?Color.ORANGE:Color.GREEN));
			JTextArea t=new JTextArea(r.toString());
			t.setFocusable(false);
			t.setEditable(false);
			t.setFont(new Font(Font.DIALOG,Font.ITALIC,Root.SCREEN_SIZE.height/40));
			t.setPreferredSize(new Dimension(r.toString().lines().mapToInt(line->t.getFontMetrics(t.getFont()).stringWidth(line)).max().getAsInt(),(int)r.toString().lines().count()*t.getFontMetrics(t.getFont()).getHeight()));
			JScrollPane s=new JScrollPane(t,JScrollPane.VERTICAL_SCROLLBAR_NEVER,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			s.setPreferredSize(new Dimension(Root.SCREEN_SIZE.width,((int)r.toString().lines().count()+1)*t.getFontMetrics(t.getFont()).getHeight()+s.getHorizontalScrollBar().getHeight()));
			p.add(s,BorderLayout.NORTH);
			if(!(r instanceof UndefinedResult)){
				boolean completed=r.checkCompletion(project);
				JPanel input=new JPanel(new GridLayout());
				input.setPreferredSize(new Dimension(Root.SCREEN_SIZE.width,Root.SCREEN_SIZE.height/15));
				JTextField prompt=new JTextField();
				prompt.setFocusable(!completed);
				prompt.setEditable(!completed);
				JButton confirm=new JButton("Сгенерировать");
				if(completed){
					prompt.setBackground(Color.GREEN);
					confirm.setBackground(Color.GREEN);
					confirm.setFocusable(false);
				}else{
					ActionListener a=new ActionListener(){
						public void actionPerformed(ActionEvent e){
							if(r.generate(prompt.getText(),project)){
								prompt.setBackground(Color.GREEN);
								prompt.setFocusable(false);
								prompt.removeActionListener(this);
								confirm.setBackground(Color.GREEN);
								confirm.setFocusable(false);
								confirm.removeActionListener(this);
								input.repaint();
							}
						}
					};
					prompt.addActionListener(a);
					confirm.addActionListener(a);
				}
				input.add(prompt);
				input.add(confirm);
				p.add(input,BorderLayout.SOUTH);
			}
			list.add(p);
		}
		list.setPreferredSize(new Dimension(Root.SCREEN_SIZE.width,Stream.of(list.getComponents()).mapToInt(c->c.getPreferredSize().height+Root.SCREEN_SIZE.height/30).sum()));
		JScrollPane s=new JScrollPane(list,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		s.getVerticalScrollBar().setUnitIncrement(Root.SCREEN_SIZE.height/40);
		frame.add(s);
		list.revalidate();
		frame.setVisible(true);
	}
}
