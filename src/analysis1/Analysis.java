package analysis1;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Analysis {
	
	public static final String LOG_FILE_NAME = "hoonlir.access.log"; // the name of the log file we are processing
	public static final String OUT_FILE_NAME = "analysis.output.txt";
	
	/**
	 * parse a line of log file
	 * 
	 * @param line
	 * @return lineMap - HashMap<String,String>
	 */
	public static HashMap<String,String> parseLine(String line) {
		// use this hashmap to store the parsed fields of a single line of the access log
		HashMap<String,String> map = new HashMap<String,String>();
		
		// the regular expressions to parse the log
		String logLinePattern[] = new String[10];
		logLinePattern[0] = "^([\\d.]+)"; // client IP (remote host)
		logLinePattern[1] = " (\\S+)"; // the identity of the user determined by identd. normally "-"
		logLinePattern[2] = " (\\S+)"; // the user name determined by HTTP authentication. normally "-"
		logLinePattern[3] = " \\[([\\w:/]+\\s[+\\-]\\d{4})\\]"; // time that the request was received, [day/month/year:hour:minute:second zone]
		logLinePattern[4] = " \"(.+?)\""; // request line from the client is given in double quotes.
		logLinePattern[5] = " (\\d{3})"; // HTTP status code
		logLinePattern[6] = " (\\d+|-)"; // the size (number of bytes) of the object returned to the client, not including the response headers. If 0, this would be -
		logLinePattern[7] = " \"([^\"]+)\""; // Referer
		logLinePattern[8] = " \"([^\"]+)\""; // User-Agent
		logLinePattern[9] = " \"([^\"]+)\""; // An additional field in the log file, indicating the site's url
		
		// we now have the regex to parse a log line
		String logLineRegex = String.join("", logLinePattern);
		Pattern pattern = Pattern.compile(logLineRegex);
		Matcher matcher = pattern.matcher(line);

		// try to match the line
		if(!matcher.matches()) {
			System.err.println("Bad entry line! -->> "+line);
			return null;
		}
	
		// put the fields in a hashmap
	    map.put("clientip", matcher.group(1));
	    map.put("identity", matcher.group(2));
	    map.put("username", matcher.group(3));
	    map.put("time", matcher.group(4));
	    map.put("request", matcher.group(5));
	    map.put("statuscode", matcher.group(6));
	    map.put("responsesize", matcher.group(7));
	    map.put("referer", matcher.group(8));
	    map.put("useragent", matcher.group(9));
	    
		return map;
	}
	
	/**
	 * check if a particular line of log stands for a valid page view
	 * 
	 * @param lineMap
	 * @return flag - boolean
	 */
	public static boolean checkIfValidPageView(Map<String,String> lineMap) {
		if(!lineMap.containsKey("statuscode") || !lineMap.containsKey("request"))
			return false;
	
		// the accesses that did not succeed with status code 200 are ignored
		if(!lineMap.get("statuscode").equals("200"))
			return false;
		
		/* 
		 * the requests that asks for resources other than pages are not counted 
		 * excluding: /, /xx.js, /xx.css, /xx.sass, /xx.less, .jpg, .jpeg, .tif, .tiff, .gif, .png, .flv
		 */
		String request = lineMap.get("request");
		
		// retrieve the second part of the request
		String regex1  = ".+ (.+) .+"; 
		Pattern p1 = Pattern.compile(regex1);
		Matcher m1 = p1.matcher(request);
		if(!m1.matches()) {
			System.err.println("Bad request! -->> "+request);
			return false;
		}
		String res = m1.group(1);
		
		// exclude the resources that are not pages
		String regex2 = ".js$|.css$|.sass$|.less$|.jpg$|.jpeg$|.tif$|.tiff$|.gif$|.png$|.flv$";
		Pattern p2 = Pattern.compile(regex2);
		Matcher m2 = p2.matcher(res);
		if(m2.find())
			//if found, then do not count this line of log as a page view
			return false;
		
		return true;
	}
	
	
	public static void main(String[] args) {

		// store all the records
		//ArrayList<Map<String, String>> records = new ArrayList<Map<String,String>>();
		
		ArrayList<Visit> visits = new ArrayList<Visit>(); // Visits
		Map<String, Visitor> visitorsMap = new HashMap<String,Visitor>(); // Unique visitors: <clientip, Visitor>
		
		int pageViewsCnt = 0; // page views counter
		
		boolean startflag   = true;
		Date starttimeOfAll = new Date(); // the start time of this log file
		Date endtimeOfAll   = new Date(); // the end time of this log file
		
		/* Processing the log file */
		
		BufferedReader reader = null;
		try {
			String currentLine;

			reader = new BufferedReader(new FileReader(LOG_FILE_NAME));

			while ((currentLine = reader.readLine()) != null) {
				Map<String, String> record = parseLine(currentLine);
				
				// eliminate lines of log that are not page views 
				if(checkIfValidPageView(record)) {
					// if the line is a line of log of a valid page view
					// now produce the visitors info during the course of processing the log file
					String clientip = record.get("clientip");
					String time = record.get("time");
					
					// retrieve the time as a Date
					Date datetime = new Date();
					
					String timeregex = "([\\w:/]+\\s[+\\-]\\d{4})";
					Pattern p = Pattern.compile(timeregex);
					Matcher m = p.matcher(time);
					if(!m.matches()) {
						System.err.println("Bad request time format! -->> " + record.get("time"));
						continue;
					}
					String datepattern = "dd/MMM/yyyy:HH:mm:ss ZZZZ";
				    SimpleDateFormat format = new SimpleDateFormat(datepattern,Locale.US);
				    try {
				    	datetime = format.parse(m.group(1));
				    	endtimeOfAll = datetime; // updating the last time of page view
				    	if(startflag) {
				    		starttimeOfAll = datetime; // record the start time of this log file
				    		startflag = false;
				    	}	
				    } catch (ParseException e) {
				        e.printStackTrace();
				        continue;
				    }
					
				    // update/insert visitors info
					if(!visitorsMap.containsKey(clientip)) {
						// if this is a new visitor
						visitorsMap.put(clientip, new Visitor(clientip, datetime));
					} else {
						// if the visitor has visited before
						Visitor tmpVisitor = visitorsMap.get(clientip);
						// add this access time into its list of access times
						tmpVisitor.accessTimes.add(datetime);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	
		
		/* traverse the visitors map to produce the visits info */
		
		// produce the visits ArrayList and count the visistsno (number of visits) for each visitor
		for(Visitor visitor: visitorsMap.values()) {
			// initiate the recording variables
			Date visitStartTime = visitor.accessTimes.get(0); // record the starting time of a visit
			Date lastAccessTime = visitor.accessTimes.get(0); // record the last time of an access
			int pagecnt = 0;  // page views counter only for a single visit
			
			int i = 1; // record the progress in the list of access times, later used to tell if the end of the list is reached
			
			for(Date tmpTime: visitor.accessTimes) {
				// a visit ends when the user hasn¡¯t viewed any pages for 30 minutes
				if((tmpTime.getTime()-lastAccessTime.getTime())/1000 > 1800) {
					// if this is a new visit (and not the first visit for this visitor)
					visits.add(new Visit(visitStartTime, lastAccessTime, pagecnt, visitor.getClientIP()));
					
					// update the visitor's info
					visitor.visitsNOIncre();  // add a visit number
					visitor.setNotBounce();   // flag the visitor that he/she is not a bounce visitor
					
					// reset the recording variables
					visitStartTime = tmpTime;
					pagecnt = 0;
				}
				
				// if this is not a new visit, do nothing
				
				// update the recording variables
				lastAccessTime = tmpTime;
				pagecnt ++;
				
				// update the page views counter
				pageViewsCnt++;
				
				// if reached the end of the last visit, explicit addition of it to the access times list is necessary
				// for example, if the visitor only visit once, the code above in the for loop will never be executed
				if(i==visitor.accessTimes.size())
					visits.add(new Visit(visitStartTime, lastAccessTime, pagecnt, visitor.getClientIP()));
				
				i++; // update i
			}
			
		}
		
		/* calculation of the metrics */
		
		double pagesPerVisit = 0;
		for(Visit visit: visits) {
			pagesPerVisit += visit.getPagesno();
		}
		pagesPerVisit /= visits.size();
		// note: pagesPerVisit can also be calculated by page views/ number of visits,
		// here I am using this loop just to check if the code overall is correct
		
		double aveVisitDuration = 0; // in seconds
		for(Visit visit: visits) {
			aveVisitDuration += visit.getDuration();
		}
		aveVisitDuration /= visits.size();
		
		double bounceRate = 0;
		for(Visitor visitor: visitorsMap.values()) {
			if(visitor.getIfBounce())
				// if the visitor is a bouncer
				bounceRate += 1;
		}
		bounceRate /= visitorsMap.size();
		
		
		/* output the results of calculation */
		
		System.out.println("time frame: from "+starttimeOfAll+ " to "+endtimeOfAll);
		System.out.println("1. Unique visitors: " + visitorsMap.size());
		System.out.println("2. Page views: " + pageViewsCnt);
		System.out.println("3. Number of visits: " + visits.size());
		System.out.println("4. Pages per visit: " + pagesPerVisit);
		System.out.println("5. Average visit duration: " + aveVisitDuration + " seconds");
		System.out.println("6. Bounce rate: " + bounceRate*100 + "%");
		System.out.println("7. Percentage of new visits: " + visitorsMap.size()*100.00/visits.size() + "%"); // every visitor would only have one new visit
		
		/* Write the results to an output file */
		try{
			File outfile = new File(OUT_FILE_NAME);
			// if the file doesn't exists, create it
			if (!outfile.exists()) {
				outfile.createNewFile();
			}
			FileWriter fw = new FileWriter(outfile.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			
			bw.write("time frame: from "+starttimeOfAll+ " to "+endtimeOfAll);
			bw.newLine();
			bw.newLine();
			bw.write("1. Unique visitors: " + visitorsMap.size());
			bw.newLine();
			bw.write("2. Page views: " + pageViewsCnt);
			bw.newLine();
			bw.write("3. Number of visits: " + visits.size());
			bw.newLine();
			bw.write("4. Pages per visit: " + pagesPerVisit);
			bw.newLine();
			bw.write("5. Average visit duration: " + aveVisitDuration + " seconds");
			bw.newLine();
			bw.write("6. Bounce rate: " + bounceRate*100 + "%");
			bw.newLine();
			bw.write("7. Percentage of new visits: " + visitorsMap.size()*100.00/visits.size() + "%"); // every visitor would only have one new visit
			bw.newLine();
			bw.newLine();
			bw.newLine();
			
			for(Visitor visitor: visitorsMap.values()){
				bw.write(visitor.output());
				bw.newLine();
			}

			bw.close();

			System.out.println("Writing the ouput file -- Done");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
	}

}
