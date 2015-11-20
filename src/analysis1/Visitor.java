package analysis1;

import java.util.ArrayList;
import java.util.Date;

public class Visitor {
	private boolean bounce; // a flag to indicate if this visit is a bounce
	private String clientip; // the clientip of this particular visitor
	private int visitsno; // the number of visits this visitor conducted to the site within the timeframe
	public ArrayList<Date> accessTimes; // the list of times of accesses (an access stands for one page view)
	
	public Visitor(String clientip, Date startTime) {
		this.clientip = clientip;
		this.visitsno = 1;
		this.accessTimes = new ArrayList<Date>();
		this.accessTimes.add(startTime);
		this.bounce = true; // set the default, update when there is more than one visit from this visitor
	}
	
	public boolean getIfBounce() {
		return this.bounce;
	}
	
	public void setNotBounce() {
		this.bounce = false;
	}
	
	public int getVisitsNo() {
		return this.visitsno;
	}
	
	public String getClientIP() {
		return this.clientip;
	}
	
	public int visitsNOIncre() {
		// increment the visitsno
		return ++this.visitsno;
	} 
	
	public void setVisitsNo(int visitsno) {
		this.visitsno = visitsno;
	}
	
	public String output() {
		String output = "Client IP: "+this.clientip+"\n\t"+"Visits#: "+this.visitsno+"\t"+"Bounce? "+this.bounce;
		return output;
	}
}
