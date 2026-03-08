package com.bpa4j.util.codegen;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.stream.Stream;

import com.bpa4j.util.codegen.ProjectGraph.ProjectNode;

public class NavigatorNode extends ProjectNode{
	public static class Instruction{
		public static enum Type{
			START,
			FEATURE,
			TEXT,
			COMMENT;
			public char toChar(){
				return switch(this){
					case START->'s';
					case FEATURE->'f';
					case TEXT->'t';
					case COMMENT->'c';
					default->throw new AssertionError("This method does not know other constants.");
				};
			}
			public static Type toType(char c){
				return switch(c){
					case 's'->Instruction.Type.START;
					case 'f'->Instruction.Type.FEATURE;
					case 't'->Instruction.Type.TEXT;
					case 'c'->Instruction.Type.COMMENT;
					default->throw new IllegalArgumentException("Char '"+c+"' does not correspond to any constant.");
				};
			}
		}
		public String text;
		public Type type;
		public Instruction(String text,Type type){this.text=text;this.type=type;}
	}
	public static class HelpEntry{
		public String text;
		public ArrayList<Instruction>instructions=new ArrayList<>();
		public HelpEntry(String text){this.text=text;}
		public void changeText(String text,NavigatorNode n){
			try{
				StringBuilder b=new StringBuilder();
				for(String l:Files.readString(n.location.toPath()).split("\n")){
					String[]s=l.split(" ",2);
					if(s[1].equals(this.text))s[1]=text;
					b.append(s[0]).append(' ').append(s[1]).append('\n');
				}
				Files.writeString(n.location.toPath(),b);
				this.text=text;
			}catch(IOException ex){throw new UncheckedIOException(ex);}
		}
		public void replaceInstruction(Instruction c,int index,NavigatorNode n){
			try{
				StringBuilder b=new StringBuilder();
				for(String l:Files.readString(n.location.toPath()).split("\n")){
					String[]s=l.split(" ",2);
					if(text.equals(s[1])){
						String[]t=s[0].split("\\.");
						int i=0;
						for(;i<index;++i)b.append(t[i]).append('.');
						b.append(c.type.toChar()).append(c.text).append('.');
						++i;
						for(;i<t.length;++i)b.append(t[i]).append('.');
						b.deleteCharAt(b.length()-1);
					}else b.append(s[0]);
					b.append(' ').append(s[1]).append('\n');
				}
				Files.writeString(n.location.toPath(),b);
				instructions.set(index,c);
			}catch(IOException ex){throw new UncheckedIOException(ex);}
		}
		public void appendInstruction(Instruction c,NavigatorNode n){
			try{
				StringBuilder b=new StringBuilder();
				for(String l:Files.readString(n.location.toPath()).split("\n")){
					String[]s=l.split(" ",2);
					b.append(s[0]);
					if(s[1].equals(text))b.append('.').append(c.type.toChar()).append(c.text);
					b.append(' ').append(s[1]).append('\n');
				}
				Files.writeString(n.location.toPath(),b);
				instructions.add(c);
			}catch(IOException ex){throw new UncheckedIOException(ex);}
		}
		public void deleteLastInstruction(NavigatorNode n){
			try{
				StringBuilder b=new StringBuilder();
				for(String l:Files.readString(n.location.toPath()).split("\n")){
					String[]s=l.split(" ",2);
					if(text.equals(s[1])){
						String[]t=s[0].split("\\.");
						for(int j=0;j<t.length-1;++j)b.append(t[j]);
					}else b.append(s[0]);
					if(!(b.isEmpty()||Character.isWhitespace(b.charAt(b.length()-1))))b.append(' ').append(s[1]).append('\n');
				}
				Files.writeString(n.location.toPath(),b);
				instructions.removeLast();
			}catch(IOException ex){throw new UncheckedIOException(ex);}
		}
	}
	public ArrayList<HelpEntry>entries=new ArrayList<>();
	public NavigatorNode(File file){
		super(file);
		try{
			String str=Files.readString(file.toPath());
			if(str.isBlank())return;
			for(String l:str.split("\n")){
				String[]s=l.split(" ",2);
				HelpEntry e=new HelpEntry(s[1]);
				e.instructions.addAll(Stream.of(s[0].split("\\.")).map(t->new Instruction(t.substring(1),Instruction.Type.toType(t.charAt(0)))).toList());
				entries.add(e);
			}
		}catch(IOException ex){throw new UncheckedIOException(ex);}
	}
	/**
	 * Creates a new NavigatorNode.
	 */
	public NavigatorNode(ProjectGraph project){
		super(new File(project.projectFolder,"resources/helppath.cfg"));
		try{location.createNewFile();}catch(IOException ex){throw new UncheckedIOException(ex);}
	}
	public void deleteEntry(String text){
		HelpEntry e=entries.stream().filter(entry->entry.text.equals(text)).findAny().get();
		try{
			StringBuilder b=new StringBuilder();
			for(String l:Files.readString(location.toPath()).split("\n")){
				String[]s=l.split(" ",2);
				if(!s[1].equals(e.text))b.append(s[0]).append(' ').append(s[1]).append('\n');
			}
			Files.writeString(location.toPath(),b);
			entries.remove(e);
		}catch(IOException ex){throw new UncheckedIOException(ex);}
	}
}