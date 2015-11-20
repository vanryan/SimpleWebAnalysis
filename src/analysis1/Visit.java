package analysis1;

import java.util.Date;

public class Visit {
	public Date startTime; // datetime
	public Date endTime; // datetime
	public long duration; // in seconds
	private int pagesno; // number of pages for this visit
	private String clientip; // the clientip of this particular visit
	
	public Visit(Date start, Date end, int pagesno, String clientIP) {
		this.startTime = start;
		this.endTime   = end;
		this.duration  = (this.endTime.getTime() - this.startTime.getTime()) / 1000;
		this.pagesno   = pagesno;
		this.clientip  = clientIP;
	}
	
	public int getPagesno() {
		return this.pagesno;
	}
	
	public long getDuration() {
		return this.duration;
	}
	
	public String getClientIP() {
		return this.clientip;
	}
}
