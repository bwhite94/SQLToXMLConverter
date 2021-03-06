/*
 * xmlFormat class
 * 
 * xmlFormat class formats the SQL data into proper XML output
 * Class is passed a ResultSet object from a selection query
 * Class is passed an ArrayList of Attribute objects from the SQLParser class
 * XML output is displayed to the console
 * 
 */

//import Java libraries
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

public class xmlFormat {
	
	public xmlFormat(ResultSet ret, ArrayList<Attribute> lst) throws SQLException{
		
		XML(ret,lst);
	}
	
	static ResultSet rSet;					// make ResultSet global
	static ArrayList<Attribute> aList;		// make ArrayList global
	static ArrayList<String> dList;
	static ArrayList<String> dtdList;
	static ArrayList<String> xsdList;
	
	public static void XML(ResultSet ret, ArrayList<Attribute> lst) throws SQLException {
		dList = new ArrayList<String>();
		dtdList = new ArrayList<String>();
		xsdList = new ArrayList<String>();
		rSet = ret;								// set the ResultSet to the global variable
		aList =lst;								// set the ArrayList to the global variable
		int colCount = 0; 						// used to keep track of the current column
		int groupCount = 0;						// used to keep track of the group scope
		int length = aList.size();				// used to track the length of the ArrayList
		int tabCnt = 1;							// used to format tab whitespace for console output
		boolean groupFlag = false;				// used to keep track of grouping
		boolean openFlag = false;				//used to make sure we close A REcord
		boolean groupCompFlag = false;
		boolean compressionFlag = false;		// used to keep track of compression
		String[] gNames = new String[5];		// used to keep track of the name of the groups
		String tempGroup = "";					// used to track the name of the current group
		String tempAtname = "";					// used as a temporary holder for the Attribute object
		String prevAtName = "";					// used to track the previous Attribute object
		
		Scanner input = new Scanner(System.in);
		String selection = "";
		
		while (!selection.equals( "1") && !selection.equals( "2") && !selection.equals("3")) {

			System.out.println("\n-----------XML MENU------------");
			
			System.out.println("1. Display XML to the console.");
			System.out.println("2. Save XML to a file.");
			System.out.println("3. Do both option 1 and option 2.");
				
			System.out.println("Enter your selection:");
			selection = input.nextLine();

			if (!selection.equals( "1") && !selection.equals( "2") && !selection.equals("3")) {

				System.out.println("Invalid selection.  Try again.");
			}
		}
		
		try {		// error handling for SQLException
			
			dList.add("<?xml version ='1.0'?>");
			dList.add("<This Query>");

			while (rSet.next()) {		// move ResultSet to the next data row
				
				if (openFlag == false) {		// checks if data is to be compressed
					
					dList.add("<A Record>");
					openFlag =true;
				}
				
				// initialize the variables for the current ResultSet data row
				String tabName = aList.get(colCount).tableName;
				String alias = aList.get(colCount).alias;
				String gName ="";
				
				colLoop: while (colCount < length) {		// move the ResultSet to the next data column
					
					tabName = aList.get(colCount).tableName;			// grabs the current Attribute's table
					alias = aList.get(colCount).alias;					// grabs the current Attribute's alias
					
					if (aList.get(colCount).compFlag == true) {

						if (compressionFlag == false)				// saves previous Attribute's name
							tempGroup = prevAtName;
						openFlag = true;
						compressionFlag = true;						// flag to track if compression is processing
						tempAtname = aList.get(colCount-1).name;	// stores the previous column's name
					}
					else if(aList.get(colCount).group!=null &&  aList.get(colCount).group.compFlag ==true){
						
						if(groupCompFlag ==false){
							tempGroup = prevAtName;
						compressionFlag = true;
						groupCompFlag = true;
						tempAtname = aList.get(colCount-1).name;
						}
					}
					else if (groupCompFlag ==true && tempAtname.equals(aList.get(colCount).name)){
						
						String f = aList.get(colCount).name;		// stores the current Attribute's name
						String d = rSet.getString(f);				// stores the current ResultSet's name
						
						if (tempGroup.equals(d)) {		// checks if the current group equals the previous group
							openFlag = true;
							colCount++;					// increase the column counter for the inner loop
							continue colLoop;			// proceed to the next column
						}
						dList.add("</A Record>");
						dList.add("<A Record>");
						compressionFlag =false;
						groupCompFlag =false;
						
					}
					else if (compressionFlag == true  && tempAtname.equals(aList.get(colCount).name)) {

						String f = aList.get(colCount).name;		// stores the current Attribute's name
						String d = rSet.getString(f);				// stores the current ResultSet's name
						
						if (tempGroup.equals(d)) {		// checks if the current group equals the previous group
						openFlag = true;
							colCount++;					// increase the column counter for the inner loop
							continue colLoop;			// proceed to the next column
						}
					
						dList.add("</A Record>");
						dList.add("<A Record>");
						compressionFlag = false;		// mark compression flag; compression completed
					}
					
					if (aList.get(colCount).group != null && groupFlag == false) {		// checks Attribute for a group 

						groupCount++;										// increment the group counter
						groupFlag = true;									// flag to track grouping
						gName = aList.get(colCount).group.name;				// grabs Attribute's group name
						gNames[groupCount] = gName;							// saves the group name to an array
																			// used to close group tags

						if(gName != null)
						dList.add(String.format("%" + (4 * tabCnt) + "s", " ") + "<" + gName + ">");
						
						tabCnt++;		// increment the tab counter for whitespace format
					}
					
					else if (groupFlag == true && (aList.get(colCount).group.name != gName)) {	// entering nested groups
						
						groupCount++;														// increment the group counter
						groupFlag = true;													// flag to track grouping
						gName = aList.get(colCount).group.name;								// grabs Attribute's group name
						gNames[groupCount] = gName;											// saves the group name to an array
																							// used to close group tags
						if(gName != null)
						dList.add(String.format("%" + (4 * tabCnt) + "s", " ") + "<" + gName + ">");
						
						tabCnt++;		// increment the tab counter for whitespace format
					}
					
					String colName = aList.get(colCount).name;			// grab the current Attribute's name
					
					if (alias == null)				// check the current group for an alias
						alias = colName;			// set alias to column name if alias does not exist
								
					prevAtName = rSet.getString(colCount+1);
							
					dList.add(String.format("%" + (4 * tabCnt) + "s", " ") + 
							"<" + alias.toUpperCase() + " table=\""+ tabName + "\" name=\"" + colName +"\">" +
							prevAtName + "</" + alias.toUpperCase() + ">");
					
					colCount++;			// increase the column counter for the inner loop
				}	// end column while loop
				
				if (groupFlag == true) {		// checks for grouping
					
					while (groupCount > 0) {	// loop through grouping to display group closing tags
						
						gName = gNames[groupCount];		// set the current group tag to the tag stored in the array		
						
						tabCnt--;		// decrement the tab counter for whitespace format
						if(gName != null)
						dList.add(String.format("%" + (4 * tabCnt) + "s", " ") + "</" + gName + ">");
						
						groupCount--;		// decrement the group counter
					}
					
					groupFlag = false;		// reset the flag for grouping
				}
				
				if(openFlag && !compressionFlag){
					dList.add("</A Record>");
					openFlag =false;
				}
				
				colCount = 0;		// reset the column counter for the next ResultSet data row
			}	// end row while loop
			
			dList.add("</This Query>");			
			
			DTD(rSet,aList);
		} catch (SQLException e) {		// catch SQLException
			e.printStackTrace();		// display stack trace for thrown exception
		}
		
		if (selection.equals("1")) {
			Parray(dList);
		}
		
		else if (selection.equals("2")) {
			Sarray(dList,"XMLFile.xml");
		}
		
		else {
			Parray(dList);
			Sarray(dList,"XMLFile.xml");
		}
		
		dtdList = DTD(rSet, aList);
		xsdList = XSD(rSet, aList);
		
		String selXSD = "";
		
		while (!selXSD.equals("1") && !selXSD.equals("2") && !selXSD.equals("3"))
		{
			System.out.println("\n-----------XSD/DTD MENU------------");
			
			System.out.println("1. Display XSD and DTD to the console.");
			System.out.println("2. Save XSD and DTD to seperate files.");
			System.out.println("3. Do both option 1 and option 2.");
				
			System.out.println("Enter your selection:");
			selXSD = input.nextLine();

			if (!selXSD.equals("1") && !selXSD.equals("2") && !selXSD.equals("3"))
			{
				System.out.println("Invalid selection.  Try again.");
			}
		}
		
		if (selXSD.equals("1"))
		{
			Parray(dtdList);
			Parray(xsdList);
		}
		
		else if (selXSD.equals("2"))
		{
			Sarray(dtdList,"dtdFile.dtd");
			Sarray(xsdList,"xsdFile.xsd");
		}
		
		else
		{
			Parray(dtdList);
			Parray(xsdList);
			Sarray(dtdList,"dtdFile.dtd");
			Sarray(xsdList,"xsdFile.xsd");
		}
	}
	
	public static ArrayList DTD(ResultSet ret, ArrayList<Attribute> lst){		//when this function is called it will print the DTD Information
		
		ArrayList<String> dtdlist = new ArrayList<String>();
		int counter = 0;
		int STnum = 0;
		int EDnum = 0;
		int length = lst.size();
		boolean flag= false;
		String asdf ="";
	
		dtdlist.add("<?xml version ='1.0'?>");
	
	
		while(!flag){
		asdf =lst.get(counter).tableName;
		dtdlist.add("<!DOCTYPE "+ asdf+ " [ \n" );
		String temp = "";
		temp = ("<!ELEMENT " + asdf+ " (" ) ;
		STnum = counter ;
		temp +=(lst.get(counter++).name );
	
		while((counter< length) &&  asdf.equals(lst.get(counter).tableName))
		{
			temp+=(", " + lst.get(counter++).name);
		}
		temp +=(")> \n \n");
		EDnum = counter;
		counter = STnum;
		
		dtdlist.add(temp);
	
		while(counter< (EDnum)){
			dtdlist.add("<!ELEMENT  " + lst.get(counter++).name + " (#PCDATA)> \n");
		}
	
		dtdlist.add("]> \n");
	
		if(flag == false && ((counter) == length)){
			flag =true;
			counter++;
		}
		
		}
	 
	
		return dtdlist;
		}
		public static void pDTD(ArrayList<Attribute> lst){
		int counter = 0;
		int length = lst.size();
		String d = "";
		while(counter< length)
		{
			if(!lst.get(counter).tableName.equals(d))
			{
				d = lst.get(counter).tableName;
				System.out.println("<!DOCTYPE " + d+ " INFORMTATION \""+ d + "_Info.dtd\">");
			}
			counter++;
		}
	}
	
	public static ArrayList XSD (ResultSet ret, ArrayList<Attribute> lst) throws SQLException {		//when this function is called it will print XSD 
		
		int counter = 0;
		int tabCnt = 1;
		String tableName = lst.get(counter).tableName;
		ResultSetMetaData rsmd = ret.getMetaData();
		
		xsdList.add("<?xml version=\"1.0\"?>");
		
		while (counter < lst.size())
		{
			tableName = lst.get(counter).tableName;
			
			xsdList.add("<schema xmlns:xsd=\"" + tableName + "XSDnew\" elementFormDefault=\"qualified\" " +
					"attributeFormDefault=\"qualified\">");
			
			xsdList.add(String.format("%" + (4 * tabCnt) + "s", " ") + "<xsd:complexType name=\"" + tableName + "\">");
			tabCnt++;
			
			while (counter < lst.size() && tableName.equals((lst.get(counter).tableName)))
			{
				xsdList.add(String.format("%" + (4 * tabCnt) + "s", " ") + "<xsd:element name=\"" + lst.get(counter).name + "\" type=\"xsd:" +
						rsmd.getColumnTypeName(counter + 1) + "\" maxOccurs=\"1\" minOccurs=\"1\" />");
				
				counter++;
			}
			
			tabCnt--;
			
			xsdList.add(String.format("%" + (4 * tabCnt) + "s", " ") + "</xsd:complexType>");
			xsdList.add("</schema>");
		}
		
		return xsdList;
	}
	
	public static void Parray(ArrayList<String> alist){			//prints the array of strings 
		int length = alist.size();
		int i = 0;
		
		while (i< length){						//for the length of the array of strings 
			if(i ==1)
				pDTD(aList);
			System.out.println(alist.get(i));
			i++;
		}
	}
	public static void Sarray(ArrayList<String> amsd,String Filename){			//saves the array of Strings
		int i = 0;
		int counter = 0;
		int length = amsd.size();
		int length1 = aList.size();
		try(  PrintWriter mout = new PrintWriter( Filename,"UTF-8" )  ){	//creates file xmlFile.xml will overwrite if already exist
			while (i< length){
				
				if(i==1){
					String d = "";
					 while(counter< length1)
					 {
						 if(!aList.get(counter).tableName.equals(d))
							 {
							 d = aList.get(counter).tableName;
							 mout.println("<!DOCTYPE " + d+ " INFORMATION \""+ d + "_Info.dtd\">");		
							 }
						counter++;
						}
				}
				
				mout.println(amsd.get(i));
				i++;
			}
			
			mout.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}	
	}
}