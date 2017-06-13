
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;






/**
 * CS 267 - Project - Implements create index, drop index, list table, and
 * exploit the index in select statements.
 */
public class DBMS {
	private static final String COMMAND_FILE_LOC = "Commands.txt";
	private static final String OUTPUT_FILE_LOC = "Output.txt";

	private static final String TABLE_FOLDER_NAME = "tables";
	private static final String TABLE_FILE_EXT = ".tab";
	private static final String INDEX_FILE_EXT = ".idx";

	private DbmsPrinter out;
	private ArrayList<Table> tables;
    private String tableName;
	public DBMS() {
		tables = new ArrayList<Table>();
	}

	/**
	 * Main method to run the DBMS engine.
	 * 
	 * @param args
	 *            arg[0] is input file, arg[1] is output file.
	 */
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		DBMS db = new DBMS();
		db.out = new DbmsPrinter();
		Scanner in = null;
		
		try {
			// set input file
			if (args.length > 0) {
				in = new Scanner(new File(args[0]));
			} else {  
				in = new Scanner(new File(COMMAND_FILE_LOC));
			}

			// set output files
			if (args.length > 1) {
				db.out.addPrinter(args[1]);
			} else {
				db.out.addPrinter(OUTPUT_FILE_LOC);
			}

			// Load data to memory
			db.loadTables();

			// Go through each line in the Command.txt file
			while (in.hasNextLine()) {
			

				String sql = in.nextLine();
				
				StringTokenizer tokenizer = new StringTokenizer(sql);

				// Evaluate the SQL statement
				if (tokenizer.hasMoreTokens()) {
					String command = tokenizer.nextToken();
					if (command.equalsIgnoreCase("CREATE")) {
						if (tokenizer.hasMoreTokens()) {
							command = tokenizer.nextToken();
							if (command.equalsIgnoreCase("TABLE")) {
								db.createTable(sql, tokenizer);
							} else if (command.equalsIgnoreCase("INDEX")) {
								// TODO your PART 1 code goes here
								
								db.createIndex(sql, tokenizer,false);
								
							}else if (command.equalsIgnoreCase("UNIQUE")|| command.equalsIgnoreCase("[UNIQUE]")) {
								
								// TODO your PART 1 code goes here
								tokenizer.nextToken();
								db.createIndex(sql, tokenizer,true);
								for (Table table : db.tables) {
									db.storeTableFile(table);
								}
							} else {
								throw new DbmsError("Invalid CREATE " + command
										+ " statement. '" + sql + "'.");
							}
						} else {
							throw new DbmsError("Invalid CREATE statement. '"
									+ sql + "'.");
						}
					} else if (command.equalsIgnoreCase("INSERT")) {
						

						db.insertInto(sql, tokenizer);
					} else if (command.equalsIgnoreCase("DROP")) {
						if (tokenizer.hasMoreTokens()) {
							command = tokenizer.nextToken();
							if (command.equalsIgnoreCase("TABLE")) {
								db.dropTable(sql, tokenizer);
							} else if (command.equalsIgnoreCase("INDEX")) {
								// TODO your PART 1 code goes here
								db.dropIndex(sql,tokenizer);
								for (Table table : db.tables) {
									db.storeTableFile(table);
								}
								//db.loadTables();
							} else {
								throw new DbmsError("Invalid DROP " + command
										+ " statement. '" + sql + "'.");
							}
						} else {
							throw new DbmsError("Invalid DROP statement. '"
									+ sql + "'.");
						}
					} else if (command.equalsIgnoreCase("RUNSTATS")) {
						// TODO your PART 1 code goes here

						// TODO replace the table name below with the table name
						// in the command to print the RUNSTATS output
						String name=db.computeRunstats(sql, tokenizer);
						db.printRunstats(name.toUpperCase());
						for (Table table : db.tables) {
							db.storeTableFile(table);
						}
					} else if (command.equalsIgnoreCase("SELECT")) {
						// TODO your PART 2 code goes here
						db.select(sql,tokenizer);
					} else if (command.equalsIgnoreCase("--")) {
						// Ignore this command as a comment
					} else if (command.equalsIgnoreCase("COMMIT")) {
						try {
							// Check for ";"
							if (!tokenizer.nextElement().equals(";")) {
								throw new NoSuchElementException();
							}

							// Check if there are more tokens
							if (tokenizer.hasMoreTokens()) {
								throw new NoSuchElementException();
							}

							// Save tables to files
							for (Table table : db.tables) {
								db.storeTableFile(table);
							}
						} catch (NoSuchElementException ex) {
							throw new DbmsError("Invalid COMMIT statement. '"
									+ sql + "'.");
						}
					} else {
						throw new DbmsError("Invalid statement. '" + sql + "'.");
					}
				}
			}

			// Save tables to files
			for (Table table : db.tables) {
				db.storeTableFile(table);
			}
		} catch (DbmsError ex) {
			db.out.println("DBMS ERROR:  " + ex.getMessage());
			ex.printStackTrace();
		} catch (Exception ex) {
			db.out.println("JAVA ERROR:  " + ex.getMessage());
			ex.printStackTrace();
		} finally {
			// clean up
			try {
				in.close();
			} catch (Exception ex) {
			}

			try {
				db.out.cleanup();
			} catch (Exception ex) {
			}
		}
	}

	/**
	 * Loads tables to memory
	 * 
	 * @throws Exception
	 */
	private void loadTables() throws Exception {
		// Get all the available tables in the "tables" directory
		File tableDir = new File(TABLE_FOLDER_NAME);
		if (tableDir.exists() && tableDir.isDirectory()) {
			for (File tableFile : tableDir.listFiles()) {
				// For each file check if the file extension is ".tab"
				String tableName = tableFile.getName();
				int periodLoc = tableName.lastIndexOf(".");
				String tableFileExt = tableName.substring(tableName
						.lastIndexOf(".") + 1);
				if (tableFileExt.equalsIgnoreCase("tab")) {
					// If it is a ".tab" file, create a table structure
					Table table = new Table(tableName.substring(0, periodLoc));
					Scanner in = new Scanner(tableFile);

					try {
						// Read the file to get Column definitions
						int numCols = Integer.parseInt(in.nextLine());
                      
						for (int i = 0; i < numCols; i++) {
							StringTokenizer tokenizer = new StringTokenizer(
									in.nextLine());
							String name = tokenizer.nextToken();
							String type = tokenizer.nextToken();
							boolean nullable = Boolean.parseBoolean(tokenizer
									.nextToken());
							switch (type.charAt(0)) {
							case 'C':
								table.addColumn(new Column(i + 1, name,
										Column.ColType.CHAR, Integer
												.parseInt(type.substring(1)),
										nullable));
								break;
							case 'I':
								table.addColumn(new Column(i + 1, name,
										Column.ColType.INT, 4, nullable));
								break;
							default:
								break;
							}
						}

						// Read the file for index definitions
						int numIdx = Integer.parseInt(in.nextLine());
						for (int i = 0; i < numIdx; i++) {
							StringTokenizer tokenizer = new StringTokenizer(
									in.nextLine());
							Index index = new Index(tokenizer.nextToken());
							index.setIsUnique(Boolean.parseBoolean(tokenizer
									.nextToken()));

							int idxColPos = 1;
							while (tokenizer.hasMoreTokens()) {
								String colDef = tokenizer.nextToken();
								Index.IndexKeyDef def = index.new IndexKeyDef();
								def.idxColPos = idxColPos;
								def.colId = Integer.parseInt(colDef.substring(
										0, colDef.length() - 1));
								switch (colDef.charAt(colDef.length() - 1)) {
								case 'A':
									def.descOrder = false;
									break;
								case 'D':
									def.descOrder = true;
									break;
								default:
									break;
								}

								index.addIdxKey(def);
								idxColPos++;
							}

							table.addIndex(index);
							loadIndex(table, index);
						}

						// Read the data from the file
						int numRows = Integer.parseInt(in.nextLine());
						for (int i = 0; i < numRows; i++) {
							table.addData(in.nextLine());
						}
						
						// Read RUNSTATS from the file
						while(in.hasNextLine()) {
							String line = in.nextLine();
							StringTokenizer toks = new StringTokenizer(line);
							if(toks.nextToken().equals("STATS")) {
								String stats = toks.nextToken();
								if(stats.equals("TABCARD")) {
									table.setTableCard(Integer.parseInt(toks.nextToken()));
								} else if (stats.equals("COLCARD")) {
									Column col = table.getColumns().get(Integer.parseInt(toks.nextToken()));
									col.setColCard(Integer.parseInt(toks.nextToken()));
									col.setHiKey(toks.nextToken());
									col.setLoKey(toks.nextToken());
								} else {
									throw new DbmsError("Invalid STATS.");
								}
							} else {
								throw new DbmsError("Invalid STATS.");
							}
						}
					} catch (DbmsError ex) {
						throw ex;
					} catch (Exception ex) {
						throw new DbmsError("Invalid table file format.");
					} finally {
						in.close();
					}
					tables.add(table);
				}
			}
		} else {
			throw new FileNotFoundException(
					"The system cannot find the tables directory specified.");
		}
	}

	/**
	 * Loads specified table to memory
	 * 
	 * @throws DbmsError
	 */
	private void loadIndex(Table table, Index index) throws DbmsError {
		try {
			Scanner in = new Scanner(new File(TABLE_FOLDER_NAME,
					table.getTableName() + index.getIdxName() + INDEX_FILE_EXT));
			String def = in.nextLine();
			String rows = in.nextLine();

			while (in.hasNext()) {
				String line = in.nextLine();
				Index.IndexKeyVal val = index.new IndexKeyVal();
				val.rid = Integer.parseInt(new StringTokenizer(line)
						.nextToken());
				val.value = line.substring(line.indexOf("'") + 1,
						line.lastIndexOf("'"));
				index.addKey(val);
			}
			in.close();
		} catch (Exception ex) {
			throw new DbmsError("Invalid index file format.");
		}
	}

	/**
	 * CREATE TABLE
	 * <table name>
	 * ( <col name> < CHAR ( length ) | INT > <NOT NULL> ) ;
	 * 
	 * @param sql
	 * @param tokenizer
	 * @throws Exception
	 */
	private void createTable(String sql, StringTokenizer tokenizer)
			throws Exception {
		try {
			// Check the table name
			String tok = tokenizer.nextToken().toUpperCase();
			if (Character.isAlphabetic(tok.charAt(0))) {
				// Check if the table already exists
				for (Table tab : tables) {
					if (tab.getTableName().equals(tok) && !tab.delete) {
						throw new DbmsError("Table " + tok
								+ "already exists. '" + sql + "'.");
					}
				}

				// Create a table instance to store data in memory
				Table table = new Table(tok.toUpperCase());

				// Check for '('
				tok = tokenizer.nextToken();
				if (tok.equals("(")) {
					// Look through the column definitions and add them to the
					// table in memory
					boolean done = false;
					int colId = 1;
					while (!done) {
						tok = tokenizer.nextToken();
						if (Character.isAlphabetic(tok.charAt(0))) {
							String colName = tok;
							Column.ColType colType = Column.ColType.INT;
							int colLength = 4;
							boolean nullable = true;

							tok = tokenizer.nextToken();
							if (tok.equalsIgnoreCase("INT")) {
								// use the default Column.ColType and colLength

								// Look for NOT NULL or ',' or ')'
								tok = tokenizer.nextToken();
								if (tok.equalsIgnoreCase("NOT")) {
									// look for NULL after NOT
									tok = tokenizer.nextToken();
									if (tok.equalsIgnoreCase("NULL")) {
										nullable = false;
									} else {
										throw new NoSuchElementException();
									}

									tok = tokenizer.nextToken();
									if (tok.equals(",")) {
										// Continue to the next column
									} else if (tok.equalsIgnoreCase(")")) {
										done = true;
									} else {
										throw new NoSuchElementException();
									}
								} else if (tok.equalsIgnoreCase(",")) {
									// Continue to the next column
								} else if (tok.equalsIgnoreCase(")")) {
									done = true;
								} else {
									throw new NoSuchElementException();
								}
							} else if (tok.equalsIgnoreCase("CHAR")) {
								colType = Column.ColType.CHAR;

								// Look for column length
								tok = tokenizer.nextToken();
								if (tok.equals("(")) {
									tok = tokenizer.nextToken();
									try {
										colLength = Integer.parseInt(tok);
									} catch (NumberFormatException ex) {
										throw new DbmsError(
												"Invalid table column length for "
														+ colName + ". '" + sql
														+ "'.");
									}

									// Check for the closing ')'
									tok = tokenizer.nextToken();
									if (!tok.equals(")")) {
										throw new DbmsError(
												"Invalid table column definition for "
														+ colName + ". '" + sql
														+ "'.");
									}

									// Look for NOT NULL or ',' or ')'
									tok = tokenizer.nextToken();
									if (tok.equalsIgnoreCase("NOT")) {
										// Look for NULL after NOT
										tok = tokenizer.nextToken();
										if (tok.equalsIgnoreCase("NULL")) {
											nullable = false;

											tok = tokenizer.nextToken();
											if (tok.equals(",")) {
												// Continue to the next column
											} else if (tok
													.equalsIgnoreCase(")")) {
												done = true;
											} else {
												throw new NoSuchElementException();
											}
										} else {
											throw new NoSuchElementException();
										}
									} else if (tok.equalsIgnoreCase(",")) {
										// Continue to the next column
									} else if (tok.equalsIgnoreCase(")")) {
										done = true;
									} else {
										throw new NoSuchElementException();
									}
								} else {
									throw new DbmsError(
											"Invalid table column definition for "
													+ colName + ". '" + sql
													+ "'.");
								}
							} else {
								throw new NoSuchElementException();
							}

							// Everything is ok. Add the column to the table
							table.addColumn(new Column(colId, colName, colType,
									colLength, nullable));
							colId++;
						} else {
							// if(colId == 1) {
							throw new DbmsError(
									"Invalid table column identifier " + tok
											+ ". '" + sql + "'.");
							// }
						}
					}

					// Check for the semicolon
					tok = tokenizer.nextToken();
					if (!tok.equals(";")) {
						throw new NoSuchElementException();
					}

					// Check if there are more tokens
					if (tokenizer.hasMoreTokens()) {
						throw new NoSuchElementException();
					}

					if (table.getNumColumns() == 0) {
						throw new DbmsError(
								"No column descriptions specified. '" + sql
										+ "'.");
					}

					// The table is stored into memory when this program exists.
					tables.add(table);

					out.println("Table " + table.getTableName()
							+ " was created.");
				} else {
					throw new NoSuchElementException();
				}
			} else {
				throw new DbmsError("Invalid table identifier " + tok + ". '"
						+ sql + "'.");
			}
		} catch (NoSuchElementException ex) {
			throw new DbmsError("Invalid CREATE TABLE statement. '" + sql
					+ "'.");
		}
	}

	/**
	 * INSERT INTO
	 * <table name>
	 * VALUES ( val1 , val2, .... ) ;
	 * 
	 * @param sql
	 * @param tokenizer
	 * @throws Exception
	 */
	private int checkUnique(String sql, Table tab)
	{
		int res=0,j=0;
		StringTokenizer tokenizer= new StringTokenizer(sql);
		String tok=null;
		tok=tokenizer.nextToken();
	
		while(true)
		{
			tok=tokenizer.nextToken();
		if(tok.equals("("))
			break;
		}
		String value="";
		for(Index in : tab.getIndexes() )
		{
			
			if(!in.delete)
			{
			if(res!=-1)
			{
			Index.IndexKeyDef def = in.new IndexKeyDef();
			if(in.getIsUnique())
			{
				for(j=0;j<in.getIdxKey().size();j++)
				{
					//form Index Key for new value to be inserted
					def=in.getIdxKey().get(j);
					
					String data=String.format("%1s", sql.substring(sql.indexOf("("))).replace(",", " ");
					String[] insertTokens= data.split("\\s+");
					value=value+insertTokens[def.colId];
				}
					value=value.trim();
					//Get Table Data 
					ArrayList<String> listData= tab.getData() ;
					ArrayList<String> tableList= new ArrayList<String>();
					for(int m=0;m<listData.size();m++)
					{
						String tableValue="";
						//Parse Table Data to for existing index keys 
						for(int n=0;n<in.getIdxKey().size();n++)
						{
							def=in.getIdxKey().get(n);
							String[] tokens=listData.get(m).split("\\s+");
						
							tableValue=tableValue+tokens[def.colId+1];
							
							
						}
						
						tableList.add(tableValue);
					if(tableList.contains(value))
					{
						res=-1;
						
						break;
					}	
					}
				
			}
			}
			else
				break;
			}
		}
		
		return res;
	}
	private void insertInto(String sql, StringTokenizer tokenizer)
			throws Exception {
		try {
			int res =0;
			String tok = tokenizer.nextToken();

			if (tok.equalsIgnoreCase("INTO")) {
				tok = tokenizer.nextToken().trim().toUpperCase();
				Table table = null;
				for (Table tab : tables) {
					if (tab.getTableName().equals(tok)) {
						table = tab;
						break;
					}
				}

				if (table == null) {
					throw new DbmsError("Table " + tok + " does not exist.");
				}
				
                res=checkUnique(sql,table);
                if(res==-1)
                	throw new DbmsError("Error: Cannot insert duplicate values.");
               
				tok = tokenizer.nextToken();
				if (tok.equalsIgnoreCase("VALUES")) {
					tok = tokenizer.nextToken();
					if (tok.equalsIgnoreCase("(")) {
						tok = tokenizer.nextToken();
						String values = String.format("%3s", table.getData()
								.size() + 1)
								+ " ";
						int colId = 0;
						boolean done = false;
						while (!done) {
							if (tok.equals(")")) {
								done = true;
								break;
							} else if (tok.equals(",")) {
								// Continue to the next value
							} else {
								if (colId == table.getNumColumns()) {
									throw new DbmsError(
											"Invalid number of values were given.");
								}

								Column col = table.getColumns().get(colId);

								if (tok.equals("-") && !col.isColNullable()) {
									throw new DbmsError(
											"A NOT NULL column cannot have null. '"
													+ sql + "'.");
								}

								if (col.getColType() == Column.ColType.INT) {
									try {
										if(!tok.equals("-")) {
											int temp = Integer.parseInt(tok);
										}
									} catch (Exception ex) {
										throw new DbmsError(
												"An INT column cannot hold a CHAR. '"
														+ sql + "'.");
									}

									tok = String.format("%10s", tok.trim());
								} else if (col.getColType() == Column.ColType.CHAR) {
									int length = tok.length();
									if (length > col.getColLength()) {
										throw new DbmsError(
												"A CHAR column cannot exceede its length. '"
														+ sql + "'.");
									}

									tok = String.format(
											"%-" + col.getColLength() + "s",
											tok.trim());
								}

								values += tok + " ";
								colId++;
							}
							tok = tokenizer.nextToken().trim();
						}

						if (colId != table.getNumColumns()) {
							throw new DbmsError(
									"Invalid number of values were given.");
						}

						// Check for the semicolon
						tok = tokenizer.nextToken();
						if (!tok.equals(";")) {
							throw new NoSuchElementException();
						}

						// Check if there are more tokens
						if (tokenizer.hasMoreTokens()) {
							throw new NoSuchElementException();
						}

						// insert the value to table
						table.addData(values);
						updateIndex(sql,table);
						out.println("One line was saved to the table. "
								+ table.getTableName() + ": " + values);
					} else {
						throw new NoSuchElementException();
					}
				} else {
					throw new NoSuchElementException();
				}
			} else {
				throw new NoSuchElementException();
			}
		} catch (NoSuchElementException ex) {
			throw new DbmsError("Invalid INSERT INTO statement. '" + sql + "'.");
		}
	}
	private void updateIndex(String sql, Table tab)
	{
		int i=0,j=0;
		String value="";
		int num=0;
		String [] indexValue=new String[20];
		String data=String.format("%1s", sql.substring(sql.indexOf("("))).replace(",", " ");
		String[] insertTokens= data.split("\\s+");
		for(Index index : tab.getIndexes())
		{
			value="";
			Index.IndexKeyVal newVal=index.new IndexKeyVal();
			for( int a=0; a<indexValue.length;a++)
				indexValue[a]= null;
			for(i=0;i<index.getIdxKey().size();i++ )
			{ 
				// Construct Key for new Value
				Index.IndexKeyDef def= index.new IndexKeyDef();
				def=index.getIdxKey().get(i);
				for(Column col : tab.getColumns())
				{
					if(def.colId==col.getColId())
					{
						if(col.getColType().equals(Column.ColType.INT))
						{
							if(insertTokens[def.colId].equals("-"))
				    		{
				    			char nullValue[]= new char [10]; 
				    			
				    			for(int a=0;a<10;a++)
					    			nullValue[a]='~';
				    			indexValue[def.idxColPos-1]=String.valueOf(nullValue) ;
				    		}
							else
							{
							num=Integer.parseInt(insertTokens[def.colId]);
							
							if(def.descOrder)
							{
								double MAX=999999999;
					    		
					    		
					    		   num=(int)MAX-num;
					    		   indexValue[def.idxColPos-1]="9"+num;
					    		   
					    		  
							}
							
					    	else
					    	indexValue[def.idxColPos-1]=String.format("%010d", num);
					    			
							}
						}
						else
						{
							if(insertTokens[def.colId].equals("-"))
				    		{
				    			char nullValue[]= new char [col.getColLength()]; 
				    			for(int a=0;a<col.getColLength();a++)
					    			nullValue[a]='~';
				    			indexValue[def.idxColPos-1]=String.valueOf(nullValue) ;
				    		}
							else
							{
							 insertTokens[def.colId]=String.format("%-"+col.getColLength()+"s", insertTokens[def.colId]);
							 
							if(def.descOrder)
							{
								indexValue[def.idxColPos-1]=complementChar(insertTokens[def.colId]);
							}
							else
								indexValue[def.idxColPos-1]=insertTokens[def.colId];
							}
						}
					}
				
				}
				
				
			}
			for(j=0;j<indexValue.length;j++)
			{
				
				if(indexValue[j]!=null)
				{
					value=value+indexValue[j];
				}
			}
			
			
	
			ArrayList<Index.IndexKeyVal> indexData= index.getKeys();
			HashMap<Integer,String> unsorted= new HashMap<Integer,String>();
			HashMap<Integer,String> sorted= new HashMap<Integer,String>();
			int [] rid= new int[indexData.size()+1];
			// insert new value to Index 
			for(j=0;j<indexData.size();j++)
			{
				rid[j]=indexData.get(j).rid;
				unsorted.put(indexData.get(j).rid, indexData.get(j).value);
			}
	
			Arrays.sort(rid);
		
			
			newVal.rid=rid[indexData.size()] + 1;
			newVal.value=value;
			index.addKey(newVal);
			
			unsorted.put(newVal.rid, value);
			//sort the updated Index Keys
			sorted=sort(unsorted);
			
			i=0;
			for(Map.Entry<Integer, String> e : sorted.entrySet())
		     {
		    	
		    		indexData.get(i).rid=e.getKey();
		    		indexData.get(i).value=e.getValue();
		    		i++;
		    	
		    	
		     }
		   
			
		}
	}

	/**
	 * DROP TABLE
	 * <table name>
	 * ;
	 * 
	 * @param sql
	 * @param tokenizer
	 * @throws Exception
	 */
	private void dropTable(String sql, StringTokenizer tokenizer)
			throws Exception {
		try {
			// Get table name
			String tableName = tokenizer.nextToken();

			// Check for the semicolon
			String tok = tokenizer.nextToken();
			if (!tok.equals(";")) {
				throw new NoSuchElementException();
			}

			// Check if there are more tokens
			if (tokenizer.hasMoreTokens()) {
				throw new NoSuchElementException();
			}

			// Delete the table if everything is ok
			boolean dropped = false;
			for (Table table : tables) {
				if (table.getTableName().equalsIgnoreCase(tableName)) {
					table.delete = true;
					//delete the indexes on table to be deleted 
					for(Index in : table.getIndexes())
					{
						in.delete=true;
					}
					dropped = true;
					break;
				}
			}

			if (dropped) {
				out.println("Table " + tableName + " was dropped.");
			} else {
				throw new DbmsError("Table " + tableName + "does not exist. '" + sql + "'."); 
			}
		} catch (NoSuchElementException ex) {
			throw new DbmsError("Invalid DROP TABLE statement. '" + sql + "'.");
		}

	}
  private void dropIndex(String sql, StringTokenizer tokenizer) throws DbmsError
  {
	  String indexName=null;
	  int chkIndex=10;
	  DBMS db= new DBMS();
	
	  try
	  {
		  // check for Syntax
		  if(sql.contains(";"))
		  {
			  
			  indexName=tokenizer.nextToken();
			 
			  if(indexName.endsWith(";"))
			  {
				  indexName=indexName.substring(0, indexName.indexOf(";"));
				 
			  }
			  for (Table table : tables ) {
				  if(chkIndex==1)
					  break;
				  if(!table.delete)
				  {
					
					  ArrayList<Index> inlist= table.getIndexes();
					  if(inlist.isEmpty())
						  chkIndex=0;
					  for(Index in:inlist )
					  {
						 
						  if(in.getIdxName().equalsIgnoreCase(indexName)&&!in.delete)
						  {
							 // Delete the index and decrement the number of indexes on Table 
							 
							  table.setNumIndexes(table.getNumIndexes()-1);
							  in.delete=true;
							
							  chkIndex=1;
						
							  
							  break;
						  }
						  else
							  chkIndex=0;
						 
					  }
				  }
				  
				  
			  }
			 // Index does not exist
			  if(chkIndex==0)
				  throw new DbmsError("Invalid Index Name: Index does not exist");
			  else
			  out.println("Index " + indexName + " was dropped.");
			  
			  
		  }
		  else
			  throw new DbmsError("Invalid Syntax: Syntax error");
	  }catch(NoSuchElementException ex){
		  throw new DbmsError("Invalid DROP INDEX statement. '" + sql + "'.");
		  
		  
	  }
	  
  }
  private String computeRunstats(String sql, StringTokenizer tokenizer ) throws DbmsError
  {
	  int chkTable=10, numDup=0,occ=0;
	  String [] columnData= new String[1000];
	try
	{
	  tableName=tokenizer.nextToken();
	 
	  int numCols=0,i=0,j=0;
	  Table table=null;
	  // check for syntax
	  if(!sql.endsWith(";"))
		  throw new DbmsError("Syntax Error: Invalid syntax");
	  for(Table tab:tables)
	  {
		  // check if table exists
		  if(tab.getTableName().equalsIgnoreCase(tableName)&&!tab.delete)
		  {
			  chkTable=1;
			  table=tab;
			  break;
		  }
		  else
			  chkTable=0;
	  }
	  if(chkTable!=1)
		  throw new DbmsError("Error: Invalid Table Name.");
	  numCols= table.getNumColumns();
	  for(Column col: table.getColumns())
	  {
		  // get table data and evaluate it column wise
		  ArrayList<String> data= table.getData();
		 
		  for(i=0;i<data.size();i++)
		  {
			  String[] tokens=data.get(i).split("\\s+");
			  
			  if(col.getColType()==Column.ColType.INT)
			  {
				
					
			    columnData[i]=String.format("%10s", tokens[col.getColId()+1]).replace(" ", "*");
				
			  }
			  else
				  columnData[i]=tokens[col.getColId()+1];
			 
			  
		  }
		  int datasize=i;
		  table.setTableCard(data.size());
		  List<String> colList=new ArrayList<String>(Arrays.asList(columnData));
		  int colCard=0;
		  List<String> duplicates=new ArrayList<String>();
		  // calculate column cardinality
		  for(i=0;i<datasize;i++)
		  {
			  if(!columnData[i].startsWith("-"))
			  {
			  int temp=(Collections.frequency(duplicates, columnData[i]));
			
			  if(temp==0)
			  {
			 
			    occ=Collections.frequency(colList, columnData[i]);
			    
			     colCard=colCard+occ;
			      if(occ>1)
			      {
				      colCard=colCard-(occ-1);
				      duplicates.add(columnData[i]);
				      
			      }
			  }
			  }
		  }
		  
		 
		 
		  col.setColCard(colCard);
		  // calculate High Key and Low Key
		  String[] sortArray=new String[datasize];
		  for(i=0;i<datasize;i++)
		  {
			  sortArray[i]=columnData[i];
			  
			 
		  }
		 
		  Arrays.sort(sortArray,String.CASE_INSENSITIVE_ORDER);
		
		 String lowkey=sortArray[0];
		 if(lowkey.startsWith("*"))
			 lowkey=lowkey.substring(lowkey.lastIndexOf("*")+1, lowkey.length());
		
		 if(lowkey.startsWith("-"))
		 {
			 for(i=0;i<datasize&&sortArray[i].contains("-");i++);
			  if(i==0)
				 i++;
			 lowkey=sortArray[i];
			 if(lowkey.startsWith("*"))
				 lowkey=lowkey.substring(lowkey.lastIndexOf("*")+1, lowkey.length());
			
		 }
		  col.setLoKey(lowkey);
		  
		  String highKey= sortArray[datasize-1];
            if(highKey.contains("*"))
            {
            	highKey=highKey.substring(highKey.lastIndexOf("*")+1, highKey.length());
		     }
		  col.setHiKey(highKey);
		 
		 
	  }
	}catch(NoSuchElementException ex)
	{
		throw new DbmsError("Invalid RUNSTATS statement. '" + sql + "'.");
	}
	return tableName;
  
  }
	private void printRunstats(String tableName) {
		for (Table table : tables) {
			if (table.getTableName().equals(tableName)) {
				out.println("TABLE CARDINALITY: " + table.getTableCard());
				for (Column column : table.getColumns()) {
					out.println(column.getColName());
					out.println("\tCOLUMN CARDINALITY: " + column.getColCard());
					out.println("\tCOLUMN HIGH KEY: " + column.getHiKey());
					out.println("\tCOLUMN LOW KEY: " + column.getLoKey());
				}
				break;
			}
		}
	}
	private int checkQuery(String sql, StringTokenizer tokenizer,String columns[],String indexName)
	{
		int columnCnt=0;
		int res=0;
		String tok;
		int chkTable=0,chkIndex=0;
		int chkCol=0;
		tok=tokenizer.nextToken();
		
		Table table=null;
		if(tok.equalsIgnoreCase("ON"))
		{
			tableName=tokenizer.nextToken();
			
				// Check if the table exists
			
			for (Table tab : tables) {
				if (tab.getTableName().equalsIgnoreCase(tableName)&& !tab.delete) {
					
					chkTable=1;
					table=tab;
					if(chkTable==1)
					{
						ArrayList<Index> indexList=tab.getIndexes();
						for(Index in : indexList )
						{
							
						
							if(in.getIdxName().equalsIgnoreCase(indexName) && !in.delete)
							{
							    chkIndex=1;
							    break;
							}
							
						}
						
					}
					break;
				}
			}
			
			if(chkTable!=1)
					res=-1;
			else
			{
		       if(chkIndex==1)
			    res=-5;
				
		        else
				{
					tok=tokenizer.nextToken();
					
					//check for syntax of query
					if(tok.equals("(" )&& sql.contains(")")&& sql.contains(";"))
					{
						
						while(tok!=")"&&tokenizer.hasMoreTokens())
						{
							tok=tokenizer.nextToken();
							columns[columnCnt]=tok;
							columnCnt++;
							tok=tokenizer.nextToken();
							if(tok.equalsIgnoreCase("ASC")|| tok.equalsIgnoreCase("DESC"))
								tok=tokenizer.nextToken();
							if(tok.equals(")"))
									break;
							
							
						}
						
						
						if(table.getNumColumns()<columnCnt) //check for number of columns in index and table
							res=-3;
						
					
						else
						{
							//check if columns exist in table
							
						for(int i=0; i<columnCnt && chkCol!=1;i++)
						{
							chkCol=0;
						for( Column column : table.getColumns())
						{
							
							if(columns[i].equalsIgnoreCase(column.getColName()))
							{
								chkCol=0;
								break;
							}
							else
								chkCol=1;
								
						}
						if (chkCol!=0)
							res=-4;
						
						}
						}
					}
					else
						res=-2;
					
				}
			
		}
		}
		else
		res=-2;
		
		
		return res;
	}
	private void createIndex(String sql, StringTokenizer tokenizer, boolean uniqueIndex) throws Exception
	{
		
	  try
	  {
	  int chkQuery=0;
	  int i = 0,len,beginIndex,lastIndex,colId,j=0,colSize=0;
	  String columns[] = new String[20];
	  String values[]= new String[10000];
	  String indexName,columnName,order,temp=null;
	  indexName=tokenizer.nextToken();
	
	  chkQuery=checkQuery(sql,tokenizer,columns,indexName);
	
	  boolean intType = false,orderType=false;
	  HashMap<Integer,String> key= new HashMap<Integer,String>();
	  char colInfo[][]=new char[40][2];
	  int size[]= new int[]{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10};
	  Table table = new Table(tableName);
	  // check syntax of query
	  if(chkQuery==-1)
	  {
		  throw new DbmsError("Table does not exist.");
	  }
	  if(chkQuery==-2)
	  {
		  throw new DbmsError("Syntax error.");
	  }
	  if(chkQuery==-3)
	  {
		  throw new DbmsError("Invalid number of columns specified.");
	  }
	  if(chkQuery==-4)
	  {
		  throw new DbmsError("Invalid column names specified.");
	  }
	  if(chkQuery==-5)
	  {
		  throw new DbmsError("Cannot create Index : Index Already Exists.");
	  }
	  if(chkQuery==0)
	  {
		  System.out.println("Valid Query");
	  }
	  Index index = new Index(indexName);
	  index.setIsUnique(uniqueIndex);
	 
	  for (Table tab : tables) {
			if (tab.getTableName().equalsIgnoreCase(tableName)&& !tab.delete) {
				table = tab;
				break;
			}
	  }
	  while(columns[i]!=null)
	  {
		  //construct Index Definition
		  
		  Index.IndexKeyDef def= index.new IndexKeyDef(); 
		  def.idxColPos=i+1;
		  
		  len=columns[i].length();
		  beginIndex=sql.indexOf(columns[i])+len+1;
		  lastIndex=sql.indexOf(columns[i])+len+5;
		  if(lastIndex<=sql.length())
		  {
		  order=sql.substring(beginIndex,lastIndex);
		  order.trim();
		 
		  if(order.equalsIgnoreCase("DESC"))
		  {
			  
			  def.descOrder=true;
		  }
		  else
			  def.descOrder=false;
		  }
		  else
			  def.descOrder=false;
		  
		  ArrayList<Column> columnList =new ArrayList<Column>();
		  columnList=table.getColumns();
		
		  for(Column col: table.getColumns())
		  {
			  
			  if(columns[i].equalsIgnoreCase(col.getColName()))
			  {
				  def.colId=col.getColId();
				  break;
			  }
		  }
		  index.addIdxKey(def);
		  i++;
	  }
	  ArrayList<Index.IndexKeyDef> list= index.getIdxKey();
	  ArrayList<String> data= table.getData();
	// Construct  Index Keys
	  for(i=0;i<list.size();i++)
	  {
	    
	    colId=list.get(i).colId;
	    
	    orderType=list.get(i).descOrder;

	    for(Column col: table.getColumns())
		  {
	    	
	         // Get Column Type
			  intType=false;
			  if(colId==col.getColId())
			  {
				 
				  if(col.getColType() == Column.ColType.INT)
					 intType=true;	
				  colInfo[i][0]='I';
				  if(col.getColType()== Column.ColType.CHAR)
				  {
				     size[col.getColId()]=col.getColLength();
				     colInfo[i][0]='C';
				  }
				  
				  break;
			  }
		  }
	    for(j=0;j<data.size();j++)
	    {
	    	String[] queryData= data.get(j).split("\\s+");
	    	
	    	int rindex=colId+1;
	    	if(intType)
	    	{
	    		if(queryData[rindex].equals("-"))
	    		{
	    			char nullValue[]= new char [10]; 
	    			for(int a=1;a<10;a++)
		    			nullValue[a]='~';
	    			 temp=String.valueOf(nullValue) ;
	    		}
	    		
	    		else
	    		{
	    		
	    		double MAX=999999999;
	    		int num=Integer.parseInt(queryData[rindex]);
	    		// If sort order Desc take complement 
	    		if(orderType)
	    		{
	    		   num=(int)MAX-num;
	    		   temp="9"+num;
	    		   colInfo[i][1]='D';
	    		  
	    		}
	    		else
	    		{
	    			 temp=String.format("%010d", num);
	    			 colInfo[i][1]='A';
	    		}
	    			
	    		}
	    	}
	    	else
	    	{
	    		
	    		colSize=size[colId];
	    		String tempData=null;
	    		if(queryData[rindex].contains("-"))
	    		{
	    		
		        	char nullValue[]= new char [colSize]; 
		    		for(int a=0;a<colSize;a++)
		    			nullValue[a]='~';
		    		temp=String.valueOf(nullValue) ;
		    		
	    		}
	    		else
	    		{
	    		tempData=String.format("%-"+colSize+"s", queryData[rindex]);
	            
	    		 colInfo[i][0]='C';
	    		// If sort order Desc take complement 
	    		 if(orderType)
		    		{
		    			tempData=complementChar(tempData);
		    			
		    			colInfo[i][1]='D';
		    		}
	    		 else
	    			 colInfo[i][1]='A';
	    		 temp=tempData;
	    		}
	    		
	    	}
	    	if(i==0)
	    		values[j]=temp;
	    	else
	    	values[j]=values[j]+temp;
	    	
	    }
	   
	    
	  }
	  for(i=0;i<data.size();i++)
	    {
	    	key.put(i+1, values[i]);
	    }
	    key=sort(key);
	   // printMap(key);
	    // Check if Unique Index 
	    if(uniqueIndex)
	    {
	        Collection<String> mapList=key.values();
	        Set<String> mapSet= new HashSet<String>(key.values());
	        // Cannot construct unique Index with duplicate values in table 
	        if(mapList.size()!=mapSet.size())
	        {
	    	throw new DbmsError("Cannot Insert Duplicate Values: Unique Index.");
	        }
	    }
	    for(Map.Entry<Integer, String> e : key.entrySet())
	     {
	    	String value=null;
	    	Index.IndexKeyVal val = index.new IndexKeyVal();
	    	val.rid=Integer.parseInt(e.getKey().toString());
	    	
	    	String[] token=e.getValue().split("\\s+");
	    	val.value=e.getValue();
	    	// Add Keys to the given Index 
	    	index.addKey(val);
	     }
	   
	     
     table.addIndex(index);
     System.out.println(" Index Created .");
	
	 }catch(NoSuchElementException ex)
	  {
		  throw new DbmsError("Invalid CREATE INDEX statement. '" + sql
					+ "'.");
	  }
	 
	 
}
public HashMap<Integer,String> convertString(HashMap<Integer,String>map, char[][] colInfo, int [] size)
{
	int i,j,colPos,colLen = 0, padLen=0;
	
	for(i=0;i<colInfo.length;i++)
	{
		
		if(colInfo[i][0]=='C'&&colInfo[i][1]=='D')
		{
	       colPos=i;
	       
	       for(Map.Entry<Integer, String> e : map.entrySet())
	       {
	    	 String value=e.getValue().toString(); 
		     String tokens[]=value.split("\\s+");
		     
		    
		     tokens[colPos]=complementChar(tokens[colPos]);
		     tokens[colPos]=tokens[colPos].trim();
		    
		     for(j=0;j<tokens.length;j++)
		     {
		    	 colLen=size[colPos];
		    	 padLen=colLen-tokens[j].length();
		    	 tokens[j]=String.format("%-"+colLen+"s", tokens[j]);	
		    	 if(j==0)
		    		 value=tokens[j];
		    	 else
		    	 value=value+tokens[j];
		     }
		     e.setValue(value);
		     
	       }
		}
	}
	return map;
}
public String complementChar(String value)
{   
	char a;
	int j;
    String complement=null;
	for(int i=0;i<value.length();i++)
	{
		a =value.charAt(i);
		if(64<(int)a && (int)a<91)
		{
			j= (int)value.charAt(i) - (int)'A' +1 ;
			j=26-j;
			j=(int)'A'+j;
		}
		else
		{
		j= (int)value.charAt(i) - (int)'a' +1 ;
		j=26-j;
		j=(int)'a'+j;
		}
		if((int)a==32)
			j=a;
		if(i==0)
			complement=Character.toString((char)j);
		else
			complement=complement+(char)j;
	}
	
	return complement;
}


public HashMap<Integer,String> sort(HashMap<Integer,String> unsorted)
{
	
	HashMap<Integer, String> sortedMap = new LinkedHashMap<Integer, String>();
	List<Entry<Integer,String>> listValues = new ArrayList<Entry<Integer, String>>(unsorted.entrySet());
	// Custom Comparator to sort Index Values 
	Comparator<Entry<Integer,String>> sortValues = new Comparator<Entry<Integer,String>>(){

		@Override
		public int compare(Entry<Integer, String> o1, Entry<Integer, String> o2) {
			// TODO Auto-generated method stub
			String first=o1.getValue();
			String second=o2.getValue();
			return first.compareToIgnoreCase(second);
			
		}
		
	};
	
	Collections.sort(listValues, sortValues);
	for(Entry<Integer,String> e : listValues)
	sortedMap.put(e.getKey(), e.getValue());
	return sortedMap;
		
		
}
public void select(String sql, StringTokenizer tokenizer) throws Exception
{
	String selectString=null, tableName=null,tempTables=null;
	String literal="no literal";
	String colName=null;
	Table table=null;
	String[][] predicates=new String[100][100];
	HashMap<String,ArrayList<String>> selectList= new HashMap<String,ArrayList<String>>(); 
	HashMap<String,ArrayList<String>> predicateList= new HashMap<String,ArrayList<String>>();
    int c=0,i=0,tabchk=-1;
    String temp=sql.toLowerCase();
    if(!temp.contains("from"))
    	throw new DbmsError("SYNTAX ERROR :");
   // check for columns to be present in result set
    if(sql.contains("*"))
    {
    	String tempSql=sql.toLowerCase();
    	if(tempSql.contains("where"))
    	tempTables=sql.substring(temp.lastIndexOf("from")+4,temp.lastIndexOf("where"));
    	else
    		tempTables=sql.substring(temp.lastIndexOf("from")+4);	
    	
    	String selectTokens[]=tempTables.split(",");
    	if(selectTokens.length>1)
    		throw new DbmsError("SYNTAX ERROR: MISSING COMMA");
    	tableName=selectTokens[0];
    	for (Table tab : tables) {
    	  if (tab.getTableName().equalsIgnoreCase(tableName.trim())&& !tab.delete) {
    			
    			tabchk=1;
    			table=tab;
    			break;
    		}
    	 }
    	
    	   if(tabchk==1)
    		{
    		   ArrayList<Column> colList=table.getColumns();
    		   ArrayList<String> list=new ArrayList<String>();
    		   for(Column col: colList)
    			list.add(col.getColName());
    			selectList.put(tableName,list);
    		}
    	       else
    	    	   throw new DbmsError("ERROR: TABLE DOES NOT EXIST");     	
    }
    else
    {
    selectString=sql.substring(6, temp.lastIndexOf("from"));
    selectString=selectString.trim();
	String [] tokens= selectString.split(",");
	for(i=0;i<tokens.length;i++)
	{
		temp=tokens[i];
		ArrayList<String> tempList=new ArrayList<String>();
		tableName=temp.substring(0, temp.lastIndexOf("."));
		tableName=tableName.trim();
		if(tableName.contains("."))
			throw new DbmsError("SYNTAX ERROR : MISSING COMMA");   
		colName=temp.substring(temp.lastIndexOf(".")+1);
		int chk=chkTableCol(tableName,colName);
	
		if(chk==1)
		{
		   if(selectList.containsKey(tableName))
			selectList.get(tableName).add(colName);
		  else
		  {
			tempList.add(colName);
			selectList.put(tableName, tempList);
		  }
		}
	
	  else if(chk==-1)
	  {
		  throw new DbmsError("ERROR: TABLE DOES NOT EXIST");
	  }
	  else if(chk==-2)
	  {
		  throw new DbmsError("ERROR: INVALID COLUMN NAME");
	  }
		
	}
    }
    
	// check for Columns listed in from clause
	Set<String> tableList=selectList.keySet();
	for(String s : tableList)
	{
		
		temp=sql.toLowerCase();
        s=s.toLowerCase();
	    if(!temp.substring(temp.lastIndexOf("from")).contains(s.trim()))
	    	throw new DbmsError("SYNTAX EROR : TABLE NOT PRESENT IN FROM CLAUSE");
	    	
	}
	
	
	//print Index Information
	for(String s : tableList)
	{
	  printIndexList(s);
	}
	if(sql.toLowerCase().contains("where"))
	{
	
	 predicates=getPredicates(sql,literal);
	 for(i=0;predicates[i][0]!=null;i++)
	 {
		 if(predicates[i][1].equalsIgnoreCase("literal"))
		 {
			 literal=predicates[i][0];
			 predicates[i][0]=null;
		 }
	 }
	 
	if(sql.toLowerCase().contains("order by"))
	{
		String Col=sql.substring(sql.toLowerCase().lastIndexOf("order by")+9);
		String[] orderToken = Col.split("\\s+");
		
	    tableName=orderToken[0].substring(0,orderToken[0].lastIndexOf("."));
	    colName=orderToken[0].substring(orderToken[0].lastIndexOf(".")+1);
	    if(colName.contains(";"))
	    	colName=colName.substring(0, colName.lastIndexOf(";"));
	  
	    int chkOrder=chkTableCol(tableName.trim(),colName.trim());
		  if(chkOrder==-1)
		  {
			  throw new DbmsError("ERROR: TABLE DOES NOT EXIST");
		  }
		  else if(chkOrder==-2)
		  {
			 
			  throw new DbmsError("ERROR: INVAILD COLUMN NAME");
		  }
		  String fromList=sql.substring(sql.toLowerCase().lastIndexOf("from"), sql.toLowerCase().lastIndexOf("where"));
		  if(!fromList.contains(tableName))
		  {
			  
			  throw new DbmsError("SYNTAX EROR : TABLE NOT PRESENT IN FROM CLAUSE");
		  }
	}
	
	int l=chkPredicates(predicates,sql,predicateList);
	ArrayList<Predicate> p=setPredicates(predicates,selectList,sql,literal);
	
	
}
else
{
	if(sql.toLowerCase().contains("order by"))
	{
		String Col=sql.substring(sql.toLowerCase().lastIndexOf("order by")+9);
		Col=Col.trim();
		String[] orderToken = Col.split("\\s+");
		
	    tableName=orderToken[0].substring(0,orderToken[0].lastIndexOf("."));
	    colName=orderToken[0].substring(orderToken[0].lastIndexOf(".")+1);
	    if(colName.contains(";"))
	    	colName=colName.substring(0, colName.lastIndexOf(";"));
	  
	    int chkOrder=chkTableCol(tableName.trim(),colName.trim());
		  if(chkOrder==-1)
		  {
			  throw new DbmsError("ERROR: TABLE DOES NOT EXIST");
		  }
		  else if(chkOrder==-2)
		  {
			 
			  throw new DbmsError("ERROR: INVAILD COLUMN NAME");
		  }
		  String fromList=sql.substring(sql.toLowerCase().lastIndexOf("from"), sql.toLowerCase().lastIndexOf("order by"));
		  
		  if(!fromList.contains(tableName))
		  {
			  
			  throw new DbmsError("SYNTAX EROR : TABLE NOT PRESENT IN FROM CLAUSE");
		  }
	}

	ArrayList<Predicate> list=new ArrayList<Predicate>();
	ArrayList<Predicate> p=setPredicates(predicates,selectList,sql,literal);
	
}
	
	
}
public Predicate chkTcp(ArrayList<Predicate> predicateList,String [][] predicates)
{
	int i=0,j=0,count=0,len=0,res=0;
	String joinPred=null,outer=null,inner=null;
	String tokens[]=new String [100];
	Predicate tcp=null;
	for(i=0;predicates[i][0]!=null;i++)
		len=len+1;
	for(i=0;i<predicateList.size(); i++)
	{
		Predicate first=predicateList.get(i);
		
		if(!first.isJoin())
		{
			if(first.getType()=='E'&& !first.getText().contains("IN"))
			{
			  tokens[j++] = first.getText().substring(0, first.getText().lastIndexOf("="));
			  tokens[j++] = "=";
			  tokens[j++] = first.getText().substring(first.getText().lastIndexOf("=")+1);
			}
			else if(first.getType()=='E'&& first.getText().contains("IN"))
			{
			  tokens[j++] = first.getDescription().substring(0, first.getDescription().lastIndexOf("="));
			  tokens[j++] = "=";
			  tokens[j++] = first.getDescription().substring(first.getDescription().lastIndexOf("=")+1);
			}
			
			else if(first.getType()=='R'&& first.getText().contains(">"))
			{
				 tokens[j++] = first.getText().substring(0, first.getText().lastIndexOf(">"));	
				 tokens[j++] = ">";
				 tokens[j++]  = first.getText().substring(first.getText().lastIndexOf(">")+1);	
				
			}
			else if(first.getType()=='R'&& first.getText().contains("<"))
			{
				 tokens[j++]  = first.getText().substring(0, first.getText().lastIndexOf("<"));
				 tokens[j++] = "<";
				 tokens[j++]  = first.getText().substring( first.getText().lastIndexOf("<")+1);
			}
			
			
		}
		else
			joinPred=first.getText();
		count=j;
	}
	if(joinPred!=null)
	{
	outer=joinPred.substring(0, joinPred.lastIndexOf('='));
	inner=joinPred.substring(joinPred.lastIndexOf('=')+1);
	
	j=0;
	for(i=0;i<count&&j<count;i++)
	{
	   if(i==0)
		   j=0;
	   else
		   j=i+2;
	   
	   if(j<count)
	   {
		   if(tokens[j].equalsIgnoreCase(inner))
		   {
		   Predicate p=new Predicate();
		   p.card1=getColCard(inner.substring(0,inner.lastIndexOf(".")),inner.substring(inner.lastIndexOf(".")+1));
		   p.ff1=(double)1/p.card1;
		   p.join=false;
		  
			   j=j+1;
			   if(tokens[j]=="=")
				   p.type='E';
			   else if(tokens[j]==">"||tokens[j]=="<")
				   p.type='R';
			   
			   
			   p.text=outer+tokens[j]+tokens[++j];
			   p.description="TCP";
			   predicates[len][0]=  p.text;
			   predicates[len][1]="0";
			   predicates[len-1][1]="1";
			   predicateList.add(p);
			   res=1;
			   tcp=p;
		 
		   }
		   else if(tokens[j].equalsIgnoreCase(outer))
		   {
		   Predicate p=new Predicate();
		   p.card1=getColCard(outer.substring(0,outer.lastIndexOf(".")),outer.substring(outer.lastIndexOf(".")+1));
		   p.ff1=(double)1/p.card1;
		   p.join=false;
		  
			   j=j+1;
			   if(tokens[j]=="=")
				   p.type='E';
			   else if(tokens[j]==">"||tokens[j]=="<")
				   p.type='R';
			   
			   p.text=inner+tokens[j]+tokens[++j];
			   p.description="TCP";
			   predicates[len][0]=  p.text;
			   predicates[len][1]="0";
			   predicates[len-1][1]="1";
			   predicateList.add(p);
			   res=1;
			   tcp=p;
		 
		   }
		 
		   
	   }
	   
	}
	}
	return tcp;
}
public void classifyPredicates(ArrayList<Predicate>predicateList, ArrayList<String> matchList,ArrayList<String> screenList, ArrayList<String> selectList,ArrayList<String>andList)
{
	String tableName=null;
	HashMap<String,HashMap<String, ArrayList<String>>> tabIndexList=new HashMap<String,HashMap<String, ArrayList<String>>>(); 
	HashMap<String,ArrayList<String>> indexList= new HashMap<String,ArrayList<String>> ();
    for(String s : selectList)
    	{
    		tableName=s;
    		indexList=getIndexList(s);
    		tabIndexList.put(tableName, indexList);
    	
    		
    	}
    	
    
    Set<String> keys= tabIndexList.keySet();
    for(String s : keys)
    {
    	HashMap<String,ArrayList<String>> currIndexList=tabIndexList.get(s);
    	Set<String> Indexes = currIndexList.keySet();
    	for(String in : Indexes)
    	{
    		String phase="matching";
    		int flag=0;
    		ArrayList<String>colList=currIndexList.get(in);
    		for(String col :colList)
    		{
    			
    			int chk=0;
    			int found=0;
    			for(Predicate p : predicateList)
    			{
    				
    				if(flag==1)
    					phase="screening";
    				
    				if(!p.isJoin())
    				{
    					
    					String temp="";
    					if(p.getText().contains("OR")||p.getText().contains("Or")||p.getText().contains("or"))
    					{
    						if(p.getDescription().contains("IN")||p.getDescription().contains("In")||p.getDescription().contains("in"))
    						{
    							if(andList.contains(p.getDescription().trim()))
    							temp=p.getDescription();
    							
    							else
        							temp=null;
    						}
    							
    						
    					}
    					else
    					{
    						
    						if(andList.contains(p.getText().trim()))
    					     temp=p.getText();
    						else
    							temp=null;
    						
    						
    					}
    					if(temp!=null)
    					{
    					temp=temp.trim();
    					if(temp.substring(0,temp.lastIndexOf(".")).equalsIgnoreCase(s.trim()))
    					{ 
    						
    						if(temp.contains(col.trim()))
    						{
    							found=1;
    							
    							if(temp.contains( " = ")&& phase.equals("matching"))
    							{
    							matchList.add(temp+"_"+in);
    							
    							}
    							else if (temp.contains("in")||temp.contains("IN")||temp.contains("In"))
    							{
    								if(phase.equals("matching"))
    								{
    								matchList.add(temp.trim()+"_"+in.trim());
    								
    								}
    							}
    							else if(temp.contains(">")||temp.contains("<"))
    							{
    								if(phase.equals("matching"))
    								{
    									matchList.add(temp.trim()+"_"+in.trim());
    									phase="screening";
    									
    								}
    								else
    									screenList.add(temp.trim()+"_"+in.trim());
    								
    							}
    							

    							else if(phase.equals("screening"))
									screenList.add(temp.trim()+"_"+in.trim());
    							if(colList.indexOf(col)==0)
    								chk=1;
    							
    						}
    						else
    						{
    							if(found!=1)
    								found=0;
    						}
    						
    						
    					}
    					
    				}
    				}
    			}
    			
				
				int pos=colList.indexOf(col);
				if(chk==0&&pos==0)
				flag=1;
				else if(found==0)
					flag=1;
				
    		}
    	}
    }
    
	
}
public HashMap<String,ArrayList<String>> getIndexList(String tableName)
{
	HashMap<String,ArrayList<String>> indexList=new HashMap<String,ArrayList<String>>();
	for(Table tab: tables)
	{
		if (tab.getTableName().equalsIgnoreCase(tableName.trim())&& !tab.delete) {
		ArrayList<Index> indexNames=tab.getIndexes();
		ArrayList<Column> colList=tab.getColumns();
		for(Index in: indexNames)
		{
		  ArrayList<Index.IndexKeyDef> indexDef= in.getIdxKey();
		  ArrayList<String> colNames= new ArrayList<String>();
		  for(Index.IndexKeyDef def : indexDef)
		  {
			  for(Column col: colList)
			  {
				  if(col.getColId()==def.colId)
				  {
					  colNames.add(col.getColName().trim());
					  break;
				  }
			  }
			  
		  }
		  
		  indexList.put(in.getIdxName().trim(), colNames);
		}
		
		}
		
	}
	return indexList;
	
}
public HashMap<String,ArrayList<String>> getIndexOrder(String tableName)
{
	HashMap<String,ArrayList<String>> indexList=new HashMap<String,ArrayList<String>>();
	for(Table tab: tables)
	{
		if (tab.getTableName().equalsIgnoreCase(tableName.trim())&& !tab.delete) {
		ArrayList<Index> indexNames=tab.getIndexes();
		ArrayList<Column> colList=tab.getColumns();
		for(Index in: indexNames)
		{
		  ArrayList<Index.IndexKeyDef> indexDef= in.getIdxKey();
		  ArrayList<String> colNames= new ArrayList<String>();
		  for(Index.IndexKeyDef def : indexDef)
		  {
			  for(Column col: colList)
			  {
				  if(col.getColId()==def.colId)
				  {
					  if(def.descOrder)
					  colNames.add(col.getColName().trim()+"_D");
					  else
						  colNames.add(col.getColName().trim()+"_A");  
					  break;
				  }
			  }
			  
		  }
		  
		  indexList.put(in.getIdxName().trim(), colNames);
		}
		
		}
		
	}
	return indexList;
	
}

public String[][] getPredicates(String sql,String literal) throws DbmsError
{
	int i=0, j=0,k=0;
	String[][] predicateList = new String[1000][2];
	String[] tempList = new String[1000];
	String temp=null;
	if(sql.toLowerCase().contains("order by"))
	temp= sql.substring(sql.toLowerCase().lastIndexOf("where")+5, sql.toLowerCase().lastIndexOf("order by"));
	else
	{
		if(!sql.toLowerCase().contains("where"))
			throw new DbmsError("SYNTAX ERROR");
		temp= sql.substring(sql.toLowerCase().lastIndexOf("where")+5);
	}
		
	if(temp.contains("and")||temp.contains("AND")||temp.contains("And"))
	tempList=temp.split("(?i)and");
	else
		tempList[0]=temp;
	j=0;
	for(i=0;i<tempList.length&&tempList[i]!=null;i++)
	{
		
		String[]ortokens=new String[60];
		if(tempList[i].contains("OR")||tempList[i].contains("or")||tempList[i].contains("Or"))
		{
		
			ortokens=tempList[i].split("(?i)or");
		    for(k=0;k<ortokens.length;k++)
		    {
		    	if(ortokens[k].contains("."))
		    	{
		    	
			        predicateList[j][0]=ortokens[k];
			        if(k+1==ortokens.length)
			        {
			        	
			         	if(i+1==tempList.length||tempList[i+1]==null)
					     predicateList[j][1]="0";
				        else
					     predicateList[j][1]="1";
					
					}
			     else
				 predicateList[j][1]="2";
		         j++;
		    	}
		    	else
		    	{
		    		literal="or "+ ortokens[k];
		    		if(j>0)
		    		predicateList[j-1][1]="0";
		    		
		    	}
		    }
		    
		}
	
		
		else
		{
			if(tempList[i].contains("."))
			{
			predicateList[j][0]=tempList[i];
			
			if(tempList[1]==null)
			predicateList[j][1]="0";
			else
				predicateList[j][1]="1";
			if(i>0 && i+1==tempList.length)
				predicateList[j][1]="0";
			if(predicateList[j][1]!="0")
				predicateList[j][1]="1";
			j++;
			}
			else
			{
				literal=" and "+tempList[i];
				if(j>0)
				predicateList[j-1][1]="0";
				
					
				
			}
		}
			
	}
	int cnt=0;
	for(i=0;i<predicateList.length&&predicateList[i][0]!=null;i++)
	{
		
		cnt++;
	}
	if(!literal.equalsIgnoreCase("no literal"))
	{
		predicateList[cnt][0]=literal;
		predicateList[cnt][1]="literal";
	}
	
	return predicateList;
}
public ArrayList<Predicate> setPredicates(String predicates[][],HashMap<String,ArrayList<String>> selectList,String sql,String literal) throws DbmsError
{
	ArrayList<Predicate> predicateList =new ArrayList<Predicate>();
	ArrayList<String> andList = new ArrayList<String>();
	HashMap<String,Integer> indexPred=new HashMap<String,Integer>();
	ArrayList<String> orList = new ArrayList<String>();
	ArrayList<String> matchList=new ArrayList<String>();
	ArrayList<String> screenList=new ArrayList<String>();
	
	int i=0,j=0;
	String tableName,colName;
	for(i=0;predicates[i][0]!=null;i++)
	{
		Predicate p= new Predicate();
		String curr= predicates[i][0];
		String currOperator=null;
		if(curr.contains("="))
		{
			currOperator="=";
			p.type='E';
		}
		else if(curr.contains(">"))
		{
			currOperator=">";
			p.type='R';
		}
		else if(curr.contains("<"))
		{
			currOperator="<";
			p.type='R';
		}
		else if(curr.contains("in"))
		{
			currOperator="in";
			p.type='I';
		}
		else if(curr.contains("IN"))
		{
			currOperator="IN";
			p.type='I';
		}
		else if(curr.contains("In"))
		{
			currOperator="In";
			p.type='I';
		}
		 
			p.text=curr;
		
			if(curr.substring(curr.lastIndexOf(currOperator)).contains("."))
			{
				
				p.join=true;
				String temp=curr.substring(0,curr.lastIndexOf(currOperator));
				tableName=temp.substring(0,temp.lastIndexOf("."));
				colName=temp.substring(temp.lastIndexOf(".")+1);
				p.card1=getColCard(tableName.trim(),colName.trim());
				tableName=curr.substring(curr.lastIndexOf(currOperator)+1, curr.lastIndexOf("."));
				colName=curr.substring(curr.lastIndexOf(".")+1);
				p.card2=getColCard(tableName.trim(),colName.trim());
				if(p.card1==0)
					p.ff1=0;
				else
				p.ff1=(double)1/(double)p.card1;
				if(p.card2==0)
					p.ff2=0;
				else
				p.ff2=1/Double.valueOf(p.card2);
				
				
			}
			else
			{
				p.join=false;
			
				tableName=curr.substring(0,curr.lastIndexOf("."));
				
				colName=curr.substring(curr.lastIndexOf(".")+1,curr.lastIndexOf(currOperator));
				String hikey=null,lowkey=null;
				hikey=getHiKey(tableName,colName);
				lowkey=getlowKey(tableName,colName);
				p.card1=getColCard(tableName.trim(),colName.trim());
				if(currOperator.equals("="))
				{
					String lValue=curr.substring(curr.lastIndexOf("=")+1);
					if(p.card1==0)
						p.ff1=0;
					  else
					  p.ff1=1/(double)p.card1;
			
				}
				else if ( currOperator.equalsIgnoreCase("IN") )
				{
					String temp=curr.substring(0,curr.lastIndexOf(currOperator));
					tableName=temp.substring(0,temp.lastIndexOf("."));
					colName=temp.substring(temp.lastIndexOf(".")+1);
					p.card1=getColCard(tableName.trim(),colName.trim());
				    String tokenString= curr.substring(curr.lastIndexOf("(")+1,curr.lastIndexOf(")"));
				    String[] tokens= tokenString.split(",");
				    if(tokens.length==1)
				    {
				    	p.description=temp+"="+ tokens[0];
				    	p.type='E';
				    }
				    p.ff1=(double)tokens.length/(double)p.card1;
				    if(p.ff1<0||p.ff1>1)
			        	p.ff1=0;
				}
				else if (currOperator.equals(">"))
				{
                    
					String literalValue=curr.substring(curr.lastIndexOf(">")+1);
					try
					{
					literalValue=literalValue.trim();
					double lit=Double.parseDouble(literalValue);
					double hiKey= Double.parseDouble(hikey);
					double lowKey= Double.parseDouble(lowkey);
			        p.ff1=(hiKey-lit)/(hiKey-lowKey);
			        if(p.ff1<0||p.ff1>1)
			        	p.ff1=0;
					
					}catch(NumberFormatException e )
					{
						int a = 0,b = 0,c=0,lit = 0,hi,low = 0;
						System.out.println("Literal "+ literalValue );
						
					    if(64<(int)literalValue.charAt(0)   && (int)literalValue.charAt(0)  <91)
						a=	(int)literalValue.charAt(0) - (int)'A' + 1 ;
					    else
					    	a=	(int)literalValue.charAt(0) - (int)'a' + 1  ;
                        if(literalValue.length()>1)
                        {
					        if(64<(int)literalValue.charAt(1)   && (int)literalValue.charAt(1)  <91)
							b=	(int)literalValue.charAt(1) - (int)'A' + 1 ;
						    else
						    b=	(int)literalValue.charAt(1) - (int)'a' +1 ;
					        lit=26*a+b;
                        }
                        else
                        	lit=a;
						
						
					    	
						if(64<(int)lowkey.charAt(0)   && (int)lowkey.charAt(0)  <91)
						a=	(int)lowkey.charAt(0) - (int)'A' + 1 ;
						else
						 a=	(int)lowkey.charAt(0) - (int)'a' + 1  ;
						if(lowkey.length()>1)
						{
						if(64<(int)lowkey.charAt(1)   && (int)lowkey.charAt(1)  <91)
						b=	(int)lowkey.charAt(1) - (int)'A' + 1 ;
						else
						b=	(int)lowkey.charAt(1) - (int)'a' +1 ;
						
						low=26*a+b;
						}
						else
							lit=a;
						
						if(64<(int)hikey.charAt(0)   && (int)hikey.charAt(0)  <91)
						a=	(int)hikey.charAt(0) - (int)'A' + 1 ;
						else
					    a=	(int)hikey.charAt(0) - (int)'a' + 1  ;
						if(hikey.length()>1)
						{
						if(64<(int)hikey.charAt(1)   && (int)hikey.charAt(1)  <91)
						b=	(int)hikey.charAt(1) - (int)'A' + 1 ;
						else
						b=	(int)hikey.charAt(1) - (int)'a' +1 ;
						
						hi=26*a+b;
						}
						else
							hi=a;
							
						p.ff1=(double)(hi-lit)/(double)(hi-low);
						if(p.ff1<0||p.ff1>1)
				        	p.ff1=0;
						
						
					}
				}
					else if (currOperator.equals("<"))
					{
	                    
						String literalValue=curr.substring(curr.lastIndexOf("<")+1);
						try
						{
						literalValue=literalValue.trim();
						double lit=Double.parseDouble(literalValue);
						
						double hiKey= Double.parseDouble(hikey);
						double lowKey= Double.parseDouble(lowkey);
						p.ff1=(lit-lowKey)/(hiKey-lowKey);
						if(p.ff1<0||p.ff1>1)
				        	p.ff1=0;
						
						}catch(NumberFormatException e )
						{
							int a = 0,b = 0,c=0,lit,hi,low;
							hikey=getHiKey(tableName,colName);
						    lowkey=getlowKey(tableName,colName);
						    if(64<(int)literalValue.charAt(0)   && (int)literalValue.charAt(0)  <91)
							a=	(int)literalValue.charAt(0) - (int)'A' + 1 ;
						    else
						    	a=	(int)literalValue.charAt(0) - (int)'a' + 1  ;
						    if(literalValue.length()>1)
						    {
						        if(64<(int)literalValue.charAt(1)   && (int)literalValue.charAt(1)  <91)
								b=	(int)literalValue.charAt(1) - (int)'A' + 1 ;
							    else
							    b=	(int)literalValue.charAt(1) - (int)'a' +1 ;
							    lit=26*a+b;
						    }
						    else
						    	lit=a;
							
							if(64<(int)lowkey.charAt(0)   && (int)lowkey.charAt(0)  <91)
							a=	(int)lowkey.charAt(0) - (int)'A' + 1 ;
							else
							 a=	(int)lowkey.charAt(0) - (int)'a' + 1  ;
							 if(literalValue.length()>1)
							 {
						     	if(64<(int)lowkey.charAt(1)   && (int)lowkey.charAt(1)  <91)
							    b=(int)lowkey.charAt(1) - (int)'A' + 1 ;
							    else
							    b=(int)lowkey.charAt(1) - (int)'a' +1 ;
							    low=26*a+b;
							 }
							 else 
								 low=a;
							
							if(64<(int)hikey.charAt(0)   && (int)hikey.charAt(0)  <91)
							a=	(int)hikey.charAt(0) - (int)'A' + 1 ;
							else
						    a=	(int)hikey.charAt(0) - (int)'a' + 1  ;
							if(hikey.length()>1)
							{
							if(64<(int)hikey.charAt(1)   && (int)hikey.charAt(1)  <91)
							b=	(int)hikey.charAt(1) - (int)'A' + 1 ;
							else
							b=	(int)hikey.charAt(1) - (int)'a' +1 ;
							hi=26*a+b;
							}
							else
								hi=a;
									
							
							p.ff1=(double)(lit-low)/(double)(hi-low);
							if(p.ff1<0||p.ff1>1)
								p.ff1=0;
							
						}
					
				
				}
				
				
			}
			predicateList.add(p);
		
			
	}
	Predicate tcp=chkTcp(predicateList,predicates);
	int g = chkOr(predicates,andList,orList,literal);
	if(!orList.isEmpty()&&andList.isEmpty())
	predicateList=chkIn(orList,predicateList,andList);
	
	String fromString=sql.substring(sql.toLowerCase().lastIndexOf("from")+5)	;
	if(fromString.toLowerCase().contains("where"))
		fromString=fromString.substring(0,fromString.toLowerCase().lastIndexOf("where"));
	else if(fromString.toLowerCase().contains("order by"))
		fromString=fromString.substring(0,fromString.toLowerCase().lastIndexOf("order by"));
	ArrayList<String> fromList=new ArrayList<String>();
	if(fromString.contains(","))
	{
		String fromTokens[]=fromString.split(",");
		for(int d=0;d<fromTokens.length;d++)
			fromList.add(fromTokens[d]);
		
	}
	fromList.add(fromString);
	String minKey="";
	if(!andList.isEmpty()&&orList.isEmpty())
	{
	classifyPredicates(predicateList,matchList,screenList,fromList,andList);
	
	
	ArrayList<String>tempList=new ArrayList<String>();
	
	if(!matchList.isEmpty())
	tempList=matchList;
	
	else if(matchList.isEmpty()&&!screenList.isEmpty())
	tempList=screenList;
	
	
	if(tempList.size()>0)
	indexPred=chkIndex(tempList);
	
	int chkJoin=chkJoin(predicateList);
	
    if(!indexPred.isEmpty()&&chkJoin!=1)
    {
    minKey=orderIndex(indexPred,predicateList,screenList);
    listOrder(andList,predicateList,0);
    }
    
    	
	}
	PlanTable table=generatePlanTable(indexPred,sql,selectList,minKey,predicateList,andList,orList,matchList,screenList);
    table.printTable(out);
   
    int chkJoin=chkJoin(predicateList);
    if(chkJoin==1)
    orderJoinList(andList,orList,predicateList,table);
    predicateList=setOrSeq(predicateList);
    if(tcp!=null)
    setTcpSeq(predicateList,tcp,table);
    if(!literal.equalsIgnoreCase("no literal"))
    predicateList=chkLiteral(predicateList,literal);
    predicateList=chkAndList(predicateList,andList);
    predicateList=chkOrList(predicateList,orList);
    predicateList=setFFSeq(predicateList);
    Predicate p=new Predicate();
    p.printTable(out, predicateList);
    
	return predicateList;
	
}
public ArrayList<Predicate> setFFSeq (ArrayList<Predicate>predicateList)
{
	int chk=0;
	for(Predicate p : predicateList)
	{
		if(p.getFf1()==0)
			chk=1;
			
	}
	if(chk==1)
	{
		for(Predicate p : predicateList)
		{
			p.setSequence(0);
		}
	}
	return predicateList;
}
public void setTcpSeq(ArrayList<Predicate> predicateList,Predicate Tcp, PlanTable table)
{
	String outer = table.getLeadTable().trim();
	String left="",right="";
	String inner="";
	String revTcp=Tcp.getText();
	revTcp.replace(outer, inner);
	for(Predicate p : predicateList)
	{
		if(p.isJoin())
		{
			if(p.getText().substring(0,p.getText().lastIndexOf("=")).contains(outer))
			{
				String temp=p.getText().substring(p.getText().lastIndexOf("=")+1);
				inner=temp.substring(0,temp.lastIndexOf("."));
				left=p.getText().substring(0,p.getText().lastIndexOf("="));
				right=p.getText().substring((p.getText().lastIndexOf("=")+1));
			}
			else
			{
				String temp=p.getText().substring(0,p.getText().lastIndexOf("="));
				inner=temp.substring(0,temp.lastIndexOf("."));
				right=p.getText().substring(0,p.getText().lastIndexOf("="));
				left=p.getText().substring((p.getText().lastIndexOf("=")+1));
			}
		}
	}
	
	
	if(Tcp.getText().contains(inner.trim()))
		Tcp.setSequence(0);
	else
	{
		revTcp=revTcp.replace(left, right);
		for(Predicate p :predicateList)
		{
			if(p.getText().trim().equalsIgnoreCase(revTcp.trim()))
			{
				p.setSequence(0);
			}
		}
	}
}
public ArrayList<Predicate> setOrSeq(ArrayList<Predicate> predicateList)
{
	ArrayList<Double> ff = new ArrayList<Double>();
	int addSeq=0;
	for(Predicate p : predicateList)
	{
		if(p.getSequence()!=0)
		{
			if(p.getSequence()>=addSeq)
			addSeq=1+p.getSequence();
		}
		else
		{
			
		if(!ff.contains(p.getFf1()))
		ff.add(p.getFf1());
		}
		
	}
	Collections.sort(ff);
	
	int i =0;
	for(double j : ff)
	{
		
		for(Predicate p : predicateList)
		{
			if(p.getSequence()==0&&p.getFf1()==j)
			{
				
				if(addSeq==0)
				{
					addSeq=1;
				}
			    p.setSequence(addSeq+i);
				i++;
				
			}
			
		}
		
	}
			
	
	return predicateList;
}
public ArrayList<Predicate> chkLiteral(ArrayList<Predicate>predicateList,String literal) throws DbmsError
{
	literal=literal.trim();
	String [] literalTokens=literal.split("\\s+");
	String [] values= literalTokens[1].split("\\s+");
	Predicate p = new Predicate();
	p.setText(literalTokens[1]+" "+literalTokens[2].trim()+" "+literalTokens[3].trim());
    byte[] fbytes=literalTokens[1].getBytes();
    byte[] sbytes=literalTokens[3].getBytes();
    if(((65<=fbytes[0])&&(fbytes[0]<=90))||((97<=fbytes[0])&&(fbytes[0]<=122)))
    {
    	if(((65<=sbytes[0])&&(sbytes[0]<=90))||((97<=sbytes[0])&&(sbytes[0]<=122)))
    	{
    	
    	}
    	else
    		throw new DbmsError("ERROR: INVALID LITERAL COMBINATION");
    }
    else
    {
    	try
    	{
    		int o= Integer.parseInt(literalTokens[3]);
    		
    	}catch(NumberFormatException s)
    	{
    		throw new DbmsError("ERROR: INVALID LITERAL COMBINATION");
    	}
    }
	p.setCard1(0);
	p.setJoin(false);
	p.setInList(false);
	p.setSequence(0);
	
	if(literalTokens[1].equalsIgnoreCase(literalTokens[3]) && literalTokens[2].contains("="))
	{
		p.setType('E');
		p.setFf1(1);
	}
	else if (!literalTokens[1].equalsIgnoreCase(literalTokens[3]) && literalTokens[2].contains("="))
	{
		p.setType('E');
		p.setFf1(0);
	}
	else if (literalTokens[2].contains(">"))
	{
		p.setType('R');
		if(literalTokens[1].compareTo(literalTokens[3])>0)
			p.setFf1(1);
		else
			p.setFf1(0);
		
	}
	else if (literalTokens[2].contains("<"))
	{
		p.setType('R');
		if(literalTokens[1].compareTo(literalTokens[3])<0)
			p.setFf1(1);
		else
			p.setFf1(0);
		
	}
	
	for(Predicate t : predicateList)
	{
		if(p.getFf1()==0)
		{
			t.setSequence(0);
		}
	
			
	}
	predicateList.add(p);
	return predicateList;
}
public ArrayList<Predicate> chkOrList(ArrayList<Predicate> predicateList, ArrayList<String> orList)
{

	int i=0,j=0;
	ArrayList<String> setSeq = new ArrayList<String>();
	for(i=0;i<orList.size();i++)
	{
		String first=orList.get(i);
		first=first.trim();
		for(j=i+1;j<orList.size();j++)
		{
			String second=orList.get(j);
			second=second.trim();
			String firstValue=first.substring(0, first.lastIndexOf(" "));
		    String secondValue=second.substring(0, second.lastIndexOf(" "));
			
			if(first.substring(0, firstValue.lastIndexOf(" ")).equalsIgnoreCase(second.substring(0, secondValue.lastIndexOf(" "))))
			{
			
				 if(first.contains("<")&& second.contains("<"))
				{
					String firstVal=first.substring(first.lastIndexOf("<")+1);
					String secondVal=second.substring(second.lastIndexOf("<")+1);
					
					if(firstVal.compareToIgnoreCase(secondVal)<=0)
					{

				      if(!setSeq.contains(second))
					  setSeq.add(second);
				   
					}
					else
					{
						if(!setSeq.contains(first))
							  setSeq.add(first);
					}
				}
				
				else if(first.contains(">")&& second.contains(">"))
				{
					String firstVal=first.substring(first.lastIndexOf(">")+1);
					String secondVal=second.substring(second.lastIndexOf(">")+1);
					
					if(firstVal.compareToIgnoreCase(secondVal)<=0)
					{

				     
				      if(!setSeq.contains(second))
					  setSeq.add(second);
					}
					else
					{
						if(!setSeq.contains(first))
							  setSeq.add(first);
					}
				}
				else if(first.contains("<")&& second.contains(">"))
				{
					String firstVal=first.substring(first.lastIndexOf("<")+1);
					String secondVal=second.substring(second.lastIndexOf(">")+1);
					
					if(firstVal.compareToIgnoreCase(secondVal)<=0)
					{

				      if(!setSeq.contains(first))
					  setSeq.add(first);
				      if(!setSeq.contains(second))
					  setSeq.add(second);
					}
				}
				else if(first.contains(">")&& second.contains("<"))
				{
					String firstVal=first.substring(first.lastIndexOf(">")+1);
					String secondVal=second.substring(second.lastIndexOf("<")+1);
					if(firstVal.compareToIgnoreCase(secondVal)>0)
					{

				      if(!setSeq.contains(first))
					  setSeq.add(first);
				      if(!setSeq.contains(second))
					  setSeq.add(second);
					}
				}
				
			    
			}
			
		}
	}
	for(String s : setSeq)
	{
		for(Predicate p: predicateList)
		{
			int currVal=0;
			if(p.getText().trim().equalsIgnoreCase(s.trim()))
			{
				currVal=p.getSequence();
				p.setSequence(0);
				
			   for(int k =0;k<predicateList.size();k++)
			    {
				   Predicate r= predicateList.get(k);
				   if(r.getSequence()!=0&&currVal<r.getSequence())
					r.setSequence(r.getSequence()-1);
			    }
			  
			}
		}
	}
	return predicateList;

}
public ArrayList<Predicate> chkAndList(ArrayList<Predicate> predicateList,ArrayList<String> andList)
{
	int i=0,j=0;
	ArrayList<String> setSeq = new ArrayList<String>();
	for(i=0;i<andList.size();i++)
	{
		String first=andList.get(i);
		first=first.trim();
		for(j=i+1;j<andList.size();j++)
		{
			String second=andList.get(j);
			second=second.trim();
			String firstValue=first.substring(0, first.lastIndexOf(" "));
		    String secondValue=second.substring(0, second.lastIndexOf(" "));
			
			if(first.substring(0, firstValue.lastIndexOf(" ")).equalsIgnoreCase(second.substring(0, secondValue.lastIndexOf(" "))))
			{
				
				
				if(first.contains("=")&& second.contains("="))
				{
					String firstVal=first.substring(first.lastIndexOf("=")+1);
					String secondVal=second.substring(second.lastIndexOf("=")+1);
					
					if(!firstVal.contains("."))	
					{
				      if(!setSeq.contains(first))
					  setSeq.add(first);
					}
					if(!secondVal.contains(".")&&!firstVal.contains("."))
					{
				       if(!setSeq.contains(second))
					   setSeq.add(second);
					}
				}
				else if(first.contains(">")&& second.contains(">"))
				{
				if(!setSeq.contains(first))
					setSeq.add(first);
				if(!setSeq.contains(second))
					setSeq.add(second);
				}
				else if(first.contains("<")&& second.contains("<"))
				{
				if(!setSeq.contains(first))
					setSeq.add(first);
				if(!setSeq.contains(second))
					setSeq.add(second);
				}
				else if(first.contains("<")&& second.contains(">"))
				{
					String firstVal=first.substring(first.lastIndexOf("<")+1);
					String secondVal=second.substring(second.lastIndexOf(">")+1);
					
					if(firstVal.compareToIgnoreCase(secondVal)<=0)
					{

				      if(!setSeq.contains(first))
					  setSeq.add(first);
				      if(!setSeq.contains(second))
					  setSeq.add(second);
					}
				}
				else if(first.contains(">")&& second.contains("<"))
				{
					String firstVal=first.substring(first.lastIndexOf(">")+1);
					String secondVal=second.substring(second.lastIndexOf("<")+1);
					if(firstVal.compareToIgnoreCase(secondVal)>0)
					{

				      if(!setSeq.contains(first))
					  setSeq.add(first);
				      if(!setSeq.contains(second))
					  setSeq.add(second);
					}
				}
				
			    
			}
			
		}
	}
	for(String s : setSeq)
	{
		for(Predicate p: predicateList)
		{
			int currVal=0;
			if(p.getText().trim().equalsIgnoreCase(s.trim()))
			{
				currVal=p.getSequence();
				if(!p.getDescription().equalsIgnoreCase("TCP"))
				{
				p.setSequence(0);
				for(int k =0;k<predicateList.size();k++)
				{
					Predicate r= predicateList.get(k);
					if(r.getSequence()!=0&&currVal<r.getSequence())
						r.setSequence(r.getSequence()-1);
				}
				}
			}
			
		}
	}
	return predicateList;
}
public PlanTable generatePlanTable(HashMap<String,Integer> indexPred,String sql,HashMap<String,ArrayList<String>> selectList,String minKey,ArrayList<Predicate>predicateList,ArrayList<String>andList,ArrayList<String>orList,ArrayList<String>matchList,ArrayList<String>screenList)
{
	PlanTable p = new PlanTable();
	String fromList=null;
	String indexName=null;
	String temp=sql.toLowerCase();
	if(temp.contains("where"))
		fromList=sql.trim().substring(temp.lastIndexOf("from")+4,temp.lastIndexOf("where"));
	else if(temp.contains("order by")&& !temp.contains("where"))
		fromList=sql.trim().substring(temp.lastIndexOf("from")+4,temp.lastIndexOf("order by"));
	else if(!temp.contains("order by")&& !temp.contains("where"))
		fromList=sql.trim().substring(temp.lastIndexOf("from")+4);
		
	String tableTokens[]=fromList.split(",");
	int chkJoin=chkJoin(predicateList);
	if(chkJoin==1)
	{
		String joinPred="";
		String res=getLeadingTable(predicateList,andList,orList,indexPred,screenList);
		
		p.setPrefetch('S');
		String [] resTokens= res.split("~");
		if(resTokens.length>4)
		{
			if(resTokens[0]!=null)
				p.setLeadTable(resTokens[0].trim());
			if(resTokens[1]!=null&&resTokens[2]!=null)
				p.setAccessName(resTokens[1].trim()+resTokens[2].trim());
			if(resTokens[3]!=null)
				p.setMatchCols(Integer.parseInt(resTokens[3].trim()));
			if(resTokens[4]!=null)
				p.setTable1Card(Integer.parseInt(resTokens[4].trim()));
			if(resTokens[5]!=null)
				p.setTable2Card(Integer.parseInt(resTokens[5].trim()));
			p.setAccessType('I');
			
			int chk=chkIndexOnly(resTokens[1].trim()+"."+"_"+resTokens[2],selectList,andList,orList);
			if(chk==1)
				p.setIndexOnly('Y');
			if(temp.contains("order by"))
			{
			   String chkOrderBy=chkOrderBy(sql);
			   if(chkOrderBy!="")
			   {
				   if(chkOrderBy.equalsIgnoreCase(resTokens[1].trim()+"_"+resTokens[2].trim()))
				   {
				     p.setSortC_orderBy('N');
				   }
				   else
					   p.setSortC_orderBy('Y');
			   }
			   else
				   p.setSortC_orderBy('Y');
			}
		}
		else
		{
			if(resTokens[0]!=null)
				p.setLeadTable(resTokens[0].trim());
			if(resTokens[1]!=null)
				p.setTable1Card(Integer.parseInt(resTokens[1].trim()));
			if(resTokens[2]!=null)
				p.setTable2Card(Integer.parseInt(resTokens[2].trim()));
			 p.setPrefetch('S');
			if(temp.contains("order by"))
			{
			   String chkOrderBy=chkOrderBy(sql);
			   String orderTokens[]=sql.substring(sql.toLowerCase().lastIndexOf("order by")+9).split(",");
			   if(chkOrderBy!="")
			   {
				 p.setAccessName(chkOrderBy.substring(0,chkOrderBy.lastIndexOf("_"))+chkOrderBy.substring(chkOrderBy.lastIndexOf("_")+1));
				 p.setAccessType('I');
				 p.setSortC_orderBy('N');
				 p.setPrefetch(' ');
			//	 p.setMatchCols(orderTokens.length);
			
			   }
			   else
			   {
				   p.setSortC_orderBy('Y');
				   p.setPrefetch('S');
				     
						   
			   }
			
				   
			}	
			
		}
			
		
	}
	else
	{
		String tableName=tableTokens[0];
		for(Table tab : tables)
		{
			if(tableName.trim().equalsIgnoreCase(tab.getTableName())&&!tab.delete)
			{
				p.table1Card=tab.getTableCard();
				break;
			}
		}
		if(tableTokens.length>1)
		{
			String tableName2=tableTokens[1];
			for(Table tab : tables)
			{
				if(tableName2.trim().equalsIgnoreCase(tab.getTableName())&&!tab.delete)
				{
					p.table2Card=tab.getTableCard();
					break;
				}
			}
		}
		
		if(indexPred.isEmpty())
		{
			p.setPrefetch('S');
			p.setAccessType('R');
			if(!temp.contains("order by"))
			{
				if(tableTokens.length==1)
				{
				String temp1=getIndexOnly(selectList,tableName);
				if(temp1.length()>=1)
				{
					int res=chkIndexOnly(temp1,selectList,andList,orList);
					if(res==1)
					{
				       p.setPrefetch(' ');
				       p.setAccessName(temp1.trim().substring(0,temp1.lastIndexOf("."))+temp1.trim().substring(temp1.lastIndexOf("_")+1));
				       p.setIndexOnly('Y');
				       p.setAccessType('I');
				    
					}
				}
				}
				
			}
			if(temp.contains("order by"))
			{
				String mKey=chkOrderBy(sql);
				
				if(mKey!="")
				{
				    p.setAccessName(mKey.substring(0,mKey.lastIndexOf("_"))+mKey.substring(mKey.lastIndexOf("_")+1));
				    p.setAccessType('I');
				    p.setSortC_orderBy('N');
				    String orderTokens[]=sql.substring(sql.toLowerCase().lastIndexOf("order by")+9).split(",");
				//    p.setMatchCols(orderTokens.length);
				    p.setPrefetch(' ');
				    
				   if(selectList.size()==1)
				   {
					  int chk= chkIndexOnly(mKey.substring(0,mKey.lastIndexOf("_"))+"._"+mKey.substring(mKey.lastIndexOf("_")+1),selectList,andList,orList);
				      if(chk==1)
				    	  p.setIndexOnly('Y');
				    }
				 
			       }
			   else
			   {
				  
				    	p.setSortC_orderBy('Y');
				    	p.setPrefetch('S');
				    	if(tableTokens.length==1)
						{
						String temp1=getIndexOnly(selectList,tableName);
						if(temp1.length()>=1)
						{
							int res=chkIndexOnly(temp1,selectList,andList,orList);
							if(res==1)
							{
						       p.setPrefetch(' ');
						       p.setAccessName(temp1.trim().substring(0,temp1.lastIndexOf("."))+temp1.trim().substring(temp1.lastIndexOf("_")+1));
						       p.setIndexOnly('Y');
						       p.setAccessType('I');
							}
						}
						}
				    
						   
			   }
			}
				   
			else
			{
			
			p.setSortC_orderBy('N');
			}
			
		}
		
		else
		{
			p.setPrefetch(' ');
			p.setAccessType('I');
			Set<String> keys=indexPred.keySet();
			for(String s : keys)
			{
				indexName=s;
				Integer matchCols=indexPred.get(s);
				p.setMatchCols(matchCols);
				if(s.contains("IN")||s.contains("in")||s.contains("In"))
				{
					String values=s.substring(s.lastIndexOf("(")+1, s.lastIndexOf(")"));
					String[] valueTokens=values.split(",");
				
					if(valueTokens.length>1)
					p.setAccessType('N');
					else
						p.setAccessType('I');
					
				}
				
			}
			if(minKey!=null)
			{
				p.setAccessName(minKey.substring(0,minKey.lastIndexOf("_"))+minKey.substring(minKey.lastIndexOf("_")+1));
				for(String s1: keys)
				{
					if(minKey.substring(minKey.lastIndexOf("_")+1).equalsIgnoreCase(s1.substring(s1.lastIndexOf("_")+1)))
						indexName=s1;
				}
				
			}
			else
			p.setAccessName(indexName.substring(0,indexName.lastIndexOf("."))+indexName.substring(indexName.lastIndexOf("_")+1));
			int chk=chkIndexOnly(indexName,selectList,andList,orList);
			if(chk==1)
				p.setIndexOnly('Y');
			else
				p.setIndexOnly('N');
			
			if(sql.contains(" order by ")||sql.contains("ORDER BY ")|| sql.contains("Order by "))
			{
				String name=sql.substring(temp.lastIndexOf("order by")+9);
				name=name.trim();
				String[] orderTokens = null ;
				String table="";
			    if(name.contains(","))
			    	orderTokens=name.split(",");
			    else
			    {
			    	orderTokens=new String[1];
			    	orderTokens[0]=name;
			    }
			    table=orderTokens[0].substring(0,orderTokens[0].lastIndexOf(".")).trim();
			    for(int z=0;z<orderTokens.length;z++)
			    {
			    	orderTokens[z]=orderTokens[z].trim();
			    	String [] tempTokens=orderTokens[z].split("\\s+");
			    	if(tempTokens.length>1)
			    	{
			    	  if(tempTokens[1].trim().equals("D")||tempTokens[1].trim().equals("A"))
			    	  orderTokens[z]=tempTokens[0].substring(tempTokens[0].lastIndexOf(".")+1)+"_"+tempTokens[1];
			        }
			    	else
			    	 orderTokens[z]=orderTokens[z].substring(orderTokens[z].lastIndexOf(".")+1)+"_A";
			    }
			   HashMap<String,ArrayList<String>>indexList=getIndexOrder(table);
			   ArrayList<String> list=indexList.get(indexName.substring(indexName.lastIndexOf("_")+1));
			   
		    	String order="";
	    		for(int a=0;a<orderTokens.length;a++)
	    			order=order+orderTokens[a];
	    		String indexOrder="";
	    		for(String h : list)
	    			indexOrder=indexOrder+h;
	    		String revOrder=order.replace("_D", "_~");
	    		revOrder=revOrder.replace("_A", "_D");
	    		revOrder=revOrder.replace("_~", "_A");
	    		
	    		String[] reverse=new String [orderTokens.length];
	    		for(int a =0;a<orderTokens.length;a++)
	    		{
	    			if(orderTokens[a].contains("_A"))
	    				reverse[a]=orderTokens[a].replace("_A", "_D");
	    			else
	    				reverse[a]=orderTokens[a].replace("_D", "_A");
	    		}
	    		
			   if(list.size()==orderTokens.length)
			   {
				   if(indexOrder.equalsIgnoreCase(order)||indexOrder.equalsIgnoreCase(revOrder))
					   p.setSortC_orderBy('N');
				   else
					   p.setSortC_orderBy('Y');
			   }
			   else if(list.size()>orderTokens.length)
			   {
				   
				   if(indexOrder.contains(order))
				   {
					   String curr="";
					   int cnt=0;
					   for(int l=0;l<list.size();l++)
					   {
						  
						   int pos=0;
						    curr = list.get(l);
						   if(order.contains(curr))
						   {
							   cnt++;
						   }
						   else
						   {
							  
							   int flag=-1;
							   for(String m : matchList)
							   {
								   m=m.substring(0, m.lastIndexOf(" "));
								   if(m.substring(0, m.lastIndexOf(" ")).equalsIgnoreCase(table+"."+curr.substring(0,curr.lastIndexOf("_"))))
									   flag=1;
							   }
							   if(flag!=1)
							   {
								   if(cnt<orderTokens.length)
								   {
									   p.setSortC_orderBy('Y');
									   break;
								   }
									   
							   }
						   }
						   
					   }
					   
					}
				   
				   else 
				   {
					   p.setSortC_orderBy('Y');
					  
				   }		   
		           }
			   else 
			   {
				   p.setSortC_orderBy('Y');
				  
			   }
							   
				}
			
			}
	}
			
			
	return p;
}
public String chkOrderBy(String sql)
{
	
	String temp=sql.toLowerCase();
	String tempTable="";
	int flag=-1;
	String mKey="";
	String name=sql.substring(temp.lastIndexOf("order by")+9);
	name=name.trim();
	String[] orderTokens = null ;
	String table="";
    if(name.contains(","))
    {
    	
    	orderTokens=name.split(",");
     	
    }
    else
    {
    	orderTokens=new String[1];
    	orderTokens[0]=name;
    }
    for(int r =0;r<orderTokens.length;r++)
    {
    	tempTable=orderTokens[r].substring(0,orderTokens[r].lastIndexOf(".")).trim();
    	for(int a =r+1;a<orderTokens.length;a++)
    	{
    		if(tempTable.equalsIgnoreCase(orderTokens[a].trim().substring(0,orderTokens[a].lastIndexOf(".")).trim()))
    		{
    			flag=0;
    			break;
    		}
    	}
    }
    if(flag!=0)
    {
        table=orderTokens[0].substring(0,orderTokens[0].lastIndexOf(".")).trim();
    	for(int z=0;z<orderTokens.length;z++)
    	{
    		
    		orderTokens[z]=orderTokens[z].trim();
    		String [] tempTokens=orderTokens[z].split("\\s+");
    		if(tempTokens.length>1)
    		{
    			
    		if(tempTokens[1].trim().equals("D")||tempTokens[1].trim().equals("A"))
    			orderTokens[z]=tempTokens[0].substring(tempTokens[0].lastIndexOf(".")+1)+"_"+tempTokens[1];
    		
            }
    		else
    			orderTokens[z]=orderTokens[z].substring(orderTokens[z].lastIndexOf(".")+1)+"_A";
    	}
     
   
    HashMap<String,ArrayList<String>>indexList=getIndexOrder(table.trim());
    Set<String> keys=indexList.keySet();
    
  
    for(String s : keys)
    {
    	ArrayList<String> list=indexList.get(s);
    	String order="";
    	
		for(int a=0;a<orderTokens.length;a++)
			order=order+orderTokens[a];
		String indexOrder="";
		for(String h : list)
			indexOrder=indexOrder+h;
		String revOrder=indexOrder.replace("_D", "_~");
		revOrder=revOrder.replace("_A", "_D");
		revOrder=revOrder.replace("_~", "_A");
		
		
    	if(list.size()==orderTokens.length)
    	{
    		
    		if(order.equalsIgnoreCase(indexOrder))
    		{
    			
    			mKey=s;
    			break;
    		}
    		else if(order.equalsIgnoreCase(revOrder))
    		{
    			mKey=s;
    			
    		}
    	}
    	else if(list.size()>orderTokens.length)
    	{
    		if(indexOrder.contains(order))
    		{
    			int pos=indexOrder.indexOf(order);
    			if(pos==0)
    			{
    				mKey=s;
    				break;
    			}
    			
    			
    		}
    		else if(revOrder.contains(order))
    		{
    			int pos=revOrder.indexOf(order);
    			if(pos==0)
    				mKey=s;	
    		}
    		
    	}
    }
    }
    else
    	mKey="";
    if(mKey!="")
    	mKey=table+"_"+mKey;
	return (mKey);
}
public String getIndexOnly(HashMap<String,ArrayList<String>> selectList,String tableName)
{
	String index="";
	int i=0,j=0,pos=-2,flag=1,cnt=0;
	HashMap<String, ArrayList<String>> indexList= getIndexList(tableName.trim());
	Set<String> indexes=indexList.keySet();
	ArrayList<Integer> loc=new ArrayList<Integer>();
	ArrayList<String> names = new ArrayList<String>();
	ArrayList<String> colList= selectList.get(tableName.trim());
	
	for(String in:indexes)
	{
		flag=1;
		ArrayList<String> list=indexList.get(in);
		for(i=0;i<colList.size()&&flag==1;i++)
		{
			pos=list.indexOf(colList.get(i).trim());
			if(pos==-1)
				break;
			else
			{
				if(colList.size()==1)
				{
					loc.add(pos);
					names.add(in);
				}
				else
				{
				for(j=i+1;j<colList.size();j++)
				{
					int innerpos=list.indexOf(colList.get(j).trim());
					if(innerpos==-1)
					{
						flag=0;
						break;
					}
					else
					{
						pos=pos+innerpos;
						if(j+1==colList.size())
						{
							loc.add(pos);
							names.add(in);
						}
					}
				}
				}
				
					
			}
			
		}
		
				
	}
	
	if(loc.size()==1)
	{
		index=tableName.trim()+"._"+names.get(0).trim();
	}
	else if(!loc.isEmpty()&& loc.size()>1)
	{
		int min = Collections.min(loc);
		index=tableName.trim()+"._"+names.get(loc.indexOf(min)).trim();
		
	}
	return index;
}
public String getLeadingTable(ArrayList<Predicate> predicateList,ArrayList<String> andList,ArrayList<String> orList,HashMap<String,Integer>indexPred,ArrayList<String> screenList)
{
	String leadingTable="";
	String joinPred="";
	
	if(predicateList.size()==1)
	{
		
		Predicate p = predicateList.get(0);
		if(p.isJoin())
		{
			
			joinPred=p.getText();
			String first= joinPred.substring(0,joinPred.lastIndexOf("="));
			String firstTable=first.substring(0,first.lastIndexOf("."));
			String second= joinPred.substring(joinPred.lastIndexOf("=")+1);
			String secondTable=second.substring(0,second.lastIndexOf("."));
			HashMap<String,ArrayList<String>> firstList=getIndexList(firstTable);
			HashMap<String,ArrayList<String>> secondList=getIndexList(secondTable);
			
			int minfPos=99999, minsPos=99999;
			String minfKey="",minsKey="";
			Set<String>firstKeys=firstList.keySet();
			Set<String>secondKeys=secondList.keySet();
			for(String s : firstKeys)
			{
					
			  ArrayList<String> fValues= firstList.get(s);
			  if(fValues.contains(first.trim().substring(first.lastIndexOf("."))))
			  {
				 int pos=fValues.indexOf(first.trim().substring(first.lastIndexOf(".")));
				 if(pos<minfPos)
				 {
					minfPos=pos;
					minfKey=s;							
				 }
			  }
			}
			for(String s : secondKeys)
			{
				ArrayList<String> sValues= secondList.get(s);
				if(sValues.contains(second.trim().substring(second.lastIndexOf("."))))
				{
					int pos=sValues.indexOf(second.trim().substring(second.lastIndexOf(".")));
					if(pos<minsPos)
					{
						minsPos=pos;
						minsKey=s;
							
					}
				}
				
				int card1=getTabCard(firstTable.trim());
				int card2=getTabCard(secondTable.trim());
				double firstff=p.getFf1();
				double secondff=p.getFf2();
				
				if(minfPos<minsPos)
				leadingTable=secondTable+"~"+firstTable+"~"+minfKey+"~1~"+card1+"~"+card2;
				else if(minfPos>minsPos)
					leadingTable=firstTable+"~"+secondTable+"~"+minsKey+"~1~"+card1+"~"+card2;
				if(minfPos==minsPos)
				{
					
					if(minfPos!=99999)
					{
						
					       if((card1)>(card2))
						   leadingTable=secondTable+"~"+firstTable+"~"+minfKey+"~1~"+card1+"~"+card2;
					       else
						   leadingTable=firstTable+"~"+secondTable+"~"+minsKey+"~1~"+card1+"~"+card2;
					
				       
					}
					else
					{
						if((card1)<(card2))
						{
							leadingTable=secondTable+"~"+card1+"~"+card2;
						}
						else
							leadingTable=firstTable+"~"+card1+"~"+card2;
					}
				}
			}
			
		}
	}
	else
	{
		Predicate t =new Predicate();
		for(Predicate p :predicateList)
		{
			if(p.isJoin())
				t=p;
		}
		joinPred=t.getText();
		String first= joinPred.substring(0,joinPred.lastIndexOf("="));
		String firstTable=first.substring(0,first.lastIndexOf("."));
		String second= joinPred.substring(joinPred.lastIndexOf("=")+1);
		String secondTable=second.substring(0,second.lastIndexOf("."));
		HashMap<String,ArrayList<String>> firstList=getIndexList(firstTable);
		HashMap<String,ArrayList<String>> secondList=getIndexList(secondTable);
		HashMap<String,ArrayList<Double>> andMap= new HashMap<String,ArrayList<Double>>();
		HashMap<String,ArrayList<Double>> orMap= new HashMap<String,ArrayList<Double>>();
		ArrayList<Double> andFirstList = new ArrayList<Double>();
		ArrayList<Double> andSecondList = new ArrayList<Double>();
		ArrayList<Double> orFirstList = new ArrayList<Double>();
		ArrayList<Double> orSecondList = new ArrayList<Double>();
		int card1 = getTabCard(firstTable.trim());
		int card2 = getTabCard(secondTable.trim());
		for(String s : andList)
		{
			for(Predicate p: predicateList)
			{
				s=s.trim();
				
				if(s.contains(firstTable.trim()))
				{
				 if(s.equalsIgnoreCase(p.getText().trim())&& !p.isJoin())
					andFirstList.add(p.getFf1());
				 else if(p.getDescription().contains("IN")||p.getDescription().contains("In")||p.getText().contains("in"))
				 {
					if(s.equalsIgnoreCase(p.getDescription().trim()))
						andFirstList.add(p.getFf1());
				}
				}
				else if(s.contains(secondTable.trim()))
				{
					s=s.trim();
					if(s.equalsIgnoreCase(p.getText().trim()))
					{
						if(!p.isJoin())
						andSecondList.add(p.getFf1());
						
					}
					else if(p.getDescription().contains("IN")||p.getDescription().contains("In")||p.getText().contains("in"))
					{
						if(s.equalsIgnoreCase(p.getDescription().trim()))
							andSecondList.add(p.getFf1());
					}
				}
			}
		}
		
		andMap.put(firstTable, andFirstList);
		andMap.put(secondTable, andSecondList);
		for(String s : orList)
		{
			for(Predicate p: predicateList)
			{
				s=s.trim();
				if(s.contains(firstTable.trim()))
				{
					
				if(s.equalsIgnoreCase(p.getText().trim())&& !p.isJoin())
					orFirstList.add(p.getFf1());
				else if(p.getDescription().contains("IN")||p.getDescription().contains("In")||p.getText().contains("in"))
				{
					if(s.equalsIgnoreCase(p.getDescription().trim()))
						orFirstList.add(p.getFf1());
				}
				}
				else if(s.contains(secondTable.trim()))
				{
					if(s.equalsIgnoreCase(p.getText().trim())&& !p.isJoin())
						orSecondList.add(p.getFf1());
					else if(p.getDescription().contains("IN")||p.getDescription().contains("In")||p.getText().contains("in"))
					{
						if(s.equalsIgnoreCase(p.getDescription().trim()))
							orSecondList.add(p.getFf1());
					}
				}
			}
		}
		orMap.put(firstTable, orFirstList);
		orMap.put(secondTable, orSecondList);
		
			int minfPos=99999, minsPos=99999;
			String minfKey="",minsKey="";
			Set<String>firstKeys=firstList.keySet();
			Set<String>secondKeys=secondList.keySet();
			for(String s : firstKeys)
			{
				
				ArrayList<String> fValues= firstList.get(s);
				
				if(fValues.contains(first.trim().substring(first.lastIndexOf("."))))
				{
					int pos=fValues.indexOf(first.trim().substring(first.lastIndexOf(".")));
					
					if(pos<minfPos)
					{
						
						minfPos=pos;
						minfKey=s;
						
					}
				}
			}
			
			for(String s : secondKeys)
			{
				ArrayList<String> sValues= secondList.get(s);
				if(sValues.contains(second.trim().substring(second.lastIndexOf("."))))
				{
					int pos=sValues.indexOf(second.trim().substring(second.lastIndexOf(".")));
					if(pos<minsPos)
					{
						minsPos=pos;
						minsKey=s;
						
					}
				}
			}
			
			if(minfPos<minsPos)
			leadingTable=secondTable.trim()+"~"+firstTable.trim()+"~"+minfKey+"~1~"+card1+"~"+card2;
			else if(minfPos>minsPos)
				leadingTable=firstTable.trim()+"~"+secondTable.trim()+"~"+minsKey+"~1~"+card1+"~"+card2;
			if(minfPos==minsPos)
			{
				
				double firstff= t.getFf1();
				double secondff=t.getFf2();
				
				if(!andFirstList.isEmpty())
				{
					for(Double d : andFirstList)
					firstff=firstff*d;
				}
				if(!andSecondList.isEmpty())
				{
					for(Double d : andSecondList)
					secondff=secondff*d;
				}
				if(!orFirstList.isEmpty())
				{
					for(Double d : orFirstList)
					firstff=firstff+d;
				}
				if(!orSecondList.isEmpty())
				{
					for(Double d : orSecondList)
					secondff=secondff+d;
				}
			
				if(minfPos!=99999)
				{
					int matchCols1=1,matchCols2=1;
					
					ArrayList<String>fValues=firstList.get(minfKey);
					ArrayList<String>sValues=secondList.get(minsKey);
					String secondScol=sValues.get(minsPos+1);
					
					for(String str:screenList)
					{
						
					    if(str.contains(secondTable.trim()+"."+secondScol)&&str.contains(minsKey.trim()))
						matchCols2=matchCols2+1;
					}
					String firstScol=fValues.get(minfPos+1);
					for(String str:screenList)
					{
						
					if(str.contains(firstTable.trim()+"."+firstScol)&&str.contains(minfKey.trim()))
						matchCols1=matchCols1+1;
					}
					
					
					if(matchCols1>matchCols2)
						leadingTable=secondTable+"~"+firstTable+"~"+minfKey+"~"+matchCols1+"~"+card1+"~"+card2;
					else if(matchCols1<matchCols2)
						leadingTable=firstTable+"~"+secondTable+"~"+minsKey+"~"+matchCols2+"~"+card1+"~"+card2;
					else if(matchCols1==matchCols2)
					{
						
					
				        if((firstff)>(secondff))
					     leadingTable=firstTable+"~"+secondTable+"~"+minsKey+"~1~"+card1+"~"+card2;
				        else
					     leadingTable=secondTable+"~"+firstTable+"~"+minfKey+"~1~"+card1+"~"+card2;
					}
				}
				else
				{
					if((card1*firstff)<(card2*secondff))
					{
						leadingTable=secondTable+"~"+card1+"~"+card2;
					}
					else
						leadingTable=firstTable+"~"+card1+"~"+card2;
				}
			
			}
		}
		
		
return leadingTable;
}
public int chkIndexOnly(String indexName,HashMap<String,ArrayList<String>> selectList,ArrayList<String>andList,ArrayList<String>orList)
{
	int res=1,i=0;
	indexName=indexName.trim();
	HashMap<String,ArrayList<String>> list=getIndexList(indexName.substring(0,indexName.lastIndexOf(".")));
	ArrayList<String> cols=list.get(indexName.substring(indexName.lastIndexOf("_")+1));
	ArrayList<String> selectCols=selectList.get(indexName.substring(0,indexName.lastIndexOf(".")));
	if(selectCols==null)
		res=0;
	else
	{
	for(String s : selectCols)
	{
		if(cols.contains(s.trim()))
		res=res*1;
		else
			res=res*0;
	}
	}
	if(res!=0)
		res=chkWhere(andList,orList,indexName,cols);
	return res;
}
public int chkWhere(ArrayList<String> andList,ArrayList<String> orList,String indexName,ArrayList<String> list )
{
	int res=-1,flag=-1;
	ArrayList<String>indexList=new ArrayList<String>();
	String table=indexName.substring(0,indexName.lastIndexOf("."));
	table=table.trim();
	for(String l : list)
		indexList.add(table+"."+l);
	if(andList.isEmpty()&&orList.isEmpty())
		res=1;
	if(!andList.isEmpty())
	{
	    for(String a : andList)
	    {
	    	if(flag==0)
	    		break;
	    	for(String b : indexList)
	    	{
	    		
	    		if(a.contains(b))
	    		{
	    			flag=1;
	    			break;
	    		}
	    		else
	    		{
	    			flag=0;
	    			
	    		}
	    			
	    	}
	    }
	}
	if(flag==0)
		res=0;
	else
	{
		if(!orList.isEmpty())
		{
		    for(String a : orList)
		    {
		    	if(flag==0)
		    		break;
		    	for(String b : indexList)
		    	{
		    		if(a.contains(b))
		    		{
		    			flag=1;
		    			
		    		}
		    		else
		    		{
		    			flag=0;
		    			break;
		    		}
		    			
		    	}
		    }
		}
		
	}
	if(flag==0)
		res=0;
	else
		res=1;
	return res;
}
public void listOrder(ArrayList<String>andList,ArrayList<Predicate>predicateList,int c)
{
  	ArrayList<String> ff = new ArrayList<String>();
  	double [] arr= new double[10];
  	for(int j=0;j<10;j++)
  		arr[j]=9999;
  	int i=0,count=0,cntSet=c;
  	for(String pred : andList)
  	{
  		
           pred=pred.trim();
           for(Predicate p : predicateList)
           {
        	   
        	   if(p.getText().trim().equalsIgnoreCase(pred))
        	   {
        		   
        		   if(p.getSequence()!=0)
                       cntSet=cntSet+1;
        		   else
        		   {
        			  
        			   int val=predicateList.indexOf(p);
        			   ff.add(val+ "_"+p.ff1);
        			   arr[i]=p.ff1;
        			   i++;
        		   }
        		   
        	   }
        	   else if(pred.contains("IN")||pred.contains("In")||pred.contains("in"))
        	  	{
        		   
        		   if(pred.contains("_"))
        		   pred=pred.substring(0,pred.lastIndexOf("_"));
        		   if(p.getDescription().trim().equalsIgnoreCase(pred))
        		   {
        		   if(p.getSequence()!=0)
                       cntSet=cntSet+1;
           		   else
           		   {
           			  
           			   int val=predicateList.indexOf(p);
           			   ff.add(val+ "_"+p.ff1);
           			   arr[i]=p.ff1;
           			   i++;
           		   }
        		   }
        	  	}
           }
  	}
  	
  	count =i;
  	Arrays.sort(arr);
  	for(i=0;i<count;i++)
  	{
  		double curr=arr[i];
  		int cnt=1;
  		for(String s : ff)
  		{
  			
  			String temp=s.substring(s.lastIndexOf("_")+1);
  			double val=Double.parseDouble(temp.trim());
  			
  			if(curr==val)
  			{
  				
  				String index=s.substring(0, s.lastIndexOf("_"));
  				Predicate p=predicateList.get(Integer.parseInt(index));
  				if(p.getSequence()==0)
  				p.setSequence(i+cntSet+cnt);
  				cnt++;
  			
  				
  			}
  		}
  	}
}
public int chkJoin(ArrayList<Predicate> predicateList)
{
	int res=0;
	for(Predicate p : predicateList)
	{
		if(p.isJoin())
		{
			res=1;
			break;
		}
		
	}
	return res;
}
public void orderJoinList(ArrayList<String> andList,ArrayList<String> orList,ArrayList<Predicate> predicateList,PlanTable table)
{
	Predicate joinPred=null;
	ArrayList<String> outerList= new ArrayList<String>();
	ArrayList<String> innerList= new ArrayList<String>();
	ArrayList<Predicate> inner= new ArrayList<Predicate>();
	for(Predicate p : predicateList)
		p.setSequence(0);
	String leadingTable=table.getLeadTable();
	String innerTable="";
	String accesName=table.getAccessName();
	for(Predicate p:predicateList)
	{
		if(p.isJoin())
			joinPred=p;
		else if(p.getText().contains(leadingTable)&&!p.isJoin())
		{
			outerList.add(p.getText());
		}
		else if(!p.getText().contains(leadingTable)&&!p.isJoin())
		{
			if(andList.contains(p.getText().trim()))
			{
			innerList.add(p.getText());
			inner.add(p);
			}
		}
	}
	if(outerList.isEmpty())
		joinPred.setSequence(1);
	else
	{
		listOrder(outerList,predicateList,0);
		joinPred.setSequence(outerList.size()+1);
	}
		String[] tok=joinPred.getText().split("=");
	if(tok[0].contains(leadingTable))
		innerTable=tok[1].substring(0,tok[1].lastIndexOf("."));
	else
		innerTable=tok[0].substring(0,tok[1].lastIndexOf("."));
	
	
	if(accesName!="")
	{
	String indexName=accesName.substring(accesName.lastIndexOf(innerTable)+innerTable.length());
	HashMap<String,ArrayList<String>> indexList=getIndexList(innerTable);
	ArrayList<String> indexCols=indexList.get(indexName);
	ArrayList<Integer> pos=new ArrayList<Integer>();
	for(String p : innerList)
	{
		String predCol="";
		String temp="";
		if(p.contains("="))
		temp=p.substring(0,p.lastIndexOf("="));
		else if(p.contains(">"))
			temp=p.substring(0,p.lastIndexOf(">"));
		else if(p.contains("<"))
			temp=p.substring(0,p.lastIndexOf("<"));
		else if(p.contains("IN"))
			temp=p.substring(0,p.lastIndexOf("IN"));
		else if(p.contains("In"))
			temp=p.substring(0,p.lastIndexOf("In"));
		else if(p.contains("in"))
			temp=p.substring(0,p.lastIndexOf("in"));
		predCol=temp.substring(temp.lastIndexOf(".")+1);
		if(indexCols.indexOf(predCol)!=-1)
		pos.add(indexCols.indexOf(predCol));
	}
	Collections.sort(pos);
	int cnt= outerList.size()+1;
	
    for(Integer n : pos)
    {
    	String col=indexCols.get(n);
    	for(Predicate p :inner)
    	{
    		if(p.getText().contains(col))
    		{
    			p.setSequence(cnt);
    			cnt++;
    		}
    	}
    }
	}
	else
	{
		listOrder(innerList,predicateList,outerList.size()+1);
		
	}
		
}
public String orderIndex(HashMap<String,Integer> matchList,ArrayList<Predicate> predicateList,ArrayList<String>screenList)
{
	Set<String> list=matchList.keySet();
	double min=9999999;
	HashMap<String,ArrayList<String>> pred= new HashMap<String,ArrayList<String>>();
	
	String first=null,minKey=null;
	if(list.size()==1)
	{
		for(String s : list)
		first=s;
        for(Predicate p: predicateList)
        {
           first=first.trim();
           
        	if(p.getText().trim().equalsIgnoreCase(first.substring(0, first.lastIndexOf("_"))))
        	{
        		
               p.setSequence(1);
        	}
        }
		
	}
	else
	{
		for(String s : list)
		{
			s=s.trim();
			ArrayList<String> values=new ArrayList<String>();
			String value=s.substring(0,s.lastIndexOf("_"));
			s=s.substring(0,s.lastIndexOf("."))+"_"+s.substring((s.lastIndexOf("_")+1));
			if(pred.containsKey(s))
			{
				values=pred.get(s);
				values.add(value);
				pred.remove(s);
				pred.put(s, values);
			}
				
			else
			{
				values.add(value);
				pred.put(s, values);
			}			
		}
		
		Set<String> predValues=pred.keySet();
		ArrayList<String> pValues=new ArrayList<String>();
		for(String s : predValues)
			pValues.add(s);
	
		if(predValues.size()==1)
		{
			for(String s : predValues)
			{
			
			HashMap<String,ArrayList<String>> indexList=getIndexList(s.substring(0,s.lastIndexOf("_")));
			ArrayList<String> Cols=indexList.get(s.substring(s.lastIndexOf("_")+1));
			ArrayList<String>values=pred.get(s);
			int cnt=1;
			for(String in : Cols)
			{
				for(Predicate p : predicateList)
				{
					if(p.getText().contains(in))
					{
						p.setSequence(cnt);
						cnt++;
					}
				}
				
			}
			}
		}
		else
		{
			Integer[] screencnt=new Integer[predValues.size()];
			for(int f=0;f<screencnt.length;f++)
				screencnt[f]=0;
			int g=0;
			for(String p1:predValues)
			{ 
				for(String t : screenList)
				{
					if(t.contains(p1.substring(p1.lastIndexOf("_")+1)))
						screencnt[g]=screencnt[g]+1;
				}
				g++;
			}
			ArrayList<Integer> screen=new ArrayList<Integer>();
			for(int b =0;b<g;b++)
			{
				screen.add(screencnt[b]);
			}
		
	       
	       int frq=Collections.frequency(screen, Collections.max(screen));
	      
	       if(frq==1)
	       {
	          minKey=pValues.get(screen.indexOf(Collections.max(screen)));
	     
	       }
	       else
	       {
	         	for(String key : predValues)
		        {
			        ArrayList<String> values=pred.get(key);
			        first=values.get(0);
			        double currff=1.0;
			        for(String s : values)
			        {
				        s=s.trim();
				        currff=1.0;
			            for(Predicate p: predicateList)
			            {
			 	            first=first.trim();
				            if(p.getText().trim().equalsIgnoreCase(s)||p.getDescription().trim().equalsIgnoreCase(s) )
	        	            {
	        		          currff=currff*p.ff1;
	        	            }
			            }
			       }
			        
	               if(currff<min)
	               {
	            	   min=currff;
	            	   minKey=key;
	               }
	              
	        	}
	       }
	        
		ArrayList<String> str=pred.get(minKey);
		
		if(str.size()==1)
		{
		for(Predicate p : predicateList)
		{
			first=str.get(0);
			first=first.trim();
			if(p.getText().trim().equalsIgnoreCase(first))
	    	{
			    p.setSequence(1);   	
	    	
	    	}
			
		
	       }
		}
		else
			listOrder(str,predicateList,0);
		}	
	
	}
	
	return minKey;
}
public ArrayList<Predicate> chkIn(ArrayList<String> list, ArrayList<Predicate> predicateList,ArrayList<String> list1)
{
	HashMap<String,ArrayList<String>> pred= new HashMap<String, ArrayList<String>>();
	int card=0;
	String text="";
    
	for(String l: list)
	{
		ArrayList<String> tempVal=new ArrayList<String>();
		l=l.trim();
		
		if(l.contains("="))
		{
			String key=l.substring(0,l.lastIndexOf("="));
			key=key.trim();
			if(pred.containsKey(key))
			{
			  tempVal=pred.get(key);
			  tempVal.add(l);
			  pred.remove(key);
			  pred.put(key, tempVal);
			}
			else
			{
				
				tempVal.add(l);
				 pred.put(key, tempVal);
			}
			
		}
	}
	
	Set<String> cols=pred.keySet();
	
	for(String col: cols)
	{
		ArrayList<String> values=pred.get(col);
		text="";
		if(values.size()>1)
		{
		list.removeAll(values);
		String inPred=col+" IN  (";
		for(String value:values)
		{
			String newline=String.format("%-72s", "\n");
			if(values.indexOf(value)+1==values.size())
				text=text+value;
			else
			text=text+value+" OR ";
			if(values.indexOf(value)+1==values.size())
				inPred=inPred + value.substring(value.lastIndexOf("=")+1)+")";
			else
			inPred=inPred + value.substring(value.lastIndexOf("=")+1)+",";
			value=value.trim();
			
			
		}
		
		ArrayList<Predicate> tempPred=new ArrayList<Predicate>();
		
		for(Predicate p: predicateList)
		{
			
				
			if(!values.contains(p.getText().trim()))
			{
				
				if(!tempPred.contains(p))
			    tempPred.add(p)	;
			   
			}
			else
				card=p.card1;
			
		}
		
		Predicate inP=new Predicate();
		inP.card1=card;
		inP.join=false;
		inP.inList=true;
		inP.type='I';
		inP.text=text;
		inP.ff1=(double)values.size()/card;
		inP.description=inPred;
		tempPred.add(inP);
		predicateList=tempPred;
		if(list1.isEmpty())
			list1.add(inPred);
		else
		list.add(inPred);
		
		}
	}

	return predicateList;
}

public HashMap<String,Integer> chkIndex(ArrayList<String> list)
{
	HashMap<String,Integer> indexPred=new HashMap<String,Integer>();
	HashMap<String,Integer> tempindexPred=new HashMap<String,Integer>();
    HashMap<String,ArrayList<String>> tabIndex=new HashMap<String,ArrayList<String>>();
    HashMap<String,Integer>matchColList=new HashMap<String,Integer>();
    ArrayList<String>setSeq=new ArrayList<String>();
	int i=0,j=0;
	for(i=0;i<list.size();i++)
	{
		String first=list.get(i);
		first=first.trim();
		for(j=i+1;j<list.size();j++)
		{
			String second=list.get(j);
			second=second.trim();
			if(first.substring(0, first.lastIndexOf(" ")).equalsIgnoreCase(second.substring(0, second.lastIndexOf(" "))))
			{
				if(first.substring(0,first.lastIndexOf("_")).equalsIgnoreCase(second.substring(0,second.lastIndexOf("_"))))
				{
				  if(first.substring(first.lastIndexOf("_")+1).equalsIgnoreCase(second.substring(second.lastIndexOf("_")+1)))
				   {
				      if(!setSeq.contains(first))
					  setSeq.add(first);
				      if(!setSeq.contains(second))
					  setSeq.add(second);
				   }
			    }
				else
				{
					
					      if(!setSeq.contains(first))
						  setSeq.add(first);
					      if(!setSeq.contains(second))
						  setSeq.add(second);
					   
				}
			}
		}
	}
	for(String currPred: list)
	{
		String temp=currPred;
		
		String tableName=temp.substring(0,temp.lastIndexOf("."));
		String indexName=temp.substring(temp.lastIndexOf("_")+1);
		
		if(tabIndex.containsKey(tableName))
		{
			ArrayList<String> indexList=tabIndex.get(tableName);
			if(!indexList.contains(indexName))
			indexList.add(indexName);
			tabIndex.remove(tableName);
			tabIndex.put(tableName, indexList);
		}
		else
		{
			ArrayList<String> indexList=new ArrayList<String>();
			indexList.add(indexName);
			tabIndex.put(tableName, indexList);
			
		}
	}
	
	Set<String> tableList=tabIndex.keySet();
	for(String s : tableList)
	{
		HashMap<String,ArrayList<String>> currList=getIndexList(s);
		Set<String> indexList=currList.keySet();
	    ArrayList<String> predIndexList=tabIndex.get(s);
	    int breakChk=0;
	    for(String str : predIndexList)
	    {
	    	int matchcols=0;
	    	
	    	ArrayList<String>colList =currList.get(str.trim());
	    	breakChk=0;
	    	for(String col: colList )
	    	{
	    		int pos=colList.indexOf(col);
	    	    if(breakChk==1&&pos!=0)
	    		 break;
	    	  for(String pred: list)
	    	  {
	    		
	    		if(pred.contains(s)&&pred.contains(str))
	    		{
	    			String predCol=null;
	    			if(pred.contains("="))
	    			predCol=pred.substring(pred.lastIndexOf(".")+1,pred.lastIndexOf("="));
	    			else if(pred.contains(">"))
		    			predCol=pred.substring(pred.lastIndexOf(".")+1,pred.lastIndexOf(">"));
	    			else if(pred.contains("<"))
		    			predCol=pred.substring(pred.lastIndexOf(".")+1,pred.lastIndexOf("<"));
	    			else if(pred.contains("in"))
		    			predCol=pred.substring(pred.lastIndexOf(".")+1,pred.lastIndexOf("in"));
	    			else if(pred.contains("IN"))
		    			predCol=pred.substring(pred.lastIndexOf(".")+1,pred.lastIndexOf("IN"));
	    			else if(pred.contains("In"))
		    			predCol=pred.substring(pred.lastIndexOf(".")+1,pred.lastIndexOf("In"));
	    			if(predCol.trim().equalsIgnoreCase(col))
	    				matchcols=matchcols+1;
	    				
	    			
	    		}
	    	}
	    	 
	    	 if(pos==0&&matchcols==0)
	    		 breakChk=1;
	    	}
	    	
	    	matchColList.put(s+"_"+str, matchcols);
	    	
	    }
	}
	Set<String> matchIndex= matchColList.keySet();
	int maxMatchCol=Collections.max(matchColList.values());
	for(String p : list )
	{
	   for(String s: matchIndex)
	   {
		   
		   if(p.contains(s.substring(0,s.lastIndexOf("_"))) && p.contains(s.substring(s.lastIndexOf("_")+1)))
		   {
			  
			   if(matchColList.get(s)==maxMatchCol)
			   {
				  indexPred.put(p, matchColList.get(s));
			   }
		   }
	   }
	}
	
	Set<String> print= indexPred.keySet();
	if(!setSeq.isEmpty())
	{
	for(String t: print)
	{
		String[] tokens= setSeq.get(0).split("\\s+");
		if(t.contains(tokens[0]))
		{
			int tempMatch=indexPred.get(t);
			int match=0;
			if(tempMatch>0)
			match=tempMatch-setSeq.size()+1;
			tempindexPred.put(t, match);
		}
		
	}
	indexPred=tempindexPred;
	}
	return indexPred;
}
public int chkOr(String predicates[][],ArrayList<String>andList,ArrayList<String>orList,String literal)
{
	int res=0;
	
	for(int i=0,j=1;predicates[i][0]!=null;i++)
	{
		
		if(predicates[i][1]=="2")
		{
			if(i==0)
			{
				if(predicates[i+1][0]==null)
				{
					orList.add(predicates[i][0].trim());
					
				}
				
				if(predicates[i+2][0]==null)
				{
					orList.add(predicates[i][0].trim());
					orList.add(predicates[j][0].trim());
				}
				
				else
				{
					orList.add(predicates[i][0].trim());
                	orList.add(predicates[j][0].trim());
				 }
				
			}
			else
				orList.add(predicates[j][0].trim());
			
			res=1;
		}
		else if(predicates[i][1]=="1")
		{		
				if(i==0)
					andList.add(predicates[i][0].trim());
				andList.add(predicates[j][0].trim());
		}
		else if(predicates[i][1]=="0"&&i==0&& !literal.contains("or"))
		{
			
			andList.add(predicates[i][0].trim());
		}
		else if(predicates[i][1]=="0"&&literal.contains("or"))
			orList.add(predicates[i][0].trim());
		j++;
		
	}
	return res;
}
public int getTabCard(String tableName)
{
	int card=0;
	for(Table tab:tables)
	{
		
		if (tab.getTableName().trim().equalsIgnoreCase(tableName.trim())&& !tab.delete) {
			
		      card=tab.getTableCard();
		      break;
			}
		}
		
	return card;
}
public int getColCard(String tableName,String colName)
{
	int colCard=0;
	Table table=null;
	
	for(Table tab:tables)
	{
		
		if (tab.getTableName().trim().equalsIgnoreCase(tableName.trim())&& !tab.delete) {
			
		      table=tab;
		      break;
			}
		}
		if(table!=null)
		{
			
			ArrayList<Column>colList=table.getColumns();
			for(Column col: colList)
			{
				
				if(col.getColName().equalsIgnoreCase(colName.trim()))
				{
				  colCard=col.getColCard();
				  
				}
			}
		
	}
	return colCard;
}
public String getlowKey(String tableName,String colName)
{
	String lowKey=null;
	Table table=null;
	
	for(Table tab:tables)
	{
		
		if (tab.getTableName().trim().equalsIgnoreCase(tableName.trim())&& !tab.delete) {
			
		      table=tab;
		      break;
			}
		}
		if(table!=null)
		{
			
			ArrayList<Column>colList=table.getColumns();
			for(Column col: colList)
			{
				
				if(col.getColName().equalsIgnoreCase(colName.trim()))
				{
				  lowKey=col.getLoKey();
				  
				}
			}
		
	}
	return lowKey;
}
public String getHiKey(String tableName,String colName)
{
	String hiKey=null;
	Table table=null;
	
	for(Table tab:tables)
	{
		
		if (tab.getTableName().trim().equalsIgnoreCase(tableName.trim())&& !tab.delete) {
			
		      table=tab;
		      break;
			}
		}
		if(table!=null)
		{
			
			ArrayList<Column>colList=table.getColumns();
			for(Column col: colList)
			{
				
				if(col.getColName().equalsIgnoreCase(colName.trim()))
				{
				  hiKey=col.getHiKey();
				  
				}
			}
		
	}
	return hiKey;
}
public int chkPredicates(String predicateList[][], String sql , HashMap<String, ArrayList<String>> predTabList) throws DbmsError
{
	int res=0, i=0,j=0;
	String fromList=sql.substring(sql.toLowerCase().lastIndexOf("from"), sql.toLowerCase().lastIndexOf("where"));
	for(i=0;predicateList[i][0]!=null;i++)
	{
		String predicateTokens[]=null;
		String currPredicate=predicateList[i][0];
		if(currPredicate.contains(">"))
		{
			predicateTokens=currPredicate.split(">");	
		}
		else if(currPredicate.contains("<")||currPredicate.contains("<"))
		{
			
			predicateTokens=currPredicate.split("<");	
		}
		else if(currPredicate.contains("=")||currPredicate.contains("!="))
		{
			if(currPredicate.contains("!="))
			predicateTokens=currPredicate.split("!=");
			else
				predicateTokens=currPredicate.split("=");	
				
		}
		
		else if(currPredicate.toLowerCase().contains("in"))
		{
			predicateTokens=currPredicate.split("(?i)in");
		}
		
		
		if(predicateTokens[0].contains("."))
		{
		String tableName=predicateTokens[0].substring(0,predicateTokens[0].lastIndexOf("."));
		String colName=predicateTokens[0].substring(predicateTokens[0].lastIndexOf(".")+1);
		predTabList=createPredList(predTabList,tableName,colName); 
		int chk=chkTableCol(tableName,colName);
		 if(chk==-1)
		  {
			  res=-1;
			  throw new DbmsError("ERROR: TABLE DOES NOT EXIST");
		  }
		  else if(chk==-2)
		  {
			  res=-1;
			  throw new DbmsError("ERROR: INVALID COLUMN NAME");
		  }
		  if(!fromList.contains(tableName.trim()))
		  {
			  res=-1;
			  throw new DbmsError("SYNTAX ERROR : TABLE NOT PRESENT IN FROM CLAUSE");
		  }
			  
		  if(!predicateTokens[1].contains("."))
		  {
			  
		   if(predicateTokens[1].trim().startsWith("("))
		   {
			  
			   if(predicateTokens[1].trim().endsWith(")"))
			   {
				   String tempInValues=predicateTokens[1].substring(predicateTokens[1].lastIndexOf("(")+1,predicateTokens[1].lastIndexOf(")"));
				   String inTokens[]=tempInValues.split(",");
				   for(int m=0;m<inTokens.length;m++)
				   {
					
					   if(inTokens[m].trim().split("\\s+").length>1)
					   {
						   res=-1;
						   throw new DbmsError("SYNTAX ERROR: MISSING COMMA");
 
					   }
					   else
					   {
						   int type=chkDataType(tableName,colName,inTokens[m]);
						   
					   }
						   	   
						   
				   }
				   
			   }
			   else
			   {
				   res=-1;
				   throw new DbmsError("SYNTAX ERROR: MISSING PARANTHESES");
			   }
			   
				   
		   }
		   else
		   {
			   int type=chkDataType(tableName,colName,predicateTokens[1]);
			   
		   }
		   if (predicateTokens[1].contains(")")&&!predicateTokens[1].contains("("))
		   {
			   res=-1;
			   throw new DbmsError("SYNTAX ERROR: MISSING PARANTHESES");
		   }
		   
		  }
		  else
		  {
			  if(predicateTokens[0].substring(0,predicateTokens[0].lastIndexOf(".")).equalsIgnoreCase(predicateTokens[1].substring(0,predicateTokens[1].lastIndexOf("."))))
				  throw new DbmsError("SYNTAX ERROR : JOIN ON SAME TABLE");
			  String firstTable=predicateTokens[0].substring(0,predicateTokens[0].lastIndexOf("."));
			  String firstCol=predicateTokens[0].substring(predicateTokens[0].lastIndexOf(".")+1);
			  String firstType=getType(firstTable,firstCol);
			  tableName=predicateTokens[1].substring(0,predicateTokens[1].lastIndexOf("."));
			  colName=predicateTokens[1].substring(predicateTokens[1].lastIndexOf(".")+1);
			  String secType=getType(tableName,colName);
			  if(!firstType.equalsIgnoreCase(secType))
				  throw new DbmsError("SYNTAX ERROR: INVALID JOIN DATA TYPES");
			  int chkJoin=chkTableCol(tableName,colName);
			  if(chkJoin==-1)
			  {
                  res=-1;
				  throw new DbmsError("ERROR: TABLE DOES NOT EXIST");
			  }
			  else if(chkJoin==-2)
			  {
				  res=-1;
				  throw new DbmsError("ERROR: INVALID COLUMN NAME");
			  }
			  if(!fromList.contains(tableName))
			  {
				  res=-1;
				  throw new DbmsError("SYNTAX ERROR: TABLE NOT PRESENT IN FROM CLAUSE");
			  }
				  
			  
		  }
		}  
		
	}
	
	return res;
}
public HashMap<String,ArrayList<String>> createPredList(HashMap<String,ArrayList<String>> list , String tableName, String colName)
{
	if(list.containsKey(tableName.trim()))
	{
		ArrayList<String>colList=list.get(tableName.trim());
		colList.add(colName.trim());
	    list.remove(tableName.trim());
	    list.put(tableName.trim(), colList);
	}
	return list;
}
public String getType(String tableName, String colName) 
{
	String type=null;
	Table table=null;
	for(Table tab : tables)
	{
		if (tab.getTableName().equalsIgnoreCase(tableName.trim())&& !tab.delete) {
	      table=tab;
	      break;
		}
	}
	if(table!=null)
	{
		ArrayList<Column>colList=table.getColumns();
		for(Column col: colList)
		{
			if(col.getColName().equalsIgnoreCase(colName.trim()))
			{
				if(col.getColType() == Column.ColType.CHAR)
				{
	                type = "CHAR";
				}
				else if (col.getColType()== Column.ColType.INT)
					type="INT";
			}
		}
	}
	return type;
}
public int chkDataType(String tableName, String colName, String value) throws DbmsError
{
	int res=-1;
	Table table=null;
	for(Table tab : tables)
	{
		if (tab.getTableName().equalsIgnoreCase(tableName.trim())&& !tab.delete) {
	      table=tab;
	      break;
		}
	}
	if(table!=null)
	{
		ArrayList<Column>colList=table.getColumns();
		for(Column col: colList)
		{
			if(col.getColName().equalsIgnoreCase(colName.trim()))
			{
				if(col.getColType() == Column.ColType.CHAR)
				{
					value=value.trim();
				     if(value.length()<=col.getColLength())
				    	 res=0;
				     else
				    	 throw new DbmsError("ERROR: NOT A VALID COLUMN VALUE");	 
				     
				}
				else if(col.getColType() == Column.ColType.INT)
				{
					
					try
					{
						int num=Integer.parseInt(value.trim());
					}catch(NumberFormatException e)
					{
						throw new DbmsError("ERROR: NOT A INTEGER TYPE");
					}
					
					
				}
					
			}
			if(res==0)
				break;
		}
	}
	return res;
}
public void printIndexList(String tableName)
{
	IndexList il= new IndexList();
	for (Table tab : tables)
	{
		if (tab.getTableName().equalsIgnoreCase(tableName.trim())&& !tab.delete) {
			ArrayList<Index> indexList=tab.getIndexes();
			for(Index in : indexList)
			{
				il.list.add(in);
			}
		   il.printTable(out);
		}
	}
}
public int chkTableCol(String tableName, String colName) 
{
	int res=0,colchk=-2,tabchk=-1;
	Table table=null;

	for (Table tab : tables) {
		if (tab.getTableName().equalsIgnoreCase(tableName.trim())&& !tab.delete) {
			
			tabchk=1;
			table=tab;
			break;
		}
	}
	
			if(tabchk==1)
			{
				ArrayList<Column> colList=table.getColumns();
				for(Column col:colList)
				{
					
					if(col.getColName().compareToIgnoreCase(colName.trim())==0)
					{
						colchk=1;
						break;
					}
					if(colchk==1)
						break;
					
				}
			 
			}
			else
				res=-1;
			if(colchk!=1 && tabchk==1)
				res=-2;
			if(colchk==1&&tabchk==1)
				res=1;
	
			return res;
	
}
	private void storeTableFile(Table table) throws FileNotFoundException {
		File tableFile = new File(TABLE_FOLDER_NAME, table.getTableName()
				+ TABLE_FILE_EXT);

		// Delete the file if it was marked for deletion
		if (table.delete) {
			try {
				tableFile.delete();
			} catch (Exception ex) {
				out.println("Unable to delete table file for "
						+ table.getTableName() + ".");
			}
			
			// Delete the index files too
			for (Index index : table.getIndexes()) {
				File indexFile = new File(TABLE_FOLDER_NAME, table.getTableName()
						+ index.getIdxName() + INDEX_FILE_EXT);
				
				try {
					indexFile.delete();
				} catch (Exception ex) {
					out.println("Unable to delete table file for "
							+ indexFile.getName() + ".");
				}
			}
		} else {
			// Create the table file writer
			PrintWriter out = new PrintWriter(tableFile);

			// Write the column descriptors
			out.println(table.getNumColumns());
			for (Column col : table.getColumns()) {
				if (col.getColType() == Column.ColType.INT) {
					out.println(col.getColName() + " I " + col.isColNullable());
				} else if (col.getColType() == Column.ColType.CHAR) {
					out.println(col.getColName() + " C" + col.getColLength()
							+ " " + col.isColNullable());
				}
			}

			// Write the index info
			out.println(table.getNumIndexes());
			for (Index index : table.getIndexes()) {
				if(!index.delete) {
					String idxInfo = index.getIdxName() + " " + index.getIsUnique()
							+ " ";

					for (Index.IndexKeyDef def : index.getIdxKey()) {
						idxInfo += def.colId;
						if (def.descOrder) {
							idxInfo += "D ";
						} else {
							idxInfo += "A ";
						}
					}
					out.println(idxInfo);
				}
			}

			// Write the rows of data
			out.println(table.getData().size());
			for (String data : table.getData()) {
				out.println(data);
			}

			// Write RUNSTATS
			out.println("STATS TABCARD " + table.getTableCard());
			for (int i = 0; i < table.getColumns().size(); i++) {
				Column col = table.getColumns().get(i);
				if(col.getHiKey() == null)
					col.setHiKey("-");
				if(col.getLoKey() == null)
					col.setLoKey("-");
				out.println("STATS COLCARD " + i + " " + col.getColCard() + " " + col.getHiKey() + " " + col.getLoKey());
			}
			
			out.flush();
			out.close();
		}

		// Save indexes to file
		for (Index index : table.getIndexes()) {

			File indexFile = new File(TABLE_FOLDER_NAME, table.getTableName()
					+ index.getIdxName() + INDEX_FILE_EXT);
            
			// Delete the file if it was marked for deletion
			if (index.delete) {
				try {
					indexFile.delete();
				} catch (Exception ex) {
					out.println("Unable to delete index file for "
							+ indexFile.getName() + ".");
				}
			} else {
				PrintWriter out = new PrintWriter(indexFile);
				String idxInfo = index.getIdxName() + " " + index.getIsUnique()
						+ " ";

				// Write index definition
				for (Index.IndexKeyDef def : index.getIdxKey()) {
					idxInfo += def.colId;
					if (def.descOrder) {
						idxInfo += "D ";
					} else {
						idxInfo += "A ";
					}
				}
				out.println(idxInfo);

				// Write index keys
				out.println(index.getKeys().size());
				for (Index.IndexKeyVal key : index.getKeys()) {
					String rid = String.format("%3s", key.rid);
					out.println(rid + " '" + key.value + "'");
				}

				out.flush();
				out.close();

			}
		}
	}
}
